package net.matsudamper.allintoolscreensaver.viewmodel

import androidx.activity.compose.ManagedActivityResultLauncher

class CalendarSelectionScreenViewModelEvent(
    private val calendarPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    private val onBackRequested: () -> Unit,
) : CalendarSelectionScreenViewModel.Event {
    override fun onCalendarPermissionLaunch() {
        calendarPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.READ_CALENDAR,
                android.Manifest.permission.WRITE_CALENDAR,
            ),
        )
    }

    override fun onBack() {
        onBackRequested()
    }
}
