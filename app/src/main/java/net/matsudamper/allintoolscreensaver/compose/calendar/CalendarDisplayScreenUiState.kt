package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.runtime.Immutable

data class CalendarDisplayScreenUiState(
    val calendarUiState: CalendarLayoutUiState,
    val isLoading: Boolean,
    val listener: Listener,
) {
    @Immutable
    interface Listener {
        suspend fun onStart()
        fun onInteraction()
    }
}
