package net.matsudamper.allintoolscreensaver.feature.notification

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NotificationRepository {
    private val _notificationFlow = MutableSharedFlow<NotificationInfo>(
        replay = 0,
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notificationFlow: Flow<NotificationInfo> = _notificationFlow.asSharedFlow()

    /**
     * @return 表示先が購読していて通知を届けられた場合 true
     */
    fun addNotification(notificationInfo: NotificationInfo): Boolean {
        if (_notificationFlow.subscriptionCount.value == 0) return false
        return _notificationFlow.tryEmit(notificationInfo)
    }
}
