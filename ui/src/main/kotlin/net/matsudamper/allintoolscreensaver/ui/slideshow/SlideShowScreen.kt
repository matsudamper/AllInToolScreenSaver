package net.matsudamper.allintoolscreensaver.ui.slideshow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import net.matsudamper.allintoolscreensaver.ui.component.SuspendLifecycleStartEffect

@Composable
fun SlideShowScreen(
    uiState: SlideshowUiState,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    SuspendLifecycleStartEffect(uiState.listener) {
        uiState.listener.onStart()
    }
    if (uiState.pagerItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
            )
        }
    } else {
        val pagerState = rememberPagerState(
            initialPage = 1,
            pageCount = { 3 },
        )
        val coroutineScope = rememberCoroutineScope()
        val listener by rememberUpdatedState(uiState.listener)

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
                    listener.onPageChanged(pagerState.targetPage)
                }
            }
        }
        LaunchedEffect(
            uiState.imageSwitchIntervalSeconds,
            // スクロールしたらintervalをリセットする為にcurrentPageを設定
            pagerState.currentPage,
        ) {
            while (isActive) {
                delay(uiState.imageSwitchIntervalSeconds.seconds)
                val currentPage = pagerState.currentPage
                if (currentPage == 1) {
                    listener.onPageChanged(2)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(2)
                    }
                } else {
                    listener.onPageChanged(currentPage)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            key = { index -> uiState.pagerItems[index].id },
        ) { page ->
            val item = uiState.pagerItems[page]

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
}
