package com.light.mimictiktok.util

import org.junit.Assert.*
import org.junit.Test

class ListLooperTest {

    @Test
    fun test_mapToRealPosition() {
        val dataSize = 5
        
        assertEquals(0, ListLooper.mapToRealPosition(0, dataSize))
        assertEquals(1, ListLooper.mapToRealPosition(1, dataSize))
        assertEquals(4, ListLooper.mapToRealPosition(4, dataSize))
        assertEquals(0, ListLooper.mapToRealPosition(5, dataSize))
        assertEquals(2, ListLooper.mapToRealPosition(7, dataSize))
        assertEquals(-1, ListLooper.mapToRealPosition(10, 0))
    }

    @Test
    fun test_getInfiniteCount() {
        assertEquals(0, ListLooper.getInfiniteCount(0))
        assertEquals(0, ListLooper.getInfiniteCount(-1))
        assertEquals(Int.MAX_VALUE, ListLooper.getInfiniteCount(1))
        assertEquals(Int.MAX_VALUE, ListLooper.getInfiniteCount(100))
    }

    @Test
    fun test_calculateStartVirtualPosition() {
        val dataSize = 10
        val defaultPosition = ListLooper.calculateStartVirtualPosition(dataSize)
        
        // Check if it's aligned to dataSize
        assertEquals(0, defaultPosition % dataSize)
        
        // Check if it's near Int.MAX_VALUE / 2
        val middle = Int.MAX_VALUE / 2
        assertTrue(abs(defaultPosition - middle) < dataSize)
    }

    @Test
    fun test_calculateStartVirtualPosition_withInitialRealPosition() {
        val dataSize = 10
        
        val position1 = ListLooper.calculateStartVirtualPosition(dataSize, 3)
        assertEquals(3, position1 % dataSize)
        
        val position2 = ListLooper.calculateStartVirtualPosition(dataSize, 7)
        assertEquals(7, position2 % dataSize)
    }

    @Test
    fun test_convertToVirtualPosition() {
        val dataSize = 10
        val currentVirtualPosition = 1073741820 // Int.MAX_VALUE / 2 aligned to dataSize
        val currentCycle = currentVirtualPosition / dataSize
        
        val realPosition = 5
        val expectedVirtual = currentCycle * dataSize + realPosition
        val actualVirtual = ListLooper.convertToVirtualPosition(realPosition, currentVirtualPosition, dataSize)
        
        assertEquals(expectedVirtual, actualVirtual)
    }

    @Test
    fun test_calculateSmoothLoopPosition() {
        val dataSize = 10
        val threshold = dataSize / 2 // = 5
        
        // Forward direction, middle position (real position 5), no jump
        val currentPos1 = 105 // 105 % 10 = 5
        val targetPos1 = ListLooper.calculateSmoothLoopPosition(currentPos1, dataSize, 1)
        assertEquals(currentPos1, targetPos1) // Should NOT jump (5 is not > 5)
        
        // Forward direction, near end (real position 8), should jump
        val currentPos2 = 108 // 108 % 10 = 8, 8 > 5!
        val targetPos2 = ListLooper.calculateSmoothLoopPosition(currentPos2, dataSize, 1)
        assertEquals(110, targetPos2) // Should jump to next cycle start: (108/10 + 1) * 10 = 110
        
        // Backward direction, middle position, no jump
        val currentPos3 = 106 // 106 % 10 = 6
        val targetPos3 = ListLooper.calculateSmoothLoopPosition(currentPos3, dataSize, -1)
        assertEquals(currentPos3, targetPos3) // Should NOT jump (6 is not < 5)
        
        // Backward direction, near beginning (real position 1), should jump
        // Use 1011 instead of 101: 1011 represents 101st cycle with position 1
        val currentPos4 = 1011 // 1011 % 10 = 1, 1 < 5!
        val targetPos4 = ListLooper.calculateSmoothLoopPosition(currentPos4, dataSize, -1)
        assertEquals(1009, targetPos4) // Should jump: (1011/10 - 1) * 10 + 9 = 1009
        
        // Edge case: exactly at threshold
        val currentPos5 = 110 // 110 % 10 = 0, 0 < 5!
        val targetPos5 = ListLooper.calculateSmoothLoopPosition(currentPos5, dataSize, -1)
        assertEquals(109, targetPos5) // Should jump: (110/10 - 1) * 10 + 9 = 109
    }

    @Test
    fun test_needsLoopReset() {
        val dataSize = 10
        
        assertTrue(ListLooper.needsLoopReset(10, dataSize)) // Second cycle start
        assertTrue(ListLooper.needsLoopReset(19, dataSize)) // Second cycle end
        assertTrue(ListLooper.needsLoopReset(Int.MAX_VALUE - 5, dataSize))
        assertFalse(ListLooper.needsLoopReset(1000, dataSize)) // Middle cycle
    }

    companion object {
        fun abs(value: Int): Int = if (value < 0) -value else value
    }
}