package net.matsudamper.allintoolscreensaver.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

@Suppress("CompositionLocalAllowlist")
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
    val updatedContent = rememberUpdatedState(content)
    val updatedDismissRequest = rememberUpdatedState(dismissRequest)

    DisposableEffect(keyHash) {
        dreamDialogContentHolder as MutableList<DialogInfo>
        val item = DialogInfo(
            key = keyHash,
            content = updatedContent.value,
            dismissRequest = updatedDismissRequest.value,
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
