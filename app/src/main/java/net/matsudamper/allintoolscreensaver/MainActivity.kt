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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.matsudamper.allintoolscreensaver.compose.CalendarSelectionScreen
import net.matsudamper.allintoolscreensaver.compose.MainScreen
import net.matsudamper.allintoolscreensaver.theme.AllInToolScreenSaverTheme
import net.matsudamper.allintoolscreensaver.viewmodel.MainActivityViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settingsManager: SettingsRepository by inject()
    private val calendarManager: CalendarRepository by inject()

    // Callback for navigation to calendar selection screen
    private var navigateToCalendarSelectionCallback: (() -> Unit)? = null

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
            // Execute the navigation callback if it's set
            navigateToCalendarSelectionCallback?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AllInToolScreenSaverTheme {
                val viewModel: MainActivityViewModel = viewModel {
                    MainActivityViewModel(
                        settingsRepository = settingsManager,
                        activityListener = activityListener,
                    )
                }

                val uiState by viewModel.uiState.collectAsState()

                // Navigation state
                var currentScreen by remember { mutableStateOf(Screen.Main) }

                // Set up navigation callback
                navigateToCalendarSelectionCallback = {
                    currentScreen = Screen.CalendarSelection
                }

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
                    when (currentScreen) {
                        Screen.Main -> {
                            MainScreen(
                                uiState = uiState,
                                onDirectoryPickerLaunch = { directoryPickerLauncher.launch(null) },
                                onCalendarPermissionLaunch = {
                                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                },
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                        Screen.CalendarSelection -> {
                            CalendarSelectionScreen(
                                availableCalendars = uiState.availableCalendars,
                                selectedCalendarIds = uiState.selectedCalendarIds,
                                hasCalendarPermission = uiState.hasCalendarPermission,
                                onCalendarSelectionChanged = { calendarId, isSelected ->
                                    uiState.listener.onCalendarSelectionChanged(calendarId, isSelected)
                                },
                                onCalendarPermissionLaunch = {
                                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                },
                                onBackClick = {
                                    currentScreen = Screen.Main
                                },
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }
                }
            }
        }
    }

    private enum class Screen {
        Main,
        CalendarSelection
    }
}
