package com.light.mimictiktok.ui.animations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.sin

class LikeAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var heartScale = 0f
    private var heartAlpha = 255
    private var rippleRadius = 0f
    private var rippleAlpha = 255

    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4458.toInt()
    }

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF4458.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val particleList = mutableListOf<Particle>()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        if (heartScale > 0f) {
            drawHeart(canvas, centerX, centerY)
        }

        if (rippleRadius > 0f && rippleAlpha > 0) {
            ripplePaint.alpha = rippleAlpha
            canvas.drawCircle(centerX, centerY, rippleRadius, ripplePaint)
        }

        for (particle in particleList) {
            particle.draw(canvas)
        }
    }

    private fun drawHeart(canvas: Canvas, centerX: Float, centerY: Float) {
        heartPaint.alpha = heartAlpha

        val scale = 40f * heartScale
        val heartPath = android.graphics.Path()

        val x1 = centerX - 12f * scale
        val x2 = centerX - 4f * scale
        val x3 = centerX + 4f * scale
        val x4 = centerX + 12f * scale
        val y1 = centerY - 8f * scale
        val y2 = centerY + 12f * scale

        heartPath.moveTo(centerX, y2)
        heartPath.cubicTo(x1, y1, x1, y1 + 6f * scale, centerX - 6f * scale, y1 + 6f * scale)
        heartPath.cubicTo(x2, y1, x2, y1 - 4f * scale, centerX, y1)
        heartPath.cubicTo(x3, y1 - 4f * scale, x3, y1, centerX + 6f * scale, y1 + 6f * scale)
        heartPath.cubicTo(x4, y1 + 6f * scale, x4, y1, centerX, y2)
        heartPath.close()

        canvas.drawPath(heartPath, heartPaint)
    }

    fun animateLike() {
        particleList.clear()
        startHeartAnimation()
        startRippleAnimation()
        startFireworksAnimation()
    }

    private fun startHeartAnimation() {
        val scaleAnimator = ObjectAnimator.ofFloat(this, "heartScale", 0f, 1f, 0f).apply {
            duration = 400
        }

        val alphaAnimator = ObjectAnimator.ofInt(this, "heartAlpha", 255, 255, 0).apply {
            duration = 400
        }

        AnimatorSet().apply {
            playTogether(scaleAnimator, alphaAnimator)
            scaleAnimator.addUpdateListener {
                invalidate()
            }
            start()
        }
    }

    private fun startRippleAnimation() {
        val rippleAnimator = ObjectAnimator.ofFloat(this, "rippleRadius", 0f, 100f).apply {
            duration = 500
        }

        val alphaAnimator = ObjectAnimator.ofInt(this, "rippleAlpha", 255, 0).apply {
            duration = 500
        }

        AnimatorSet().apply {
            playTogether(rippleAnimator, alphaAnimator)
            rippleAnimator.addUpdateListener {
                invalidate()
            }
            start()
        }
    }

    private fun startFireworksAnimation() {
        val centerX = width / 2f
        val centerY = height / 2f

        repeat(8) { index ->
            val angle = (index * 45f) * Math.PI / 180f
            val particle = Particle(
                x = centerX,
                y = centerY,
                vx = (cos(angle) * 150f).toFloat(),
                vy = (sin(angle) * 150f).toFloat()
            )
            particleList.add(particle)

            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 600
                addUpdateListener {
                    val progress = it.animatedValue as Float
                    particle.update(progress)
                    invalidate()
                }
            }
            animator.start()
        }
    }

    @Suppress("unused")
    fun setHeartScale(scale: Float) {
        heartScale = scale
        invalidate()
    }

    @Suppress("unused")
    fun setHeartAlpha(alpha: Int) {
        heartAlpha = alpha
        invalidate()
    }

    @Suppress("unused")
    fun setRippleRadius(radius: Float) {
        rippleRadius = radius
        invalidate()
    }

    @Suppress("unused")
    fun setRippleAlpha(alpha: Int) {
        rippleAlpha = alpha
        invalidate()
    }

    private inner class Particle(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float
    ) {
        var alpha = 255
        var radius = 3f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF4458.toInt()
        }

        fun update(progress: Float) {
            x += vx * progress
            y += vy * progress + 100f * progress * progress
            alpha = (255 * (1f - progress)).toInt()
            radius = (3f * (1f - progress * 0.5f))
        }

        fun draw(canvas: Canvas) {
            paint.alpha = alpha
            canvas.drawCircle(x, y, radius, paint)
        }
    }
}
