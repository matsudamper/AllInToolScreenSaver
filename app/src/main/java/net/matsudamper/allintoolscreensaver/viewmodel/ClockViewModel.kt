package net.matsudamper.allintoolscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.ui.compose.ClockUiState

class ClockViewModel(
    clock: Clock,
) : ViewModel() {
    val uiStateFlow: StateFlow<ClockUiState> = MutableStateFlow(
        ClockUiState(
            timeText = getCurrentTime(),
            dateText = getCurrentDate(),
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            while (isActive) {
                uiStateFlow.update { uiState ->
                    ClockUiState(
                        timeText = getCurrentTime(),
                        dateText = getCurrentDate(),
                    )
                }

                val now = Instant.now(clock)
                val delayTime = 1.seconds - (now.toEpochMilli() % 1000).milliseconds
                delay(delayTime.coerceAtLeast(1.milliseconds))
            }
        }
    }.asStateFlow()

    private fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy年MM月dd日(E)", Locale.JAPANESE)
        return formatter.format(Date())
    }
}
