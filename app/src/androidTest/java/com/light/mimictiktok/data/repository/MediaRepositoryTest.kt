package com.light.mimictiktok.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.light.mimictiktok.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var videoRepository: VideoRepository
    private lateinit var mediaRepository: MediaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        videoRepository = VideoRepository(database.appDao())
        mediaRepository = MediaRepository(context, videoRepository)
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun scanAllVideos_returnsFlow() = runBlocking {
        val videos = mediaRepository.scanAllVideos().first()

        assertNotNull(videos)
        assertTrue(videos is List)
    }

    @Test
    fun getVideoMetadataFromUri_withInvalidUri_returnsNull() = runBlocking {
        val invalidUri = Uri.parse("invalid://test")
        val result = mediaRepository.getVideoMetadataFromUri(invalidUri)

        assertNull(result)
    }

    @Test
    fun isVideoImported_withNewUri_returnsFalse() = runBlocking {
        val testUri = Uri.parse("content://test/video1")
        val result = mediaRepository.isVideoImported(testUri)

        assertFalse(result)
    }

    @Test
    fun importMediaStoreVideos_withEmptyList_returnsZero() = runBlocking {
        val result = mediaRepository.importMediaStoreVideos(emptyList())

        assertEquals(0, result)
    }
}
