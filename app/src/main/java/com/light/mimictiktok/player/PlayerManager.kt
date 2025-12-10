package com.light.mimictiktok.player

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.light.mimictiktok.data.db.VideoEntity

class PlayerManager(private val context: Context) {
    private val playerPool = PlayerPool(context, poolSize = 3)
    private var currentPlayingPosition: Int = -1
    private var currentPlayer: ExoPlayer? = null
    private val playbackStateListeners = mutableMapOf<Int, Player.Listener>()

    fun preparePlayer(position: Int, video: VideoEntity): ExoPlayer {
        val player = playerPool.acquire(position)
        
        player.stop()
        player.clearMediaItems()
        
        if (video.path.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(video.path)
            player.setMediaItem(mediaItem)
            player.prepare()
        }
        
        return player
    }

    fun playAtPosition(position: Int, video: VideoEntity): ExoPlayer {
        pauseCurrent()
        
        val player = preparePlayer(position, video)
        player.playWhenReady = true
        
        currentPlayingPosition = position
        currentPlayer = player
        
        return player
    }

    fun pauseCurrent() {
        currentPlayer?.let { player ->
            player.playWhenReady = false
        }
    }

    fun resumeCurrent() {
        currentPlayer?.let { player ->
            player.playWhenReady = true
        }
    }

    fun releasePlayer(position: Int) {
        if (position == currentPlayingPosition) {
            currentPlayer = null
            currentPlayingPosition = -1
        }
        playbackStateListeners.remove(position)
        playerPool.release(position)
    }

    fun getCurrentPlayingPosition(): Int = currentPlayingPosition

    fun isPlaying(): Boolean = currentPlayer?.isPlaying ?: false

    fun togglePlayPause() {
        currentPlayer?.let { player ->
            player.playWhenReady = !player.playWhenReady
        }
    }

    fun releaseAll() {
        currentPlayer = null
        currentPlayingPosition = -1
        playbackStateListeners.clear()
        playerPool.releaseAll()
    }

    fun getPlayerPool(): PlayerPool = playerPool
}
