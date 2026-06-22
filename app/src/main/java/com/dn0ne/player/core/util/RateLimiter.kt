package com.dn0ne.player.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RateLimiter(
    capacity: Int = 5,
    private val refillPerSecond: Int = 1,
) {
    private val tokens = Channel<Unit>(capacity = capacity)

    init {
        repeat(capacity) { tokens.trySend(Unit) }
    }

    fun start(scope: CoroutineScope) {
        val intervalMs = 1000L / refillPerSecond
        scope.launch {
            while (true) {
                delay(intervalMs)
                tokens.trySend(Unit)
            }
        }
    }

    suspend fun acquire() {
        tokens.receive()
    }
}
