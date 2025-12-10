package com.light.mimictiktok.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey
    val videoId: String,
    val progressMs: Long,
    val durationMs: Long,
    val playSpeed: Float = 1.0f,
    val lastWatchedTime: Long = System.currentTimeMillis()
)