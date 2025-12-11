package com.light.mimictiktok.ui.widgets

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.light.mimictiktok.R

class PlayPauseButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private enum class State {
        PLAY, PAUSE
    }
    
    private var currentState = State.PAUSE
    
    // Animation and drawing properties
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null
    private var progress = 0f // 0 = play, 1 = pause
    
    // Colors
    @ColorInt
    private var iconColor: Int = ContextCompat.getColor(context, android.R.color.white)
    
    // Icon appearance
    private var iconSize = 0f
    private var totalWidth = 0f
    private var totalHeight = 0f
    private var centerX = 0f
    private var centerY = 0f
    private val cornerRadius = 12f
    private val backgroundAlpha = 0.7f
    
    // Touch feedback
    private var isPressed = false
    private var downScale = 1f
    
    // Listeners
    private var clickListener: (() -> Unit)? = null
    
    init {
        // Enable click handling
        isClickable = true
        isFocusable = true
        
        // Setup paint
        paint.style = Paint.Style.FILL
        
        // Start in pause state
        progress = 0f
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Default size if no specific dimensions are provided
        val desiredSize = 80 // Default 80dp
        
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> Math.min(desiredSize, widthSize)
            else -> desiredSize
        }
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> Math.min(desiredSize, heightSize)
            else -> desiredSize
        }
        
        setMeasuredDimension(width, height)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        totalWidth = w.toFloat()
        totalHeight = h.toFloat()
        
        // Use the smaller dimension for the icon
        iconSize = Math.min(totalWidth, totalHeight) * 0.4f
        centerX = totalWidth / 2f
        centerY = totalHeight / 2f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Apply scale for press feedback
        canvas.save()
        canvas.scale(downScale, downScale, centerX, centerY)
        
        // Draw circular background
        drawBackground(canvas)
        
        // Draw play/pause icon
        when (currentState) {
            State.PLAY -> drawPauseIcon(canvas)
            State.PAUSE -> drawPlayIcon(canvas, progress)
        }
        
        canvas.restore()
    }
    
    private fun drawBackground(canvas: Canvas) {
        paint.color = Color.BLACK
        paint.alpha = (255 * backgroundAlpha).toInt()
        paint.style = Paint.Style.FILL
        
        val radius = Math.min(totalWidth, totalHeight) / 2f
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Reset alpha
        paint.alpha = 255
    }
    
    private fun drawPlayIcon(canvas: Canvas, progress: Float) {
        paint.color = iconColor
        
        // Simple play icon (triangle)
        val playSize = iconSize * (1 + progress * 0.1f)
        val path = Path()
        
        // Triangle points
        val leftX = centerX - playSize * 0.3f
        val topY = centerY - playSize * 0.4f
        val bottomY = centerY + playSize * 0.4f
        val rightX = centerX + playSize * 0.4f
        
        path.moveTo(leftX, topY)
        path.lineTo(leftX, bottomY)
        path.lineTo(rightX, centerY)
        path.close()
        
        canvas.drawPath(path, paint)
    }
    
    private fun drawPauseIcon(canvas: Canvas) {
        paint.color = iconColor
        
        // Pause icon (two vertical bars)
        val barWidth = iconSize * 0.12f
        val barHeight = iconSize * 0.6f
        val barSpacing = iconSize * 0.15f
        
        val leftBarLeft = centerX - barSpacing - barWidth / 2
        val rightBarLeft = centerX + barSpacing - barWidth / 2
        val barTop = centerY - barHeight / 2
        val barBottom = centerY + barHeight / 2
        
        val rect = RectF()
        
        // Left bar
        rect.set(leftBarLeft, barTop, leftBarLeft + barWidth, barBottom)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        
        // Right bar
        rect.set(rightBarLeft, barTop, rightBarLeft + barWidth, barBottom)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                animateDown(true)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isPressed) {
                    isPressed = false
                    animateDown(false)
                    performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                animateDown(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun animateDown(pressed: Boolean) {
        val targetScale = if (pressed) 0.9f else 1f
        ObjectAnimator.ofFloat(this, "downScale", downScale, targetScale).apply {
            duration = 150
            start()
        }
    }
    
    // Property for animation
    private fun setDownScale(scale: Float) {
        downScale = scale
        invalidate()
    }
    
    override fun performClick(): Boolean {
        val handled = super.performClick()
        clickListener?.invoke()
        return handled
    }
    
    // Public API
    fun setOnClickListener(listener: () -> Unit) {
        clickListener = listener
    }
    
    fun setPlaying(isPlaying: Boolean, animate: Boolean = true) {
        val newState = if (isPlaying) State.PLAY else State.PAUSE
        if (newState == currentState) return
        
        currentState = newState
        
        if (animate) {
            animateToState(newState)
        } else {
            progress = if (newState == State.PAUSE) 0f else 1f
            invalidate()
        }
    }
    
    private fun animateToState(targetState: State) {
        animator?.cancel()
        
        val startProgress = progress
        val endProgress = when (targetState) {
            State.PLAY -> 1f
            State.PAUSE -> 0f
        }
        
        animator = ValueAnimator.ofFloat(startProgress, endProgress).apply {
            duration = 300
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    fun isPlaying(): Boolean = currentState == State.PLAY
    
    fun setIconColor(@ColorInt color: Int) {
        iconColor = color
        invalidate()
    }
}