package net.matsudamper.allintoolscreensaver.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.matsudamper.allintoolscreensaver.ui.state.AlertDialogUiState

@Composable
fun AlertOverlayDialog(
    alertInfo: AlertDialogUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .size(width = 400.dp, height = 300.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = alertInfo.title,
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${alertInfo.alertTypeDisplayText}のアラート",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alertInfo.eventStartTimeText,
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (alertInfo.isRepeatingAlert) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "※ このアラートは10秒おきに5分間繰り返されます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = alertInfo.description,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onDismiss,
                ) {
                    Text("閉じる")
                }
            }
        }
    }
}
