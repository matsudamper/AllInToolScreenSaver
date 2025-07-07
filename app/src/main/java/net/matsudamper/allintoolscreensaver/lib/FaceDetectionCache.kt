package net.matsudamper.allintoolscreensaver.lib

import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.mlkit.vision.face.Face

class FaceDetectionCache(maxSize: Int = 100) {
    private val cache = LruCache<String, CachedFaceData>(maxSize)
    private val mutex = Mutex()

    suspend fun get(imageUri: Uri): CachedFaceData? = mutex.withLock {
        cache.get(imageUri.toString())
    }

    suspend fun put(imageUri: Uri, faces: List<Face>, imageSize: IntSize) = mutex.withLock {
        val cacheData = CachedFaceData(
            faces = faces.map { face ->
                CachedFace(
                    boundingBox = face.boundingBox,
                    trackingId = face.trackingId,
                )
            },
            imageSize = imageSize,
            timestamp = System.currentTimeMillis(),
        )
        cache.put(imageUri.toString(), cacheData)
    }

    suspend fun clear() = mutex.withLock {
        cache.evictAll()
    }

    suspend fun size(): Int = mutex.withLock {
        cache.size()
    }

    fun isValid(cacheData: CachedFaceData): Boolean {
        val maxAge = 60 * 60 * 1000L // 1時間 - 電力効率のため長めに設定
        return System.currentTimeMillis() - cacheData.timestamp < maxAge
    }
}

data class CachedFaceData(
    val faces: List<CachedFace>,
    val imageSize: IntSize,
    val timestamp: Long,
)

data class CachedFace(
    val boundingBox: android.graphics.Rect,
    val trackingId: Int?,
) 