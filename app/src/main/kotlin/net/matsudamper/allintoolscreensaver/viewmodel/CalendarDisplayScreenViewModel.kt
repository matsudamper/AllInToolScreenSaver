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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.feature.calendar.AttendeeStatus
import net.matsudamper.allintoolscreensaver.feature.calendar.CalendarRepository
import net.matsudamper.allintoolscreensaver.feature.setting.SettingsRepository
import net.matsudamper.allintoolscreensaver.ui.calendar.CalendarDisplayScreenUiState
import net.matsudamper.allintoolscreensaver.ui.calendar.CalendarLayoutUiState

class CalendarDisplayScreenViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    private val clock: Clock,
) : AndroidViewModel(application) {
    private val context get() = application.applicationContext
    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            lastInteractionTime = Instant.now(clock),
        ),
    )

    private val listener = object : CalendarDisplayScreenUiState.Listener {
        override suspend fun onStart() {
            coroutineScope {
                launch {
                    fetchEvent()
                }
                launch { observeCalendarChanges() }
                launch { observeAlertSettings() }
                launch { observeDateChanges() }
            }
        }

        override fun onInteraction() {
            viewModelStateFlow.update { state ->
                state.copy(lastInteractionTime = Instant.now(clock))
            }
        }

        override fun onAlertEnabledChanged(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.saveAlertEnabled(enabled)
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
            alertEnabled = false,
            listener = listener,
            operationFlow = operationFlow,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        calendarUiState = CalendarLayoutUiState(
                            events = viewModelState.events.filterIsInstance<CalendarRepository.CalendarEvent.Time>()
                                .map { event ->
                                    CalendarLayoutUiState.Event.Time(
                                        startTime = LocalTime.ofInstant(event.startTime, ZoneId.systemDefault()),
                                        endTime = LocalTime.ofInstant(event.endTime, ZoneId.systemDefault()),
                                        title = event.title,
                                        displayTime = createDisplayTime(event.startTime, event.endTime),
                                        color = Color(event.color),
                                        description = event.description,
                                        isTransparent = convertAttendeeStatusToTransparent(event.attendeeStatus),
                                        showBorder = convertAttendeeStatusToShowBorder(event.attendeeStatus),
                                        hasTextDecoration = convertAttendeeStatusToTextDecoration(event.attendeeStatus),
                                    )
                                },
                            allDayEvents = viewModelState.events.filterIsInstance<CalendarRepository.CalendarEvent.AllDay>()
                                .map { event ->
                                    CalendarLayoutUiState.Event.AllDay(
                                        title = event.title,
                                        description = event.description,
                                        color = Color(event.color),
                                        isTransparent = convertAttendeeStatusToTransparent(event.attendeeStatus),
                                        showBorder = convertAttendeeStatusToShowBorder(event.attendeeStatus),
                                        hasTextDecoration = convertAttendeeStatusToTextDecoration(event.attendeeStatus),
                                    )
                                },
                        ),
                        alertEnabled = viewModelState.alertEnabled,
                    )
                }
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.map { it.lastInteractionTime }
                .distinctUntilChanged()
                .collectLatest { _ ->
                    delay(1.minutes)
                    while (isActive) {
                        operationFlow.send {
                            it.moveCurrentTime()
                        }
                        delay(5.minutes)
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

    private fun fetchEvent() {
        viewModelScope.launch {
            val selectedCalendarIds = settingsRepository.settingsFlow.first().selectedCalendarIdsList

            if (selectedCalendarIds.isNotEmpty()) {
                val today = LocalDate.now(clock)
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

    private suspend fun observeAlertSettings() {
        settingsRepository.getAlertEnabledFlow().collectLatest { alertEnabled ->
            viewModelStateFlow.update { state ->
                state.copy(alertEnabled = alertEnabled)
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

    private suspend fun observeDateChanges() {
        while (currentCoroutineContext().isActive) {
            val now = LocalDateTime.now(clock.withZone(ZoneId.systemDefault()))
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val delayUntilMidnight = Duration.between(now, nextMidnight)

            delay(delayUntilMidnight.toMillis())
            delay(1.seconds)

            fetchEvent()
        }
    }

    private data class ViewModelState(
        val events: List<CalendarRepository.CalendarEvent> = listOf(),
        val lastInteractionTime: Instant,
        val alertEnabled: Boolean = false,
    )

    private fun convertAttendeeStatusToTransparent(attendeeStatus: AttendeeStatus): Boolean {
        return when (attendeeStatus) {
            AttendeeStatus.TENTATIVE,
            AttendeeStatus.DECLINED,
            -> true

            AttendeeStatus.NONE,
            AttendeeStatus.ACCEPTED,
            AttendeeStatus.INVITED,
            -> false
        }
    }

    private fun convertAttendeeStatusToShowBorder(attendeeStatus: AttendeeStatus): Boolean {
        return when (attendeeStatus) {
            AttendeeStatus.TENTATIVE,
            AttendeeStatus.DECLINED,
            -> true

            AttendeeStatus.NONE,
            AttendeeStatus.ACCEPTED,
            AttendeeStatus.INVITED,
            -> false
        }
    }

    private fun convertAttendeeStatusToTextDecoration(attendeeStatus: AttendeeStatus): Boolean {
        return when (attendeeStatus) {
            AttendeeStatus.DECLINED,
            -> true

            AttendeeStatus.TENTATIVE,
            AttendeeStatus.NONE,
            AttendeeStatus.ACCEPTED,
            AttendeeStatus.INVITED,
            -> false
        }
    }
}
