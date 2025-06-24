package net.matsudamper.allintoolscreensaver.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Calendar
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
import net.matsudamper.allintoolscreensaver.CalendarManager
import net.matsudamper.allintoolscreensaver.SettingsManager

class CalendarDisplayScreenViewModel(
    private val settingsManager: SettingsManager,
    private val calendarManager: CalendarManager,
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
                    lastInteractionTime = System.currentTimeMillis(),
                )
            }
        }

        override fun onZoomOut() {
            viewModelStateFlow.update { state ->
                state.copy(
                    scale = max(0.5f, state.scale / 1.2f),
                    lastInteractionTime = System.currentTimeMillis(),
                )
            }
        }

        override fun onInteraction() {
            viewModelStateFlow.update { state ->
                state.copy(lastInteractionTime = System.currentTimeMillis())
            }
        }
    }

    val uiState: StateFlow<CalendarDisplayScreenUiState> = MutableStateFlow(
        CalendarDisplayScreenUiState(
            events = listOf(),
            timeSlots = generateMinuteTimeSlots(),
            currentTime = System.currentTimeMillis(),
            scale = 1f,
            isLoading = true,
            listener = listener,
        ),
    ).also { uiStateFlow ->
        // 現在時刻を1分ごとに更新
        viewModelScope.launch {
            while (isActive) {
                uiStateFlow.update { uiState ->
                    uiState.copy(currentTime = System.currentTimeMillis())
                }
                delay(60000) // 1分ごとに更新
            }
        }

        // ViewModelStateの変更をUiStateに反映
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

        val selectedCalendarIds = settingsManager.getSelectedCalendarIds()

        if (selectedCalendarIds.isNotEmpty()) {
            val (startTime, endTime) = calendarManager.getTodayRange()
            val events = calendarManager.getEventsForTimeRange(selectedCalendarIds, startTime, endTime)

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

    private fun generateMinuteTimeSlots(): List<TimeSlot> {
        val timeSlots = mutableListOf<TimeSlot>()
        val calendar = Calendar.getInstance()

        // 今日の0時から開始
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 24時間 × 60分 = 1440分
        for (totalMinutes in 0 until 1440) {
            val startTime = calendar.timeInMillis
            calendar.add(Calendar.MINUTE, 1)
            val endTime = calendar.timeInMillis

            val hour = totalMinutes / 60
            val minute = totalMinutes % 60
            val hourText = String.format(Locale.US, "%02d:%02d", hour, minute)

            timeSlots.add(TimeSlot(startTime, endTime, hourText))

            // 1分戻す（次のループで正しく進むため）
            calendar.add(Calendar.MINUTE, -1)
            calendar.add(Calendar.MINUTE, 1)
        }

        return timeSlots
    }

    private data class ViewModelState(
        val events: List<CalendarEvent> = listOf(),
        val scale: Float = 1f,
        val isLoading: Boolean = true,
        val lastInteractionTime: Long = System.currentTimeMillis(),
    )
}
