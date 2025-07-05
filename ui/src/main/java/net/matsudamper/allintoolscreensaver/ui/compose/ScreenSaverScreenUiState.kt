package net.matsudamper.allintoolscreensaver.ui.compose

data class ScreenSaverScreenUiState(
    val clockUiState: ClockUiState,
    val slideShowUiState: SlideShowUiState,
    val eventAlertUiState: EventAlertUiState?,
)

data class ClockUiState(
    val dateText: String,
    val timeText: String,
    val shouldShowDarkBackground: Boolean,
)

data class SlideShowUiState(
    val imageItems: List<SlideShowImageItem>,
    val currentPageIndex: Int,
    val intervalSeconds: Int,
)

data class SlideShowImageItem(
    val id: String,
    val imageUri: String?,
)

data class EventAlertUiState(
    val title: String,
    val alertTypeDisplayText: String,
    val eventStartTimeText: String,
    val description: String,
    val isRepeatingAlert: Boolean,
)
