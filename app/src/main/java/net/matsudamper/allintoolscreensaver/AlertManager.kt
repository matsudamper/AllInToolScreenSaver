package net.matsudamper.allintoolscreensaver

import android.app.Application
import android.media.Ringtone
import android.media.RingtoneManager
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.AttendeeStatus
import net.matsudamper.allintoolscreensaver.compose.eventalert.AlertType

class AlertManager(
    private val calendarRepository: CalendarRepository,
    private val settingsRepository: SettingsRepository,
    private val application: Application,
    private val clock: Clock,
) {
    private var ringtone: Ringtone? = null
    private val activeAlerts = mutableMapOf<String, AlertInfo>()
    private val repeatingAlerts = mutableMapOf<String, Instant>()
    private val dismissedAlerts = mutableSetOf<String>()
    private val alertKey = AlertKey()

    var onAlertTriggered: ((CalendarEvent, AlertType, LocalTime, Boolean) -> Unit)? = null
    var onScreenSaverAlertTriggered: ((CalendarEvent, AlertType, LocalTime, Boolean) -> Unit)? = null
    private var isDreamServiceActive = false

    suspend fun startAlertMonitoring() {
        coroutineScope {
            ringtone = RingtoneManager.getRingtone(
                application,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            )
            launch {
                while (isActive) {
                    if (settingsRepository.settingsFlow.first().alertEnabled) {
                        checkUpcomingEvents()
                        checkRepeatingAlerts()
                    }
                    delay(10.seconds)
                }
            }
        }
    }

    private suspend fun checkUpcomingEvents() {
        val alertCalendarIds = settingsRepository.settingsFlow.first().alertCalendarIdsList
        if (alertCalendarIds.isEmpty()) return

        val now = Instant.now(clock)
        val endTime = now.plusSeconds(6 * 60)

        val events = calendarRepository.getEventsForTimeRange(
            calendarIds = alertCalendarIds,
            startTime = now.minusSeconds(60),
            endTime = endTime,
        )

        events.filterIsInstance<CalendarEvent.Time>().forEach { event ->
            checkAlertForEvent(event, now)
        }

        cleanupOldAlerts(events)
    }

    private fun checkAlertForEvent(event: CalendarEvent.Time, now: Instant) {
        if (event.attendeeStatus == AttendeeStatus.DECLINED) {
            return
        }

        val eventStartTime = event.startTime

        AlertType.entries.forEach { alertType ->
            val alertTime = eventStartTime.minusSeconds((alertType.minutesBefore * 60).toLong())
            val alertKeyValue = alertKey.create(event, alertType)

            if (shouldTriggerAlert(alertKeyValue, alertTime, now)) {
                val alertInfo = AlertInfo(
                    event = event,
                    alertType = alertType,
                    triggeredAt = now,
                    isRepeating = alertType == AlertType.EVENT_TIME,
                )
                activeAlerts[alertKeyValue] = alertInfo

                if (alertType == AlertType.EVENT_TIME) {
                    repeatingAlerts[alertKeyValue] = now
                }

                triggerAlert(event, alertType)
            }
        }
    }

    private fun shouldTriggerAlert(alertKeyValue: String, alertTime: Instant, now: Instant): Boolean {
        return alertKeyValue !in activeAlerts &&
            alertKeyValue !in dismissedAlerts &&
            now.isAfter(alertTime.minusSeconds(30)) &&
            now.isBefore(alertTime.plusSeconds(30))
    }

    private fun checkRepeatingAlerts() {
        val now = Instant.now(clock)
        val toRemove = mutableListOf<String>()

        repeatingAlerts.forEach { (alertKeyValue, startTime) ->
            val elapsedTime = now.epochSecond - startTime.epochSecond

            if (elapsedTime >= 5 * 60) {
                toRemove.add(alertKeyValue)
            } else if (elapsedTime % 10 == 0L && elapsedTime > 0) {
                val alertInfo = activeAlerts[alertKeyValue]
                if (alertInfo != null) {
                    triggerAlert(alertInfo.event, alertInfo.alertType, isRepeating = true)
                }
            }
        }

        toRemove.forEach { key ->
            repeatingAlerts.remove(key)
        }
    }

    private fun cleanupOldAlerts(currentEvents: List<CalendarEvent>) {
        val currentEventIds = currentEvents.map { it.id }.toSet()
        val keysToRemove = activeAlerts.keys.filter { key ->
            val eventId = alertKey.parseEventId(key)
            eventId !in currentEventIds
        }

        keysToRemove.forEach { key ->
            activeAlerts.remove(key)
            repeatingAlerts.remove(key)
        }

        val dismissedKeysToRemove = dismissedAlerts.filter { key ->
            val eventId = alertKey.parseEventId(key)
            eventId !in currentEventIds
        }

        dismissedKeysToRemove.forEach { key ->
            dismissedAlerts.remove(key)
        }
    }

    private fun triggerAlert(event: CalendarEvent, alertType: AlertType, isRepeating: Boolean = false) {
        ringtone?.play()

        val eventStartTime = (event as CalendarEvent.Time).startTime.atZone(ZoneId.systemDefault()).toLocalTime()

        if (isDreamServiceActive) {
            onScreenSaverAlertTriggered?.invoke(event, alertType, eventStartTime, isRepeating)
        } else {
            onAlertTriggered?.invoke(event, alertType, eventStartTime, isRepeating)
        }
    }

    fun setDreamServiceActive(isActive: Boolean) {
        isDreamServiceActive = isActive
    }

    fun dismissAlert(event: CalendarEvent, alertType: AlertType) {
        val alertKeyValue = alertKey.create(event, alertType)
        activeAlerts.remove(alertKeyValue)
        repeatingAlerts.remove(alertKeyValue)
        dismissedAlerts.add(alertKeyValue)
    }

    data class AlertInfo(
        val event: CalendarEvent,
        val alertType: AlertType,
        val triggeredAt: Instant,
        val isRepeating: Boolean = false,
    )

    class AlertKey {
        fun create(event: CalendarEvent, alertType: AlertType): String {
            return "${event.id}_${alertType.name}"
        }

        fun parseEventId(alertKey: String): Long? {
            return alertKey.split("_")[0].toLongOrNull()
        }
    }
}
