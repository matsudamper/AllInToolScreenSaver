package net.matsudamper.allintoolscreensaver.compose

import android.net.Uri
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import net.matsudamper.allintoolscreensaver.ImageColorAnalyzer
import net.matsudamper.allintoolscreensaver.ImageManager
import net.matsudamper.allintoolscreensaver.SettingsManager

@Composable
fun ImageDisplayScreen(
    modifier: Modifier = Modifier,
    imageChangeIntervalSeconds: Int = 30,
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val imageManager = remember { ImageManager(context) }

    var imageUris by remember { mutableStateOf<List<Uri>>(listOf()) }
    var currentImageIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var autoChangeEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val directoryUri = settingsManager.getImageDirectoryUri()
        if (directoryUri != null) {
            val uris = imageManager.getImageUrisFromDirectory(directoryUri)
            imageUris = uris
            if (uris.isNotEmpty()) {
                currentImageIndex = (0 until uris.size).random()
            }
        }
        isLoading = false
    }

    LaunchedEffect(imageUris, autoChangeEnabled) {
        if (imageUris.isNotEmpty() && autoChangeEnabled) {
            while (true) {
                delay(imageChangeIntervalSeconds * 1000L)
                currentImageIndex = (0 until imageUris.size).random()
            }
        }
    }

    fun nextImage() {
        if (imageUris.isNotEmpty()) {
            currentImageIndex = (currentImageIndex + 1) % imageUris.size
        }
    }

    fun previousImage() {
        if (imageUris.isNotEmpty()) {
            currentImageIndex = if (currentImageIndex == 0) {
                imageUris.size - 1
            } else {
                currentImageIndex - 1
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        autoChangeEnabled = true
                    },
                ) { _, dragAmount ->
                    autoChangeEnabled = false
                    val dragThreshold = 100f

                    if (abs(dragAmount.x) > dragThreshold) {
                        if (dragAmount.x > 0) {
                            previousImage()
                        } else {
                            nextImage()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                )
            }
            imageUris.isEmpty() -> {
                // 画像がない場合は何も表示しない
            }
            else -> {
                AsyncImage(
                    model = imageUris[currentImageIndex],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
