package com.dn0ne.player.app.domain.track

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Build a Media3 [MediaItem] with full [MediaMetadata] populated.
 *
 * Without this metadata, the system media notification falls back to the
 * app name ("Lotus is playing") because there's no track title to show.
 * This helper is the single source of truth for Track → MediaItem
 * conversion, used both at scan time ([TrackRepositoryImpl]) and when
 * deserialising saved player state ([TrackSerializer]).
 *
 * `displayTitle` and `subtitle` are set explicitly so the notification's
 * primary line and secondary line are stable across Android versions —
 * different OEMs prefer different fields, setting both is the safe move.
 *
 * `artworkUri` points at the MediaStore album-art URI; the system loads
 * it into the notification + lockscreen + Android Auto / Wear without us
 * decoding the bitmap ourselves.
 */
fun buildMediaItem(
    uri: Uri,
    title: String?,
    artist: String?,
    album: String?,
    albumArtist: String?,
    genre: String?,
    year: String?,
    trackNumber: String?,
    coverArtUri: Uri?,
): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setAlbumArtist(albumArtist)
        .setGenre(genre)
        .setReleaseYear(year?.toIntOrNull())
        .setTrackNumber(trackNumber?.toIntOrNull())
        .setArtworkUri(coverArtUri)
        .setDisplayTitle(title)
        .setSubtitle(artist)
        .build()

    return MediaItem.Builder()
        .setUri(uri)
        // Stable id so the MediaSession can correlate "this is the same
        // track" across queue reorders and process restarts.
        .setMediaId(uri.toString())
        .setMediaMetadata(metadata)
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(uri)
                .build()
        )
        .build()
}
