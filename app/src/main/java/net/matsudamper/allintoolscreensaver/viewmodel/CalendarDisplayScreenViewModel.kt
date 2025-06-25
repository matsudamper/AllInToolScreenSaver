package net.matsudamper.allintoolscreensaver.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.CalendarEvent
import net.matsudamper.allintoolscreensaver.CalendarRepository
import net.matsudamper.allintoolscreensaver.SettingsRepository
import net.matsudamper.allintoolscreensaver.compose.calendar.CalendarDisplayScreenUiState
import net.matsudamper.allintoolscreensaver.compose.calendar.CalendarLayoutUiState

class CalendarDisplayScreenViewModel(
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val listener = object : CalendarDisplayScreenUiState.Listener {
        override suspend fun onStart() {
            loadEvents()
        }

        override fun onInteraction() {
            viewModelStateFlow.update { state ->
                state.copy(lastInteractionTime = Instant.now())
            }
        }
    }

    val uiState: StateFlow<CalendarDisplayScreenUiState> = MutableStateFlow(
        CalendarDisplayScreenUiState(
            calendarUiState = CalendarLayoutUiState(
                events = listOf(),
                allDayEvents = listOf(),
            ),
            isLoading = true,
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        calendarUiState = CalendarLayoutUiState(
                            events = viewModelState.events.filterIsInstance<CalendarEvent.Time>()
                                .map { event ->
                                    CalendarLayoutUiState.Event.Time(
                                        startTime = LocalTime.ofInstant(event.startTime, ZoneId.systemDefault()),
                                        endTime = LocalTime.ofInstant(event.endTime, ZoneId.systemDefault()),
                                        title = event.title,
                                        description = event.description,
                                        color = Color(event.color),
                                    )
                                },
                            allDayEvents = viewModelState.events.filterIsInstance<CalendarEvent.AllDay>()
                                .map { event ->
                                    CalendarLayoutUiState.Event.AllDay(
                                        title = event.title,
                                        description = event.description,
                                        color = Color(event.color),
                                    )
                                },
                        ),
                        isLoading = viewModelState.isLoading,
                    )
                }
            }
        }
    }.asStateFlow()

    private suspend fun loadEvents() {
        viewModelStateFlow.update { state ->
            state.copy(isLoading = true)
        }

        val selectedCalendarIds = settingsRepository.getSelectedCalendarIds()

        if (selectedCalendarIds.isNotEmpty()) {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val events = calendarRepository.getEventsForTimeRange(selectedCalendarIds, startOfDay, endOfDay)
            viewModelStateFlow.update { state ->
                state.copy(
                    events = events,
                    isLoading = false,
                )
            }
        } else {
            viewModelStateFlow.update { state ->
                state.copy(isLoading = false)
            }
        }
    }

    private data class ViewModelState(
        val events: List<CalendarEvent> = listOf(),
        val scale: Float = 1f,
        val isLoading: Boolean = true,
        val lastInteractionTime: Instant = Instant.now(),
    )
}
