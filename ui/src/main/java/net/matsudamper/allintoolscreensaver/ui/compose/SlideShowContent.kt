package net.matsudamper.allintoolscreensaver.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.tooling.preview.Preview
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

@Preview
@Composable
private fun SlideShowContentPreview() {
    SlideShowContent(
        uiState = SlideShowUiState(
            imageItems = listOf(
                SlideShowImageItem(
                    id = "1",
                    imageUri = null,
                ),
                SlideShowImageItem(
                    id = "2",
                    imageUri = "https://picsum.photos/800/600?random=1",
                ),
                SlideShowImageItem(
                    id = "3",
                    imageUri = "https://picsum.photos/800/600?random=2",
                ),
            ),
            currentPageIndex = 0,
            intervalSeconds = 5,
        ),
        onPageChange = {},
        onPageChanged = {},
        hazeState = HazeState(),
    )
}
