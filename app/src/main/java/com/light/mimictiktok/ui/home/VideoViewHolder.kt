package com.light.mimictiktok.ui.home

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
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
import com.light.mimictiktok.ui.widgets.GestureOverlay
import com.light.mimictiktok.util.GestureDetector
import com.light.mimictiktok.util.GestureListener
import com.light.mimictiktok.util.ThumbnailCache
import com.light.mimictiktok.util.ThumbnailGenerator
import com.light.mimictiktok.util.ThumbnailResult
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

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
    private val mainContainer: FrameLayout = itemView.findViewById(R.id.videoPlayerContainer)
    
    private var currentPlayer: ExoPlayer? = null
    private var playbackListener: Player.Listener? = null
    private var currentVideo: VideoEntity? = null
    private val viewHolderScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var gestureOverlay: GestureOverlay? = null
    private var gestureDetector: GestureDetector? = null
    
    // 手势相关状态
    private var volumeLevel = 50
    private var brightnessLevel = 50
    private var videoDuration = 0L

    private val scaleTransformationMatrix = ScaleTransformationMatrix()
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private var isZooming = false
    
    var onLongPressed: (() -> Unit)? = null

    init {
        setupGestureControl()
    }

    private fun setupGestures() {
        scaleGestureDetector = ScaleGestureDetector(itemView.context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isZooming = true
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val videoSurfaceView = playerView.videoSurfaceView as? TextureView
                videoSurfaceView?.let { view ->
                    scaleTransformationMatrix.setViewSize(view.width.toFloat(), view.height.toFloat())
                    val matrix = scaleTransformationMatrix.onScale(detector.scaleFactor, detector.focusX, detector.focusY)
                    view.setTransform(matrix)
                }
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isZooming = false
                val currentScale = scaleTransformationMatrix.getCurrentScale()
                if (currentScale < 1.05f) {
                     // Reset if scale is close to 1
                     val videoSurfaceView = playerView.videoSurfaceView as? TextureView
                     videoSurfaceView?.let { view ->
                         view.setTransform(scaleTransformationMatrix.reset())
                     }
                }
            }
        })

        gestureDetector = GestureDetector(itemView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                togglePlayPause()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                currentVideo?.let { video ->
                    val context = itemView.context
                    val intent = FullscreenVideoActivity.newIntent(context, video.path)
                    context.startActivity(intent)
                }
                return true
            }
            
            override fun onLongPress(e: MotionEvent) {
                onLongPressed?.invoke()
            }
            
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (scaleTransformationMatrix.getCurrentScale() > 1.05f) {
                    val videoSurfaceView = playerView.videoSurfaceView as? TextureView
                    videoSurfaceView?.let { view ->
                        val matrix = scaleTransformationMatrix.onScroll(distanceX, distanceY)
                        view.setTransform(matrix)
                    }
                    return true
                }
                return false
            }
        })
    }


    fun bind(video: VideoEntity, player: ExoPlayer) {
        currentPlayer = player
        currentVideo = video
        playerView.player = player
        videoDuration = video.duration
        
        tvTitle.text = video.title ?: "Unknown"
        tvDuration.text = formatDuration(video.duration)
        
        // Setup playback controls
        setupPlaybackControlView(player, video)
        
        // 初始化手势覆盖层
        initGestureOverlay()
        
        // 加载缩略图
        loadThumbnail(video)
        
        // 加载点赞状态
        loadLikeState(video)
    }

    fun setResizeMode(mode: Int) {
        playerView.resizeMode = mode
    }

    fun unbind() {
        // Save playback progress before unbinding
        savePlaybackProgress()
        
        // 移除手势覆盖层
        gestureOverlay?.let {
            mainContainer.removeView(it)
            gestureOverlay = null
        }
        
        // 移除手势监听器
        gestureDetector?.detachFromView()
        gestureDetector = null
        
        // 取消协程任务
        viewHolderScope.cancel()
        
        // 重置缩略图
        ivThumbnail.setImageDrawable(null)
    }
    
    private fun setupGestureControl() {
        val gestureListener = object : GestureListener {
            override fun onSingleTap() {
                togglePlayPause()
            }
            
            override fun onDoubleTap() {}
            
            override fun onDoubleTapLeft() {
                fastRewind()
            }
            
            override fun onDoubleTapRight() {
                fastForward()
            }
            
            override fun onHorizontalScroll(deltaX: Float, deltaY: Float, totalDeltaX: Float) {
                seekVideoByGesture(totalDeltaX)
            }
            
            override fun onVerticalScrollStart(isLeft: Boolean) {
                // 初始化垂直滑动手势
            }
            
            override fun onVerticalScroll(deltaY: Float, totalDeltaY: Float, isLeft: Boolean) {
                val deltaPercent = (totalDeltaY / itemView.height * -100).toInt()
                
                if (isLeft) {
                    adjustBrightness(deltaPercent)
                } else {
                    adjustVolume(deltaPercent)
                }
            }
            
            override fun onVerticalScrollEnd() {
                gestureOverlay?.hideAllOverlays()
            }
            
            override fun onFling(velocityX: Float, velocityY: Float) {}
        }
        
        gestureDetector = GestureDetector(
            context = itemView.context,
            listener = gestureListener,
            view = itemView
        )
        gestureDetector?.attachToView()
    }
    
    private fun initGestureOverlay() {
        if (gestureOverlay == null) {
            gestureOverlay = GestureOverlay(itemView.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            mainContainer.addView(gestureOverlay)
        }
    }
    
    private fun fastForward() {
        currentPlayer?.let { player ->
            val currentPosition = player.currentPosition
            val newPosition = currentPosition + 10000
            player.seekTo(min(newPosition, player.duration))
            gestureOverlay?.showProgress(
                player.currentPosition, 
                player.duration, 
                (player.currentPosition * 100 / player.duration).toInt()
            )
        }
    }
    
    private fun fastRewind() {
        currentPlayer?.let { player ->
            val currentPosition = player.currentPosition
            val newPosition = currentPosition - 10000
            player.seekTo(max(0L, newPosition))
            gestureOverlay?.showProgress(
                player.currentPosition, 
                player.duration, 
                (player.currentPosition * 100 / player.duration).toInt()
            )
        }
    }
    
    private fun seekVideoByGesture(totalDeltaX: Float) {
        currentPlayer?.let { player ->
            val duration = player.duration
            if (duration <= 0) return
            
            val seekRatio = totalDeltaX / itemView.width
            val seekTime = (duration * seekRatio * 0.3).toLong()
            val newPosition = player.currentPosition + seekTime
            
            val boundedPosition = newPosition.coerceIn(0L, duration)
            player.seekTo(boundedPosition)
            
            gestureOverlay?.showProgress(
                boundedPosition,
                duration,
                (boundedPosition * 100 / duration).toInt()
            )
        }
    }
    
    private fun adjustBrightness(deltaPercent: Int) {
        brightnessLevel = max(0, min(100, brightnessLevel - deltaPercent))
        gestureOverlay?.showBrightness(brightnessLevel)
    }
    
    private fun adjustVolume(deltaPercent: Int) {
        volumeLevel = max(0, min(100, volumeLevel - deltaPercent))
        gestureOverlay?.showVolume(volumeLevel)
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
