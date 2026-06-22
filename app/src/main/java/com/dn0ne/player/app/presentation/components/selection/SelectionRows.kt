package com.dn0ne.player.app.presentation.components.selection

import android.net.Uri
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dn0ne.player.app.domain.sort.PlaylistSort
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.sortedBy
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.presentation.components.CoverArt
import com.dn0ne.player.app.presentation.components.playlist.FourArtsPreview
import com.dn0ne.player.app.presentation.components.playlist.TrackCountBubble
import com.kmpalette.rememberDominantColorState
import kotlinx.coroutines.launch

fun LazyGridScope.selectionRows(
    playlists: List<Playlist>,
    selectedPlaylists: List<Playlist>,
    fallbackPlaylistTitle: String,
    sort: PlaylistSort,
    sortOrder: SortOrder,
    onRowClick: (Playlist) -> Unit,
    showSinglePreview: Boolean = false,
) {
    items(
        items = playlists.sortedBy(sort, sortOrder),
        key = { "${it.name}-${it.trackList}" }
    ) { playlist ->
        SelectionRow(
            title = playlist.name
                ?: fallbackPlaylistTitle,
            trackCount = playlist.trackList.size,
            coverArtPreviewUris = playlist.trackList
                .take(if (showSinglePreview) 1 else 4)
                .map { it.coverArtUri },
            isSelected = playlist in selectedPlaylists,
            modifier = Modifier
                .clip(ShapeDefaults.Medium)
                .clickable {
                    onRowClick(playlist)
                }
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
        )
    }
}

@Composable
fun SelectionRow(
    title: String,
    trackCount: Int,
    coverArtPreviewUris: List<Uri>,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val dominantColorState = rememberDominantColorState()
        val coroutineScope = rememberCoroutineScope()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Box {
                if (coverArtPreviewUris.size <= 1) {
                    CoverArt(
                        uri = coverArtPreviewUris.firstOrNull() ?: Uri.EMPTY,
                        onCoverArtLoaded = { bitmap ->
                            bitmap?.let {
                                coroutineScope.launch {
                                    dominantColorState.updateFrom(it)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .clip(ShapeDefaults.Small)
                    )
                } else {
                    FourArtsPreview(
                        coverArtPreviewUris = coverArtPreviewUris,
                        containerShape = ShapeDefaults.Small,
                        artShape = ShapeDefaults.ExtraSmall,
                        spaceBetween = 2.dp,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.basicMarquee()
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrackCountBubble(
                trackCount = trackCount,
                contentColor = dominantColorState.onColor,
                containerColor = dominantColorState.color
            )

            Checkbox(
                checked = isSelected,
                onCheckedChange = null
            )
        }
    }
}