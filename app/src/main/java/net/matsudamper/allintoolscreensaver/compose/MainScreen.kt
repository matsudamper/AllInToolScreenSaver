package net.matsudamper.allintoolscreensaver.compose

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.CalendarInfo
import net.matsudamper.allintoolscreensaver.CalendarManager
import net.matsudamper.allintoolscreensaver.SettingsManager
import net.matsudamper.allintoolscreensaver.theme.AllInToolScreenSaverTheme

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val settingsManager = remember { SettingsManager(context) }
    val calendarManager = remember { CalendarManager(context) }

    var selectedDirectoryPath by remember { mutableStateOf<String?>(null) }
    var availableCalendars by remember { mutableStateOf<List<CalendarInfo>>(listOf()) }
    var selectedCalendarIds by remember { mutableStateOf<List<Long>>(listOf()) }
    var hasCalendarPermission by remember { mutableStateOf(false) }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            activity.lifecycleScope.launch {
                settingsManager.saveImageDirectoryUri(uri)
                selectedDirectoryPath = uri.toString()
                Log.d("MainActivity", "Selected directory: $uri")
            }
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasCalendarPermission = isGranted
        if (isGranted) {
            // 権限が許可されたらカレンダーを読み込む
        }
    }

    LaunchedEffect(Unit) {
        // 初期設定の読み込み
        val currentDirectoryUri = settingsManager.getImageDirectoryUri()
        if (currentDirectoryUri != null) {
            selectedDirectoryPath = currentDirectoryUri.toString()
        }

        selectedCalendarIds = settingsManager.getSelectedCalendarIds()

        // カレンダー権限チェック
        hasCalendarPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCalendarPermission) {
            availableCalendars = calendarManager.getAvailableCalendars()
        }
    }

    LaunchedEffect(hasCalendarPermission) {
        if (hasCalendarPermission) {
            availableCalendars = calendarManager.getAvailableCalendars()
        }
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
                    directoryPickerLauncher.launch(null)
                },
            ) {
                Text("画像フォルダを選択")
            }
        }

        if (selectedDirectoryPath != null) {
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
                            text = selectedDirectoryPath.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            if (!hasCalendarPermission) {
                Button(
                    onClick = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
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

        if (hasCalendarPermission && availableCalendars.isNotEmpty()) {
            items(availableCalendars) { calendar ->
                CalendarItem(
                    calendar = calendar,
                    isSelected = selectedCalendarIds.contains(calendar.id),
                    onSelectionChanged = { isSelected ->
                        val newIds = if (isSelected) {
                            selectedCalendarIds + calendar.id
                        } else {
                            selectedCalendarIds - calendar.id
                        }
                        selectedCalendarIds = newIds

                        activity.lifecycleScope.launch {
                            settingsManager.saveSelectedCalendarIds(newIds)
                        }
                    },
                )
            }
        }

        item {
            Button(
                onClick = {
                    Log.d("MainActivity", "Opening dream settings")
                    val intent = Intent(Settings.ACTION_DREAM_SETTINGS)
                    context.startActivity(intent)
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
private fun CalendarItem(
    calendar: CalendarInfo,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                onCheckedChange = onSelectionChanged,
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
        MainScreen()
    }
}
