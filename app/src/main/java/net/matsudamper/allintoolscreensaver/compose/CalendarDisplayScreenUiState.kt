package net.matsudamper.allintoolscreensaver.compose

import androidx.compose.runtime.Immutable
import net.matsudamper.allintoolscreensaver.CalendarEvent

data class CalendarDisplayScreenUiState(
    val events: List<CalendarEvent>,
    val timeSlots: List<TimeSlot>,
    val currentTime: Long,
    val scale: Float,
    val isLoading: Boolean,
    val listener: Listener,
) {
    @Immutable
    interface Listener {
        suspend fun onStart()
        fun onZoomIn()
        fun onZoomOut()
        fun onInteraction()
    }
}

data class TimeSlot(
    val startTime: Long,
    val endTime: Long,
    val hourText: String,
)
