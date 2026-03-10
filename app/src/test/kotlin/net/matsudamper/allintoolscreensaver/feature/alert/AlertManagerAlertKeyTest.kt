package net.matsudamper.allintoolscreensaver.feature.alert

import java.time.Instant
import net.matsudamper.allintoolscreensaver.feature.calendar.AttendeeStatus
import net.matsudamper.allintoolscreensaver.feature.calendar.CalendarRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AlertManagerAlertKeyTest {

    /**
     * 同一のイベントIDを持つ繰り返し予定でも、発生日時が異なれば
     * 通知キーは別物として扱われることを確認する。
     */
    @Test
    fun recurringEventWithSameEventIdHasDifferentAlertKeyPerOccurrence() {
        val alertKey = AlertManager.AlertKey()
        val firstOccurrence = createTimeEvent(
            id = 42,
            startTime = Instant.parse("2026-01-01T23:59:00Z"),
        )
        val nextDayOccurrence = createTimeEvent(
            id = 42,
            startTime = Instant.parse("2026-01-02T23:59:00Z"),
        )

        val firstKey = alertKey.create(firstOccurrence, AlertManager.AlertType.EVENT_TIME)
        val nextDayKey = alertKey.create(nextDayOccurrence, AlertManager.AlertType.EVENT_TIME)

        assertNotEquals(firstKey, nextDayKey)
    }

    /**
     * 生成済みの通知キーから、発生単位で一意なイベント識別子を
     * 正しく取り出せることを確認する。
     */
    @Test
    fun parseEventIdentifierReturnsOccurrenceScopedIdentifier() {
        val alertKey = AlertManager.AlertKey()
        val event = createTimeEvent(
            id = 99,
            startTime = Instant.parse("2026-02-01T00:00:00Z"),
        )

        val key = alertKey.create(event, AlertManager.AlertType.ONE_MINUTE_BEFORE)

        assertEquals("99@1769904000", alertKey.parseEventIdentifier(key))
    }

    private fun createTimeEvent(id: Long, startTime: Instant): CalendarRepository.CalendarEvent.Time {
        return CalendarRepository.CalendarEvent.Time(
            id = id,
            calendarId = 1,
            title = "event",
            description = null,
            color = 0,
            attendeeStatus = AttendeeStatus.ACCEPTED,
            startTime = startTime,
            endTime = startTime.plusSeconds(600),
        )
    }
}
