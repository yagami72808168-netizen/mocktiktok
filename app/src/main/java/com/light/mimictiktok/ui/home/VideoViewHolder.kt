package com.light.mimictiktok.ui.home

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.light.mimictiktok.R
import com.light.mimictiktok.data.db.VideoEntity
import com.light.mimictiktok.data.repository.LikeRepository
import com.light.mimictiktok.ui.widgets.LikeButton
import com.light.mimictiktok.util.ThumbnailCache
import com.light.mimictiktok.util.ThumbnailGenerator
import com.light.mimictiktok.util.ThumbnailResult
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class VideoViewHolder(
    itemView: View,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val thumbnailCache: ThumbnailCache,
    private val likeRepository: LikeRepository? = null,
    private val onLikeChanged: ((String) -> Unit)? = null
) : RecyclerView.ViewHolder(itemView) {
    private val playerView: PlayerView = itemView.findViewById(R.id.playerView)
    private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
    private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
    private val ivPlayPause: ImageView = itemView.findViewById(R.id.ivPlayPause)
    private val likeButton: LikeButton = itemView.findViewById(R.id.likeButton)
    
    private var currentPlayer: ExoPlayer? = null
    private var playbackListener: Player.Listener? = null
    private var currentVideo: VideoEntity? = null
    private val viewHolderScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        itemView.setOnClickListener {
            togglePlayPause()
        }
        
        setupLikeButton()
    }

    fun bind(video: VideoEntity, player: ExoPlayer) {
        currentPlayer = player
        currentVideo = video
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
        
        // 加载缩略图
        loadThumbnail(video)
        
        // 加载点赞状态
        loadLikeState(video)
    }

    fun unbind() {
        removePlaybackListener()
        playerView.player = null
        currentPlayer = null
        currentVideo = null
        
        // 取消协程任务
        viewHolderScope.cancel()
        
        // 重置缩略图
        ivThumbnail.setImageDrawable(null)
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

    /**
     * 加载视频缩略图
     */
    private fun loadThumbnail(video: VideoEntity) {
        // 优先显示占位符
        ivThumbnail.alpha = 0f
        
        viewHolderScope.launch {
            try {
                // 生成缓存键
                val cacheKey = generateCacheKey(video)
                
                // 检查缓存
                val cachedPath = thumbnailCache.getThumbnail(cacheKey, video.path)
                
                if (cachedPath != null) {
                    // 缓存命中，直接显示
                    displayThumbnail(cachedPath)
                } else {
                    // 缓存未命中，异步生成
                    generateAndDisplayThumbnail(video, cacheKey)
                }
            } catch (e: Exception) {
                // 显示默认占位符
                showDefaultThumbnail()
            }
        }
    }

    /**
     * 生成并显示缩略图
     */
    private suspend fun generateAndDisplayThumbnail(video: VideoEntity, cacheKey: String) {
        withContext(Dispatchers.IO) {
            try {
                val result = if (!video.coverPath.isNullOrEmpty()) {
                    thumbnailGenerator.generateThumbnailFromPath(video.coverPath!!)
                } else {
                    thumbnailGenerator.generateThumbnailFromPath(video.path)
                }
                
                when (result) {
                    is ThumbnailResult.Success -> {
                        // 存储到缓存
                        thumbnailCache.putThumbnail(cacheKey, result.filePath, video.path)
                        
                        withContext(Dispatchers.Main) {
                            displayThumbnail(result.filePath)
                        }
                    }
                    is ThumbnailResult.Failure -> {
                        withContext(Dispatchers.Main) {
                            showDefaultThumbnail()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showDefaultThumbnail()
                }
            }
        }
    }

    /**
     * 显示缩略图
     */
    private fun displayThumbnail(filePath: String) {
        ivThumbnail.load(filePath) {
            crossfade(true)
            crossfade(300)
            transformations(RoundedCornersTransformation(12f))
            placeholder(R.drawable.ic_video_placeholder)
            error(R.drawable.ic_video_error)
        }
        
        // 淡入动画
        ivThumbnail.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    /**
     * 显示默认占位符
     */
    private fun showDefaultThumbnail() {
        ivThumbnail.load(R.drawable.ic_video_placeholder) {
            transformations(RoundedCornersTransformation(12f))
        }
        ivThumbnail.alpha = 1f
    }

    /**
     * 生成缓存键
     */
    private fun generateCacheKey(video: VideoEntity): String {
        val source = video.path
        return "${source}_480x800"
    }

    private fun setupLikeButton() {
        likeButton.setOnLikeClickListener {
            currentVideo?.let { video ->
                viewHolderScope.launch {
                    try {
                        likeRepository?.toggleLike(video.id)
                        onLikeChanged?.invoke(video.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun loadLikeState(video: VideoEntity) {
        likeRepository?.let {
            viewHolderScope.launch {
                try {
                    val isLiked = it.isVideoLiked(video.id)
                    val likeCount = it.getLikeCount(video.id)
                    likeButton.setLiked(isLiked, likeCount)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
