package net.matsudamper.allintoolscreensaver.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

private val SectionHorizontalPadding = 12.dp

private val SectionLargeRadiusSize = 16.dp
private val SectionSmallRadiusSize = 8.dp

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
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = paddingValues,
        ) {
            item {
                ScreenSaverSection(
                    selectionPath = uiState.selectedDirectoryPath.orEmpty(),
                    onClickSelection = {
                        directoryPickerLauncher.launch(null)
                    },
                    imageSwitchIntervalSeconds = uiState.imageSwitchIntervalSeconds,
                    onImageSwitchIntervalChanged = { seconds ->
                        uiState.listener.onImageSwitchIntervalChanged(seconds)
                    },
                )
            }

            item {
                CalendarSection(
                    modifier = Modifier.fillMaxWidth(),
                    selectedCalendar = uiState.selectedCalendar,
                    onCalendarSelected = {
                        if (uiState.hasCalendarPermission) {
                            uiState.listener.onNavigateToCalendarSelection()
                        } else {
                            calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                        }
                    },
                )
            }

            item {
                Section(
                    modifier = modifier,
                    title = null,
                    contents = listOf(
                        { contentPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        uiState.listener.onOpenDreamSettings()
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
    selectionPath: String,
    imageSwitchIntervalSeconds: Int,
    onClickSelection: () -> Unit,
    onImageSwitchIntervalChanged: (Int) -> Unit,
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
                        text = selectionPath,
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
                        currentInterval = imageSwitchIntervalSeconds,
                        onIntervalSelect = { seconds ->
                            onImageSwitchIntervalChanged(seconds)
                        },
                    )
                }
            },
        ),
    )
}

@Composable
private fun CalendarSection(
    modifier: Modifier = Modifier,
    onCalendarSelected: () -> Unit,
    selectedCalendar: String,
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
                            onCalendarSelected()
                        }
                        .padding(paddingValues),
                ) {
                    Text(
                        text = "カレンダーを選択",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedCalendar,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            { paddingValues ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues),
                    text = "アラート機能",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
        ),
    )
}

@Composable
private fun ImageSwitchIntervalSelector(
    currentInterval: Int,
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
            listOf(5, 15, 30, 60).forEach { seconds ->
                Button(
                    onClick = {
                        onIntervalSelect(seconds)
                    },
                    colors = if (currentInterval == seconds) {
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
                selectedCalendar = "未選択",
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
