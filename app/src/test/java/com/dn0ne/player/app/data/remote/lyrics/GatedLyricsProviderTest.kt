package com.dn0ne.player.app.data.remote.lyrics

import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class GatedLyricsProviderTest {

    private class FakeLyricsProvider(
        var getLyricsCallCount: Int = 0,
    ) : LyricsProvider {
        override suspend fun getLyrics(track: Track): Result<Lyrics, DataError.Network> {
            getLyricsCallCount++
            return Result.Error(DataError.Network.NotFound)
        }

        override suspend fun postLyrics(
            track: Track,
            lyrics: Lyrics,
        ): Result<Unit, DataError.Network> {
            return Result.Success(Unit)
        }
    }

    // Track's constructor requires Android types (Uri, MediaItem) that can't
    // be instantiated in JVM tests. Both the gated provider and fake delegate
    // pass Track through without reading fields, so a mock is safe here.
    private val dummyTrack: Track = mock()

    @Test
    fun `disabled gate returns NotFound without calling delegate`() = runBlocking {
        val delegate = FakeLyricsProvider()
        val gated = GatedLyricsProvider(delegate, isEnabled = { false })

        val result = gated.getLyrics(dummyTrack)

        assertEquals(0, delegate.getLyricsCallCount)
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.NotFound, (result as Result.Error).error)
    }

    @Test
    fun `enabled gate calls delegate and passes result through`() = runBlocking {
        val delegate = FakeLyricsProvider()
        val gated = GatedLyricsProvider(delegate, isEnabled = { true })

        val result = gated.getLyrics(dummyTrack)

        assertEquals(1, delegate.getLyricsCallCount)
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.NotFound, (result as Result.Error).error)
    }

    @Test
    fun `disabled gate returns NotFound for postLyrics without calling delegate`() = runBlocking {
        val delegate = FakeLyricsProvider()
        val gated = GatedLyricsProvider(delegate, isEnabled = { false })

        val result = gated.postLyrics(dummyTrack, Lyrics(uri = "test"))

        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.NotFound, (result as Result.Error).error)
    }

    @Test
    fun `enabled gate calls delegate postLyrics and passes result through`() = runBlocking {
        val delegate = FakeLyricsProvider()
        val gated = GatedLyricsProvider(delegate, isEnabled = { true })

        val result = gated.postLyrics(dummyTrack, Lyrics(uri = "test"))

        assertTrue(result is Result.Success)
    }
}
