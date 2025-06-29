package net.matsudamper.allintoolscreensaver.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHashCode
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

    val keyHash = currentCompositeKeyHashCode
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

    if (LocalInspectionMode.current) {
        content()
    }
}

@Composable
fun DreamAlertDialog(
    title: @Composable () -> Unit,
    dismissRequest: () -> Unit,
    positiveButton: @Composable (() -> Unit)? = {
        OutlinedButton(onClick = {}) {
            Text(text = "OK")
        }
    },
    negativeButton: @Composable (() -> Unit)? = {
        OutlinedButton(onClick = {}) {
            Text(text = "CANCEL")
        }
    },
    onClickPositive: () -> Unit = {},
    onClickNegative: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    DreamDialog(dismissRequest) {
        Card(
            modifier = Modifier
                .sizeIn(
                    minWidth = 500.dp,
                    minHeight = 300.dp,
                )
                .height(IntrinsicSize.Min)
                .width(IntrinsicSize.Min),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.Start,
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.headlineLarge,
                ) {
                    title()
                }
                Spacer(modifier = Modifier.height(24.dp))
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyLarge,
                ) {
                    content()
                }
                if (positiveButton != null || negativeButton != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        if (positiveButton != null) {
                            OutlinedButton(
                                onClick = onClickPositive,
                            ) {
                                positiveButton()
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (negativeButton != null) {
                            OutlinedButton(
                                onClick = onClickNegative,
                            ) {
                                negativeButton()
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DialogInfo(
    val key: Long,
    val content: @Composable () -> Unit,
    val dismissRequest: () -> Unit,
)
