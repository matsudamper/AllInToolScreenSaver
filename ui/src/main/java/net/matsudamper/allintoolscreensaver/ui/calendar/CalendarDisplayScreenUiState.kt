package net.matsudamper.allintoolscreensaver.ui.calendar

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.channels.Channel

data class CalendarDisplayScreenUiState(
    val calendarUiState: CalendarLayoutUiState,
    val operationFlow: Channel<(Operation) -> Unit>,
    val alertEnabled: Boolean,
    val listener: Listener,
) {
    @Immutable
    interface Operation {
        fun moveCurrentTime()
    }

    @Immutable
    interface Listener {
        suspend fun onStart()
        fun onInteraction()
        fun onAlertEnabledChanged(enabled: Boolean)
    }
}
