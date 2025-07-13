package net.matsudamper.allintoolscreensaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import net.matsudamper.allintoolscreensaver.adapter.CalendarDisplayScreenAdapter
import net.matsudamper.allintoolscreensaver.adapter.CalendarSelectionScreenAdapter
import net.matsudamper.allintoolscreensaver.adapter.MainScreenAdapter
import net.matsudamper.allintoolscreensaver.adapter.NotificationAdapter
import net.matsudamper.allintoolscreensaver.adapter.SlideShowScreenAdapter
import net.matsudamper.allintoolscreensaver.navigation.CustomTwoPaneSceneStrategy
import net.matsudamper.allintoolscreensaver.navigation.NavKeys
import net.matsudamper.allintoolscreensaver.ui.calendar.CalendarSelectionMode
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
                        CalendarSelectionScreenAdapter(
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
                        CalendarSelectionScreenAdapter(
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
                        CalendarDisplayScreenAdapter(
                            contentWindowInsets = WindowInsets.systemBars,
                        )
                    }
                    entry<NavKeys.SlideShowPreview>(
                        metadata = CustomTwoPaneSceneStrategy.extendPane(),
                    ) { _ ->
                        SlideShowScreenAdapter()
                    }
                    entry<NavKeys.NotificationPreview>(
                        metadata = CustomTwoPaneSceneStrategy.extendPane(),
                    ) { _ ->
                        val hazeState = rememberHazeState()
                        NotificationAdapter(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing
                                        .only(
                                            WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Right,
                                        ),
                                )
                                .background(Color.Gray)
                                .hazeSource(hazeState),
                            hazeState = hazeState,
                        )
                    }
                },
            )
        }
    }
}
