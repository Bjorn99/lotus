package com.dn0ne.player.app.data.remote.metadata

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ranking, de-dup and wire-shape tolerance for the MusicBrainz search mapper.
 *
 * The existing [MusicBrainzMetadataProviderTest] covers query building and the
 * flat DTO mapping; this covers the ordering layer added on top of it.
 */
class MusicBrainzRankingTest {

    // Mirrors the client configuration in PlayerModule: unknown keys ignored,
    // and NOT lenient. Any test that decodes JSON must use this exact shape or
    // it proves nothing about production behaviour.
    private val json = Json { ignoreUnknownKeys = true }

    private fun release(
        id: String,
        title: String = "Album $id",
        status: String? = null,
        date: String? = null,
        groupId: String? = "rg-$id",
        primaryType: String? = null,
        secondaryTypes: List<String>? = null,
        artistId: String? = null,
    ) = Release(
        id = id,
        title = title,
        artistCredit = listOf(Artist(name = "Album Artist", id = artistId)),
        media = listOf(Media(listOf(MediaTrack("1")))),
        status = status,
        date = date,
        releaseGroup = groupId?.let {
            ReleaseGroup(id = it, primaryType = primaryType, secondaryTypes = secondaryTypes)
        },
    )

    private fun recording(
        id: String = "rec-1",
        score: Int? = null,
        releases: List<Release>,
    ) = Recording(
        id = id,
        title = "Track",
        artistCredit = listOf(Artist("Artist")),
        releases = releases,
        score = score?.let { JsonPrimitive(it) },
    )

    private fun map(vararg recordings: Recording) =
        SearchResultDto(recordings.toList()).toMetadataSearchResultList()

    // ---- score wire-shape tolerance (the one that can kill the response) ----

    @Test
    fun `score arrives as a bare number`() {
        val decoded = json.decodeFromString<Recording>(
            """{"id":"r","title":"t","artist-credit":[{"name":"a"}],"score":100}"""
        )
        assertEquals(100, decoded.score.asIntOrNull())
    }

    @Test
    fun `score arrives as a quoted string`() {
        val decoded = json.decodeFromString<Recording>(
            """{"id":"r","title":"t","artist-credit":[{"name":"a"}],"score":"100"}"""
        )
        assertEquals(100, decoded.score.asIntOrNull())
    }

    @Test
    fun `absent score decodes to null rather than failing`() {
        val decoded = json.decodeFromString<Recording>(
            """{"id":"r","title":"t","artist-credit":[{"name":"a"}]}"""
        )
        assertNull(decoded.score.asIntOrNull())
    }

    @Test
    fun `explicit null score decodes to null`() {
        val decoded = json.decodeFromString<Recording>(
            """{"id":"r","title":"t","artist-credit":[{"name":"a"}],"score":null}"""
        )
        assertNull(decoded.score.asIntOrNull())
    }

    // A non-numeric score must NOT take the whole response down with it. This
    // is the entire reason the field is a raw primitive instead of an Int.
    @Test
    fun `unparseable score degrades to null instead of throwing`() {
        val decoded = json.decodeFromString<Recording>(
            """{"id":"r","title":"t","artist-credit":[{"name":"a"}],"score":"n/a"}"""
        )
        assertNull(decoded.score.asIntOrNull())
    }

    @Test
    fun `fractional score degrades to null instead of throwing`() {
        val decoded = json.decodeFromString<Recording>(
            """{"id":"r","title":"t","artist-credit":[{"name":"a"}],"score":99.5}"""
        )
        assertNull(decoded.score.asIntOrNull())
    }

    // ---- a full search payload, in the shape MusicBrainz actually sends ----

