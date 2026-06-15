package com.dn0ne.player.app.domain.sort

import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track

enum class SortOrder {
    Ascending, Descending
}

enum class TrackSort {
    Title, Album, Artist, Genre, Year, TrackNumber, DateModified
}

fun List<Track>.sortedBy(sort: TrackSort, order: SortOrder): List<Track> {
    return when (order) {
        SortOrder.Ascending -> {
            when (sort) {
                TrackSort.Title -> sortedBy { it.title }
                TrackSort.Album -> sortedBy { it.album }
                TrackSort.Artist -> sortedBy { it.albumArtist?.takeIf { it.isNotBlank() } ?: it.artist }
                TrackSort.Genre -> sortedBy { it.genre?.take(10) }
                TrackSort.Year -> sortedBy { it.year }
                TrackSort.TrackNumber -> sortedBy {
                    if (it.trackNumber?.any { it.isLetter() } == true) {
                        it.trackNumber.map { it.code }.joinToString("").toIntOrNull()
                    } else it.trackNumber?.toIntOrNull()
                }
                TrackSort.DateModified -> sortedBy { it.dateModified }
            }
        }

        SortOrder.Descending -> {
            when (sort) {
                TrackSort.Title -> sortedByDescending { it.title }
                TrackSort.Album -> sortedByDescending { it.album }
                TrackSort.Artist -> sortedByDescending { it.albumArtist?.takeIf { it.isNotBlank() } ?: it.artist }
                TrackSort.Genre -> sortedByDescending { it.genre?.take(10) }
                TrackSort.Year -> sortedByDescending { it.year }
                TrackSort.TrackNumber -> sortedByDescending {
                    if (it.trackNumber?.any { it.isLetter() } == true) {
                        it.trackNumber.map { it.code }.joinToString("").toIntOrNull()
                    } else it.trackNumber?.toIntOrNull()
                }
                TrackSort.DateModified -> sortedByDescending { it.dateModified }
            }
        }
    }
}

enum class PlaylistSort {
    Title, TrackCount, Artist, Year
}

fun List<Playlist>.sortedBy(
    sort: PlaylistSort,
    order: SortOrder
): List<Playlist> {
    val artistOf: (Playlist) -> String = { p ->
        p.trackList.firstOrNull()?.albumArtist?.takeIf { it.isNotBlank() }
            ?: (p.trackList.firstOrNull()?.artist ?: "")
    }
    val yearOf: (Playlist) -> String? = { p -> p.trackList.firstOrNull()?.year }

    return when(order) {
        SortOrder.Ascending -> {
            when(sort) {
                PlaylistSort.Title -> sortedBy { it.name }
                PlaylistSort.Artist -> sortedBy(artistOf)
                PlaylistSort.Year -> sortedBy(yearOf)
                PlaylistSort.TrackCount -> sortedBy { it.trackList.size }
            }
        }
        SortOrder.Descending -> {
            when(sort) {
                PlaylistSort.Title -> sortedByDescending { it.name }
                PlaylistSort.Artist -> sortedByDescending(artistOf)
                PlaylistSort.Year -> sortedByDescending(yearOf)
                PlaylistSort.TrackCount -> sortedByDescending { it.trackList.size }
            }
        }
    }
}