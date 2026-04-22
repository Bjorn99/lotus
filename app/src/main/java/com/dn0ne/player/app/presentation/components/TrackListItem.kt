package com.dn0ne.player.app.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.track.Track

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListItem(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onViewTrackInfoClick: () -> Unit,
    onGoToAlbumClick: () -> Unit,
    onGoToArtistClick: () -> Unit,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
    dragHandle: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(ShapeDefaults.Medium)
            .combinedClickable(
                onLongClick = onLongClick
            ) {
                onClick()
            }
            .background(
                color = if (isCurrent) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent
            )
            .padding(vertical = 8.dp)
            .padding(start = 8.dp, end = if (dragHandle != null) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(.8f),
            verticalAlignment = Alignment.CenterVertically
        ) {

            CoverArt(
                uri = track.coverArtUri,
                modifier = Modifier
                    .size(60.dp)
                    .clip(ShapeDefaults.Small)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = track.title ?: context.resources.getString(R.string.unknown_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = track.artist ?: context.resources.getString(R.string.unknown_artist),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.basicMarquee()
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackMenuButton(
                onPlayNextClick = onPlayNextClick,
                onAddToQueueClick = onAddToQueueClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onViewTrackInfoClick = onViewTrackInfoClick,
                onGoToAlbumClick = onGoToAlbumClick,
                onGoToArtistClick = onGoToArtistClick,
                onShareClick = { shareTrack(context, track) },
                onRemoveFromPlaylistClick = onRemoveFromPlaylistClick
            )

            dragHandle?.invoke()
        }
    }
}