package com.light.mimictiktok.player

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultAllocator
import java.util.concurrent.CopyOnWriteArrayList

@Suppress("DEPRECATION")
class PlayerPool(private val context: Context, poolSize: Int = 2) {
    private val players = CopyOnWriteArrayList<ExoPlayer>()
    private val poolSize = maxOf(poolSize, 1)

    init {
        repeat(this.poolSize) {
            val player = createConfiguredPlayer()
            players.add(player)
        }
    }

    private fun createConfiguredPlayer(): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSizeSd()
                    .setPreferredAudioLanguage("en")
            )
        }

        val loadControl = createLoadControl()

        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = false
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
    }

    private fun createLoadControl(): LoadControl {
        val allocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)
        
        return DefaultLoadControl.Builder()
            .setAllocator(allocator)
            .setBufferDurationsMs(
                15000,  // minBufferMs
                50000,  // maxBufferMs
                2500,   // bufferForPlaybackMs
                5000    // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    fun acquire(index: Int): ExoPlayer {
        return players[index % poolSize]
    }

    fun release(index: Int) {
        val player = acquire(index)
        player.stop()
        player.clearMediaItems()
    }

    fun releaseAll() {
        players.forEach {
            it.stop()
            it.clearMediaItems()
            it.release()
        }
        players.clear()
    }

    fun getPoolSize(): Int = poolSize
}
