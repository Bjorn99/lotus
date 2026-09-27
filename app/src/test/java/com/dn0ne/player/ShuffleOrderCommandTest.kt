package com.dn0ne.player

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShuffleOrderCommandTest {

    @Test
    fun `a permutation of the queue is accepted`() {
        val order = intArrayOf(2, 0, 3, 1)
        assertArrayEquals(order, validShuffleOrder(order, itemCount = 4))
    }

    @Test
    fun `a length that does not match the queue is rejected`() {
        assertNull(validShuffleOrder(intArrayOf(1, 0), itemCount = 3))
        assertNull(validShuffleOrder(intArrayOf(1, 0, 2, 3), itemCount = 3))
    }

    @Test
    fun `duplicates and out-of-range indices are rejected`() {
        assertNull(validShuffleOrder(intArrayOf(0, 0, 2), itemCount = 3))
        assertNull(validShuffleOrder(intArrayOf(0, 1, 3), itemCount = 3))
        assertNull(validShuffleOrder(intArrayOf(-1, 0, 1), itemCount = 3))
    }

    @Test
    fun `a missing order is rejected`() {
        assertNull(validShuffleOrder(null, itemCount = 3))
    }

    @Test
    fun `an empty queue takes an empty order`() {
        assertArrayEquals(intArrayOf(), validShuffleOrder(intArrayOf(), itemCount = 0))
    }

    @Test
    fun `an order for the current queue is applied`() {
        assertEquals(ShuffleOrderAction.APPLY, shuffleOrderAction(intArrayOf(1, 0, 2), itemCount = 3))
    }

    @Test
    fun `a well-formed order that arrives before its queue is deferred`() {
        assertEquals(ShuffleOrderAction.DEFER, shuffleOrderAction(intArrayOf(1, 0, 2), itemCount = 0))
        assertEquals(ShuffleOrderAction.DEFER, shuffleOrderAction(intArrayOf(1, 0, 2), itemCount = 5))
    }

    @Test
    fun `a malformed order is rejected whatever the queue`() {
        assertEquals(ShuffleOrderAction.REJECT, shuffleOrderAction(intArrayOf(0, 0, 2), itemCount = 3))
        assertEquals(ShuffleOrderAction.REJECT, shuffleOrderAction(intArrayOf(0, 5), itemCount = 0))
        assertEquals(ShuffleOrderAction.REJECT, shuffleOrderAction(null, itemCount = 3))
    }
}

