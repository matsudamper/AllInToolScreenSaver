package net.matsudamper.allintoolscreensaver.ui.notification

import androidx.compose.runtime.Immutable

data class NotificationOverlayUiState(
    val notifications: List<NotificationItem>,
    val isVisible: Boolean,
    val listener: Listener,
) {
    @Immutable
    interface Listener

    data class NotificationItem(
        val title: String,
        val text: String,
        val listener: ItemListener,
    ) {
        @Immutable
        interface ItemListener {
            fun dismissRequest()
            fun onClick()
        }
    }
}
