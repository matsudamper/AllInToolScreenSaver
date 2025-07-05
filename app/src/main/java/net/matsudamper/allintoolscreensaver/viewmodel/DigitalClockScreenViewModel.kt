package net.matsudamper.allintoolscreensaver.viewmodel

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.ImageManager
import net.matsudamper.allintoolscreensaver.InMemoryCache
import net.matsudamper.allintoolscreensaver.SettingsRepository
import net.matsudamper.allintoolscreensaver.compose.DigitalClockScreenUiState
import net.matsudamper.allintoolscreensaver.compose.PagerItem

class DigitalClockScreenViewModel(
    private val settingsRepositor: SettingsRepository,
    private val imageManager: ImageManager,
    private val inMemoryCache: InMemoryCache,
    private val clock: Clock,
) : ViewModel() {
    private val viewModelStateFlow = MutableStateFlow(
        run {
            val imageInfo = inMemoryCache.imageInfo
            if (imageInfo != null) {
                ViewModelState(
                    images = imageInfo.imageUris,
                    imagesShuffledIndex = imageInfo.imagesShuffledIndex,
                    currentIndex = imageInfo.currentIndex,
                    imagesLastUpdate = Instant.now(clock),
                    isLoading = false,
                )
            } else {
                ViewModelState()
            }
        },
    )

    private val listener = object : DigitalClockScreenUiState.Listener {
        override suspend fun onStart() {
            updateImages()
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
            imageSwitchIntervalSeconds = null,
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

                val now = Instant.now(clock)
                val delayTime = 1.seconds - (now.toEpochMilli() % 1000).milliseconds
                delay(delayTime.coerceAtLeast(1.milliseconds))
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        imageUri = viewModelState.images.getOrNull(
                            viewModelState.imagesShuffledIndex.getOrNull(viewModelState.currentIndex) ?: 0,
                        ),
                        isLoading = viewModelState.isLoading,
                        pagerItems = createPagerItems(viewModelState),
                        imageSwitchIntervalSeconds = viewModelState.imageSwitchIntervalSeconds,
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepositor.getImageSwitchIntervalSecondsFlow().collectLatest { intervalSeconds ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        imageSwitchIntervalSeconds = intervalSeconds,
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

        val currentIndex = viewModelState.currentIndex
        val shuffledIndices = viewModelState.imagesShuffledIndex

        val prevIndex = if (currentIndex > 0) currentIndex - 1 else shuffledIndices.size - 1
        val nextIndex = if (currentIndex < shuffledIndices.size - 1) currentIndex + 1 else 0

        return listOf(
            PagerItem(
                id = viewModelState.getPagerImage(prevIndex).toString(),
                imageUri = viewModelState.getPagerImage(prevIndex),
            ),
            PagerItem(
                id = viewModelState.getPagerImage(currentIndex).toString(),
                imageUri = viewModelState.getPagerImage(currentIndex),
            ),
            PagerItem(
                id = viewModelState.getPagerImage(nextIndex).toString(),
                imageUri = viewModelState.getPagerImage(nextIndex),
            ),
        )
    }

    private fun moveToNextImage() {
        viewModelStateFlow.update { viewModelState ->
            val nextShuffledIndex = viewModelState.currentIndex + 1
            val nextImageIndex = viewModelState.imagesShuffledIndex.getOrNull(nextShuffledIndex)

            viewModelState.copy(
                currentIndex = if (nextImageIndex == null) {
                    0
                } else {
                    nextShuffledIndex
                },
            )
        }
    }

    private fun moveToPreviousImage() {
        viewModelStateFlow.update { viewModelState ->
            val prevShuffledIndex = if (viewModelState.currentIndex > 0) {
                viewModelState.currentIndex - 1
            } else {
                viewModelState.imagesShuffledIndex.size - 1
            }

            viewModelState.copy(
                currentIndex = prevShuffledIndex,
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
        if (viewModelStateFlow.value.images.size > 1000 &&
            viewModelStateFlow.value.imagesLastUpdate
                .plusMillis(1.hours.inWholeMilliseconds)
                .isAfter(Instant.now(clock))
        ) {
            return
        }

        val directoryUri = settingsRepositor.settingsFlow.first().imageDirectoryUri.takeIf { it.isNotEmpty() }?.toUri() ?: return updateLoadingFalse()

        if (viewModelStateFlow.value.images.isEmpty()) {
            val uris = imageManager.getImageUrisFromDirectory(directoryUri)
            val firstSize = 10
            val firstList = uris.take(firstSize).toList()
            val firstImagesShuffledIndex = firstList.indices.shuffled()
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    images = firstList,
                    imagesShuffledIndex = firstImagesShuffledIndex,
                    currentIndex = 0,
                    imagesLastUpdate = Instant.now(clock),
                    isLoading = false,
                )
            }

            val secondList = uris.drop(firstSize).toList()
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    images = firstList + secondList,
                    imagesShuffledIndex = firstImagesShuffledIndex + secondList.indices.shuffled().map { firstImagesShuffledIndex.size + it },
                    imagesLastUpdate = Instant.now(clock),
                    isLoading = false,
                )
            }
            val state = viewModelStateFlow.value
            inMemoryCache.imageInfo = InMemoryCache.ImageInfo(
                imageUris = state.images,
                imagesShuffledIndex = state.imagesShuffledIndex,
                currentIndex = state.currentIndex,
            )
        } else {
            val uris = imageManager.getImageUrisFromDirectory(directoryUri).toList()

            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    images = uris,
                    imagesShuffledIndex = uris.indices.shuffled(),
                    currentIndex = 0,
                    imagesLastUpdate = Instant.now(clock),
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
        val currentIndex: Int = 0,
        val imagesLastUpdate: Instant = Instant.MIN,
        val isLoading: Boolean = true,
        val imageSwitchIntervalSeconds: Int? = null,
    ) {
        fun getPagerImage(index: Int): Uri {
            return images[imagesShuffledIndex[index]]
        }
    }
}
