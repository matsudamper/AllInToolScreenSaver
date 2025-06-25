package net.matsudamper.allintoolscreensaver.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.staticCompositionLocalOf

val LocalDreamDialogContentHolder = staticCompositionLocalOf<List<DialogInfo>> {
    mutableListOf()
}

@Composable
fun DreamDialog(
    dismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dreamDialogContentHolder = LocalDreamDialogContentHolder.current
    val keyHash = currentCompositeKeyHash
    DisposableEffect(content, keyHash) {
        dreamDialogContentHolder as MutableList
        val item = DialogInfo(
            key = keyHash,
            content = content,
            dismissRequest = dismissRequest,
        )
        dreamDialogContentHolder.add(item)
        onDispose {
            dreamDialogContentHolder.remove(item)
        }
    }
}

data class DialogInfo(
    val key: Int,
    val content: @Composable () -> Unit,
    val dismissRequest: () -> Unit,
)
