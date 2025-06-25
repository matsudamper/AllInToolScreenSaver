package net.matsudamper.allintoolscreensaver.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.matsudamper.allintoolscreensaver.CalendarInfo
import net.matsudamper.allintoolscreensaver.compose.component.SuspendLifecycleStartEffect
import net.matsudamper.allintoolscreensaver.theme.AllInToolScreenSaverTheme

@Composable
fun MainScreen(
    uiState: MainActivityUiState,
    onDirectoryPickerLaunch: () -> Unit,
    onCalendarPermissionLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SuspendLifecycleStartEffect(Unit) {
        uiState.listener.onStart()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "オールインワンツールスクリーンセーバー",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
        }

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
                    onDirectoryPickerLaunch()
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
                            text = uiState.selectedDirectoryPath,
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
            if (!uiState.hasCalendarPermission) {
                Button(
                    onClick = {
                        onCalendarPermissionLaunch()
                    },
                ) {
                    Text("カレンダーアクセス許可")
                }
            } else {
                Text(
                    text = "カレンダー選択:",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        if (uiState.hasCalendarPermission && uiState.availableCalendars.isNotEmpty()) {
            items(uiState.availableCalendars) { calendar ->
                CalendarItem(
                    calendar = calendar,
                    isSelected = uiState.selectedCalendarIds.contains(calendar.id),
                    onSelectionChange = { isSelected ->
                        uiState.listener.onCalendarSelectionChanged(calendar.id, isSelected)
                    },
                )
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

@Composable
private fun CalendarItem(
    calendar: CalendarInfo,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
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
                checked = isSelected,
                onCheckedChange = onSelectionChange,
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
                // カレンダーカラーを表示
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
                    override suspend fun onStart() {}
                    override fun onDirectorySelected(uri: android.net.Uri) = Unit
                    override fun onCalendarPermissionRequested() = Unit
                    override fun onCalendarSelectionChanged(calendarId: Long, isSelected: Boolean) = Unit
                    override fun onImageSwitchIntervalChanged(seconds: Int) = Unit
                    override fun onOpenDreamSettings() = Unit
                },
            ),
            onDirectoryPickerLaunch = { },
            onCalendarPermissionLaunch = { },
        )
    }
}
