package net.matsudamper.allintoolscreensaver.compose.eventalert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                        currentAlert = viewModelState.currentAlert?.let { event ->
                            EventAlertUiState.DialogInfo(event = event)
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    private suspend fun collectAlertMonitoring() {
        alertManager.onAlertTriggered = { event ->
            viewModelStateFlow.update { state ->
                state.copy(currentAlert = event)
            }
        }
        alertManager.startAlertMonitoring()
    }

    private data class ViewModelState(
        val currentAlert: CalendarEvent? = null,
    )
}
