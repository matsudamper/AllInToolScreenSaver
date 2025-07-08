package net.matsudamper.allintoolscreensaver.adapter

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import net.matsudamper.allintoolscreensaver.ui.component.SuspendLifecycleResumeEffect
import net.matsudamper.allintoolscreensaver.ui.component.SuspendLifecycleStartEffect
import net.matsudamper.allintoolscreensaver.ui.main.CalendarSectionUiState
import net.matsudamper.allintoolscreensaver.ui.main.IntervalOption
import net.matsudamper.allintoolscreensaver.ui.main.MainScreen
import net.matsudamper.allintoolscreensaver.ui.main.MainScreenUiState
import net.matsudamper.allintoolscreensaver.ui.main.ScreenSaverSectionUiState
import net.matsudamper.allintoolscreensaver.viewmodel.MainScreenViewModel
import net.matsudamper.allintoolscreensaver.viewmodel.MainScreenViewModelListenerImpl
import org.koin.core.context.GlobalContext

@Composable
fun MainScreenAdapter(
    backStack: NavBackStack,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel {
        val koin = GlobalContext.get()
        MainScreenViewModel(
            settingsRepository = koin.get(),
            inMemoryCache = koin.get(),
        )
    },
) {
    val businessUiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel.eventHandler) {
        val koin = GlobalContext.get()
        viewModel.eventHandler.collect(
            MainScreenViewModelListenerImpl(
                application = koin.get(),
                calendarManager = koin.get(),
                backStack = backStack,
            ),
        )
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            businessUiState.listener.onDirectorySelected(uri)
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        businessUiState.listener.updatePermissions(calendar = isGranted)
    }

    SuspendLifecycleResumeEffect(Unit) {
        businessUiState.listener.onResume()
    }

    SuspendLifecycleStartEffect(Unit) {
        businessUiState.listener.onStart()
    }

    val intervalOptions = listOf(5, 15, 30, 60).map { seconds ->
        IntervalOption(
            seconds = seconds,
            displayText = "${seconds}秒",
            isSelected = businessUiState.imageSwitchIntervalSeconds == seconds,
        )
    }

    val uiState = MainScreenUiState(
        screenSaverSectionUiState = ScreenSaverSectionUiState(
            selectedDirectoryPath = businessUiState.selectedDirectoryPath.orEmpty(),
            imageSwitchIntervalSeconds = businessUiState.imageSwitchIntervalSeconds,
            intervalOptions = intervalOptions,
        ),
        calendarSectionUiState = CalendarSectionUiState(
            selectedCalendarDisplayName = businessUiState.selectedCalendar,
            selectedAlertCalendarDisplayName = businessUiState.selectedAlertCalendar,
            hasOverlayPermission = businessUiState.hasOverlayPermission,
            hasCalendarPermission = businessUiState.hasCalendarPermission,
        ),
    )

    MainScreen(
        uiState = uiState,
        onDirectorySelect = {
            directoryPickerLauncher.launch(null)
        },
        onImageSwitchIntervalChange = { seconds ->
            businessUiState.listener.onImageSwitchIntervalChanged(seconds)
        },
        onCalendarSelect = {
            if (businessUiState.hasCalendarPermission) {
                businessUiState.listener.onNavigateToCalendarSelection()
            } else {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        },
        onAlertCalendarSelect = {
            if (businessUiState.hasCalendarPermission) {
                businessUiState.listener.onNavigateToAlertCalendarSelection()
            } else {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        },
        onCalendarPreview = {
            businessUiState.listener.onNavigateToCalendarDisplay()
        },
        onSlideShowPreview = {
            businessUiState.listener.onNavigateToSlideShowPreview()
        },
        onOpenDreamSettings = {
            businessUiState.listener.onOpenDreamSettings()
        },
        onRequestOverlayPermission = {
            businessUiState.listener.onRequestOverlayPermission()
        },
        modifier = modifier,
    )
}
