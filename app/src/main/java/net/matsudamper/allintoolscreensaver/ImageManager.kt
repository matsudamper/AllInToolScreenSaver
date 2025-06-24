package net.matsudamper.allintoolscreensaver

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ImageManager(private val context: Context) {

    fun getImageUrisFromDirectory(directoryUri: Uri): Flow<Uri> {
        val directory = DocumentFile.fromTreeUri(context, directoryUri)
        return flow {
            if (directory?.exists() == true && directory.isDirectory) {
                collectImages(folder = directory)
            }
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun FlowCollector<Uri>.collectImages(
        folder: DocumentFile,
    ) {
        folder.listFiles().forEach { file ->
            if (file.isDirectory) {
                collectImages(file)
            } else if (file.isFile && isImageFile(file)) {
                emit(file.uri)
            }
        }
    }

    private fun isImageFile(file: DocumentFile): Boolean {
        val mimeType = file.type
        return mimeType?.startsWith("image/") == true
    }
}
