package net.matsudamper.allintoolscreensaver.adapter

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import net.matsudamper.allintoolscreensaver.ActivityResultRequest
import net.matsudamper.allintoolscreensaver.ui.main.MainScreen
import net.matsudamper.allintoolscreensaver.viewmodel.MainScreenViewModel
import net.matsudamper.allintoolscreensaver.viewmodel.MainScreenViewModelListenerImpl
import org.koin.core.context.GlobalContext

@Composable
fun MainScreenAdapter(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel {
        val koin = GlobalContext.get()
        MainScreenViewModel(
            settingsRepository = koin.get(),
            inMemoryCache = koin.get(),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            uiState.listener.onDirectorySelected(uri)
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        uiState.listener.updatePermissions(calendar = isGranted)
    }

    val requestState = remember {
        mutableStateOf<ActivityResultRequest<Any, Any>?>(null)
    }
    run {
        val request = requestState.value
        if (request != null) {
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = request.contract,
            ) {
                request.result(it)
            }
            LaunchedEffect(Unit) {
                Log.d("LOG", "launch")
                notificationPermissionLauncher.launch(request.input)
            }
        }
    }

    LaunchedEffect(viewModel.eventHandler) {
        val koin = GlobalContext.get()
        viewModel.eventHandler.collect(
            MainScreenViewModelListenerImpl(
                application = koin.get(),
                calendarManager = koin.get(),
                backStack = backStack,
                activityResultRequest = {
                    requestState.value = it
                },
            ),
        )
    }

    MainScreen(
        uiState = uiState,
        onDirectorySelect = {
            directoryPickerLauncher.launch(null)
        },
        onImageSwitchIntervalChange = { seconds ->
            uiState.listener.onImageSwitchIntervalChanged(seconds)
        },
        onNotificationDisplayDurationChange = { duration ->
            uiState.listener.onNotificationDisplayDurationChanged(duration)
        },
        onCalendarSelect = {
            if (uiState.calendarSectionUiState.hasCalendarPermission) {
                uiState.listener.onNavigateToCalendarSelection()
            } else {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        },
        onAlertCalendarSelect = {
            if (uiState.calendarSectionUiState.hasCalendarPermission) {
                uiState.listener.onNavigateToAlertCalendarSelection()
            } else {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        },
        onCalendarPreview = {
            uiState.listener.onNavigateToCalendarDisplay()
        },
        onSlideShowPreview = {
            uiState.listener.onNavigateToSlideShowPreview()
        },
        onNotificationPreview = {
            uiState.listener.onNavigateToNotificationPreview()
        },
        onOpenDreamSettings = {
            uiState.listener.onOpenDreamSettings()
        },
        onRequestOverlayPermission = {
            uiState.listener.onRequestOverlayPermission()
        },
        modifier = modifier,
    )
}
