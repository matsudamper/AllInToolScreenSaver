package net.matsudamper.allintoolscreensaver

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.io.PlatformTestStorageRegistry
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import net.matsudamper.allintoolscreensaver.CalendarEvent
import net.matsudamper.allintoolscreensaver.CalendarRepository
import net.matsudamper.allintoolscreensaver.SettingsRepository
import net.matsudamper.allintoolscreensaver.compose.calendar.CalendarDisplayScreen
import net.matsudamper.allintoolscreensaver.compose.calendar.CalendarDisplayScreenUiState
import net.matsudamper.allintoolscreensaver.compose.calendar.previewCalendarLayoutClock
import net.matsudamper.allintoolscreensaver.waitUntilExactlyOne
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
@Suppress("RemoveRedundantBackticks", "NonAsciiCharacters")
class CalendarDisplayScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var settingsRepository: FakeSettingsManager
    private lateinit var calendarRepository: FakeCalendarRepository

    @Before
    fun before() {
        stopKoin()
        settingsRepository = FakeSettingsManager()
        calendarRepository = FakeCalendarRepository()

        startKoin {
            modules(
                module {
                    single<SettingsRepository> { settingsRepository }
                    single<CalendarRepository> { calendarRepository }
                    single {
                        net.matsudamper.allintoolscreensaver.viewmodel.CalendarDisplayScreenViewModel(
                            application = ApplicationProvider.getApplicationContext<Application>(),
                            settingsRepository = get(),
                            calendarRepository = get(),
                        )
                    }
                },
            )
        }
    }

    @After
    fun after() {
        stopKoin()
    }

    @Test(timeout = 10_000L)
    fun `カレンダーの表示が重ならないか確認する`() = runAndCaptureScreen {
        val calendarId = 1L
        settingsRepository.setSelectedCalendarIds(listOf(calendarId))

        val now = LocalDateTime.now()
        val startTime = now.withHour(10).withMinute(0).withSecond(0).withNano(0)
        val endTime = now.withHour(11).withMinute(0).withSecond(0).withNano(0)

        val event1 = CalendarEvent.Time(
            id = 1L,
            title = "会議A",
            description = "重要な会議",
            color = Color.Red.hashCode(),
            startTime = startTime.atZone(ZoneId.systemDefault()).toInstant(),
            endTime = endTime.atZone(ZoneId.systemDefault()).toInstant(),
        )

        val event2 = CalendarEvent.Time(
            id = 2L,
            title = "会議B",
            description = "別の会議",
            color = Color.Blue.hashCode(),
            startTime = startTime.plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant(),
            endTime = endTime.plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant(),
        )

        calendarRepository.addEvent(event1)
        calendarRepository.addEvent(event2)

        val testEvents = listOf(
            net.matsudamper.allintoolscreensaver.compose.calendar.CalendarLayoutUiState.Event.Time(
                startTime = java.time.LocalTime.of(10, 0),
                endTime = java.time.LocalTime.of(11, 0),
                title = "会議A",
                displayTime = "10:00 - 11:00",
                description = "重要な会議",
                color = Color.Red,
            ),
            net.matsudamper.allintoolscreensaver.compose.calendar.CalendarLayoutUiState.Event.Time(
                startTime = java.time.LocalTime.of(10, 30),
                endTime = java.time.LocalTime.of(11, 30),
                title = "会議B",
                displayTime = "10:30 - 11:30",
                description = "別の会議",
                color = Color.Blue,
            ),
        )

        composeTestRule.setContent {
            CalendarDisplayScreen(
                uiState = CalendarDisplayScreenUiState(
                    calendarUiState = net.matsudamper.allintoolscreensaver.compose.calendar.CalendarLayoutUiState(
                        events = testEvents,
                        allDayEvents = listOf(),
                    ),
                    operationFlow = Channel(Channel.UNLIMITED),
                    listener = object : CalendarDisplayScreenUiState.Listener {
                        override suspend fun onStart() = Unit
                        override fun onInteraction() = Unit
                    },
                ),
                clock = previewCalendarLayoutClock,
                contentWindowInsets = WindowInsets(),
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.waitForIdle()

        val event1Node = composeTestRule.waitUntilExactlyOne(
            matcher = hasText("会議A"),
            timeout = 10.seconds,
        )

        val event2Node = composeTestRule.waitUntilExactlyOne(
            matcher = hasText("会議B"),
            timeout = 10.seconds,
        )

        val event1Bounds = event1Node.getBoundsInRoot()
        val event2Bounds = event2Node.getBoundsInRoot()

        val isOverlapping = !(
            event1Bounds.bottom <= event2Bounds.top ||
                event2Bounds.bottom <= event1Bounds.top ||
                event1Bounds.right <= event2Bounds.left ||
                event2Bounds.right <= event1Bounds.left
            )

        assert(!isOverlapping) {
            "イベントが重複して表示されています。会議A: $event1Bounds, 会議B: $event2Bounds"
        }
    }

    private fun runAndCaptureScreen(
        filename: String = checkNotNull(object : Any() {}.javaClass.enclosingMethod).name,
        block: suspend () -> Unit,
    ) {
        runTest {
            val result = runCatching { block() }

            composeTestRule.waitForIdle()
            captureScreen(filename)

            result.getOrThrow()
        }
    }

    private fun captureScreen(filename: String) {
        val bitmap = composeTestRule.onRoot()
            .captureToImage()
            .asAndroidBitmap()

        PlatformTestStorageRegistry.getInstance().openOutputFile("$filename.png").use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
    }
}
