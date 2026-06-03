package com.dn0ne.player.app.presentation.components.trackinfo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowRightAlt
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil3.compose.AsyncImage
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.metadata.Metadata
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import com.dn0ne.player.app.presentation.components.NoteCard

@Composable
fun ChangesSheet(
    track: Track,
    state: ChangesSheetState,
    isMetadataWritable: Boolean,
    onBackClick: () -> Unit,
    onOverwriteClick: (Metadata) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }
    var metadataToOverwrite by remember {
        mutableStateOf(state.metadata)
    }

    Box {
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
                        contentDescription = context.resources.getString(R.string.back)
                    )
                }

                Text(
                    text = context.resources.getString(R.string.changes),
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
            contentPadding = PaddingValues(horizontal = 16.dp),
            contentHorizontalAlignment = Alignment.CenterHorizontally,
            contentVerticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Text(
                text = context.resources.getString(R.string.select_fields_to_overwrite),
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 12.dp)
            )

            state.metadata.run {
                AnimatedVisibility(
                    visible = state.isLoadingArt || coverArtBytes != null
                ) {
                    CoverArtChange(
                        isLoadingArt = state.isLoadingArt,
                        isArtFromGallery = state.isArtFromGallery,
                        oldModel = track.coverArtUri,
                        newModel = coverArtBytes,
                        overwriteChecked = metadataToOverwrite.coverArtBytes != null,
                        onCheckedChange = {
                            metadataToOverwrite = metadataToOverwrite.copy(
                                coverArtBytes = if (it) coverArtBytes else null
                            )
                        }
                    )
                }

                title?.let {
                    TagChangesRow(
                        tag = context.resources.getString(R.string.title),
                        oldValue = track.title
                            ?: context.resources.getString(R.string.unknown_title),
                        newValue = it,
                        overwriteChecked = metadataToOverwrite.title != null,
                        onCheckedChange = { isChecked ->
                            metadataToOverwrite = metadataToOverwrite.copy(
                                title = if (isChecked) it else null
                            )
                        }
                    )
                }

                album?.let {
                    TagChangesRow(
                        tag = context.resources.getString(R.string.album),
                        oldValue = track.album
                            ?: context.resources.getString(R.string.unknown_album),
                        newValue = it,
                        overwriteChecked = metadataToOverwrite.album != null,
                        onCheckedChange = { isChecked ->
                            metadataToOverwrite = metadataToOverwrite.copy(
                                album = if (isChecked) it else null
                            )
                        }
                    )
                }

                artist?.let {
                    TagChangesRow(
                        tag = context.resources.getString(R.string.artist),
                        oldValue = track.artist
                            ?: context.resources.getString(R.string.unknown_artist),
                        newValue = it,
                        overwriteChecked = metadataToOverwrite.artist != null,
                        onCheckedChange = { isChecked ->
                            metadataToOverwrite = metadataToOverwrite.copy(
                                artist = if (isChecked) it else null
                            )
                        }
                    )
                }

                albumArtist?.let {
                    TagChangesRow(
                        tag = context.resources.getString(R.string.album_artist),
                        oldValue = track.albumArtist
                            ?: context.resources.getString(R.string.unknown_album_artist),
                        newValue = it,
                        overwriteChecked = metadataToOverwrite.albumArtist != null,
                        onCheckedChange = { isChecked ->
                            metadataToOverwrite = metadataToOverwrite.copy(
                                albumArtist = if (isChecked) it else null
                            )
                        }
                    )
                }

                genre?.let {
                    TagChangesRow(
                        tag = context.resources.getString(R.string.genre),
                        oldValue = track.genre
                            ?: context.resources.getString(R.string.unknown_genre),
                        newValue = it,
                        overwriteChecked = metadataToOverwrite.genre != null,
                        onCheckedChange = { isChecked ->
                            metadataToOverwrite = metadataToOverwrite.copy(
                                genre = if (isChecked) it else null
                            )
                        }
                    )
                }

                year?.let {
                    TagChangesRow(
                        tag = context.resources.getString(R.string.year),
                        oldValue = track.year ?: context.resources.getString(R.string.unknown_year),
                        newValue = it,
                        overwriteChecked = metadataToOverwrite.year != null,
                        onCheckedChange = { isChecked ->
                            metadataToOverwrite = metadataToOverwrite.copy(
                                year = if (isChecked) it else null
                            )
                        }
                    )
                }

                trackNumber?.let {
                    TagChangesRow(
                        tag = context.resources.getString(R.string.track_number),
                        oldValue = track.trackNumber
                            ?: context.resources.getString(R.string.unknown_track_number),
                        newValue = it,
                        overwriteChecked = metadataToOverwrite.trackNumber != null,
                        onCheckedChange = { isChecked ->
                            metadataToOverwrite = metadataToOverwrite.copy(
                                trackNumber = if (isChecked) it else null
                            )
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (isMetadataWritable) {
                    onOverwriteClick(metadataToOverwrite)
                }
            },
            containerColor = if (isMetadataWritable)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .offset(x = (-28).dp, y = (-28).dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = if (isMetadataWritable)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isMetadataWritable)
                        context.resources.getString(R.string.overwrite)
                    else
                        context.resources.getString(R.string.format_not_supported_for_writing),
                    color = if (isMetadataWritable)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CoverArtChange(
    isLoadingArt: Boolean,
    isArtFromGallery: Boolean,
    oldModel: Any,
    newModel: Any?,
    overwriteChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(ShapeDefaults.Medium)
                .clickable {
                    onCheckedChange(!overwriteChecked)
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val context = LocalContext.current

            Checkbox(
                checked = overwriteChecked,
                onCheckedChange = onCheckedChange
            )

            AsyncImage(
                model = oldModel,
                contentDescription = context.resources.getString(R.string.old_cover_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .weight(1f)
                    .clip(ShapeDefaults.Small)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowRightAlt,
                contentDescription = null
            )

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .weight(1f)
                    .clip(ShapeDefaults.Small),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isLoadingArt,
                    label = "New cover art enter animation",
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                ) { isLoading ->
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        AsyncImage(
                            model = newModel,
                            contentDescription = context.resources.getString(R.string.new_cover_art),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val context = LocalContext.current
        NoteCard(
            label = context.resources.getString(R.string.important_to_know),
            leadingIcon = Icons.Rounded.Lightbulb,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = context.resources.getString(R.string.artwork_change_note) + if (isArtFromGallery) {
                    "\n\n" + context.resources.getString(R.string.cover_art_from_gallery_note)
                } else ""
            )
        }
    }
}

@Composable
fun TagChangesRow(
    tag: String,
    oldValue: String,
    newValue: String,
    overwriteChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeDefaults.Medium)
            .clickable {
                onCheckedChange(!overwriteChecked)
            }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = overwriteChecked,
                onCheckedChange = onCheckedChange
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = tag,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = oldValue,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    textAlign = TextAlign.End,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowRightAlt,
                    contentDescription = null
                )
            }

            Text(
                text = newValue,
                textAlign = TextAlign.End
            )
        }
    }
}