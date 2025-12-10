package com.light.mimictiktok.util

import kotlin.math.abs

/**
 * 无限循环列表工具类
 * 提供虚拟化列表的位置映射和循环管理功能
 */
object ListLooper {
    
    /**
     * 将虚拟位置映射到实际数据索引
     * 通过取模运算实现虚拟到实际的映射
     * 
     * @param virtualPosition 虚拟列表中的位置（在 Int.MAX_VALUE 范围内）
     * @param dataSize 实际数据列表的大小
     * @return 实际数据列表中的索引，如果为空返回 -1
     */
    fun mapToRealPosition(virtualPosition: Int, dataSize: Int): Int {
        if (dataSize <= 0) return -1
        return virtualPosition % dataSize
    }
    
    /**
     * 获取无限循环模式下的项目总数
     * 使用 Int.MAX_VALUE 作为虚拟列表大小
     * 
     * @param dataSize 实际数据列表的大小
     * @return 如果数据为空返回 0，否则返回 Int.MAX_VALUE
     */
    fun getInfiniteCount(dataSize: Int): Int {
        return if (dataSize <= 0) 0 else Int.MAX_VALUE
    }
    
    /**
     * 计算适合开始的虚拟位置
     * 选择一个靠近 Int.MAX_VALUE 中间的位置，使其对齐到数据大小的倍数
     * 这样可以确保向前和向后都有足够的滚动空间
     * 
     * @param dataSize 实际数据列表的大小
     * @param initialRealPosition 希望开始播放的实际数据索引（可选，默认 0）
     * @return 计算出的虚拟起始位置
     */
    fun calculateStartVirtualPosition(dataSize: Int, initialRealPosition: Int = 0): Int {
        if (dataSize <= 0) return 0
        
        val basePosition = Int.MAX_VALUE / 2
        val alignedPosition = basePosition - (basePosition % dataSize)
        
        if (initialRealPosition in 0 until dataSize) {
            return alignedPosition + initialRealPosition
        }
        
        return alignedPosition
    }
    
    /**
     * 将实际数据索引转换为当前循环中的虚拟位置
     * 用于在当前可见的循环范围内导航
     * 
     * @param realPosition 实际数据索引
     * @param currentVirtualPosition 当前虚拟位置
     * @param dataSize 实际数据列表的大小
     * @return 对应的虚拟位置
     */
    fun convertToVirtualPosition(realPosition: Int, currentVirtualPosition: Int, dataSize: Int): Int {
        if (dataSize <= 0 || realPosition !in 0 until dataSize) return currentVirtualPosition
        
        val currentRealPosition = mapToRealPosition(currentVirtualPosition, dataSize)
        val currentCycle = currentVirtualPosition / dataSize
        
        return currentCycle * dataSize + realPosition
    }
    
    /**
     * 计算目标虚拟位置以便平滑循环
     * 当接近列表边界时，计算下一个循环的起始位置
     * 
     * @param currentPosition 当前虚拟位置
     * @param dataSize 实际数据列表的大小
     * @param direction 滚动方向（1 表示向下/正向，-1 表示向上/反向）
     * @return 推荐的目标虚拟位置
     */
    fun calculateSmoothLoopPosition(currentPosition: Int, dataSize: Int, direction: Int): Int {
        if (dataSize <= 0) return currentPosition
        
        val threshold = dataSize / 2
        val realPosition = mapToRealPosition(currentPosition, dataSize)
        
        return when {
            direction > 0 && realPosition > dataSize - threshold -> {
                ((currentPosition / dataSize) + 1) * dataSize
            }
            direction < 0 && realPosition < threshold -> {
                ((currentPosition / dataSize) - 1) * dataSize + dataSize - 1
            }
            else -> currentPosition
        }
    }
    
    /**
     * 检查是否需要在循环边界跳转
     * 用于判断是否需要进行无闪烁的循环重置
     * 
     * @param virtualPosition 当前虚拟位置
     * @param dataSize 实际数据列表大小
     * @return 如果需要跳转返回 true
     */
    fun needsLoopReset(virtualPosition: Int, dataSize: Int): Boolean {
        if (dataSize <= 0) return false
        
        val currentRealPosition = mapToRealPosition(virtualPosition, dataSize)
        val cycle = virtualPosition / dataSize
        val totalCycles = Int.MAX_VALUE / dataSize
        
        return cycle <= 1 || cycle >= totalCycles - 1
    }
}