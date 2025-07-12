package net.matsudamper.allintoolscreensaver.viewmodel

import android.Manifest
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.feature.InMemoryCache
import net.matsudamper.allintoolscreensaver.feature.calendar.CalendarRepository
import net.matsudamper.allintoolscreensaver.feature.setting.SettingsRepository
import net.matsudamper.allintoolscreensaver.lib.EventSender
import net.matsudamper.allintoolscreensaver.ui.main.CalendarSectionUiState
import net.matsudamper.allintoolscreensaver.ui.main.IntervalOption
import net.matsudamper.allintoolscreensaver.ui.main.MainScreenUiState
import net.matsudamper.allintoolscreensaver.ui.main.NotificationSectionUiState
import net.matsudamper.allintoolscreensaver.ui.main.ScreenSaverSectionUiState

class MainScreenViewModel(
    private val settingsRepository: SettingsRepository,
    private val inMemoryCache: InMemoryCache,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val eventSender = EventSender<Listener>()
    val eventHandler = eventSender.asHandler()

    private val listener = object : MainScreenUiState.Listener {
        override suspend fun onStart() {
            coroutineScope {
                launch {
                    loadAvailableCalendars()
                    updateOverlayPermissionState()
                    updateNotificationPermissionState()
                }
            }
        }

        override suspend fun onResume() {
            coroutineScope {
                checkCalendarPermission()
                updateNotificationPermissionState()
            }
        }

        override fun onNavigateToCalendarDisplay() {
            viewModelScope.launch {
                eventSender.send {
                    it.onNavigateToCalendarDisplay()
                }
            }
        }

        override fun onNavigateToSlideShowPreview() {
            viewModelScope.launch {
                eventSender.send {
                    it.onNavigateToSlideShowPreview()
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

    val notificationSettingListener = object : NotificationSectionUiState.Listener {
        override fun onClickSendTestNotification() {
            viewModelScope.launch {
                suspend fun sendTestNotification() {
                    delay(5.seconds)
                    eventSender.send {
                        it.onSendNotification()
                    }
                }
                if (viewModelStateFlow.value.hasNotificationPermission) {
                    sendTestNotification()
                } else {
                    val result = eventSender.send {
                        it.requestPermission(
                            ActivityResultContracts.RequestPermission(),
                            Manifest.permission.POST_NOTIFICATIONS,
                        )
                    }
                    updateNotificationPermissionState()
                    if (result) {
                        sendTestNotification()
                    }
                }
            }
        }

        override fun onOpenNotificationListenerSettings() {
            viewModelScope.launch {
                eventSender.send {
                    it.onOpenNotificationListenerSettings()
                }
            }
        }
    }

    val uiState: StateFlow<MainScreenUiState> = MutableStateFlow(
        MainScreenUiState(
            screenSaverSectionUiState = ScreenSaverSectionUiState(
                selectedDirectoryPath = "",
                imageSwitchIntervalSeconds = 30,
                intervalOptions = listOf(),
            ),
            calendarSectionUiState = CalendarSectionUiState(
                selectedCalendarDisplayName = "",
                selectedAlertCalendarDisplayName = "",
                hasOverlayPermission = false,
                hasCalendarPermission = false,
            ),
            notificationSectionUiState = NotificationSectionUiState(
                hasNotificationPermission = false,
                hasNotificationListenerPermission = false,
                listener = notificationSettingListener,
            ),
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            combine(
                viewModelStateFlow,
                settingsRepository.settingsFlow,
                settingsRepository.getAlertCalendarIdsFlow(),
            ) { viewModelState, settings, alertCalendarIds ->
                val intervalOptions = listOf(5, 15, 30, 60).map { seconds ->
                    IntervalOption(
                        seconds = seconds,
                        displayText = "${seconds}秒",
                        isSelected = (if (settings.imageSwitchIntervalSeconds == 0) 30 else settings.imageSwitchIntervalSeconds) == seconds,
                    )
                }

                MainScreenUiState(
                    screenSaverSectionUiState = ScreenSaverSectionUiState(
                        selectedDirectoryPath = settings.imageDirectoryUri.ifEmpty { null }.orEmpty(),
                        imageSwitchIntervalSeconds = if (settings.imageSwitchIntervalSeconds == 0) {
                            30
                        } else {
                            settings.imageSwitchIntervalSeconds
                        },
                        intervalOptions = intervalOptions,
                    ),
                    calendarSectionUiState = CalendarSectionUiState(
                        selectedCalendarDisplayName = settings.selectedCalendarIdsList
                            .mapNotNull { calendarId ->
                                viewModelState.availableCalendars.find { it.id == calendarId }
                                    ?.displayName
                            }
                            .joinToString(", "),
                        selectedAlertCalendarDisplayName = alertCalendarIds
                            .mapNotNull { calendarId ->
                                viewModelState.availableCalendars.find { it.id == calendarId }
                                    ?.displayName
                            }
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(", ") ?: "未選択",
                        hasOverlayPermission = viewModelState.hasOverlayPermission,
                        hasCalendarPermission = viewModelState.hasCalendarPermission,
                    ),
                    notificationSectionUiState = NotificationSectionUiState(
                        hasNotificationPermission = viewModelState.hasNotificationPermission,
                        hasNotificationListenerPermission = viewModelState.hasNotificationListenerPermission,
                        listener = notificationSettingListener,
                    ),
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

    private fun updateNotificationPermissionState() {
        viewModelScope.launch {
            val hasNotificationPermission = eventSender.send {
                it.checkNotificationPermission()
            }
            val hasNotificationListenerPermission = eventSender.send {
                it.checkNotificationListenerPermission()
            }
            viewModelStateFlow.update { state ->
                state.copy(
                    hasNotificationPermission = hasNotificationPermission,
                    hasNotificationListenerPermission = hasNotificationListenerPermission,
                )
            }
        }
    }

    private data class ViewModelState(
        val availableCalendars: List<CalendarRepository.CalendarInfo> = listOf(),
        val hasCalendarPermission: Boolean = false,
        val hasOverlayPermission: Boolean = false,
        val hasNotificationPermission: Boolean = false,
        val hasNotificationListenerPermission: Boolean = false,
    )

    interface Listener {
        fun onDirectorySelected(uri: Uri)
        fun onOpenDreamSettings()
        fun checkCalendarPermission(): Boolean
        suspend fun loadAvailableCalendars(): List<CalendarRepository.CalendarInfo>
        fun onNavigateToCalendarSelection()
        fun onNavigateToCalendarDisplay()
        fun onNavigateToSlideShowPreview()
        fun onNavigateToAlertCalendarSelection()
        fun checkOverlayPermission(): Boolean
        fun onRequestOverlayPermission()
        fun onSendNotification()
        fun onOpenNotificationListenerSettings()
        fun checkNotificationPermission(): Boolean
        fun checkNotificationListenerPermission(): Boolean
        suspend fun <I, O> requestPermission(
            contract: ActivityResultContract<I, O>,
            input: I,
        ): O
    }
}
