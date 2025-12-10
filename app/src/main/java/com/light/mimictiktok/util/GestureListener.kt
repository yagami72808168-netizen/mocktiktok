package com.light.mimictiktok.util

/**
 * 手势监听器接口
 * 定义视频播放器的各种手势回调事件
 */
interface GestureListener {
    
    /**
     * 单击事件
     * 通常用于播放/暂停切换
     */
    fun onSingleTap()
    
    /**
     * 双击事件
     */
    fun onDoubleTap()
    
    /**
     * 双击左半屏（快速后退）
     */
    fun onDoubleTapLeft()
    
    /**
     * 双击右半屏（快速前进）
     */
    fun onDoubleTapRight()
    
    /**
     * 横向滑动（进度调节）
     * @param deltaX 水平滑动距离（像素）
     * @param deltaY 垂直滑动距离（像素）
     * @param totalDeltaX 累计水平滑动距离
     */
    fun onHorizontalScroll(deltaX: Float, deltaY: Float, totalDeltaX: Float)
    
    /**
     * 垂直滑动开始
     * @param isLeft 是否在屏幕左侧（用于判断是亮度还是音量）
     */
    fun onVerticalScrollStart(isLeft: Boolean)
    
    /**
     * 垂直滑动更新
     * @param deltaY 垂直滑动距离（像素）
     * @param totalDeltaY 累计垂直滑动距离
     * @param isLeft 是否在屏幕左侧
     */
    fun onVerticalScroll(deltaY: Float, totalDeltaY: Float, isLeft: Boolean)
    
    /**
     * 垂直滑动结束
     */
    fun onVerticalScrollEnd()
    
    /**
     * 快速滑动
     */
    fun onFling(velocityX: Float, velocityY: Float)
}