package com.light.mimictiktok.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val path: String,
    val title: String?,
    val duration: Long,
    val dateTaken: Long,
    val size: Long,
    val coverPath: String?,
    val isFavorite: Boolean = false,
    val likeCount: Long = 0L
)
