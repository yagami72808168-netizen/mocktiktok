package com.light.mimictiktok.player

import android.content.Context
import com.light.mimictiktok.data.repository.VideoRepository

/**
 * Factory for creating PlayerManager instances with proper dependency injection
 */
object PlayerManagerFactory {
    fun create(
        context: Context,
        videoRepository: VideoRepository
    ): PlayerManager {
        val playerPool = PlayerPool(context, poolSize = 2)
        return PlayerManager(playerPool, videoRepository)
    }
}