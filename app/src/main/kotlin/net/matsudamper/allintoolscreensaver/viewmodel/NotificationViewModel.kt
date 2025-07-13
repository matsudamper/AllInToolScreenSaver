package net.matsudamper.allintoolscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.feature.notification.NotificationInfo
import net.matsudamper.allintoolscreensaver.feature.notification.NotificationRepository
import net.matsudamper.allintoolscreensaver.ui.notification.NotificationOverlayUiState

class NotificationViewModel(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val listener = object : NotificationOverlayUiState.Listener {
    }

    init {
        viewModelScope.launch {
            notificationRepository.notificationFlow.collect { newNotification ->
                viewModelStateFlow.update { state ->
                    state.copy(
                        notifications = state.notifications.plus(
                            ViewModelState.NotificationItem(
                                id = Random().nextDouble(),
                                item = newNotification,
                            ),
                        ),
                    )
                }
            }
        }
    }

    val uiState: StateFlow<NotificationOverlayUiState> = MutableStateFlow(
        NotificationOverlayUiState(
            notifications = listOf(),
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update {
                    NotificationOverlayUiState(
                        notifications = viewModelState.notifications.map { notification ->
                            NotificationOverlayUiState.NotificationItem(
                                id = notification.id.toString(),
                                title = notification.item.title,
                                text = notification.item.text,
                                listener = ItemListener(notification = notification),
                            )
                        },
                        listener = listener,
                    )
                }
            }
        }
    }.asStateFlow()

    private inner class ItemListener(
        private val notification: ViewModelState.NotificationItem,
    ) : NotificationOverlayUiState.NotificationItem.ItemListener {
        override fun dismissRequest() {
            viewModelStateFlow.update { currentState ->
                currentState.copy(
                    notifications = currentState.notifications.filter { n ->
                        n != notification
                    },
                )
            }
        }

        override fun onClick() = Unit
    }

    private data class ViewModelState(
        val notifications: List<NotificationItem> = listOf(),
        val isServiceConnected: Boolean = false,
    ) {
        data class NotificationItem(
            val id: Double,
            val item: NotificationInfo,
        )
    }
}
