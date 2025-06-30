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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.CalendarInfo
import net.matsudamper.allintoolscreensaver.InMemoryCache
import net.matsudamper.allintoolscreensaver.SettingsRepository
import net.matsudamper.allintoolscreensaver.compose.MainActivityUiState
import net.matsudamper.allintoolscreensaver.lib.EventSender

class MainScreenViewModel(
    private val settingsRepository: SettingsRepository,
    private val inMemoryCache: InMemoryCache,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val eventSender = EventSender<Listener>()
    val eventHandler = eventSender.asHandler()

    private val listener = object : MainActivityUiState.Listener {
        override suspend fun onStart() {
            coroutineScope {
                launch {
                    loadAvailableCalendars()
                    updateOverlayPermissionState()
                }
            }
        }

        override suspend fun onResume() {
            coroutineScope {
                checkCalendarPermission()
            }
        }

        override fun onNavigateToCalendarDisplay() {
            viewModelScope.launch {
                eventSender.send {
                    it.onNavigateToCalendarDisplay()
                }
            }
        }

        override fun onDirectorySelected(uri: Uri) {
            inMemoryCache.imageInfo = null
            viewModelScope.launch {
                eventSender.send {
                    it.onDirectorySelected(uri)
                }
            }
            viewModelScope.launch {
                settingsRepository.saveImageDirectoryUri(uri)
            }
        }

        override fun onCalendarSelectionChanged(calendarId: Long, isSelected: Boolean) {
            viewModelScope.launch {
                val currentIds = settingsRepository.settingsFlow.first().selectedCalendarIdsList
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

        override fun onNavigateToAlertCalendarSelection() {
            viewModelScope.launch {
                eventSender.send {
                    it.onNavigateToAlertCalendarSelection()
                }
            }
        }

        override fun onRequestOverlayPermission() {
            viewModelScope.launch {
                eventSender.send {
                    it.onRequestOverlayPermission()
                }
            }
        }

        override fun updatePermissions(calendar: Boolean?, overlay: Boolean?) {
            if (calendar != null) {
                this@MainScreenViewModel.updateCalendarPermission(calendar)
            }
            if (overlay == null) {
                updateOverlayPermissionState()
            } else {
                viewModelStateFlow.update { state ->
                    state.copy(hasOverlayPermission = overlay)
                }
            }
        }
    }

    val uiState: StateFlow<MainActivityUiState> = MutableStateFlow(
        MainActivityUiState(
            selectedDirectoryPath = null,
            availableCalendars = listOf(),
            selectedCalendarIds = listOf(),
            hasCalendarPermission = false,
            imageSwitchIntervalSeconds = 30,
            selectedCalendar = "",
            hasOverlayPermission = false,
            alertCalendarIds = listOf(),
            selectedAlertCalendar = "",
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            combine(
                viewModelStateFlow,
                settingsRepository.settingsFlow,
                settingsRepository.getAlertCalendarIdsFlow(),
            ) { viewModelState, settings, alertCalendarIds ->
                val availableCalendarItems = viewModelState.availableCalendars.map { calendarInfo ->
                    createCalendarItem(calendarInfo, settings.selectedCalendarIdsList)
                }

                MainActivityUiState(
                    selectedDirectoryPath = settings.imageDirectoryUri
                        .ifEmpty { null },
                    availableCalendars = availableCalendarItems,
                    selectedCalendarIds = settings.selectedCalendarIdsList,
                    hasCalendarPermission = viewModelState.hasCalendarPermission,
                    imageSwitchIntervalSeconds = if (settings.imageSwitchIntervalSeconds == 0) {
                        30
                    } else {
                        settings.imageSwitchIntervalSeconds
                    },
                    selectedCalendar = settings.selectedCalendarIdsList
                        .mapNotNull { calendarId ->
                            viewModelState.availableCalendars.find { it.id == calendarId }
                                ?.displayName
                        }
                        .joinToString(", "),
                    hasOverlayPermission = viewModelState.hasOverlayPermission,
                    alertCalendarIds = alertCalendarIds,
                    selectedAlertCalendar = alertCalendarIds
                        .mapNotNull { calendarId ->
                            viewModelState.availableCalendars.find { it.id == calendarId }
                                ?.displayName
                        }
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") ?: "未選択",
                    listener = listener,
                )
            }.collectLatest { newUiState ->
                uiStateFlow.update { newUiState }
            }
        }
    }.asStateFlow()

    private fun createCalendarItem(
        calendarInfo: CalendarInfo,
        selectedCalendarIds: List<Long>,
    ): MainActivityUiState.CalendarItem {
        return MainActivityUiState.CalendarItem(
            id = calendarInfo.id,
            displayName = calendarInfo.displayName,
            accountName = calendarInfo.accountName,
            color = calendarInfo.color,
            isSelected = calendarInfo.id in selectedCalendarIds,
            listener = object : MainActivityUiState.CalendarItemListener {
                override fun onSelectionChanged(isSelected: Boolean) {
                    listener.onCalendarSelectionChanged(calendarInfo.id, isSelected)
                }
            },
        )
    }

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

    private fun updateOverlayPermissionState() {
        viewModelScope.launch {
            val hasOverlayPermission = eventSender.send {
                it.checkOverlayPermission()
            }
            viewModelStateFlow.update { state ->
                state.copy(hasOverlayPermission = hasOverlayPermission)
            }
        }
    }

    private data class ViewModelState(
        val availableCalendars: List<CalendarInfo> = listOf(),
        val hasCalendarPermission: Boolean = false,
        val hasOverlayPermission: Boolean = false,
    )

    interface Listener {
        fun onDirectorySelected(uri: Uri)
        fun onOpenDreamSettings()
        fun checkCalendarPermission(): Boolean
        suspend fun loadAvailableCalendars(): List<CalendarInfo>
        fun onNavigateToCalendarSelection()
        fun onNavigateToCalendarDisplay()
        fun onNavigateToAlertCalendarSelection()
        fun checkOverlayPermission(): Boolean
        fun onRequestOverlayPermission()
    }
}
