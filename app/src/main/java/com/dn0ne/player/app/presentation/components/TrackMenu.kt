package com.dn0ne.player.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dn0ne.player.R

@Composable
fun TrackMenu(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onViewTrackInfoClick: () -> Unit,
    onGoToAlbumClick: () -> Unit,
    onGoToArtistClick: () -> Unit,
    onShareClick: () -> Unit,
    onRemoveFromPlaylistClick: (() -> Unit)? = null
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = onDismissRequest,
        shape = ShapeDefaults.Medium,
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        val context = LocalContext.current
        DropdownMenuItem(
            text = {
                Text(text = context.resources.getString(R.string.play_next))
            },
            onClick = {
                onPlayNextClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.QueuePlayNext,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = {
                Text(text = context.resources.getString(R.string.add_to_queue))
            },
            onClick = {
                onAddToQueueClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.AddToQueue,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = {
                Text(text = context.resources.getString(R.string.add_to_playlist))
            },
            onClick = {
                onAddToPlaylistClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null
                )
            }
        )

        onRemoveFromPlaylistClick?.let {
            DropdownMenuItem(
                text = {
                    Text(text = context.resources.getString(R.string.remove_from_playlist))
                },
                onClick = {
                    onRemoveFromPlaylistClick()
                    onDismissRequest()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistRemove,
                        contentDescription = null
                    )
                }
            )
        }

        DropdownMenuItem(
            text = {
                Text(text = context.resources.getString(R.string.share_track))
            },
            onClick = {
                onShareClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null
                )
            }
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onGoToAlbumClick()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = context.resources.getString(R.string.go_to_album)
                    )
                }

                IconButton(
                    onClick = {
                        onGoToArtistClick()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = context.resources.getString(R.string.go_to_artist)
                    )
                }
            }

            IconButton(
                onClick = {
                    onViewTrackInfoClick()
                    onDismissRequest()
                },
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = context.resources.getString(R.string.track_info)
                )
            }
        }
        /*DropdownMenuItem(
            text = {
                Text(text = context.resources.getString(R.string.track_info))
            },
            onClick = {
                onViewTrackInfoClick()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.AudioFile,
                    contentDescription = null
                )
            }
        )*/
    }
}

@Composable
fun TrackMenuButton(
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onViewTrackInfoClick: () -> Unit,
    onGoToAlbumClick: () -> Unit,
    onGoToArtistClick: () -> Unit,
    onShareClick: () -> Unit,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
) {
    Box {
        var isMenuExpanded by remember {
            mutableStateOf(false)
        }

        IconButton(
            onClick = {
                isMenuExpanded = true
            }
        ) {
            val context = LocalContext.current
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = context.resources.getString(R.string.track_menu_button),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TrackMenu(
            isExpanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            onPlayNextClick = onPlayNextClick,
            onAddToQueueClick = onAddToQueueClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onViewTrackInfoClick = onViewTrackInfoClick,
            onGoToAlbumClick = onGoToAlbumClick,
            onGoToArtistClick = onGoToArtistClick,
            onShareClick = onShareClick,
            onRemoveFromPlaylistClick = onRemoveFromPlaylistClick
        )
    }
}