package net.matsudamper.allintoolscreensaver

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlertManager(
    private val calendarRepository: CalendarRepository,
    private val settingsRepository: SettingsRepository,
) {
    private var alertJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private val alertScope = CoroutineScope(Dispatchers.Main)
    private val alreadyTriggeredEvents = mutableSetOf<Long>()

    var onAlertTriggered: ((CalendarEvent) -> Unit)? = null

    init {
        initializeToneGenerator()
    }

    private fun initializeToneGenerator() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (_: RuntimeException) {
            // ToneGeneratorの初期化に失敗した場合
        }
    }

    fun startAlertMonitoring() {
        stopAlertMonitoring()
        alertJob = alertScope.launch {
            while (isActive) {
                checkUpcomingEvents()
                delay(10_000L)
            }
        }
    }

    private suspend fun checkUpcomingEvents() {
        val selectedCalendarIds = settingsRepository.getSelectedCalendarIds()

        if (selectedCalendarIds.isEmpty()) return

        val now = Instant.now()
        val endTime = now.plusSeconds(60)

        val events = calendarRepository.getEventsForTimeRange(
            calendarIds = selectedCalendarIds,
            startTime = now.minusSeconds(60),
            endTime = endTime,
        )

        events.filterIsInstance<CalendarEvent.Time>().forEach { event ->
            if (event.id !in alreadyTriggeredEvents &&
                event.startTime <= now.plusSeconds(30) &&
                event.startTime > now.minusSeconds(30)
            ) {
                alreadyTriggeredEvents.add(event.id)
                triggerAlert(event)
            }
        }

        alreadyTriggeredEvents.removeAll { eventId ->
            events.none { it.id == eventId }
        }
    }

    private fun triggerAlert(event: CalendarEvent) {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
        onAlertTriggered?.invoke(event)
    }

    fun stopAlertMonitoring() {
        alertJob?.cancel()
        alertJob = null
    }

    fun cleanup() {
        stopAlertMonitoring()
        alertScope.cancel()
        toneGenerator?.release()
        toneGenerator = null
    }
}
