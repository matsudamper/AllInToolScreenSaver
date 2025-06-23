package net.matsudamper.allintoolscreensaver

import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ImageDisplayScreen(
    modifier: Modifier = Modifier,
    imageChangeIntervalSeconds: Int = 30
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val imageManager = remember { ImageManager(context) }
    
    var imageUris by remember { mutableStateOf<List<Uri>>(listOf()) }
    var currentImageIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val directoryUri = settingsManager.getImageDirectoryUri()
        if (directoryUri != null) {
            val uris = imageManager.getImageUrisFromDirectory(directoryUri)
            imageUris = uris
            if (uris.isNotEmpty()) {
                currentImageIndex = Random.nextInt(uris.size)
            }
        }
        isLoading = false
    }

    LaunchedEffect(imageUris) {
        if (imageUris.isNotEmpty()) {
            while (true) {
                delay(imageChangeIntervalSeconds * 1000L)
                currentImageIndex = Random.nextInt(imageUris.size)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
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
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
} 