package net.matsudamper.allintoolscreensaver.compose
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue

enum class CalendarSelectionMode {
    DISPLAY,
    ALERT,
}

data class CalendarSelectionScreenUiState(
    val availableCalendars: List<Calendar>,
    val hasCalendarPermission: Boolean,
    val selectionMode: CalendarSelectionMode,
    val listener: Listener,
) {
    data class Calendar(
        val id: Long,
        val displayName: String,
        val accountName: String,
        val color: Int,
        val isSelected: Boolean,
        val listener: CalendarListener,
    )

    @Immutable
    interface CalendarListener {
        fun onSelectionChanged(isSelected: Boolean)
    }

    @Immutable
    interface Listener {
        suspend fun onStart()
        fun updateCalendarPermission(granted: Boolean)
        fun onCalendarPermissionLaunch()
        fun onBack()
    }
}
