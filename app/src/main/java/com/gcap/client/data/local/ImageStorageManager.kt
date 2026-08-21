package com.gcap.client.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import com.gcap.client.data.model.MessageImage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val attachmentsDir: File by lazy {
        File(context.filesDir, "attachments").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Saves base64 image or file data to internal storage and returns the local file URI string.
     */
    fun saveBase64Image(base64Data: String, mimeType: String = "image/png", suggestedName: String? = null): String? {
        return try {
            val cleanBase64 = base64Data.substringAfter("base64,").trim()
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                ?: if (mimeType.contains("jpeg") || mimeType.contains("jpg")) "jpg" else "png"
            val filename = suggestedName ?: "file_${UUID.randomUUID()}.$extension"
            val file = File(attachmentsDir, filename)
            FileOutputStream(file).use { it.write(bytes) }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies any file from an external content/file Uri into internal storage.
     */
    fun saveFileFromUri(uri: Uri): MessageImage? {
        return try {
            val (name, size) = queryFileInfo(uri)
            val detectedMime = context.contentResolver.getType(uri)
                ?: getMimeTypeFromFilename(name)
                ?: "application/octet-stream"

            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(detectedMime)
                ?: name.substringAfterLast('.', "")
            val safeFilename = "${UUID.randomUUID()}_$name"
            val destinationFile = File(attachmentsDir, safeFilename)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            MessageImage(
                uri = Uri.fromFile(destinationFile).toString(),
                mimeType = detectedMime,
                name = name,
                size = if (size > 0) size else destinationFile.length()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads a local file URI and returns its Base64 string for API payloads.
     */
    fun getBase64FromUri(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = if (uri.scheme == "file") {
                File(uri.path ?: "").inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }
            inputStream?.use { input ->
                val bytes = input.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun queryFileInfo(uri: Uri): Pair<String, Long> {
        var name = "attachment_${System.currentTimeMillis()}"
        var size: Long = 0
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            uri.lastPathSegment?.let { name = it }
        }
        return Pair(name, size)
    }

    private fun getMimeTypeFromFilename(filename: String): String? {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } else null
    }
}
