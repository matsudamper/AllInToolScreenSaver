package net.matsudamper.allintoolscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.feature.notification.NotificationRepository
import net.matsudamper.allintoolscreensaver.ui.notification.NotificationOverlayUiState

class NotificationViewModel(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val listener = object : NotificationOverlayUiState.Listener {
    }

    val uiState: StateFlow<NotificationOverlayUiState> = MutableStateFlow(
        NotificationOverlayUiState(
            notifications = listOf(),
            isVisible = false,
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            notificationRepository.notificationFlow.collectLatest { newNotification ->
                viewModelStateFlow.update { state ->
                    val notificationId = "${newNotification.packageName}_${newNotification.timestamp}"
                    val notificationItem = NotificationOverlayUiState.NotificationItem(
                        id = notificationId,
                        title = newNotification.title,
                        text = newNotification.text,
                        packageName = newNotification.packageName,
                        timestamp = newNotification.timestamp,
                        isVisible = true,
                        listener = object : NotificationOverlayUiState.NotificationItem.ItemListener {
                            override fun dismissRequest() {
                                viewModelStateFlow.update { currentState ->
                                    currentState.copy(
                                        notifications = currentState.notifications.map { notification ->
                                            if (notification.id == notificationId) {
                                                notification.copy(isVisible = false)
                                            } else {
                                                notification
                                            }
                                        },
                                    )
                                }
                            }

                            override fun onClick() = Unit
                        },
                    )

                    val updatedNotifications = state.notifications.toMutableList()
                    updatedNotifications.add(notificationItem)

                    val cleanedNotifications = updatedNotifications
                        .takeLast(10)
                        .filter { it.isVisible }

                    state.copy(notifications = cleanedNotifications)
                }

                val currentState = viewModelStateFlow.value
                uiStateFlow.update {
                    NotificationOverlayUiState(
                        notifications = currentState.notifications,
                        isVisible = currentState.notifications.any { notification -> notification.isVisible },
                        listener = listener,
                    )
                }
            }
        }
    }.asStateFlow()

    private data class ViewModelState(
        val notifications: List<NotificationOverlayUiState.NotificationItem> = listOf(),
        val isServiceConnected: Boolean = false,
    )
}
