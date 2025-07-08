package net.matsudamper.allintoolscreensaver.ui.screensaver

import java.time.LocalTime

data class EventAlertUiState(
    val currentAlert: DialogInfo?,
    val listener: Listener,
) {
    data class DialogInfo(
        val title: String,
        val description: String,
        val alertTypeDisplayText: String,
        val eventStartTime: LocalTime,
        val eventStartTimeText: String,
        val isRepeatingAlert: Boolean,
    )

    interface Listener {
        suspend fun onStart()
        fun onAlertDismiss()
    }
}
