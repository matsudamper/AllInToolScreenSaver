package net.matsudamper.allintoolscreensaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import net.matsudamper.allintoolscreensaver.compose.CalendarSelectionMode
import net.matsudamper.allintoolscreensaver.compose.CalendarSelectionScreen
import net.matsudamper.allintoolscreensaver.compose.MainScreenAdapter
import net.matsudamper.allintoolscreensaver.compose.ScreenSaverScreenAdapter
import net.matsudamper.allintoolscreensaver.compose.calendar.CalendarDisplayScreen
import net.matsudamper.allintoolscreensaver.navigation.CustomTwoPaneSceneStrategy
import net.matsudamper.allintoolscreensaver.ui.compose.ScreenSaverScreen
import net.matsudamper.allintoolscreensaver.ui.compose.SlideShowScreen
import net.matsudamper.allintoolscreensaver.ui.theme.AllInToolScreenSaverTheme
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarSelectionScreenViewModel
import net.matsudamper.allintoolscreensaver.viewmodel.MainScreenViewModel
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AllInToolScreenSaverTheme(
                clock = GlobalContext.get().get(),
            ) {
                AppNavigation()
            }
        }
    }

    @Composable
    private fun AppNavigation() {
        val backStack = rememberNavBackStack(NavKeys.Main)
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                onBack = { count ->
                    repeat(count) {
                        if (backStack.isNotEmpty()) {
                            backStack.removeLastOrNull()
                        }
                    }
                },
                sceneStrategy = CustomTwoPaneSceneStrategy(),
                entryProvider = entryProvider {
                    entry<NavKeys.Main> {
                        val viewModel = viewModel {
                            val koin = GlobalContext.get()
                            MainScreenViewModel(
                                settingsRepository = koin.get(),
                                inMemoryCache = koin.get(),
                            )
                        }
                        MainScreenAdapter(
                            backStack = backStack,
                            viewModel = viewModel,
                        )
                    }
                    entry<NavKeys.CalendarSelection>(
                        metadata = CustomTwoPaneSceneStrategy.extendPane(),
                    ) {
                        CalendarSelectionScreen(
                            backStack = backStack,
                            viewModel = viewModel(key = NavKeys.CalendarSelection::class.java.name) {
                                val koin = GlobalContext.get()
                                CalendarSelectionScreenViewModel(
                                    calendarRepository = koin.get(),
                                    settingsRepository = koin.get(),
                                    selectionMode = CalendarSelectionMode.DISPLAY,
                                )
                            },
                        )
                    }
                    entry<NavKeys.AlertCalendarSelection>(
                        metadata = CustomTwoPaneSceneStrategy.extendPane(),
                    ) {
                        CalendarSelectionScreen(
                            backStack = backStack,
                            viewModel = viewModel(key = NavKeys.AlertCalendarSelection::class.java.name) {
                                val koin = GlobalContext.get()
                                CalendarSelectionScreenViewModel(
                                    calendarRepository = koin.get(),
                                    settingsRepository = koin.get(),
                                    selectionMode = CalendarSelectionMode.ALERT,
                                )
                            },
                        )
                    }
                    entry<NavKeys.CalendarDisplay>(
                        metadata = CustomTwoPaneSceneStrategy.extendPane(),
                    ) { _ ->
                        CalendarDisplayScreen(
                            contentWindowInsets = WindowInsets.systemBars,
                        )
                    }
                    entry<NavKeys.SlideShowPreview>(
                        metadata = CustomTwoPaneSceneStrategy.extendPane(),
                    ) { _ ->
                        ScreenSaverScreenAdapter()
                    }
                },
            )
        }
    }
}
