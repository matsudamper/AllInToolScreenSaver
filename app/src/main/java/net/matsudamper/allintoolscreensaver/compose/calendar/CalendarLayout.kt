package net.matsudamper.allintoolscreensaver.compose.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
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
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.matsudamper.allintoolscreensaver.compose.component.DreamDialog

data class CalendarLayoutUiState(
    val events: List<Event.Time>,
    val allDayEvents: List<Event.AllDay>,
) {
    sealed interface Event {
        val title: String
        val description: String?
        val color: Color

        data class Time(
            val startTime: LocalTime,
            val endTime: LocalTime,
            val displayTime: String,
            override val title: String,
            override val description: String?,
            override val color: Color,
        ) : Event

        data class AllDay(
            override val title: String,
            override val description: String?,
            override val color: Color,
        ) : Event
    }
}

private const val HourSplitCount = 4
private val CurrentTimeDividerSize = 4.dp
private val CurrentTimeMarkerRadius = 5.dp

@Stable
class CalendarState internal constructor(
    internal val scrollState: ScrollState,
    private val density: Density,
    hourSize: Dp,
) {

    var hourSize: Dp by mutableStateOf(hourSize)

    fun zoomIn() {
        hourSize = (hourSize + 10.dp)
            .coerceAtMost(200.dp)
    }

    fun zoomOut() {
        hourSize = (hourSize - 10.dp)
            .coerceAtLeast(10.dp)
    }

    fun isCurrentTimeDisplayed(): Boolean {
        val hourHeightPx = with(density) { hourSize.roundToPx() }
        val now = LocalTime.now()
        val currentTimeValue = (now.hour / (now.minute / 60f) * hourHeightPx).roundToInt()
        val startValue = scrollState.value + hourHeightPx
        val endValue = scrollState.maxValue - hourHeightPx
        return if (startValue > endValue) {
            currentTimeValue in (scrollState.value)..(scrollState.maxValue)
        } else {
            currentTimeValue in (scrollState.value + hourHeightPx)..(scrollState.maxValue - hourHeightPx)
        }
    }

    suspend fun scrollToHours(hours: Int) {
        val offset = with(density) { (hours * hourSize).roundToPx() }
        scrollState.animateScrollTo(offset)
        scrollState.scrollTo(offset)
    }

    suspend fun animateScrollToHours(hours: Int) {
        val offset = with(density) { (hours * hourSize).roundToPx() }
        scrollState.animateScrollTo(offset)
    }

    suspend fun addAnimateScrollToHours(hours: Int) {
        with(density) {
            scrollState.animateScrollTo(scrollState.value + (hours * hourSize).roundToPx())
        }
    }
}

@Composable
fun rememberCalendarState(initialHourSize: Dp = 100.dp): CalendarState {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    return remember(scrollState, density) {
        CalendarState(
            scrollState = scrollState,
            density = density,
            hourSize = initialHourSize,
        )
    }
}

@Composable
internal fun CalendarLayout(
    uiState: CalendarLayoutUiState,
    modifier: Modifier = Modifier,
    state: CalendarState = rememberCalendarState(),
    clock: Clock = remember { Clock.systemDefaultZone() },
) {
    val hourSize = state.hourSize
    val calcTimeEvents by remember(uiState.events) {
        val baseList = buildList {
            uiState.events.map { event ->
                val eventSize = run {
                    val second = event.endTime.toSecondOfDay() - event.startTime.toSecondOfDay()
                    val minutes = second / 60f
                    ceil(minutes / 15f).toInt()
                }.coerceAtLeast(2)

                val index = run {
                    event.startTime.hour * HourSplitCount + floor(event.startTime.minute / 15f)
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
        DreamDialog(
            dismissRequest = { dialogInfoState.value = null },
        ) {
            EventDialogContent(
                event = dialogInfo,
                onDismissRequest = { dialogInfoState.value = null },
            )
        }
    }

    Surface(modifier = modifier) {
        Column {
            for (event in uiState.allDayEvents) {
                AllDayCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    event = event,
                    onClick = {
                        dialogInfoState.value = event
                    },
                )
            }
            HorizontalDivider()
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
                measurePolicy = remember(hourSize, currentDayOfMinutes, calcTimeEvents) {
                    CalendarMeasurePolicy(
                        hourSize = hourSize,
                        currentDayOfMinutes = currentDayOfMinutes,
                        calcTimeEvents = calcTimeEvents,
                    )
                },
            )
        }
    }
}

@Composable
private fun EventDialogContent(
    event: CalendarLayoutUiState.Event,
    onDismissRequest: () -> Unit,
) {
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
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))
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

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onDismissRequest,
            ) {
                Text(text = "CLOSE")
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
    // 時間の高さをの1/4を1としたサイズ。最低の高さは2
    val heightCount: Int,
    // 時間の高さをの1/4を1としたサイズの0時からの開始位置
    val startIndex: Int,
    val uiState: CalendarLayoutUiState.Event.Time,
    // その時間帯が何個で分割されているか
    val rowSplitSize: Int,
    val rowIndex: Int,
)

