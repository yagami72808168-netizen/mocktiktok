package com.light.mimictiktok.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LikeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Delete
    suspend fun deleteLike(like: LikeEntity)

    @Query("DELETE FROM likes WHERE videoId = :videoId")
    suspend fun deleteLikesByVideoId(videoId: String)

    @Query("SELECT COUNT(*) FROM likes WHERE videoId = :videoId")
    suspend fun getLikeCount(videoId: String): Int

    @Query("SELECT COUNT(*) FROM likes WHERE videoId = :videoId")
    fun getLikeCountFlow(videoId: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE videoId = :videoId)")
    suspend fun isVideoLiked(videoId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE videoId = :videoId)")
    fun isVideoLikedFlow(videoId: String): Flow<Boolean>

    @Query("SELECT * FROM likes WHERE videoId = :videoId ORDER BY likeTime DESC")
    suspend fun getLikesByVideoId(videoId: String): List<LikeEntity>

    @Query("SELECT * FROM likes ORDER BY likeTime DESC")
    fun getAllLikesFlow(): Flow<List<LikeEntity>>

    @Query("SELECT * FROM likes WHERE videoId = :videoId ORDER BY likeTime DESC LIMIT 1")
    suspend fun getLatestLikeByVideoId(videoId: String): LikeEntity?
}
