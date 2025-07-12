package net.matsudamper.allintoolscreensaver.ui.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun NotificationOverlay(
    uiState: NotificationOverlayUiState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = uiState.isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            uiState.notifications.filter { it.isVisible }.forEach { notification ->
                NotificationItem(
                    notification = notification,
                    dismissRequest = notification.listener::dismissRequest,
                    onClick = notification.listener::onClick,
                )
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationOverlayUiState.NotificationItem,
    dismissRequest: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(notification.id) {
        delay(5000) // 5秒後に自動で消す
        dismissRequest()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Column {
            if (notification.title.isNotEmpty()) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (notification.text.isNotEmpty()) {
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
