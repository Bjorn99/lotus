package com.dn0ne.player.app.domain.track

import android.net.Uri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * Wire format for a persisted [Track].
 *
 * **The element names below are the on-disk format and must not be changed.**
 * They do not describe what is stored at that index. The names were fixed
 * before the encoding order settled, and the two have never matched:
 *
 * | index | element name   | what is actually stored |
 * |-------|----------------|-------------------------|
 * | 0     | `uri`          | uri                     |
 * | 1     | `title`        | **coverArtUri**         |
 * | 2     | `artist`       | **duration**            |
 * | 3     | `coverArtUri`  | **size**                |
 * | 4     | `duration`     | **dateModified**        |
 * | 5     | `album`        | **data**                |
 * | 6     | `albumArtist`  | **title**               |
 * | 7     | `genre`        | **album**               |
 * | 8     | `year`         | **artist**              |
 * | 9     | `discNumber`   | **albumArtist**         |
 * | 10    | `trackNumber`  | **genre**               |
 * | 11    | `bitrate`      | **year**                |
 * | 12    | `size`         | **trackNumber**         |
 * | 13    | `dateModified` | **bitrate**             |
 *
 * `discNumber` is not a [Track] field at all, and there is no element named
 * `data`. The declared types lie too: `element<Int>("duration")` is written
 * with `encodeLongElement`, and `element<String>("coverArtUri")` with a Long.
 *
 * None of this is broken, because [serialize] and [deserialize] agree on the
 * *indices* and JSON never consults the declared types. It round-trips
 * correctly and always has.
 *
 * It is a trap, though. Renaming these to match what they hold silently
 * repoints every key in every saved state: a real file has `"title"` holding an
 * albumart URI, `"size"` holding a track number and `"dateModified"` holding a
 * bitrate. After a rename those decode into the wrong fields or fail outright,
 * and [com.dn0ne.player.app.data.SavedPlayerState] would quietly drop the
 * playlist and resume point for every user on the release that did it.
 *
 * `TrackSerializerDescriptorTest` pins the names so that cannot happen by
 * accident. If you genuinely want honest names, that is a deliberate format
 * migration with a user-visible cost, not a tidy-up.
 */
object TrackSerializer : KSerializer<Track> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("Track") {
            element<String>("uri")
            element<String>("title")
            element<String>("artist")
            element<String>("coverArtUri")
            element<Int>("duration")
            element<String>("album")
            element<String>("albumArtist")
            element<String>("genre")
            element<String>("year")
            element<String>("discNumber")
            element<String>("trackNumber")
            element<String>("bitrate")
            element<Long>("size")
            element<String>("dateModified")
        }

    override fun serialize(
        encoder: Encoder,
        value: Track
    ) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.uri.toString())
            encodeStringElement(descriptor, 1, value.coverArtUri.toString())
            encodeIntElement(descriptor, 2, value.duration)
            encodeLongElement(descriptor, 3, value.size)
            encodeLongElement(descriptor, 4, value.dateModified)
            encodeStringElement(descriptor, 5, value.data)

            encodeStringElement(descriptor, 6, value.title ?: "null")
            encodeStringElement(descriptor, 7, value.album ?: "null")
            encodeStringElement(descriptor, 8, value.artist ?: "null")
            encodeStringElement(descriptor, 9, value.albumArtist ?: "null")
            encodeStringElement(descriptor, 10, value.genre ?: "null")
            encodeStringElement(descriptor, 11, value.year ?: "null")
            encodeStringElement(descriptor, 12, value.trackNumber ?: "null")
            encodeStringElement(descriptor, 13, value.bitrate ?: "null")
        }
    }

    override fun deserialize(decoder: Decoder): Track =
        decoder.decodeStructure(descriptor) {
            var uriString = ""
            var coverArtUriString = ""
            var duration = -1
            var size = -1L
            var dateModified = -1L
            var data = ""

            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var albumArtist: String? = null
            var genre: String? = null
            var year: String? = null
            var trackNumber: String? = null
            var bitrate: String? = null

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> uriString = decodeStringElement(descriptor, 0)
                    1 -> coverArtUriString = decodeStringElement(descriptor, 1)
                    2 -> duration = decodeIntElement(descriptor, 2)
                    3 -> size = decodeLongElement(descriptor, 3)
                    4 -> dateModified = decodeLongElement(descriptor, 4)
                    5 -> data = decodeStringElement(descriptor, 5)

                    6 -> title = decodeStringElement(descriptor, 6).takeIf { it != "null" }
                    // These two read their own index. They used to read each
                    // other's (7 read 8, 8 read 7). Harmless in JSON, where the
                    // index is not what selects the value, but wrong.
                    7 -> album = decodeStringElement(descriptor, 7).takeIf { it != "null" }
                    8 -> artist = decodeStringElement(descriptor, 8).takeIf { it != "null" }
                    9 -> albumArtist = decodeStringElement(descriptor, 9).takeIf { it != "null" }
                    10 -> genre = decodeStringElement(descriptor, 10).takeIf { it != "null" }
                    11 -> year = decodeStringElement(descriptor, 11).takeIf { it != "null" }
                    12 -> trackNumber = decodeStringElement(descriptor, 12).takeIf { it != "null" }
                    13 -> bitrate = decodeStringElement(descriptor, 13).takeIf { it != "null" }
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            require(uriString.isNotBlank() && coverArtUriString.isNotBlank() && duration >= 0 && size >= 0)

            val uri = Uri.parse(uriString)
            val coverArtUri = Uri.parse(coverArtUriString)
            // Rebuild the MediaItem with full metadata so the system media
            // notification has a title / artist / artwork to show after a
            // process restart, not just "Lotus is playing".
            val mediaItem = buildMediaItem(
                uri = uri,
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                genre = genre,
                year = year,
                trackNumber = trackNumber,
                coverArtUri = coverArtUri,
            )
            Track(
                uri = uri,
                mediaItem = mediaItem,
                coverArtUri = coverArtUri,
                duration = duration,
                size = size,
                dateModified = dateModified,
                data = data,

                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                genre = genre,
                year = year,
                trackNumber = trackNumber,
                bitrate = bitrate
            )
        }
}