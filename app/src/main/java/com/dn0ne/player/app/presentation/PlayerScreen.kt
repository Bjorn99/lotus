package com.dn0ne.player.app.presentation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dn0ne.player.R
import com.dn0ne.player.app.data.CoverArtColorExtractor
import com.dn0ne.player.app.domain.sort.PlaylistSort
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.TrackSort
import com.dn0ne.player.app.domain.metadata.MetadataSearchResult
import com.dn0ne.player.app.domain.playlist.SmartPlaylists
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.filterPlaylists
import com.dn0ne.player.app.domain.track.filterTracks
import com.dn0ne.player.app.presentation.components.GlobalSearchSheet
import com.dn0ne.player.app.presentation.components.LocalDominantColorCache
import com.dn0ne.player.app.presentation.components.PlaylistSortButton
import com.dn0ne.player.app.presentation.components.TrackSortButton
import com.dn0ne.player.app.presentation.components.playback.PlayerSheet
import com.dn0ne.player.app.presentation.components.playlist.AddToOrCreatePlaylistBottomSheet
import com.dn0ne.player.app.presentation.components.playlist.DeletePlaylistDialog
import com.dn0ne.player.app.presentation.components.playlist.PlaylistSectionHeader
import com.dn0ne.player.app.presentation.components.playlist.MutablePlaylist
import com.dn0ne.player.app.presentation.components.playlist.Playlist
import com.dn0ne.player.app.presentation.components.playlist.RenamePlaylistBottomSheet
import com.dn0ne.player.app.presentation.components.playlist.playlistCards
import com.dn0ne.player.app.presentation.components.playlist.playlistRows
import com.dn0ne.player.app.presentation.components.selection.selectionCards
import com.dn0ne.player.app.presentation.components.selection.selectionList
import com.dn0ne.player.app.presentation.components.selection.selectionRows
import com.dn0ne.player.app.presentation.components.settings.SettingsSheet
import com.dn0ne.player.app.presentation.components.settings.Theme
import com.dn0ne.player.app.presentation.components.topbar.LazyGridWithCollapsibleTabsTopBar
import com.dn0ne.player.app.presentation.components.topbar.Tab
import com.dn0ne.player.app.presentation.components.topbar.TopBarContent
import com.dn0ne.player.app.presentation.components.trackList
import com.dn0ne.player.app.presentation.components.trackinfo.SearchField
import com.dn0ne.player.app.presentation.components.trackinfo.TrackInfoSheet
import androidx.compose.ui.graphics.toArgb
import com.kmpalette.color
import com.kmpalette.rememberDominantColorState
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.toHct
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onCoverArtPick: () -> Unit,
    onFolderPick: (scan: Boolean) -> Unit,
    onSidecarFolderPick: () -> Unit,
    onLyricsPick: () -> Unit,
    onPlaylistPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val useDynamicColor by viewModel.settings.useDynamicColor.collectAsState()
    val useAlbumArtColor by viewModel.settings.useAlbumArtColor.collectAsState()
    val dominantColorCache by viewModel.dominantColorCache.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentTrack by remember {
        derivedStateOf { playbackState.currentTrack }
    }
    val dominantColorState = rememberDominantColorState()
    var coverArtBitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    var coverArtBitmapTrackUri by remember {
        mutableStateOf<Uri?>(null)
    }
    val cachedColor = currentTrack?.let { dominantColorCache[it.coverArtUri.toString()] }
    val bitmapIsFresh = currentTrack?.uri == coverArtBitmapTrackUri
    val colorToApply by remember(cachedColor, coverArtBitmap, coverArtBitmapTrackUri, useAlbumArtColor, useDynamicColor) {
        derivedStateOf {
            if (useAlbumArtColor && cachedColor != null) {
                Color(cachedColor)
            } else if (useAlbumArtColor && coverArtBitmap != null && bitmapIsFresh) {
                dominantColorState.result
                    ?.paletteOrNull
                    ?.let { CoverArtColorExtractor.selectDominantColor(it) }
                    ?: dominantColorState.color
            } else dominantColorState.color
        }
    }

    LaunchedEffect(useAlbumArtColor, useDynamicColor, coverArtBitmap, coverArtBitmapTrackUri) {
        val bitmap = coverArtBitmap
        if (useAlbumArtColor && bitmap != null && coverArtBitmapTrackUri == currentTrack?.uri) {
            dominantColorState.updateFrom(bitmap)
            val computedColor = dominantColorState.result
                ?.paletteOrNull
                ?.let { CoverArtColorExtractor.selectDominantColor(it) }
                ?: dominantColorState.color
            currentTrack?.let { track ->
                viewModel.cacheDominantColor(track.coverArtUri.toString(), computedColor.toArgb())
            }
        } else {
            dominantColorState.reset()
        }
    }

    LaunchedEffect(currentTrack) {
        if (currentTrack == null) {
            coverArtBitmap = null
            coverArtBitmapTrackUri = null
            dominantColorState.reset()
        }
    }

    val appearance by viewModel.settings.appearance.collectAsState()
    val amoledDarkTheme by viewModel.settings.amoledDarkTheme.collectAsState()
    val paletteStyle by viewModel.settings.paletteStyle.collectAsState()
    DynamicMaterialTheme(
        seedColor = colorToApply,
        primary = colorToApply.takeIf { it.toHct().chroma <= 20 },
        isDark = when (appearance) {
            Theme.Appearance.System -> isSystemInDarkTheme()
            Theme.Appearance.Light -> false
            Theme.Appearance.Dark -> true
        },
        isAmoled = amoledDarkTheme,
        style = when (paletteStyle) {
            Theme.PaletteStyle.TonalSpot -> PaletteStyle.TonalSpot
            Theme.PaletteStyle.Neutral -> PaletteStyle.Neutral
            Theme.PaletteStyle.Vibrant -> PaletteStyle.Vibrant
            Theme.PaletteStyle.Expressive -> PaletteStyle.Expressive
            Theme.PaletteStyle.Rainbow -> PaletteStyle.Rainbow
            Theme.PaletteStyle.FruitSalad -> PaletteStyle.FruitSalad
            Theme.PaletteStyle.Monochrome -> PaletteStyle.Monochrome
            Theme.PaletteStyle.Fidelity -> PaletteStyle.Fidelity
            Theme.PaletteStyle.Content -> PaletteStyle.Content
        },
        animationSpec = tween<Color>(300, 200),
        animate = true
    ) {
        val rippleColor = MaterialTheme.colorScheme.primaryContainer
        val ripple = remember(rippleColor) {
            ripple(color = rippleColor)
        }
        val rippleConfiguration = remember(rippleColor) {
            RippleConfiguration(color = rippleColor)
        }
        CompositionLocalProvider(
            LocalIndication provides ripple,
            LocalRippleConfiguration provides rippleConfiguration,
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            LocalDominantColorCache provides dominantColorCache,
        ) {

            Box(
                modifier = modifier
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                val context = LocalContext.current
                val perTrackArtwork = viewModel.settings.perTrackArtwork
                val trackSort by viewModel.trackSort.collectAsState()
                val trackSortOrder by viewModel.trackSortOrder.collectAsState()
                val playlistSort by viewModel.playlistSort.collectAsState()
                val playlistSortOrder by viewModel.playlistSortOrder.collectAsState()
                val albumSort by viewModel.albumSort.collectAsState()
                val albumSortOrder by viewModel.albumSortOrder.collectAsState()
                val artistSort by viewModel.artistSort.collectAsState()
                val artistSortOrder by viewModel.artistSortOrder.collectAsState()
                val genreSort by viewModel.genreSort.collectAsState()
                val genreSortOrder by viewModel.genreSortOrder.collectAsState()
                val folderSort by viewModel.folderSort.collectAsState()
                val folderSortOrder by viewModel.folderSortOrder.collectAsState()

                // Hoisted to PlayerScreen scope so the NavHost composables and
                // PlayerSheet (rendered outside the NavHost) can all read the
                // same Set without duplicate collection.
                val lovedUris by viewModel.lovedUris.collectAsState()

                val replaceSearchWithFilter by viewModel.settings
                    .replaceSearchWithFilter.collectAsState()

                var showAddToOrCreatePlaylistSheet by rememberSaveable {
                    mutableStateOf(false)
                }
                var showCreatePlaylistOnly by rememberSaveable {
                    mutableStateOf(false)
                }
                var tracksToAddToPlaylist by remember {
                    mutableStateOf<List<Track>?>(null)
                }

                val navController = rememberNavController()

                var showScrollToTopButton by remember {
                    mutableStateOf(false)
                }
                var onScrollToTopClick by remember {
                    mutableStateOf(suspend {})
                }
                var showLocateButton by remember {
                    mutableStateOf(false)
                }
                var onLocateClick by remember {
                    mutableStateOf(suspend {})
                }

                NavHost(
                    navController = navController,
                    enterTransition = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                slideInVertically(initialOffsetY = { it / 10 })
                    },
                    exitTransition = {
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                slideOutVertically(targetOffsetY = { -it / 10 })
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                slideInVertically(initialOffsetY = { -it / 10 })
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                slideOutVertically(targetOffsetY = { it / 10 })
                    },
                    startDestination = PlayerRoutes.Main
                ) {
                    composable<PlayerRoutes.Main> {
                        val trackList by viewModel.trackList.collectAsState()
                        val playlists by viewModel.playlists.collectAsState()
                        val albumPlaylists by viewModel.albumPlaylists.collectAsState()
                        val artistPlaylists by viewModel.artistPlaylists.collectAsState()
                        val genrePlaylists by viewModel.genrePlaylists.collectAsState()
                        val folderPlaylists by viewModel.folderPlaylists.collectAsState()

                        // Smart playlists are derived here (not in the VM)
                        // so the localised names come from the Compose
                        // resource lookup without forcing a Context into
                        // PlayerViewModel's constructor. Recomputed only
                        // when trackList changes — "Random Mix" stays
                        // stable within a session.
                        val recentlyAddedName = stringResource(R.string.smart_recently_added)
                        val randomMixName = stringResource(R.string.smart_random_mix)
                        val lovedName = stringResource(R.string.smart_loved)
                        val smartPlaylists = remember(
                            trackList, lovedUris, recentlyAddedName, randomMixName, lovedName
                        ) {
                            val seed = System.currentTimeMillis()
                            buildList {
                                SmartPlaylists
                                    .loved(trackList, lovedUris, lovedName)
                                    ?.let(::add)
                                SmartPlaylists
                                    .recentlyAdded(trackList, recentlyAddedName)
                                    ?.let(::add)
                                SmartPlaylists
                                    .randomMix(trackList, randomMixName, seed)
                                    ?.let(::add)
                            }
                        }

                        val gridState = rememberLazyGridState()
                        val gridPlaylists by viewModel.settings.gridPlaylists.collectAsState()

                        val tabs by viewModel.settings.tabs.collectAsState()
                        var selectedTab by rememberSaveable {
                            mutableStateOf(viewModel.settings.defaultTab)
                        }

                        val shouldShowLocateButton by remember(currentTrack, trackList) {
                            derivedStateOf {
                                selectedTab == Tab.Tracks &&
                                        currentTrack != null &&
                                        gridState.layoutInfo.visibleItemsInfo.fastFirstOrNull {
                                            it.index == trackList.indexOf(currentTrack)
                                        } == null
                            }
                        }
                        onLocateClick = remember(currentTrack, trackList) {
                            {
                                val currentTrackIndex = trackList.indexOf(currentTrack)
                                val preAnimateItemIndex = if (
                                    gridState.firstVisibleItemIndex < currentTrackIndex
                                ) {
                                    (currentTrackIndex - 5).coerceAtLeast(0)
                                } else currentTrackIndex + 5
                                gridState.scrollToItem(preAnimateItemIndex)
                                gridState.animateScrollToItem(currentTrackIndex)
                            }
                        }
                        LaunchedEffect(shouldShowLocateButton) {
                            showLocateButton = shouldShowLocateButton
                        }

                        val isScrolledEnough by remember {
                            derivedStateOf {
                                gridState.firstVisibleItemIndex >= 5
                            }
                        }
                        onScrollToTopClick = {
                            gridState.scrollToItem(5)
                            gridState.animateScrollToItem(0)
                        }

                        LaunchedEffect(isScrolledEnough) {
                            showScrollToTopButton = isScrolledEnough
                        }

                        MainPlayerScreen(
                            gridState = gridState,
                            topBarTabs = tabs,
                            defaultTab = viewModel.settings.defaultTab,
                            onTabChange = {
                                selectedTab = it
                            },
                            trackList = trackList,
                            currentTrack = currentTrack,
                            onTrackClick = { track, playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnTrackClick(
                                        track = track,
                                        playlist = playlist
                                    )
                                )
                            },
                            onPlayNextClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnPlayNextClick(it))
                            },
                            onAddToQueueClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnAddToQueueClick(it))
                            },
                            onAddToPlaylistClick = {
                                showAddToOrCreatePlaylistSheet = true
                                showCreatePlaylistOnly = false
                                tracksToAddToPlaylist = it
                            },
                            onViewTrackInfoClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnViewTrackInfoClick(it))
                            },
                            onGoToAlbumClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnGoToAlbumClick(it))
                                navController.popBackStack(PlayerRoutes.Main, false)
                                navController.navigate(PlayerRoutes.Playlist)
                            },
                            lovedUris = lovedUris,
                            onToggleLovedClick = { viewModel.toggleLoved(it) },
                            onGoToArtistClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnGoToArtistClick(it))
                                navController.popBackStack(PlayerRoutes.Main, false)
                                navController.navigate(PlayerRoutes.Playlist)
                            },
                            playlists = playlists,
                            smartPlaylists = smartPlaylists,
                            albumPlaylists = albumPlaylists,
                            artistPlaylists = artistPlaylists,
                            genrePlaylists = genrePlaylists,
                            folderPlaylists = folderPlaylists,
                            trackSort = trackSort,
                            trackSortOrder = trackSortOrder,
                            playlistSort = playlistSort,
                            playlistSortOrder = playlistSortOrder,
                            albumSort = albumSort,
                            albumSortOrder = albumSortOrder,
                            artistSort = artistSort,
                            artistSortOrder = artistSortOrder,
                            genreSort = genreSort,
                            genreSortOrder = genreSortOrder,
                            folderSort = folderSort,
                            folderSortOrder = folderSortOrder,
                            onTrackSortChange = { sort, order ->
                                viewModel.onEvent(PlayerScreenEvent.OnTrackSortChange(sort, order))
                            },
                            onPlaylistSortChange = { sort, order ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSortChange(
                                        sort,
                                        order
                                    )
                                )
                            },
                            onAlbumSortChange = { sort, order ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnAlbumSortChange(sort, order)
                                )
                            },
                            onArtistSortChange = { sort, order ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnArtistSortChange(sort, order)
                                )
                            },
                            onGenreSortChange = { sort, order ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnGenreSortChange(sort, order)
                                )
                            },
                            onFolderSortChange = { sort, order ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnFolderSortChange(sort, order)
                                )
                            },
                            onPlaylistSelection = { playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSelection(
                                        playlist.copy(
                                            name = playlist.name
                                                ?: context.resources.getString(R.string.unknown)
                                        )
                                    )
                                )
                                navController.navigate(PlayerRoutes.MutablePlaylist)
                            },
                            onAlbumPlaylistSelection = { playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSelection(
                                        playlist.copy(
                                            name = playlist.name
                                                ?: context.resources.getString(R.string.unknown_album)
                                        )
                                    )
                                )
                                navController.navigate(PlayerRoutes.Playlist)
                            },
                            onArtistPlaylistSelection = { playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSelection(
                                        playlist.copy(
                                            name = playlist.name
                                                ?: context.resources.getString(R.string.unknown_artist)
                                        )
                                    )
                                )
                                navController.navigate(PlayerRoutes.Playlist)
                            },
                            onGenrePlaylistSelection = { playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSelection(
                                        playlist.copy(
                                            name = playlist.name
                                                ?: context.resources.getString(R.string.unknown_genre)
                                        )
                                    )
                                )
                                navController.navigate(PlayerRoutes.Playlist)
                            },
                            onFolderPlaylistSelection = { playlist ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnPlaylistSelection(
                                        playlist
                                    )
                                )
                                navController.navigate(PlayerRoutes.Playlist)
                            },
                            onSettingsClick = {
                                viewModel.onEvent(PlayerScreenEvent.OnSettingsClick)
                            },
                            replaceSearchWithFilter = replaceSearchWithFilter,
                            gridPlaylists = gridPlaylists,
                            onGridPlaylistsClick = {
                                viewModel.settings.updateGridPlaylists(
                                    !gridPlaylists
                                )
                            },
                            perTrackArtwork = perTrackArtwork,
                            onDominantColorExtracted = { uri, color ->
                                viewModel.cacheDominantColor(uri, color)
                            },
                        )
                    }

                    composable<PlayerRoutes.Playlist> {
                        val listState = rememberLazyListState()
                        val isScrolledEnough by remember {
                            derivedStateOf {
                                listState.firstVisibleItemIndex >= 5
                            }
                        }
                        onScrollToTopClick = {
                            listState.scrollToItem(5)
                            listState.animateScrollToItem(0)
                        }

                        LaunchedEffect(isScrolledEnough) {
                            showScrollToTopButton = isScrolledEnough
                        }

                        val playlist by viewModel.selectedPlaylist.collectAsState()
                        playlist?.let { playlist ->
                            val shouldShowLocateButton by remember(currentTrack, playlist) {
                                derivedStateOf {
                                    val index = playlist.trackList.indexOf(currentTrack)
                                    currentTrack != null &&
                                            index >= 0 &&
                                            listState.layoutInfo.visibleItemsInfo.fastFirstOrNull {
                                                it.index == index
                                            } == null
                                }
                            }
                            onLocateClick = remember(currentTrack, playlist) {
                                {
                                    val currentTrackIndex = playlist.trackList.indexOf(currentTrack)
                                    val preAnimateItemIndex = if (
                                        listState.firstVisibleItemIndex < currentTrackIndex
                                    ) {
                                        (currentTrackIndex - 5).coerceAtLeast(0)
                                    } else currentTrackIndex + 5
                                    listState.scrollToItem(preAnimateItemIndex)
                                    listState.animateScrollToItem(currentTrackIndex)
                                }
                            }
                            LaunchedEffect(shouldShowLocateButton) {
                                showLocateButton = shouldShowLocateButton
                            }

                            Playlist(
                                listState = listState,
                                playlist = playlist,
                                currentTrack = currentTrack,
                                lovedUris = lovedUris,
                                perTrackArtwork = perTrackArtwork,
                                onTrackClick = { track, playlist ->
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnTrackClick(
                                            track,
                                            playlist
                                        )
                                    )
                                },
                                onToggleLovedClick = { viewModel.toggleLoved(it) },
                                onPlayNextClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnPlayNextClick(it))
                                },
                                onAddToQueueClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnAddToQueueClick(it))
                                },
                                onAddToPlaylistClick = {
                                    showAddToOrCreatePlaylistSheet = true
                                    showCreatePlaylistOnly = false
                                    tracksToAddToPlaylist = it
                                },
                                onViewTrackInfoClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnViewTrackInfoClick(it))
                                },
                                onGoToAlbumClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnGoToAlbumClick(it))
                                    navController.popBackStack(PlayerRoutes.Main, false)
                                    navController.navigate(PlayerRoutes.Playlist)
                                },
                                onGoToArtistClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnGoToArtistClick(it))
                                    navController.popBackStack(PlayerRoutes.Main, false)
                                    navController.navigate(PlayerRoutes.Playlist)
                                },
                                trackSort = trackSort,
                                trackSortOrder = trackSortOrder,
                                onTrackSortChange = { sort, order ->
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnTrackSortChange(
                                            sort,
                                            order
                                        )
                                    )
                                },
                                onBackClick = {
                                    navController.navigateUp()
                                },
                                replaceSearchWithFilter = replaceSearchWithFilter
                            )
                        }
                    }

                    composable<PlayerRoutes.MutablePlaylist> {
                        var showRenameSheet by remember {
                            mutableStateOf(false)
                        }
                        var showDeleteDialog by remember {
                            mutableStateOf(false)
                        }

                        val listState = rememberLazyListState()
                        val isScrolledEnough by remember {
                            derivedStateOf {
                                listState.firstVisibleItemIndex >= 5
                            }
                        }
                        onScrollToTopClick = {
                            listState.scrollToItem(5)
                            listState.animateScrollToItem(0)
                        }

                        LaunchedEffect(isScrolledEnough) {
                            showScrollToTopButton = isScrolledEnough
                        }

                        val playlists by viewModel.playlists.collectAsState()
                        val playlist by viewModel.selectedPlaylist.collectAsState()
                        playlist?.let { playlist ->
                            var changedTrackList by remember {
                                mutableStateOf(playlist.trackList)
                            }
                            val shouldShowLocateButton by remember(currentTrack, changedTrackList) {
                                derivedStateOf {
                                    val index = changedTrackList.indexOf(currentTrack)
                                    currentTrack != null &&
                                            index >= 0 &&
                                            listState.layoutInfo.visibleItemsInfo.fastFirstOrNull {
                                                it.index == index
                                            } == null
                                }
                            }
                            onLocateClick = remember(currentTrack, changedTrackList) {
                                {
                                    val currentTrackIndex = changedTrackList.indexOf(currentTrack)
                                    val preAnimateItemIndex = if (
                                        listState.firstVisibleItemIndex < currentTrackIndex
                                    ) {
                                        (currentTrackIndex - 5).coerceAtLeast(0)
                                    } else currentTrackIndex + 5
                                    listState.scrollToItem(preAnimateItemIndex)
                                    listState.animateScrollToItem(currentTrackIndex)
                                }
                            }
                            LaunchedEffect(shouldShowLocateButton) {
                                showLocateButton = shouldShowLocateButton
                            }

                            MutablePlaylist(
                                listState = listState,
                                playlist = playlist,
                                currentTrack = currentTrack,
                                lovedUris = lovedUris,
                                perTrackArtwork = perTrackArtwork,
                                onRenamePlaylistClick = {
                                    showRenameSheet = true
                                },
                                onDeletePlaylistClick = {
                                    showDeleteDialog = true
                                },
                                onTrackClick = { track, playlist ->
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnTrackClick(
                                            track,
                                            playlist
                                        )
                                    )
                                },
                                onToggleLovedClick = { viewModel.toggleLoved(it) },
                                onPlayNextClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnPlayNextClick(it))
                                },
                                onAddToQueueClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnAddToQueueClick(it))
                                },
                                onAddToPlaylistClick = {
                                    showAddToOrCreatePlaylistSheet = true
                                    showCreatePlaylistOnly = false
                                    tracksToAddToPlaylist = it
                                },
                                onRemoveFromPlaylistClick = {
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnRemoveFromPlaylist(
                                            it,
                                            playlist
                                        )
                                    )
                                },
                                onViewTrackInfoClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnViewTrackInfoClick(it))
                                },
                                onGoToAlbumClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnGoToAlbumClick(it))
                                    navController.popBackStack(PlayerRoutes.Main, false)
                                    navController.navigate(PlayerRoutes.Playlist)
                                },
                                onGoToArtistClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnGoToArtistClick(it))
                                    navController.popBackStack(PlayerRoutes.Main, false)
                                    navController.navigate(PlayerRoutes.Playlist)
                                },
                                onTrackListReorder = {
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnPlaylistReorder(
                                            it,
                                            playlist
                                        )
                                    )
                                    changedTrackList = it
                                },
                                onBackClick = {
                                    navController.navigateUp()
                                },
                                replaceSearchWithFilter = replaceSearchWithFilter
                            )

                            if (showRenameSheet) {
                                RenamePlaylistBottomSheet(
                                    playlists = playlists,
                                    initialName = playlist.name ?: "",
                                    onRenameClick = {
                                        viewModel.onEvent(
                                            PlayerScreenEvent.OnRenamePlaylistClick(
                                                it,
                                                playlist
                                            )
                                        )
                                    },
                                    onDismissRequest = {
                                        showRenameSheet = false
                                    }
                                )
                            }

                            if (showDeleteDialog) {
                                DeletePlaylistDialog(
                                    onConfirm = {
                                        showDeleteDialog = false
                                        navController.navigateUp()
                                        viewModel.onEvent(
                                            PlayerScreenEvent.OnDeletePlaylistClick(
                                                playlist
                                            )
                                        )
                                    },
                                    onDismissRequest = {
                                        showDeleteDialog = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    val isPlayerExpanded by remember {
                        derivedStateOf { playbackState.isPlayerExpanded }
                    }
                    if (!isPlayerExpanded) {
                        ScrollToTopAndLocateButtons(
                            showScrollToTopButton = showScrollToTopButton,
                            onScrollToTopClick = onScrollToTopClick,
                            showLocateButton = showLocateButton,
                            onLocateClick = onLocateClick,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    AnimatedVisibility(
                        visible = currentTrack != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier
                            .align(alignment = Alignment.CenterHorizontally)
                    ) {
                        currentTrack?.let {

                            if (useAlbumArtColor) {
                                LaunchedEffect(coverArtBitmap) {
                                    coverArtBitmap?.let {
                                        dominantColorState.updateFrom(it)
                                    }
                                }
                            }

                            PlayerSheet(
                                playbackStateFlow = viewModel.playbackState,
                                onPlayerExpandedChange = {
                                    viewModel.onEvent(PlayerScreenEvent.OnPlayerExpandedChange(it))
                                },
                                onPlayClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnPlayClick)
                                },
                                onPauseClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnPauseClick)
                                },
                                onSeekToNextClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnSeekToNextClick)
                                },
                                onSeekToPreviousClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnSeekToPreviousClick)
                                },
                                onSeekTo = {
                                    viewModel.onEvent(PlayerScreenEvent.OnSeekTo(it))
                                },
                                onReset = {
                                    viewModel.onEvent(PlayerScreenEvent.OnResetPlayback)
                                },
                                onPlaybackModeClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnPlaybackModeClick)
                                },
                                onCoverArtLoaded = {
                                    coverArtBitmap = it
                                    coverArtBitmapTrackUri = currentTrack?.uri
                                },
                                onPlayNextClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnPlayNextClick(it))
                                },
                                onAddToQueueClick = {
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnAddToQueueClick(
                                            listOf(it)
                                        )
                                    )
                                },
                                onAddToPlaylistClick = {
                                    showAddToOrCreatePlaylistSheet = true
                                    showCreatePlaylistOnly = false
                                    tracksToAddToPlaylist = listOf(it)
                                },
                                onViewTrackInfoClick = {
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnViewTrackInfoClick(it)
                                    )
                                },
                                onGoToAlbumClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnGoToAlbumClick(it))
                                    viewModel.onEvent(PlayerScreenEvent.OnPlayerExpandedChange(false))
                                    navController.popBackStack(PlayerRoutes.Main, false)
                                    navController.navigate(PlayerRoutes.Playlist)
                                },
                                onGoToArtistClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnGoToArtistClick(it))
                                    viewModel.onEvent(PlayerScreenEvent.OnPlayerExpandedChange(false))
                                    navController.popBackStack(PlayerRoutes.Main, false)
                                    navController.navigate(PlayerRoutes.Playlist)
                                },
                                onLyricsSheetExpandedChange = {
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnLyricsSheetExpandedChange(it)
                                    )
                                },
                                onLyricsClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnLyricsClick)
                                },
                                settings = viewModel.settings,
                                onRemoveFromQueueClick = {
                                    viewModel.onEvent(PlayerScreenEvent.OnRemoveFromQueueClick(it))
                                },
                                onReorderingQueue = { from, to ->
                                    viewModel.onEvent(PlayerScreenEvent.OnReorderingQueue(from, to))
                                },
                                onTrackClick = { track, playlist ->
                                    viewModel.onEvent(
                                        PlayerScreenEvent.OnTrackClick(
                                            track = track,
                                            playlist = playlist
                                        )
                                    )
                                },
                                lovedUris = lovedUris,
                                onToggleLovedClick = { viewModel.toggleLoved(it) },
                                modifier = Modifier
                                    .align(alignment = Alignment.CenterHorizontally)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }

                val trackInfoSheetState by viewModel.trackInfoSheetState.collectAsState()
                TrackInfoSheet(
                    state = trackInfoSheetState,
                    onCloseClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnCloseTrackInfoSheetClick)
                    },
                    onSearchInfo = { query ->
                        viewModel.onEvent(PlayerScreenEvent.OnSearchInfo(query))
                    },
                    onSearchResultClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnMetadataSearchResultPick(it))
                    },
                    onOverwriteMetadataClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnOverwriteMetadataClick(it))
                    },
                    onPickCoverArtClick = onCoverArtPick,
                    onRestoreCoverArtClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnRestoreCoverArtClick)
                    },
                    onConfirmMetadataEditClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnConfirmMetadataEditClick(it))
                    },
                    onRisksOfMetadataEditingAccept = {
                        viewModel.onEvent(PlayerScreenEvent.OnAcceptingRisksOfMetadataEditing)
                    },
                    onLyricsControlClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnLyricsControlClick)
                    },
                    onPickLyricsClick = onLyricsPick,
                    onDeleteLyricsClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnDeleteLyricsClick)
                    },
                    onFetchLyricsFromRemoteClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnFetchLyricsFromRemoteClick)
                    },
                    onCopyLyricsFromTagClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnCopyLyricsFromTagClick)
                    },
                    onWriteLyricsToTagClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnWriteLyricsToTagClick)
                    },
                    onPublishLyricsOnRemoteClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnPublishLyricsOnRemoteClick)
                    },
                    matchDurationWhenSearchMetadata = viewModel.settings.matchDurationWhenSearchMetadata,
                    onMatchDurationWhenSearchMetadataClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnMatchDurationWhenSearchMetadataClick)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                )



                if (showAddToOrCreatePlaylistSheet) {
                    val playlists by viewModel.playlists.collectAsState()
                    AddToOrCreatePlaylistBottomSheet(
                        playlists = playlists,
                        createOnly = showCreatePlaylistOnly,
                        onDismissRequest = {
                            showAddToOrCreatePlaylistSheet = false
                        },
                        onCreateClick = {
                            viewModel.onEvent(PlayerScreenEvent.OnCreatePlaylistClick(it))
                        },
                        onPlaylistSelection = { playlist ->
                            tracksToAddToPlaylist?.let { tracks ->
                                viewModel.onEvent(
                                    PlayerScreenEvent.OnAddToPlaylist(
                                        tracks = tracks,
                                        playlist = playlist
                                    )
                                )
                            }
                        }
                    )
                }

                val settingsSheetState by viewModel.settingsSheetState.collectAsState()
                SettingsSheet(
                    state = settingsSheetState,
                    perTrackArtwork = perTrackArtwork,
                    onFolderPick = onFolderPick,
                    onSidecarFolderPick = onSidecarFolderPick,
                    onPlaylistPick = onPlaylistPick,
                    onScanFoldersClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnScanFoldersClick)
                    },
                    onCloseClick = {
                        viewModel.onEvent(PlayerScreenEvent.OnCloseSettingsClick)
                    },
                    onBackupExport = { uri, cb -> viewModel.exportBackup(uri, cb) },
                    onBackupImport = { uri, cb -> viewModel.importBackup(uri, cb) },
                    dominantColorState = dominantColorState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun LazyGridScope.TabContent(
    playlists: List<Playlist>,
    gridPlaylists: Boolean,
    playlistSort: PlaylistSort,
    playlistSortOrder: SortOrder,
    fallbackPlaylistTitle: String,
    showSinglePreview: Boolean = false,
    onPlaylistClick: (Playlist) -> Unit,
    isInSelectionMode: Boolean,
    selectedPlaylists: List<Playlist>,
    onEnterSelectionMode: (Playlist) -> Unit,
    onToggleSelection: (Playlist) -> Unit,
    perTrackArtwork: Boolean = false,
    onDominantColorExtracted: (String, Int) -> Unit = { _, _ -> },
) {
    if (gridPlaylists) {
        if (!isInSelectionMode) {
            playlistCards(
                playlists = playlists,
                sort = playlistSort,
                sortOrder = playlistSortOrder,
                fallbackPlaylistTitle = fallbackPlaylistTitle,
                showSinglePreview = showSinglePreview,
                perTrackArtwork = perTrackArtwork,
                onCardClick = onPlaylistClick,
                onLongClick = { onEnterSelectionMode(it) },
                onDominantColorExtracted = onDominantColorExtracted,
            )
        } else {
            selectionCards(
                playlists = playlists,
                selectedPlaylists = selectedPlaylists,
                sort = playlistSort,
                sortOrder = playlistSortOrder,
                fallbackPlaylistTitle = fallbackPlaylistTitle,
                showSinglePreview = showSinglePreview,
                perTrackArtwork = perTrackArtwork,
                onCardClick = { onToggleSelection(it) },
                onDominantColorExtracted = onDominantColorExtracted,
            )
        }
    } else {
        if (!isInSelectionMode) {
            playlistRows(
                playlists = playlists,
                sort = playlistSort,
                sortOrder = playlistSortOrder,
                fallbackPlaylistTitle = fallbackPlaylistTitle,
                showSinglePreview = showSinglePreview,
                perTrackArtwork = perTrackArtwork,
                onRowClick = onPlaylistClick,
                onLongClick = { onEnterSelectionMode(it) },
                onDominantColorExtracted = onDominantColorExtracted,
            )
        } else {
            selectionRows(
                playlists = playlists,
                selectedPlaylists = selectedPlaylists,
                sort = playlistSort,
                sortOrder = playlistSortOrder,
                fallbackPlaylistTitle = fallbackPlaylistTitle,
                showSinglePreview = showSinglePreview,
                perTrackArtwork = perTrackArtwork,
                onRowClick = { onToggleSelection(it) },
                onDominantColorExtracted = onDominantColorExtracted,
            )
        }
    }
}

