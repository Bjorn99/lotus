package com.dn0ne.player.app.domain.metadata

import androidx.compose.runtime.Stable

@Stable
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

data class ReleaseMetadata(
    val title: String,
    val artist: String,
    val date: String?,
    val genres: List<String>?,
    val coverArtArchiveFront: Boolean,
    val tracks: List<ReleaseTrack>,
)

data class ReleaseTrack(
    val recordingId: String,
    val title: String,
    val trackNumber: String?,
)
