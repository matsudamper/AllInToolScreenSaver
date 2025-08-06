package net.matsudamper.allintoolscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.feature.notification.NotificationInfo
import net.matsudamper.allintoolscreensaver.feature.notification.NotificationRepository
import net.matsudamper.allintoolscreensaver.feature.setting.SettingsRepository
import net.matsudamper.allintoolscreensaver.ui.notification.NotificationOverlayUiState

class NotificationViewModel(
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
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

        viewModelScope.launch {
            settingsRepository.getNotificationDisplayDurationFlow().collectLatest { duration ->
                viewModelStateFlow.update { state ->
                    state.copy(displayDuration = duration)
                }
            }
        }
    }

    val uiState: StateFlow<NotificationOverlayUiState> = MutableStateFlow(
        NotificationOverlayUiState(
            notifications = listOf(),
            displayDuration = 5.seconds,
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
                                appName = notification.item.appName,
                                listener = ItemListener(notification = notification),
                            )
                        },
                        displayDuration = viewModelState.displayDuration,
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
        val displayDuration: Duration = 5.seconds,
    ) {
        data class NotificationItem(
            val id: Double,
            val item: NotificationInfo,
        )
    }
}
