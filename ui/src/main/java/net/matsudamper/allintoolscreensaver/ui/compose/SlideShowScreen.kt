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

data class PagerItem(
    val id: String,
    val imageUri: String?,
)

@Composable
fun SlideShowScreen(
    pagerItems: List<PagerItem>,
    onPageChange: (Int) -> Unit,
    @Suppress("ParameterNaming") onPageChanged: () -> Unit,
    imageSwitchIntervalSeconds: Int?,
    modifier: Modifier = Modifier,
) {
    if (pagerItems.isEmpty()) {
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
        imageSwitchIntervalSeconds,
        // スクロールしたらintervalをリセットする為にcurrentPageを設定
        pagerState.currentPage,
    ) {
        if (imageSwitchIntervalSeconds == null) return@LaunchedEffect

        while (isActive) {
            delay(imageSwitchIntervalSeconds.seconds)
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
        modifier = modifier.fillMaxSize(),
        key = { index -> pagerItems[index].id },
    ) { page ->
        val item = pagerItems[page]

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
