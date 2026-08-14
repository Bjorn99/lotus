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

    // ---- hop policy ----
    //
    // The real CoverArtArchive chain, which the single-hop version could not
    // complete:
    //   coverartarchive.org/release/{id}/front -> 307 archive.org/download/...
    //   archive.org/download/...               -> 302 dn######.ca.archive.org/...
    //   dn######.ca.archive.org/...            -> 200 image

    private fun step(
        status: Int,
        location: String? = null,
        hopsUsed: Int = 0,
        maxHops: Int = MAX_COVER_ART_HOPS,
    ) = nextCoverArtStep(status, location, hopsUsed, maxHops, allowed)

    @Test
    fun `200 reads the body`() {
        assertEquals(CoverArtStep.Read, step(status = 200))
    }

    @Test
    fun `the full CoverArtArchive chain completes`() {
        val first = step(
            status = 307,
            location = "https://archive.org/download/mbid-abc/mbid-abc-123.jpg",
            hopsUsed = 0,
        )
        assertEquals(
            CoverArtStep.Follow("https://archive.org/download/mbid-abc/mbid-abc-123.jpg"),
            first,
        )

        // The hop that used to fail: a 302 fell through to "unknown error"
        // because only 200 and 404 were handled after the first redirect.
        val second = step(
            status = 302,
            location = "https://dn720706.ca.archive.org/0/items/mbid-abc/mbid-abc-123.jpg",
            hopsUsed = 1,
        )
        assertEquals(
            CoverArtStep.Follow("https://dn720706.ca.archive.org/0/items/mbid-abc/mbid-abc-123.jpg"),
            second,
        )

        assertEquals(CoverArtStep.Read, step(status = 200, hopsUsed = 2))
    }

    @Test
    fun `every redirect status in the family is followed`() {
        for (status in listOf(301, 302, 303, 307, 308)) {
            assertEquals(
                "status $status must be treated as a hop",
                CoverArtStep.Follow("https://archive.org/front.jpg"),
                step(status = status, location = "https://archive.org/front.jpg"),
            )
        }
    }

    @Test
    fun `a later hop is host-checked, not just the first`() {
        // The security property that matters: hop 2 pointing off-site is
        // rejected exactly like hop 1 would be.
        val result = step(
            status = 302,
            location = "https://evil.com/front.jpg",
            hopsUsed = 1,
        )
        assertEquals(CoverArtStep.Fail(DataError.Network.Unknown), result)
    }

    @Test
    fun `a redirect loop is cut off at the hop limit`() {
        val result = step(
            status = 302,
            location = "https://archive.org/front.jpg",
            hopsUsed = MAX_COVER_ART_HOPS,
        )
        assertEquals(CoverArtStep.Fail(DataError.Network.Unknown), result)
    }

    @Test
    fun `the last allowed hop is still followed`() {
        val result = step(
            status = 302,
            location = "https://archive.org/front.jpg",
            hopsUsed = MAX_COVER_ART_HOPS - 1,
        )
        assertEquals(CoverArtStep.Follow("https://archive.org/front.jpg"), result)
    }

    @Test
    fun `a redirect with no Location header fails instead of looping`() {
        assertEquals(
            CoverArtStep.Fail(DataError.Network.Unknown),
            step(status = 302, location = null),
        )
    }

    @Test
    fun `error statuses map to their own errors`() {
        assertEquals(CoverArtStep.Fail(DataError.Network.BadRequest), step(status = 400))
        assertEquals(CoverArtStep.Fail(DataError.Network.NotFound), step(status = 404))
        assertEquals(
            CoverArtStep.Fail(DataError.Network.ServiceUnavailable),
            step(status = 503),
        )
        assertEquals(CoverArtStep.Fail(DataError.Network.Unknown), step(status = 418))
    }

    @Test
    fun `404 stays a 404 so the release-group fallback still triggers`() {
        // shouldRetryWithReleaseGroup keys off NotFound specifically. If a 404
        // were flattened to Unknown here, the fallback added alongside it
        // would go quiet again.
        assertEquals(CoverArtStep.Fail(DataError.Network.NotFound), step(status = 404))
    }
}
