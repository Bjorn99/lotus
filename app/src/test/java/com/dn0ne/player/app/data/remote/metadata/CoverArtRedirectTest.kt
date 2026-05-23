package com.dn0ne.player.app.data.remote.metadata

import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverArtRedirectTest {

    private val allowed = listOf("archive.org")

    @Test
    fun `HTTPS archive org URL is accepted`() {
        val result = validateCoverArtRedirect(
            location = "https://archive.org/download/mbid-abc123/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Success)
        assertEquals(
            "https://archive.org/download/mbid-abc123/front.jpg",
            (result as Result.Success).data,
        )
    }

    @Test
    fun `HTTPS subdomain of archive org is accepted`() {
        val result = validateCoverArtRedirect(
            location = "https://ia800.us.archive.org/download/mbid-abc123/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Success)
    }

    @Test
    fun `HTTP scheme is rejected`() {
        val result = validateCoverArtRedirect(
            location = "http://archive.org/download/mbid-abc123/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `missing Location header is rejected`() {
        val result = validateCoverArtRedirect(
            location = null,
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `blank Location header is rejected`() {
        val result = validateCoverArtRedirect(
            location = "   ",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `non-allow-listed host is rejected`() {
        val result = validateCoverArtRedirect(
            location = "https://evil.com/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `substring attack is rejected`() {
        // archive.org.evil.com must NOT pass because it "contains" archive.org
        val result = validateCoverArtRedirect(
            location = "https://archive.org.evil.com/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `closely named domain is rejected`() {
        // myarchive.org contains "archive.org" as a substring but is not a subdomain
        val result = validateCoverArtRedirect(
            location = "https://myarchive.org/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `malformed URL is rejected`() {
        val result = validateCoverArtRedirect(
            location = "not a url at all !!!",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
    }
}
