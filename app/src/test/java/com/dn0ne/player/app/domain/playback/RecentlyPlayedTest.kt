package com.dn0ne.player.app.domain.playback

import org.junit.Assert.*
import org.junit.Test

class RecentlyPlayedTest {

    @Test
    fun `no previous order means nothing is recent`() {
        assertEquals(emptySet<Int>(), recentlyPlayedIndices(null))
    }

    @Test
    fun `an empty or single-track order has no meaningful tail`() {
        assertEquals(emptySet<Int>(), recentlyPlayedIndices(IntArray(0)))
        assertEquals(emptySet<Int>(), recentlyPlayedIndices(intArrayOf(7)))
    }

    @Test
    fun `takes the last quarter of the previous order`() {
        val previous = IntArray(40) { it }
        val recent = recentlyPlayedIndices(previous)

        assertEquals(10, recent.size)
        assertEquals((30 until 40).toSet(), recent)
    }

    @Test
    fun `reads positions not track numbers`() {
        // The tail of the ORDER, whatever track indices happen to sit there.
        val previous = intArrayOf(9, 4, 7, 1, 8, 3, 0, 5)
        assertEquals(setOf(0, 5), recentlyPlayedIndices(previous))
    }

    @Test
    fun `short orders still mark at least one track recent`() {
        assertEquals(setOf(9), recentlyPlayedIndices(intArrayOf(3, 9)))
        assertEquals(setOf(1), recentlyPlayedIndices(intArrayOf(5, 2, 1)))
    }

    @Test
    fun `never marks the whole queue recent`() {
        // The defect this function exists to prevent: a penalty that applies to
        // every track adds a constant and cannot rank one order above another.
        for (size in 2..200) {
            val recent = recentlyPlayedIndices(IntArray(size) { it })
            assertTrue("size $size marked all $size tracks recent", recent.size < size)
        }
    }
}
