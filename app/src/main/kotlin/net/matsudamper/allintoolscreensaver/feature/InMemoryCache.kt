package net.matsudamper.allintoolscreensaver.feature

import android.net.Uri

class InMemoryCache {
    var imageInfo: ImageInfo? = null

    data class ImageInfo(
        val imageUris: List<Uri>,
        val imagesShuffledIndex: List<Int>,
        val currentIndex: Int,
    )
}
