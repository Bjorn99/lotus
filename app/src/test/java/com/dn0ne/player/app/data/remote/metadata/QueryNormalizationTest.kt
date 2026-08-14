package com.dn0ne.player.app.data.remote.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What actually leaves the app when the user types into the metadata search box.
 *
 * The bug this pins down: [escapeLuceneQuery] was applied on both paths, and it
 * escapes ':' and '"'. A field query therefore arrived at MusicBrainz as
 * literal text, so `artist:"Chevelle" AND recording:"A Miracle"` returned 47
 * rows of noise (every recording containing the word "artist") instead of the
 * two real ones. Companion to [MusicBrainzMetadataProviderTest], which covers
 * the raw regexes.
 */
class QueryNormalizationTest {

    // ---- structured-mode detection ----

    @Test
    fun `a balanced pair of quotes means structured`() {
        assertTrue(hasStructuredSyntax("\"A Miracle\" AND \"Chevelle\""))
    }

    @Test
    fun `plain text is not structured`() {
        assertFalse(hasStructuredSyntax("A Miracle Chevelle"))
    }

    @Test
    fun `a single stray quote is not structured`() {
        assertFalse(hasStructuredSyntax("12\" remix"))
    }

    @Test
    fun `three quotes are not structured`() {
        assertFalse(hasStructuredSyntax("\"a\" \"b"))
    }

    @Test
    fun `four quotes are structured`() {
        assertTrue(hasStructuredSyntax("artist:\"a\" AND recording:\"b\""))
    }

    // ---- the regression this fix exists for ----

    @Test
    fun `field query keeps its colons and quotes`() {
        assertEquals(
            "artist:\"Chevelle\" AND recording:\"A Miracle\"",
            normalizeQuery("artist:\"Chevelle\" AND recording:\"A Miracle\"")
        )
    }

    @Test
    fun `capitalised field names are lowercased but still work`() {
        assertEquals(
            "artist:\"Chevelle\" AND recording:\"A miracle\"",
            normalizeQuery("Artist:\"Chevelle\" AND Recording:\"A miracle\"")
        )
    }

    @Test
    fun `quoted phrases without a field are left intact`() {
        assertEquals(
            "\"A Miracle\" AND \"Chevelle\"",
            normalizeQuery("\"A Miracle\" AND \"Chevelle\"")
        )
    }

    @Test
    fun `a quoted time signature survives`() {
        // "4:44" is one of the app's own documented examples; the colon must
        // stay inside the phrase or it stops being that song.
        assertEquals("\"4:44\"", normalizeQuery("\"4:44\""))
    }

    // ---- AND/OR/NOT handling is unchanged ----

    @Test
    fun `AND stays an operator in structured mode`() {
        assertTrue(normalizeQuery("\"a\" AND \"b\"").contains(" AND "))
    }

    @Test
    fun `AND becomes a literal word in plain text`() {
        // Only the operator is lowercased; the user's own capitalisation stays.
        assertEquals("Roses and Thorns", normalizeQuery("Roses AND Thorns"))
    }

    @Test
    fun `OR and NOT are also lowercased in plain text`() {
        assertEquals("a or b not c", normalizeQuery("a OR b NOT c"))
    }

    // ---- dangerous characters are still neutralised ----

    @Test
    fun `structured mode still escapes braces and tildes`() {
        assertEquals(
            "artist:\"x\" \\{\\}\\~",
            normalizeQuery("artist:\"x\" {}~")
        )
    }

    @Test
    fun `structured mode still escapes the boolean symbol operators`() {
        assertEquals(
            "artist:\"x\" \\&& \\|| \\!",
            normalizeQuery("artist:\"x\" && || !")
        )
    }

    @Test
    fun `structured mode still escapes backslash and slash`() {
        assertEquals("\"a\" \"b\" \\\\ \\/", normalizeQuery("\"a\" \"b\" \\ /"))
    }

    @Test
    fun `an unbalanced quote falls back to full escaping`() {
        // Would otherwise reach Lucene as an unterminated phrase and 400.
        assertEquals("12\\\" remix", normalizeQuery("12\" remix"))
    }

    @Test
    fun `plain text still escapes a colon`() {
        assertEquals("4\\:44", normalizeQuery("4:44"))
    }

    @Test
    fun `a hyphenated title is still escaped in plain text`() {
        assertEquals("well\\-known song", normalizeQuery("well-known song"))
    }

    // ---- the two escapers differ only in ':' and '"' ----

    @Test
    fun `structured escaper preserves exactly colon and quote`() {
        val sample = "a:b\"c{d}e~f"
        assertEquals("a\\:b\\\"c\\{d\\}e\\~f", escapeLuceneQuery(sample))
        assertEquals("a:b\"c\\{d\\}e\\~f", escapeStructuredQuery(sample))
    }
}
