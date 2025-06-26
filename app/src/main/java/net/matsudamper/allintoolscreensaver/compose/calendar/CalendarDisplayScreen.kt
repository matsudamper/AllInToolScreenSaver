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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import java.time.Clock
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.compose.component.DreamDialog
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarDisplayScreenViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CalendarDisplayScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarDisplayScreenViewModel = koinViewModel(),
    clock: Clock = remember { Clock.systemDefaultZone() },
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val calendarState = rememberCalendarState()

    LifecycleStartEffect(Unit) {
        val scope = CoroutineScope(Job())
        scope.launch { uiState.listener.onStart() }
        onStopOrDispose { scope.cancel() }
    }

    LaunchedEffect(Unit) {
        uiState.operationFlow.receiveAsFlow().collect {
            it(
                object : CalendarDisplayScreenUiState.Operation {
                    override fun moveCurrentTime() {
                        if (calendarState.isCurrentTimeDisplayed().not()) {
                            coroutineScope.launch {
                                val now = LocalTime.now(clock)
                                calendarState.animateScrollToHours(
                                    (now.hour - 1).coerceAtLeast(0),
                                )
                            }
                        }
                    }
                },
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
    ) { paddingValues ->
        Box {
            CalendarLayout(
                modifier = Modifier.padding(paddingValues),
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

    val currentAlert = uiState.currentAlert
    if (currentAlert != null) {
        DreamDialog(
            dismissRequest = {
                uiState.listener.onAlertDismiss()
            },
        ) {
            AlertContent(
                eventTitle = currentAlert.title,
                eventDescription = currentAlert.description,
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

@Composable
private fun AlertContent(
    eventTitle: String,
    eventDescription: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "予定の時間です",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.padding(16.dp))
        Text(
            text = eventTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        if (eventDescription.isNullOrBlank().not()) {
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = eventDescription.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
