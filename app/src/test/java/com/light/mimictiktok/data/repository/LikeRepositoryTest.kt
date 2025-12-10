package com.light.mimictiktok.data.repository

import com.light.mimictiktok.data.db.LikeDao
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LikeRepositoryTest {
    private lateinit var likeRepository: LikeRepository
    private lateinit var mockLikeDao: LikeDao

    @Before
    fun setup() {
        mockLikeDao = mock(LikeDao::class.java)
        likeRepository = LikeRepository(mockLikeDao)
    }

    @Test
    fun testIsVideoLiked() = runBlocking {
        val videoId = "test-video-1"
        `when`(mockLikeDao.isVideoLiked(videoId)).thenReturn(true)
        
        val result = likeRepository.isVideoLiked(videoId)
        
        assert(result)
    }

    @Test
    fun testGetLikeCount() = runBlocking {
        val videoId = "test-video-1"
        `when`(mockLikeDao.getLikeCount(videoId)).thenReturn(5)
        
        val count = likeRepository.getLikeCount(videoId)
        
        assert(count == 5)
    }

    @Test
    fun testRepositoryCreation() {
        assert(likeRepository != null)
    }
}
