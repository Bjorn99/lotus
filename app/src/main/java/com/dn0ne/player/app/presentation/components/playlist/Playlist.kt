package com.dn0ne.player.app.presentation.components.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.TrackSort
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.filterTracks
import com.dn0ne.player.app.presentation.components.TrackSortButton
import com.dn0ne.player.app.presentation.components.selection.selectionList
import com.dn0ne.player.app.presentation.components.topbar.LazyColumnWithCollapsibleTopBar
import com.dn0ne.player.app.presentation.components.topbar.TopBarContent
import com.dn0ne.player.app.presentation.components.trackList
import com.dn0ne.player.app.presentation.components.trackinfo.SearchField

@Composable
fun Playlist(
    listState: LazyListState = rememberLazyListState(),
    playlist: Playlist,
    currentTrack: Track?,
    onTrackClick: (Track, Playlist) -> Unit,
    onPlayNextClick: (Track) -> Unit,
    onAddToQueueClick: (List<Track>) -> Unit,
    onAddToPlaylistClick: (List<Track>) -> Unit,
    onViewTrackInfoClick: (Track) -> Unit,
    onGoToAlbumClick: (Track) -> Unit,
    onGoToArtistClick: (Track) -> Unit,
    trackSort: TrackSort,
    trackSortOrder: SortOrder,
    onTrackSortChange: (TrackSort?, SortOrder?) -> Unit,
    onBackClick: () -> Unit,
    replaceSearchWithFilter: Boolean
) {
    val context = LocalContext.current

    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }

    BackHandler {
        onBackClick()
    }

    var searchFieldValue by rememberSaveable {
        mutableStateOf("")
    }
    var showSearchField by rememberSaveable {
        mutableStateOf(false)
    }

    var isInSelectionMode by remember {
        mutableStateOf(false)
    }
    val selectedTracks = remember {
        mutableStateListOf<Track>()
    }

    val topBarContent by remember {
        derivedStateOf {
            when {
                showSearchField && isInSelectionMode -> TopBarContent.Search
                showSearchField -> TopBarContent.Search
                isInSelectionMode -> TopBarContent.Selection
                else -> TopBarContent.Default
            }
        }
    }

    LazyColumnWithCollapsibleTopBar(
        listState = listState,
        topBarContent = {
            Text(
                text = playlist.name
                    ?: context.resources.getString(R.string.unknown),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = lerp(
                        start = MaterialTheme.typography.titleLarge.fontSize,
                        stop = MaterialTheme.typography.headlineLarge.fontSize,
                        fraction = collapseFraction
                    ),
                ),
                softWrap = collapseFraction > .2f,
                overflow = if (collapseFraction > .2f) {
                    TextOverflow.Clip
                } else TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = if (collapseFraction > .2f) 28.dp else 108.dp)
            )

            AnimatedContent(
                targetState = topBarContent,
                label = "top-bar-search-bar-animation",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .align(Alignment.BottomCenter)
            ) { state ->
                when (state) {
                    TopBarContent.Default -> {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                IconButton(
                                    onClick = onBackClick
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowBackIosNew,
                                        contentDescription = context.resources.getString(
                                            R.string.back
                                        )
                                    )
                                }

                                TrackSortButton(
                                    sort = trackSort,
                                    order = trackSortOrder,
                                    onSortChange = {
                                        onTrackSortChange(it, null)
                                    },
                                    onSortOrderChange = {
                                        onTrackSortChange(null, it)
                                    }
                                )
                            }

                            Row {
                                val exportM3u = rememberM3uExport()
                                IconButton(
                                    onClick = {
                                        exportM3u(
                                            playlist.name
                                                ?: context.resources.getString(R.string.unknown),
                                            playlist.trackList
                                        )
                                    },
                                    enabled = playlist.trackList.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = context.resources.getString(
                                            R.string.export_m3u
                                        )
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        showSearchField = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (replaceSearchWithFilter) {
                                            Icons.Rounded.FilterList
                                        } else Icons.Rounded.Search,
                                        contentDescription = context.resources.getString(
                                            R.string.track_search
                                        )
                                    )
                                }
                            }
                        }
                    }

                    TopBarContent.Search -> {
                        BackHandler {
                            showSearchField = false
                            searchFieldValue = ""
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val focusRequester = remember {
                                FocusRequester()
                            }
                            SearchField(
                                value = searchFieldValue,
                                onValueChange = {
                                    searchFieldValue = it.trimStart()
                                },
                                icon = if (replaceSearchWithFilter) {
                                    Icons.Rounded.FilterList
                                } else Icons.Rounded.Search,
                                placeholder = if (replaceSearchWithFilter) {
                                    context.resources.getString(R.string.filter)
                                } else context.resources.getString(R.string.search),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp)
                                    .align(Alignment.Center)
                                    .focusRequester(focusRequester)
                            )

                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }

                            IconButton(
                                onClick = {
                                    showSearchField = false
                                    searchFieldValue = ""
                                },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = context.resources.getString(
                                        R.string.close_track_search
                                    )
                                )
                            }
                        }
                    }

                    TopBarContent.Selection -> {
                        BackHandler {
                            isInSelectionMode = false
                            selectedTracks.clear()
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        isInSelectionMode = false
                                        selectedTracks.clear()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = context.resources.getString(R.string.back)
                                    )
                                }

                                Text(
                                    text = selectedTracks.size.toString(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Row {
                                if (selectedTracks.size < playlist.trackList.size) {
                                    IconButton(
                                        onClick = {
                                            selectedTracks.clear()
                                            selectedTracks.addAll(playlist.trackList)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SelectAll,
                                            contentDescription = context.resources.getString(R.string.select_all)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        onAddToQueueClick(selectedTracks.toList())
                                        isInSelectionMode = false
                                        selectedTracks.clear()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddToQueue,
                                        contentDescription = context.resources.getString(R.string.add_to_queue)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onAddToPlaylistClick(selectedTracks.toList())
                                        isInSelectionMode = false
                                        selectedTracks.clear()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                        contentDescription = context.resources.getString(R.string.add_to_playlist)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        showSearchField = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (replaceSearchWithFilter) {
                                            Icons.Rounded.FilterList
                                        } else Icons.Rounded.Search,
                                        contentDescription = context.resources.getString(
                                            R.string.track_search
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        collapseFraction = {
            collapseFraction = it
        },
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        if (!isInSelectionMode) {
            trackList(
                trackList = playlist.trackList.filterTracks(searchFieldValue),
                currentTrack = currentTrack,
                onTrackClick = { track ->
                    onTrackClick(
                        track,
                        if (replaceSearchWithFilter) {
                            playlist.copy(
                                trackList = playlist.trackList.filterTracks(searchFieldValue)
                            )
                        } else playlist
                    )
                },
                onPlayNextClick = onPlayNextClick,
                onAddToQueueClick = { onAddToQueueClick(listOf(it)) },
                onAddToPlaylistClick = { onAddToPlaylistClick(listOf(it)) },
                onViewTrackInfoClick = onViewTrackInfoClick,
                onGoToAlbumClick = onGoToAlbumClick,
                onGoToArtistClick = onGoToArtistClick,
                onLongClick = {
                    isInSelectionMode = true
                    selectedTracks.add(it)
                }
            )
        } else {
            selectionList(
                trackList = playlist.trackList.filterTracks(searchFieldValue),
                selectedTracks = selectedTracks,
                onTrackClick = {
                    if (it in selectedTracks) {
                        selectedTracks.remove(it)
                    } else selectedTracks.add(it)

                    if (selectedTracks.isEmpty()) {
                        isInSelectionMode = false
                    }
                }
            )
        }
    }
}