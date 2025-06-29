package net.matsudamper.allintoolscreensaver.compose.eventalert

import java.time.LocalTime
import net.matsudamper.allintoolscreensaver.CalendarEvent

enum class AlertType(val minutesBefore: Int, val displayText: String) {
    FIVE_MINUTES_BEFORE(5, "5分前"),
    ONE_MINUTE_BEFORE(1, "1分前"),
    EVENT_TIME(0, "開始時刻"),
}

data class EventAlertUiState(
    val currentAlert: DialogInfo?,
    val listener: Listener,
) {
    data class DialogInfo(
        val event: CalendarEvent,
        val alertType: AlertType,
        val eventStartTime: LocalTime,
        val eventStartTimeText: String,
        val isRepeatingAlert: Boolean,
    )

    interface Listener {
        suspend fun onStart()
        fun onAlertDismiss()
    }
}
