package com.dn0ne.player.app.domain.track

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class AlbumVisibilityTest {

    @Test
    fun `disabled leaves the list untouched`() {
        val albums = listOf(album("one", 1), album("two", 2))

        assertEquals(albums, albums.withoutSingleTrackAlbums(enabled = false))
    }

    @Test
    fun `enabled drops single-track albums`() {
        val albums = listOf(album("single", 1), album("full", 5))

        val visible = albums.withoutSingleTrackAlbums(enabled = true)

        assertEquals(listOf("full"), visible.map { it.name })
    }

    @Test
    fun `an album of exactly two tracks is kept`() {
        val albums = listOf(album("pair", 2))

        assertEquals(albums, albums.withoutSingleTrackAlbums(enabled = true))
    }

    @Test
    fun `an empty album is dropped`() {
        val albums = listOf(album("empty", 0), album("full", 3))

        val visible = albums.withoutSingleTrackAlbums(enabled = true)

        assertEquals(listOf("full"), visible.map { it.name })
    }

    @Test
    fun `order of the surviving albums is preserved`() {
        val albums = listOf(
            album("a", 3), album("b", 1), album("c", 2), album("d", 1), album("e", 4)
        )

        val visible = albums.withoutSingleTrackAlbums(enabled = true)

        assertEquals(listOf("a", "c", "e"), visible.map { it.name })
    }

    @Test
    fun `an all-single-track library yields an empty tab`() {
        val albums = listOf(album("a", 1), album("b", 1))

        assertEquals(emptyList<Playlist>(), albums.withoutSingleTrackAlbums(enabled = true))
    }

    @Test
    fun `an empty list stays empty`() {
        assertEquals(emptyList<Playlist>(), emptyList<Playlist>().withoutSingleTrackAlbums(true))
    }

    @Test
    fun `an unnamed album is filtered on its track count like any other`() {
        val albums = listOf(album(null, 1), album(null, 2))

        val visible = albums.withoutSingleTrackAlbums(enabled = true)

        assertEquals(1, visible.size)
        assertEquals(2, visible.single().trackList.size)
    }

    // Only the number of tracks matters here - the filter never reads a field off
    // a Track - so a stand-in avoids building the Android types a real one needs.
    private val dummyTrack: Track = mock()

    private fun album(name: String?, trackCount: Int) =
        Playlist(name = name, trackList = List(trackCount) { dummyTrack })
}
