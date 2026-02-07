package net.matsudamper.allintoolscreensaver.ui.slideshow

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment

data class SlideshowUiState(
    val showAlertDialog: Boolean,
    val imageUri: Uri?,
    val isLoading: Boolean,
    val pagerItems: List<PagerItem>,
    val imageSwitchIntervalSeconds: Int,
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
    val alignment: Alignment,
)
