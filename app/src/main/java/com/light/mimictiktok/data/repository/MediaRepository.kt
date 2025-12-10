package com.light.mimictiktok.data.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.light.mimictiktok.data.db.VideoEntity
import com.light.mimictiktok.util.MediaScanUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository for media-related operations including scanning and importing local video files.
 */
class MediaRepository(
    private val context: Context,
    private val videoRepository: VideoRepository
) {
    /**
     * Represents a video file from MediaStore.
     */
    data class MediaStoreVideo(
        val id: Long,
        val uri: Uri,
        val title: String,
        val duration: Long,
        val dateTaken: Long,
        val size: Long,
        val path: String
    )

    /**
     * Scans all video files from MediaStore.
     *
     * @return Flow of lists of MediaStoreVideo as they are discovered
     */
    fun scanAllVideos(): Flow<List<MediaStoreVideo>> = flow {
        val videos = mutableListOf<MediaStoreVideo>()
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: "Unknown"
                val duration = it.getLong(durationColumn)
                val dateTaken = it.getLong(dateTakenColumn)
                val size = it.getLong(sizeColumn)
                val path = it.getString(dataColumn) ?: ""

                val uri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                videos.add(
                    MediaStoreVideo(
                        id = id,
                        uri = uri,
                        title = title,
                        duration = duration,
                        dateTaken = dateTaken,
                        size = size,
                        path = path
                    )
                )
            }
        }

        emit(videos)
    }.flowOn(Dispatchers.IO)

    /**
     * Imports videos from MediaStore into the database.
     *
     * @param videos List of MediaStoreVideo to import
     * @param onProgress Optional progress callback (current, total)
     * @return Number of successfully imported videos
     */
    suspend fun importMediaStoreVideos(
        videos: List<MediaStoreVideo>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        var importedCount = 0

        videos.forEachIndexed { index, video ->
            try {
                val videoEntity = VideoEntity(
                    id = "mediastore_${video.id}",
                    path = video.uri.toString(),
                    title = video.title,
                    duration = video.duration,
                    dateTaken = video.dateTaken,
                    size = video.size,
                    coverPath = null,
                    isFavorite = false,
                    likeCount = 0L
                )

                videoRepository.insertVideo(videoEntity)
                importedCount++

                onProgress?.invoke(index + 1, videos.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        importedCount
    }

    /**
     * Scans and imports all videos from MediaStore.
     *
     * @param onProgress Optional progress callback (current, total)
     * @return Flow emitting the number of imported videos
     */
    fun scanAndImportAllVideos(
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Flow<Int> = flow {
        scanAllVideos().collect { videos ->
            val imported = importMediaStoreVideos(videos, onProgress)
            emit(imported)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Gets video metadata for a specific URI.
     *
     * @param uri Video URI
     * @return VideoEntity if metadata extraction succeeds, null otherwise
     */
    suspend fun getVideoMetadataFromUri(uri: Uri): VideoEntity? = withContext(Dispatchers.IO) {
        try {
            val metadata = MediaScanUtil.getVideoMetadata(context, uri) ?: return@withContext null

            VideoEntity(
                id = "uri_${uri.toString().hashCode().toString().replace("-", "")}",
                path = uri.toString(),
                title = metadata.title ?: "Unknown",
                duration = metadata.duration,
                dateTaken = metadata.dateTaken,
                size = metadata.size,
                coverPath = null,
                isFavorite = false,
                likeCount = 0L
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a video already exists in the database by URI.
     *
     * @param uri Video URI
     * @return true if video exists, false otherwise
     */
    suspend fun isVideoImported(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val videoId = "uri_${uri.toString().hashCode().toString().replace("-", "")}"
        videoRepository.getVideoById(videoId) != null
    }
}