@Composable
fun MainPlayerScreen(
    gridState: LazyGridState = rememberLazyGridState(),
    topBarTabs: List<Tab>,
    defaultTab: Tab,
    onTabChange: (Tab) -> Unit = {},
    trackList: List<Track>,
    currentTrack: Track?,
    onTrackClick: (Track, Playlist) -> Unit,
    onPlayNextClick: (Track) -> Unit,
    onAddToQueueClick: (List<Track>) -> Unit,
    onAddToPlaylistClick: (List<Track>) -> Unit,
    onViewTrackInfoClick: (Track) -> Unit,
    onGoToAlbumClick: (Track) -> Unit,
    onGoToArtistClick: (Track) -> Unit,
    lovedUris: Set<String>,
    onToggleLovedClick: (Track) -> Unit,
    playlists: List<Playlist>,
    smartPlaylists: List<Playlist>,
    albumPlaylists: List<Playlist>,
    artistPlaylists: List<Playlist>,
    genrePlaylists: List<Playlist>,
    folderPlaylists: List<Playlist>,
    trackSort: TrackSort,
    trackSortOrder: SortOrder,
    playlistSort: PlaylistSort,
    playlistSortOrder: SortOrder,
    albumSort: PlaylistSort,
    albumSortOrder: SortOrder,
    artistSort: PlaylistSort,
    artistSortOrder: SortOrder,
    genreSort: PlaylistSort,
    genreSortOrder: SortOrder,
    folderSort: PlaylistSort,
    folderSortOrder: SortOrder,
    onTrackSortChange: (TrackSort?, SortOrder?) -> Unit,
    onPlaylistSortChange: (PlaylistSort?, SortOrder?) -> Unit,
    onAlbumSortChange: (PlaylistSort?, SortOrder?) -> Unit,
    onArtistSortChange: (PlaylistSort?, SortOrder?) -> Unit,
    onGenreSortChange: (PlaylistSort?, SortOrder?) -> Unit,
    onFolderSortChange: (PlaylistSort?, SortOrder?) -> Unit,
    onPlaylistSelection: (Playlist) -> Unit,
    onAlbumPlaylistSelection: (Playlist) -> Unit,
    onArtistPlaylistSelection: (Playlist) -> Unit,
    onGenrePlaylistSelection: (Playlist) -> Unit,
    onFolderPlaylistSelection: (Playlist) -> Unit,
    replaceSearchWithFilter: Boolean,
    gridPlaylists: Boolean,
    onGridPlaylistsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDominantColorExtracted: (String, Int) -> Unit = { _, _ -> },
    perTrackArtwork: Boolean = false,
) {
    val context = LocalContext.current

    var searchFieldValue by rememberSaveable {
        mutableStateOf("")
    }
    var showSearchField by rememberSaveable {
        mutableStateOf(false)
    }
    var showGlobalSearch by rememberSaveable {
        mutableStateOf(false)
    }

    var isInSelectionMode: Boolean by remember {
        mutableStateOf(false)
    }
    val selectedTracks = remember {
        mutableStateListOf<Track>()
    }
    val selectedPlaylists = remember {
        mutableStateListOf<Playlist>()
    }

    var albumContextMenuPlaylist by remember {
        mutableStateOf<Playlist?>(null)
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

    LazyGridWithCollapsibleTabsTopBar(
        gridState = gridState,
        topBarTabs = topBarTabs,
        defaultSelectedTab = defaultTab,
        onTabChange = {
            showSearchField = false
            searchFieldValue = ""

            isInSelectionMode = false
            selectedTracks.clear()
            selectedPlaylists.clear()

            onTabChange(it)
        },
        topBarContent = topBarContent,
        startButtons = { tab ->
            IconButton(
                onClick = onSettingsClick
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = context.resources.getString(
                        R.string.settings
                    )
                )
            }

            if (tab == Tab.Tracks) {
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
            } else if (tab == Tab.Albums) {
                PlaylistSortButton(
                    sort = albumSort,
                    order = albumSortOrder,
                    onSortChange = {
                        onAlbumSortChange(it, null)
                    },
                    onSortOrderChange = {
                        onAlbumSortChange(null, it)
                    }
                )
            } else if (tab == Tab.Artists) {
                PlaylistSortButton(
                    sort = artistSort,
                    order = artistSortOrder,
                    onSortChange = {
                        onArtistSortChange(it, null)
                    },
                    onSortOrderChange = {
                        onArtistSortChange(null, it)
                    }
                )
            } else if (tab == Tab.Genres) {
                PlaylistSortButton(
                    sort = genreSort,
                    order = genreSortOrder,
                    onSortChange = {
                        onGenreSortChange(it, null)
                    },
                    onSortOrderChange = {
                        onGenreSortChange(null, it)
                    }
                )
            } else if (tab == Tab.Folders) {
                PlaylistSortButton(
                    sort = folderSort,
                    order = folderSortOrder,
                    onSortChange = {
                        onFolderSortChange(it, null)
                    },
                    onSortOrderChange = {
                        onFolderSortChange(null, it)
                    }
                )
            } else {
                PlaylistSortButton(
                    sort = playlistSort,
                    order = playlistSortOrder,
                    onSortChange = {
                        onPlaylistSortChange(it, null)
                    },
                    onSortOrderChange = {
                        onPlaylistSortChange(null, it)
                    }
                )
            }
        },
        endButtons = { tab ->
            if (tab != Tab.Tracks) {
                IconButton(
                    onClick = onGridPlaylistsClick
                ) {
                    Icon(
                        imageVector = if (gridPlaylists) {
                            Icons.Rounded.GridView
                        } else Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = context.resources.getString(
                            if (gridPlaylists) {
                                R.string.enable_list_view
                            } else R.string.enable_grid_view
                        )
                    )
                }
            }

            IconButton(
                onClick = { showGlobalSearch = true }
            ) {
                Icon(
                    imageVector = Icons.Rounded.TravelExplore,
                    contentDescription = context.resources.getString(
                        R.string.search_library
                    )
                )
            }

            IconButton(
                onClick = {
                    showSearchField = true
                }
            ) {
                Icon(
                    imageVector = if (replaceSearchWithFilter && tab == Tab.Tracks) {
                        Icons.Rounded.FilterList
                    } else Icons.Rounded.Search,
                    contentDescription = context.resources.getString(
                        R.string.track_search
                    )
                )
            }
        },
        topBarOverlay = { tab ->
            when (topBarContent) {
                TopBarContent.Search -> {
                    BackHandler {
                        showSearchField = false
                        searchFieldValue = ""
                    }
                    val focusRequester = remember {
                        FocusRequester()
                    }
                    SearchField(
                        value = searchFieldValue,
                        onValueChange = {
                            searchFieldValue = it.trimStart()
                        },
                        icon = if (replaceSearchWithFilter && tab == Tab.Tracks) {
                            Icons.Rounded.FilterList
                        } else Icons.Rounded.Search,
                        placeholder = if (replaceSearchWithFilter && tab == Tab.Tracks) {
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

                TopBarContent.Selection -> {
                    BackHandler {
                        isInSelectionMode = false
                        selectedTracks.clear()
                        selectedPlaylists.clear()
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = MaterialTheme.colorScheme.background)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    isInSelectionMode = false
                                    selectedTracks.clear()
                                    selectedPlaylists.clear()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = context.resources.getString(R.string.back)
                                )
                            }

                            Text(
                                text = (selectedTracks.size + selectedPlaylists.size).toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row {
                            if (tab == Tab.Tracks && selectedTracks.size < trackList.size) {
                                IconButton(
                                    onClick = {
                                        selectedTracks.clear()
                                        selectedTracks.addAll(trackList)
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
                                    if (selectedTracks.isNotEmpty()) {
                                        onAddToQueueClick(selectedTracks.toList())
                                    } else if (selectedPlaylists.isNotEmpty()) {
                                        onAddToQueueClick(
                                            selectedPlaylists.flatMap {
                                                it.trackList
                                            }.distinct()
                                        )
                                    }
                                    isInSelectionMode = false
                                    selectedTracks.clear()
                                    selectedPlaylists.clear()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddToQueue,
                                    contentDescription = context.resources.getString(R.string.add_to_queue)
                                )
                            }

                            if (tab == Tab.Tracks) {
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
                            }

                            IconButton(
                                onClick = {
                                    showSearchField = true
                                }
                            ) {
                                Icon(
                                    imageVector = if (replaceSearchWithFilter && tab == Tab.Tracks) {
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

                TopBarContent.Default -> {
                    // Composable never invokes this branch — startButtons /
                    // endButtons are shown for Default mode.
                }
            }
        },
        contentHorizontalArrangement = Arrangement.spacedBy(
            16.dp,
            alignment = Alignment.CenterHorizontally
        ),
        contentVerticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        gridCells = {
            if (it == Tab.Tracks || !gridPlaylists) GridCells.Fixed(1) else {
                GridCells.Adaptive(150.dp)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) { tab ->
        when (tab) {
            Tab.Tracks -> {
                if (!isInSelectionMode) {
                    trackList(
                        trackList = trackList.filterTracks(searchFieldValue),
                        currentTrack = currentTrack,
                        lovedUris = lovedUris,
                        onTrackClick = {
                            onTrackClick(
                                it,
                                Playlist(
                                    name = null,
                                    trackList = if (replaceSearchWithFilter) {
                                        trackList.filterTracks(searchFieldValue)
                                    } else trackList
                                )
                            )
                        },
                        onToggleLovedClick = onToggleLovedClick,
                        onPlayNextClick = onPlayNextClick,
                        onAddToQueueClick = {
                            onAddToQueueClick(listOf(it))
                        },
                        onAddToPlaylistClick = {
                            onAddToPlaylistClick(listOf(it))
                        },
                        onViewTrackInfoClick = onViewTrackInfoClick,
                        onGoToAlbumClick = onGoToAlbumClick,
                        onGoToArtistClick = onGoToArtistClick,
                        perTrackArtwork = perTrackArtwork,
                        onLongClick = {
                            isInSelectionMode = true
                            selectedTracks.add(it)
                        }
                    )
                } else {
                    selectionList(
                        trackList = trackList.filterTracks(searchFieldValue),
                        selectedTracks = selectedTracks,
                        onTrackClick = {
                            if (it in selectedTracks) {
                                selectedTracks.remove(it)
                            } else selectedTracks.add(it)

                            if (selectedTracks.isEmpty()) {
                                isInSelectionMode = false
                            }
                        },
                        perTrackArtwork = perTrackArtwork,
                    )
                }
            }

            Tab.Playlists -> {
                val filteredSmart = smartPlaylists.filterPlaylists(searchFieldValue)
                if (gridPlaylists) {
                    if (!isInSelectionMode) {
                        if (filteredSmart.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PlaylistSectionHeader(
                                    text = context.resources.getString(R.string.smart_playlists)
                                )
                            }
                            playlistCards(
                                playlists = filteredSmart,
                                sort = playlistSort,
                                sortOrder = playlistSortOrder,
                                fallbackPlaylistTitle = context.resources.getString(R.string.unknown),
                                showSinglePreview = false,
                                perTrackArtwork = perTrackArtwork,
                                // Smart playlists navigate to the immutable view
                                // (same as album/artist/genre) — they can't be
                                // renamed or reordered.
                                onCardClick = onAlbumPlaylistSelection,
                                onLongClick = { /* smart lists aren't selectable */ }
                            )
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PlaylistSectionHeader(
                                    text = context.resources.getString(R.string.playlists)
                                )
                            }
                        }
                        playlistCards(
                            playlists = playlists.filterPlaylists(searchFieldValue),
                            sort = playlistSort,
                            sortOrder = playlistSortOrder,
                            fallbackPlaylistTitle = context.resources.getString(R.string.unknown),
                            showSinglePreview = false,
                            perTrackArtwork = perTrackArtwork,
                            onCardClick = onPlaylistSelection,
                            onLongClick = {
                                isInSelectionMode = true
                                selectedPlaylists.add(it)
                            }
                        )
                    } else {
                        selectionCards(
                            playlists = playlists.filterPlaylists(searchFieldValue),
                            selectedPlaylists = selectedPlaylists,
                            sort = playlistSort,
                            sortOrder = playlistSortOrder,
                            fallbackPlaylistTitle = context.resources.getString(R.string.unknown),
                            showSinglePreview = false,
                            perTrackArtwork = perTrackArtwork,
                            onCardClick = {
                                if (it in selectedPlaylists) {
                                    selectedPlaylists.remove(it)
                                } else selectedPlaylists.add(it)

                                if (selectedPlaylists.isEmpty()) {
                                    isInSelectionMode = false
                                }
                            }
                        )
                    }
                } else {
                    if (!isInSelectionMode) {
                        if (filteredSmart.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PlaylistSectionHeader(
                                    text = context.resources.getString(R.string.smart_playlists)
                                )
                            }
                            playlistRows(
                                playlists = filteredSmart,
                                sort = playlistSort,
                                sortOrder = playlistSortOrder,
                                fallbackPlaylistTitle = context.resources.getString(R.string.unknown),
                                showSinglePreview = false,
                                perTrackArtwork = perTrackArtwork,
                                onRowClick = onAlbumPlaylistSelection,
                                onLongClick = { /* smart lists aren't selectable */ }
                            )
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PlaylistSectionHeader(
                                    text = context.resources.getString(R.string.playlists)
                                )
                            }
                        }
                        playlistRows(
                            playlists = playlists.filterPlaylists(searchFieldValue),
                            sort = playlistSort,
                            sortOrder = playlistSortOrder,
                            fallbackPlaylistTitle = context.resources.getString(R.string.unknown),
                            showSinglePreview = false,
                            perTrackArtwork = perTrackArtwork,
                            onRowClick = onPlaylistSelection,
                            onLongClick = {
                                isInSelectionMode = true
                                selectedPlaylists.add(it)
                            }
                        )
                    } else {
                        selectionRows(
                            playlists = playlists.filterPlaylists(searchFieldValue),
                            selectedPlaylists = selectedPlaylists,
                            sort = playlistSort,
                            sortOrder = playlistSortOrder,
                            fallbackPlaylistTitle = context.resources.getString(R.string.unknown),
                            showSinglePreview = false,
                            perTrackArtwork = perTrackArtwork,
                            onRowClick = {
                                if (it in selectedPlaylists) {
                                    selectedPlaylists.remove(it)
                                } else selectedPlaylists.add(it)

                                if (selectedPlaylists.isEmpty()) {
                                    isInSelectionMode = false
                                }
                            }
                        )
                    }
                }
            }

            Tab.Albums -> {
                TabContent(
                    // The Albums tab always shows one large album cover per
                    // tile, regardless of the global per-track-artwork setting.
                    // A per-track 2x2 mosaic makes no sense for a single album:
                    // an album with no per-track embedded art collapses into
                    // four identical folder.jpg thumbnails (uhrfra's #87
                    // follow-up), and the per-track option text itself promises
                    // album art as the fallback. Playlists and the Tracks tab
                    // keep per-track artwork.
                    perTrackArtwork = false,
                    playlists = albumPlaylists.filterPlaylists(searchFieldValue),
                    gridPlaylists = gridPlaylists,
                    playlistSort = albumSort,
                    playlistSortOrder = albumSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_album),
                    showSinglePreview = true,
                    onPlaylistClick = onAlbumPlaylistSelection,
                    isInSelectionMode = isInSelectionMode,
                    selectedPlaylists = selectedPlaylists,
                    onEnterSelectionMode = { playlist ->
                        albumContextMenuPlaylist = playlist
                    },
                    onToggleSelection = { playlist ->
                        if (playlist in selectedPlaylists) {
                            selectedPlaylists.remove(playlist)
                        } else selectedPlaylists.add(playlist)
                        if (selectedPlaylists.isEmpty()) {
                            isInSelectionMode = false
                        }
                    },
                    onDominantColorExtracted = onDominantColorExtracted,
                )
            }

            Tab.Artists -> {
                TabContent(
                    perTrackArtwork = perTrackArtwork,
                    playlists = artistPlaylists.filterPlaylists(searchFieldValue),
                    gridPlaylists = gridPlaylists,
                    playlistSort = artistSort,
                    playlistSortOrder = artistSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_artist),
                    onPlaylistClick = onArtistPlaylistSelection,
                    isInSelectionMode = isInSelectionMode,
                    selectedPlaylists = selectedPlaylists,
                    onEnterSelectionMode = { playlist ->
                        isInSelectionMode = true
                        selectedPlaylists.add(playlist)
                    },
                    onToggleSelection = { playlist ->
                        if (playlist in selectedPlaylists) {
                            selectedPlaylists.remove(playlist)
                        } else selectedPlaylists.add(playlist)
                        if (selectedPlaylists.isEmpty()) {
                            isInSelectionMode = false
                        }
                    },
                    onDominantColorExtracted = onDominantColorExtracted,
                )
            }

            Tab.Genres -> {
                TabContent(
                    perTrackArtwork = perTrackArtwork,
                    playlists = genrePlaylists.filterPlaylists(searchFieldValue),
                    gridPlaylists = gridPlaylists,
                    playlistSort = genreSort,
                    playlistSortOrder = genreSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_genre),
                    onPlaylistClick = onGenrePlaylistSelection,
                    isInSelectionMode = isInSelectionMode,
                    selectedPlaylists = selectedPlaylists,
                    onEnterSelectionMode = { playlist ->
                        isInSelectionMode = true
                        selectedPlaylists.add(playlist)
                    },
                    onToggleSelection = { playlist ->
                        if (playlist in selectedPlaylists) {
                            selectedPlaylists.remove(playlist)
                        } else selectedPlaylists.add(playlist)
                        if (selectedPlaylists.isEmpty()) {
                            isInSelectionMode = false
                        }
                    },
                    onDominantColorExtracted = onDominantColorExtracted,
                )
            }

            Tab.Folders -> {
                TabContent(
                    perTrackArtwork = perTrackArtwork,
                    playlists = folderPlaylists.filterPlaylists(searchFieldValue),
                    gridPlaylists = gridPlaylists,
                    playlistSort = folderSort,
                    playlistSortOrder = folderSortOrder,
                    fallbackPlaylistTitle = context.resources.getString(R.string.unknown_folder),
                    onPlaylistClick = onFolderPlaylistSelection,
                    isInSelectionMode = isInSelectionMode,
                    selectedPlaylists = selectedPlaylists,
                    onEnterSelectionMode = { playlist ->
                        isInSelectionMode = true
                        selectedPlaylists.add(playlist)
                    },
                    onToggleSelection = { playlist ->
                        if (playlist in selectedPlaylists) {
                            selectedPlaylists.remove(playlist)
                        } else selectedPlaylists.add(playlist)
                        if (selectedPlaylists.isEmpty()) {
                            isInSelectionMode = false
                        }
                    },
                    onDominantColorExtracted = onDominantColorExtracted,
                )
            }
        }
    }

    GlobalSearchSheet(
        isVisible = showGlobalSearch,
        onDismiss = { showGlobalSearch = false },
        allTracks = trackList,
        playlists = playlists,
        albumPlaylists = albumPlaylists,
        artistPlaylists = artistPlaylists,
        genrePlaylists = genrePlaylists,
        onTrackClick = onTrackClick,
        onPlaylistClick = onPlaylistSelection,
        onAlbumPlaylistClick = onAlbumPlaylistSelection,
        onArtistPlaylistClick = onArtistPlaylistSelection,
        onGenrePlaylistClick = onGenrePlaylistSelection,
        perTrackArtwork = perTrackArtwork,
    )

    if (albumContextMenuPlaylist != null) {
        AlertDialog(
            onDismissRequest = { albumContextMenuPlaylist = null },
            title = {
                Text(text = albumContextMenuPlaylist?.name
                    ?: context.resources.getString(R.string.unknown_album))
            },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            val playlist = albumContextMenuPlaylist
                            albumContextMenuPlaylist = null
                            playlist?.let {
                                isInSelectionMode = true
                                selectedPlaylists.add(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = context.resources.getString(R.string.select_items))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { albumContextMenuPlaylist = null }) {
                    Text(text = context.resources.getString(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ScrollToTopAndLocateButtons(
    showScrollToTopButton: Boolean,
    onScrollToTopClick: suspend () -> Unit,
    showLocateButton: Boolean,
    onLocateClick: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        horizontalArrangement = Arrangement.End
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        AnimatedVisibility(
            visible = showLocateButton,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            FilledTonalIconButton(
                onClick = {
                    coroutineScope.launch {
                        onLocateClick()
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = context.resources.getString(R.string.scroll_to_current_track)
                )
            }
        }

        AnimatedVisibility(
            visible = showScrollToTopButton,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            FilledTonalIconButton(
                onClick = {
                    coroutineScope.launch {
                        onScrollToTopClick()
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = context.resources.getString(R.string.scroll_to_top)
                )
            }
        }
    }
}


@Serializable
private sealed interface PlayerRoutes {
    @Serializable
    data object Main : PlayerRoutes

    @Serializable
    data object Playlist : PlayerRoutes

    @Serializable
    data object MutablePlaylist : PlayerRoutes
}
