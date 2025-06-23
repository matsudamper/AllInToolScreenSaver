package net.matsudamper.allintoolscreensaver.compose

import android.net.Uri
import androidx.compose.runtime.Immutable
import net.matsudamper.allintoolscreensaver.CalendarEvent

data class DigitalClockScreenUiState(
    val currentTime: String,
    val currentDate: String,
    val showAlertDialog: Boolean,
    val currentAlert: CalendarEvent?,
    val imageUri: Uri?,
    val isLoading: Boolean,
    val pagerItems: List<PagerItem>,
    val listener: Listener,
) {
    @Immutable
    interface Listener {
        suspend fun onStart()
        fun onPageChanged(newPage: Int)
    }
}

data class PagerItem(
    val id: String,
    val imageUri: Uri?,
)
