package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import net.matsudamper.allintoolscreensaver.CalendarEvent


@Composable
internal fun CalendarLayout(
    uiState: CalendarDisplayScreenUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    val spacingDp = if (0.2.dp * uiState.scale > 0.1.dp) 0.2.dp * uiState.scale else 0.1.dp

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag(CalendarDisplayScreenTestTag.CalendarLayout.testTag())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacingDp),
    ) {
        items(uiState.timeSlots) { timeSlot ->
            TimeSlotLayout(
                timeSlot = timeSlot,
                events = uiState.events.filter { event ->
                    event.startTime.isBefore(timeSlot.endTime) && event.endTime.isAfter(timeSlot.startTime)
                },
                currentTime = uiState.currentTime,
                scale = uiState.scale,
            )
        }
    }
}

@Composable
private fun TimeSlotLayout(
    timeSlot: CalendarDisplayScreenUiState.TimeSlot,
    events: List<CalendarEvent>,
    currentTime: Instant,
    scale: Float,
) {
    val isCurrentTime = !currentTime.isBefore(timeSlot.startTime) && currentTime.isBefore(timeSlot.endTime)
    val isHourMark = timeSlot.hourText.endsWith("00")

    val itemHeight = if (20.dp * scale > 20.dp) 20.dp * scale else 20.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .background(
                if (isCurrentTime) Color.Yellow.copy(alpha = 0.3f) else Color.Transparent,
            )
            .drawBehind {
                if (isHourMark) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f,
                    )
                }

                if (isCurrentTime) {
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2f,
                    )
                }
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isHourMark) {
            Text(
                text = timeSlot.hourText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(60.dp),
            )
        } else {
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (events.isNotEmpty()) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                events.forEach { event ->
                    EventItem(event = event, scale = scale)
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EventItem(event: CalendarEvent, scale: Float) {
    val eventHeight = if (16.dp * scale > 16.dp) 16.dp * scale else 16.dp
    val fontSize = if (10.sp * scale > 10.sp) 10.sp * scale else 10.sp

    Box(
        modifier = Modifier
            .background(
                Color.Blue.copy(alpha = 0.7f),
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .height(eventHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = event.title,
            color = Color.White,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}