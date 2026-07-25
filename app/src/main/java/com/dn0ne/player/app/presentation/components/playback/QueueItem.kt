package com.dn0ne.player.app.presentation.components.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.dn0ne.player.app.presentation.components.CoverArt

@Composable
fun QueueItem(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemoveFromQueueClick: () -> Unit,
    dragHandle: @Composable () -> Unit,
    perTrackArtwork: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(ShapeDefaults.Medium)
            .clickable {
                onClick()
            }
            .background(
                color = if (isCurrent) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent
            )
            .padding(8.dp),
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
                trackUri = track.uri,
                perTrackArtwork = perTrackArtwork,
                crossfade = false,
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
            IconButton(
                onClick = onRemoveFromQueueClick
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = context.resources.getString(R.string.remove_from_queue) +
                            " ${track.title}"
                )
            }

            dragHandle()
        }
    }
}