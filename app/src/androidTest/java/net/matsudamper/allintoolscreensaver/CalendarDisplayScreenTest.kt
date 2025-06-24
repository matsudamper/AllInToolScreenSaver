package net.matsudamper.allintoolscreensaver

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import net.matsudamper.allintoolscreensaver.compose.CalendarDisplayScreen
import net.matsudamper.allintoolscreensaver.compose.CalendarDisplayScreenTestTag
import net.matsudamper.allintoolscreensaver.compose.CalendarDisplayScreenViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.dsl.viewModel

@RunWith(AndroidJUnit4::class)
@Suppress("TestFunctionName", "NonAsciiCharacters")
class CalendarDisplayScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var settingsRepository: FakeSettingsManager
    lateinit var calendarRepository: FakeCalendarRepository

    @Before
    fun before() {
        settingsRepository = FakeSettingsManager()
        calendarRepository = FakeCalendarRepository()
    }

    @Test(timeout = 30000)
    fun カレンダーの予定が正常に表示される() {
        val viewModel = CalendarDisplayScreenViewModel(
            settingsRepository = settingsRepository,
            calendarRepository = calendarRepository,
        )
        // TODO calendarRepository.add
        composeTestRule.setContent {
            CalendarDisplayScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // TODO
    }

    @Test(timeout = 30000)
    fun ズームボタンが正常に動作する() {
        val viewModel = CalendarDisplayScreenViewModel(
            settingsRepository = settingsRepository,
            calendarRepository = calendarRepository,
        )
        // TODO calendarRepository.add
        composeTestRule.setContent {
            CalendarDisplayScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test(timeout = 30000)
    fun スクロールボタンが正常に動作する() {
        val viewModel = CalendarDisplayScreenViewModel(
            settingsRepository = settingsRepository,
            calendarRepository = calendarRepository,
        )
        // TODO calendarRepository.add
        composeTestRule.setContent {
            CalendarDisplayScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun captureScreen(filename: String) {
        val bitmap = composeTestRule.onRoot()
            .captureToImage()
            .asAndroidBitmap()
        
        PlatformTestStorageRegistry.getInstance().openOutputFile("$filename.png").use { outputStream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
        }
    }
}