    @Test
    fun `realistic search payload parses every ranking signal`() {
        val payload = """
            {"recordings":[{
              "id":"rec-1","title":"Blow Your Mind","score":100,
              "artist-credit":[{"name":"Dua Lipa","id":"art-1"}],
              "first-release-date":"2016-08-26",
              "releases":[{
                "id":"rel-1","title":"Blow Your Mind","status":"Official",
                "date":"2016-08-26","country":"XW",
                "release-group":{"id":"rg-1","primary-type":"Single"},
                "artist-credit":[{"name":"Dua Lipa","id":"art-1"}]
              }]
            }]}
        """.trimIndent()

        val results = json.decodeFromString<SearchResultDto>(payload)
            .toMetadataSearchResultList()

        assertEquals(1, results.size)
        assertEquals("rg-1", results[0].releaseGroupId)
        assertEquals("art-1", results[0].albumArtistId)
        assertEquals("rel-1", results[0].albumId)
    }

    // Unknown fields must stay survivable — MusicBrainz adds keys over time.
    @Test
    fun `unrecognised fields in the payload do not break parsing`() {
        val payload = """
            {"created":"2026-01-01","count":1,"offset":0,"recordings":[{
              "id":"rec-1","title":"T","score":90,"length":178000,
              "artist-credit":[{"name":"A"}],
              "releases":[{"id":"rel-1","title":"Al","packaging":"Jewel Case",
                "release-group":{"id":"rg-1","primary-type":"Album",
                  "secondary-types":[],"title":"Al"}}]
            }]}
        """.trimIndent()
        val results = json.decodeFromString<SearchResultDto>(payload)
            .toMetadataSearchResultList()
        assertEquals(1, results.size)
        assertEquals("rg-1", results[0].releaseGroupId)
    }

    // ---- ranking ----

    @Test
    fun `higher score ranks first`() {
        val results = map(
            recording(id = "low", score = 50, releases = listOf(release("a"))),
            recording(id = "high", score = 100, releases = listOf(release("b"))),
        )
        assertEquals(listOf("high", "low"), results.map { it.id })
    }

    @Test
    fun `a scored recording outranks an unscored one`() {
        val results = map(
            recording(id = "none", score = null, releases = listOf(release("a"))),
            recording(id = "scored", score = 1, releases = listOf(release("b"))),
        )
        assertEquals(listOf("scored", "none"), results.map { it.id })
    }

