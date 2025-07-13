package net.matsudamper.allintoolscreensaver.ui.notification

import androidx.compose.runtime.Immutable
import kotlin.time.Duration

data class NotificationOverlayUiState(
    val notifications: List<NotificationItem>,
    val displayDuration: Duration,
    val listener: Listener,
) {
    @Immutable
    interface Listener

    data class NotificationItem(
        val id: String,
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
