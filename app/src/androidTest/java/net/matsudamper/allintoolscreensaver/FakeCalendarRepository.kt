package net.matsudamper.allintoolscreensaver

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FakeCalendarRepository : CalendarRepository {
    private val fakeCalendars = mutableListOf<CalendarInfo>()
    private val fakeEvents = mutableListOf<CalendarEvent>()

    fun addCalendar(calendar: CalendarInfo) {
        fakeCalendars.add(calendar)
    }

    fun addEvent(event: CalendarEvent) {
        fakeEvents.add(event)
    }

    override suspend fun getAvailableCalendars(): List<CalendarInfo> {
        return fakeCalendars.toList()
    }

    override suspend fun getEventsForTimeRange(
        calendarIds: List<Long>,
        startTime: Instant,
        endTime: Instant,
    ): List<CalendarEvent> {
        return fakeEvents.filter { event ->
            calendarIds.contains(event.calendarId) &&
                event.startTime.isBefore(endTime) &&
                event.endTime.isAfter(startTime)
        }
    }

    override fun getTodayRange(): Pair<Instant, Instant> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        
        return Pair(startOfDay, endOfDay)
    }
}
