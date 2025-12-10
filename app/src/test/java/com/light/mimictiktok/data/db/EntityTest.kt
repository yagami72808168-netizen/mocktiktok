package com.light.mimictiktok.data.db

import org.junit.Test
import org.junit.Assert.*

class EntityTest {

    @Test
    fun videoEntityCreation() {
        val video = VideoEntity(
            id = "video1",
            path = "/path/to/video.mp4",
            title = "Test Video",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb.jpg",
            isFavorite = true,
            likeCount = 5L
        )

        assertEquals("video1", video.id)
        assertEquals("/path/to/video.mp4", video.path)
        assertEquals("Test Video", video.title)
        assertEquals(30000L, video.duration)
        assertEquals(1024000L, video.size)
        assertEquals("/path/to/thumb.jpg", video.coverPath)
        assertTrue(video.isFavorite)
        assertEquals(5L, video.likeCount)
    }

    @Test
    fun videoEntityDefaults() {
        val video = VideoEntity(
            id = "video1",
            path = "/path/to/video.mp4",
            title = null,
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = null
        )

        assertEquals("video1", video.id)
        assertNull(video.title)
        assertNull(video.coverPath)
        assertFalse(video.isFavorite)
        assertEquals(0L, video.likeCount)
    }

    @Test
    fun playlistEntityCreation() {
        val currentTime = System.currentTimeMillis()
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "My Playlist",
            createTime = currentTime,
            updateTime = currentTime
        )

        assertEquals("playlist1", playlist.id)
        assertEquals("My Playlist", playlist.name)
        assertEquals(currentTime, playlist.createTime)
        assertEquals(currentTime, playlist.updateTime)
    }

    @Test
    fun playlistVideoCrossRefCreation() {
        val crossRef = PlaylistVideoCrossRef(
            playlistId = "playlist1",
            videoId = "video1",
            position = 5
        )

        assertEquals("playlist1", crossRef.playlistId)
        assertEquals("video1", crossRef.videoId)
        assertEquals(5, crossRef.position)
    }

    @Test
    fun playlistVideoCrossRefDefaultPosition() {
        val crossRef = PlaylistVideoCrossRef(
            playlistId = "playlist1",
            videoId = "video1"
        )

        assertEquals("playlist1", crossRef.playlistId)
        assertEquals("video1", crossRef.videoId)
        assertEquals(0, crossRef.position)
    }

    @Test
    fun playlistWithVideosCreation() {
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "My Playlist",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis()
        )

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

        val playlistWithVideos = PlaylistWithVideos(
            playlist = playlist,
            videos = listOf(video1, video2)
        )

        assertEquals("playlist1", playlistWithVideos.playlist.id)
        assertEquals("My Playlist", playlistWithVideos.playlist.name)
        assertEquals(2, playlistWithVideos.videos.size)
        assertEquals("video1", playlistWithVideos.videos[0].id)
        assertEquals("video2", playlistWithVideos.videos[1].id)
    }

    @Test
    fun playlistWithVideosEmptyVideos() {
        val playlist = PlaylistEntity(
            id = "playlist1",
            name = "Empty Playlist",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis()
        )

        val playlistWithVideos = PlaylistWithVideos(
            playlist = playlist,
            videos = emptyList()
        )

        assertEquals("playlist1", playlistWithVideos.playlist.id)
        assertEquals("Empty Playlist", playlistWithVideos.playlist.name)
        assertEquals(0, playlistWithVideos.videos.size)
        assertTrue(playlistWithVideos.videos.isEmpty())
    }

    @Test
    fun videoEntityEquality() {
        val video1 = VideoEntity(
            id = "video1",
            path = "/path/to/video.mp4",
            title = "Test Video",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb.jpg"
        )

        val video2 = VideoEntity(
            id = "video1",
            path = "/path/to/video.mp4",
            title = "Test Video",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb.jpg"
        )

        assertEquals(video1, video2)
        assertEquals(video1.hashCode(), video2.hashCode())
    }

    @Test
    fun videoEntityInequality() {
        val video1 = VideoEntity(
            id = "video1",
            path = "/path/to/video1.mp4",
            title = "Test Video 1",
            duration = 30000L,
            dateTaken = System.currentTimeMillis(),
            size = 1024000L,
            coverPath = "/path/to/thumb1.jpg"
        )

        val video2 = VideoEntity(
            id = "video2",
            path = "/path/to/video2.mp4",
            title = "Test Video 2",
            duration = 45000L,
            dateTaken = System.currentTimeMillis(),
            size = 2048000L,
            coverPath = "/path/to/thumb2.jpg"
        )

        assertNotEquals(video1, video2)
    }

    @Test
    fun playlistEntityEquality() {
        val currentTime = System.currentTimeMillis()
        val playlist1 = PlaylistEntity(
            id = "playlist1",
            name = "My Playlist",
            createTime = currentTime,
            updateTime = currentTime
        )

        val playlist2 = PlaylistEntity(
            id = "playlist1",
            name = "My Playlist",
            createTime = currentTime,
            updateTime = currentTime
        )

        assertEquals(playlist1, playlist2)
        assertEquals(playlist1.hashCode(), playlist2.hashCode())
    }
}