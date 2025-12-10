package com.light.mimictiktok.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.light.mimictiktok.R
import kotlin.math.roundToInt

class ProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Colors
    private val progressColor = ContextCompat.getColor(context, R.color.progress_active)
    private val bufferColor = ContextCompat.getColor(context, R.color.progress_buffer)
    private val backgroundColor = ContextCompat.getColor(context, R.color.progress_background)
    
    // Paints
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }
    
    private val bufferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bufferColor
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }
    
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }
    
    // Progress values
    private var progress: Float = 0f        // 0-1 range
    private var bufferProgress: Float = 0f  // 0-1 range
    var isDragging = false
        private set

    // Listeners
    private var seekListener: OnSeekListener? = null
    
    interface OnSeekListener {
        fun onSeek(progress: Float)
        fun onSeekStart()
        fun onSeekEnd()
    }
    
    fun setOnSeekListener(listener: OnSeekListener) {
        seekListener = listener
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val padding = progressPaint.strokeWidth / 2
        val rect = RectF(
            padding,
            padding,
            width - padding,
            height - padding
        )
        
        // Draw background track
        canvas.drawArc(rect, 90f, 180f, false, backgroundPaint)
        
        // Draw buffer progress
        val bufferAngle = bufferProgress * 180f
        canvas.drawArc(rect, 90f, bufferAngle, false, bufferPaint)
        
        // Draw main progress
        val progressAngle = progress * 180f
        if (progressAngle > 0) {
            canvas.drawArc(rect, 90f, progressAngle, false, progressPaint)
        }
        
        // Draw progress handle
        val handleX = width * progress
        val handleY = height / 2f
        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = progressColor
            style = Paint.Style.FILL
        }
        
        if (isDragging) {
            canvas.drawCircle(handleX, handleY, 10f, handlePaint)
        } else if (progress > 0 && progress < 1) {
            canvas.drawCircle(handleX, handleY, 6f, handlePaint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                val handleX = width * progress
                val handleY = height / 2f
                
                if (x >= handleX - 20 && x <= handleX + 20 && y >= handleY - 20 && y <= handleY + 20) {
                    isDragging = true
                    seekListener?.onSeekStart()
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val newProgress = (event.x / width).coerceIn(0f, 1f)
                    progress = newProgress
                    seekListener?.onSeek(newProgress)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    seekListener?.onSeekEnd()
                    parent.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
    
    fun setProgress(progress: Float) {
        if (!isDragging) {
            this.progress = progress.coerceIn(0f, 1f)
            invalidate()
        }
    }
    
    fun setBufferProgress(progress: Float) {
        this.bufferProgress = progress.coerceIn(0f, 1f)
        invalidate()
    }
    
    fun getProgress(): Float = progress
    
    fun reset() {
        progress = 0f
        bufferProgress = 0f
        isDragging = false
        invalidate()
    }
    
    companion object {
        fun formatTime(timeMs: Long): String {
            val totalSeconds = (timeMs / 1000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "${minutes}:${String.format("%02d", seconds)}"
        }
    }
}