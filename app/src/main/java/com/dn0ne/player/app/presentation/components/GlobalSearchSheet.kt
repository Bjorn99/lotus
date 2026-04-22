package com.dn0ne.player.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.filterPlaylists
import com.dn0ne.player.app.domain.track.filterTracks
import com.dn0ne.player.app.presentation.components.trackinfo.SearchField

// One sheet, five sections. The per-tab search in the top bar already
// handles "find within this tab"; this one answers "find anywhere in my
// library", which is otherwise five tab switches + five re-queries.
private const val SECTION_LIMIT = 5
private const val TRACK_LIMIT = 8

@Composable
fun GlobalSearchSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    allTracks: List<Track>,
    playlists: List<Playlist>,
    albumPlaylists: List<Playlist>,
    artistPlaylists: List<Playlist>,
    genrePlaylists: List<Playlist>,
    onTrackClick: (Track, Playlist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onAlbumPlaylistClick: (Playlist) -> Unit,
    onArtistPlaylistClick: (Playlist) -> Unit,
    onGenrePlaylistClick: (Playlist) -> Unit,
) {
    if (!isVisible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            val context = LocalContext.current
            var query by rememberSaveable { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchField(
                        value = query,
                        onValueChange = { query = it.trimStart() },
                        placeholder = context.resources.getString(R.string.search_library),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = context.resources.getString(R.string.close_track_search)
                        )
                    }
                }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                val tracks = remember(allTracks, query) { allTracks.filterTracks(query) }
                val userPlaylistsMatch = remember(playlists, query) {
                    playlists.filterPlaylists(query)
                }
                val albums = remember(albumPlaylists, query) { albumPlaylists.filterPlaylists(query) }
                val artists = remember(artistPlaylists, query) {
                    artistPlaylists.filterPlaylists(query)
                }
                val genres = remember(genrePlaylists, query) { genrePlaylists.filterPlaylists(query) }

                when {
                    query.isBlank() -> EmptyState(
                        icon = Icons.Rounded.Search,
                        text = context.resources.getString(R.string.search_library_hint)
                    )
                    tracks.isEmpty() && userPlaylistsMatch.isEmpty() &&
                        albums.isEmpty() && artists.isEmpty() && genres.isEmpty() ->
                        EmptyState(
                            icon = Icons.Rounded.Search,
                            text = context.resources.getString(R.string.search_no_results)
                        )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (tracks.isNotEmpty()) {
                            item("h-tracks") {
                                SectionHeader(
                                    label = context.resources.getString(R.string.tracks),
                                    count = tracks.size
                                )
                            }
                            items(tracks.take(TRACK_LIMIT), key = { "t-" + it.uri }) { track ->
                                CompactTrackRow(track) {
                                    onTrackClick(
                                        track,
                                        Playlist(name = null, trackList = tracks)
                                    )
                                    onDismiss()
                                }
                            }
                        }
                        if (albums.isNotEmpty()) {
                            item("h-albums") {
                                SectionHeader(
                                    label = context.resources.getString(R.string.albums),
                                    count = albums.size
                                )
                            }
                            items(
                                albums.take(SECTION_LIMIT),
                                key = { "al-" + (it.name ?: "?") }
                            ) { pl ->
                                CompactPlaylistRow(
                                    playlist = pl,
                                    fallbackLabel = context.resources.getString(R.string.unknown_album),
                                    icon = Icons.Rounded.Album
                                ) {
                                    onAlbumPlaylistClick(pl)
                                    onDismiss()
                                }
                            }
                        }
                        if (artists.isNotEmpty()) {
                            item("h-artists") {
                                SectionHeader(
                                    label = context.resources.getString(R.string.artists),
                                    count = artists.size
                                )
                            }
                            items(
                                artists.take(SECTION_LIMIT),
                                key = { "ar-" + (it.name ?: "?") }
                            ) { pl ->
                                CompactPlaylistRow(
                                    playlist = pl,
                                    fallbackLabel = context.resources.getString(R.string.unknown_artist),
                                    icon = Icons.Rounded.Person
                                ) {
                                    onArtistPlaylistClick(pl)
                                    onDismiss()
                                }
                            }
                        }
                        if (genres.isNotEmpty()) {
                            item("h-genres") {
                                SectionHeader(
                                    label = context.resources.getString(R.string.genres),
                                    count = genres.size
                                )
                            }
                            items(
                                genres.take(SECTION_LIMIT),
                                key = { "ge-" + (it.name ?: "?") }
                            ) { pl ->
                                CompactPlaylistRow(
                                    playlist = pl,
                                    fallbackLabel = context.resources.getString(R.string.unknown_genre),
                                    icon = Icons.Rounded.MusicNote
                                ) {
                                    onGenrePlaylistClick(pl)
                                    onDismiss()
                                }
                            }
                        }
                        if (userPlaylistsMatch.isNotEmpty()) {
                            item("h-playlists") {
                                SectionHeader(
                                    label = context.resources.getString(R.string.playlists),
                                    count = userPlaylistsMatch.size
                                )
                            }
                            items(
                                userPlaylistsMatch.take(SECTION_LIMIT),
                                key = { "pl-" + (it.name ?: "?") }
                            ) { pl ->
                                CompactPlaylistRow(
                                    playlist = pl,
                                    fallbackLabel = context.resources.getString(R.string.unknown),
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic
                                ) {
                                    onPlaylistClick(pl)
                                    onDismiss()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactTrackRow(track: Track, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeDefaults.Medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(
            uri = track.coverArtUri,
            modifier = Modifier
                .size(44.dp)
                .clip(ShapeDefaults.Small)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title ?: context.resources.getString(R.string.unknown_title),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = track.artist ?: context.resources.getString(R.string.unknown_artist),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactPlaylistRow(
    playlist: Playlist,
    fallbackLabel: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeDefaults.Medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(ShapeDefaults.Small)
                .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name ?: fallbackLabel,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${playlist.trackList.size}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
