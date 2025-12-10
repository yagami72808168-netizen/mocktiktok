package com.light.mimictiktok.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        preferencesManager = PreferencesManager(context)
    }

    @Test
    fun test_getDefaultVirtualPosition_withData() {
        val dataSize = 10
        val defaultPosition = preferencesManager.getDefaultVirtualPosition(dataSize)
        
        // Verify it's aligned to dataSize
        assertEquals(0, defaultPosition % dataSize)
        
        // Verify it's reasonably centered in Int range
        val middle = Int.MAX_VALUE / 2
        assertTrue(kotlin.math.abs(defaultPosition - middle) < dataSize)
    }

    @Test
    fun test_getDefaultVirtualPosition_emptyData() {
        val defaultPosition = preferencesManager.getDefaultVirtualPosition(0)
        assertEquals(Int.MAX_VALUE / 2, defaultPosition)
    }

    @Test
    fun test_initialPositionFlow() = runTest {
        // Initial value should be default or stored value
        val initialPosition = preferencesManager.initialPosition.first()
        assertNotNull(initialPosition)
        assertTrue(initialPosition > 0)
    }

    @Test
    fun test_setAndGetInitialPosition() = runTest {
        val testPosition = Int.MAX_VALUE / 2 + 5
        
        preferencesManager.setInitialPosition(testPosition)
        
        val retrievedPosition = preferencesManager.initialPosition.first()
        assertEquals(testPosition, retrievedPosition)
    }

    @Test
    fun test_setInitialPosition_multipleTimes() = runTest {
        val position1 = Int.MAX_VALUE / 2 + 10
        val position2 = Int.MAX_VALUE / 2 + 20
        
        preferencesManager.setInitialPosition(position1)
        assertEquals(position1, preferencesManager.initialPosition.first())
        
        preferencesManager.setInitialPosition(position2)
        assertEquals(position2, preferencesManager.initialPosition.first())
    }
}