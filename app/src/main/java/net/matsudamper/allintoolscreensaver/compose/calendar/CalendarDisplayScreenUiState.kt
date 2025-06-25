package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.runtime.Immutable
import net.matsudamper.allintoolscreensaver.CalendarEvent
import java.time.Instant

data class CalendarDisplayScreenUiState(
    val events: List<CalendarEvent>,
    val timeSlots: List<TimeSlot>,
    val currentTime: Instant,
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

    data class TimeSlot(
        val startTime: Instant,
        val endTime: Instant,
        val hourText: String,
    )
}
