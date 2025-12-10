package com.light.mimictiktok.player

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PlayerPoolTest {

    private lateinit var context: Context
    private lateinit var playerPool: PlayerPool

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()
        playerPool = PlayerPool(context, poolSize = 2)
    }

    @Test
    fun testPlayerPoolCreation() {
        assertNotNull(playerPool)
        assertEquals(2, playerPool.getPoolSize())
    }

    @Test
    fun testPlayerAcquisition() {
        val player1 = playerPool.acquire(0)
        val player2 = playerPool.acquire(1)
        
        assertNotNull(player1)
        assertNotNull(player2)
        assertTrue(player1 is ExoPlayer)
        assertTrue(player2 is ExoPlayer)
    }

    @Test
    fun testPlayerPoolWrapping() {
        val player0 = playerPool.acquire(0)
        val player2 = playerPool.acquire(2)
        
        assertEquals(player0, player2)
    }

    @Test
    fun testPlayerRelease() {
        val player = playerPool.acquire(0)
        assertNotNull(player)
        
        playerPool.release(0)
    }

    @Test
    fun testReleaseAll() {
        playerPool.releaseAll()
        assertEquals(2, playerPool.getPoolSize())
    }
}
