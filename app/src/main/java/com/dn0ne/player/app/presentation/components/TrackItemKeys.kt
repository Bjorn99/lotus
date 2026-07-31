package com.dn0ne.player.app.presentation.components

import com.dn0ne.player.app.domain.track.Track

/**
 * Separates a URI from its occurrence count. A null character cannot appear in a
 * URI, so a suffixed key can never collide with the key of a real track.
 */
private const val OCCURRENCE_SEPARATOR = "\u0000"

/**
 * Builds unique lazy-list keys for a list of track URIs.
 *
 * A playlist may legally hold the same track more than once - imported playlists
 * commonly do - but a lazy list requires unique item keys and throws as soon as
 * one repeats. Repeated URIs therefore get an occurrence suffix.
 *
 * The suffix counts how often a URI has already appeared rather than where it
 * sits, so reordering the list rearranges the keys without changing the set of
 * them. Item animations and drag-to-reorder rely on that.
 */
fun uniqueTrackKeys(uris: List<String>): List<String> {
    val occurrences = HashMap<String, Int>(uris.size)
    return uris.map { uri ->
        val seen = occurrences[uri] ?: 0
        occurrences[uri] = seen + 1
        if (seen == 0) uri else "$uri$OCCURRENCE_SEPARATOR$seen"
    }
}

fun List<Track>.trackItemKeys(): List<String> = uniqueTrackKeys(map { it.uri.toString() })
