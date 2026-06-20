package com.dn0ne.player.app.presentation

import com.dn0ne.player.app.domain.track.Playlist

internal data class AlbumSearchQuery(
    val query: String,
    val albumName: String,
    val consensusArtist: String,
    val isVA: Boolean,
)

internal object AlbumSearchQueryBuilder {

    fun build(playlist: Playlist): AlbumSearchQuery? {
        val albumNames = playlist.trackList.mapNotNull { it.album }
        val albumArtists = playlist.trackList.mapNotNull {
            it.albumArtist?.takeIf { a -> a.isNotBlank() } ?: it.artist
        }

        val albumName = albumNames.groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: return null
        val consensusArtist = albumArtists.groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: ""

        val isVA = isVariousArtists(consensusArtist)

        var query = "release:\"$albumName\""
        if (!isVA && consensusArtist.isNotBlank()) {
            query += " AND artist:\"$consensusArtist\""
        }
        return AlbumSearchQuery(query, albumName, consensusArtist, isVA)
    }

    fun isVariousArtists(artist: String): Boolean {
        val lowered = artist.lowercase().trim()
        return lowered == "various artists" || lowered == "various"
            || lowered == "va" || lowered == "v/a" || lowered == "various artist"
    }
}
