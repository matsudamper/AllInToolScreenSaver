package net.matsudamper.allintoolscreensaver.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import coil.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlideShowScreen(
    pagerItems: List<PagerItem>,
    onPageChange: (Int) -> Unit,
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
        initialPage = 1, // 中央から開始
        pageCount = { 3 },
    )
    var lastProcessedPage by remember { mutableIntStateOf(1) }
    val currentOnPageChange by rememberUpdatedState(onPageChange)

    // スクロール完了を監視してページ変更を処理
    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.currentPage to pagerState.isScrollInProgress
        }.collectLatest { (currentPage, isScrolling) ->
            // スクロールが完了し、ページが変更された場合のみ処理
            if (!isScrolling && currentPage != lastProcessedPage) {
                currentOnPageChange(currentPage)
                lastProcessedPage = currentPage

                // 左端または右端にスワイプした場合、中央にリセット
                if (currentPage == 0 || currentPage == 2) {
                    pagerState.animateScrollToPage(1)
                }
            }
        }
    }

    // pagerItemsが変更された際にページを中央にリセット
    LaunchedEffect(pagerItems) {
        if (pagerState.currentPage != 1) {
            pagerState.scrollToPage(1)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        key = { index -> pagerItems.getOrNull(index)?.id ?: "empty_$index" },
    ) { page ->
        val item = pagerItems.getOrNull(page)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                item?.imageUri == null -> {
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
                    )
                }
            }
        }
    }
}
