package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.runtime.Immutable
import net.matsudamper.allintoolscreensaver.CalendarEvent
import java.time.Instant

data class CalendarDisplayScreenUiState(
    val calendarUiState: CalendarLayoutUiState,
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
}
