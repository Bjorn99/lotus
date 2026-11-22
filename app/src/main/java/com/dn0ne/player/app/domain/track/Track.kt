package com.dn0ne.player.app.domain.track

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.media3.common.MediaItem
import kotlinx.serialization.Serializable


@Stable
@Serializable(with = TrackSerializer::class)
data class Track(
    val uri: Uri,
    val mediaItem: MediaItem,
    val coverArtUri: Uri,
    val duration: Int,
    val size: Long,
    val dateModified: Long,
    val data: String,

    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: String? = null,
    val trackNumber: String? = null,
    val bitrate: String? = null
)