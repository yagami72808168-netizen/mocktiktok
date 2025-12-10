package com.light.mimictiktok.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class AppDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AppDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.appDao()
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

        dao.insertVideo(video)
        val retrievedVideo = dao.getVideoById("video1")
        
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

        dao.insertVideos(video1, video2)
        val videos = dao.getAllVideosFlow().first()
        
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

        dao.insertVideo(video)
        
        val updatedVideo = video.copy(title = "Updated Title", isFavorite = true)
        dao.updateVideo(updatedVideo)
        
        val retrievedVideo = dao.getVideoById("video1")
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

        dao.insertVideo(video)
        var retrievedVideo = dao.getVideoById("video1")
        assertNotNull(retrievedVideo)
        
        dao.deleteVideoById("video1")
        retrievedVideo = dao.getVideoById("video1")
        assertNull(retrievedVideo)
    }

    @Test
    fun insertAndGetPlaylist() = runTest {
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "Test Playlist",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis()
        )

        dao.insertPlaylist(playlist)
        val retrievedPlaylist = dao.getPlaylistById("playlist1")
        
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

        dao.insertPlaylist(playlist1)
        dao.insertPlaylist(playlist2)
        val playlists = dao.getAllPlaylistsFlow().first()
        
        assertEquals(4, playlists.size) // 2 default playlists + 2 test playlists
        assertTrue(playlists.any { it.name == "Playlist 1" })
        assertTrue(playlists.any { it.name == "Playlist 2" })
    }

    @Test
    fun playlistVideoAssociation() = runTest {
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

        dao.insertVideo(video)
        dao.insertPlaylist(playlist)
        
        // Associate video with playlist
        val crossRef = PlaylistVideoCrossRef("playlist1", "video1", 0)
        dao.insertPlaylistVideoCrossRef(crossRef)
        
        // Verify association
        val playlistWithVideos = dao.getPlaylistWithVideos("playlist1")
        assertNotNull(playlistWithVideos)
        assertEquals("Test Playlist", playlistWithVideos?.playlist?.name)
        assertEquals(1, playlistWithVideos?.videos?.size)
        assertEquals("video1", playlistWithVideos?.videos?.get(0)?.id)
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

        dao.insertVideo(video)
        dao.insertPlaylist(playlist)
        
        // Associate video with playlist
        val crossRef = PlaylistVideoCrossRef("playlist1", "video1", 0)
        dao.insertPlaylistVideoCrossRef(crossRef)
        
        // Verify association exists
        var playlistWithVideos = dao.getPlaylistWithVideos("playlist1")
        assertEquals(1, playlistWithVideos?.videos?.size)
        
        // Remove video from playlist
        dao.deletePlaylistVideo("playlist1", "video1")
        
        // Verify association removed
        playlistWithVideos = dao.getPlaylistWithVideos("playlist1")
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

        dao.insertVideos(video1, video2)
        dao.insertPlaylist(playlist)
        
        // Associate videos with playlist
        val crossRefs = arrayOf(
            PlaylistVideoCrossRef("playlist1", "video1", 0),
            PlaylistVideoCrossRef("playlist1", "video2", 1)
        )
        dao.insertPlaylistVideoCrossRefs(*crossRefs)
        
        // Verify all playlists with videos
        val playlists = dao.getAllPlaylistsWithVideosFlow().first()
        assertEquals(3, playlists.size) // 2 default playlists + 1 test playlist
        
        val testPlaylist = playlists.find { it.playlist.id == "playlist1" }
        assertNotNull(testPlaylist)
        assertEquals("Test Playlist", testPlaylist?.playlist?.name)
        assertEquals(2, testPlaylist?.videos?.size)
    }
}