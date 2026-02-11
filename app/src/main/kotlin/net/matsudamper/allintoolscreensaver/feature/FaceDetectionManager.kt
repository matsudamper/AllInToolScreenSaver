package net.matsudamper.allintoolscreensaver.feature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectionManager(private val context: Context) {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build(),
    )

    suspend fun detectFaceAlignment(uri: Uri): Alignment {
        return withContext(Dispatchers.IO) {
            val bitmap = loadBitmap(uri) ?: return@withContext Alignment.Center
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            val faces = detectFaces(bitmap)
            bitmap.recycle()

            if (faces.isNullOrEmpty()) {
                return@withContext Alignment.Center
            }

            val faceCenterX = faces.sumOf { it.centerX.toDouble() } / faces.size
            val faceCenterY = faces.sumOf { it.centerY.toDouble() } / faces.size

            val horizontalBias = ((faceCenterX / bitmapWidth) * 2 - 1).toFloat()
            val verticalBias = ((faceCenterY / bitmapHeight) * 2 - 1).toFloat()

            BiasAlignment(
                horizontalBias = horizontalBias.coerceIn(-1f, 1f),
                verticalBias = verticalBias.coerceIn(-1f, 1f),
            )
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSampleSize(4)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.getOrNull()
    }

    private suspend fun detectFaces(bitmap: Bitmap): List<FaceCenter>? {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { continuation ->
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    val centers = faces.map { face ->
                        FaceCenter(
                            centerX = face.boundingBox.exactCenterX(),
                            centerY = face.boundingBox.exactCenterY(),
                        )
                    }
                    continuation.resume(centers)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }

    private data class FaceCenter(
        val centerX: Float,
        val centerY: Float,
    )
}
