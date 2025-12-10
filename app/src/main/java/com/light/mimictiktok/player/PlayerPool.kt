package com.light.mimictiktok.player

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import java.util.concurrent.CopyOnWriteArrayList

@Suppress("DEPRECATION")
class PlayerPool(private val context: Context, poolSize: Int = 2) {
    private val players = CopyOnWriteArrayList<ExoPlayer>()
    private val poolSize = maxOf(poolSize, 1)

    init {
        repeat(this.poolSize) {
            val player = ExoPlayer.Builder(context).build()
            player.repeatMode = Player.REPEAT_MODE_OFF
            players.add(player)
        }
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
