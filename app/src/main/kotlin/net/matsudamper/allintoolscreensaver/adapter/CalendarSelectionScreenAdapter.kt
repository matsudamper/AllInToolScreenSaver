package net.matsudamper.allintoolscreensaver.adapter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import net.matsudamper.allintoolscreensaver.ui.calendar.CalendarSelectionScreen
import net.matsudamper.allintoolscreensaver.ui.component.SuspendLifecycleStartEffect
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarSelectionScreenViewModel
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarSelectionScreenViewModelEvent
import org.koin.core.context.GlobalContext

@Composable
fun CalendarSelectionScreenAdapter(
    backStack: NavBackStack<*>,
    modifier: Modifier = Modifier,
    viewModel: CalendarSelectionScreenViewModel = viewModel {
        val koin = GlobalContext.get()
        CalendarSelectionScreenViewModel(
            calendarRepository = koin.get(),
            settingsRepository = koin.get(),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        uiState.listener.updateCalendarPermission(isGranted)
    }

    LaunchedEffect(viewModel.eventHandler) {
        viewModel.eventHandler.collect(
            CalendarSelectionScreenViewModelEvent(
                calendarPermissionLauncher = calendarPermissionLauncher,
                onBackRequested = { backStack.removeLastOrNull() },
            ),
        )
    }
    SuspendLifecycleStartEffect(uiState.listener) {
        uiState.listener.onStart()
    }

    CalendarSelectionScreen(
        uiState = uiState,
        modifier = modifier,
    )
}
