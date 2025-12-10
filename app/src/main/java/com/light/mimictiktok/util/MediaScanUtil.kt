package com.light.mimictiktok.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object MediaScanUtil {
    data class VideoMetadata(
        val title: String?,
        val duration: Long,
        val dateTaken: Long,
        val size: Long,
        val path: String?
    )

    fun getVideoMetadata(context: Context, uri: Uri): VideoMetadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val dateValue = getFileDetails(context, uri)
            VideoMetadata(
                title = uri.lastPathSegment,
                duration = duration,
                dateTaken = dateValue.second,
                size = dateValue.first,
                path = uri.path
            )
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun getFileDetails(context: Context, uri: Uri): Pair<Long, Long> {
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateIndex = it.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                val date = if (dateIndex >= 0) it.getLong(dateIndex) * 1000 else System.currentTimeMillis()
                return Pair(size, date)
            }
        }
        return Pair(0L, System.currentTimeMillis())
    }
}