    @Test
    fun `official outranks bootleg within the same score`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release("boot", status = "Bootleg"),
                    release("promo", status = "Promotion"),
                    release("official", status = "Official"),
                ),
            )
        )
        assertEquals(listOf("official", "promo", "boot"), results.map { it.albumId })
    }

    // An unknown status must not be punished harder than a known-bad one —
    // plenty of legitimate releases carry no status at all.
    @Test
    fun `unknown status sorts above promo and below official`() {
        assertTrue(statusRank("Official") < statusRank(null))
        assertEquals(statusRank(null), statusRank("Something New"))
        assertTrue(statusRank(null) < statusRank("Promotion"))
        assertTrue(statusRank("Promotion") < statusRank("Bootleg"))
    }

    @Test
    fun `status matching is case insensitive`() {
        assertEquals(statusRank("Official"), statusRank("official"))
        assertEquals(statusRank("Bootleg"), statusRank("BOOTLEG"))
    }

    @Test
    fun `album outranks ep outranks single`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release("single", primaryType = "Single"),
                    release("album", primaryType = "Album"),
                    release("ep", primaryType = "EP"),
                ),
            )
        )
        assertEquals(listOf("album", "ep", "single"), results.map { it.albumId })
    }

    // The point of parsing secondary types: a greatest-hits compilation is
    // still primary-type Album, so without them it would tie with the real
    // album and could win on an earlier reissue date.
    @Test
    fun `a compilation ranks below a plain album of the same type`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release(
                        "comp", primaryType = "Album",
                        secondaryTypes = listOf("Compilation"), date = "1990",
                    ),
                    release("studio", primaryType = "Album", date = "1995"),
                ),
            )
        )
        assertEquals(listOf("studio", "comp"), results.map { it.albumId })
    }

    @Test
    fun `a live album ranks below a studio album`() {
        assertTrue(
            releaseTypeRank("Album", null) <
                releaseTypeRank("Album", listOf("Live"))
        )
    }

    // Secondary types must only break ties inside a primary tier — a
    // compilation album should still beat a plain single.
    @Test
    fun `a compilation album still outranks a single`() {
        assertTrue(
            releaseTypeRank("Album", listOf("Compilation")) <
                releaseTypeRank("Single", null)
        )
    }

    // Pins the whole ordering, not just its endpoints: a secondary type costs
    // strictly less than a step down in primary type, and no two combinations
    // may tie. Without the strictness check a demotion big enough to collide
    // with the next tier down (a compilation album landing level with a plain
    // EP) would go unnoticed.
    @Test
    fun `secondary types demote within a tier but never across tiers`() {
        val ascending = listOf(
            releaseTypeRank("Album", null),
            releaseTypeRank("Album", listOf("Compilation")),
            releaseTypeRank("EP", null),
            releaseTypeRank("EP", listOf("Live")),
            releaseTypeRank("Single", null),
            releaseTypeRank("Single", listOf("Remix")),
            releaseTypeRank("Broadcast", null),
            releaseTypeRank("Broadcast", listOf("Live")),
            releaseTypeRank(null, null),
        )
        assertEquals("must be in ascending desirability order", ascending.sorted(), ascending)
        assertEquals("no two combinations may tie", ascending.distinct(), ascending)
    }

    @Test
    fun `unknown primary type sorts last`() {
        assertTrue(releaseTypeRank("Broadcast", null) < releaseTypeRank(null, null))
        assertTrue(releaseTypeRank("Broadcast", null) < releaseTypeRank("Nonsense", null))
    }

    @Test
    fun `earliest release date wins the tiebreak`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release("remaster", status = "Official", date = "2015-01-01"),
                    release("original", status = "Official", date = "1973-03-01"),
                ),
            )
        )
        assertEquals(listOf("original", "remaster"), results.map { it.albumId })
    }

    @Test
    fun `a dated release outranks an undated one`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release("undated", status = "Official", date = null),
                    release("dated", status = "Official", date = "2001"),
                ),
            )
        )
        assertEquals(listOf("dated", "undated"), results.map { it.albumId })
    }

    @Test
    fun `release year parses all musicbrainz date shapes`() {
        assertEquals(2016, releaseYear("2016-08-26"))
        assertEquals(2016, releaseYear("2016-08"))
        assertEquals(2016, releaseYear("2016"))
        assertNull(releaseYear(null))
        assertNull(releaseYear(""))
        assertNull(releaseYear("nope"))
        // A truncated year is not a year; better to sort last than to invent 201.
        assertNull(releaseYear("201"))
    }

    // ---- de-dup ----

    @Test
    fun `many pressings of one album collapse to a single row`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release("us", groupId = "rg-1", status = "Official", date = "1973"),
                    release("uk", groupId = "rg-1", status = "Official", date = "1974"),
                    release("jp", groupId = "rg-1", status = "Official", date = "1975"),
                ),
            )
        )
        assertEquals(1, results.size)
        // and the survivor is the best-ranked pressing, not just the first seen
        assertEquals("us", results[0].albumId)
    }

    @Test
    fun `distinct albums are all kept`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release("a", groupId = "rg-1"),
                    release("b", groupId = "rg-2"),
                    release("c", groupId = "rg-3"),
                ),
            )
        )
        assertEquals(3, results.size)
    }

    // Releases with no group id cannot be grouped; dropping them would lose
    // real results, so they must fall back to their own id and survive.
    @Test
    fun `releases without a group id are never collapsed together`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release("x", groupId = null),
                    release("y", groupId = null),
                ),
            )
        )
        assertEquals(2, results.size)
        assertEquals(listOf("x", "y"), results.map { it.albumId })
    }

    @Test
    fun `de-dup keeps the official pressing over an earlier bootleg`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release("boot", groupId = "rg-1", status = "Bootleg", date = "1970"),
                    release("official", groupId = "rg-1", status = "Official", date = "1990"),
                ),
            )
        )
        assertEquals(1, results.size)
        assertEquals("official", results[0].albumId)
    }

    // De-dup runs across the whole response, not per recording: two recordings
    // of the same song on the same album should still show that album once.
    @Test
    fun `the same album from two recordings appears once`() {
        val results = map(
            recording(id = "rec-a", score = 100, releases = listOf(release("r1", groupId = "rg-1"))),
            recording(id = "rec-b", score = 90, releases = listOf(release("r2", groupId = "rg-1"))),
        )
        assertEquals(1, results.size)
        assertEquals("rec-a", results[0].id)
    }

    // ---- ordering stability + regression guards ----

    @Test
    fun `equally ranked rows keep their original api order`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release("first", groupId = "rg-1", status = "Official", date = "2000"),
                    release("second", groupId = "rg-2", status = "Official", date = "2000"),
                    release("third", groupId = "rg-3", status = "Official", date = "2000"),
                ),
            )
        )
        assertEquals(listOf("first", "second", "third"), results.map { it.albumId })
    }

    // Guards the documented precedence: score is checked before status, so a
    // better-scored bootleg still beats a worse-scored official release.
    @Test
    fun `score outranks status`() {
        val results = map(
            recording(id = "hi", score = 100, releases = listOf(release("boot", status = "Bootleg"))),
            recording(id = "lo", score = 40, releases = listOf(release("off", status = "Official"))),
        )
        assertEquals(listOf("hi", "lo"), results.map { it.id })
    }

    // ...and status before type, so an official single beats a bootleg album.
    @Test
    fun `status outranks release type`() {
        val results = map(
            recording(
                score = 100,
                releases = listOf(
                    release("bootAlbum", status = "Bootleg", primaryType = "Album"),
                    release("officialSingle", status = "Official", primaryType = "Single"),
                ),
            )
        )
        assertEquals(listOf("officialSingle", "bootAlbum"), results.map { it.albumId })
    }

    // The mapper must not lose or invent rows: existing behaviour for the
    // fields the old code already produced has to survive the rewrite.
    @Test
    fun `existing field mapping is unchanged by ranking`() {
        val rec = Recording(
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
                    media = listOf(Media(listOf(MediaTrack("3")))),
                    disambiguation = "album disambig",
                    date = "1999",
                )
            ),
            tags = listOf(Tag("Rock")),
        )
        val r = map(rec).single()
        assertEquals("rec-1", r.id)
        assertEquals("Track Title", r.title)
        assertEquals("Artist Name", r.artist)
        assertEquals("rel-1", r.albumId)
        assertEquals("Album Title", r.album)
        assertEquals("Album Artist", r.albumArtist)
        assertEquals("3", r.trackNumber)
        assertEquals("track disambig", r.description)
        assertEquals("album disambig", r.albumDescription)
        assertEquals(listOf("Rock"), r.genres)
        // year stays the RECORDING's first-release date even though the
        // release carries its own, later date used only for ranking
        assertEquals("2020", r.year)
    }

    @Test
    fun `recording without releases still yields nothing`() {
        assertEquals(0, map(recording(releases = emptyList())).size)
    }

    @Test
    fun `null release group leaves releaseGroupId null`() {
        val r = map(recording(score = 1, releases = listOf(release("a", groupId = null)))).single()
        assertNull(r.releaseGroupId)
        assertNotNull(r.albumId)
    }

    // A large response must not be reordered into nonsense or lose rows it
    // shouldn't: 40 distinct albums in, 40 out, best score first.
    @Test
    fun `large response keeps every distinct album and orders by score`() {
        val recordings = (1..40).map { i ->
            recording(
                id = "rec-$i",
                score = i,
                releases = listOf(release("rel-$i", groupId = "rg-$i")),
            )
        }
        val results = SearchResultDto(recordings).toMetadataSearchResultList()
        assertEquals(40, results.size)
        assertEquals("rec-40", results.first().id)
        assertEquals("rec-1", results.last().id)
    }
}
