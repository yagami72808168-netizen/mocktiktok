package com.light.mimictiktok.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.light.mimictiktok.data.db.AppDao
import com.light.mimictiktok.data.db.AppDatabase
import com.light.mimictiktok.data.db.PlaylistEntity
import com.light.mimictiktok.data.db.PlaylistVideoCrossRef
import com.light.mimictiktok.data.db.VideoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class VideoRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AppDao
    private lateinit var repository: VideoRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.appDao()
        repository = VideoRepository(dao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetVideo() = runTest {
        val video = VideoEntity(
            id = "video1",
            path = "/path/to/video1.mp4",
            title = "Test Video 1",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb1.jpg"
        )

        repository.insertVideo(video)
        val retrievedVideo = repository.getVideoById("video1")
        
        assertNotNull(retrievedVideo)
        assertEquals("video1", retrievedVideo?.id)
        assertEquals("Test Video 1", retrievedVideo?.title)
        assertEquals("/path/to/video1.mp4", retrievedVideo?.path)
    }

    @Test
    fun getAllVideosFlow() = runTest {
        val video1 = VideoEntity(
            id = "video1",
            path = "/path/to/video1.mp4",
            title = "Test Video 1",
            duration = 30000L,
            dateTaken = 1000L,
            size = 1024000L,
            coverPath = "/path/to/thumb1.jpg"
        )
        
        val video2 = VideoEntity(
            id = "video2",
            path = "/path/to/video2.mp4",
            title = "Test Video 2",
            duration = 45000L,
            dateTaken = 2000L,
            size = 2048000L,
            coverPath = "/path/to/thumb2.jpg"
        )

        repository.insertVideos(video1, video2)
        val videos = repository.getAllVideosFlow().first()
        
        assertEquals(2, videos.size)
        assertEquals("Test Video 2", videos[0].title) // Should be ordered by dateTaken DESC
        assertEquals("Test Video 1", videos[1].title)
    }

    @Test
    fun updateVideo() = runTest {
        val video = VideoEntity(
            id = "video1",
            path = "/path/to/video1.mp4",
            title = "Original Title",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb1.jpg"
        )

        repository.insertVideo(video)
        
        val updatedVideo = video.copy(title = "Updated Title", isFavorite = true)
        repository.updateVideo(updatedVideo)
        
        val retrievedVideo = repository.getVideoById("video1")
        assertEquals("Updated Title", retrievedVideo?.title)
        assertTrue(retrievedVideo?.isFavorite == true)
    }

    @Test
    fun deleteVideo() = runTest {
        val video = VideoEntity(
            id = "video1",
            path = "/path/to/video1.mp4",
            title = "Test Video",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb1.jpg"
        )

        repository.insertVideo(video)
        var retrievedVideo = repository.getVideoById("video1")
        assertNotNull(retrievedVideo)
        
        repository.deleteVideoById("video1")
        retrievedVideo = repository.getVideoById("video1")
        assertNull(retrievedVideo)
    }

    @Test
    fun createAndGetPlaylist() = runTest {
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "Test Playlist",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis()
        )

        repository.createPlaylist(playlist)
        val retrievedPlaylist = repository.getPlaylistById("playlist1")
        
        assertNotNull(retrievedPlaylist)
        assertEquals("playlist1", retrievedPlaylist?.id)
        assertEquals("Test Playlist", retrievedPlaylist?.name)
    }

    @Test
    fun getAllPlaylistsFlow() = runTest {
        val playlist1 = PlaylistEntity(
            id = "playlist1",
            name = "Playlist 1",
            createTime = 1000L,
            updateTime = 1000L
        )
        
        val playlist2 = PlaylistEntity(
            id = "playlist2",
            name = "Playlist 2",
            createTime = 2000L,
            updateTime = 2000L
        )

        repository.createPlaylist(playlist1)
        repository.createPlaylist(playlist2)
        val playlists = repository.getAllPlaylistsFlow().first()
        
        assertEquals(4, playlists.size) // 2 default playlists + 2 test playlists
        assertTrue(playlists.any { it.name == "Playlist 1" })
        assertTrue(playlists.any { it.name == "Playlist 2" })
    }

    @Test
    fun updatePlaylist() = runTest {
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "Original Name",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis()
        )

        repository.createPlaylist(playlist)
        
        val updatedPlaylist = playlist.copy(name = "Updated Name", updateTime = System.currentTimeMillis() + 1000)
        repository.updatePlaylist(updatedPlaylist)
        
        val retrievedPlaylist = repository.getPlaylistById("playlist1")
        assertEquals("Updated Name", retrievedPlaylist?.name)
    }

    @Test
    fun deletePlaylist() = runTest {
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "Test Playlist",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis()
        )

        repository.createPlaylist(playlist)
        var retrievedPlaylist = repository.getPlaylistById("playlist1")
        assertNotNull(retrievedPlaylist)
        
        repository.deletePlaylistById("playlist1")
        retrievedPlaylist = repository.getPlaylistById("playlist1")
        assertNull(retrievedPlaylist)
    }

    @Test
    fun addVideoToPlaylist() = runTest {
        // Insert test data
        val video = VideoEntity(
            id = "video1",
            path = "/path/to/video1.mp4",
            title = "Test Video",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb1.jpg"
        )
        
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "Test Playlist",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis()
        )

        repository.insertVideo(video)
        repository.createPlaylist(playlist)
        
        // Add video to playlist
        repository.addVideoToPlaylist("playlist1", "video1", 0)
        
        // Verify association
        val playlistWithVideos = repository.getPlaylistWithVideos("playlist1")
        assertNotNull(playlistWithVideos)
        assertEquals("Test Playlist", playlistWithVideos?.playlist?.name)
        assertEquals(1, playlistWithVideos?.videos?.size)
        assertEquals("video1", playlistWithVideos?.videos?.get(0)?.id)
    }

    @Test
    fun addVideosToPlaylist() = runTest {
        // Insert test data
        val video1 = VideoEntity(
            id = "video1",
            path = "/path/to/video1.mp4",
            title = "Video 1",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb1.jpg"
        )
        
        val video2 = VideoEntity(
            id = "video2",
            path = "/path/to/video2.mp4",
            title = "Video 2",
            duration = 45000L,
            dateTaken = System.currentTimeMillis(),
            size = 2048000L,
            coverPath = "/path/to/thumb2.jpg"
        )
        
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "Test Playlist",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis()
        )

        repository.insertVideos(video1, video2)
        repository.createPlaylist(playlist)
        
        // Add videos to playlist
        repository.addVideosToPlaylist("playlist1", listOf("video1", "video2"))
        
        // Verify association
        val playlistWithVideos = repository.getPlaylistWithVideos("playlist1")
        assertNotNull(playlistWithVideos)
        assertEquals("Test Playlist", playlistWithVideos?.playlist?.name)
        assertEquals(2, playlistWithVideos?.videos?.size)
    }

    @Test
    fun removeVideoFromPlaylist() = runTest {
        // Insert test data
        val video = VideoEntity(
            id = "video1",
            path = "/path/to/video1.mp4",
            title = "Test Video",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb1.jpg"
        )
        
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "Test Playlist",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis()
        )

        repository.insertVideo(video)
        repository.createPlaylist(playlist)
        
        // Add video to playlist
        repository.addVideoToPlaylist("playlist1", "video1", 0)
        
        // Verify association exists
        var playlistWithVideos = repository.getPlaylistWithVideos("playlist1")
        assertEquals(1, playlistWithVideos?.videos?.size)
        
        // Remove video from playlist
        repository.removeVideoFromPlaylist("playlist1", "video1")
        
        // Verify association removed
        playlistWithVideos = repository.getPlaylistWithVideos("playlist1")
        assertEquals(0, playlistWithVideos?.videos?.size)
    }

    @Test
    fun getAllPlaylistsWithVideosFlow() = runTest {
        // Insert test data
        val video1 = VideoEntity(
            id = "video1",
            path = "/path/to/video1.mp4",
            title = "Video 1",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb1.jpg"
        )
        
        val video2 = VideoEntity(
            id = "video2",
            path = "/path/to/video2.mp4",
            title = "Video 2",
            duration = 45000L,
            dateTaken = System.currentTimeMillis(),
            size = 2048000L,
            coverPath = "/path/to/thumb2.jpg"
        )
        
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "Test Playlist",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis()
        )

        repository.insertVideos(video1, video2)
        repository.createPlaylist(playlist)
        
        // Associate videos with playlist
        repository.addVideosToPlaylist("playlist1", listOf("video1", "video2"))
        
        // Verify all playlists with videos
        val playlists = repository.getAllPlaylistsWithVideosFlow().first()
        assertEquals(3, playlists.size) // 2 default playlists + 1 test playlist
        
        val testPlaylist = playlists.find { it.playlist.id == "playlist1" }
        assertNotNull(testPlaylist)
        assertEquals("Test Playlist", testPlaylist?.playlist?.name)
        assertEquals(2, testPlaylist?.videos?.size)
    }
}