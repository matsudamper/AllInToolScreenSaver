package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarDisplayScreenViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CalendarDisplayScreen(
    viewModel: CalendarDisplayScreenViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {

    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        uiState.listener.onStart()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, _, zoom, _ ->
                        uiState.listener.onInteraction()
                        if (zoom > 1f) {
                            uiState.listener.onZoomIn()
                        } else if (zoom < 1f) {
                            uiState.listener.onZoomOut()
                        }
                    },
                )
            },
    ) {
        CalendarLayout(
            uiState = uiState,
            listState = listState,
        )

        ZoomControls(
            onZoomIn = { uiState.listener.onZoomIn() },
            onZoomOut = { uiState.listener.onZoomOut() },
            modifier = Modifier.align(Alignment.BottomStart),
        )

        ScrollControls(
            onScrollUp = {
                uiState.listener.onInteraction()
                coroutineScope.launch {
                    val currentIndex = listState.firstVisibleItemIndex
                    val targetIndex = max(0, currentIndex - 60)
                    listState.animateScrollToItem(targetIndex)
                }
            },
            onScrollDown = {
                uiState.listener.onInteraction()
                coroutineScope.launch {
                    val currentIndex = listState.firstVisibleItemIndex
                    val targetIndex = min(uiState.timeSlots.size - 1, currentIndex + 60)
                    listState.animateScrollToItem(targetIndex)
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun ZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FloatingActionButton(
            onClick = onZoomIn,
            modifier = Modifier
                .size(40.dp)
                .testTag(CalendarDisplayScreenTestTag.ZoomInButton.testTag()),
            containerColor = Color.Black.copy(alpha = 0.7f),
            contentColor = Color.White,
        ) {
            Text("＋")
        }

        FloatingActionButton(
            onClick = onZoomOut,
            modifier = Modifier
                .size(40.dp)
                .testTag(CalendarDisplayScreenTestTag.ZoomOutButton.testTag()),
            containerColor = Color.Black.copy(alpha = 0.7f),
            contentColor = Color.White,
        ) {
            Text("－")
        }
    }
}

@Composable
private fun ScrollControls(
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FloatingActionButton(
            onClick = onScrollUp,
            modifier = Modifier
                .size(40.dp)
                .testTag(CalendarDisplayScreenTestTag.ScrollUpButton.testTag()),
            containerColor = Color.Black.copy(alpha = 0.7f),
            contentColor = Color.White,
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上にスクロール")
        }

        FloatingActionButton(
            onClick = onScrollDown,
            modifier = Modifier
                .size(40.dp)
                .testTag(CalendarDisplayScreenTestTag.ScrollDownButton.testTag()),
            containerColor = Color.Black.copy(alpha = 0.7f),
            contentColor = Color.White,
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下にスクロール")
        }
    }
}

