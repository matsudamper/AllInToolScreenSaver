package net.matsudamper.allintoolscreensaver.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavBackStack
import net.matsudamper.allintoolscreensaver.feature.calendar.CalendarRepository
import net.matsudamper.allintoolscreensaver.navigation.NavKeys

class MainScreenViewModelListenerImpl(
    private val application: Application,
    private val calendarManager: CalendarRepository,
    private val backStack: NavBackStack,
) : MainScreenViewModel.Listener {
    override fun onDirectorySelected(uri: Uri) {
        application.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    override fun onOpenDreamSettings() {
        val intent = Intent(Settings.ACTION_DREAM_SETTINGS).also { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        application.startActivity(intent)
    }

    override fun checkCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun loadAvailableCalendars(): List<CalendarRepository.CalendarInfo> {
        return calendarManager.getAvailableCalendars()
    }

    override fun onNavigateToCalendarSelection() {
        backStack.addLast(NavKeys.CalendarSelection)
    }

    override fun onNavigateToCalendarDisplay() {
        backStack.addLast(NavKeys.CalendarDisplay)
    }

    override fun onNavigateToSlideShowPreview() {
        backStack.addLast(NavKeys.SlideShowPreview)
    }

    override fun onNavigateToAlertCalendarSelection() {
        backStack.addLast(NavKeys.AlertCalendarSelection)
    }

    override fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(application)
    }

    override fun onRequestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).also { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = "package:${application.packageName}".toUri()
        }
        application.startActivity(intent)
    }
}
