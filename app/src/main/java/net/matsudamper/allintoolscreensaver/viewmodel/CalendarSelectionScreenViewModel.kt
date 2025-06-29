package net.matsudamper.allintoolscreensaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.CalendarInfo
import net.matsudamper.allintoolscreensaver.CalendarRepository
import net.matsudamper.allintoolscreensaver.SettingsRepository
import net.matsudamper.allintoolscreensaver.compose.CalendarSelectionMode
import net.matsudamper.allintoolscreensaver.compose.CalendarSelectionScreenUiState
import net.matsudamper.allintoolscreensaver.lib.EventSender

class CalendarSelectionScreenViewModel(
    private val calendarRepository: CalendarRepository,
    private val settingsRepository: SettingsRepository,
    private val selectionMode: CalendarSelectionMode = CalendarSelectionMode.DISPLAY,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val eventSender = EventSender<Event>()
    val eventHandler = eventSender.asHandler()

    private val listener = object : CalendarSelectionScreenUiState.Listener {
        override suspend fun onStart() {
            coroutineScope {
                launch {
                    loadAvailableCalendars()
                }
                launch {
                    eventSender.send {
                        it.onCalendarPermissionLaunch()
                    }
                }
                launch {
                    val flow = when (selectionMode) {
                        CalendarSelectionMode.DISPLAY -> settingsRepository.getSelectedCalendarIdsFlow()
                        CalendarSelectionMode.ALERT -> settingsRepository.getAlertCalendarIdsFlow()
                    }
                    flow.collectLatest { idList ->
                        viewModelStateFlow.update { currentState ->
                            currentState.copy(
                                selectedCalendarIds = idList.toSet(),
                            )
                        }
                    }
                }
            }
        }

        override fun updateCalendarPermission(granted: Boolean) {
            viewModelScope.launch {
                onCalendarPermissionLaunch()
                viewModelStateFlow.update {
                    it.copy(
                        hasCalendarPermission = granted,
                    )
                }
            }
        }

        override fun onCalendarPermissionLaunch() {
            viewModelScope.launch {
                eventSender.send { event ->
                    event.onCalendarPermissionLaunch()
                }
            }
        }

        override fun onBack() {
            viewModelScope.launch {
                eventSender.send { event ->
                    event.onBack()
                }
            }
        }
    }

    val uiState = MutableStateFlow(
        CalendarSelectionScreenUiState(
            availableCalendars = listOf(),
            hasCalendarPermission = false,
            selectionMode = selectionMode,
            listener = listener,
        ),
    ).also { mutableStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { state ->
                mutableStateFlow.value = CalendarSelectionScreenUiState(
                    availableCalendars = state.availableCalendars.map {
                        CalendarSelectionScreenUiState.Calendar(
                            id = it.id,
                            displayName = it.displayName,
                            accountName = it.accountName,
                            color = it.color,
                            isSelected = state.selectedCalendarIds.contains(it.id),
                            listener = object : CalendarSelectionScreenUiState.CalendarListener {
                                override fun onSelectionChanged(isSelected: Boolean) {
                                    viewModelScope.launch {
                                        val currentState = viewModelStateFlow.value
                                        val newSelectedIds = if (isSelected) {
                                            currentState.selectedCalendarIds + it.id
                                        } else {
                                            currentState.selectedCalendarIds - it.id
                                        }

                                        when (selectionMode) {
                                            CalendarSelectionMode.DISPLAY -> {
                                                settingsRepository.saveSelectedCalendarIds(newSelectedIds.toList())
                                            }
                                            CalendarSelectionMode.ALERT -> {
                                                settingsRepository.saveAlertCalendarIds(newSelectedIds.toList())
                                            }
                                        }

                                        viewModelStateFlow.update {
                                            currentState.copy(
                                                selectedCalendarIds = newSelectedIds,
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    },
                    hasCalendarPermission = state.hasCalendarPermission,
                    selectionMode = selectionMode,
                    listener = listener,
                )
            }
        }
    }.asStateFlow()

    private suspend fun loadAvailableCalendars() {
        val calendars = calendarRepository.getAvailableCalendars()
        val currentState = viewModelStateFlow.value
        viewModelStateFlow.update {
            currentState.copy(
                availableCalendars = calendars,
            )
        }
    }

    interface Event {
        fun onCalendarPermissionLaunch()
        fun onBack()
    }

    data class ViewModelState(
        val hasCalendarPermission: Boolean = false,
        val availableCalendars: List<CalendarInfo> = listOf(),
        val selectedCalendarIds: Set<Long> = setOf(),
    )
}
