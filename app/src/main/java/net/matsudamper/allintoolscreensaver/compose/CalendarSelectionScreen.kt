package net.matsudamper.allintoolscreensaver.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import net.matsudamper.allintoolscreensaver.compose.component.SuspendLifecycleStartEffect
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarSelectionScreenViewModel
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarSelectionScreenViewModelEvent
import org.koin.core.context.GlobalContext

@Composable
fun CalendarSelectionScreen(
    backStack: NavBackStack,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarSelectionScreen(
    uiState: CalendarSelectionScreenUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.selectionMode) {
                            CalendarSelectionMode.DISPLAY -> "カレンダー選択"
                            CalendarSelectionMode.ALERT -> "アラート対象カレンダー選択"
                        },
                    )
                },
                navigationIcon = {
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = uiState.listener::onBack,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        },
    ) { paddingValues ->
        if (!uiState.hasCalendarPermission) {
            CalendarPermissionRequestContent(
                modifier = Modifier.padding(paddingValues),
                onCalendarPermissionLaunch = uiState.listener::onCalendarPermissionLaunch,
            )
        } else {
            CalendarListContent(
                calendars = uiState.availableCalendars,
                contentPadding = paddingValues,
            )
        }
    }
}

@Composable
private fun CalendarPermissionRequestContent(
    onCalendarPermissionLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "カレンダー機能を使用するには許可が必要です",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onCalendarPermissionLaunch,
        ) {
            Text("カレンダーアクセス許可")
        }
    }
}

@Composable
private fun CalendarListContent(
    calendars: List<CalendarSelectionScreenUiState.Calendar>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = contentPadding,
    ) {
        if (calendars.isNotEmpty()) {
            items(calendars) { calendar ->
                CalendarItem(
                    calendar = calendar,
                )
            }
        } else {
            item {
                Text(
                    text = "利用可能なカレンダーがありません",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CalendarItem(
    calendar: CalendarSelectionScreenUiState.Calendar,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                calendar.listener.onSelectionChanged(!calendar.isSelected)
            },
        colors = CardDefaults.cardColors(
            containerColor = if (calendar.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = calendar.isSelected,
                onCheckedChange = {
                    calendar.listener.onSelectionChanged(it)
                },
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = calendar.displayName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = calendar.accountName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.size(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(calendar.color),
                    ),
                ) {}
            }
        }
    }
}
