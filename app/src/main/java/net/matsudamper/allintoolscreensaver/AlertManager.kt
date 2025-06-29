package net.matsudamper.allintoolscreensaver

import android.media.AudioManager
import android.media.ToneGenerator
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlertManager(
    private val calendarRepository: CalendarRepository,
    private val settingsRepository: SettingsRepository,
) {
    private var toneGenerator: ToneGenerator? = null
    private val alreadyTriggeredEvents = mutableSetOf<Long>()

    var onAlertTriggered: ((CalendarEvent) -> Unit)? = null

    suspend fun startAlertMonitoring() {
        coroutineScope {
            toneGenerator = runCatching {
                ToneGenerator(AudioManager.STREAM_RING, 100)
            }.getOrNull()
            launch {
                while (isActive) {
                    checkUpcomingEvents()
                    delay(10.seconds)
                }
            }
            launch {
                runCatching { awaitCancellation() }
                toneGenerator?.release()
                toneGenerator = null
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
                event.startTime.isBefore(now.plusSeconds(30)) &&
                event.startTime.isAfter(now.minusSeconds(30))
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
}
