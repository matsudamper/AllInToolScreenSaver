package net.matsudamper.allintoolscreensaver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.compose.eventalert.AlertType
import net.matsudamper.allintoolscreensaver.ui.compose.AlertOverlayDialog
import net.matsudamper.allintoolscreensaver.ui.state.AlertDialogUiState
import net.matsudamper.allintoolscreensaver.ui.theme.AllInToolScreenSaverTheme
import org.koin.core.context.GlobalContext

class AlertService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var alertManager: AlertManager? = null
    private var serviceScope: CoroutineScope? = null
    private var currentAlert by mutableStateOf<AlertDialogInfo?>(null)
    private var isDreamServiceActive = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )

        initializeAlertManager()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS_ALERT -> {
                dismissCurrentAlert()
            }
            ACTION_DREAM_STATE_CHANGED -> {
                val isActive = intent.getBooleanExtra(EXTRA_DREAM_ACTIVE, false)
                isDreamServiceActive = isActive
                if (isActive) {
                    hideOverlay()
                } else if (currentAlert != null) {
                    showOverlay()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        serviceScope?.coroutineContext?.get(Job)?.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "アラート監視サービス",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "カレンダーアラートの監視を行います"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("アラート監視中")
        .setContentText("カレンダーアラートを監視しています")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setOngoing(true)
        .build()

    private fun initializeAlertManager() {
        val koin = GlobalContext.get()
        alertManager = koin.get<AlertManager>()

        alertManager?.onAlertTriggered = { event, alertType, eventStartTime, isRepeating ->
            currentAlert = AlertDialogInfo(
                event = event,
                alertType = alertType,
                eventStartTime = eventStartTime,
                eventStartTimeText = "開始時刻: ${eventStartTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                isRepeatingAlert = isRepeating,
            )
            showOverlay()
        }

        serviceScope?.launch {
            alertManager?.startAlertMonitoring()
        }
    }

    private fun showOverlay() {
        if (overlayView != null || !canDrawOverlays()) return

        val alert = currentAlert ?: return

        val composeView = ComposeView(this)
        composeView.setContent {
            AllInToolScreenSaverTheme {
                AlertOverlayDialog(
                    alertInfo = AlertDialogUiState(
                        title = alert.event.title,
                        alertTypeDisplayText = alert.alertType.displayText,
                        eventStartTimeText = alert.eventStartTimeText,
                        description = alert.event.description.orEmpty(),
                        isRepeatingAlert = alert.isRepeatingAlert,
                    ),
                    onDismiss = ::dismissCurrentAlert,
                )
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView = composeView
        windowManager.addView(composeView, layoutParams)
    }

    private fun hideOverlay() {
        overlayView?.let { view ->
            windowManager.removeView(view)
            overlayView = null
        }
    }

    private fun dismissCurrentAlert() {
        val alert = currentAlert
        if (alert != null) {
            alertManager?.dismissAlert(alert.event, alert.alertType)
            currentAlert = null
        }
        hideOverlay()
    }

    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    data class AlertDialogInfo(
        val event: CalendarEvent,
        val alertType: AlertType,
        val eventStartTime: LocalTime,
        val eventStartTimeText: String,
        val isRepeatingAlert: Boolean,
    )

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "alert_service_channel"
        private const val ACTION_DISMISS_ALERT = "dismiss_alert"
        private const val ACTION_DREAM_STATE_CHANGED = "dream_state_changed"
        private const val EXTRA_DREAM_ACTIVE = "dream_active"

        fun startService(context: Context) {
            val intent = Intent(context, AlertService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AlertService::class.java)
            context.stopService(intent)
        }

        fun notifyDreamStateChanged(context: Context, isActive: Boolean) {
            val intent = Intent(context, AlertService::class.java)
            intent.action = ACTION_DREAM_STATE_CHANGED
            intent.putExtra(EXTRA_DREAM_ACTIVE, isActive)
            context.startService(intent)
        }
    }
}
