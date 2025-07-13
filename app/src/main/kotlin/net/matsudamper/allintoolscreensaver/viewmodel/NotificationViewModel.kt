package net.matsudamper.allintoolscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                        notifications = state.notifications.plus(newNotification),
                    )
                }
            }
        }
    }

    val uiState: StateFlow<NotificationOverlayUiState> = MutableStateFlow(
        NotificationOverlayUiState(
            notifications = listOf(),
            isVisible = false,
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update {
                    NotificationOverlayUiState(
                        notifications = viewModelState.notifications.map { notification ->
                            NotificationOverlayUiState.NotificationItem(
                                title = notification.title,
                                text = notification.text,
                                listener = object : NotificationOverlayUiState.NotificationItem.ItemListener {
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
                                },
                            )
                        },
                        isVisible = viewModelState.notifications.isNotEmpty(),
                        listener = listener,
                    )
                }
            }
        }
    }.asStateFlow()

    private data class ViewModelState(
        val notifications: List<NotificationInfo> = listOf(),
        val isServiceConnected: Boolean = false,
    )
}
