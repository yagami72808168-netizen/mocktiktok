package com.light.mimictiktok.util

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.light.mimictiktok.data.db.AppDatabase
import com.light.mimictiktok.data.repository.VideoRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistImporterTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: VideoRepository
    private lateinit var importer: PlaylistImporter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = VideoRepository(database.appDao())
        importer = PlaylistImporter(context, repository)
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun importVideos_withEmptyList_returnsFailure() = runBlocking {
        val result = importer.importVideos(emptyList())

        assertTrue(result is PlaylistImporter.ImportResult.Failure)
        val failure = result as PlaylistImporter.ImportResult.Failure
        assertEquals("No videos selected", failure.error)
    }

    @Test
    fun validateVideos_withEmptyList_returnsEmptyPair() = runBlocking {
        val (valid, invalid) = importer.validateVideos(emptyList())

        assertTrue(valid.isEmpty())
        assertTrue(invalid.isEmpty())
    }

    @Test
    fun importSingleVideo_withInvalidUri_returnsNull() = runBlocking {
        val invalidUri = Uri.parse("invalid://test")
        val result = importer.importSingleVideo(invalidUri)

        assertNull(result)
    }
}
