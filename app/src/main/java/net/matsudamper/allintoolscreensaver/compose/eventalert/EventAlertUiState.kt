package net.matsudamper.allintoolscreensaver.compose.eventalert

import net.matsudamper.allintoolscreensaver.CalendarEvent

data class EventAlertUiState(
    val currentAlert: CalendarEvent?,
    val listener: Listener,
) {
    interface Listener {
        suspend fun onStart()
        fun onAlertDismiss()
    }
}
