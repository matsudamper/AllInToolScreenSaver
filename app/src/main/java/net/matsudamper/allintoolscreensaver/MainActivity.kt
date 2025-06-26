package net.matsudamper.allintoolscreensaver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import net.matsudamper.allintoolscreensaver.compose.CalendarSelectionScreen
import net.matsudamper.allintoolscreensaver.compose.CalendarSelectionScreenUiState
import net.matsudamper.allintoolscreensaver.compose.MainScreen
import net.matsudamper.allintoolscreensaver.theme.AllInToolScreenSaverTheme
import net.matsudamper.allintoolscreensaver.viewmodel.MainActivityViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settingsManager: SettingsRepository by inject()
    private val calendarManager: CalendarRepository by inject()

    @Serializable
    data object Main : NavKey

    @Serializable
    data object CalendarSelection : NavKey

    private val activityListener = object : MainActivityViewModel.ActivityListener {
        override fun onDirectorySelected(uri: Uri) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }

        override fun onOpenDreamSettings() {
            val intent = Intent(Settings.ACTION_DREAM_SETTINGS)
            startActivity(intent)
        }

        override fun checkCalendarPermission(): Boolean {
            return ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_CALENDAR,
            ) == PackageManager.PERMISSION_GRANTED
        }

        override suspend fun loadAvailableCalendars(): List<CalendarInfo> {
            return calendarManager.getAvailableCalendars()
        }

        override fun onNavigateToCalendarSelection() {
            // This will be called from the ViewModel, but navigation is now handled by Navigation 3
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AllInToolScreenSaverTheme {
                AppNavigation()
            }
        }
    }

    @Composable
    private fun AppNavigation() {
        val viewModel: MainActivityViewModel = viewModel {
            MainActivityViewModel(
                settingsRepository = settingsManager,
                activityListener = activityListener,
            )
        }

        val uiState by viewModel.uiState.collectAsState()

        val backStack = rememberNavBackStack(Main)

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
            viewModel.updateCalendarPermission(isGranted)
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(innerPadding),
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<Main> {
                        MainScreen(
                            uiState = uiState,
                            onDirectoryPickerLaunch = { directoryPickerLauncher.launch(null) },
                            onNavigateToCalendarSelection = {
                                backStack.add(CalendarSelection)
                            },
                            modifier = Modifier,
                        )
                    }
                    entry<CalendarSelection> {
                        CalendarSelectionScreen(
                            uiState = CalendarSelectionScreenUiState(
                                availableCalendars = uiState.availableCalendars,
                                selectedCalendarIds = uiState.selectedCalendarIds,
                                hasCalendarPermission = uiState.hasCalendarPermission,
                            ),
                            listener = object : CalendarSelectionScreenUiState.Listener {
                                override fun onCalendarSelect(calendarId: Long, isSelected: Boolean) {
                                    uiState.listener.onCalendarSelectionChanged(calendarId, isSelected)
                                }

                                override fun onCalendarPermissionLaunch() {
                                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                }

                                override fun onBack() {
                                    backStack.removeLastOrNull()
                                }
                            },
                            modifier = Modifier,
                        )
                    }
                },
            )
        }
    }
}
