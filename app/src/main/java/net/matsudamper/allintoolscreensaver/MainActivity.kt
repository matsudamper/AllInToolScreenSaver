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
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import net.matsudamper.allintoolscreensaver.compose.CalendarSelectionScreen
import net.matsudamper.allintoolscreensaver.compose.MainScreen
import net.matsudamper.allintoolscreensaver.compose.calendar.CalendarDisplayScreen
import net.matsudamper.allintoolscreensaver.navigation.CustomTwoPaneSceneStrategy
import net.matsudamper.allintoolscreensaver.theme.AllInToolScreenSaverTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AllInToolScreenSaverTheme {
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
                        MainScreen(
                            backStack = backStack,
                        )
                    }
                    entry<NavKeys.CalendarSelection>(
                        metadata = CustomTwoPaneSceneStrategy.extendPane(),
                    ) {
                        CalendarSelectionScreen(
                            backStack = backStack,
                        )
                    }
                    entry<NavKeys.CalendarDisplay>(
                        metadata = CustomTwoPaneSceneStrategy.extendPane(),
                    ) { _ ->
                        CalendarDisplayScreen(
                            contentWindowInsets = WindowInsets.systemBars,
                        )
                    }
                },
            )
        }
    }
}
