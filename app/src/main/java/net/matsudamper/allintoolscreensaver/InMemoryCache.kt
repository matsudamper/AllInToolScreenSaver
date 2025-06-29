package net.matsudamper.allintoolscreensaver

import android.net.Uri

class InMemoryCache {
    var imageInfo: ImageInfo? = null
    var alreadyTriggeredEvents = mutableSetOf<Long>()

    data class ImageInfo(
        val imageUris: List<Uri>,
        val imagesShuffledIndex: List<Int>,
        val currentIndex: Int,
    )
}
