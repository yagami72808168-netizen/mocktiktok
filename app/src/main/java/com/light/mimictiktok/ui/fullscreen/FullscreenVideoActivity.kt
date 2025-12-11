package com.light.mimictiktok.ui.fullscreen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.light.mimictiktok.R
import com.light.mimictiktok.util.ScaleTransformationMatrix

class FullscreenVideoActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var ivBack: ImageView
    private var player: ExoPlayer? = null
    private var videoPath: String? = null
    
    private lateinit var scaleTransformationMatrix: ScaleTransformationMatrix
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private var isZooming = false

    companion object {
        private const val EXTRA_VIDEO_PATH = "extra_video_path"
        
        fun newIntent(context: Context, videoPath: String): Intent {
            return Intent(context, FullscreenVideoActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_PATH, videoPath)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_video)
        
        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
        
        playerView = findViewById(R.id.playerView)
        ivBack = findViewById(R.id.ivBack)
        scaleTransformationMatrix = ScaleTransformationMatrix()
        
        hideSystemUI()
        initializePlayer()
        setupGestures()
        
        ivBack.setOnClickListener {
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
    
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        
        videoPath?.let { path ->
            val mediaItem = MediaItem.fromUri(path)
            player?.setMediaItem(mediaItem)
            player?.repeatMode = Player.REPEAT_MODE_ONE
            player?.prepare()
            player?.play()
        }
    }

    private fun setupGestures() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
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
                     val videoSurfaceView = playerView.videoSurfaceView as? TextureView
                     videoSurfaceView?.let { view ->
                         view.setTransform(scaleTransformationMatrix.reset())
                     }
                }
            }
        })

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                finish()
                return true
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
        
        playerView.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }
    }
}
