package net.matsudamper.allintoolscreensaver.feature.calendar

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.matsudamper.allintoolscreensaver.lib.PermissionChecker

interface CalendarRepository {
    suspend fun getAvailableCalendars(): List<CalendarInfo>
    suspend fun getEventsForTimeRange(
        calendarIds: List<Long>,
        startTime: Instant,
        endTime: Instant,
    ): List<CalendarEvent>

    data class CalendarInfo(
        val id: Long,
        val displayName: String,
        val accountName: String,
        val color: Int,
    )

    sealed interface CalendarEvent {
        val id: Long
        val calendarId: Long
        val title: String
        val description: String?
        val color: Int
        val attendeeStatus: AttendeeStatus

        data class Time(
            override val id: Long,
            override val calendarId: Long,
            override val title: String,
            override val description: String?,
            override val color: Int,
            override val attendeeStatus: AttendeeStatus,
            val startTime: Instant,
            val endTime: Instant,
        ) : CalendarEvent

        data class AllDay(
            override val id: Long,
            override val calendarId: Long,
            override val title: String,
            override val description: String?,
            override val color: Int,
            override val attendeeStatus: AttendeeStatus,
        ) : CalendarEvent
    }
}

class CalendarRepositoryImpl(private val context: Context) : CalendarRepository {

    override suspend fun getAvailableCalendars(): List<CalendarRepository.CalendarInfo> {
        return withContext(Dispatchers.IO) {
            val permissionChecker = PermissionChecker(context)
            if (!permissionChecker.hasCalendarReadPermission()) {
                return@withContext emptyList()
            }

            val calendars = mutableListOf<CalendarRepository.CalendarInfo>()

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

                    calendars.add(CalendarRepository.CalendarInfo(id, displayName, accountName, color))
                }
            }

            calendars
        }
    }

    override suspend fun getEventsForTimeRange(
        calendarIds: List<Long>,
        startTime: Instant,
        endTime: Instant,
    ): List<CalendarRepository.CalendarEvent> {
        return withContext(Dispatchers.IO) {
            val permissionChecker = PermissionChecker(context)
            if (!permissionChecker.hasCalendarReadPermission()) {
                return@withContext emptyList()
            }

            val projection = arrayOf(
                CalendarContract.Instances._ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.DTSTART,
                CalendarContract.Instances.DTEND,
                CalendarContract.Instances.DURATION,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.CALENDAR_COLOR,
                CalendarContract.Instances.EVENT_COLOR,
                CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            )

            // その日付のUTCでの始まりの時間を取得
            val minDayStartUTC = run {
                val localDate = LocalDate.ofInstant(startTime, ZoneId.systemDefault())
                LocalDateTime.of(localDate, LocalDateTime.MIN.toLocalTime())
                    .atZone(ZoneId.of("UTC"))
                    .toInstant()
            }

            val selection = listOf(
                "${CalendarContract.Events.CALENDAR_ID} IN (${calendarIds.joinToString(",")})",
                "(${CalendarContract.Events.ALL_DAY} = 0) OR (${CalendarContract.Events.ALL_DAY} = 1 AND ${CalendarContract.Events.DTSTART} = ${minDayStartUTC.toEpochMilli()})",
                "${CalendarContract.Events.DTSTART} < ${endTime.toEpochMilli()}",
            ).joinToString(" AND ") { "($it) " }

            val cursor = context.contentResolver.query(
                CalendarContract.Instances.CONTENT_URI.buildUpon().also { builder ->
                    ContentUris.appendId(builder, startTime.toEpochMilli())
                    ContentUris.appendId(builder, endTime.toEpochMilli())
                }.build(),
                projection,
                selection,
                null,
                null,
            ) ?: return@withContext listOf()

            collectCalendarEvent(cursor)
        }
    }

    private fun collectCalendarEvent(
        cursor: Cursor,
    ): MutableList<CalendarRepository.CalendarEvent> {
        val events = mutableListOf<CalendarRepository.CalendarEvent>()
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events._ID))
                val calendarId = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID))
                val title = c.getString(c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)).orEmpty()
                val description = c.getString(c.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION))
                val eventStartTime = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                val eventEndTime = c.getLongOrNull(c.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                val eventDuration = c.getStringOrNull(c.getColumnIndexOrThrow(CalendarContract.Instances.DURATION))
                val allDay = c.getInt(c.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1
                val color = run {
                    val eventColorIndex = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_COLOR)
                    val calendarColorIndex = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)
                    if (!c.isNull(eventColorIndex)) c.getInt(eventColorIndex) else c.getInt(calendarColorIndex)
                }
                val attendeeStatus = run {
                    val statusIndex = c.getColumnIndexOrThrow(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                    if (c.isNull(statusIndex)) {
                        AttendeeStatus.UNKNOWN
                    } else {
                        when (c.getInt(statusIndex)) {
                            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED -> AttendeeStatus.ACCEPTED
                            CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED -> AttendeeStatus.DECLINED
                            CalendarContract.Attendees.ATTENDEE_STATUS_INVITED -> AttendeeStatus.INVITED
                            CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE -> AttendeeStatus.TENTATIVE
                            else -> AttendeeStatus.UNKNOWN
                        }
                    }
                }
                events.add(
                    if (allDay) {
                        CalendarRepository.CalendarEvent.AllDay(
                            id = id,
                            calendarId = calendarId,
                            title = title,
                            description = description,
                            color = color,
                            attendeeStatus = attendeeStatus,
                        )
                    } else {
                        val startTime = Instant.ofEpochMilli(eventStartTime)
                        val endTime: Instant
                        val parseFailedDescription: String?

                        if (eventEndTime != null) {
                            endTime = Instant.ofEpochMilli(eventEndTime)
                            parseFailedDescription = null
                        } else {
                            if (eventDuration != null) {
                                val parsedDuration = parseDuration(eventDuration)
                                if (parsedDuration != null) {
                                    endTime = Instant.ofEpochMilli(eventStartTime + parsedDuration.inWholeMilliseconds)
                                    parseFailedDescription = null
                                } else {
                                    endTime = startTime.plus(java.time.Duration.ofMinutes(30))
                                    parseFailedDescription = "「${eventDuration}」がパースできませんでした".trimIndent()
                                }
                            } else {
                                endTime = startTime.plus(java.time.Duration.ofMinutes(30))
                                parseFailedDescription = "DTENDもDURATIONも設定されていません。30分で設定しました。".trimIndent()
                            }
                        }
                        CalendarRepository.CalendarEvent.Time(
                            id = id,
                            calendarId = calendarId,
                            title = title,
                            description = buildString {
                                if (parseFailedDescription != null) {
                                    appendLine("アプリからのお知らせ")
                                    appendLine(parseFailedDescription)
                                    appendLine(List(20) { "-" }.joinToString("") { it })
                                }
                                append(description)
                            },
                            startTime = startTime,
                            endTime = endTime,
                            color = color,
                            attendeeStatus = attendeeStatus,
                        )
                    },
                )
            }
        }
        return events
    }

    private fun parseDuration(durationStr: String): Duration? {
        return if (durationStr.contains("T")) {
            Duration.parseIsoStringOrNull(durationStr)
        } else {
            if (durationStr.startsWith("P")) {
                Duration.parseIsoStringOrNull(
                    durationStr.replaceFirst("P", "PT"),
                )
            } else {
                null
            }
        }
    }
}
