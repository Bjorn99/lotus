package com.dn0ne.player.app.presentation

import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AlbumSearchQueryTest {

    private fun track(
        album: String? = null,
        albumArtist: String? = null,
        artist: String? = null,
    ): Track = mock<Track>().also {
        `when`(it.album).thenReturn(album)
        `when`(it.albumArtist).thenReturn(albumArtist)
        `when`(it.artist).thenReturn(artist)
    }

    private fun playlist(name: String, tracks: List<Track>) = Playlist(
        name = name,
        trackList = tracks,
    )

    @Test
    fun `normal album with consistent albumArtist`() {
        val tracks = listOf(
            track(album = "Abbey Road", albumArtist = "The Beatles"),
            track(album = "Abbey Road", albumArtist = "The Beatles"),
        )
        val playlist = playlist("Abbey Road", tracks)
        val query = AlbumSearchQueryBuilder.build(playlist)
        assertEquals("release:\"Abbey Road\" AND artist:\"The Beatles\"", query?.query)
        assertEquals("The Beatles", query?.consensusArtist)
        assertEquals("Abbey Road", query?.albumName)
        assertEquals(false, query?.isVA)
    }

    @Test
    fun `Various Artists detected via albumArtist`() {
        val tracks = listOf(
            track(album = "Greatest Hits", albumArtist = "Various Artists"),
        )
        val playlist = playlist("Greatest Hits", tracks)
        val query = AlbumSearchQueryBuilder.build(playlist)
        assertEquals("release:\"Greatest Hits\"", query?.query)
        assertEquals(true, query?.isVA)
    }

    @Test
    fun `VA lowercase variant`() {
        val tracks = listOf(
            track(album = "Best Of", albumArtist = "various artists"),
        )
        val playlist = playlist("Best Of", tracks)
        val query = AlbumSearchQueryBuilder.build(playlist)
        assertEquals(true, query?.isVA)
    }

    @Test
    fun `falls back to track artist when albumArtist is blank`() {
        val tracks = listOf(
            track(album = "OK Computer", albumArtist = null, artist = "Radiohead"),
            track(album = "OK Computer", albumArtist = "", artist = "Radiohead"),
        )
        val playlist = playlist("OK Computer", tracks)
        val query = AlbumSearchQueryBuilder.build(playlist)
        assertEquals("release:\"OK Computer\" AND artist:\"Radiohead\"", query?.query)
    }

    @Test
    fun `consensus artist is mode not first`() {
        val tracks = listOf(
            track(album = "Collab", albumArtist = "Radiohead"),
            track(album = "Collab", albumArtist = "Radiohead"),
            track(album = "Collab", albumArtist = "Bjork"),
        )
        val playlist = playlist("Collab", tracks)
        val query = AlbumSearchQueryBuilder.build(playlist)
        assertEquals("Radiohead", query?.consensusArtist)
    }

    @Test
    fun `all tracks have null album returns null`() {
        val tracks = listOf(
            track(album = null, albumArtist = "Someone"),
        )
        val playlist = playlist("Unknown", tracks)
        val query = AlbumSearchQueryBuilder.build(playlist)
        assertNull(query)
    }

    @Test
    fun `VA detection variants`() {
        val vaPatterns = listOf("Various Artists", "various", "va", "v/a", "Various Artist")
        for (pattern in vaPatterns) {
            assertTrue(
                "Should detect '$pattern' as VA",
                AlbumSearchQueryBuilder.isVariousArtists(pattern)
            )
        }
    }
}
