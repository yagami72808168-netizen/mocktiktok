package com.light.mimictiktok.ui.home

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.light.mimictiktok.data.db.VideoEntity

@Suppress("DEPRECATION")
class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var currentPlayer: ExoPlayer? = null

    fun bind(video: VideoEntity, player: ExoPlayer) {
        currentPlayer = player
        player.clearMediaItems()
        
        if (video.path.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(video.path)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun unbind() {
        currentPlayer?.pause()
        currentPlayer?.clearMediaItems()
        currentPlayer = null
    }
}
