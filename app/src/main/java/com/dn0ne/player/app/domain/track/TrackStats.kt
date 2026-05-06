package com.dn0ne.player.app.domain.track

// Plain domain model the repository hands back. Mirrors the columns of the
// Room entity but lives outside the data layer so the rest of the app
// doesn't import androidx.room types.
data class TrackStats(
    val uri: String,
    val playCount: Int,
    val skipCount: Int,
    val firstPlayedAt: Long?,
    val lastPlayedAt: Long?,
    val totalListeningMs: Long,
)
