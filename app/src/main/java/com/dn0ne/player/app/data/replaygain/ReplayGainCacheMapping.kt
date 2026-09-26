package com.dn0ne.player.app.data.replaygain

import com.dn0ne.player.app.data.db.ReplayGainCacheEntity
import com.dn0ne.player.app.domain.replaygain.GainPeak
import com.dn0ne.player.app.domain.replaygain.ReplayGainTags
import com.dn0ne.player.app.domain.replaygain.TagState

/** What MediaStore says about a file: enough to name the temp copy and to spot a retag. */
data class MediaFileInfo(val dateModified: Long, val size: Long, val extension: String)

// Retagging (e.g. running rsgain) rewrites the file, so MediaStore reports a new mtime
// and size after its rescan and the row refreshes by itself.
fun ReplayGainCacheEntity.isFreshFor(info: MediaFileInfo): Boolean =
    dateModified == info.dateModified && size == info.size

fun ReplayGainCacheEntity.toTagState(): TagState {
    val track = trackGainDb?.let { GainPeak(it, trackPeak) }
    val album = albumGainDb?.let { GainPeak(it, albumPeak) }
    return if (track == null && album == null) {
        TagState.Untagged
    } else {
        TagState.Tagged(ReplayGainTags(track, album))
    }
}

fun TagState.toCacheEntity(uri: String, info: MediaFileInfo): ReplayGainCacheEntity? =
    when (this) {
        TagState.Unknown -> null
        TagState.Untagged -> ReplayGainCacheEntity(uri, info.dateModified, info.size)
        is TagState.Tagged -> ReplayGainCacheEntity(
            uri = uri,
            dateModified = info.dateModified,
            size = info.size,
            trackGainDb = tags.track?.gainDb,
            trackPeak = tags.track?.peak,
            albumGainDb = tags.album?.gainDb,
            albumPeak = tags.album?.peak,
        )
    }
