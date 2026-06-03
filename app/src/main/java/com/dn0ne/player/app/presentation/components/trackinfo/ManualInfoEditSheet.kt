package com.dn0ne.player.app.presentation.components.trackinfo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil3.compose.AsyncImage
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.metadata.Metadata
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar

@Composable
fun ManualInfoEditSheet(
    track: Track,
    state: ManualInfoEditSheetState,
    isCoverArtEditable: Boolean,
    onPickCoverArtClick: () -> Unit,
    onRestoreCoverArtClick: () -> Unit,
    onNextClick: (Metadata) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }
    val context = LocalContext.current
    Box {
        var title by rememberSaveable {
            mutableStateOf(track.title ?: "")
        }
        var album by rememberSaveable {
            mutableStateOf(track.album ?: "")
        }
        var artist by rememberSaveable {
            mutableStateOf(track.artist ?: "")
        }
        var albumArtist by rememberSaveable {
            mutableStateOf(track.albumArtist ?: "")
        }
        var genre by rememberSaveable {
            mutableStateOf(track.genre ?: "")
        }
        var year by rememberSaveable {
            mutableStateOf(track.year ?: "")
        }
        var trackNumber by rememberSaveable {
            mutableStateOf(track.trackNumber ?: "")
        }

        ColumnWithCollapsibleTopBar(
            topBarContent = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBackIosNew,
                        contentDescription = context.resources.getString(R.string.back_to_track_info)
                    )
                }

                Text(
                    text = context.resources.getString(R.string.edit),
                    fontSize = lerp(
                        MaterialTheme.typography.titleLarge.fontSize,
                        MaterialTheme.typography.displaySmall.fontSize,
                        collapseFraction
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp)
                )
            },
            collapsedByDefault = true,
            collapseFraction = {
                collapseFraction = it
            },
            contentPadding = PaddingValues(horizontal = 20.dp),
            contentHorizontalAlignment = Alignment.CenterHorizontally,
            contentVerticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            val context = LocalContext.current

            if (isCoverArtEditable) {
                AnimatedContent(
                    targetState = state.pickedCoverArtBytes,
                    label = "art fade animation",
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }
                ) { bytes ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                    ) {
                        AsyncImage(
                            model = bytes ?: track.coverArtUri,
                            contentScale = ContentScale.Crop,
                            contentDescription = context.resources.getString(R.string.new_cover_art),
                            modifier = Modifier
                                .requiredWidthIn(max = 400.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp)
                                .aspectRatio(1f)
                                .clip(ShapeDefaults.Medium)
                        )

                        if (bytes == null) {
                            Button(
                                onClick = onPickCoverArtClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Collections,
                                    contentDescription = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = context.resources.getString(R.string.pick_art)
                                )
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onRestoreCoverArtClick,
                                enabled = state.pickedCoverArtBytes != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = context.resources.getString(R.string.restore_art)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            TagTextField(
                value = title,
                onValueChange = {
                    title = it
                },
                label = context.resources.getString(R.string.title),
                showRestoreButton = title != (track.title ?: ""),
                onRestoreClick = {
                    title = track.title ?: ""
                },
                modifier = Modifier.fillMaxWidth()
            )

            TagTextField(
                value = album,
                onValueChange = {
                    album = it
                },
                label = context.resources.getString(R.string.album),
                showRestoreButton = album != (track.album ?: ""),
                onRestoreClick = {
                    album = track.album ?: ""
                },
                modifier = Modifier.fillMaxWidth()
            )

            TagTextField(
                value = artist,
                onValueChange = {
                    artist = it
                },
                label = context.resources.getString(R.string.artist),
                showRestoreButton = artist != (track.artist ?: ""),
                onRestoreClick = {
                    artist = track.artist ?: ""
                },
                modifier = Modifier.fillMaxWidth()
            )

            TagTextField(
                value = albumArtist,
                onValueChange = {
                    albumArtist = it
                },
                label = context.resources.getString(R.string.album_artist),
                showRestoreButton = albumArtist != (track.albumArtist ?: ""),
                onRestoreClick = {
                    albumArtist = track.albumArtist ?: ""
                },
                modifier = Modifier.fillMaxWidth()
            )

            TagTextField(
                value = genre,
                onValueChange = {
                    genre = it
                },
                label = context.resources.getString(R.string.genre),
                showRestoreButton = genre != (track.genre ?: ""),
                onRestoreClick = {
                    genre = track.genre ?: ""
                },
                modifier = Modifier.fillMaxWidth()
            )

            TagTextField(
                value = year,
                onValueChange = {
                    year = it
                },
                label = context.resources.getString(R.string.year),
                showRestoreButton = year != (track.year ?: ""),
                onRestoreClick = {
                    year = track.year ?: ""
                },
                modifier = Modifier.fillMaxWidth()
            )

            TagTextField(
                value = trackNumber,
                onValueChange = {
                    trackNumber = it
                },
                label = context.resources.getString(R.string.track_number),
                showRestoreButton = trackNumber != (track.trackNumber ?: ""),
                onRestoreClick = {
                    trackNumber = track.trackNumber ?: ""
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = title != (track.title ?: "") || album != (track.album
                ?: "") || artist != (track.artist ?: "") || albumArtist != (track.albumArtist
                ?: "") || genre != (track.genre ?: "") || year != (track.year
                ?: "") || trackNumber != (track.trackNumber
                ?: "") || state.pickedCoverArtBytes != null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .offset(x = (-28).dp, y = (-28).dp)
        ) {
            FloatingActionButton(
                onClick = {
                    onNextClick(
                        Metadata(
                            title = title.trim().takeIf { it.isNotBlank() && it != track.title },
                            album = album.trim().takeIf { it.isNotBlank() && it != track.album },
                            artist = artist.trim().takeIf { it.isNotBlank() && it != track.artist },
                            albumArtist = albumArtist.trim().takeIf { it.isNotBlank() && it != track.albumArtist },
                            genre = genre.trim().takeIf { it.isNotBlank() && it != track.genre },
                            year = year.trim().takeIf { it.isNotBlank() && it != track.year },
                            trackNumber = trackNumber.trim().takeIf { it.isNotBlank() && it != track.trackNumber },
                            coverArtBytes = state.pickedCoverArtBytes
                        )
                    )
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(text = context.resources.getString(R.string.next))
                }
            }
        }
    }
}

@Composable
fun TagTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    showRestoreButton: Boolean = false,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it.trimStart().filter { it != '\n' && it != '\r' })
            },
            label = label?.let {
                {
                    Text(text = label)
                }
            },
            placeholder = placeholder?.let {
                {
                    Text(text = placeholder)
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.weight(1f)
        )

        AnimatedVisibility(
            visible = showRestoreButton
        ) {
            Spacer(modifier = Modifier.width(16.dp))

            val context = LocalContext.current
            IconButton(
                onClick = onRestoreClick
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = context.resources.getString(R.string.restore_tag)
                )
            }
        }

    }
}