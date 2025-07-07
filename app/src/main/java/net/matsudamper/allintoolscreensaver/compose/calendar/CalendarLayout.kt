package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.text.HtmlCompat
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.matsudamper.allintoolscreensaver.ui.AttendeeStatus
import net.matsudamper.allintoolscreensaver.ui.CalendarState
import net.matsudamper.allintoolscreensaver.ui.compose.LocalClock
import net.matsudamper.allintoolscreensaver.ui.compose.component.DreamAlertDialog
import net.matsudamper.allintoolscreensaver.ui.rememberCalendarState

data class CalendarLayoutUiState(
    val events: List<Event.Time>,
    val allDayEvents: List<Event.AllDay>,
) {
    sealed interface Event {
        val title: String
        val description: String?
        val color: Color
        val attendeeStatus: AttendeeStatus

        data class Time(
            val startTime: LocalTime,
            val endTime: LocalTime,
            val displayTime: String,
            override val title: String,
            override val description: String?,
            override val color: Color,
            override val attendeeStatus: AttendeeStatus,
        ) : Event

        data class AllDay(
            override val title: String,
            override val description: String?,
            override val color: Color,
            override val attendeeStatus: AttendeeStatus,
        ) : Event
    }
}

private const val HourSplitCount = 60
private val CurrentTimeDividerSize = 4.dp
private val CurrentTimeMarkerRadius = 5.dp

@Composable
internal fun CalendarLayout(
    uiState: CalendarLayoutUiState,
    modifier: Modifier = Modifier,
    state: CalendarState = rememberCalendarState(),
    clock: Clock = LocalClock.current,
) {
    val hourSize = state.hourSize
    val calcTimeEvents by remember(uiState.events) {
        val baseList = buildList {
            uiState.events.map { event ->
                val eventSize = run {
                    val second = event.endTime.toSecondOfDay() - event.startTime.toSecondOfDay()
                    val minutes = second / 60f
                    ceil(minutes / (60f / HourSplitCount)).toInt()
                }.coerceAtLeast(
                    // 最低の高さは15分
                    HourSplitCount / 4,
                )

                val index = run {
                    event.startTime.hour * HourSplitCount + floor(event.startTime.minute / (60f / HourSplitCount))
                }.toInt()
                add(
                    CalcTimeEvent(
                        uiState = event,
                        heightCount = eventSize,
                        startIndex = index,
                        rowSplitSize = 1,
                        rowIndex = 0,
                    ),
                )
            }
        }
        mutableStateOf(assignEventRows(baseList))
    }
    LaunchedEffect(Unit) {
        state.scrollToHours(LocalTime.now(clock).hour - 1)
    }

    var currentDayOfMinutes by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            val now = LocalTime.now(clock)
            currentDayOfMinutes = now.hour * 60 + now.minute

            delay(60.seconds - now.second.seconds)
            delay((1.seconds.inWholeNanoseconds - now.nano).nanoseconds)
        }
    }
    val dialogInfoState = remember { mutableStateOf<CalendarLayoutUiState.Event?>(null) }
    val dialogInfo = dialogInfoState.value
    if (dialogInfo != null) {
        EventDialog(
            event = dialogInfo,
            onDismissRequest = { dialogInfoState.value = null },
        )
    }

    Surface(modifier = modifier) {
        Column {
            if (uiState.allDayEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                for (event in uiState.allDayEvents) {
                    AllDayCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp,
                            ),
                        event = event,
                        onClick = {
                            dialogInfoState.value = event
                        },
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
            }
            Layout(
                modifier = Modifier.verticalScroll(state.scrollState),
                content = {
                    (0 until 24).map {
                        Text("${it.toString().padStart(2, '0')}:00")
                    }
                    (0 until 24).map {
                        HorizontalDivider()
                    }
                    CurrentTimeIndicator(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        lineThickness = CurrentTimeDividerSize,
                        markerRadius = CurrentTimeMarkerRadius,
                    )
                    for (event in calcTimeEvents) {
                        TimeCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(event.heightCount * hourSize / HourSplitCount)
                                .padding(1.dp),
                            event = event,
                            onClick = {
                                dialogInfoState.value = event.uiState
                            },
                        )
                    }
                },
                measurePolicy = remember(
                    hourSize,
                    currentDayOfMinutes,
                    calcTimeEvents,
                    state,
                ) {
                    CalendarMeasurePolicy(
                        hourSize = hourSize,
                        currentDayOfMinutes = currentDayOfMinutes,
                        calcTimeEvents = calcTimeEvents,
                        calendarState = state,
                    )
                },
            )
        }
    }
}

