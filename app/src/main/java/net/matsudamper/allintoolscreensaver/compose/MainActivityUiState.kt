package net.matsudamper.allintoolscreensaver.compose

import android.net.Uri
import androidx.compose.runtime.Immutable
import net.matsudamper.allintoolscreensaver.CalendarInfo

@Immutable
data class MainActivityUiState(
    val selectedDirectoryPath: String?,
    val availableCalendars: List<CalendarInfo>,
    val selectedCalendarIds: List<Long>,
    val hasCalendarPermission: Boolean,
    val imageSwitchIntervalSeconds: Int,
    val listener: Listener,
) {
    @Immutable
    interface Listener {
        suspend fun onStart()
        fun onDirectorySelected(uri: Uri)
        fun onCalendarPermissionRequested()
        fun onCalendarSelectionChanged(calendarId: Long, isSelected: Boolean)
        fun onImageSwitchIntervalChanged(seconds: Int)
        fun onOpenDreamSettings()
    }
}
