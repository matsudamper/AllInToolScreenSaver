package net.matsudamper.allintoolscreensaver.viewmodel

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.AlertManager
import net.matsudamper.allintoolscreensaver.CalendarEvent
import net.matsudamper.allintoolscreensaver.CalendarRepository
import net.matsudamper.allintoolscreensaver.SettingsRepository
import net.matsudamper.allintoolscreensaver.compose.calendar.CalendarDisplayScreenUiState
import net.matsudamper.allintoolscreensaver.compose.calendar.CalendarLayoutUiState
import org.koin.java.KoinJavaComponent.inject

class CalendarDisplayScreenViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
) : AndroidViewModel(application) {
    private val context get() = application.applicationContext

    private val alertManager: AlertManager by inject(AlertManager::class.java)
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val listener = object : CalendarDisplayScreenUiState.Listener {
        override suspend fun onStart() {
            coroutineScope {
                launch {
                    startAlertMonitoring()
                }
                launch {
                    fetchEvent()
                }
                launch { observeCalendarChanges() }
            }
        }

        override fun onInteraction() {
            viewModelStateFlow.update { state ->
                state.copy(lastInteractionTime = Instant.now())
            }
        }

        override fun onAlertDismiss() {
            viewModelStateFlow.update { state ->
                state.copy(currentAlert = null)
            }
        }
    }
    private val operationFlow = Channel<(CalendarDisplayScreenUiState.Operation) -> Unit>(Channel.UNLIMITED)
    val uiState: StateFlow<CalendarDisplayScreenUiState> = MutableStateFlow(
        CalendarDisplayScreenUiState(
            calendarUiState = CalendarLayoutUiState(
                events = listOf(),
                allDayEvents = listOf(),
            ),
            currentAlert = null,
            listener = listener,
            operationFlow = operationFlow,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        calendarUiState = CalendarLayoutUiState(
                            events = viewModelState.events.filterIsInstance<CalendarEvent.Time>()
                                .map { event ->
                                    CalendarLayoutUiState.Event.Time(
                                        startTime = LocalTime.ofInstant(event.startTime, ZoneId.systemDefault()),
                                        endTime = LocalTime.ofInstant(event.endTime, ZoneId.systemDefault()),
                                        title = event.title,
                                        description = createDisplayTime(event.startTime, event.endTime),
                                        color = Color(event.color),
                                    )
                                },
                            allDayEvents = viewModelState.events.filterIsInstance<CalendarEvent.AllDay>()
                                .map { event ->
                                    CalendarLayoutUiState.Event.AllDay(
                                        title = event.title,
                                        description = event.description,
                                        color = Color(event.color),
                                    )
                                },
                        ),
                        currentAlert = viewModelState.currentAlert,
                    )
                }
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.map { it.lastInteractionTime }
                .distinctUntilChanged()
                .collectLatest { _ ->
                    delay(1.minutes)
                    operationFlow.send {
                        it.moveCurrentTime()
                    }
                }
        }
    }.asStateFlow()

    private fun createDisplayTime(start: Instant, end: Instant): String {
        val startTime = LocalTime.ofInstant(start, ZoneId.systemDefault())
        val endTime = LocalTime.ofInstant(end, ZoneId.systemDefault())
        fun Int.padding(): String = toString().padStart(2, '0')
        return "${startTime.hour.padding()}:${startTime.minute.padding()} - ${endTime.hour.padding()}:${endTime.minute.padding()}"
    }

    private fun startAlertMonitoring() {
        alertManager.onAlertTriggered = { event ->
            viewModelStateFlow.update { state ->
                state.copy(currentAlert = event)
            }
        }
        alertManager.startAlertMonitoring()
    }

    private fun fetchEvent() {
        viewModelScope.launch {
            val selectedCalendarIds = settingsRepository.getSelectedCalendarIds()

            if (selectedCalendarIds.isNotEmpty()) {
                val today = LocalDate.now()
                val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

                val events = calendarRepository.getEventsForTimeRange(selectedCalendarIds, startOfDay, endOfDay)
                viewModelStateFlow.update { state ->
                    state.copy(
                        events = events,
                    )
                }
            }
        }
    }

    private suspend fun observeCalendarChanges() {
        coroutineScope {
            launch {
                val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
                        fetchEvent()
                    }
                }
                context.contentResolver.registerContentObserver(
                    CalendarContract.Calendars.CONTENT_URI,
                    true,
                    observer,
                )
                runCatching {
                    awaitCancellation()
                }
                context.contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        alertManager.cleanup()
    }

    private data class ViewModelState(
        val events: List<CalendarEvent> = listOf(),
        val lastInteractionTime: Instant = Instant.now(),
        val currentAlert: CalendarEvent? = null,
    )
}
