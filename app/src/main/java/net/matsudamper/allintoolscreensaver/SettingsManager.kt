package net.matsudamper.allintoolscreensaver

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class SettingsManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "screensaver_settings", 
        Context.MODE_PRIVATE
    )

    fun saveImageDirectoryUri(uri: Uri) {
        sharedPreferences.edit()
            .putString(KEY_IMAGE_DIRECTORY_URI, uri.toString())
            .apply()
    }

    fun getImageDirectoryUri(): Uri? {
        val uriString = sharedPreferences.getString(KEY_IMAGE_DIRECTORY_URI, null)
        return if (uriString != null) Uri.parse(uriString) else null
    }

    fun saveSelectedCalendarIds(calendarIds: List<Long>) {
        val idsString = calendarIds.joinToString(",")
        sharedPreferences.edit()
            .putString(KEY_SELECTED_CALENDAR_IDS, idsString)
            .apply()
    }

    fun getSelectedCalendarIds(): List<Long> {
        val idsString = sharedPreferences.getString(KEY_SELECTED_CALENDAR_IDS, null)
        return if (idsString.isNullOrEmpty()) {
            listOf()
        } else {
            idsString.split(",").mapNotNull { it.toLongOrNull() }
        }
    }



    companion object {
        private const val KEY_IMAGE_DIRECTORY_URI = "image_directory_uri"
        private const val KEY_SELECTED_CALENDAR_IDS = "selected_calendar_ids"
    }
} 