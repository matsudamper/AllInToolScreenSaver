package net.matsudamper.allintoolscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.feature.alert.AlertManager
import net.matsudamper.allintoolscreensaver.feature.calendar.CalendarRepository
import net.matsudamper.allintoolscreensaver.ui.screensaver.EventAlertUiState

class EventAlertViewModel(
    private val alertManager: AlertManager,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val listener = object : EventAlertUiState.Listener {
        override suspend fun onStart() {
            collectAlertMonitoring()
        }

        override fun onAlertDismiss() {
            val currentAlert = viewModelStateFlow.value.currentAlert
            if (currentAlert != null) {
                alertManager.dismissAlert(currentAlert.event, currentAlert.alertType)
            }
            viewModelStateFlow.update { state ->
                state.copy(currentAlert = null)
            }
        }
    }

    val uiState: StateFlow<EventAlertUiState> = MutableStateFlow(
        EventAlertUiState(
            currentAlert = null,
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        currentAlert = viewModelState.currentAlert?.let { alert ->
                            EventAlertUiState.DialogInfo(
                                title = alert.event.title,
                                eventStartTime = alert.eventStartTime,
                                eventStartTimeText = "開始時刻: ${alert.eventStartTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                isRepeatingAlert = alert.isRepeatingAlert,
                                alertTypeDisplayText = alert.alertType.toDisplayText(),
                                description = alert.event.description.orEmpty(),
                            )
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    private suspend fun collectAlertMonitoring() {
        coroutineScope {
            launch {
                alertManager.calendarAlertFlow
                    .collect { alert ->
                        viewModelStateFlow.update { state ->
                            state.copy(
                                currentAlert = AlertDialogInfo(
                                    event = alert.event,
                                    alertType = alert.alertType,
                                    eventStartTime = alert.eventStartTime,
                                    isRepeatingAlert = alert.isRepeating,
                                ),
                            )
                        }
                    }
            }
            launch {
                alertManager.startAlertMonitoring()
            }
        }
    }

    private data class ViewModelState(
        val currentAlert: AlertDialogInfo? = null,
    )

    private data class AlertDialogInfo(
        val event: CalendarRepository.CalendarEvent,
        val alertType: AlertManager.AlertType,
        val eventStartTime: LocalTime,
        val isRepeatingAlert: Boolean,
    )
}

fun AlertManager.AlertType.toDisplayText(): String {
    return when (this) {
        AlertManager.AlertType.FIVE_MINUTES_BEFORE -> "5分前"
        AlertManager.AlertType.ONE_MINUTE_BEFORE -> "1分前"
        AlertManager.AlertType.EVENT_TIME -> "開始時刻"
    }
}
