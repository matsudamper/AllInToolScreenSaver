package net.matsudamper.allintoolscreensaver.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.CalendarEvent
import net.matsudamper.allintoolscreensaver.CalendarRepository
import net.matsudamper.allintoolscreensaver.SettingsRepository

class CalendarDisplayScreenViewModel(
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val listener = object : CalendarDisplayScreenUiState.Listener {
        override suspend fun onStart() {
            loadEvents()
        }

        override fun onZoomIn() {
            viewModelStateFlow.update { state ->
                state.copy(
                    scale = min(3f, state.scale * 1.2f),
                    lastInteractionTime = Instant.now(),
                )
            }
        }

        override fun onZoomOut() {
            viewModelStateFlow.update { state ->
                state.copy(
                    scale = max(0.5f, state.scale / 1.2f),
                    lastInteractionTime = Instant.now(),
                )
            }
        }

        override fun onInteraction() {
            viewModelStateFlow.update { state ->
                state.copy(lastInteractionTime = Instant.now())
            }
        }
    }

    val uiState: StateFlow<CalendarDisplayScreenUiState> = MutableStateFlow(
        CalendarDisplayScreenUiState(
            events = listOf(),
            timeSlots = generateMinuteTimeSlots(),
            currentTime = Instant.now(),
            scale = 1f,
            isLoading = true,
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            while (isActive) {
                uiStateFlow.update { uiState ->
                    uiState.copy(currentTime = Instant.now())
                }
                delay(60000)
            }
        }

        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        events = viewModelState.events,
                        scale = viewModelState.scale,
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
            val (startTime, endTime) = calendarRepository.getTodayRange()
            val events = calendarRepository.getEventsForTimeRange(selectedCalendarIds, startTime, endTime)

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

    private fun generateMinuteTimeSlots(): List<CalendarDisplayScreenUiState.TimeSlot> {
        val timeSlots = mutableListOf<CalendarDisplayScreenUiState.TimeSlot>()
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()

        for (totalMinutes in 0 until 1440) {
            val slotStart = startOfDay.plusSeconds(totalMinutes * 60L)
            val slotEnd = slotStart.plusSeconds(60)

            val hour = totalMinutes / 60
            val minute = totalMinutes % 60
            val hourText = String.format(Locale.US, "%02d:%02d", hour, minute)

            timeSlots.add(CalendarDisplayScreenUiState.TimeSlot(slotStart, slotEnd, hourText))
        }

        return timeSlots
    }

    private data class ViewModelState(
        val events: List<CalendarEvent> = listOf(),
        val scale: Float = 1f,
        val isLoading: Boolean = true,
        val lastInteractionTime: Instant = Instant.now(),
    )
}
