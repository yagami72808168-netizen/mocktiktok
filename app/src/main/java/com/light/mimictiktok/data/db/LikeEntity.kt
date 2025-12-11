package com.light.mimictiktok.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "likes",
    primaryKeys = ["videoId", "likeTime"],
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["videoId"]),
        Index(value = ["likeTime"])
    ]
)
data class LikeEntity(
    val videoId: String,
    val likeTime: Long = System.currentTimeMillis()
)
