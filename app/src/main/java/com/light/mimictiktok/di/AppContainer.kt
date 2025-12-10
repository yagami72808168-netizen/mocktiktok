package com.light.mimictiktok.di

import android.content.Context
import com.light.mimictiktok.data.db.AppDatabase
import com.light.mimictiktok.data.repository.MediaRepository
import com.light.mimictiktok.data.repository.VideoRepository
import com.light.mimictiktok.player.PlayerPool
import com.light.mimictiktok.player.PlayerManager
import com.light.mimictiktok.util.PlaylistImporter
import com.light.mimictiktok.util.ThumbnailGenerator
import com.light.mimictiktok.util.ThumbnailCache

object AppContainer {
    private var instance: AppDependencies? = null

    fun initialize(context: Context) {
        instance = AppDependencies(context)
    }

    fun get(): AppDependencies {
        return instance ?: throw IllegalStateException("AppContainer not initialized")
    }

    fun destroy() {
        instance?.cleanup()
        instance = null
    }
}

class AppDependencies(private val context: Context) {
    val appDatabase: AppDatabase = AppDatabase.getInstance(context)
    val videoRepository: VideoRepository = VideoRepository(appDatabase.appDao())
    val mediaRepository: MediaRepository = MediaRepository(context, videoRepository)
    val playlistImporter: PlaylistImporter = PlaylistImporter(context, videoRepository)
    val playerPool: PlayerPool = PlayerPool(context, poolSize = 2)
    
    // 移除 @Singleton 注解，使用简单实例化
    val thumbnailGenerator: ThumbnailGenerator = ThumbnailGenerator(context)
    val thumbnailCache: ThumbnailCache = ThumbnailCache(context)
    val playerManager: PlayerManager = PlayerManager(playerPool, videoRepository)

    fun cleanup() {
        playerPool.releaseAll()
    }
}
