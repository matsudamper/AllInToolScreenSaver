package net.matsudamper.allintoolscreensaver.ui.state

data class AlertDialogUiState(
    val title: String,
    val alertTypeDisplayText: String,
    val eventStartTimeText: String,
    val description: String,
    val isRepeatingAlert: Boolean,
)
