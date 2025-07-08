package net.matsudamper.allintoolscreensaver

import java.time.Instant
import net.matsudamper.allintoolscreensaver.feature.calendar.CalendarRepository

class FakeCalendarRepository : CalendarRepository {
    private val fakeCalendars = mutableListOf<CalendarRepository.CalendarInfo>()
    private val fakeEvents = mutableListOf<CalendarRepository.CalendarEvent>()

    fun addCalendar(calendar: CalendarRepository.CalendarInfo) {
        fakeCalendars.add(calendar)
    }

    fun addEvent(event: CalendarRepository.CalendarEvent) {
        fakeEvents.add(event)
    }

    override suspend fun getAvailableCalendars(): List<CalendarRepository.CalendarInfo> {
        return fakeCalendars.toList()
    }

    override suspend fun getEventsForTimeRange(
        calendarIds: List<Long>,
        startTime: Instant,
        endTime: Instant,
    ): List<CalendarRepository.CalendarEvent> {
        return fakeEvents.filter { event ->
            when (event) {
                is CalendarRepository.CalendarEvent.Time -> {
                    event.startTime >= startTime && event.startTime <= endTime
                }
                is CalendarRepository.CalendarEvent.AllDay -> {
                    true
                }
            }
        }
    }
}
