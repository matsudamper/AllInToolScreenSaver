package net.matsudamper.allintoolscreensaver.lib

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

class SmartImageAlignmentManager(private val context: Context) {
    private val faceDetectionManager = FaceDetectionManager()
    private val faceDetectionCache = FaceDetectionCache()
    private val imageLoader = ImageLoader(context)

    suspend fun getImageAlignment(
        imageUri: Uri,
        containerSize: IntSize,
    ): Alignment = withContext(Dispatchers.IO) {
        try {
            val cachedData = faceDetectionCache.get(imageUri)
            if (cachedData != null && faceDetectionCache.isValid(cachedData)) {
                return@withContext calculateAlignmentFromCache(cachedData, containerSize)
            }

            val bitmap = loadImageAsBitmap(imageUri) ?: return@withContext Alignment.Center
            val imageSize = IntSize(bitmap.width, bitmap.height)

            // 電力効率のため、解像度を制限
            val scaledBitmap = scaleDownBitmapForProcessing(bitmap)
            val faces = faceDetectionManager.detectFaces(scaledBitmap)

            faceDetectionCache.put(imageUri, faces, imageSize)

            val alignment = faceDetectionManager.calculateImageAlignment(
                faces,
                imageSize,
                containerSize,
            )

            convertToComposeAlignment(alignment)
        } catch (e: Exception) {
            Log.e("SmartImageAlignment", "Error processing image alignment", e)
            Alignment.Center
        }
    }

    private suspend fun loadImageAsBitmap(imageUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(imageUri)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("SmartImageAlignment", "Error loading image", e)
            null
        }
    }

    private fun calculateAlignmentFromCache(
        cachedData: CachedFaceData,
        containerSize: IntSize,
    ): Alignment {
        val alignment = calculateImageAlignmentFromCachedFaces(
            cachedData.faces,
            cachedData.imageSize,
            containerSize,
        )

        return convertToComposeAlignment(alignment)
    }

    private fun calculateImageAlignmentFromCachedFaces(
        cachedFaces: List<CachedFace>,
        imageSize: IntSize,
        containerSize: IntSize,
    ): ImageAlignment {
        if (cachedFaces.isEmpty()) return ImageAlignment.CENTER

        val primaryFace = cachedFaces.maxByOrNull {
            it.boundingBox.width() * it.boundingBox.height()
        } ?: return ImageAlignment.CENTER

        val faceCenterX = primaryFace.boundingBox.centerX()
        val faceCenterY = primaryFace.boundingBox.centerY()
        val imageWidth = imageSize.width
        val imageHeight = imageSize.height
        val thresholdX = imageWidth * 0.3f
        val thresholdY = imageHeight * 0.3f

        return when {
            faceCenterX < thresholdX && faceCenterY < thresholdY -> ImageAlignment.TOP_LEFT
            faceCenterX > imageWidth - thresholdX && faceCenterY < thresholdY -> ImageAlignment.TOP_RIGHT
            faceCenterX < thresholdX && faceCenterY > imageHeight - thresholdY -> ImageAlignment.BOTTOM_LEFT
            faceCenterX > imageWidth - thresholdX && faceCenterY > imageHeight - thresholdY -> ImageAlignment.BOTTOM_RIGHT
            faceCenterX < thresholdX -> ImageAlignment.LEFT
            faceCenterX > imageWidth - thresholdX -> ImageAlignment.RIGHT
            faceCenterY < thresholdY -> ImageAlignment.TOP
            faceCenterY > imageHeight - thresholdY -> ImageAlignment.BOTTOM
            else -> ImageAlignment.CENTER
        }
    }

    private fun convertToComposeAlignment(alignment: ImageAlignment): Alignment {
        return when (alignment) {
            ImageAlignment.TOP_LEFT -> Alignment.TopStart
            ImageAlignment.TOP -> Alignment.TopCenter
            ImageAlignment.TOP_RIGHT -> Alignment.TopEnd
            ImageAlignment.LEFT -> Alignment.CenterStart
            ImageAlignment.CENTER -> Alignment.Center
            ImageAlignment.RIGHT -> Alignment.CenterEnd
            ImageAlignment.BOTTOM_LEFT -> Alignment.BottomStart
            ImageAlignment.BOTTOM -> Alignment.BottomCenter
            ImageAlignment.BOTTOM_RIGHT -> Alignment.BottomEnd
        }
    }

    private fun scaleDownBitmapForProcessing(bitmap: Bitmap): Bitmap {
        val maxSize = 1024
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }

    fun close() {
        faceDetectionManager.close()
    }
} 