package com.light.mimictiktok.util

import android.content.Context
import android.net.Uri
import com.light.mimictiktok.data.db.PlaylistEntity
import com.light.mimictiktok.data.db.VideoEntity
import com.light.mimictiktok.data.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Utility class for importing media files into playlists.
 * Handles batch video import with metadata extraction and database persistence.
 */
class PlaylistImporter(
    private val context: Context,
    private val repository: VideoRepository
) {
    /**
     * Result of an import operation.
     */
    sealed class ImportResult {
        data class Success(
            val importedCount: Int,
            val playlistId: String
        ) : ImportResult()

        data class PartialSuccess(
            val importedCount: Int,
            val failedCount: Int,
            val playlistId: String,
            val errors: List<String>
        ) : ImportResult()

        data class Failure(
            val error: String
        ) : ImportResult()
    }

    /**
     * Imports videos from URIs into a new or existing playlist.
     *
     * @param uris List of video URIs to import
     * @param playlistName Name of the playlist (creates new if doesn't exist)
     * @param playlistId Optional existing playlist ID
     * @param onProgress Optional callback for progress updates (current, total)
     * @return ImportResult with operation details
     */
    suspend fun importVideos(
        uris: List<Uri>,
        playlistName: String = "All Videos",
        playlistId: String? = null,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext ImportResult.Failure("No videos selected")
        }

        try {
            // Get or create playlist
            val targetPlaylistId = playlistId ?: createPlaylist(playlistName)

            val videoEntities = mutableListOf<VideoEntity>()
            val errors = mutableListOf<String>()
            var processedCount = 0

            // Process each URI
            uris.forEachIndexed { index, uri ->
                try {
                    // Validate URI
                    if (!MediaPicker.isValidVideoUri(context, uri)) {
                        errors.add("Invalid video URI: $uri")
                        return@forEachIndexed
                    }

                    // Extract metadata
                    val metadata = MediaScanUtil.getVideoMetadata(context, uri)
                    if (metadata == null) {
                        errors.add("Failed to extract metadata for: $uri")
                        return@forEachIndexed
                    }

                    // Create video entity
                    val videoEntity = VideoEntity(
                        id = generateVideoId(uri),
                        path = uri.toString(),
                        title = metadata.title ?: "Unknown",
                        duration = metadata.duration,
                        dateTaken = metadata.dateTaken,
                        size = metadata.size,
                        coverPath = null,
                        isFavorite = false,
                        likeCount = 0L
                    )

                    videoEntities.add(videoEntity)
                    processedCount++

                    // Report progress
                    onProgress?.invoke(index + 1, uris.size)
                } catch (e: Exception) {
                    errors.add("Error processing ${uri.lastPathSegment}: ${e.message}")
                }
            }

            // Save to database
            if (videoEntities.isNotEmpty()) {
                repository.insertVideos(*videoEntities.toTypedArray())
                
                // Add videos to playlist
                val videoIds = videoEntities.map { it.id }
                repository.addVideosToPlaylist(targetPlaylistId, videoIds)
            }

            // Return result
            when {
                videoEntities.isEmpty() -> ImportResult.Failure(
                    "No videos could be imported. Errors: ${errors.joinToString(", ")}"
                )
                errors.isEmpty() -> ImportResult.Success(
                    importedCount = processedCount,
                    playlistId = targetPlaylistId
                )
                else -> ImportResult.PartialSuccess(
                    importedCount = processedCount,
                    failedCount = errors.size,
                    playlistId = targetPlaylistId,
                    errors = errors
                )
            }
        } catch (e: Exception) {
            ImportResult.Failure("Import failed: ${e.message}")
        }
    }

    /**
     * Creates a new playlist with the given name.
     *
     * @param name Playlist name
     * @return ID of the created playlist
     */
    private suspend fun createPlaylist(name: String): String {
        val playlistId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        
        val playlist = PlaylistEntity(
            id = playlistId,
            name = name,
            createTime = currentTime,
            updateTime = currentTime
        )
        
        repository.createPlaylist(playlist)
        return playlistId
    }

    /**
     * Generates a unique video ID based on the URI.
     *
     * @param uri Video URI
     * @return Unique video ID
     */
    private fun generateVideoId(uri: Uri): String {
        // Use URI string hash for consistent ID generation
        return "video_${uri.toString().hashCode().toString().replace("-", "")}"
    }

    /**
     * Imports a single video into the database.
     *
     * @param uri Video URI
     * @param playlistId Optional playlist ID to add the video to
     * @return VideoEntity if successful, null otherwise
     */
    suspend fun importSingleVideo(
        uri: Uri,
        playlistId: String? = null
    ): VideoEntity? = withContext(Dispatchers.IO) {
        try {
            // Validate URI
            if (!MediaPicker.isValidVideoUri(context, uri)) {
                return@withContext null
            }

            // Extract metadata
            val metadata = MediaScanUtil.getVideoMetadata(context, uri) ?: return@withContext null

            // Create video entity
            val videoEntity = VideoEntity(
                id = generateVideoId(uri),
                path = uri.toString(),
                title = metadata.title ?: "Unknown",
                duration = metadata.duration,
                dateTaken = metadata.dateTaken,
                size = metadata.size,
                coverPath = null,
                isFavorite = false,
                likeCount = 0L
            )

            // Save to database
            repository.insertVideo(videoEntity)

            // Add to playlist if specified
            playlistId?.let {
                repository.addVideoToPlaylist(it, videoEntity.id)
            }

            videoEntity
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validates if a list of URIs are all valid video files.
     *
     * @param uris List of URIs to validate
     * @return Pair of (valid URIs, invalid URIs)
     */
    suspend fun validateVideos(uris: List<Uri>): Pair<List<Uri>, List<Uri>> =
        withContext(Dispatchers.IO) {
            val valid = mutableListOf<Uri>()
            val invalid = mutableListOf<Uri>()

            uris.forEach { uri ->
                if (MediaPicker.isValidVideoUri(context, uri)) {
                    valid.add(uri)
                } else {
                    invalid.add(uri)
                }
            }

            Pair(valid, invalid)
        }
}
