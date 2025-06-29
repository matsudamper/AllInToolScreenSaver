package net.matsudamper.allintoolscreensaver.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("CompositionLocalAllowlist")
val LocalDreamDialogContentHolder = compositionLocalOf<MutableStateFlow<List<DialogInfo>>> {
    MutableStateFlow(listOf())
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
        val item = DialogInfo(
            key = keyHash,
            content = updatedContent.value,
            dismissRequest = updatedDismissRequest.value,
        )
        run {
            val items = dreamDialogContentHolder.value.toMutableList()
            items.add(item)
            dreamDialogContentHolder.value = items
        }
        onDispose {
            val items = dreamDialogContentHolder.value.toMutableList()
            items.remove(item)
            dreamDialogContentHolder.value = items
        }
    }

    if (LocalInspectionMode.current) {
        content()
    }
}

@Composable
fun DreamDialogHost() {
    for (dialogItem in LocalDreamDialogContentHolder.current.collectAsState().value) {
        var visible by remember { mutableStateOf(false) }
        DisposableEffect(Unit) {
            visible = true
            onDispose { visible = false }
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(initialAlpha = 0.5f),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { dialogItem.dismissRequest() },
                    )
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                dialogItem.content()
            }
        }
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
