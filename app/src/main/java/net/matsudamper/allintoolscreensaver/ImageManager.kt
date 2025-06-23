package net.matsudamper.allintoolscreensaver

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageManager(private val context: Context) {

    suspend fun getImageUrisFromDirectory(directoryUri: Uri): List<Uri> {
        return withContext(Dispatchers.IO) {
            val directory = DocumentFile.fromTreeUri(context, directoryUri)
            if (directory?.exists() != true || !directory.isDirectory) {
                return@withContext listOf()
            }

            mutableListOf<Uri>().collectImages(folder = directory)
        }
    }

    private fun MutableList<Uri>.collectImages(folder: DocumentFile): MutableList<Uri> {
        folder.listFiles().forEach { file ->
            if (file.isDirectory) {
                collectImages(file)
            } else if (file.isFile && isImageFile(file)) {
                add(file.uri)
            }
        }
        return this
    }

    private fun isImageFile(file: DocumentFile): Boolean {
        val mimeType = file.type
        return mimeType?.startsWith("image/") == true
    }
}
