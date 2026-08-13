package com.dn0ne.player.app.domain.metadata

data class MetadataSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val albumId: String,
    val album: String,
    val albumArtist: String,
    val trackNumber: String? = null,
    val description: String? = null,
    val albumDescription: String? = null,
    val year: String? = null,
    val genres: List<String>? = null,
    // MusicBrainz release-group id — the "album" as a concept, independent of
    // which pressing you happen to hold. Used to collapse the many pressings
    // of one album into a single row, and to fall back to the group's cover
    // art when this particular release has none. Null when the provider
    // didn't supply one.
    val releaseGroupId: String? = null,
    // MBID of the release's first credited artist, carried so the pick path
    // can persist it alongside the other MusicBrainz ids.
    val albumArtistId: String? = null,
)
