package com.dn0ne.player.app.presentation.components.trackinfo

import com.dn0ne.player.app.domain.metadata.MetadataSearchResult
import com.dn0ne.player.app.domain.track.Playlist

data class AlbumInfoSheetState(
    val isShown: Boolean = false,
    val playlist: Playlist? = null,
    val isLoading: Boolean = false,
    val isFetchingRelease: Boolean = false,
    val searchResults: List<MetadataSearchResult> = emptyList(),
)
