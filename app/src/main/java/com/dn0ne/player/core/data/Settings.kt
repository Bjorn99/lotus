package com.dn0ne.player.core.data

import android.content.Context
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.dn0ne.player.app.domain.sort.PlaylistSort
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.TrackSort
import com.dn0ne.player.app.presentation.components.settings.Theme
import com.dn0ne.player.app.presentation.components.topbar.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class Settings(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val handleAudioFocusKey = "audio-focus"

    private val replaceSearchWithFilterKey = "replace-search-with-filter"

    private val appearanceKey = "appearance"
    private val useDynamicColorKey = "use-dynamic-color"
    private val useAlbumArtColorKey = "use-album-art-color"
    private val paletteStyleKey = "palette-style"
    private val amoledDarkThemeKey = "amoled-dark-theme"

    private val lyricsFontSizeKey = "lyrics-font-size"
    private val lyricsFontWeightKey = "lyrics-font-weight"
    private val lyricsLineHeightKey = "lyrics-line-height"
    private val lyricsLetterSpacingKey = "lyrics-letter-spacing"
    private val lyricsAlignmentKey = "lyrics-alignment"
    private val sidecarFolderUriKey = "sidecar-folder-uri"
    private val useDarkPaletteOnLyricsSheetKey = "dark-palette-on-lyrics-sheet"
    private val networkLookupsEnabledKey = "network-lookups-enabled"
    private val trackPlayStatsKey = "track-play-stats"

    private val areRisksOfMetadataEditingAcceptedKey = "metadata-editing-dialog"

    private val matchDurationWhenSearchMetadataKey = "match-duration"

    private val perTrackArtworkKey = "per-track-artwork"

    private val trackSortKey = "track-sort-key"
    private val trackSortOrderKey = "track-sort-order-key"
    private val playlistSortKey = "playlist-sort-key"
    private val playlistSortOrderKey = "playlist-sort-order-key"
    private val albumSortKey = "album-sort-key"
    private val albumSortOrderKey = "album-sort-order-key"
    private val artistSortKey = "artist-sort-key"
    private val artistSortOrderKey = "artist-sort-order-key"
    private val genreSortKey = "genre-sort-key"
    private val genreSortOrderKey = "genre-sort-order-key"
    private val folderSortKey = "folder-sort-key"
    private val folderSortOrderKey = "folder-sort-order-key"

    private val isScanModeInclusiveKey = "is-scan-mode-inclusive"
    private val ignoreShortTracksKey = "exclude-short-tracks"
    private val scanMusicFolderKey = "scan-music-in-music"
    private val extraScanFoldersKey = "extra-scan-folders"
    private val excludedScanFoldersKey = "excluded-scan-folders"
    private val scanOnAppLaunchKey = "scan-on-app-launch"

    private val tabOrderKey = "tab-order"
    private val defaultTabKey = "default-tab"

    private val jumpToBeginningKey = "jump-to-beginning"

    private val gridPlaylistsKey = "grid-playlists"

    var handleAudioFocus: Boolean
        get() = sharedPreferences.getBoolean(handleAudioFocusKey, true)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(handleAudioFocusKey, value)
                apply()
            }
        }

    private val _replaceSearchWithFilter = MutableStateFlow(
        sharedPreferences.getBoolean(replaceSearchWithFilterKey, false)
    )
    val replaceSearchWithFilter = _replaceSearchWithFilter.asStateFlow()
    fun updateReplaceSearchWithFilter(value: Boolean) {
        _replaceSearchWithFilter.update { value  }
        with(sharedPreferences.edit()) {
            putBoolean(replaceSearchWithFilterKey, value)
            apply()
        }
    }

    private val _appearance = MutableStateFlow(
        Theme.Appearance.entries[sharedPreferences.getInt(appearanceKey, 0)]
    )
    val appearance = _appearance.asStateFlow()
    fun updateAppearance(appearance: Theme.Appearance) {
        _appearance.update {
            appearance
        }
        with(sharedPreferences.edit()) {
            putInt(appearanceKey, appearance.ordinal)
            apply()
        }
    }

    private val _useDynamicColor = MutableStateFlow(
        sharedPreferences.getBoolean(useDynamicColorKey, true)
    )
    val useDynamicColor = _useDynamicColor.asStateFlow()
    fun updateUseDynamicColor(value: Boolean) {
        _useDynamicColor.update { value }
        with(sharedPreferences.edit()) {
            putBoolean(useDynamicColorKey, value)
            apply()
        }
    }

    private val _useAlbumArtColor = MutableStateFlow(
        sharedPreferences.getBoolean(useAlbumArtColorKey, true)
    )
    val useAlbumArtColor = _useAlbumArtColor.asStateFlow()
    fun updateUseAlbumArtColor(value: Boolean) {
        _useAlbumArtColor.update { value }
        with(sharedPreferences.edit()) {
            putBoolean(useAlbumArtColorKey, value)
            apply()
        }
    }

    private val _paletteStyle = MutableStateFlow(
        Theme.PaletteStyle.entries[sharedPreferences.getInt(paletteStyleKey, 0)]
    )
    val paletteStyle = _paletteStyle.asStateFlow()
    fun updatePaletteStyle(paletteStyle: Theme.PaletteStyle) {
        _paletteStyle.update {
            paletteStyle
        }
        with(sharedPreferences.edit()) {
            putInt(paletteStyleKey, paletteStyle.ordinal)
            apply()
        }
    }

    private val _amoledDarkTheme = MutableStateFlow(
        sharedPreferences.getBoolean(amoledDarkThemeKey, false)
    )
    val amoledDarkTheme = _amoledDarkTheme.asStateFlow()
    fun updateAmoledDarkTheme(value: Boolean) {
        _amoledDarkTheme.update {
            value
        }
        with(sharedPreferences.edit()) {
            putBoolean(amoledDarkThemeKey, value)
            apply()
        }
    }

    var lyricsFontSize: TextUnit
        get() = sharedPreferences.getFloat(lyricsFontSizeKey, 28f).sp
        set(value) {
            with(sharedPreferences.edit()) {
                putFloat(lyricsFontSizeKey, value.value)
                apply()
            }
        }

    var lyricsFontWeight: Int
        get() = sharedPreferences.getInt(lyricsFontWeightKey, 600)
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(lyricsFontWeightKey, value)
                apply()
            }
        }

    var lyricsLineHeight: TextUnit
        get() = sharedPreferences.getFloat(lyricsLineHeightKey, 32f).sp
        set(value) {
            with(sharedPreferences.edit()) {
                putFloat(lyricsLineHeightKey, value.value)
                apply()
            }
        }

    var lyricsLetterSpacing: TextUnit
        get() = sharedPreferences.getFloat(lyricsLetterSpacingKey, 0f).sp
        set(value) {
            with(sharedPreferences.edit()) {
                putFloat(lyricsLetterSpacingKey, value.value)
                apply()
            }
        }

    var lyricsAlignment: TextAlign
        get() = when (sharedPreferences.getInt(lyricsAlignmentKey, 0)) {
            1 -> TextAlign.Center
            2 -> TextAlign.End
            else -> TextAlign.Start
        }
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(
                    lyricsAlignmentKey,
                    when (value) {
                        TextAlign.Center -> 1
                        TextAlign.End -> 2
                        else -> 0
                    }
                )
                apply()
            }
        }

    fun resetLyricsStyle() {
        with(sharedPreferences.edit()) {
            remove(lyricsFontSizeKey)
            remove(lyricsFontWeightKey)
            remove(lyricsLineHeightKey)
            remove(lyricsLetterSpacingKey)
            remove(lyricsAlignmentKey)
            apply()
        }
    }

    var sidecarFolderUri: String?
        get() = sharedPreferences.getString(sidecarFolderUriKey, null)
        set(value) {
            with(sharedPreferences.edit()) {
                putString(sidecarFolderUriKey, value)
                apply()
            }
        }

    var useDarkPaletteOnLyricsSheet: Boolean
        get() = sharedPreferences.getBoolean(useDarkPaletteOnLyricsSheetKey, true)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(useDarkPaletteOnLyricsSheetKey, value)
                apply()
            }
        }

    // Master gate over every outbound HTTP call the app makes. When false,
    // lyrics + metadata providers short-circuit before touching the network.
    // Default false: network features are opt-in (F-Droid policy).
    var networkLookupsEnabled: Boolean
        get() = sharedPreferences.getBoolean(networkLookupsEnabledKey, false)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(networkLookupsEnabledKey, value)
                apply()
            }
        }

    // Privacy toggle for the listening-stats feature. Default true so the
    // backing track_stats table fills up by default; flipping off both
    // halts new writes (the listener checks this on every event) and
    // clears the existing rows (PlaybackService observes the flow).
    private val _trackPlayStats = MutableStateFlow(
        sharedPreferences.getBoolean(trackPlayStatsKey, true)
    )
    val trackPlayStats = _trackPlayStats.asStateFlow()
    fun updateTrackPlayStats(value: Boolean) {
        _trackPlayStats.update { value }
        with(sharedPreferences.edit()) {
            putBoolean(trackPlayStatsKey, value)
            apply()
        }
    }

    var areRisksOfMetadataEditingAccepted: Boolean
        get() = sharedPreferences.getBoolean(areRisksOfMetadataEditingAcceptedKey, false)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(areRisksOfMetadataEditingAcceptedKey, value)
                apply()
            }
        }

    var matchDurationWhenSearchMetadata: Boolean
        get() = sharedPreferences.getBoolean(matchDurationWhenSearchMetadataKey, false)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(matchDurationWhenSearchMetadataKey, value)
                apply()
            }
        }

    var perTrackArtwork: Boolean
        get() = sharedPreferences.getBoolean(perTrackArtworkKey, false)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(perTrackArtworkKey, value)
                apply()
            }
        }

    var trackSort: TrackSort
        get() = TrackSort.entries[sharedPreferences.getInt(trackSortKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(trackSortKey, value.ordinal)
                apply()
            }
        }

    var trackSortOrder: SortOrder
        get() = SortOrder.entries[sharedPreferences.getInt(trackSortOrderKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(trackSortOrderKey, value.ordinal)
                apply()
            }
        }

    var playlistSort: PlaylistSort
        get() = PlaylistSort.entries[sharedPreferences.getInt(playlistSortKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(playlistSortKey, value.ordinal)
                apply()
            }
        }

    var playlistSortOrder: SortOrder
        get() = SortOrder.entries[sharedPreferences.getInt(playlistSortOrderKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(playlistSortOrderKey, value.ordinal)
                apply()
            }
        }

    var albumSort: PlaylistSort
        get() = PlaylistSort.entries[sharedPreferences.getInt(albumSortKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(albumSortKey, value.ordinal)
                apply()
            }
        }

    var albumSortOrder: SortOrder
        get() = SortOrder.entries[sharedPreferences.getInt(albumSortOrderKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(albumSortOrderKey, value.ordinal)
                apply()
            }
        }

    var artistSort: PlaylistSort
        get() = PlaylistSort.entries[sharedPreferences.getInt(artistSortKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(artistSortKey, value.ordinal)
                apply()
            }
        }

    var artistSortOrder: SortOrder
        get() = SortOrder.entries[sharedPreferences.getInt(artistSortOrderKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(artistSortOrderKey, value.ordinal)
                apply()
            }
        }

    var genreSort: PlaylistSort
        get() = PlaylistSort.entries[sharedPreferences.getInt(genreSortKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(genreSortKey, value.ordinal)
                apply()
            }
        }

    var genreSortOrder: SortOrder
        get() = SortOrder.entries[sharedPreferences.getInt(genreSortOrderKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(genreSortOrderKey, value.ordinal)
                apply()
            }
        }

    var folderSort: PlaylistSort
        get() = PlaylistSort.entries[sharedPreferences.getInt(folderSortKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(folderSortKey, value.ordinal)
                apply()
            }
        }

    var folderSortOrder: SortOrder
        get() = SortOrder.entries[sharedPreferences.getInt(folderSortOrderKey, 0)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(folderSortOrderKey, value.ordinal)
                apply()
            }
        }

    private val _isScanModeInclusive = MutableStateFlow(
        sharedPreferences.getBoolean(isScanModeInclusiveKey, true)
    )
    val isScanModeInclusive = _isScanModeInclusive.asStateFlow()
    fun updateIsScanModeInclusive(value: Boolean) {
        _isScanModeInclusive.update { value }
        with(sharedPreferences.edit()) {
            putBoolean(isScanModeInclusiveKey, value)
            apply()
        }
    }

    var ignoreShortTracks: Boolean
        get() = sharedPreferences.getBoolean(ignoreShortTracksKey, true)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(ignoreShortTracksKey, value)
                apply()
            }
        }

    private val _scanMusicFolder = MutableStateFlow(
        sharedPreferences.getBoolean(scanMusicFolderKey, true)
    )
    val scanMusicFolder = _scanMusicFolder.asStateFlow()
    fun updateScanMusicFolder(value: Boolean) {
        _scanMusicFolder.update { value }
        with(sharedPreferences.edit()) {
            putBoolean(scanMusicFolderKey, value)
            apply()
        }
    }

    private val _extraScanFolders = MutableStateFlow(
        sharedPreferences.getStringSet(extraScanFoldersKey, setOf<String>()) ?: setOf<String>()
    )
    val extraScanFolders = _extraScanFolders.asStateFlow()
    fun updateExtraScanFolders(value: Set<String>) {
        _extraScanFolders.update { value }
        with(sharedPreferences.edit()) {
            putStringSet(extraScanFoldersKey, value)
            apply()
        }
    }

    private val _excludedScanFolders = MutableStateFlow(
        sharedPreferences.getStringSet(excludedScanFoldersKey, setOf<String>()) ?: setOf<String>()
    )
    val excludedScanFolders = _excludedScanFolders.asStateFlow()
    fun updateExcludedScanFolders(value: Set<String>) {
        _excludedScanFolders.update { value }
        with(sharedPreferences.edit()) {
            putStringSet(excludedScanFoldersKey, value)
            apply()
        }
    }

    private val _scanOnAppLaunch = MutableStateFlow(
        sharedPreferences.getBoolean(scanOnAppLaunchKey, true)
    )
    val scanOnAppLaunch = _scanOnAppLaunch.asStateFlow()
    fun updateScanOnAppLaunch(value: Boolean) {
        _scanOnAppLaunch.update { value }
        with(sharedPreferences.edit()) {
            putBoolean(scanOnAppLaunchKey, value)
            apply()
        }
    }

    private val _tabs = MutableStateFlow(
        sharedPreferences.getString(tabOrderKey, null)?.let {
            it.split(";").map {
                Tab.valueOf(it)
            }
        } ?: Tab.entries.toList()
    )
    val tabs = _tabs.asStateFlow()
    fun updateTabOrder(tabs: List<Tab>) {
        _tabs.update { tabs }
        with(sharedPreferences.edit()) {
            putString(tabOrderKey, tabs.map { it.name }.joinToString(";"))
            apply()
        }
    }

    var defaultTab: Tab
        get() = Tab.entries[sharedPreferences.getInt(defaultTabKey, 1)]
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(defaultTabKey, value.ordinal)
                apply()
            }
        }

    var jumpToBeginning: Boolean
        get() = sharedPreferences.getBoolean(jumpToBeginningKey, true)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(jumpToBeginningKey, value)
                apply()
            }
        }

    private val _gridPlaylist = MutableStateFlow(
        sharedPreferences.getBoolean(gridPlaylistsKey, true)
    )
    val gridPlaylists = _gridPlaylist.asStateFlow()
    fun updateGridPlaylists(value: Boolean) {
        _gridPlaylist.update { value }
        with(sharedPreferences.edit()) {
            putBoolean(gridPlaylistsKey, value)
            apply()
        }
    }
}