private data class CalendarMeasurePolicy(
    val hourSize: Dp,
    val currentDayOfMinutes: Int,
    val calcTimeEvents: List<CalcTimeEvent>,
) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val hourPlaceableList = (0 until 24).map { index ->
            measurables[hourIndex(index)].measure(constraints)
        }
        val hourMaxWidth = hourPlaceableList.maxOf { it.width }
        val hourAverageHeightPx = hourPlaceableList.sumOf { it.height } / 24

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
            hourDividerPlaceableList.forEachIndexed { index, placeable ->
                placeable.place(
                    x = hourMaxWidth,
                    y = (hourSize * index).roundToPx() + (hourAverageHeightPx / 2),
                )
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
        onClick = onClick,
    )
}

@Composable
private fun EventCard(
    title: String,
    displayTime: String?,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = color,
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
        ) {
            BasicText(
                text = title,
                autoSize = TextAutoSize.StepBased(
                    maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
                ),
            )
            if (displayTime != null) {
                BasicText(
                    text = remember(displayTime) {
                        htmlToAnnotatedString(displayTime)
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
    EventDialogContent(
        event = CalendarLayoutUiState.Event.Time(
            startTime = LocalTime.of(1, 0),
            endTime = LocalTime.of(2, 0),
            title = "Sample Event",
            displayTime = "01:00 - 02:00",
            description = "This is a sample event description.",
            color = Color.Blue,
        ),
        onDismissRequest = {},
    )
}

@Composable
@Preview(showBackground = true)
private fun Preview() {
    val clock = remember {
        val zone = ZoneId.of("UTC")
        Clock.fixed(
            LocalDateTime.of(
                LocalDate.of(2010, 10, 10),
                LocalTime.of(1, 30, 0),
            ).toInstant(ZoneOffset.ofHours(0)),
            zone,
        )
    }
    MaterialTheme {
        CalendarLayout(
            uiState = CalendarLayoutUiState(
                events = listOf(
                    CalendarLayoutUiState.Event.Time(
                        startTime = LocalTime.of(0, 0),
                        endTime = LocalTime.of(1, 0),
                        title = "One",
                        displayTime = "00:00 - 01:00",
                        description = "description",
                        color = Color.Red,
                    ),
                    CalendarLayoutUiState.Event.Time(
                        startTime = LocalTime.of(1, 0),
                        endTime = LocalTime.of(3, 0),
                        title = "Two",
                        displayTime = "01:00 - 03:00",
                        description = "description",
                        color = Color.Blue,
                    ),
                    CalendarLayoutUiState.Event.Time(
                        startTime = LocalTime.of(1, 0),
                        endTime = LocalTime.of(2, 0),
                        title = "Three",
                        displayTime = "01:00 - 02:00",
                        description = "description",
                        color = Color.Yellow,
                    ),
                    CalendarLayoutUiState.Event.Time(
                        startTime = LocalTime.of(1, 30),
                        endTime = LocalTime.of(3, 0),
                        title = "Four",
                        displayTime = "01:30 - 03:00",
                        description = "description",
                        color = Color.Green,
                    ),
                    CalendarLayoutUiState.Event.Time(
                        startTime = LocalTime.of(2, 0),
                        endTime = LocalTime.of(3, 0),
                        title = "Five",
                        displayTime = "02:00 - 03:00",
                        description = "description",
                        color = Color.Magenta,
                    ),
                    CalendarLayoutUiState.Event.Time(
                        startTime = LocalTime.of(3, 15),
                        endTime = LocalTime.of(3, 30),
                        title = "Six",
                        displayTime = "03:15 - 03:30",
                        description = "description",
                        color = Color.Cyan,
                    ),
                ),
                allDayEvents = listOf(
                    CalendarLayoutUiState.Event.AllDay(
                        title = "All Day Event",
                        description = "Description",
                        color = Color.Red,
                    ),
                    CalendarLayoutUiState.Event.AllDay(
                        title = "All Day Event 2",
                        description = "Description",
                        color = Color.Yellow,
                    ),
                ),
            ),
            clock = clock,
        )
    }
}
