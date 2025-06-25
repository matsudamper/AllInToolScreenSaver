package net.matsudamper.allintoolscreensaver.compose

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
        initialPage = 1,
        pageCount = { 3 },
    )
    var lastProcessedPage by remember { mutableIntStateOf(1) }
    val currentOnPageChange by rememberUpdatedState(onPageChange)

    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.currentPage to pagerState.isScrollInProgress
        }.collectLatest { (currentPage, isScrolling) ->
            if (!isScrolling && currentPage != lastProcessedPage) {
                currentOnPageChange(currentPage)
                lastProcessedPage = currentPage

                if (currentPage == 0 || currentPage == 2) {
                    pagerState.scrollToPage(1)
                }
            }
        }
    }

    LaunchedEffect(pagerItems) {
        if (pagerState.currentPage != 1) {
            pagerState.animateScrollToPage(1)
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
                    )
                }
            }
        }
    }
}
