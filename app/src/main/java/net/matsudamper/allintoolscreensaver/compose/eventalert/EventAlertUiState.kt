package net.matsudamper.allintoolscreensaver.compose.eventalert

import net.matsudamper.allintoolscreensaver.CalendarEvent

data class EventAlertUiState(
    val currentAlert: DialogInfo?,
    val listener: Listener,
) {
    data class DialogInfo(
        val event: CalendarEvent,
    )
    
    interface Listener {
        suspend fun onStart()
        fun onAlertDismiss()
    }
}
