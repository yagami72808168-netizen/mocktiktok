package com.light.mimictiktok.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.MediaItem
import com.light.mimictiktok.R
import com.light.mimictiktok.data.db.VideoEntity
import com.light.mimictiktok.player.PlayerManager
import com.light.mimictiktok.util.ThumbnailGenerator
import com.light.mimictiktok.util.ThumbnailCache

class VideoAdapter(
    private val playerManager: PlayerManager,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val thumbnailCache: ThumbnailCache
) : RecyclerView.Adapter<VideoViewHolder>() {
    
    private var data: List<VideoEntity> = emptyList()
    private var currentPlayingPosition: Int = -1

    fun updateData(newData: List<VideoEntity>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = if (data.isEmpty()) 0 else Int.MAX_VALUE

    private fun getRealPosition(position: Int): Int {
        return position % data.size
    }

    private fun getVideo(position: Int): VideoEntity {
        return data[getRealPosition(position)]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        
        return VideoViewHolder(
            itemView = itemView,
            thumbnailGenerator = thumbnailGenerator,
            thumbnailCache = thumbnailCache
        )
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getVideo(position)
        val player = playerManager.preparePlayer(position, video)
        
        if (video.path.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(video.path)
            player.setMediaItem(mediaItem)
            player.prepare()
        }
        
        holder.bind(video, player)
    }

    override fun onViewDetachedFromWindow(holder: VideoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.unbind()
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun playVideoAtPosition(position: Int) {
        if (position == currentPlayingPosition) {
            return
        }
        
        if (data.isEmpty()) return
        
        val video = getVideo(position)
        playerManager.playAtPosition(position, video)
        currentPlayingPosition = position
    }

    fun pauseCurrentVideo() {
        playerManager.pauseCurrent()
    }

    fun resumeCurrentVideo() {
        playerManager.resumeCurrent()
    }
}
