package com.light.mimictiktok.di

import android.content.Context
import com.light.mimictiktok.data.db.AppDatabase
import com.light.mimictiktok.data.repository.VideoRepository
import com.light.mimictiktok.player.PlayerPool

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

class AppDependencies(context: Context) {
    val appDatabase: AppDatabase = AppDatabase.getInstance(context)
    val videoRepository: VideoRepository = VideoRepository(appDatabase.appDao())
    val playerPool: PlayerPool = PlayerPool(context, poolSize = 2)

    fun cleanup() {
        playerPool.releaseAll()
    }
}
