package net.matsudamper.allintoolscreensaver.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun SlideShowScreen(
    uiState: SlideShowUiState,
    onPageChange: (Int) -> Unit,
    @Suppress("ParameterNaming") onPageChanged: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {

    if (uiState.imageItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
            )
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { 3 },
    )
    val coroutineScope = rememberCoroutineScope()
    val latestOnPageChange by rememberUpdatedState(onPageChange)
    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.currentPage to pagerState.isScrollInProgress
        }.collectLatest { (currentPage, isScrolling) ->
            if (isScrolling) return@collectLatest

            if (currentPage != 1) {
                latestOnPageChange(currentPage)
            }
        }
    }
    val latestOnPageChanged by rememberUpdatedState(onPageChanged)
    LaunchedEffect(pagerState) {
        snapshotFlow {
            listOf(
                pagerState.currentPage,
                pagerState.isScrollInProgress,
                pagerState.targetPage,
            )
        }.collectLatest {
            if (pagerState.isScrollInProgress) return@collectLatest

            if (pagerState.targetPage == pagerState.currentPage) {
                latestOnPageChanged()
            }
        }
    }
    LaunchedEffect(
        uiState.intervalSeconds,
        // スクロールしたらintervalをリセットする為にcurrentPageを設定
        pagerState.currentPage,
    ) {
        while (isActive) {
            delay(uiState.intervalSeconds.seconds)
            val currentPage = pagerState.currentPage
            if (currentPage == 1) {
                latestOnPageChange(2)
                coroutineScope.launch {
                    pagerState.animateScrollToPage(2)
                }
            } else {
                latestOnPageChange(currentPage)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
            .hazeSource(hazeState),
        key = { index -> uiState.imageItems[index].id },
    ) { page ->
        val item = uiState.imageItems[page]

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                item.imageUri == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                    )
                }

                else -> {
                    AsyncImage(
                        model = item.imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SlideShowContentPreview() {
    SlideShowScreen(
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
        hazeState = remember { HazeState() },
    )
}
