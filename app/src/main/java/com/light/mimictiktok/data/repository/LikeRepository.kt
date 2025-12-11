package com.light.mimictiktok.data.repository

import com.light.mimictiktok.data.db.LikeDao
import com.light.mimictiktok.data.db.LikeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LikeRepository(private val likeDao: LikeDao) {
    suspend fun toggleLike(videoId: String) = withContext(Dispatchers.IO) {
        if (isVideoLiked(videoId)) {
            unlikeVideo(videoId)
        } else {
            likeVideo(videoId)
        }
    }

    suspend fun likeVideo(videoId: String) = withContext(Dispatchers.IO) {
        likeDao.insertLike(LikeEntity(videoId))
    }

    suspend fun unlikeVideo(videoId: String) = withContext(Dispatchers.IO) {
        likeDao.deleteLikesByVideoId(videoId)
    }

    suspend fun isVideoLiked(videoId: String): Boolean = withContext(Dispatchers.IO) {
        likeDao.isVideoLiked(videoId)
    }

    fun isVideoLikedFlow(videoId: String): Flow<Boolean> = likeDao.isVideoLikedFlow(videoId)

    suspend fun getLikeCount(videoId: String): Int = withContext(Dispatchers.IO) {
        likeDao.getLikeCount(videoId)
    }

    fun getLikeCountFlow(videoId: String): Flow<Int> = likeDao.getLikeCountFlow(videoId)

    suspend fun getLikesByVideoId(videoId: String): List<LikeEntity> = withContext(Dispatchers.IO) {
        likeDao.getLikesByVideoId(videoId)
    }

    fun getAllLikesFlow(): Flow<List<LikeEntity>> = likeDao.getAllLikesFlow()
}
