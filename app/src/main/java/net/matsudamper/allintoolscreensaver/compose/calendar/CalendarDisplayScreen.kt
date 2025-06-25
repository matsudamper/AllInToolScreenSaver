package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarDisplayScreenViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CalendarDisplayScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarDisplayScreenViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        uiState.listener.onStart()
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        val calendarState = rememberCalendarState()
        CalendarLayout(
            uiState = uiState.calendarUiState,
            state = calendarState,
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        ) {
            ZoomControls(
                onZoomIn = {
                    calendarState.zoomIn()
                    uiState.listener.onInteraction()
                },
                onZoomOut = {
                    calendarState.zoomOut()
                    uiState.listener.onInteraction()
                },
                modifier = Modifier.align(Alignment.Bottom),
            )
            Spacer(modifier = Modifier.width(18.dp))
            ScrollControls(
                onScrollUp = {
                    uiState.listener.onInteraction()
                    coroutineScope.launch {
                        calendarState.addAnimateScrollToHours(-3)
                    }
                },
                onScrollDown = {
                    uiState.listener.onInteraction()
                    coroutineScope.launch {
                        calendarState.addAnimateScrollToHours(3)
                    }
                },
            )
        }
    }
}

@Composable
private fun ZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FloatingActionButton(
            modifier = Modifier
                .testTag(CalendarDisplayScreenTestTag.ZoomOutButton.testTag()),
            onClick = onZoomOut,
        ) {
            Text(
                text = "－",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        FloatingActionButton(
            modifier = Modifier
                .testTag(CalendarDisplayScreenTestTag.ZoomInButton.testTag()),
            onClick = onZoomIn,
        ) {
            Text(
                text = "＋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
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
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FloatingActionButton(
            modifier = Modifier
                .testTag(CalendarDisplayScreenTestTag.ScrollUpButton.testTag()),
            onClick = onScrollUp,
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上にスクロール")
        }

        FloatingActionButton(
            modifier = Modifier
                .testTag(CalendarDisplayScreenTestTag.ScrollDownButton.testTag()),
            onClick = onScrollDown,
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下にスクロール")
        }
    }
}
