package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.runtime.Immutable
import java.time.Instant

data class CalendarDisplayScreenUiState(
    val calendarUiState: CalendarLayoutUiState,
    val currentTime: Instant,
    val isLoading: Boolean,
    val listener: Listener,
) {
    @Immutable
    interface Listener {
        suspend fun onStart()
        fun onInteraction()
    }
}
