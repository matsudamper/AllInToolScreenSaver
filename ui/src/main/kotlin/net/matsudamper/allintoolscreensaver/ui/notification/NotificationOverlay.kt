package net.matsudamper.allintoolscreensaver.ui.notification

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

@Composable
fun NotificationOverlay(
    uiState: NotificationOverlayUiState,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            top = 16.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        userScrollEnabled = false,
    ) {
        items(
            items = uiState.notifications,
            key = { notification -> notification.id },
        ) { notification ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value != SwipeToDismissBoxValue.Settled) {
                        notification.listener.dismissRequest()
                        true
                    } else {
                        false
                    }
                },
            )
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {},
            ) {
                NotificationItem(
                    notification = notification,
                    hazeState = hazeState,
                    uiState = uiState,
                    dismissRequest = {
                        coroutineScope.launch {
                            dismissState.dismiss(SwipeToDismissBoxValue.StartToEnd)
                            notification.listener.dismissRequest()
                        }
                    },
                    onClick = notification.listener::onClick,
                )
            }
        }
    }
}

internal class DerivedOffsetAnimationSpec(private val boundsSpec: FiniteAnimationSpec<IntRect>) :
    FiniteAnimationSpec<IntOffset> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<IntOffset, V>,
    ): VectorizedFiniteAnimationSpec<V> =
        boundsSpec.vectorize(
            object : TwoWayConverter<IntRect, V> {
                override val convertFromVector: (V) -> IntRect = { vector ->
                    with(converter.convertFromVector(vector)) { IntRect(x, y, x, y) }
                }
                override val convertToVector: (IntRect) -> V = { bounds ->
                    converter.convertToVector(bounds.topLeft)
                }
            },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DerivedOffsetAnimationSpec) return false
        return boundsSpec == other.boundsSpec
    }

    override fun hashCode(): Int = boundsSpec.hashCode()
}

@Composable
private fun NotificationItem(
    notification: NotificationOverlayUiState.NotificationItem,
    hazeState: HazeState,
    uiState: NotificationOverlayUiState,
    dismissRequest: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissRequestUpdated = rememberUpdatedState(dismissRequest)
    LaunchedEffect(uiState.displayDuration) {
        if (uiState.displayDuration.isInfinite().not()) {
            delay(uiState.displayDuration)
            dismissRequestUpdated.value()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .hazeEffect(
                state = hazeState,
                style = HazeStyle.Unspecified.copy(
                    blurRadius = 8.dp,
                ),
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (notification.title.isNotEmpty()) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (notification.appName.isNotEmpty()) {
                    Text(
                        text = "・${notification.appName}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            if (notification.text.isNotEmpty()) {
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
@Preview
private fun Preview() {
    MaterialTheme {
        NotificationOverlay(
            uiState = NotificationOverlayUiState(
                notifications = List(3) { index ->
                    NotificationOverlayUiState.NotificationItem(
                        id = index.toString(),
                        title = "通知タイトル $index",
                        text = "通知の内容がここに表示されます。",
                        appName = "アプリ名",
                        listener = object : NotificationOverlayUiState.NotificationItem.ItemListener {
                            override fun dismissRequest() = Unit
                            override fun onClick() = Unit
                        },
                    )
                },
                displayDuration = 10.seconds,
                listener = object : NotificationOverlayUiState.Listener {},
            ),
            hazeState = remember { HazeState() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
