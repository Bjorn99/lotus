package com.dn0ne.player.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {

    @Test
    fun `initial capacity allows burst of acquires without blocking`() {
        val scope = CoroutineScope(Job())
        val limiter = RateLimiter(capacity = 5, refillPerSecond = 1)
        limiter.start(scope)

        runBlocking {
            val start = System.currentTimeMillis()
            repeat(5) { limiter.acquire() }
            val elapsed = System.currentTimeMillis() - start

            assertTrue("Five acquires should complete within 100ms", elapsed < 100)
        }
        scope.cancel()
    }

    @Test
    fun `acquire blocks when bucket is empty`() {
        val scope = CoroutineScope(Job())
        val limiter = RateLimiter(capacity = 1, refillPerSecond = 1)
        limiter.start(scope)

        runBlocking {
            limiter.acquire() // drain the only token

            val result = withTimeoutOrNull(500L) {
                limiter.acquire()
                "acquired"
            }

            assertNull("Second acquire should time out before refill", result)
        }
        scope.cancel()
    }

    @Test
    fun `token refills after delay`() {
        val scope = CoroutineScope(Job())
        val limiter = RateLimiter(capacity = 1, refillPerSecond = 2) // refill every 500ms
        limiter.start(scope)

        runBlocking {
            limiter.acquire() // drain the only token

            val result = withTimeoutOrNull(1000L) {
                limiter.acquire()
                "acquired"
            }

            assertEquals("Token should refill after 500ms", "acquired", result)
        }
        scope.cancel()
    }

    @Test
    fun `refill does not exceed capacity`() {
        val scope = CoroutineScope(Job())
        val limiter = RateLimiter(capacity = 3, refillPerSecond = 1)
        limiter.start(scope)

        runBlocking {
            // With refill 1/sec, only the initial 3 tokens should be available
            val start = System.currentTimeMillis()
            repeat(3) { limiter.acquire() }
            val elapsed = System.currentTimeMillis() - start

            assertTrue("Three acquires from initial capacity should complete within 100ms", elapsed < 100)

            // Fourth acquire should block (bucket empty, refill is 1/sec)
            val blocked = withTimeoutOrNull(300L) {
                limiter.acquire()
            }
            assertNull("Fourth acquire should block (bucket empty)", blocked)
        }
        scope.cancel()
    }

    @Test
    fun `concurrent acquires share tokens correctly`() {
        val scope = CoroutineScope(Job())
        val limiter = RateLimiter(capacity = 2, refillPerSecond = 1)
        limiter.start(scope)

        runBlocking {
            val acquired = mutableListOf<String>()

            val job1 = launch { limiter.acquire(); acquired.add("a") }
            val job2 = launch { limiter.acquire(); acquired.add("b") }
            job1.join()
            job2.join()

            assertEquals(2, acquired.size)

            // Third acquire should block — refill hasn't fired yet
            val blocked = withTimeoutOrNull(300L) {
                limiter.acquire()
            }
            assertNull("Third acquire should block when bucket is empty", blocked)
        }
        scope.cancel()
    }
}
