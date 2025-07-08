package net.matsudamper.allintoolscreensaver.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.ui.CalendarState

class TimeRangeSliderState(
    private val density: Density,
    private val coroutineScope: CoroutineScope,
    private val calendarState: CalendarState,
    private val minRange: Float = 6 / 24f,
) {
    var rangeStartRatio by mutableFloatStateOf(0f)
        private set
    var rangeEndRatio by mutableFloatStateOf(0f)
        private set

    private val mutableSliderStateChanged = Channel<SliderStateChanged>(Channel.UNLIMITED)
    val sliderStateChanged: Flow<SliderStateChanged> = mutableSliderStateChanged.receiveAsFlow()

    private var dragMode by mutableStateOf(DragMode.NONE)
        private set

    private val handleWidth = 42.dp

    init {
        coroutineScope.launch {
            snapshotFlow {
                listOf(
                    calendarState.scrollState.value,
                    calendarState.scrollState.maxValue,
                    calendarState.scrollState.viewportSize,
                    calendarState.hourSize,
                )
            }.collectLatest {
                updateRange(
                    calendarScrollValue = calendarState.scrollState.value,
                    calendarScrollMaxValue = calendarState.scrollState.maxValue,
                    calendarHourHeight = calendarState.hourSize,
                    calendarScrollViewPortSize = calendarState.scrollState.viewportSize,
                )
            }
        }
    }

    fun onDragStart(offset: Offset, size: Size) {
        dragMode = with(density) {
            val handleWidthRatio = handleWidth.toPx() / size.width
            when (offset.x / size.width) {
                in (rangeStartRatio - handleWidthRatio / 2)..(rangeStartRatio + handleWidthRatio / 2) -> DragMode.START
                in (rangeEndRatio - handleWidthRatio / 2)..(rangeEndRatio + handleWidthRatio / 2) -> DragMode.END
                in rangeStartRatio..rangeEndRatio -> DragMode.MIDDLE
                else -> DragMode.NONE
            }
        }
    }

    fun onDrag(dragAmount: Offset, size: Size) {
        val dragRatio = dragAmount.x / size.width
        when (dragMode) {
            DragMode.START -> onDragStart(dragRatio = dragRatio)
            DragMode.END -> onDragEnd(dragRatio = dragRatio)
            DragMode.MIDDLE -> onDragMiddle(dragRatio = dragRatio)
            DragMode.NONE -> Unit
        }
    }

    private fun onDragMiddle(dragRatio: Float) {
        val rangeSize = rangeEndRatio - rangeStartRatio
        var newStart = rangeStartRatio + dragRatio
        var newEnd = rangeEndRatio + dragRatio

        if (newStart < 0f) {
            newStart = 0f
            newEnd = rangeSize
        } else if (newEnd > 1f) {
            newEnd = 1f
            newStart = 1f - rangeSize
        }

        if (newStart == rangeStartRatio) return
        rangeStartRatio = newStart
        rangeEndRatio = newEnd

        val targetHour = rangeStartRatio * 24f
        val newScrollPosition = with(density) { (calendarState.hourSize * targetHour).roundToPx() }
        coroutineScope.launch {
            calendarState.scrollState.scrollTo(newScrollPosition)
        }
    }

    private fun onDragStart(
        dragRatio: Float,
    ) {
        if (rangeStartRatio + dragRatio < 0f) {
            rangeStartRatio = 0f
        } else if (rangeEndRatio - (rangeStartRatio + dragRatio) < minRange) {
            if (rangeEndRatio + dragRatio <= 1f) {
                rangeEndRatio += dragRatio
                rangeStartRatio += dragRatio
            } else {
                rangeEndRatio = 1f
                rangeStartRatio = 1f - minRange
            }
        } else {
            rangeStartRatio += dragRatio
        }

        val targetHour = rangeStartRatio * 24f
        val newScrollPosition = with(density) { (calendarState.hourSize * targetHour).roundToPx() }
        coroutineScope.launch {
            updateHourSize()
            calendarState.scrollState.scrollTo(newScrollPosition)
        }
    }

    private fun onDragEnd(dragRatio: Float) {
        if (rangeEndRatio + dragRatio > 1f) {
            rangeEndRatio = 1f
        } else if ((rangeEndRatio + dragRatio) - rangeStartRatio < minRange) {
            if (rangeStartRatio + dragRatio >= 0f) {
                rangeEndRatio += dragRatio
                rangeStartRatio += dragRatio
            } else {
                rangeStartRatio = 0f
                rangeEndRatio = minRange
            }
        } else {
            rangeEndRatio += dragRatio
        }

        val targetHour = rangeEndRatio * 24f
        val viewportSizeInHours = with(density) {
            calendarState.scrollState.viewportSize.toFloat() / calendarState.hourSize.toPx()
        }
        val endScrollHour = targetHour - viewportSizeInHours
        val newScrollPosition = with(density) {
            (calendarState.hourSize * endScrollHour).roundToPx()
        }.coerceIn(0, calendarState.scrollState.maxValue)
        coroutineScope.launch {
            updateHourSize()
            calendarState.scrollState.scrollTo(newScrollPosition)
        }
    }

    fun onDragEnd() {
        dragMode = DragMode.NONE
        mutableSliderStateChanged.trySend(
            SliderStateChanged(
                rangeStart = rangeStartRatio,
                rangeEnd = rangeEndRatio,
                isUserInteraction = true,
            ),
        )
    }

    fun getHandleWidthPx(): Float = with(density) { handleWidth.toPx() }

    private var size by mutableStateOf(IntSize.Zero)
    fun updateSize(size: IntSize) {
        this.size = size
    }

    private fun updateHourSize() {
        calendarState.hourSize = with(density) {
            val displayRatio = rangeEndRatio - rangeStartRatio
            run {
                val displayHour = 24 * displayRatio
                calendarState.scrollState.viewportSize / displayHour
            }.toDp()
        }
    }

    private fun updateRange(
        calendarScrollValue: Int,
        calendarScrollMaxValue: Int,
        calendarScrollViewPortSize: Int,
        calendarHourHeight: Dp,
    ) {
        if (dragMode != DragMode.NONE) return

        with(density) {
            val calendarScrollRatio = calendarScrollValue.toFloat() / (calendarScrollMaxValue + calendarScrollViewPortSize)

            rangeStartRatio = calendarScrollRatio
            rangeEndRatio = calendarScrollRatio + (calendarScrollViewPortSize / calendarHourHeight.toPx() / 24f)
        }

        mutableSliderStateChanged.trySend(
            SliderStateChanged(
                rangeStart = rangeStartRatio,
                rangeEnd = rangeEndRatio,
                isUserInteraction = false,
            ),
        )
    }

    data class SliderStateChanged(
        val rangeStart: Float,
        val rangeEnd: Float,
        val isUserInteraction: Boolean,
    )
}

