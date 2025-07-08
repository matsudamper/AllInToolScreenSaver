package net.matsudamper.allintoolscreensaver.ui.calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import java.time.Clock
import java.time.LocalTime
import kotlin.math.roundToInt
import net.matsudamper.allintoolscreensaver.ui.LocalClock

@Stable
class CalendarState internal constructor(
    val scrollState: ScrollState,
    private val density: Density,
    initialHourSize: Dp,
    private val clock: Clock,
) {
    var verticalPadding by mutableIntStateOf(0)
    var hourSize: Dp by mutableStateOf(initialHourSize)

    fun isCurrentTimeDisplayed(): Boolean {
        val hourHeightPx = with(density) { hourSize.roundToPx() }
        val now = LocalTime.now(clock)
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
    val clock = LocalClock.current
    return remember(scrollState, density, clock) {
        CalendarState(
            scrollState = scrollState,
            density = density,
            initialHourSize = initialHourSize,
            clock = clock,
        )
    }
}
