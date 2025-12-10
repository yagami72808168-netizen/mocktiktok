package com.light.mimictiktok.ui.home

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.light.mimictiktok.R
import com.light.mimictiktok.data.db.VideoEntity
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val playerView: PlayerView = itemView.findViewById(R.id.playerView)
    private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
    private val ivPlayPause: ImageView = itemView.findViewById(R.id.ivPlayPause)
    
    private var currentPlayer: ExoPlayer? = null
    private var playbackListener: Player.Listener? = null

    init {
        itemView.setOnClickListener {
            togglePlayPause()
        }
    }

    fun bind(video: VideoEntity, player: ExoPlayer) {
        currentPlayer = player
        playerView.player = player
        
        tvTitle.text = video.title ?: "Unknown"
        tvDuration.text = formatDuration(video.duration)
        
        removePlaybackListener()
        
        playbackListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon(isPlaying)
            }
        }
        player.addListener(playbackListener!!)
        
        updatePlayPauseIcon(player.isPlaying)
    }

    fun unbind() {
        removePlaybackListener()
        playerView.player = null
        currentPlayer = null
    }

    private fun removePlaybackListener() {
        playbackListener?.let { listener ->
            currentPlayer?.removeListener(listener)
        }
        playbackListener = null
    }

    private fun togglePlayPause() {
        currentPlayer?.let { player ->
            player.playWhenReady = !player.playWhenReady
            updatePlayPauseIcon(player.playWhenReady)
            showPlayPauseIcon()
        }
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        ivPlayPause.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }

    private fun showPlayPauseIcon() {
        ivPlayPause.visibility = View.VISIBLE
        ivPlayPause.animate()
            .alpha(1f)
            .setDuration(200)
            .withEndAction {
                ivPlayPause.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setStartDelay(500)
                    .withEndAction {
                        ivPlayPause.visibility = View.GONE
                    }
                    .start()
            }
            .start()
    }

    private fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
