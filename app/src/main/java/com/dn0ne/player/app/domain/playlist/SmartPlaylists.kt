package com.dn0ne.player.app.domain.playlist

import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import kotlin.random.Random

/**
 * Builders for synthetic "smart" playlists computed from the track library.
 *
 * Unlike album/artist/genre groupings, smart playlists don't correspond to a
 * tag field — they're rule-based views ("recently added", "random mix").
 *
 * Kept in the `domain` layer as pure functions so unit tests can exercise
 * them without touching Android types or resources. The UI layer is
 * responsible for the localised name — callers pass the already-resolved
 * strings in.
 */
object SmartPlaylists {

    // Window for "Recently Added", in seconds. Track.dateModified is epoch
    // seconds (MediaStore convention — see TrackInfoSheet's *1000 conversion).
    private const val RECENTLY_ADDED_WINDOW_SECONDS: Long = 30L * 24L * 60L * 60L

    // Cap the synthetic lists so the UI stays snappy even on large libraries.
    private const val MAX_ENTRIES = 100

    fun recentlyAdded(
        tracks: List<Track>,
        name: String,
        nowSeconds: Long = System.currentTimeMillis() / 1000L,
    ): Playlist? {
        val cutoff = nowSeconds - RECENTLY_ADDED_WINDOW_SECONDS
        val picked = tracks
            .asSequence()
            .filter { it.dateModified > cutoff }
            .sortedByDescending { it.dateModified }
            .take(MAX_ENTRIES)
            .toList()
        return if (picked.isEmpty()) null else Playlist(name = name, trackList = picked)
    }

    fun randomMix(
        tracks: List<Track>,
        name: String,
        seed: Long = Random.Default.nextLong(),
    ): Playlist? {
        if (tracks.isEmpty()) return null
        // Seeded so a given (tracks, seed) is deterministic — callers that
        // want a stable order across recompositions can pass a cached seed;
        // callers that want a fresh shuffle each tap pass a new one.
        val picked = tracks.shuffled(Random(seed)).take(MAX_ENTRIES)
        return Playlist(name = name, trackList = picked)
    }
}
