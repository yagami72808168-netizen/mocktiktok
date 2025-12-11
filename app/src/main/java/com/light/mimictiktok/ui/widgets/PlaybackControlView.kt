package com.light.mimictiktok.ui.widgets

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.light.mimictiktok.R
import kotlin.math.abs

class PlaybackControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    
    // UI Components
    private lateinit var playPauseButton: PlayPauseButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var ivSpeedControl: ImageView
    
    // State
    private var isVisible = true
    private var hideDelay = 3000L
    private var hideRunnable: Runnable? = null
    private var handler = android.os.Handler()
    private var currentSpeedIndex = 0
    private val playbackSpeeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
    
    // Listeners
    private var controlListener: ControlListener? = null
    
    interface ControlListener {
        fun onPlayPause()
        fun onSeek(progress: Float)
        fun onSpeedControl(speed: Float)
        fun onFastForward()
        fun onRewind()
    }
    
    // Gesture detection
    private val gestureDetector: GestureDetector
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Handle double tap for fast forward/rewind
            val centerX = width / 2f
            when {
                e.x < centerX * 0.4f -> {
                    // Left side double tap - rewind
                    controlListener?.onRewind()
                }
                e.x > centerX * 1.6f -> {
                    // Right side double tap - fast forward
                    controlListener?.onFastForward()
                }
            }
            return super.onDoubleTap(e)
        }
        
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // Toggle visibility on single tap
            toggleVisibility()
            return super.onSingleTapConfirmed(e)
        }
    }
    
    init {
        // Inflate layout
        inflate(context, R.layout.view_playback_controls, this)
        
        // Initialize gesture detector
        gestureDetector = GestureDetector(context, gestureListener)
        
        // Initialize UI components
        initializeViews()
        
        // Setup timeline bar
        setupTimelineBar()
        
        // Setup speed control
        setupSpeedControl()
    }
    
    private fun initializeViews() {
        playPauseButton = findViewById(R.id.playPauseButton)
        progressBar = findViewById(R.id.progressBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        ivSpeedControl = findViewById(R.id.ivSpeedControl)
        
        // Set initial visibility
        showControls()
    }
    
    private fun setupTimelineBar() {
        progressBar.setOnSeekListener(object : ProgressBar.OnSeekListener {
            override fun onSeek(progress: Float) {
                val currentTime = (getTotalTime() * progress).toLong()
                tvCurrentTime.text = ProgressBar.formatTime(currentTime)
                controlListener?.onSeek(progress)
            }
            
            override fun onSeekStart() {
                cancelHideTimeout()
            }
            
            override fun onSeekEnd() {
                scheduleHideTimeout()
            }
        })
    }
    
    private fun setupSpeedControl() {
        ivSpeedControl.setOnClickListener {
            cyclePlaybackSpeed()
        }
        
        updateSpeedDisplay()
    }
    
    private fun cyclePlaybackSpeed() {
        currentSpeedIndex = (currentSpeedIndex + 1) % playbackSpeeds.size
        val speed = playbackSpeeds[currentSpeedIndex]
        updateSpeedDisplay()
        controlListener?.onSpeedControl(speed)
    }
    
    private fun updateSpeedDisplay() {
        val speed = playbackSpeeds[currentSpeedIndex]
        val speedText = when (speed) {
            0.5f -> "0.5x"
            1.25f -> "1.25x"
            1.5f -> "1.5x"
            2.0f -> "2x"
            else -> "1x"
        }
        // Create text view or use text instead of image
        // For now, using a simple text display
        if (ivSpeedControl is TextView) {
            (ivSpeedControl as TextView).text = speedText
        }
    }
    
    fun setControlListener(listener: ControlListener) {
        controlListener = listener
    }
    
    fun attachPlayer(player: ExoPlayer) {
        playPauseButton.setOnClickListener {
            controlListener?.onPlayPause()
        }
    }
    
    // Timeline updates
    fun updateProgress(currentMs: Long, totalMs: Long) {
        val progress = if (totalMs > 0) currentMs.toFloat() / totalMs else 0f
        progressBar.setProgress(progress)
        tvCurrentTime.text = ProgressBar.formatTime(currentMs)
        tvTotalTime.text = ProgressBar.formatTime(totalMs)
    }
    
    fun updateBufferProgress(bufferedMs: Long, totalMs: Long) {
        val bufferProgress = if (totalMs > 0) bufferedMs.toFloat() / totalMs else 0f
        progressBar.setBufferProgress(bufferProgress)
    }
    
    fun setTotalTime(totalMs: Long) {
        tvTotalTime.text = ProgressBar.formatTime(totalMs)
    }
    
    private fun getTotalTime(): Long {
        // Parse total time from text view
        val timeStr = tvTotalTime.text.toString()
        val parts = timeStr.split(":")
        return if (parts.size == 2) {
            val minutes = parts[0].toIntOrNull() ?: 0
            val seconds = parts[1].toIntOrNull() ?: 0
            ((minutes * 60L) + seconds) * 1000L
        } else 0L
    }
    
    // Visibility control
    fun showControls() {
        if (!isVisible) {
            isVisible = true
            animateVisibility(1f)
            scheduleHideTimeout()
        }
    }
    
    fun hideControls() {
        if (isVisible && !progressBar.isDragging) {
            isVisible = false
            animateVisibility(0f)
            cancelHideTimeout()
        }
    }
    
    fun toggleVisibility() {
        if (isVisible) {
            hideControls()
        } else {
            showControls()
        }
    }
    
    private fun animateVisibility(alpha: Float) {
        val views = listOf(playPauseButton, progressBar, tvCurrentTime, tvTotalTime, ivSpeedControl)
        
        views.forEach { view ->
            view.animate()
                .alpha(alpha)
                .setDuration(300)
                .withStartAction {
                    if (alpha > 0) view.visibility = View.VISIBLE
                }
                .withEndAction {
                    if (alpha == 0f) view.visibility = View.GONE
                }
                .start()
        }
    }
    
    private fun scheduleHideTimeout() {
        cancelHideTimeout()
        hideRunnable = Runnable {
            hideControls()
        }
        handler.postDelayed(hideRunnable!!, hideDelay)
    }
    
    private fun cancelHideTimeout() {
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = null
    }
    
    // Remove double tap animation helpers - animations handled by parent layout
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }
    
    // Lifecycle methods
    fun onResume() {
        if (isVisible) {
            scheduleHideTimeout()
        }
    }
    
    fun onPause() {
        cancelHideTimeout()
    }
    
    fun reset() {
        progressBar.reset()
        updateProgress(0, 0)
        showControls()
    }
}