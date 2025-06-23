package net.matsudamper.allintoolscreensaver.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import net.matsudamper.allintoolscreensaver.AlertManager
import net.matsudamper.allintoolscreensaver.CalendarEvent

@Composable
fun DigitalClockScreen() {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var currentDate by remember { mutableStateOf(getCurrentDate()) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var currentAlert by remember { mutableStateOf<CalendarEvent?>(null) }

    val alertManager = remember { AlertManager(context) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = getCurrentTime()
            currentDate = getCurrentDate()
        }
    }

    LaunchedEffect(Unit) {
        alertManager.onAlertTriggered = { calendarEvent ->
            currentAlert = calendarEvent
            showAlertDialog = true
        }
        alertManager.startAlertMonitoring()
    }

    DisposableEffect(Unit) {
        onDispose {
            alertManager.cleanup()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.1f),
            ) {
                ImageDisplayScreen()

                Clock(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 24.dp,
                            bottom = 24.dp,
                        ),
                    date = currentDate,
                    time = currentTime,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.1f),
            ) {
                CalendarDisplayScreen()
            }
        }

        // アラートダイアログ
        if (showAlertDialog && currentAlert != null) {
            AlertDialogScreen(
                alertTime = currentAlert!!,
                onDismiss = {
                    showAlertDialog = false
                    currentAlert = null
                },
                alertManager = alertManager,
            )
        }
    }
}

@Composable
fun AlertDialogScreen(
    alertTime: CalendarEvent,
    onDismiss: () -> Unit,
    alertManager: AlertManager,
) {
    var repeatCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (repeatCount < 30) { // 最大30回（5分間）
            delay(10000) // 10秒待機
            alertManager.playAlertSound()
            repeatCount++
        }
        onDismiss() // 最大時間に達したら自動で閉じる
    }

    Dialog(
        onDismissRequest = { /* 空にして自動で閉じないようにする */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = 0.9f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "予定のアラート",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = alertTime.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(8.dp))

                val startTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(alertTime.startTime))
                Text(
                    text = "開始時刻: $startTime",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                )

                if (!alertTime.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = alertTime.description!!,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 3,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("OK", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun Clock(
    date: String,
    time: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = time,
            color = Color.White,
            fontSize = 48.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = date,
            color = Color.Gray,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
        )
    }
}

private fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date())
}

private fun getCurrentDate(): String {
    val formatter = SimpleDateFormat("yyyy年MM月dd日 (E)", Locale.JAPANESE)
    return formatter.format(Date())
}
