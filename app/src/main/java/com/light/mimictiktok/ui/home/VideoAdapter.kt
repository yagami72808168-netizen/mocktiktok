package com.light.mimictiktok.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.light.mimictiktok.data.db.VideoEntity
import com.light.mimictiktok.data.repository.VideoRepository
import com.light.mimictiktok.player.PlayerPool

class VideoAdapter(
    private val context: Context,
    private val playerPool: PlayerPool,
    private val repository: VideoRepository
) : RecyclerView.Adapter<VideoViewHolder>() {
    
    private var data: List<VideoEntity> = emptyList()

    fun updateData(newData: List<VideoEntity>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = if (data.isEmpty()) 0 else Int.MAX_VALUE

    private fun getRealPosition(position: Int): VideoEntity {
        return data[position % data.size]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val itemView = LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return VideoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getRealPosition(position)
        val player = playerPool.acquire(position)
        holder.bind(video, player)
    }

    override fun onViewDetachedFromWindow(holder: VideoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.unbind()
    }
}
