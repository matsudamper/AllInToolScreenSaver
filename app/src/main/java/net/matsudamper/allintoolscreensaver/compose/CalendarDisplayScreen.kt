package net.matsudamper.allintoolscreensaver.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import net.matsudamper.allintoolscreensaver.CalendarEvent
import net.matsudamper.allintoolscreensaver.CalendarManager
import net.matsudamper.allintoolscreensaver.SettingsManager

@Composable
fun CalendarDisplayScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val calendarManager = remember { CalendarManager(context) }

    var events by remember { mutableStateOf<List<CalendarEvent>>(listOf()) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var timeSlots by remember { mutableStateOf<List<TimeSlot>>(listOf()) }
    var scale by remember { mutableFloatStateOf(1f) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val listState = rememberLazyListState()

    // 現在時刻を更新
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(60000) // 1分ごとに更新
        }
    }

    // イベントの読み込み
    LaunchedEffect(Unit) {
        val selectedCalendarIds = settingsManager.getSelectedCalendarIds()
        if (selectedCalendarIds.isNotEmpty()) {
            val (startTime, endTime) = calendarManager.getTodayRange()
            events = calendarManager.getEventsForTimeRange(selectedCalendarIds, startTime, endTime)
        }

        // 24時間分のタイムスロットを生成
        timeSlots = generateTimeSlots()
    }

    // 無操作時の自動スクロール
    LaunchedEffect(lastInteractionTime) {
        delay(60000) // 1分待機
        if (System.currentTimeMillis() - lastInteractionTime >= 60000) {
            // ズームされている状態で現在時刻が画面外にある場合、自動スクロール
            if (scale > 1f) {
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val targetIndex = currentHour
                if (targetIndex < timeSlots.size) {
                    // 上1/3の位置に現在時刻が来るようにスクロール
                    val firstVisibleIndex = listState.firstVisibleItemIndex
                    val visibleItemsCount = listState.layoutInfo.visibleItemsInfo.size
                    val targetFirstVisibleIndex = max(0, targetIndex - visibleItemsCount / 3)

                    if (targetFirstVisibleIndex != firstVisibleIndex) {
                        listState.animateScrollToItem(targetFirstVisibleIndex)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, pan, zoom, _ ->
                        lastInteractionTime = System.currentTimeMillis()
                        scale = max(0.5f, min(3f, scale * zoom))
                    },
                )
            },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp)),
        ) {
            items(timeSlots) { timeSlot ->
                TimeSlotItem(
                    timeSlot = timeSlot,
                    events = events.filter { event ->
                        event.startTime <= timeSlot.endTime && event.endTime >= timeSlot.startTime
                    },
                    isCurrentTime = isCurrentTimeSlot(currentTime, timeSlot),
                    scale = scale,
                )
            }
        }
    }
}

data class TimeSlot(
    val startTime: Long,
    val endTime: Long,
    val hourText: String,
)

@Composable
private fun TimeSlotItem(
    timeSlot: TimeSlot,
    events: List<CalendarEvent>,
    isCurrentTime: Boolean,
    scale: Float,
) {
    val backgroundColor = if (isCurrentTime) {
        Color.Yellow.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    val itemHeight = (60.dp * scale).coerceAtLeast(30.dp)
    val fontSize = 14.sp * scale
    val eventFontSize = 10.sp * scale

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .background(backgroundColor)
            .drawBehind {
                if (isCurrentTime) {
                    // 現在時刻に横線を描画
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = (4.dp * scale).toPx(),
                    )
                }
            }
            .padding(horizontal = (8.dp * scale).coerceAtLeast(4.dp), vertical = (4.dp * scale).coerceAtLeast(2.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 時刻表示
        Text(
            text = timeSlot.hourText,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width((60.dp * scale).coerceAtLeast(40.dp)),
        )

        Spacer(modifier = Modifier.width((8.dp * scale).coerceAtLeast(4.dp)))

        // イベント表示エリア
        if (events.isNotEmpty()) {
            // 複数のイベントがある場合は2列、3列に分けて表示
            val columns = when {
                events.size <= 1 -> 1
                events.size <= 2 -> 2
                else -> 3
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy((4.dp * scale).coerceAtLeast(2.dp)),
            ) {
                events.chunked(events.size / columns + if (events.size % columns > 0) 1 else 0)
                    .forEach { columnEvents ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp)),
                        ) {
                            columnEvents.forEach { event ->
                                EventItem(event = event, scale = scale)
                            }
                        }
                    }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EventItem(event: CalendarEvent, scale: Float) {
    val eventFontSize = 10.sp * scale
    val timeFontSize = 8.sp * scale

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Blue.copy(alpha = 0.7f),
        ),
        shape = RoundedCornerShape((4.dp * scale).coerceAtLeast(2.dp)),
    ) {
        Column(
            modifier = Modifier.padding((6.dp * scale).coerceAtLeast(3.dp)),
        ) {
            Text(
                text = event.title,
                color = Color.White,
                fontSize = eventFontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            if (!event.allDay) {
                val startTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.startTime))
                val endTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.endTime))
                Text(
                    text = "$startTime-$endTime",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = timeFontSize,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun generateTimeSlots(): List<TimeSlot> {
    val timeSlots = mutableListOf<TimeSlot>()
    val calendar = Calendar.getInstance()

    // 今日の0時から開始
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    for (hour in 0..23) {
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        val endTime = calendar.timeInMillis

        val hourText = String.format("%02d:00", hour)
        timeSlots.add(TimeSlot(startTime, endTime, hourText))

        // 1時間戻す（次のループで正しく進むため）
        calendar.add(Calendar.HOUR_OF_DAY, -1)
        calendar.add(Calendar.HOUR_OF_DAY, 1)
    }

    return timeSlots
}

private fun isCurrentTimeSlot(currentTime: Long, timeSlot: TimeSlot): Boolean {
    return currentTime >= timeSlot.startTime && currentTime < timeSlot.endTime
}
