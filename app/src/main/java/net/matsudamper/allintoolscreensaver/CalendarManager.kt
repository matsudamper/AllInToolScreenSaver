package net.matsudamper.allintoolscreensaver

import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
)

sealed interface CalendarEvent {
    val id: Long
    val title: String
    val description: String?
    val color: Int

    data class Time(
        override val id: Long,
        override val title: String,
        override val description: String?,
        override val color: Int,
        val startTime: Instant,
        val endTime: Instant,
    ) : CalendarEvent

    data class AllDay(
        override val id: Long,
        override val title: String,
        override val description: String?,
        override val color: Int,
    ) : CalendarEvent
}

interface CalendarRepository {
    suspend fun getAvailableCalendars(): List<CalendarInfo>
    suspend fun getEventsForTimeRange(
        calendarIds: List<Long>,
        startTime: Instant,
        endTime: Instant,
    ): List<CalendarEvent>
}

class CalendarManager(private val context: Context) : CalendarRepository {

    override suspend fun getAvailableCalendars(): List<CalendarInfo> {
        return withContext(Dispatchers.IO) {
            val calendars = mutableListOf<CalendarInfo>()

            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
            )

            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null,
            )

            cursor?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                    val displayName = c.getString(c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                    val accountName = c.getString(c.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))
                    val color = c.getInt(c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR))

                    calendars.add(CalendarInfo(id, displayName, accountName, color))
                }
            }

            calendars
        }
    }

    override suspend fun getEventsForTimeRange(
        calendarIds: List<Long>,
        startTime: Instant,
        endTime: Instant,
    ): List<CalendarEvent> {
        return withContext(Dispatchers.IO) {
            val events = mutableListOf<CalendarEvent>()

            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.CALENDAR_COLOR,
            )

            val selection = "${CalendarContract.Events.CALENDAR_ID} IN (${calendarIds.joinToString(",")}) AND " +
                "${CalendarContract.Events.DTSTART} <= ? AND " +
                "${CalendarContract.Events.DTEND} >= ?"

            val selectionArgs = arrayOf(endTime.toEpochMilli().toString(), startTime.toEpochMilli().toString())

            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC",
            )

            cursor?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events._ID))
                    val title = c.getString(c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)).orEmpty()
                    val description = c.getString(c.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION))
                    val eventStartTime = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                    val eventEndTime = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                    val allDay = c.getInt(c.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1
                    val color = c.getInt(c.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_COLOR))
                    events.add(
                        if (allDay) {
                            CalendarEvent.AllDay(
                                id = id,
                                title = title,
                                description = description,
                                color = color,
                            )
                        } else {
                            CalendarEvent.Time(
                                id = id,
                                title = title,
                                description = description,
                                startTime = Instant.ofEpochMilli(eventStartTime),
                                endTime = Instant.ofEpochMilli(eventEndTime),
                                color = color,
                            )
                        },
                    )
                }
            }

            events
        }
    }
}
