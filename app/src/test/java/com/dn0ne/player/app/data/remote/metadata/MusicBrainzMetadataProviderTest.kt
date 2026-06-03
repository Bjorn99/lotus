package com.dn0ne.player.app.data.remote.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicBrainzMetadataProviderTest {

    // ---- MBID regex ----

    @Test
    fun `uuid string matches MBID regex`() {
        assertTrue(MBID_REGEX.matches(
            "9c2e84b8-1234-5678-9abc-def012345678"
        ))
    }

    @Test
    fun `uppercase uuid matches MBID regex`() {
        assertTrue(MBID_REGEX.matches(
            "9C2E84B8-1234-5678-9ABC-DEF012345678"
        ))
    }

    @Test
    fun `non-uuid string does not match MBID regex`() {
        assertFalse(MBID_REGEX.matches("Karrierelied"))
    }

    @Test
    fun `uuid with extra prefix does not match MBID regex`() {
        assertFalse(MBID_REGEX.matches(
            "mbid:9c2e84b8-1234-5678-9abc-def012345678"
        ))
    }

    @Test
    fun `empty string does not match MBID regex`() {
        assertFalse(MBID_REGEX.matches(""))
    }

    // ---- Lucene escaping ----

    @Test
    fun `escapeLuceneQuery escapes hyphen`() {
        assertEquals("\\-", escapeLuceneQuery("-"))
    }

    @Test
    fun `escapeLuceneQuery escapes colon`() {
        assertEquals("\\:", escapeLuceneQuery(":"))
    }

    @Test
    fun `escapeLuceneQuery escapes double AND operator`() {
        assertEquals("\\&&", escapeLuceneQuery("&&"))
    }

    @Test
    fun `escapeLuceneQuery escapes double OR operator`() {
        assertEquals("\\||", escapeLuceneQuery("||"))
    }

    @Test
    fun `escapeLuceneQuery escapes all special chars`() {
        val result = escapeLuceneQuery("+-!(){}[]^\"~*?:\\/")
        assertEquals(
            "\\+\\-\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\?\\:\\\\\\/",
            result
        )
    }

    @Test
    fun `escapeLuceneQuery leaves plain text unchanged`() {
        assertEquals("Karrierelied", escapeLuceneQuery("Karrierelied"))
    }

    @Test
    fun `escapeLuceneQuery handles track name with hyphen`() {
        assertEquals("Jay\\-Z", escapeLuceneQuery("Jay-Z"))
    }

    // ---- HAS_LUCENE_SYNTAX ----

    @Test
    fun `query with double quotes signals Lucene syntax`() {
        assertTrue(HAS_LUCENE_SYNTAX.containsMatchIn(
            "artist:\"Jay-Z\""
        ))
    }

    @Test
    fun `query without double quotes does not signal Lucene syntax`() {
        assertFalse(HAS_LUCENE_SYNTAX.containsMatchIn(
            "Karrierelied"
        ))
    }

    // ---- FIELD_NORMALIZE ----

    @Test
    fun `capitalized field qualifiers are lowercased`() {
        val result = "Artist:\"Alanis Morissette\" AND Release:\"Wunderkind\""
            .replace(FIELD_NORMALIZE) { it.value.lowercase() }
        assertEquals(
            "artist:\"Alanis Morissette\" AND release:\"Wunderkind\"",
            result
        )
    }

    @Test
    fun `already-lowercase field qualifiers are unchanged`() {
        val result = "artist:\"Jay-Z\""
            .replace(FIELD_NORMALIZE) { it.value.lowercase() }
        assertEquals("artist:\"Jay-Z\"", result)
    }

    // ---- toMetadataSearchResultList ----

    @Test
    fun `recording without releases returns empty list`() {
        val recording = Recording(
            id = "abc-123",
            title = "Test",
            artistCredit = listOf(Artist("Artist")),
            releases = null,
        )
        val dto = SearchResultDto(listOf(recording))
        assertEquals(0, dto.toMetadataSearchResultList().size)
    }

    @Test
    fun `recording with empty releases returns empty list`() {
        val recording = Recording(
            id = "abc-123",
            title = "Test",
            artistCredit = listOf(Artist("Artist")),
            releases = emptyList(),
        )
        val dto = SearchResultDto(listOf(recording))
        assertEquals(0, dto.toMetadataSearchResultList().size)
    }

    @Test
    fun `full release data parses correctly`() {
        val recording = Recording(
            id = "rec-1",
            title = "Track Title",
            artistCredit = listOf(Artist("Artist Name")),
            disambiguation = "track disambig",
            firstReleaseDate = "2020",
            releases = listOf(
                Release(
                    id = "rel-1",
                    title = "Album Title",
                    artistCredit = listOf(Artist("Album Artist")),
                    media = listOf(
                        Media(listOf(MediaTrack("3")))
                    ),
                    disambiguation = "album disambig",
                )
            ),
            tags = listOf(Tag("Rock"), Tag("Pop")),
        )
        val dto = SearchResultDto(listOf(recording))
        val results = dto.toMetadataSearchResultList()

        assertEquals(1, results.size)
        val r = results[0]
        assertEquals("rec-1", r.id)
        assertEquals("Track Title", r.title)
        assertEquals("Artist Name", r.artist)
        assertEquals("rel-1", r.albumId)
        assertEquals("Album Title", r.album)
        assertEquals("Album Artist", r.albumArtist)
        assertEquals("3", r.trackNumber)
        assertEquals("2020", r.year)
        assertEquals("track disambig", r.description)
        assertEquals("album disambig", r.albumDescription)
        assertEquals(listOf("Rock", "Pop"), r.genres)
    }

    @Test
    fun `release without media returns null trackNumber`() {
        val recording = Recording(
            id = "rec-1",
            title = "Track",
            artistCredit = listOf(Artist("Artist")),
            releases = listOf(
                Release(
                    id = "rel-1",
                    title = "Album",
                    artistCredit = listOf(Artist("Album Artist")),
                    media = null,
                )
            ),
        )
        val dto = SearchResultDto(listOf(recording))
        val results = dto.toMetadataSearchResultList()

        assertEquals(1, results.size)
        assertNull(results[0].trackNumber)
    }

    @Test
    fun `release without artistCredit falls back to recording artist`() {
        val recording = Recording(
            id = "rec-1",
            title = "Track",
            artistCredit = listOf(Artist("Recording Artist")),
            releases = listOf(
                Release(
                    id = "rel-1",
                    title = "Album",
                    artistCredit = null,
                    media = listOf(Media(listOf(MediaTrack("1")))),
                )
            ),
        )
        val dto = SearchResultDto(listOf(recording))
        val results = dto.toMetadataSearchResultList()

        assertEquals(1, results.size)
        assertEquals("Recording Artist", results[0].albumArtist)
    }

    @Test
    fun `recording with empty artistCredit returns empty artist`() {
        val recording = Recording(
            id = "rec-1",
            title = "Track",
            artistCredit = emptyList(),
            releases = listOf(
                Release(
                    id = "rel-1",
                    title = "Album",
                    artistCredit = listOf(Artist("Album Artist")),
                    media = listOf(Media(listOf(MediaTrack("1")))),
                )
            ),
        )
        val dto = SearchResultDto(listOf(recording))
        val results = dto.toMetadataSearchResultList()

        assertEquals(1, results.size)
        assertEquals("", results[0].artist)
    }

    @Test
    fun `release without media tracks returns null trackNumber`() {
        val recording = Recording(
            id = "rec-1",
            title = "Track",
            artistCredit = listOf(Artist("Artist")),
            releases = listOf(
                Release(
                    id = "rel-1",
                    title = "Album",
                    artistCredit = listOf(Artist("Album Artist")),
                    media = listOf(Media(emptyList())),
                )
            ),
        )
        val dto = SearchResultDto(listOf(recording))
        val results = dto.toMetadataSearchResultList()

        assertEquals(1, results.size)
        assertNull(results[0].trackNumber)
    }
}
