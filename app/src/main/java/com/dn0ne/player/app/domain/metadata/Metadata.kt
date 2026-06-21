package com.dn0ne.player.app.domain.metadata

data class Metadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: String? = null,
    val trackNumber: String? = null,
    val coverArtBytes: ByteArray? = null,
    val lyrics: String? = null,
    val mbAlbumId: String? = null,
    val mbReleaseGroupId: String? = null,
    val mbAlbumArtistId: String? = null,
)
