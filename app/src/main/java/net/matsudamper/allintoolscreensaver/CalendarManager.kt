package net.matsudamper.allintoolscreensaver

import android.content.Context
import android.provider.CalendarContract
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
)

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean,
    val calendarId: Long,
)

class CalendarManager(private val context: Context) {

    suspend fun getAvailableCalendars(): List<CalendarInfo> {
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

    suspend fun getEventsForTimeRange(
        calendarIds: List<Long>,
        startTime: Long,
        endTime: Long,
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
            )

            val selection = "${CalendarContract.Events.CALENDAR_ID} IN (${calendarIds.joinToString(",")}) AND " +
                "${CalendarContract.Events.DTSTART} <= ? AND " +
                "${CalendarContract.Events.DTEND} >= ?"

            val selectionArgs = arrayOf(endTime.toString(), startTime.toString())

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
                    val calendarId = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID))

                    events.add(CalendarEvent(id, title, description, eventStartTime, eventEndTime, allDay, calendarId))
                }
            }

            events
        }
    }

    fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }
}
