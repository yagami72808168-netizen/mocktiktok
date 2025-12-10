package com.light.mimictiktok.ui.widgets

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.light.mimictiktok.R
import com.light.mimictiktok.ui.animations.LikeAnimationView

class LikeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val likeIcon: ImageView
    private val likeCount: TextView
    private val animationView: LikeAnimationView
    private var isLiked = false
    private var likeCountValue = 0
    private var onLikeClickListener: (() -> Unit)? = null

    init {
        inflate(context, R.layout.widget_like_button, this)
        likeIcon = findViewById(R.id.likeIcon)
        likeCount = findViewById(R.id.likeCount)
        animationView = findViewById(R.id.animationView)

        setOnClickListener {
            toggleLike()
            onLikeClickListener?.invoke()
        }
    }

    fun setLiked(liked: Boolean, count: Int = likeCountValue) {
        isLiked = liked
        likeCountValue = count
        updateUI()
    }

    fun toggleLike() {
        isLiked = !isLiked
        likeCountValue += if (isLiked) 1 else -1
        updateUI()
        animationView.animateLike()
        animateLikeIcon()
    }

    fun setLikeCount(count: Int) {
        likeCountValue = count
        updateUI()
    }

    fun setOnLikeClickListener(listener: (() -> Unit)?) {
        onLikeClickListener = listener
    }

    private fun updateUI() {
        val iconRes = if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        likeIcon.setImageResource(iconRes)

        val iconColor = if (isLiked) 0xFFFF4458.toInt() else 0xFF999999.toInt()
        likeIcon.setColorFilter(iconColor)

        likeCount.text = likeCountValue.toString()
        val countColor = if (isLiked) 0xFFFF4458.toInt() else 0xFFFFFFFF.toInt()
        likeCount.setTextColor(countColor)
    }

    private fun animateLikeIcon() {
        if (!isLiked) return

        val scaleX = ObjectAnimator.ofFloat(likeIcon, "scaleX", 1f, 1.2f, 1f).apply {
            duration = 200
        }
        val scaleY = ObjectAnimator.ofFloat(likeIcon, "scaleY", 1f, 1.2f, 1f).apply {
            duration = 200
        }

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
    }
}
