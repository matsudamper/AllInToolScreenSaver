package net.matsudamper.allintoolscreensaver

import java.time.Instant

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
            when (event) {
                is CalendarEvent.Time -> {
                    // テスト用：時間範囲のチェックのみ行い、calendarIdは無視
                    event.startTime.isBefore(endTime) &&
                        event.endTime.isAfter(startTime)
                }
                is CalendarEvent.AllDay -> {
                    // テスト用：終日イベントは常に含める
                    true
                }
            }
        }
    }
}
