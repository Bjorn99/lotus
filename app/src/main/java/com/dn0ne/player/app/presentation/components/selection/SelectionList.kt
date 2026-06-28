package com.dn0ne.player.app.presentation.components.selection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dn0ne.player.app.domain.track.Track

fun LazyListScope.selectionList(
    trackList: List<Track>,
    selectedTracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    perTrackArtwork: Boolean = false,
) {
    items(
        items = trackList,
        key = { it.uri }
    ) { track ->
        SelectionListItem(
            track = track,
            isSelected = track in selectedTracks,
            onClick = { onTrackClick(track) },
            perTrackArtwork = perTrackArtwork,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

fun LazyGridScope.selectionList(
    trackList: List<Track>,
    selectedTracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    perTrackArtwork: Boolean = false,
) {
    items(
        items = trackList,
        key = { it.uri }
    ) { track ->
        SelectionListItem(
            track = track,
            isSelected = track in selectedTracks,
            onClick = { onTrackClick(track) },
            perTrackArtwork = perTrackArtwork,
            modifier = Modifier.fillMaxWidth()
        )
    }
}