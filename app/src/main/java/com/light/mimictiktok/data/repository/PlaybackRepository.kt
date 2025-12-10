package com.light.mimictiktok.data.repository

import com.light.mimictiktok.data.db.AppDao
import com.light.mimictiktok.data.db.PlaybackProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackRepository(
    private val appDao: AppDao
) {
    
    suspend fun savePlaybackProgress(
        videoId: String,
        progressMs: Long,
        durationMs: Long,
        playSpeed: Float = 1.0f
    ) = withContext(Dispatchers.IO) {
        val existingProgress = appDao.getPlaybackProgress(videoId)
        val progress = if (existingProgress != null) {
            existingProgress.copy(
                progressMs = progressMs,
                durationMs = durationMs,
                playSpeed = playSpeed,
                lastWatchedTime = System.currentTimeMillis()
            )
        } else {
            PlaybackProgressEntity(
                videoId = videoId,
                progressMs = progressMs,
                durationMs = durationMs,
                playSpeed = playSpeed,
                lastWatchedTime = System.currentTimeMillis()
            )
        }
        appDao.insertPlaybackProgress(progress)
    }
    
    suspend fun getPlaybackProgress(videoId: String): PlaybackProgressEntity? = withContext(Dispatchers.IO) {
        appDao.getPlaybackProgress(videoId)
    }
    
    suspend fun getRecentWatchedVideos(limit: Int = 50): List<PlaybackProgressEntity> = withContext(Dispatchers.IO) {
        appDao.getRecentWatchedVideos(limit)
    }
    
    suspend fun deletePlaybackProgress(videoId: String) = withContext(Dispatchers.IO) {
        appDao.deletePlaybackProgress(videoId)
    }
    
    suspend fun cleanOldProgress(thresholdDays: Int = 30) = withContext(Dispatchers.IO) {
        val thresholdTime = System.currentTimeMillis() - (thresholdDays * 24 * 60 * 60 * 1000)
        val oldVideos = appDao.getRecentWatchedVideos(Int.MAX_VALUE)
            .filter { it.lastWatchedTime < thresholdTime }
        
        oldVideos.forEach { progress ->
            appDao.deletePlaybackProgress(progress.videoId)
        }
    }
}