@Composable
private fun EventDialog(
    event: CalendarLayoutUiState.Event,
    onDismissRequest: () -> Unit,
) {
    DreamAlertDialog(
        dismissRequest = onDismissRequest,
        title = {
            Text(
                text = event.title,
            )
        },
        negativeButton = {
            Text(text = "CLOSE")
        },
        positiveButton = null,
        onClickNegative = {
            onDismissRequest()
        },
    ) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.Start,
        ) {
            when (event) {
                is CalendarLayoutUiState.Event.Time -> {
                    Text(
                        text = event.displayTime,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = event.description.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                is CalendarLayoutUiState.Event.AllDay -> {
                    Text(
                        text = event.description.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

private fun hourIndex(hour: Int): Int {
    return hour
}

private fun hourDividerIndex(hour: Int): Int {
    return 24 + hour
}

private fun currentTimeIndex(): Int {
    return 24 + 24
}

private fun eventItemIndex(index: Int): Int {
    return 24 + 24 + 1 + index
}

data class CalcTimeEvent(
    // 時間の高さをの1/HourSplitCountを1としたサイズ。最低の高さは15分
    val heightCount: Int,
    // 時間の高さをの1/HourSplitCountを1としたサイズの0時からの開始位置
    val startIndex: Int,
    val uiState: CalendarLayoutUiState.Event.Time,
    // その時間帯が何個で分割されているか
    val rowSplitSize: Int,
    val rowIndex: Int,
)

private data class CalendarMeasurePolicy(
    private val hourSize: Dp,
    private val currentDayOfMinutes: Int,
    private val calcTimeEvents: List<CalcTimeEvent>,
    private val calendarState: CalendarState,
) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val hourPlaceableList = (0 until 24).map { index ->
            measurables[hourIndex(index)].measure(constraints)
        }
        val hourMaxWidth = hourPlaceableList.maxOf { it.width }
        val hourAverageHeightPx = hourPlaceableList.sumOf { it.height } / 24
        calendarState.verticalPadding = hourAverageHeightPx / 2

        val hourDividerPlaceableList = (0 until 24).map { index ->
            measurables[hourDividerIndex(index)].measure(
                constraints.copy(
                    minWidth = constraints.maxWidth - hourMaxWidth,
                    maxWidth = constraints.maxWidth - hourMaxWidth,
                ),
            )
        }

        val eventPlaceableList = calcTimeEvents.map { event ->
            measurables[eventItemIndex(calcTimeEvents.indexOf(event))].measure(
                constraints.copy(
                    minWidth = (constraints.maxWidth - hourMaxWidth) / event.rowSplitSize,
                    maxWidth = (constraints.maxWidth - hourMaxWidth) / event.rowSplitSize,
                ),
            )
        }

        val currentTimeDividerPlaceable = measurables[currentTimeIndex()].measure(
            run {
                val width = (constraints.maxWidth - hourMaxWidth) + (CurrentTimeMarkerRadius * 2)
                    .roundToPx()
                constraints.copy(
                    minWidth = width,
                    maxWidth = width,
                )
            },
        )

        return layout(constraints.maxWidth, (hourSize * 24).roundToPx()) {
            hourPlaceableList.forEachIndexed { index, placeable ->
                placeable.place(0, hourSize.roundToPx() * index)
            }
            eventPlaceableList.forEachIndexed { index, placeable ->
                val event = calcTimeEvents[index]
                val eventWidth = (constraints.maxWidth - hourMaxWidth) / event.rowSplitSize
                placeable.place(
                    x = hourMaxWidth + (eventWidth * event.rowIndex),
                    y = (hourSize * event.startIndex / HourSplitCount).roundToPx() + (hourAverageHeightPx / 2),
                )
            }
            currentTimeDividerPlaceable.place(
                x = hourMaxWidth - CurrentTimeMarkerRadius.roundToPx() * 2,
                y = run {
                    val paddingTop = (hourAverageHeightPx / 2) - (currentTimeDividerPlaceable.height / 2)

                    ((currentDayOfMinutes / 60f) * hourSize).roundToPx() + paddingTop
                },
            )
            hourDividerPlaceableList.forEachIndexed { index, placeable ->
                placeable.place(
                    x = hourMaxWidth,
                    y = (hourSize * index).roundToPx() + (hourAverageHeightPx / 2),
                )
            }
        }
    }
}

@Composable
private fun CurrentTimeIndicator(
    color: Color,
    lineThickness: Dp,
    markerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val lineThicknessPx = lineThickness.toPx()
        val markerRadiusPx = markerRadius.toPx()
        val centerY = size.height / 2f

        val markerCenterX = markerRadiusPx
        drawCircle(
            color = color,
            radius = markerRadiusPx,
            center = Offset(markerCenterX, centerY),
            style = Stroke(width = lineThicknessPx),
        )

        drawLine(
            color = color,
            start = Offset(markerRadiusPx * 2, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = lineThicknessPx,
        )
    }
}

@Composable
@NonRestartableComposable
private fun AllDayCard(
    event: CalendarLayoutUiState.Event.AllDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EventCard(
        modifier = modifier,
        title = event.title,
        displayTime = null,
        color = event.color,
        attendeeStatus = event.attendeeStatus,
        onClick = onClick,
    )
}

@Composable
@NonRestartableComposable
private fun TimeCard(
    event: CalcTimeEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EventCard(
        title = event.uiState.title,
        displayTime = event.uiState.displayTime,
        modifier = modifier,
        color = event.uiState.color,
        attendeeStatus = event.uiState.attendeeStatus,
        onClick = onClick,
    )
}

@Composable
private fun EventCard(
    title: String,
    displayTime: String?,
    color: Color,
    attendeeStatus: AttendeeStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 0.dp,
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(2.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (attendeeStatus) {
                    AttendeeStatus.TENTATIVE -> Color.Transparent
                    AttendeeStatus.DECLINED -> Color.Transparent
                    else -> color
                },
            ),
            border = when (attendeeStatus) {
                AttendeeStatus.TENTATIVE,
                AttendeeStatus.DECLINED,
                -> BorderStroke(2.dp, color)
                else -> null
            },
            onClick = onClick,
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
            ) {
                BasicText(
                    text = remember(title, attendeeStatus) {
                        when (attendeeStatus) {
                            AttendeeStatus.DECLINED -> buildAnnotatedString {
                                append(title)
                                addStyle(
                                    style = androidx.compose.ui.text.SpanStyle(
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                    ),
                                    start = 0,
                                    end = title.length,
                                )
                                // Add second line for thicker strikethrough
                                addStyle(
                                    style = androidx.compose.ui.text.SpanStyle(
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            offset = Offset(0f, 1f),
                                            blurRadius = 0f,
                                        ),
                                    ),
                                    start = 0,
                                    end = title.length,
                                )
                            }

                            else -> AnnotatedString(title)
                        }
                    },
                    autoSize = TextAutoSize.StepBased(
                        maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
                    ),
                )
                if (displayTime != null) {
                    BasicText(
                        text = remember(displayTime, attendeeStatus) {
                            val annotatedString = htmlToAnnotatedString(displayTime)
                            when (attendeeStatus) {
                                AttendeeStatus.DECLINED -> buildAnnotatedString {
                                    append(annotatedString)
                                    addStyle(
                                        style = androidx.compose.ui.text.SpanStyle(
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                        ),
                                        start = 0,
                                        end = annotatedString.length,
                                    )
                                    // Add second line for thicker strikethrough
                                    addStyle(
                                        style = androidx.compose.ui.text.SpanStyle(
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                offset = Offset(0f, 1f),
                                                blurRadius = 0f,
                                            ),
                                        ),
                                        start = 0,
                                        end = annotatedString.length,
                                    )
                                }

                                else -> annotatedString
                            }
                        },
                        autoSize = TextAutoSize.StepBased(
                            maxFontSize = MaterialTheme.typography.labelSmall.fontSize,
                        ),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun htmlToAnnotatedString(html: String): AnnotatedString {
    return buildAnnotatedString {
        val htmlCompat = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        append(htmlCompat.toString())
    }
}

private fun assignEventRows(events: List<CalcTimeEvent>): List<CalcTimeEvent> {
    // スイープライン法でrowIndex/rowSplitSizeを割り当て
    data class TempEvent(
        val base: CalcTimeEvent,
        var rowIndex: Int = -1,
        var rowSplitSize: Int = 1,
    )

    val tempEvents = events.map {
        TempEvent(
            base = it,
            rowIndex = -1,
            rowSplitSize = 1,
        )
    }.toMutableList()
    val active = mutableListOf<TempEvent>()
    // 並び替えせず、リスト順で処理
    val allEvents = tempEvents.withIndex()
        .sortedWith(
            compareBy(
                { it.value.base.startIndex }, // 開始時刻
                { it.index }, // 元リストの順番
            ),
        )
        .map { it.value }
    for (event in allEvents) {
        // 終了したイベントをactiveから除外
        active.removeAll { it.base.startIndex + it.base.heightCount <= event.base.startIndex }
        // 使われているrowIndexを調べる
        val usedRows = active.map { it.rowIndex }.toSet()
        // 最小の未使用rowIndexを割り当て
        var row = 0
        while (row in usedRows) row++
        event.rowIndex = row
        active.add(event)
        // 現在のグループの最大重なり数を更新
        val overlapCount = active.size
        active.forEach { it.rowSplitSize = max(it.rowSplitSize, overlapCount) }
    }
    return tempEvents.map {
        it.base.copy(
            rowIndex = it.rowIndex,
            rowSplitSize = it.rowSplitSize,
        )
    }
}

@Composable
@Preview
private fun PreviewEventDialogContent() {
    EventDialog(
        event = CalendarLayoutUiState.Event.Time(
            startTime = LocalTime.of(1, 0),
            endTime = LocalTime.of(2, 0),
            title = "Sample Event",
            displayTime = "01:00 - 02:00",
            description = "This is a sample event description.",
            color = Color.Blue,
            attendeeStatus = AttendeeStatus.ACCEPTED,
        ),
        onDismissRequest = {},
    )
}

internal val previewCalendarLayoutUiState = CalendarLayoutUiState(
    events = listOf(
        CalendarLayoutUiState.Event.Time(
            startTime = LocalTime.of(0, 0),
            endTime = LocalTime.of(1, 0),
            title = "One",
            displayTime = "00:00 - 01:00",
            description = "description",
            color = Color.Red,
            attendeeStatus = AttendeeStatus.ACCEPTED,
        ),
        CalendarLayoutUiState.Event.Time(
            startTime = LocalTime.of(1, 0),
            endTime = LocalTime.of(3, 0),
            title = "Two",
            displayTime = "01:00 - 03:00",
            description = "description",
            color = Color.Blue,
            attendeeStatus = AttendeeStatus.DECLINED,
        ),
        CalendarLayoutUiState.Event.Time(
            startTime = LocalTime.of(1, 0),
            endTime = LocalTime.of(2, 0),
            title = "Three",
            displayTime = "01:00 - 02:00",
            description = "description",
            color = Color.Yellow,
            attendeeStatus = AttendeeStatus.TENTATIVE,
        ),
        CalendarLayoutUiState.Event.Time(
            startTime = LocalTime.of(1, 30),
            endTime = LocalTime.of(3, 0),
            title = "Four",
            displayTime = "01:30 - 03:00",
            description = "description",
            color = Color.Green,
            attendeeStatus = AttendeeStatus.INVITED,
        ),
        CalendarLayoutUiState.Event.Time(
            startTime = LocalTime.of(2, 0),
            endTime = LocalTime.of(3, 0),
            title = "Five",
            displayTime = "02:00 - 03:00",
            description = "description",
            color = Color.Magenta,
            attendeeStatus = AttendeeStatus.ACCEPTED,
        ),
        CalendarLayoutUiState.Event.Time(
            startTime = LocalTime.of(3, 15),
            endTime = LocalTime.of(3, 30),
            title = "Six",
            displayTime = "03:15 - 03:30",
            description = "description",
            color = Color.Cyan,
            attendeeStatus = AttendeeStatus.NONE,
        ),
    ),
    allDayEvents = listOf(
        CalendarLayoutUiState.Event.AllDay(
            title = "All Day Event",
            description = "Description",
            color = Color.Red,
            attendeeStatus = AttendeeStatus.ACCEPTED,
        ),
        CalendarLayoutUiState.Event.AllDay(
            title = "All Day Event 2",
            description = "Description",
            color = Color.Yellow,
            attendeeStatus = AttendeeStatus.DECLINED,
        ),
    ),
)

val previewCalendarLayoutClock: Clock = run {
    val zone = ZoneId.of("UTC")
    Clock.fixed(
        LocalDateTime.of(
            LocalDate.of(2010, 10, 10),
            LocalTime.of(1, 30, 0),
        ).toInstant(ZoneOffset.ofHours(0)),
        zone,
    )
}

@Composable
@Preview(showBackground = true)
private fun Preview() {
    MaterialTheme {
        CalendarLayout(
            uiState = previewCalendarLayoutUiState,
            clock = previewCalendarLayoutClock,
        )
    }
}
