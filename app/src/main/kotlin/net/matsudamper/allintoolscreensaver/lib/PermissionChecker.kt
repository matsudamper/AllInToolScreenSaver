package net.matsudamper.allintoolscreensaver.lib

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

class PermissionChecker(private val context: Context) {

    fun hasCalendarReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPostNotificationsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasSystemAlertWindowPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }
}
