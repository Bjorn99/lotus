package com.dn0ne.player.app.domain.track

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the persisted wire format of a [Track].
 *
 * The element names in `TrackSerializer` do not describe what is stored at that
 * index — `"title"` holds the cover art URI, `"size"` holds the track number,
 * `"dateModified"` holds the bitrate, and so on. That is ugly but stable, and
 * every saved player state on every device is written in it.
 *
 * Renaming them to match what they hold would silently repoint every key in
 * every saved file. `SavedPlayerState` catches the resulting decode failure and
 * clears the key, so nobody crashes — they just lose their playlist and resume
 * point, quietly, on whichever release did it.
 *
 * So this test exists to make that impossible to do by accident. If it fails,
 * you are changing the on-disk format. That is a deliberate migration with a
 * user-visible cost, not a tidy-up, and the changelog has to say so.
 *
 * Deliberately asserts names and order only. Nothing here touches `android.*`,
 * so it runs as a plain JVM test — unlike the encode/decode paths, which need
 * `Uri` and `MediaItem`.
 */
class TrackSerializerDescriptorTest {

    private val expected = listOf(
        "uri",          // 0  -> uri
        "title",        // 1  -> coverArtUri
        "artist",       // 2  -> duration
        "coverArtUri",  // 3  -> size
        "duration",     // 4  -> dateModified
        "album",        // 5  -> data
        "albumArtist",  // 6  -> title
        "genre",        // 7  -> album
        "year",         // 8  -> artist
        "discNumber",   // 9  -> albumArtist
        "trackNumber",  // 10 -> genre
        "bitrate",      // 11 -> year
        "size",         // 12 -> trackNumber
        "dateModified", // 13 -> bitrate
    )

    @Test
    fun `element names and their order are the on-disk format and must not change`() {
        val actual = (0 until TrackSerializer.descriptor.elementsCount)
            .map { TrackSerializer.descriptor.getElementName(it) }
        assertEquals(expected, actual)
    }

    @Test
    fun `the format has exactly fourteen elements`() {
        assertEquals(14, TrackSerializer.descriptor.elementsCount)
    }

    @Test
    fun `serial name is stable`() {
        assertEquals("Track", TrackSerializer.descriptor.serialName)
    }
}
