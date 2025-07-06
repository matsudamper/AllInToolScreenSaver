package net.matsudamper.allintoolscreensaver.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
internal fun SlideShowContent(
    uiState: SlideShowUiState,
    onPageChange: (Int) -> Unit,
    onPageChanged: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val graphicsLayer = rememberGraphicsLayer()

    val pagerItems = uiState.imageItems.map { item ->
        PagerItem(
            id = item.id,
            imageUri = item.imageUri,
        )
    }

    SlideShowScreen(
        pagerItems = pagerItems,
        onPageChange = onPageChange,
        onPageChanged = onPageChanged,
        imageSwitchIntervalSeconds = uiState.intervalSeconds,
        modifier = modifier
            .hazeSource(hazeState)
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            },
    )
}
