package net.matsudamper.allintoolscreensaver.ui.compose

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class MainActivityUiState(
    val selectedDirectoryPath: String?,
    val availableCalendars: List<CalendarItem>,
    val selectedCalendarIds: List<Long>,
    val hasCalendarPermission: Boolean,
    val selectedCalendar: String,
    val imageSwitchIntervalSeconds: Int,
    val hasOverlayPermission: Boolean,
    val alertCalendarIds: List<Long>,
    val selectedAlertCalendar: String,
    val listener: Listener,
) {
    data class CalendarItem(
        val id: Long,
        val displayName: String,
        val accountName: String,
        val color: Int,
        val isSelected: Boolean,
        val listener: CalendarItemListener,
    )

    @Immutable
    interface CalendarItemListener {
        fun onSelectionChanged(isSelected: Boolean)
    }

    @Immutable
    interface Listener {
        suspend fun onStart()
        fun onDirectorySelected(uri: Uri)
        fun onCalendarSelectionChanged(calendarId: Long, isSelected: Boolean)
        fun onImageSwitchIntervalChanged(seconds: Int)
        fun onOpenDreamSettings()
        fun onNavigateToCalendarSelection()
        fun onNavigateToAlertCalendarSelection()
        fun onNavigateToCalendarDisplay()
        fun onNavigateToSlideShowPreview()
        fun onRequestOverlayPermission()
        fun updatePermissions(calendar: Boolean? = null, overlay: Boolean? = null)
        suspend fun onResume()
    }
}
