package net.matsudamper.allintoolscreensaver.viewmodel

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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.ImageManager
import net.matsudamper.allintoolscreensaver.SettingsManager
import net.matsudamper.allintoolscreensaver.compose.DigitalClockScreenUiState
import net.matsudamper.allintoolscreensaver.compose.PagerItem

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
                        val intervalSeconds = settingsManager.getImageSwitchIntervalSeconds()
                        delay(intervalSeconds.seconds)
                        moveToNextImage()
                    }
                }
            }
        }

        override fun onPageChanged(newPage: Int) {
            when (newPage) {
                0 -> moveToPreviousImage()
                2 -> moveToNextImage()
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
            pagerItems = listOf(),
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
                        pagerItems = createPagerItems(viewModelState),
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createPagerItems(viewModelState: ViewModelState): List<PagerItem> {
        if (viewModelState.images.isEmpty()) {
            return listOf(
                PagerItem(id = "left", imageUri = null),
                PagerItem(id = "center", imageUri = null),
                PagerItem(id = "right", imageUri = null),
            )
        }

        val currentIndex = viewModelState.currentShuffledIndex
        val shuffledIndices = viewModelState.imagesShuffledIndex
        val images = viewModelState.images

        val prevIndex = if (currentIndex > 0) currentIndex - 1 else shuffledIndices.size - 1
        val nextIndex = if (currentIndex < shuffledIndices.size - 1) currentIndex + 1 else 0

        fun getImageUri(index: Int): Uri? {
            return images.getOrNull(shuffledIndices.getOrNull(index) ?: 0)
        }

        return listOf(
            PagerItem(
                id = getImageUri(prevIndex).toString(),
                imageUri = getImageUri(prevIndex),
            ),
            PagerItem(
                id = getImageUri(currentIndex).toString(),
                imageUri = getImageUri(currentIndex),
            ),
            PagerItem(
                id = getImageUri(nextIndex).toString(),
                imageUri = getImageUri(nextIndex),
            ),
        )
    }

    private fun moveToNextImage() {
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

    private fun moveToPreviousImage() {
        viewModelStateFlow.update { viewModelState ->
            val prevShuffledIndex = if (viewModelState.currentShuffledIndex > 0) {
                viewModelState.currentShuffledIndex - 1
            } else {
                viewModelState.imagesShuffledIndex.size - 1
            }

            viewModelState.copy(
                currentShuffledIndex = prevShuffledIndex,
            )
        }
    }

    private suspend fun updateImages() {
        fun updateLoadingFalse() {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    isLoading = false,
                )
            }
        }

        // 1000枚以上の場合は負荷を軽減する為に更新頻度を設定
        if (viewModelStateFlow.value.images.size > 1000 && viewModelStateFlow.value.imagesLastUpdate.plusMillis(1.hours.inWholeMilliseconds).isAfter(Instant.now())) return

        val directoryUri = settingsManager.getImageDirectoryUri() ?: return updateLoadingFalse()

        if (viewModelStateFlow.value.images.isEmpty()) {
            val uris = imageManager.getImageUrisFromDirectory(directoryUri)
            val firstSize = 10
            val firstList = uris.take(firstSize).toList()
            val firstImagesShuffledIndex = firstList.indices.shuffled()
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    images = firstList,
                    imagesShuffledIndex = firstImagesShuffledIndex,
                    currentShuffledIndex = 0,
                    imagesLastUpdate = Instant.now(),
                    isLoading = false,
                )
            }

            val secondList = uris.drop(firstSize).toList()
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    images = firstList + secondList,
                    imagesShuffledIndex = firstImagesShuffledIndex + secondList.indices.shuffled(),
                    imagesLastUpdate = Instant.now(),
                    isLoading = false,
                )
            }
        } else {
            val uris = imageManager.getImageUrisFromDirectory(directoryUri).toList()

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
    }

    private fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy年MM月dd日(E)", Locale.JAPANESE)
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
