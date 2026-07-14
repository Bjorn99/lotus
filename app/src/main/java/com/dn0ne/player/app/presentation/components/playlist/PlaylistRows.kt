package com.dn0ne.player.app.presentation.components.playlist

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
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
import com.dn0ne.player.app.presentation.components.NothingYet
import com.kmpalette.rememberDominantColorState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
fun LazyGridScope.playlistRows(
    playlists: List<Playlist>,
    fallbackPlaylistTitle: String,
    sort: PlaylistSort,
    sortOrder: SortOrder,
    onRowClick: (Playlist) -> Unit,
    onLongClick: (Playlist) -> Unit,
    showSinglePreview: Boolean = false,
    perTrackArtwork: Boolean = false,
) {
    if (playlists.isEmpty()) {
        item(
            span = {
                GridItemSpan(maxLineSpan)
            }
        ) {
            NothingYet()
        }
    }

    items(
        items = playlists.sortedBy(sort, sortOrder),
        key = { "${it.name}-${it.trackList}" }
    ) { playlist ->
        PlaylistRow(
            title = playlist.name
                ?: fallbackPlaylistTitle,
            trackCount = playlist.trackList.size,
            coverArtPreviewUris = playlist.trackList
                .take(if (showSinglePreview) 1 else 4)
                .map { it.coverArtUri },
            trackUris = playlist.trackList
                .take(if (showSinglePreview) 1 else 4)
                .map { it.uri },
            perTrackArtwork = perTrackArtwork,
            modifier = Modifier
                .clip(ShapeDefaults.Medium)
                .combinedClickable(
                    onLongClick = {
                        onLongClick(playlist)
                    }
                ) {
                    onRowClick(playlist)
                }
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
        )
    }
}

@Composable
fun PlaylistRow(
    title: String,
    trackCount: Int,
    coverArtPreviewUris: List<Uri>,
    trackUris: List<Uri> = emptyList(),
    perTrackArtwork: Boolean = false,
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
                        trackUri = trackUris.firstOrNull(),
                        perTrackArtwork = perTrackArtwork,
                        crossfade = false,
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
                        trackUris = trackUris,
                        perTrackArtwork = perTrackArtwork,
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

        TrackCountBubble(
            trackCount = trackCount,
            contentColor = dominantColorState.onColor,
            containerColor = dominantColorState.color
        )
    }
}