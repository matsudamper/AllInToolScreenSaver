package net.matsudamper.allintoolscreensaver

import android.app.Application
import android.media.Ringtone
import android.media.RingtoneManager
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlertManager(
    private val calendarRepository: CalendarRepository,
    private val settingsRepository: SettingsRepository,
    private val inMemoryCache: InMemoryCache,
    private val application: Application,
) {
    private var ringtone: Ringtone? = null
    var onAlertTriggered: ((CalendarEvent) -> Unit)? = null

    suspend fun startAlertMonitoring() {
        coroutineScope {
            ringtone = RingtoneManager.getRingtone(
                application,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            )
            launch {
                while (isActive) {
                    checkUpcomingEvents()
                    delay(10.seconds)
                }
            }
        }
    }

    private suspend fun checkUpcomingEvents() {
        val selectedCalendarIds = settingsRepository.getSelectedCalendarIds()

        if (selectedCalendarIds.isEmpty()) return

        val now = Instant.now()
        val endTime = now.plusSeconds(60)

        val targetEvent = calendarRepository.getEventsForTimeRange(
            calendarIds = selectedCalendarIds,
            startTime = now.minusSeconds(60),
            endTime = endTime,
        )

        targetEvent.filterIsInstance<CalendarEvent.Time>().forEach { event ->
            if (event.id !in inMemoryCache.alreadyTriggeredEvents &&
                event.startTime.isBefore(now.plusSeconds(30)) &&
                event.startTime.isAfter(now.minusSeconds(30))
            ) {
                inMemoryCache.alreadyTriggeredEvents.add(event.id)
                triggerAlert(event)
            }
        }

        inMemoryCache.alreadyTriggeredEvents.removeAll { eventId ->
            targetEvent.none { it.id == eventId }
        }
    }

    private fun triggerAlert(event: CalendarEvent) {
        ringtone?.play()
        onAlertTriggered?.invoke(event)
    }
}
