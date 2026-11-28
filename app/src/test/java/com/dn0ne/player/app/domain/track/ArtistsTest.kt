package com.dn0ne.player.app.domain.track

import android.net.Uri
import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class ArtistsTest {

    @Test
    fun `comma and semicolon separated artists are split and trimmed`() {
        assertEquals(
            listOf("David Guetta", "Post Malone", "Sia"),
            "David Guetta, Post Malone ;Sia".splitArtists()
        )
    }

    @Test
    fun `blank entries and duplicates are dropped`() {
        assertEquals(listOf("Sia"), "Sia, , Sia,".splitArtists())
    }

    @Test
    fun `a multi-artist track lands in every artist's group`() {
        val collab = track(artist = "David Guetta, Post Malone")
        val solo = track(artist = "Post Malone")

        val groups = listOf(collab, solo).groupByArtist()

        assertEquals(listOf(collab), groups["David Guetta"])
        assertEquals(listOf(collab, solo), groups["Post Malone"])
        assertEquals(setOf("David Guetta", "Post Malone"), groups.keys)
    }

    @Test
    fun `album artist takes precedence over track artist`() {
        val t = track(artist = "Guest", albumArtist = "Main, Other")

        assertEquals(setOf("Main", "Other"), listOf(t).groupByArtist().keys)
    }

    @Test
    fun `tracks without an artist stay grouped under null`() {
        val t = track(artist = null)

        assertEquals(mapOf(null to listOf(t)), listOf(t).groupByArtist())
    }

    private val uri: Uri = mock()
    private val mediaItem: MediaItem = mock()

    private fun track(artist: String?, albumArtist: String? = null) = Track(
        uri = uri,
        mediaItem = mediaItem,
        coverArtUri = uri,
        duration = 0,
        size = 0,
        dateModified = 0,
        data = "",
        artist = artist,
        albumArtist = albumArtist,
    )
}
