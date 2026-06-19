package com.dn0ne.player.app.data.remote.metadata

import com.dn0ne.player.app.domain.metadata.MetadataSearchResult
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result

// Wraps a real MetadataProvider with a runtime enable check. When disabled
// every call returns a "no internet" style result without touching the
// network. The same shape exists for lyrics in GatedLyricsProvider.
class GatedMetadataProvider(
    private val delegate: MetadataProvider,
    private val isEnabled: () -> Boolean,
) : MetadataProvider {
    override suspend fun searchMetadata(
        query: String,
        trackDuration: Long,
        matchDuration: Boolean,
    ): Result<List<MetadataSearchResult>, DataError> =
        if (isEnabled()) {
            delegate.searchMetadata(query, trackDuration, matchDuration)
        } else {
            Result.Error(DataError.Network.NoInternet)
        }

    override suspend fun searchReleases(
        query: String,
        trackDuration: Long,
        matchDuration: Boolean,
    ): Result<List<MetadataSearchResult>, DataError> =
        if (isEnabled()) {
            delegate.searchReleases(query, trackDuration, matchDuration)
        } else {
            Result.Error(DataError.Network.NoInternet)
        }

    override suspend fun getCoverArtBytes(
        searchResult: MetadataSearchResult,
    ): Result<ByteArray, DataError> =
        if (isEnabled()) {
            delegate.getCoverArtBytes(searchResult)
        } else {
            Result.Error(DataError.Network.NoInternet)
        }
}
