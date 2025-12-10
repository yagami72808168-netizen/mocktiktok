package com.light.mimictiktok.data.repository

import com.light.mimictiktok.data.db.AppDao
import com.light.mimictiktok.data.db.PlaylistEntity
import com.light.mimictiktok.data.db.PlaylistVideoCrossRef
import com.light.mimictiktok.data.db.PlaylistWithVideos
import com.light.mimictiktok.data.db.VideoEntity
import kotlinx.coroutines.flow.Flow

class VideoRepository(private val appDao: AppDao) {
    // Video operations
    fun getAllVideosFlow(): Flow<List<VideoEntity>> = appDao.getAllVideosFlow()

    suspend fun getVideoById(id: String): VideoEntity? = appDao.getVideoById(id)

    suspend fun insertVideos(vararg videos: VideoEntity) = appDao.insertVideos(*videos)

    suspend fun insertVideo(video: VideoEntity) = appDao.insertVideo(video)

    suspend fun updateVideo(video: VideoEntity) = appDao.updateVideo(video)

    suspend fun deleteVideoById(id: String) = appDao.deleteVideoById(id)

    // Playlist operations
    suspend fun createPlaylist(playlist: PlaylistEntity) = appDao.insertPlaylist(playlist)

    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>> = appDao.getAllPlaylistsFlow()

    suspend fun getPlaylistById(id: String): PlaylistEntity? = appDao.getPlaylistById(id)

    suspend fun updatePlaylist(playlist: PlaylistEntity) = appDao.updatePlaylist(playlist)

    suspend fun deletePlaylistById(id: String) = appDao.deletePlaylistById(id)

    // Playlist-Video association
    suspend fun addVideoToPlaylist(playlistId: String, videoId: String, position: Int = 0) {
        appDao.insertPlaylistVideoCrossRef(
            PlaylistVideoCrossRef(playlistId, videoId, position)
        )
    }

    suspend fun addVideosToPlaylist(
        playlistId: String,
        videoIds: List<String>
    ) {
        val crossRefs = videoIds.mapIndexed { index, videoId ->
            PlaylistVideoCrossRef(playlistId, videoId, index)
        }.toTypedArray()
        appDao.insertPlaylistVideoCrossRefs(*crossRefs)
    }

    suspend fun removeVideoFromPlaylist(playlistId: String, videoId: String) {
        appDao.deletePlaylistVideo(playlistId, videoId)
    }

    // Query with relations
    suspend fun getPlaylistWithVideos(playlistId: String): PlaylistWithVideos? {
        return appDao.getPlaylistWithVideos(playlistId)
    }

    fun getAllPlaylistsWithVideosFlow(): Flow<List<PlaylistWithVideos>> {
        return appDao.getAllPlaylistsWithVideosFlow()
    }
}
