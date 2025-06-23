package net.matsudamper.allintoolscreensaver

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun DigitalClockScreen() {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var currentDate by remember { mutableStateOf(getCurrentDate()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = getCurrentTime()
            currentDate = getCurrentDate()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 左側：画像表示エリア（40%の幅）
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f)
            ) {
                ImageDisplayScreen()
            }
            
            // 中央：デジタル時計エリア（20%の幅）
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.2f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ClockTimeText(currentTime)
                    Spacer(modifier = Modifier.height(16.dp))
                    ClockDateText(currentDate)
                }
            }
            
            // 右側：カレンダー表示エリア（40%の幅）
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f)
            ) {
                CalendarDisplayScreen()
            }
        }
    }
}

@Composable
private fun ClockTimeText(time: String) {
    Text(
        text = time,
        color = Color.White,
        fontSize = 48.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ClockDateText(date: String) {
    Text(
        text = date,
        color = Color.Gray,
        fontSize = 18.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
    )
}

private fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date())
}

private fun getCurrentDate(): String {
    val formatter = SimpleDateFormat("yyyy年MM月dd日 (E)", Locale.JAPANESE)
    return formatter.format(Date())
}
