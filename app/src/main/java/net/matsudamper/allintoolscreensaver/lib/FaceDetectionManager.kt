package net.matsudamper.allintoolscreensaver.lib

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.unit.IntSize
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectionManager {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build(),
    )

    suspend fun detectFaces(bitmap: Bitmap): List<Face> = withContext(Dispatchers.Default) {
        suspendCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    continuation.resume(faces)
                }
                .addOnFailureListener { exception ->
                    Log.e("FaceDetection", "Face detection failed", exception)
                    continuation.resume(emptyList())
                }
        }
    }

    fun calculateImageAlignment(
        faces: List<Face>,
        imageSize: IntSize,
        containerSize: IntSize,
    ): ImageAlignment {
        if (faces.isEmpty()) return ImageAlignment.CENTER

        val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: return ImageAlignment.CENTER

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

    fun close() {
        detector.close()
    }
}

enum class ImageAlignment {
    TOP_LEFT,
    TOP,
    TOP_RIGHT,
    LEFT,
    CENTER,
    RIGHT,
    BOTTOM_LEFT,
    BOTTOM,
    BOTTOM_RIGHT,
} 