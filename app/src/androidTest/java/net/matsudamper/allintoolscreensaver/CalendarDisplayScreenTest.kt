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
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import net.matsudamper.allintoolscreensaver.feature.calendar.CalendarRepository
import net.matsudamper.allintoolscreensaver.feature.setting.SettingsRepository
import net.matsudamper.allintoolscreensaver.AttendeeStatus
import net.matsudamper.allintoolscreensaver.ui.calendar.CalendarDisplayScreen
import net.matsudamper.allintoolscreensaver.ui.calendar.CalendarDisplayScreenUiState
import net.matsudamper.allintoolscreensaver.ui.calendar.CalendarLayoutUiState
import net.matsudamper.allintoolscreensaver.ui.calendar.previewCalendarLayoutClock
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
                            clock = Clock.systemDefaultZone(),
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

        val event1 = CalendarRepository.CalendarEvent.Time(
            id = 1L,
            title = "会議A",
            description = "重要な会議",
            color = Color.Red.hashCode(),
            startTime = startTime.atZone(ZoneId.systemDefault()).toInstant(),
            endTime = endTime.atZone(ZoneId.systemDefault()).toInstant(),
            attendeeStatus = AttendeeStatus.ACCEPTED,
        )

        val event2 = CalendarRepository.CalendarEvent.Time(
            id = 2L,
            title = "会議B",
            description = "別の会議",
            color = Color.Blue.hashCode(),
            startTime = startTime.plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant(),
            endTime = endTime.plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant(),
            attendeeStatus = AttendeeStatus.ACCEPTED,
        )

        calendarRepository.addEvent(event1)
        calendarRepository.addEvent(event2)

        val testEvents = listOf(
            CalendarLayoutUiState.Event.Time(
                startTime = java.time.LocalTime.of(10, 0),
                endTime = java.time.LocalTime.of(11, 0),
                title = "会議A",
                displayTime = "10:00 - 11:00",
                description = "重要な会議",
                color = Color.Red,
                isTransparent = false,
                showBorder = false,
                hasTextDecoration = false,
            ),
            CalendarLayoutUiState.Event.Time(
                startTime = java.time.LocalTime.of(10, 30),
                endTime = java.time.LocalTime.of(11, 30),
                title = "会議B",
                displayTime = "10:30 - 11:30",
                description = "別の会議",
                color = Color.Blue,
                isTransparent = false,
                showBorder = false,
                hasTextDecoration = false,
            ),
        )

        composeTestRule.setContent {
            CalendarDisplayScreen(
                uiState = CalendarDisplayScreenUiState(
                    calendarUiState = CalendarLayoutUiState(
                        events = testEvents,
                        allDayEvents = listOf(),
                    ),
                    alertEnabled = false,
                    operationFlow = Channel(Channel.UNLIMITED),
                    listener = object : CalendarDisplayScreenUiState.Listener {
                        override suspend fun onStart() = Unit
                        override fun onInteraction() = Unit
                        override fun onAlertEnabledChanged(enabled: Boolean) = Unit
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
