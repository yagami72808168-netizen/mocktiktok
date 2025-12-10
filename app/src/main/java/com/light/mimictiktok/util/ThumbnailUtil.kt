package com.light.mimictiktok.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ThumbnailUtil {
    fun generateThumbnail(context: Context, uri: Uri): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bitmap = retriever.frameAtTime ?: return null
            val cacheDir = context.cacheDir
            val thumbFile = File(cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
            FileOutputStream(thumbFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            thumbFile.absolutePath
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun generateThumbnailFromPath(context: Context, videoPath: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            val bitmap = retriever.frameAtTime ?: return null
            val cacheDir = context.cacheDir
            val thumbFile = File(cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
            FileOutputStream(thumbFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            thumbFile.absolutePath
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun deleteThumbnail(context: Context, thumbPath: String?): Boolean {
        return if (thumbPath != null) {
            try {
                File(thumbPath).delete()
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
}
