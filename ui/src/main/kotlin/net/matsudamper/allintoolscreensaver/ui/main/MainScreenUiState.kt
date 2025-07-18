package net.matsudamper.allintoolscreensaver.ui.main

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlin.time.Duration

data class MainScreenUiState(
    val screenSaverSectionUiState: ScreenSaverSectionUiState,
    val calendarSectionUiState: CalendarSectionUiState,
    val notificationSectionUiState: NotificationSectionUiState,
    val listener: Listener,
) {
    @Immutable
    interface Listener {
        suspend fun onStart()
        fun onDirectorySelected(uri: Uri)
        fun onCalendarSelectionChanged(calendarId: Long, isSelected: Boolean)
        fun onImageSwitchIntervalChanged(seconds: Int)
        fun onNotificationDisplayDurationChanged(duration: Duration)
        fun onOpenDreamSettings()
        fun onNavigateToCalendarSelection()
        fun onNavigateToAlertCalendarSelection()
        fun onNavigateToCalendarDisplay()
        fun onNavigateToSlideShowPreview()
        fun onNavigateToNotificationPreview()
        fun onRequestOverlayPermission()
        fun updatePermissions(calendar: Boolean? = null, overlay: Boolean? = null)
        suspend fun onResume()
    }
}

data class ScreenSaverSectionUiState(
    val selectedDirectoryPath: String,
    val imageSwitchIntervalSeconds: Int,
    val intervalOptions: List<IntervalOption>,
)

data class IntervalOption(
    val seconds: Int,
    val displayText: String,
    val isSelected: Boolean,
)

data class CalendarSectionUiState(
    val selectedCalendarDisplayName: String,
    val selectedAlertCalendarDisplayName: String,
    val hasOverlayPermission: Boolean,
    val hasCalendarPermission: Boolean,
)

data class NotificationSectionUiState(
    val hasNotificationPermission: Boolean,
    val hasNotificationListenerPermission: Boolean,
    val displayDurationOptions: List<DurationOption>,
    val listener: Listener,
) {
    interface Listener {
        fun onClickSendTestNotification()
        fun onOpenNotificationListenerSettings()
    }

    data class DurationOption(
        val duration: Duration,
        val displayText: String,
        val isSelected: Boolean,
    )
}
