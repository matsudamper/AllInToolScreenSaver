package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.channels.Channel
import net.matsudamper.allintoolscreensaver.CalendarEvent

data class CalendarDisplayScreenUiState(
    val calendarUiState: CalendarLayoutUiState,
    val operationFlow: Channel<(Operation) -> Unit>,
    val currentAlert: CalendarEvent?,
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
        fun onAlertDismiss()
    }
}