@Composable
fun rememberTimeRangeSlider(
    calendarState: CalendarState = rememberCalendarState(),
): TimeRangeSliderState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    return remember(density, coroutineScope, calendarState) {
        TimeRangeSliderState(
            density = density,
            coroutineScope = coroutineScope,
            calendarState = calendarState,
        )
    }
}

@Composable
fun TimeRangeSlider(
    items: List<TimeRangeSliderItem>,
    modifier: Modifier = Modifier,
    state: TimeRangeSliderState = rememberTimeRangeSlider(),
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryColorAlpha = primaryColor.copy(alpha = 0.7f)
    val backgroundColor = Color.LightGray.copy(alpha = 0.5f)
    val markerColor = Color.Black
    val eventMarkerColor = Color.Red

    Canvas(
        modifier = modifier
            .onSizeChanged {
                state.updateSize(
                    size = it,
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        state.onDragStart(offset, Size(size.width.toFloat(), size.height.toFloat()))
                    },
                    onDrag = { _, dragAmount ->
                        state.onDrag(dragAmount, Size(size.width.toFloat(), size.height.toFloat()))
                    },
                    onDragEnd = {
                        state.onDragEnd()
                    },
                )
            },
    ) {
        drawRect(
            color = backgroundColor,
            size = size,
        )

        // center
        drawRect(
            color = primaryColorAlpha,
            topLeft = Offset(size.width * state.rangeStartRatio, 0f),
            size = Size(size.width * (state.rangeEndRatio - state.rangeStartRatio), size.height),
        )

        val handleWidthPx = state.getHandleWidthPx()

        if (state.rangeEndRatio != 0f) {
            // start
            drawRect(
                color = primaryColor,
                topLeft = Offset(size.width * state.rangeStartRatio, 0f),
                size = Size(handleWidthPx, size.height),
            )

            // end
            drawRect(
                color = primaryColor,
                topLeft = Offset(size.width * state.rangeEndRatio - handleWidthPx, 0f),
                size = Size(handleWidthPx, size.height),
            )
        }

        for (hour in 0..24) {
            val x = size.width * (hour / 24f)
            drawLine(
                color = markerColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
        }

        items.forEach { event ->
            val startTimeRatio = (event.startTime.hour + event.startTime.minute / 60f) / 24f
            val x = size.width * startTimeRatio

            drawCircle(
                color = eventMarkerColor,
                radius = 5f,
                center = Offset(x, size.height / 2),
            )
        }
    }
}

internal enum class DragMode {
    NONE,
    START,
    END,
    MIDDLE,
}

@Composable
@Preview
private fun Preview() {
    TimeRangeSlider(
        items = listOf(
            TimeRangeSliderItem(
                startTime = LocalTime.of(10, 0, 0),
            ),
            TimeRangeSliderItem(
                startTime = LocalTime.of(20, 0, 0),
            ),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        state = rememberTimeRangeSlider(),
    )
}

data class TimeRangeSliderItem(
    val startTime: LocalTime,
)
