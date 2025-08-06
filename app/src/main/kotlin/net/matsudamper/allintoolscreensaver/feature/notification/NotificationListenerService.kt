package net.matsudamper.allintoolscreensaver.feature.notification

import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import android.util.Log
import androidx.core.app.NotificationCompat
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

        val title = run {
            val keys = listOf(
                NotificationCompat.EXTRA_TITLE,
                NotificationCompat.EXTRA_TITLE_BIG,
            )

            val base = keys.firstNotNullOfOrNull {
                notification.extras.getText(it)
            } ?: notification.tickerText?.toString()

            base.orEmpty()
        }

        val textKeys = listOf(
            NotificationCompat.EXTRA_TEXT,
            NotificationCompat.EXTRA_BIG_TEXT,
            NotificationCompat.EXTRA_SUB_TEXT,
            NotificationCompat.EXTRA_SUMMARY_TEXT,
            NotificationCompat.EXTRA_INFO_TEXT,
            NotificationCompat.EXTRA_SHORT_CRITICAL_TEXT,
        )
        val text = textKeys.firstNotNullOfOrNull {
            notification.extras.getText(it)
        }
            ?: notification.extras.keySet()
                .filterNot { it in textKeys }
                .associateWith { notification.extras.getText(it) }
                .filterValues { it != null }
                .toList()
                .joinToString(", ") { "${it.first} to ${it.second?.let { value -> value::class.java.name }}" }

        val appName = packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(sbn.packageName, 0),
        ).toString()

        val notificationInfo = NotificationInfo(
            title = title,
            text = text,
            appName = appName,
            timestamp = sbn.postTime,
        )

        notificationRepository.addNotification(notificationInfo)
    }

    private fun Bundle.getText(key: String): String? {
        return getString(key) ?: getParcelable(key, SpannableString::class.java)?.toString()
    }

    companion object {
        private val instance = AtomicReference<NotificationListenerService?>()
    }
}

data class NotificationInfo(
    val title: String,
    val text: String,
    val appName: String,
    val timestamp: Long,
)
