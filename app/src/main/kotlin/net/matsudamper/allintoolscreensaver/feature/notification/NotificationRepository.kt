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

    fun addNotification(notificationInfo: NotificationInfo) {
        _notificationFlow.tryEmit(notificationInfo)
    }
}
