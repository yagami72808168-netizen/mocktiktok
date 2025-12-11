package com.light.mimictiktok.util

import android.content.Context
import android.view.GestureDetector as AndroidGestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * 手势检测器
 * 负责识别各种手势并与RecyclerView滚动协调
 */
class GestureDetector(
    context: Context,
    private val listener: GestureListener,
    private val view: View
) {
    companion object {
        private const val SWIPE_THRESHOLD = 50 // 滑动阈值（像素）
        private const val SWIPE_VELOCITY_THRESHOLD = 100 // 速度阈值
        private const val DOUBLE_TAP_TIMEOUT = 300 // 双击超时时间（毫秒）
    }

    private val gestureDetector: AndroidGestureDetector
    private var lastTapTime = 0L
    private var isScrolling = false
    private var initialX = 0f
    private var initialY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var totalDeltaX = 0f
    private var totalDeltaY = 0f
    private var scrollDirection: ScrollDirection? = null

    enum class ScrollDirection {
        HORIZONTAL, VERTICAL, NONE
    }

    init {
        gestureDetector = AndroidGestureDetector(context, object : AndroidGestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                handleSingleTap()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val isLeftSide = isLeftSide(e.x)
                if (isLeftSide) {
                    listener.onDoubleTapLeft()
                } else {
                    listener.onDoubleTapRight()
                }
                listener.onDoubleTap()
                return true
            }

            override fun onScroll(
                e1: MotionEvent?, 
                e2: MotionEvent, 
                distanceX: Float, 
                distanceY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                
                if (!isScrolling) {
                    // 确定滚动方向
                    if (abs(deltaX) > abs(deltaY) && abs(deltaX) > 20) {
                        scrollDirection = ScrollDirection.HORIZONTAL
                        initialX = e1.x
                        totalDeltaX = 0f
                    } else if (abs(deltaY) > abs(deltaX) && abs(deltaY) > 20) {
                        scrollDirection = ScrollDirection.VERTICAL
                        initialY = e1.y
                        totalDeltaY = 0f
                        val isLeft = isLeftSide(e1.x)
                        listener.onVerticalScrollStart(isLeft)
                    }
                    isScrolling = true
                    lastX = e1.x
                    lastY = e1.y
                }
                
                when (scrollDirection) {
                    ScrollDirection.HORIZONTAL -> {
                        totalDeltaX += deltaX
                        listener.onHorizontalScroll(deltaX, deltaY, totalDeltaX)
                    }
                    ScrollDirection.VERTICAL -> {
                        totalDeltaY += deltaY
                        val isLeft = isLeftSide(e2.x)
                        listener.onVerticalScroll(deltaY, totalDeltaY, isLeft)
                    }
                    ScrollDirection.NONE -> {}
                    null -> {}
                }
                
                lastX = e2.x
                lastY = e2.y
                
                return true
            }

            override fun onFling(
                e1: MotionEvent?, 
                e2: MotionEvent, 
                velocityX: Float, 
                velocityY: Float
            ): Boolean {
                if (e1 != null) {
                    val diffY = e2.y - e1.y
                    val diffX = e2.x - e1.x
                    
                    if (abs(diffX) > abs(diffY) && 
                        abs(diffX) > SWIPE_THRESHOLD && 
                        abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        listener.onFling(velocityX, velocityY)
                        return true
                    }
                }
                return false
            }
        })
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isScrolling = false
                scrollDirection = ScrollDirection.NONE
                initialX = event.x
                initialY = event.y
                lastX = event.x
                lastY = event.y
                totalDeltaX = 0f
                totalDeltaY = 0f
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isScrolling) {
                    when (scrollDirection) {
                        ScrollDirection.VERTICAL -> {
                            listener.onVerticalScrollEnd()
                        }
                        ScrollDirection.HORIZONTAL -> {
                            // 横向滑动结束处理
                        }
                        ScrollDirection.NONE -> {
                            // 检查是否是点击
                            if (shouldHandleTap()) {
                                handleSingleTap()
                            }
                        }
                        null -> {}
                    }
                }
                isScrolling = false
                scrollDirection = ScrollDirection.NONE
            }
        }
        
        return gestureDetector.onTouchEvent(event)
    }

    private fun handleSingleTap() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT) {
            // 双击事件已被 GestureDetector 处理
            lastTapTime = 0L
        } else {
            listener.onSingleTap()
            lastTapTime = currentTime
        }
    }

    private fun isLeftSide(x: Float): Boolean {
        return x < view.width / 2f
    }

    private fun shouldHandleTap(): Boolean {
        return !isScrolling && 
               abs(totalDeltaX) < 20 && 
               abs(totalDeltaY) < 20
    }

    fun attachToView() {
        view.setOnTouchListener { _, event ->
            onTouchEvent(event)
        }
    }

    fun detachFromView() {
        view.setOnTouchListener(null)
    }
}