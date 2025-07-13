package net.matsudamper.allintoolscreensaver.adapter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import net.matsudamper.allintoolscreensaver.ui.notification.NotificationOverlay
import net.matsudamper.allintoolscreensaver.viewmodel.NotificationViewModel
import org.koin.core.context.GlobalContext

@Composable
fun NotificationAdapter(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = viewModel {
        val koin = GlobalContext.get()
        NotificationViewModel(
            notificationRepository = koin.get(),
            settingsRepository = koin.get(),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    NotificationOverlay(
        uiState = uiState,
        hazeState = hazeState,
        modifier = modifier,
    )
}
