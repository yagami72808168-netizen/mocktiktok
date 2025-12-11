package com.light.mimictiktok.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.light.mimictiktok.R

/**
 * 手势反馈覆盖层
 * 显示亮度、音量、进度等手势操作的视觉反馈
 */
class GestureOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val handler = Handler(Looper.getMainLooper())
    private var currentProgressBar: ProgressBar? = null
    private var currentValueTextView: TextView? = null
    private var currentContainer: View? = null
    
    // 中心区域控件
    private lateinit var centerContainer: View
    private lateinit var progressBarHorizontal: ProgressBar
    private lateinit var tvProgressValue: TextView
    private lateinit var tvProgressLabel: TextView
    
    // 左侧区域控件（亮度）
    private lateinit var brightnessContainer: View
    private lateinit var progressBarBrightness: ProgressBar
    private lateinit var tvBrightnessValue: TextView
    private lateinit var ivBrightnessIcon: ImageView
    
    // 右侧区域控件（音量）
    private lateinit var volumeContainer: View
    private lateinit var progressBarVolume: ProgressBar
    private lateinit var tvVolumeValue: TextView
    private lateinit var ivVolumeIcon: ImageView

    init {
        initView()
    }

    @SuppressLint("InflateParams")
    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.gesture_overlay, this, true)
        
        // 初始化中心区域
        centerContainer = findViewById(R.id.gestureCenterContainer)
        progressBarHorizontal = findViewById(R.id.progressBarHorizontal)
        tvProgressValue = findViewById(R.id.tvProgressValue)
        tvProgressLabel = findViewById(R.id.tvProgressLabel)
        
        // 初始化亮度区域
        brightnessContainer = findViewById(R.id.brightnessContainer)
        progressBarBrightness = findViewById(R.id.progressBarBrightness)
        tvBrightnessValue = findViewById(R.id.tvBrightnessValue)
        ivBrightnessIcon = findViewById(R.id.ivBrightnessIcon)
        
        // 初始化音量区域
        volumeContainer = findViewById(R.id.volumeContainer)
        progressBarVolume = findViewById(R.id.progressBarVolume)
        tvVolumeValue = findViewById(R.id.tvVolumeValue)
        ivVolumeIcon = findViewById(R.id.ivVolumeIcon)
        
        // 初始隐藏所有容器
        centerContainer.alpha = 0f
        brightnessContainer.alpha = 0f
        volumeContainer.alpha = 0f
    }

    /**
     * 显示进度调节
     */
    fun showProgress(currentPosition: Long, totalDuration: Long, progressPercent: Int) {
        hideAllOverlays()
        
        currentContainer = centerContainer
        currentProgressBar = progressBarHorizontal
        currentValueTextView = tvProgressValue
        
        tvProgressLabel.text = "进度"
        tvProgressValue.text = String.format("%d%%", progressPercent)
        progressBarHorizontal.progress = progressPercent
        
        showWithAnimation(centerContainer)
        scheduleHide()
    }

    /**
     * 显示亮度调节
     */
    fun showBrightness(progress: Int) {
        hideAllOverlays()
        
        currentContainer = brightnessContainer
        currentProgressBar = progressBarBrightness
        currentValueTextView = tvBrightnessValue
        
        tvBrightnessValue.text = String.format("%d%%", progress)
        progressBarBrightness.progress = progress
        
        updateBrightnessIcon(progress)
        
        showWithAnimation(brightnessContainer)
        scheduleHide()
    }

    /**
     * 显示音量调节
     */
    fun showVolume(progress: Int, isMuted: Boolean = false) {
        hideAllOverlays()
        
        currentContainer = volumeContainer
        currentProgressBar = progressBarVolume
        currentValueTextView = tvVolumeValue
        
        tvVolumeValue.text = String.format("%d%%", progress)
        progressBarVolume.progress = if (isMuted) 0 else progress
        
        updateVolumeIcon(progress, isMuted)
        
        showWithAnimation(volumeContainer)
        scheduleHide()
    }

    /**
     * 更新当前进度条的值
     */
    fun updateProgress(progress: Int, maxValue: Int = 100) {
        currentProgressBar?.max = maxValue
        currentProgressBar?.progress = progress
        currentValueTextView?.text = String.format("%d%%", progress * 100 / maxValue)
    }

    /**
     * 隐藏所有覆盖层
     */
    fun hideAllOverlays() {
        handler.removeCallbacksAndMessages(null)
        
        centerContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .start()
            
        brightnessContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .start()
            
        volumeContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .start()
    }

    private fun showWithAnimation(view: View) {
        view.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun scheduleHide() {
        handler.postDelayed({
            hideAllOverlays()
        }, 1500) // 1.5秒后自动隐藏
    }

    private fun updateBrightnessIcon(brightness: Int) {
        val iconRes = when {
            brightness > 80 -> android.R.drawable.ic_menu_manage
            brightness > 60 -> android.R.drawable.ic_menu_manage
            brightness > 40 -> android.R.drawable.ic_menu_manage
            brightness > 20 -> android.R.drawable.ic_menu_manage
            else -> android.R.drawable.ic_menu_manage
        }
        ivBrightnessIcon.setImageResource(iconRes)
    }

    private fun updateVolumeIcon(volume: Int, isMuted: Boolean) {
        val iconRes = when {
            isMuted -> android.R.drawable.ic_lock_silent_mode
            volume == 0 -> android.R.drawable.ic_lock_silent_mode
            volume < 30 -> android.R.drawable.ic_lock_silent_mode
            volume < 70 -> android.R.drawable.ic_lock_silent_mode_off
            else -> android.R.drawable.ic_lock_silent_mode_off
        }
        ivVolumeIcon.setImageResource(iconRes)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }
}