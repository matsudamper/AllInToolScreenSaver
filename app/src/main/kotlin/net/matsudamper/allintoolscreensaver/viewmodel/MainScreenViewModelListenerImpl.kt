package net.matsudamper.allintoolscreensaver.viewmodel

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import net.matsudamper.allintoolscreensaver.ActivityResultRequest
import net.matsudamper.allintoolscreensaver.feature.calendar.CalendarRepository
import net.matsudamper.allintoolscreensaver.navigation.NavKeys

class MainScreenViewModelListenerImpl(
    private val application: Application,
    private val calendarManager: CalendarRepository,
    private val backStack: NavBackStack<NavKey>,
    private val activityResultRequest: (ActivityResultRequest<Any, Any>) -> Unit,
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

    override fun onNavigateToNotificationPreview() {
        backStack.addLast(NavKeys.NotificationPreview)
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

    override fun onSendNotification() {
        val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "test_notification_channel"
        val channel = NotificationChannel(
            channelId,
            "テスト通知",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "テスト通知用のチャンネル"
            enableVibration(false)
            setSound(null, null)
            enableLights(false)
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(application, channelId)
            .setContentTitle("テスト通知")
            .setContentText("これはテスト通知です")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setVibrate(null)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onOpenNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).also { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        application.startActivity(intent)
    }

    override fun checkNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun checkNotificationListenerPermission(): Boolean {
        val enabledNotificationListeners = Settings.Secure.getString(
            application.contentResolver,
            "enabled_notification_listeners",
        )
        val packageName = application.packageName
        return enabledNotificationListeners?.contains(packageName) == true
    }

    override suspend fun <I, O> requestPermission(contract: ActivityResultContract<I, O>, input: I): O {
        return suspendCancellableCoroutine { continuation ->
            @Suppress("UNCHECKED_CAST")
            activityResultRequest(
                ActivityResultRequest(
                    contract = contract,
                    input = input,
                    result = { output ->
                        continuation.resume(output)
                    },
                ) as ActivityResultRequest<Any, Any>,
            )
        }
    }
}
