package net.matsudamper.allintoolscreensaver.adapter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import net.matsudamper.allintoolscreensaver.ui.notification.NotificationOverlay
import net.matsudamper.allintoolscreensaver.viewmodel.NotificationViewModel
import org.koin.core.context.GlobalContext

@Composable
fun NotificationAdapter(
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = viewModel {
        val koin = GlobalContext.get()
        NotificationViewModel(
            notificationRepository = koin.get(),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    NotificationOverlay(
        uiState = uiState,
        modifier = modifier,
    )
}
