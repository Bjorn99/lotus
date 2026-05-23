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

class ChainLyricsProviderTest {

    private class FakeLyricsProvider(
        private val getLyricsResult: Result<Lyrics, DataError.Network>,
        var getLyricsCallCount: Int = 0,
        var postLyricsCallCount: Int = 0,
    ) : LyricsProvider {
        override suspend fun getLyrics(track: Track): Result<Lyrics, DataError.Network> {
            getLyricsCallCount++
            return getLyricsResult
        }

        override suspend fun postLyrics(
            track: Track,
            lyrics: Lyrics,
        ): Result<Unit, DataError.Network> {
            postLyricsCallCount++
            return Result.Success(Unit)
        }
    }

    private val dummyTrack: Track = mock()
    private val dummyLyrics = Lyrics(uri = "test")

    @Test
    fun `first provider success returns immediately without calling second`() = runBlocking {
        val success = Lyrics(uri = "found", synced = emptyList())
        val p1 = FakeLyricsProvider(getLyricsResult = Result.Success(success))
        val p2 = FakeLyricsProvider(getLyricsResult = Result.Error(DataError.Network.NotFound))
        val chain = ChainLyricsProvider(listOf(p1, p2))

        val result = chain.getLyrics(dummyTrack)

        assertEquals(1, p1.getLyricsCallCount)
        assertEquals(0, p2.getLyricsCallCount)
        assertTrue(result is Result.Success)
        assertEquals(success, (result as Result.Success).data)
    }

    @Test
    fun `first NotFound falls through to second success`() = runBlocking {
        val success = Lyrics(uri = "found", synced = emptyList())
        val p1 = FakeLyricsProvider(getLyricsResult = Result.Error(DataError.Network.NotFound))
        val p2 = FakeLyricsProvider(getLyricsResult = Result.Success(success))
        val chain = ChainLyricsProvider(listOf(p1, p2))

        val result = chain.getLyrics(dummyTrack)

        assertEquals(1, p1.getLyricsCallCount)
        assertEquals(1, p2.getLyricsCallCount)
        assertTrue(result is Result.Success)
        assertEquals(success, (result as Result.Success).data)
    }

    @Test
    fun `all NotFound returns last error preserved`() = runBlocking {
        val p1 = FakeLyricsProvider(getLyricsResult = Result.Error(DataError.Network.NotFound))
        val p2 = FakeLyricsProvider(getLyricsResult = Result.Error(DataError.Network.ServiceUnavailable))
        val chain = ChainLyricsProvider(listOf(p1, p2))

        val result = chain.getLyrics(dummyTrack)

        assertEquals(1, p1.getLyricsCallCount)
        assertEquals(1, p2.getLyricsCallCount)
        assertTrue(result is Result.Error)
        // Last error wins — p2's ServiceUnavailable, not p1's NotFound
        assertEquals(DataError.Network.ServiceUnavailable, (result as Result.Error).error)
    }

    @Test
    fun `BadRequest short-circuits chain immediately`() = runBlocking {
        val p1 = FakeLyricsProvider(getLyricsResult = Result.Error(DataError.Network.BadRequest))
        val p2 = FakeLyricsProvider(getLyricsResult = Result.Error(DataError.Network.NotFound))
        val chain = ChainLyricsProvider(listOf(p1, p2))

        val result = chain.getLyrics(dummyTrack)

        assertEquals(1, p1.getLyricsCallCount)
        assertEquals(0, p2.getLyricsCallCount)
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.BadRequest, (result as Result.Error).error)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty provider list throws IllegalArgumentException`() {
        ChainLyricsProvider(emptyList())
    }

    @Test
    fun `postLyrics delegates to the first provider in the list`() = runBlocking {
        val p1 = FakeLyricsProvider(getLyricsResult = Result.Error(DataError.Network.NotFound))
        val p2 = FakeLyricsProvider(getLyricsResult = Result.Error(DataError.Network.NotFound))
        val chain = ChainLyricsProvider(listOf(p1, p2))

        chain.postLyrics(dummyTrack, dummyLyrics)

        assertEquals(1, p1.postLyricsCallCount)
        assertEquals(0, p2.postLyricsCallCount)
    }
}
