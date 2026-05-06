package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.domain.track.TrackStats

// Pure merge function for backup imports. Kept out of the repository so it
// can be unit-tested without Room.
//
// Rules:
//   - playCount, skipCount, totalListeningMs, lastPlayedAt → max(local, incoming)
//   - firstPlayedAt → min(local, incoming) — earliest known first-play wins
// Re-importing the same backup is a no-op. Importing an older backup never
// erases newer activity.
internal fun mergeStats(local: TrackStats?, incoming: TrackStats): TrackStats {
    if (local == null) return incoming
    return TrackStats(
        uri = incoming.uri,
        playCount = maxOf(local.playCount, incoming.playCount),
        skipCount = maxOf(local.skipCount, incoming.skipCount),
        firstPlayedAt = minNonNull(local.firstPlayedAt, incoming.firstPlayedAt),
        lastPlayedAt = maxNonNull(local.lastPlayedAt, incoming.lastPlayedAt),
        totalListeningMs = maxOf(local.totalListeningMs, incoming.totalListeningMs),
    )
}

// Treat null as "unknown" — defer to whichever side has a value.
private fun minNonNull(a: Long?, b: Long?): Long? = when {
    a == null -> b
    b == null -> a
    else -> minOf(a, b)
}

private fun maxNonNull(a: Long?, b: Long?): Long? = when {
    a == null -> b
    b == null -> a
    else -> maxOf(a, b)
}
