package com.dn0ne.player.app.domain.track

/**
 * Splits a multi-artist tag ("David Guetta, Post Malone") so each artist
 * gets their own artist playlist and stats instead of a combined entry.
 */
fun String.splitArtists(): List<String> =
    split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

fun List<Track>.groupByArtist(): Map<String?, List<Track>> =
    flatMap { track ->
        val artistKey = track.albumArtist?.takeIf { it.isNotBlank() } ?: track.artist
        (artistKey?.splitArtists()?.takeIf { it.isNotEmpty() } ?: listOf(artistKey))
            .map { it to track }
    }.groupBy({ it.first }, { it.second })
