package net.matsudamper.allintoolscreensaver.viewmodel

import androidx.activity.compose.ManagedActivityResultLauncher

class CalendarSelectionScreenViewModelEvent(
    private val calendarPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    private val onBackRequested: () -> Unit,
) : CalendarSelectionScreenViewModel.Event {
    override fun onCalendarPermissionLaunch() {
        calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
    }

    override fun onBack() {
        onBackRequested()
    }
}
