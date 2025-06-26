package net.matsudamper.allintoolscreensaver.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import net.matsudamper.allintoolscreensaver.compose.component.SuspendLifecycleStartEffect
import net.matsudamper.allintoolscreensaver.theme.AllInToolScreenSaverTheme
import net.matsudamper.allintoolscreensaver.viewmodel.MainScreenViewModel
import net.matsudamper.allintoolscreensaver.viewmodel.MainScreenViewModelListenerImpl
import org.koin.core.context.GlobalContext

@Composable
fun MainScreen(
    backStack: NavBackStack,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel {
        val koin = GlobalContext.get()
        MainScreenViewModel(
            settingsRepository = koin.get(),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsState()

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

    MainScreen(
        uiState = uiState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    uiState: MainActivityUiState,
    modifier: Modifier = Modifier,
) {
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
        uiState.listener.updateCalendarPermission(isGranted)
    }

    SuspendLifecycleStartEffect(Unit) {
        uiState.listener.onStart()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "オールインワンツールスクリーンセーバー",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = paddingValues,
        ) {
            item {
                Text(
                    text = "このアプリはデジタル時計、画像表示、カレンダー機能を持つスクリーンセーバーです。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            item {
                Button(
                    onClick = {
                        directoryPickerLauncher.launch(null)
                    },
                ) {
                    Text("画像フォルダを選択")
                }
            }

            if (uiState.selectedDirectoryPath != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                text = "選択されたフォルダ:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.selectedDirectoryPath.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "画像切り替え時間:",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            item {
                ImageSwitchIntervalSelector(
                    currentInterval = uiState.imageSwitchIntervalSeconds,
                    onIntervalSelect = { seconds ->
                        uiState.listener.onImageSwitchIntervalChanged(seconds)
                    },
                )
            }

            item {
                Button(
                    onClick = {
                        if (uiState.hasCalendarPermission) {
                            uiState.listener.onNavigateToCalendarSelection()
                        } else {
                            calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                        }
                    },
                ) {
                    Text("カレンダー選択画面へ")
                }
            }

            item {
                Button(
                    onClick = {
                        uiState.listener.onOpenDreamSettings()
                    },
                ) {
                    Text("スクリーンセーバー設定を開く")
                }
            }

            item {
                Text(
                    text = "設定画面で「オールインワンツールスクリーンセーバー」を選択してください。",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "アラート機能について:",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            item {
                Text(
                    text = "選択されたカレンダーの予定開始時刻に自動でアラートが鳴ります。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ImageSwitchIntervalSelector(
    currentInterval: Int,
    onIntervalSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf(5, 15, 30, 60).forEach { seconds ->
                    Button(
                        onClick = {
                            onIntervalSelect(seconds)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentInterval == seconds) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                    ) {
                        Text(
                            text = when (seconds) {
                                5 -> "5秒"
                                15 -> "15秒"
                                30 -> "30秒"
                                60 -> "1分"
                                else -> "${seconds}秒"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    AllInToolScreenSaverTheme {
        MainScreen(
            uiState = MainActivityUiState(
                selectedDirectoryPath = null,
                availableCalendars = listOf(),
                selectedCalendarIds = listOf(),
                hasCalendarPermission = false,
                imageSwitchIntervalSeconds = 30,
                listener = object : MainActivityUiState.Listener {
                    override suspend fun onStart() = Unit
                    override fun onDirectorySelected(uri: android.net.Uri) = Unit
                    override fun onCalendarPermissionRequested() = Unit
                    override fun onCalendarSelectionChanged(calendarId: Long, isSelected: Boolean) = Unit
                    override fun onImageSwitchIntervalChanged(seconds: Int) = Unit
                    override fun onOpenDreamSettings() = Unit
                    override fun onNavigateToCalendarSelection() = Unit
                    override fun updateCalendarPermission(isGranted: Boolean) = Unit
                },
            ),
        )
    }
}
