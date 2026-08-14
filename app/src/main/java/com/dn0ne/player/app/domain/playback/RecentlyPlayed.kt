package com.dn0ne.player.app.domain.playback

private const val RECENT_TAIL_DIVISOR = 4

/**
 * The tracks at the tail of a finished shuffle order — the ones heard most
 * recently, which a new order should avoid opening with.
 *
 * Passing the whole previous order instead would name every track in the queue,
 * and a penalty that applies to everything ranks nothing.
 */
fun recentlyPlayedIndices(previousOrder: IntArray?): Set<Int> {
    if (previousOrder == null || previousOrder.size < 2) return emptySet()

    val recentCount = (previousOrder.size / RECENT_TAIL_DIVISOR).coerceAtLeast(1)
    val recent = HashSet<Int>(recentCount * 2)
    for (position in previousOrder.size - recentCount until previousOrder.size) {
        recent.add(previousOrder[position])
    }
    return recent
}
