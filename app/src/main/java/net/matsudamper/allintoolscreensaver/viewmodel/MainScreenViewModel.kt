package net.matsudamper.allintoolscreensaver.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.CalendarInfo
import net.matsudamper.allintoolscreensaver.SettingsRepository
import net.matsudamper.allintoolscreensaver.compose.MainActivityUiState
import net.matsudamper.allintoolscreensaver.lib.EventSender

class MainScreenViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val eventSender = EventSender<Listener>()
    val eventHandler = eventSender.asHandler()

    private val listener = object : MainActivityUiState.Listener {
        override suspend fun onStart() {
            coroutineScope {
                launch {
                    checkCalendarPermission()
                    loadAvailableCalendars()
                }
            }
        }

        override fun onDirectorySelected(uri: Uri) {
            viewModelScope.launch {
                eventSender.send {
                    it.onDirectorySelected(uri)
                }
            }
            viewModelScope.launch {
                settingsRepository.saveImageDirectoryUri(uri)
            }
        }

        override fun onCalendarPermissionRequested() {
            checkCalendarPermission()
        }

        override fun onCalendarSelectionChanged(calendarId: Long, isSelected: Boolean) {
            viewModelScope.launch {
                val currentIds = settingsRepository.getSelectedCalendarIds()
                val newIds = if (isSelected) {
                    currentIds + calendarId
                } else {
                    currentIds - calendarId
                }
                settingsRepository.saveSelectedCalendarIds(newIds)
            }
        }

        override fun onImageSwitchIntervalChanged(seconds: Int) {
            viewModelScope.launch {
                settingsRepository.saveImageSwitchIntervalSeconds(seconds)
            }
        }

        override fun onOpenDreamSettings() {
            viewModelScope.launch {
                eventSender.send {
                    it.onOpenDreamSettings()
                }
            }
        }

        override fun onNavigateToCalendarSelection() {
            viewModelScope.launch {
                eventSender.send {
                    it.onNavigateToCalendarSelection()
                }
            }
        }

        override fun updateCalendarPermission(isGranted: Boolean) {
            this@MainScreenViewModel.updateCalendarPermission(isGranted)
        }
    }

    val uiState: StateFlow<MainActivityUiState> = MutableStateFlow(
        MainActivityUiState(
            selectedDirectoryPath = null,
            availableCalendars = listOf(),
            selectedCalendarIds = listOf(),
            hasCalendarPermission = false,
            imageSwitchIntervalSeconds = 30,
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            combine(
                viewModelStateFlow,
                settingsRepository.settingsFlow,
            ) { viewModelState, settings ->
                MainActivityUiState(
                    selectedDirectoryPath = settings.imageDirectoryUri
                        .ifEmpty { null },
                    availableCalendars = viewModelState.availableCalendars,
                    selectedCalendarIds = settings.selectedCalendarIdsList,
                    hasCalendarPermission = viewModelState.hasCalendarPermission,
                    imageSwitchIntervalSeconds = if (settings.imageSwitchIntervalSeconds == 0) {
                        30
                    } else {
                        settings.imageSwitchIntervalSeconds
                    },
                    listener = listener,
                )
            }.collectLatest { newUiState ->
                uiStateFlow.update { newUiState }
            }
        }
    }.asStateFlow()

    private fun checkCalendarPermission() {
        viewModelScope.launch {
            val hasPermission = eventSender.send {
                it.checkCalendarPermission()
            }
            updateCalendarPermission(hasPermission)
        }
    }

    private fun loadAvailableCalendars() {
        viewModelScope.launch {
            if (viewModelStateFlow.value.hasCalendarPermission) {
                val calendars = eventSender.send {
                    it.loadAvailableCalendars()
                }
                viewModelStateFlow.update { state ->
                    state.copy(availableCalendars = calendars)
                }
            }
        }
    }

    private fun updateCalendarPermission(isGranted: Boolean) {
        viewModelStateFlow.update { state ->
            state.copy(hasCalendarPermission = isGranted)
        }
        if (isGranted) {
            loadAvailableCalendars()
        }
    }

    private data class ViewModelState(
        val availableCalendars: List<CalendarInfo> = listOf(),
        val hasCalendarPermission: Boolean = false,
    )

    interface Listener {
        fun onDirectorySelected(uri: Uri)
        fun onOpenDreamSettings()
        fun checkCalendarPermission(): Boolean
        suspend fun loadAvailableCalendars(): List<CalendarInfo>
        fun onNavigateToCalendarSelection()
    }
}
