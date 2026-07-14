package com.dn0ne.player.app.presentation.components.playlist

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
fun LazyGridScope.playlistCards(
    playlists: List<Playlist>,
    fallbackPlaylistTitle: String,
    sort: PlaylistSort,
    sortOrder: SortOrder,
    onCardClick: (Playlist) -> Unit,
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
        PlaylistCard(
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
                .clip(ShapeDefaults.Large)
                .combinedClickable(
                    onLongClick = {
                        onLongClick(playlist)
                    }
                ) {
                    onCardClick(playlist)
                }
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
        )
    }
}

@Composable
fun PlaylistCard(
    title: String,
    trackCount: Int,
    coverArtPreviewUris: List<Uri>,
    trackUris: List<Uri> = emptyList(),
    perTrackArtwork: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            if (coverArtPreviewUris.size <= 1) {
                val dominantColorState = rememberDominantColorState()
                val coroutineScope = rememberCoroutineScope()
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
                        .fillMaxWidth()
                        .clip(ShapeDefaults.Large)
                )

                TrackCountBubble(
                    trackCount = trackCount,
                    contentColor = dominantColorState.onColor,
                    containerColor = dominantColorState.color,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp)
                )
            } else {
                FourArtsPreview(
                    coverArtPreviewUris = coverArtPreviewUris,
                    trackUris = trackUris,
                    perTrackArtwork = perTrackArtwork,
                    modifier = Modifier.fillMaxWidth()
                )

                TrackCountBubble(
                    trackCount = trackCount,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FourArtsPreview(
    coverArtPreviewUris: List<Uri>,
    trackUris: List<Uri> = emptyList(),
    perTrackArtwork: Boolean = false,
    containerShape: Shape = ShapeDefaults.Large,
    artShape: Shape = ShapeDefaults.Small,
    spaceBetween: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(containerShape)
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(spaceBetween)
    ) {
        Row {
            coverArtPreviewUris.getOrNull(0)?.let {
                CoverArt(
                    uri = it,
                    trackUri = trackUris.getOrNull(0),
                    perTrackArtwork = perTrackArtwork,
                    crossfade = false,
                    modifier = Modifier
                        .weight(1f)
                        .clip(artShape)
                )
            } ?: Box(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.width(spaceBetween))

            coverArtPreviewUris.getOrNull(1)?.let {
                CoverArt(
                    uri = it,
                    trackUri = trackUris.getOrNull(1),
                    perTrackArtwork = perTrackArtwork,
                    crossfade = false,
                    modifier = Modifier
                        .weight(1f)
                        .clip(artShape)
                )
            } ?: Box(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(spaceBetween))

        Row {
            coverArtPreviewUris.getOrNull(2)?.let {
                CoverArt(
                    uri = it,
                    trackUri = trackUris.getOrNull(2),
                    perTrackArtwork = perTrackArtwork,
                    crossfade = false,
                    modifier = Modifier
                        .weight(1f)
                        .clip(artShape)
                )
            } ?: Box(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.width(spaceBetween))

            coverArtPreviewUris.getOrNull(3)?.let {
                CoverArt(
                    uri = it,
                    trackUri = trackUris.getOrNull(3),
                    perTrackArtwork = perTrackArtwork,
                    crossfade = false,
                    modifier = Modifier
                        .weight(1f)
                        .clip(artShape)
                )
            } ?: Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun TrackCountBubble(
    trackCount: Int,
    contentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(ShapeDefaults.ExtraLarge)
            .background(color = containerColor)
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = .1f),
                shape = ShapeDefaults.ExtraLarge
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = "$trackCount",
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}