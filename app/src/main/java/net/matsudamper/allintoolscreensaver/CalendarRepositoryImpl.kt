package net.matsudamper.allintoolscreensaver

import android.content.ContentUris
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

class CalendarRepositoryImpl(private val context: Context) : CalendarRepository {

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
                CalendarContract.Instances._ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.DTSTART,
                CalendarContract.Instances.DTEND,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.CALENDAR_COLOR,
                CalendarContract.Instances.EVENT_COLOR,
            )

            val selection = "${CalendarContract.Events.CALENDAR_ID} IN (${calendarIds.joinToString(",")}) AND 1 "

            val cursor = context.contentResolver.query(
                CalendarContract.Instances.CONTENT_URI.buildUpon().also { builder ->
                    ContentUris.appendId(builder, startTime.toEpochMilli())
                    ContentUris.appendId(builder, endTime.toEpochMilli())
                }.build(),
                projection,
                selection,
                null,
                null,
            )

            cursor?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events._ID))
                    val title = c.getString(c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)).orEmpty()
                    val description = c.getString(c.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION))
                    val eventStartTime = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                    val eventEndTime = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                    val allDay = c.getInt(c.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1
                    val color = run {
                        val eventColorIndex = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_COLOR)
                        val calendarColorIndex = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)
                        if (!c.isNull(eventColorIndex)) c.getInt(eventColorIndex) else c.getInt(calendarColorIndex)
                    }
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
