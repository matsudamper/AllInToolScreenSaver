package net.matsudamper.allintoolscreensaver.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.matsudamper.allintoolscreensaver.ui.component.SuspendLifecycleResumeEffect
import net.matsudamper.allintoolscreensaver.ui.component.SuspendLifecycleStartEffect

private val SectionHorizontalPadding = 12.dp
private val SectionLargeRadiusSize = 16.dp
private val SectionSmallRadiusSize = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainScreenUiState,
    onDirectorySelect: () -> Unit,
    onImageSwitchIntervalChange: (Int) -> Unit,
    onNotificationDisplayDurationChange: (kotlin.time.Duration) -> Unit,
    onCalendarSelect: () -> Unit,
    onAlertCalendarSelect: () -> Unit,
    onCalendarPreview: () -> Unit,
    onSlideShowPreview: () -> Unit,
    onOpenDreamSettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SuspendLifecycleResumeEffect(Unit) {
        uiState.listener.onResume()
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
                        text = "All in One Tool Screen Saver",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = paddingValues
                .plus(PaddingValues(horizontal = 16.dp))
                .plus(PaddingValues(top = 16.dp)),
        ) {
            item {
                ScreenSaverSection(
                    uiState = uiState.screenSaverSectionUiState,
                    onClickSelection = onDirectorySelect,
                    onImageSwitchIntervalChange = onImageSwitchIntervalChange,
                    onSlideShowPreview = onSlideShowPreview,
                )
            }

            item {
                CalendarSection(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.calendarSectionUiState,
                    onCalendarSelect = onCalendarSelect,
                    onClickAlertSettings = onAlertCalendarSelect,
                    onClickCalendar = onCalendarPreview,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                )
            }

            item {
                NotificationSection(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.notificationSectionUiState,
                    onNotificationDisplayDurationChange = onNotificationDisplayDurationChange,
                )
            }

            item {
                Section(
                    modifier = Modifier.fillMaxWidth(),
                    title = null,
                    contents = listOf(
                        { contentPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        onOpenDreamSettings()
                                    }
                                    .padding(contentPadding),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    textAlign = TextAlign.Center,
                                    text = "端末のスクリーンセーバー設定を開く",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun Section(
    title: String?,
    contents: List<@Composable (PaddingValues) -> Unit>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (title != null) {
            Text(
                modifier = Modifier.padding(
                    horizontal = 6.dp,
                ),
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (index in contents.indices) {
                val content = contents[index]
                val shape = if (contents.size == 1) {
                    RoundedCornerShape(
                        topStart = SectionLargeRadiusSize,
                        topEnd = SectionLargeRadiusSize,
                        bottomStart = SectionLargeRadiusSize,
                        bottomEnd = SectionLargeRadiusSize,
                    )
                } else {
                    when (index) {
                        0 -> RoundedCornerShape(
                            topStart = SectionLargeRadiusSize,
                            topEnd = SectionLargeRadiusSize,
                            bottomStart = SectionSmallRadiusSize,
                            bottomEnd = SectionSmallRadiusSize,
                        )

                        contents.lastIndex -> RoundedCornerShape(
                            topStart = SectionSmallRadiusSize,
                            topEnd = SectionSmallRadiusSize,
                            bottomStart = SectionLargeRadiusSize,
                            bottomEnd = SectionLargeRadiusSize,
                        )

                        else -> RoundedCornerShape(
                            topStart = SectionSmallRadiusSize,
                            topEnd = SectionSmallRadiusSize,
                            bottomStart = SectionSmallRadiusSize,
                            bottomEnd = SectionSmallRadiusSize,
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp)
                        .height(IntrinsicSize.Min),
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    content(
                        PaddingValues(
                            vertical = 12.dp,
                            horizontal = SectionHorizontalPadding,
                        ),
                    )
                }
                if (index != contents.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun ScreenSaverSection(
    uiState: ScreenSaverSectionUiState,
    onClickSelection: () -> Unit,
    onImageSwitchIntervalChange: (Int) -> Unit,
    onSlideShowPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(
        modifier = modifier,
        title = "スクリーンセーバー",
        contents = listOf(
            { paddingValues ->
                Column(
                    modifier = Modifier
                        .clickable(
                            onClick = onClickSelection,
                            indication = null,
                            interactionSource = null,
                        )
                        .padding(paddingValues),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "表示フォルダ",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = uiState.selectedDirectoryPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            { paddingValues ->
                Column(
                    modifier = Modifier.padding(paddingValues),
                ) {
                    Text(
                        text = "画像切り替え時間",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ImageSwitchIntervalSelector(
                        intervalOptions = uiState.intervalOptions,
                        onIntervalSelect = onImageSwitchIntervalChange,
                    )
                }
            },
            { paddingValues ->
                Text(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            onSlideShowPreview()
                        }
                        .padding(paddingValues),
                    text = "プレビュー",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
        ),
    )
}

@Composable
private fun CalendarSection(
    uiState: CalendarSectionUiState,
    onCalendarSelect: () -> Unit,
    onClickCalendar: () -> Unit,
    onClickAlertSettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(
        modifier = modifier,
        title = "カレンダー",
        contents = listOf(
            { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCalendarSelect()
                        }
                        .padding(paddingValues),
                ) {
                    Text(
                        text = "カレンダーを選択",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.selectedCalendarDisplayName,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (uiState.hasOverlayPermission) {
                                onClickAlertSettings()
                            } else {
                                onRequestOverlayPermission()
                            }
                        }
                        .padding(paddingValues),
                ) {
                    Text(
                        text = "アラート対象カレンダー選択",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (uiState.hasOverlayPermission) {
                        Text(
                            text = uiState.selectedAlertCalendarDisplayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = "※ オーバーレイ権限が必要です",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            { paddingValues ->
                Text(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            onClickCalendar()
                        }
                        .padding(paddingValues),
                    text = "プレビュー",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
        ),
    )
}

@Composable
private fun ImageSwitchIntervalSelector(
    intervalOptions: List<IntervalOption>,
    onIntervalSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            intervalOptions.forEach { option ->
                Button(
                    onClick = {
                        onIntervalSelect(option.seconds)
                    },
                    colors = if (option.isSelected) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                ) {
                    Text(text = option.displayText)
                }
            }
        }
    }
}

@Composable
private fun NotificationSection(
    uiState: NotificationSectionUiState,
    onNotificationDisplayDurationChange: (kotlin.time.Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(
        modifier = modifier,
        title = "通知",
        contents = listOf(
            { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            uiState.listener.onOpenNotificationListenerSettings()
                        }
                        .padding(paddingValues),
                ) {
                    Text(
                        text = "通知アクセス権限設定",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.hasNotificationListenerPermission) {
                            "権限が許可されています"
                        } else {
                            "通知アクセス権限が必要です"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.hasNotificationListenerPermission) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            },
            { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            uiState.listener.onClickSendTestNotification()
                        }
                        .padding(paddingValues),
                ) {
                    Text(
                        text = "テスト通知を送信",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.hasNotificationPermission) {
                            "5秒後に送信"
                        } else {
                            "許可されていません"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            { paddingValues ->
                Column(
                    modifier = Modifier.padding(paddingValues),
                ) {
                    Text(
                        text = "通知表示時間",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NotificationDisplayDurationSelector(
                        durationOptions = uiState.displayDurationOptions,
                        onDurationSelect = onNotificationDisplayDurationChange,
                    )
                }
            },
        ),
    )
}
