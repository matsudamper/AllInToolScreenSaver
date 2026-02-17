package net.matsudamper.allintoolscreensaver.viewmodel

import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.matsudamper.allintoolscreensaver.feature.FaceDetectionManager
import net.matsudamper.allintoolscreensaver.feature.ImageManager
import net.matsudamper.allintoolscreensaver.feature.InMemoryCache
import net.matsudamper.allintoolscreensaver.feature.setting.SettingsRepository
import net.matsudamper.allintoolscreensaver.ui.slideshow.PagerItem
import net.matsudamper.allintoolscreensaver.ui.slideshow.SlideshowUiState

class SlideshowScreenViewModel(
    private val settingsRepositor: SettingsRepository,
    private val imageManager: ImageManager,
    private val inMemoryCache: InMemoryCache,
    private val clock: Clock,
    private val faceDetectionManager: FaceDetectionManager,
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

    private val listener = object : SlideshowUiState.Listener {
        override suspend fun onStart() {
            updateImages()
        }

        override fun onPageChanged(newPage: Int) {
            val centerPage = 2
            val offset = newPage - centerPage
            if (offset != 0) {
                moveByOffset(offset)
            }
        }
    }

    val uiState: StateFlow<SlideshowUiState> = MutableStateFlow(
        SlideshowUiState(
            showAlertDialog = false,
            imageUri = null,
            isLoading = true,
            pagerItems = listOf(),
            imageSwitchIntervalSeconds = defaultSlideshowDuration.inWholeSeconds.toInt(),
            listener = listener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        imageUri = viewModelState.images.getOrNull(
                            viewModelState.imagesShuffledIndex.getOrNull(viewModelState.currentIndex)
                                ?: 0,
                        ),
                        isLoading = viewModelState.isLoading,
                        pagerItems = createPagerItems(viewModelState),
                        imageSwitchIntervalSeconds = viewModelState.imageSwitchIntervalSeconds
                            ?: defaultSlideshowDuration.inWholeSeconds.toInt(),
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
                PagerItem(id = "left2", imageUri = null, alignment = Alignment.Center),
                PagerItem(id = "left1", imageUri = null, alignment = Alignment.Center),
                PagerItem(id = "center", imageUri = null, alignment = Alignment.Center),
                PagerItem(id = "right1", imageUri = null, alignment = Alignment.Center),
                PagerItem(id = "right2", imageUri = null, alignment = Alignment.Center),
            )
        }

        val currentIndex = viewModelState.currentIndex
        val shuffledIndices = viewModelState.imagesShuffledIndex

        val indices = (-2..2).map { offset ->
            wrapIndex(currentIndex + offset, shuffledIndices.size)
        }

        val pagerUris = indices.map { viewModelState.getPagerImage(it) }

        requestFaceDetection(pagerUris)

        return pagerUris.map { uri ->
            PagerItem(
                id = uri.toString(),
                imageUri = uri,
                alignment = viewModelState.faceAlignmentCache[uri] ?: Alignment.Center,
            )
        }
    }

    private val requestFaceDetectionMutex = Mutex()
    private fun requestFaceDetection(uris: List<Uri>) {
        viewModelScope.launch {
            requestFaceDetectionMutex.withLock {
                uris.forEach { uri ->
                    if (viewModelStateFlow.value.faceAlignmentCache.containsKey(uri)) return@forEach
                    val alignment = faceDetectionManager.detectFaceAlignment(uri)
                    viewModelStateFlow.update {
                        it.copy(
                            faceAlignmentCache = it.faceAlignmentCache.plus(uri to alignment),
                        )
                    }
                }
            }
        }
    }

    private fun wrapIndex(index: Int, size: Int): Int {
        return ((index % size) + size) % size
    }

    private fun moveByOffset(offset: Int) {
        viewModelStateFlow.update { viewModelState ->
            val newIndex = wrapIndex(
                viewModelState.currentIndex + offset,
                viewModelState.imagesShuffledIndex.size,
            )
            viewModelState.copy(currentIndex = newIndex)
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

        val directoryUri =
            settingsRepositor.settingsFlow.first().imageDirectoryUri.takeIf { it.isNotEmpty() }
                ?.toUri() ?: return updateLoadingFalse()

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
            updateInMemoryCache()

            val secondList = uris.drop(firstSize).toList()
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    images = firstList + secondList,
                    imagesShuffledIndex = firstImagesShuffledIndex + secondList.indices.shuffled()
                        .map { firstImagesShuffledIndex.size + it },
                    imagesLastUpdate = Instant.now(clock),
                    isLoading = false,
                )
            }
            updateInMemoryCache()
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

    private fun updateInMemoryCache() {
        val state = viewModelStateFlow.value
        inMemoryCache.imageInfo = InMemoryCache.ImageInfo(
            imageUris = state.images,
            imagesShuffledIndex = state.imagesShuffledIndex,
            currentIndex = state.currentIndex,
        )
    }

    data class ViewModelState(
        val images: List<Uri> = listOf(),
        val imagesShuffledIndex: List<Int> = listOf(),
        val currentIndex: Int = 0,
        val imagesLastUpdate: Instant = Instant.MIN,
        val isLoading: Boolean = true,
        val imageSwitchIntervalSeconds: Int? = null,
        val faceAlignmentCache: Map<Uri, Alignment> = mapOf(),
    ) {
        fun getPagerImage(index: Int): Uri {
            return images[imagesShuffledIndex[index]]
        }
    }

    companion object {
        val defaultSlideshowDuration = 30.seconds
    }
}
