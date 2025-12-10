package com.light.mimictiktok.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Video operations
    @Query("SELECT * FROM videos ORDER BY dateTaken DESC")
    fun getAllVideosFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(vararg videos: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)

    // Playlist operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: String)

    // Cross-reference operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistVideoCrossRef(crossRef: PlaylistVideoCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistVideoCrossRefs(vararg crossRefs: PlaylistVideoCrossRef)

    @Query("DELETE FROM playlist_video_cross_ref WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun deletePlaylistVideo(playlistId: String, videoId: String)

    // Query with relations
    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistWithVideos(playlistId: String): PlaylistWithVideos?

    @Transaction
    @Query("SELECT * FROM playlists")
    fun getAllPlaylistsWithVideosFlow(): Flow<List<PlaylistWithVideos>>
    
    // Playback progress operations
    @Query("SELECT * FROM playback_progress WHERE videoId = :videoId")
    suspend fun getPlaybackProgress(videoId: String): PlaybackProgressEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackProgress(progress: PlaybackProgressEntity)
    
    @Query("UPDATE playback_progress SET progressMs = :progressMs, lastWatchedTime = :lastWatchedTime WHERE videoId = :videoId")
    suspend fun updatePlaybackProgress(videoId: String, progressMs: Long, lastWatchedTime: Long)
    
    @Query("SELECT * FROM playback_progress ORDER BY lastWatchedTime DESC LIMIT :limit")
    suspend fun getRecentWatchedVideos(limit: Int = 50): List<PlaybackProgressEntity>
    
    @Query("DELETE FROM playback_progress WHERE videoId = :videoId")
    suspend fun deletePlaybackProgress(videoId: String)
}
