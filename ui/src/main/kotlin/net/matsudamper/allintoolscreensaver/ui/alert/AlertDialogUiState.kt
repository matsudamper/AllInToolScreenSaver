package net.matsudamper.allintoolscreensaver.ui.alert

data class AlertDialogUiState(
    val title: String,
    val alertTypeDisplayText: String,
    val eventStartTimeText: String,
    val description: String,
    val isRepeatingAlert: Boolean,
)
