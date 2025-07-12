package net.matsudamper.allintoolscreensaver.feature.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.atomic.AtomicReference
import org.koin.core.context.GlobalContext

class NotificationListenerService : NotificationListenerService() {
    private val notificationRepository: NotificationRepository by GlobalContext.get().inject()

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
        val title = notification.extras.getString("android.title").orEmpty()
        val text = notification.extras.getString("android.text").orEmpty()
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
