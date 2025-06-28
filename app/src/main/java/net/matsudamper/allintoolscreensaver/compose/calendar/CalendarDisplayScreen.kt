package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
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
import net.matsudamper.allintoolscreensaver.TimeRangeSlider
import net.matsudamper.allintoolscreensaver.TimeRangeSliderItem
import net.matsudamper.allintoolscreensaver.compose.component.DreamDialog
import net.matsudamper.allintoolscreensaver.rememberTimeRangeSlider
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarDisplayScreenViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CalendarDisplayScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarDisplayScreenViewModel = koinViewModel(),
    clock: Clock = remember { Clock.systemDefaultZone() },
) {
    val uiState by viewModel.uiState.collectAsState()
    CalendarDisplayScreen(
        modifier = modifier,
        uiState = uiState,
        clock = clock,
    )
}

@Composable
fun CalendarDisplayScreen(
    uiState: CalendarDisplayScreenUiState,
    clock: Clock,
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
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues),
        ) {
            CalendarLayout(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                uiState = uiState.calendarUiState,
                state = calendarState,
                clock = clock,
            )
            val sliderState = rememberTimeRangeSlider(calendarState = calendarState)
            LaunchedEffect(sliderState) {
                sliderState.sliderStateChanged
                    .filter { it.isUserInteraction }
                    .collectLatest {
                        uiState.listener.onInteraction()
                    }
            }
            TimeRangeSlider(
                modifier = Modifier
                    .fillMaxWidth()
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
                text = eventDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
@Preview
private fun Preview() {
    CalendarDisplayScreen(
        uiState = CalendarDisplayScreenUiState(
            calendarUiState = previewCalendarLayoutUiState,
            operationFlow = remember { Channel(Channel.UNLIMITED) },
            listener = object : CalendarDisplayScreenUiState.Listener {
                override suspend fun onStart() = Unit
                override fun onInteraction() = Unit
                override fun onAlertDismiss() = Unit
            },
            currentAlert = null,
        ),
        clock = previewCalendarLayoutClock,
        modifier = Modifier.fillMaxSize(),
    )
}
