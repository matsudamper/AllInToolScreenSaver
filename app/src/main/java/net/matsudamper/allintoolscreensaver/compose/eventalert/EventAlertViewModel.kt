package net.matsudamper.allintoolscreensaver.compose.eventalert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.AlertManager
import net.matsudamper.allintoolscreensaver.CalendarEvent

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
                                event = alert.event,
                                alertType = alert.alertType,
                                eventStartTime = alert.eventStartTime,
                                eventStartTimeText = "開始時刻: ${alert.eventStartTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                isRepeatingAlert = alert.isRepeatingAlert,
                            )
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    private suspend fun collectAlertMonitoring() {
        alertManager.onScreenSaverAlertTriggered = { event, alertType, eventStartTime, isRepeating ->
            viewModelStateFlow.update { state ->
                state.copy(
                    currentAlert = AlertDialogInfo(
                        event = event,
                        alertType = alertType,
                        eventStartTime = eventStartTime,
                        isRepeatingAlert = isRepeating,
                    ),
                )
            }
        }
        alertManager.startAlertMonitoring()
    }

    private data class ViewModelState(
        val currentAlert: AlertDialogInfo? = null,
    )

    private data class AlertDialogInfo(
        val event: CalendarEvent,
        val alertType: AlertType,
        val eventStartTime: LocalTime,
        val isRepeatingAlert: Boolean,
    )
}
