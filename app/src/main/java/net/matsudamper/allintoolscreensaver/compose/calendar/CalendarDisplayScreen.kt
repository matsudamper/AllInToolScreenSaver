package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import java.time.Clock
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.ui.TimeRangeSlider
import net.matsudamper.allintoolscreensaver.ui.TimeRangeSliderItem
import net.matsudamper.allintoolscreensaver.ui.rememberCalendarState
import net.matsudamper.allintoolscreensaver.ui.rememberTimeRangeSlider
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarDisplayScreenViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CalendarDisplayScreen(
    contentWindowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    viewModel: CalendarDisplayScreenViewModel = koinViewModel(),
    clock: Clock = remember { Clock.systemDefaultZone() },
) {
    val uiState by viewModel.uiState.collectAsState()
    CalendarDisplayScreen(
        modifier = modifier,
        uiState = uiState,
        clock = clock,
        contentWindowInsets = contentWindowInsets,
    )
}

@Composable
fun CalendarDisplayScreen(
    uiState: CalendarDisplayScreenUiState,
    clock: Clock,
    contentWindowInsets: WindowInsets,
    modifier: Modifier = Modifier,
) {
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
        contentWindowInsets = contentWindowInsets,
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                CalendarLayout(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState.calendarUiState,
                    state = calendarState,
                    clock = clock,
                )
                val interactionSource = remember { MutableInteractionSource() }
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp,
                    ),
                    onClick = {
                        uiState.listener.onAlertEnabledChanged(!uiState.alertEnabled)
                    },
                    interactionSource = interactionSource,
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = if (uiState.alertEnabled) "アラート有効" else "アラート無効",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (uiState.alertEnabled) 1.0f else 0.5f),
                            modifier = Modifier.size(16.dp),
                        )
                        Switch(
                            checked = uiState.alertEnabled,
                            interactionSource = interactionSource,
                            onCheckedChange = { enabled ->
                                uiState.listener.onAlertEnabledChanged(enabled)
                            },
                        )
                    }
                }
            }
            val sliderState = rememberTimeRangeSlider(calendarState = calendarState)
            LaunchedEffect(sliderState) {
                sliderState.sliderStateChanged
                    .filter { it.isUserInteraction }
                    .collectLatest {
                        uiState.listener.onInteraction()
                    }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                TimeRangeSlider(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    state = sliderState,
                    items = remember(uiState.calendarUiState.events) {
                        uiState.calendarUiState.events.map {
                            TimeRangeSliderItem(
                                startTime = it.startTime,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
@Preview
private fun Preview() {
    CalendarDisplayScreen(
        uiState = CalendarDisplayScreenUiState(
            calendarUiState = previewCalendarLayoutUiState,
            alertEnabled = false,
            operationFlow = remember { Channel(Channel.UNLIMITED) },
            listener = object : CalendarDisplayScreenUiState.Listener {
                override suspend fun onStart() = Unit
                override fun onInteraction() = Unit
                override fun onAlertEnabledChanged(enabled: Boolean) = Unit
            },
        ),
        clock = previewCalendarLayoutClock,
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
    )
}
