package net.matsudamper.allintoolscreensaver.compose

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.ImageManager
import net.matsudamper.allintoolscreensaver.SettingsManager

class DigitalClockScreenViewModel(
    private val settingsManager: SettingsManager,
    private val imageManager: ImageManager,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val listener = object : DigitalClockScreenUiState.Listener {
        override suspend fun onStart() {
            coroutineScope {
                launch {
                    updateImages()
                }
                launch {
                    while (isActive) {
                        delay(30.seconds)
                        viewModelStateFlow.update { viewModelState ->
                            val nextShuffledIndex = viewModelState.currentShuffledIndex + 1
                            val nextImageIndex = viewModelState.imagesShuffledIndex.getOrNull(nextShuffledIndex)

                            viewModelState.copy(
                                currentShuffledIndex = if (nextImageIndex == null) {
                                    0
                                } else {
                                    nextShuffledIndex
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    val uiState: StateFlow<DigitalClockScreenUiState> = MutableStateFlow(
        DigitalClockScreenUiState(
            currentTime = getCurrentTime(),
            currentDate = getCurrentDate(),
            showAlertDialog = false,
            currentAlert = null,
            imageUri = null,
            isLoading = true,
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            while (isActive) {
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        currentTime = getCurrentTime(),
                        currentDate = getCurrentDate(),
                    )
                }

                val now = Instant.now()
                val delayTime = 1.seconds - (now.toEpochMilli() % 1000).milliseconds
                delay(delayTime.coerceAtLeast(1.milliseconds))
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        imageUri = viewModelState.images.getOrNull(
                            viewModelState.imagesShuffledIndex.getOrNull(viewModelState.currentShuffledIndex) ?: 0,
                        ),
                        isLoading = viewModelState.isLoading,
                    )
                }
            }
        }
    }.asStateFlow()

    private suspend fun updateImages() {
        fun updateLoadingFalse() {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    isLoading = false,
                )
            }
        }

        val directoryUri = settingsManager.getImageDirectoryUri() ?: return updateLoadingFalse()
        val uris = imageManager.getImageUrisFromDirectory(directoryUri)

        if (viewModelStateFlow.value.images == uris) return updateLoadingFalse()
        // 1000枚以上の場合は負荷を軽減する為に更新頻度を設定
        if (uris.size > 1000 && viewModelStateFlow.value.imagesLastUpdate.plusMillis(1.hours.inWholeMilliseconds).isAfter(Instant.now())) return

        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                images = uris,
                imagesShuffledIndex = uris.indices.shuffled(),
                currentShuffledIndex = 0,
                imagesLastUpdate = Instant.now(),
                isLoading = false,
            )
        }
    }

    private fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy年MM月dd日 (E)", Locale.JAPANESE)
        return formatter.format(Date())
    }

    data class ViewModelState(
        val images: List<Uri> = listOf(),
        val imagesShuffledIndex: List<Int> = listOf(),
        val currentShuffledIndex: Int = 0,
        val imagesLastUpdate: Instant = Instant.MIN,
        val isLoading: Boolean = true,
    )
}
