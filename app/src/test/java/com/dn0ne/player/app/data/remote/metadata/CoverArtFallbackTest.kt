package com.dn0ne.player.app.data.remote.metadata

import com.dn0ne.player.app.domain.result.DataError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The release → release-group fallback decision for cover art.
 *
 * The HTTP round trip itself needs a Ktor engine, so the branch that decides
 * whether to spend a second request is kept pure and tested here. Companion to
 * [CoverArtRedirectTest], which covers the redirect allow-list.
 */
class CoverArtFallbackTest {

    private val groupId = "rg-1234"

    @Test
    fun `a 404 with a known release group is retried`() {
        assertTrue(
            shouldRetryWithReleaseGroup(DataError.Network.NotFound, groupId)
        )
    }

    // Without a group id there is nothing to retry against.
    @Test
    fun `a 404 without a release group is not retried`() {
        assertFalse(
            shouldRetryWithReleaseGroup(DataError.Network.NotFound, null)
        )
    }

    @Test
    fun `a 404 with a blank release group is not retried`() {
        assertFalse(shouldRetryWithReleaseGroup(DataError.Network.NotFound, ""))
        assertFalse(shouldRetryWithReleaseGroup(DataError.Network.NotFound, "   "))
    }

    // Everything below is the important half: a second request must only be
    // spent when the first failure actually implies the group might do better.
    // A timeout or a dead network says nothing about cover art availability,
    // and retrying would double the delay before the user sees an error.
    @Test
    fun `transport failures are never retried`() {
        val notWorthRetrying = listOf(
            DataError.Network.NoInternet,
            DataError.Network.RequestTimeout,
            DataError.Network.ServiceUnavailable,
            DataError.Network.InternalServerError,
            DataError.Network.Unknown,
        )
        notWorthRetrying.forEach { error ->
            assertFalse(
                "$error should not trigger a release-group retry",
                shouldRetryWithReleaseGroup(error, groupId)
            )
        }
    }

    @Test
    fun `client errors other than not-found are never retried`() {
        val notWorthRetrying = listOf(
            DataError.Network.BadRequest,
            DataError.Network.Unauthorized,
            DataError.Network.Forbidden,
            DataError.Network.ParseError,
        )
        notWorthRetrying.forEach { error ->
            assertFalse(
                "$error should not trigger a release-group retry",
                shouldRetryWithReleaseGroup(error, groupId)
            )
        }
    }

    // Exhaustive guard: of every network error the app can produce, exactly one
    // is a retry trigger. If someone adds a new DataError.Network constant and
    // wires it into the retry set by accident, this fails.
    @Test
    fun `not-found is the only retryable network error`() {
        val retryable = DataError.Network.entries.filter {
            shouldRetryWithReleaseGroup(it, groupId)
        }
        assertEquals(listOf(DataError.Network.NotFound), retryable)
    }

    // Local errors can't reach this path today, but the signature accepts the
    // wider DataError type, so none of them may trigger a network retry.
    @Test
    fun `local errors never trigger a network retry`() {
        DataError.Local.entries.forEach { error ->
            assertFalse(shouldRetryWithReleaseGroup(error, groupId))
        }
    }

    @Test
    fun `path constants address the two distinct endpoints`() {
        assertEquals("release", RELEASE_PATH)
        assertEquals("release-group", RELEASE_GROUP_PATH)
    }
}
