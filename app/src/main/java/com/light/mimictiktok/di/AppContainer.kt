package com.light.mimictiktok.di

import android.content.Context
import com.light.mimictiktok.data.db.AppDatabase
import com.light.mimictiktok.data.preferences.PreferencesManager
import com.light.mimictiktok.data.repository.LikeRepository
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
    val likeRepository: LikeRepository = LikeRepository(appDatabase.likeDao())
    val mediaRepository: MediaRepository = MediaRepository(context, videoRepository)
    val playlistImporter: PlaylistImporter = PlaylistImporter(context, videoRepository)
    val playerPool: PlayerPool = PlayerPool(context, poolSize = 2)
    val preferencesManager: PreferencesManager = PreferencesManager(context)
    val thumbnailGenerator: ThumbnailGenerator = ThumbnailGenerator()
    val thumbnailCache: ThumbnailCache = ThumbnailCache(context)

    fun cleanup() {
        playerPool.releaseAll()
    }
}
