package com.dn0ne.player.app.domain.lyrics

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Pins the lyric-source precedence and the privacy-relevant invariant that the
// remote (network) source is tried LAST and only when every local source
// misses. Each supplier records that it ran, so the tests assert both the
// winner and the exact call order / short-circuiting.
class LyricsResolverTest {

    private fun lyrics(tag: String) = Lyrics(uri = tag)

    @Test
    fun `sidecar wins and short-circuits when enabled`() = runBlocking {
        val called = mutableListOf<String>()
        val result = resolveLyrics(
            sidecarEnabled = true,
            sidecar = { called += "sidecar"; lyrics("sidecar") },
            cache = { called += "cache"; lyrics("cache") },
            embedded = { called += "embedded"; lyrics("embedded") },
            remote = { called += "remote"; lyrics("remote") },
        )

        assertEquals("sidecar", result?.uri)
        assertEquals(listOf("sidecar"), called)
    }

    @Test
    fun `sidecar is never called when disabled`() = runBlocking {
        val called = mutableListOf<String>()
        val result = resolveLyrics(
            sidecarEnabled = false,
            sidecar = { called += "sidecar"; lyrics("sidecar") },
            cache = { called += "cache"; lyrics("cache") },
            embedded = { called += "embedded"; lyrics("embedded") },
            remote = { called += "remote"; lyrics("remote") },
        )

        assertEquals("cache", result?.uri)
        assertEquals(listOf("cache"), called)
    }

    @Test
    fun `cache precedes embedded and remote`() = runBlocking {
        val called = mutableListOf<String>()
        val result = resolveLyrics(
            sidecarEnabled = true,
            sidecar = { called += "sidecar"; null },
            cache = { called += "cache"; lyrics("cache") },
            embedded = { called += "embedded"; lyrics("embedded") },
            remote = { called += "remote"; lyrics("remote") },
        )

        assertEquals("cache", result?.uri)
        assertEquals(listOf("sidecar", "cache"), called)
    }

    @Test
    fun `embedded precedes remote`() = runBlocking {
        val called = mutableListOf<String>()
        val result = resolveLyrics(
            sidecarEnabled = true,
            sidecar = { called += "sidecar"; null },
            cache = { called += "cache"; null },
            embedded = { called += "embedded"; lyrics("embedded") },
            remote = { called += "remote"; lyrics("remote") },
        )

        assertEquals("embedded", result?.uri)
        assertEquals(listOf("sidecar", "cache", "embedded"), called)
    }

    // Privacy invariant: network is reached only on a total local miss, and it
    // is the last source tried.
    @Test
    fun `remote is last and only on total local miss`() = runBlocking {
        val called = mutableListOf<String>()
        val result = resolveLyrics(
            sidecarEnabled = true,
            sidecar = { called += "sidecar"; null },
            cache = { called += "cache"; null },
            embedded = { called += "embedded"; null },
            remote = { called += "remote"; lyrics("remote") },
        )

        assertEquals("remote", result?.uri)
        assertEquals(listOf("sidecar", "cache", "embedded", "remote"), called)
    }

    @Test
    fun `all sources miss returns null`() = runBlocking {
        val result = resolveLyrics(
            sidecarEnabled = true,
            sidecar = { null },
            cache = { null },
            embedded = { null },
            remote = { null },
        )

        assertNull(result)
    }
}
