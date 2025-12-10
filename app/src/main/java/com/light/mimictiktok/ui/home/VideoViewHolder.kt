package com.light.mimictiktok.ui.home

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.light.mimictiktok.R
import com.light.mimictiktok.data.db.VideoEntity
import com.light.mimictiktok.ui.widgets.PlaybackControlView
import com.light.mimictiktok.util.ThumbnailCache
import com.light.mimictiktok.util.ThumbnailGenerator
import com.light.mimictiktok.util.ThumbnailResult
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class VideoViewHolder(
    itemView: View,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val thumbnailCache: ThumbnailCache,
    private val playbackControlViewModel: PlaybackControlViewModel? = null
) : RecyclerView.ViewHolder(itemView) {
    private val playerView: PlayerView = itemView.findViewById(R.id.playerView)
    private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
    private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
    private val playbackControlView: PlaybackControlView = itemView.findViewById(R.id.playbackControlView)
    
    private var currentPlayer: ExoPlayer? = null
    private var playbackListener: Player.Listener? = null
    private var currentVideo: VideoEntity? = null
    private val viewHolderScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        setupPlaybackControls()
        // Keep click listener for backward compatibility
        itemView.setOnClickListener {
            playbackControlView.toggleVisibility()
        }
    }

    private fun setupPlaybackControls() {
        playbackControlView.setControlListener(object : PlaybackControlView.ControlListener {
            override fun onPlayPause() {
                currentPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                }
            }
            
            override fun onSeek(progress: Float) {
                currentPlayer?.let { player ->
                    val duration = player.duration
                    if (duration > 0) {
                        val targetPosition = (duration * progress).toLong()
                        player.seekTo(targetPosition)
                    }
                }
            }
            
            override fun onSpeedControl(speed: Float) {
                currentPlayer?.setPlaybackSpeed(speed)
            }
            
            override fun onFastForward() {
                currentPlayer?.let { player ->
                    val newPosition = player.currentPosition + 10000 // 10 seconds
                    player.seekTo(newPosition.coerceIn(0, player.duration))
                    playbackControlView.showControls() // Show feedback
                }
            }
            
            override fun onRewind() {
                currentPlayer?.let { player ->
                    val newPosition = player.currentPosition - 10000 // 10 seconds
                    player.seekTo(newPosition.coerceIn(0, player.duration))
                    playbackControlView.showControls() // Show feedback
                }
            }
        })
    }

    fun bind(video: VideoEntity, player: ExoPlayer) {
        currentPlayer = player
        currentVideo = video
        playerView.player = player
        
        tvTitle.text = video.title ?: "Unknown"
        tvDuration.text = formatDuration(video.duration)
        
        // Setup playback controls
        setupPlaybackControlView(player, video)
        
        // 加载缩略图
        loadThumbnail(video)
    }

    fun unbind() {
        // Save playback progress before unbinding
        savePlaybackProgress()
        
        // 取消协程任务
        viewHolderScope.cancel()
        
        // 重置缩略图
        ivThumbnail.setImageDrawable(null)
    }

    private fun setupPlaybackControlView(player: ExoPlayer, video: VideoEntity) {
        // Initialize control view with video info
        playbackControlView.setTotalTime(video.duration)
        playbackControlView.attachPlayer(player)
        
        // Setup player listeners for updates
        removePlaybackListener()
        playbackListener = player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playbackControlView.showControls() // Show controls on state change
                if (isPlaying) {
                    scheduleProgressUpdates(player)
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        // Player is ready, show initial controls
                        playbackControlView.showControls()
                        playbackControlView.setTotalTime(player.duration)
                    }
                    Player.STATE_BUFFERING -> {
                        // Show buffer state
                    }
                    Player.STATE_ENDED -> {
                        // Video ended - save final progress
                        savePlaybackProgress()
                    }
                }
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                // Update progress when seeking
                updateProgress(player)
            }
            
            override fun onRenderedFirstFrame() {
                // Video started playing, fade out thumbnail
                fadeOutThumbnail()
            }
        })
        
        // Show initial controls
        playbackControlView.showControls()
    }
    
    private fun scheduleProgressUpdates(player: ExoPlayer) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (player.isPlaying) {
                    updateProgress(player)
                    handler.postDelayed(this, 100) // Update every 100ms
                }
            }
        }
        handler.postDelayed(runnable, 100)
        
        // Store reference to cancel later if needed
        itemView.tag = runnable
    }
    
    private fun updateProgress(player: ExoPlayer) {
        // Update playback control view with current progress
        val duration = player.duration
        val position = player.currentPosition
        val buffered = player.bufferedPosition
        
        if (duration > 0) {
            playbackControlView.updateProgress(position, duration)
            playbackControlView.updateBufferProgress(buffered, duration)
        }
    }
    
    private fun savePlaybackProgress() {
        currentVideo?.let { video ->
            currentPlayer?.let { player ->
                if (player.duration > 0) {
                    // Cancel any existing progress update handlers
                    (itemView.tag as? Runnable)?.let { runnable ->
                        Handler(Looper.getMainLooper()).removeCallbacks(runnable)
                    }
                }
            }
        }
    }
    
    private fun fadeOutThumbnail() {
        ivThumbnail.animate()
            .alpha(0f)
            .setDuration(500)
            .start()
    }
    
    private fun removePlaybackListener() {
        playbackListener?.let { listener ->
            currentPlayer?.removeListener(listener)
        }
        playbackListener = null
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
}
