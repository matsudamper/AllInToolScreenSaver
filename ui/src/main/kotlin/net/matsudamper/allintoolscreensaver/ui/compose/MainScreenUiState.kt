package net.matsudamper.allintoolscreensaver.ui.compose

data class MainScreenUiState(
    val screenSaverSectionUiState: ScreenSaverSectionUiState,
    val calendarSectionUiState: CalendarSectionUiState,
)

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
