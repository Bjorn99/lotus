package com.dn0ne.player.app.data.remote.metadata

import com.dn0ne.player.app.domain.metadata.MetadataSearchResult
import com.dn0ne.player.app.domain.metadata.ReleaseMetadata
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GatedMetadataProviderTest {

    private class FakeMetadataProvider(
        var searchCallCount: Int = 0,
        var searchReleasesCallCount: Int = 0,
        var coverArtCallCount: Int = 0,
        var releaseMetadataCallCount: Int = 0,
        private val searchResult: Result<List<MetadataSearchResult>, DataError> =
            Result.Error(DataError.Network.NotFound),
        private val searchReleasesResult: Result<List<MetadataSearchResult>, DataError> =
            Result.Error(DataError.Network.NotFound),
        private val coverArtResult: Result<ByteArray, DataError> =
            Result.Error(DataError.Network.NotFound),
        private val releaseMetadataResult: Result<ReleaseMetadata, DataError> =
            Result.Error(DataError.Network.NotFound),
    ) : MetadataProvider {
        override suspend fun searchMetadata(
            query: String,
            trackDuration: Long,
            matchDuration: Boolean,
        ): Result<List<MetadataSearchResult>, DataError> {
            searchCallCount++
            return searchResult
        }

        override suspend fun searchReleases(
            query: String,
            trackDuration: Long,
            matchDuration: Boolean,
        ): Result<List<MetadataSearchResult>, DataError> {
            searchReleasesCallCount++
            return searchReleasesResult
        }

        override suspend fun getCoverArtBytes(
            searchResult: MetadataSearchResult,
        ): Result<ByteArray, DataError> {
            coverArtCallCount++
            return coverArtResult
        }

        override suspend fun getReleaseMetadata(
            releaseId: String,
        ): Result<ReleaseMetadata, DataError> {
            releaseMetadataCallCount++
            return releaseMetadataResult
        }
    }

    private val dummySearchResult = MetadataSearchResult(
        id = "test",
        title = "Test",
        artist = "Artist",
        albumId = "album",
        album = "Album",
        albumArtist = "Album Artist",
    )

    @Test
    fun `disabled gate returns NoInternet for searchMetadata without calling delegate`() = runBlocking {
        val delegate = FakeMetadataProvider()
        val gated = GatedMetadataProvider(delegate, isEnabled = { false })

        val result = gated.searchMetadata("query", 0L)

        assertEquals(0, delegate.searchCallCount)
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.NoInternet, (result as Result.Error).error)
    }

    @Test
    fun `disabled gate returns NoInternet for getCoverArtBytes without calling delegate`() = runBlocking {
        val delegate = FakeMetadataProvider()
        val gated = GatedMetadataProvider(delegate, isEnabled = { false })

        val result = gated.getCoverArtBytes(dummySearchResult)

        assertEquals(0, delegate.coverArtCallCount)
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.NoInternet, (result as Result.Error).error)
    }

    @Test
    fun `enabled gate calls delegate for searchMetadata and passes result through`() = runBlocking {
        val expected = listOf(dummySearchResult)
        val delegate = FakeMetadataProvider(
            searchResult = Result.Success(expected),
        )
        val gated = GatedMetadataProvider(delegate, isEnabled = { true })

        val result = gated.searchMetadata("query", 0L)

        assertEquals(1, delegate.searchCallCount)
        assertTrue(result is Result.Success)
        assertEquals(expected, (result as Result.Success).data)
    }

    @Test
    fun `disabled gate returns NoInternet for searchReleases without calling delegate`() = runBlocking {
        val delegate = FakeMetadataProvider()
        val gated = GatedMetadataProvider(delegate, isEnabled = { false })

        val result = gated.searchReleases("query", 0L, false)

        assertEquals(0, delegate.searchReleasesCallCount)
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.NoInternet, (result as Result.Error).error)
    }

    @Test
    fun `enabled gate calls delegate for searchReleases and passes result through`() = runBlocking {
        val expected = listOf(dummySearchResult)
        val delegate = FakeMetadataProvider(
            searchReleasesResult = Result.Success(expected),
        )
        val gated = GatedMetadataProvider(delegate, isEnabled = { true })

        val result = gated.searchReleases("query", 0L, false)

        assertEquals(1, delegate.searchReleasesCallCount)
        assertTrue(result is Result.Success)
        assertEquals(expected, (result as Result.Success).data)
    }

    @Test
    fun `enabled gate calls delegate for getCoverArtBytes and passes result through`() = runBlocking {
        val expected = byteArrayOf(1, 2, 3)
        val delegate = FakeMetadataProvider(
            coverArtResult = Result.Success(expected),
        )
        val gated = GatedMetadataProvider(delegate, isEnabled = { true })

        val result = gated.getCoverArtBytes(dummySearchResult)

        assertEquals(1, delegate.coverArtCallCount)
        assertTrue(result is Result.Success)
        assertArrayEquals(expected, (result as Result.Success).data)
    }
}
