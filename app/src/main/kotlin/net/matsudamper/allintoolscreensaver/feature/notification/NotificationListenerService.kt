package net.matsudamper.allintoolscreensaver.feature.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.atomic.AtomicReference
import org.koin.core.context.GlobalContext

class NotificationListenerService : NotificationListenerService() {
    private val notificationRepository: NotificationRepository get() = GlobalContext.get().get()

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance.set(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance.set(null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val notification = sbn.notification

        // 消せない通知等をフィルター
        if ((notification.flags and NotificationCompat.FLAG_FOREGROUND_SERVICE) != 0) return
        if ((notification.flags and NotificationCompat.FLAG_ONGOING_EVENT) != 0) return
        if ((notification.flags and NotificationCompat.FLAG_NO_CLEAR) != 0) return

        val title = notification.extras.getString(NotificationCompat.EXTRA_TITLE)
            ?: notification.extras.getString(NotificationCompat.EXTRA_TITLE_BIG)
            ?: notification.tickerText?.toString()
            ?: packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()

        val text = notification.extras.getString(NotificationCompat.EXTRA_TEXT)
            ?: notification.extras.getString(NotificationCompat.EXTRA_BIG_TEXT)
            ?: notification.extras.getString(NotificationCompat.EXTRA_SUB_TEXT)
            ?: notification.extras.getString(NotificationCompat.EXTRA_SUMMARY_TEXT)
            ?: notification.extras.getString(NotificationCompat.EXTRA_INFO_TEXT)
            ?: notification.extras.keySet()
                .filter { it != Notification.EXTRA_TITLE }
                .filter { it != Notification.EXTRA_TITLE_BIG }
                .filter { it != Notification.EXTRA_TEXT }
                .filter { it != Notification.EXTRA_BIG_TEXT }
                .filter { it != Notification.EXTRA_SUB_TEXT }
                .filter { it != Notification.EXTRA_SUMMARY_TEXT }
                .filter { it != Notification.EXTRA_INFO_TEXT }
                .joinToString(",")

        val packageName = sbn.packageName

        val notificationInfo = NotificationInfo(
            title = title,
            text = text,
            packageName = packageName,
            timestamp = sbn.postTime,
        )

        notificationRepository.addNotification(notificationInfo)
    }

    companion object {
        private val instance = AtomicReference<NotificationListenerService?>()
    }
}

data class NotificationInfo(
    val title: String,
    val text: String,
    val packageName: String,
    val timestamp: Long,
)
