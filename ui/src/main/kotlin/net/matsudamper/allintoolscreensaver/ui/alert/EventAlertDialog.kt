package net.matsudamper.allintoolscreensaver.ui.alert

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.allintoolscreensaver.ui.component.DreamAlertDialog
import net.matsudamper.allintoolscreensaver.ui.component.SuspendLifecycleStartEffect
import net.matsudamper.allintoolscreensaver.ui.screensaver.EventAlertUiState

@Suppress("ModifierMissing")
@Composable
fun EventAlertDialog(
    uiState: EventAlertUiState,
) {
    SuspendLifecycleStartEffect(uiState.listener) {
        uiState.listener.onStart()
    }
    val currentAlert = uiState.currentAlert
    if (currentAlert != null) {
        DreamAlertDialog(
            title = {
                Column {
                    Text(
                        text = currentAlert.title,
                    )
                    Text(
                        text = "${currentAlert.alertTypeDisplayText}のアラート",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            dismissRequest = { uiState.listener.onAlertDismiss() },
            onClickNegative = { uiState.listener.onAlertDismiss() },
            negativeButton = {
                Text(text = "CLOSE")
            },
            positiveButton = null,
        ) {
            Column {
                Text(
                    text = currentAlert.eventStartTimeText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (currentAlert.isRepeatingAlert) {
                    Text(
                        text = "※ このアラートは10秒おきに5分間繰り返されます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(text = currentAlert.description)
            }
        }
    }
}
