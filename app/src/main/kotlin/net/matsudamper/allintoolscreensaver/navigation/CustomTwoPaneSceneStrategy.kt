package net.matsudamper.allintoolscreensaver.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.Scene
import androidx.navigation3.ui.SceneStrategy
import androidx.window.core.layout.WindowSizeClass

class CustomTwoPaneSceneStrategy<T : Any> : SceneStrategy<T> {
    @Composable
    override fun calculateScene(entries: List<NavEntry<T>>, onBack: (Int) -> Unit): Scene<T>? {
        if (entries.isEmpty()) {
            return null
        }
        val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

        val isExtended = remember(windowSizeClass) {
            windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
        }
        return if (isExtended) {
            val extendPane = entries.lastOrNull { it.metadata.containsKey(KEY_EXTEND_PANE) }
            val previousEntries = entries.filterNot { it.metadata.containsKey(KEY_EXTEND_PANE) }
            val mainPane = previousEntries.last()

            TwoPaneScene(
                key = listOf(mainPane.contentKey, extendPane?.contentKey),
                previousEntries = previousEntries.dropLast(1),
                firstEntry = mainPane,
                secondEntry = extendPane,
            )
        } else {
            null
        }
    }

    companion object {
        const val KEY_EXTEND_PANE = "extend_pane"

        fun extendPane() = mapOf(KEY_EXTEND_PANE to true)
    }
}

private class TwoPaneScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    private val firstEntry: NavEntry<T>,
    private val secondEntry: NavEntry<T>?,
) : Scene<T> {
    override val entries: List<NavEntry<T>>
        get() = listOfNotNull(firstEntry, secondEntry)

    override val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                firstEntry.Content()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                secondEntry?.Content()
            }
        }
    }
}
