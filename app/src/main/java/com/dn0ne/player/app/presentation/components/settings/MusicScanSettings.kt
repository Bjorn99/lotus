package com.dn0ne.player.app.presentation.components.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import com.dn0ne.player.core.data.MusicScanner
import com.dn0ne.player.core.data.Settings
import kotlinx.coroutines.launch

@Composable
fun MusicScanSettings(
    settings: Settings,
    musicScanner: MusicScanner,
    foldersWithAudio: Set<String>,
    onFolderPick: (scan: Boolean) -> Unit,
    onScanFoldersClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var collapseFraction by remember {
        mutableFloatStateOf(0f)
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
                    contentDescription = context.resources.getString(R.string.back)
                )
            }

            Text(
                text = context.resources.getString(R.string.music_scan),
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
        collapseFraction = {
            collapseFraction = it
        },
        contentPadding = PaddingValues(horizontal = 28.dp),
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        MusicScanSettingsContent(
            settings = settings,
            musicScanner = musicScanner,
            onFolderPick = onFolderPick,
            foldersWithAudio = foldersWithAudio,
            onScanFoldersClick = onScanFoldersClick
        )
    }
}

@Composable
fun MusicScanSettingsContent(
    settings: Settings,
    musicScanner: MusicScanner,
    foldersWithAudio: Set<String>,
    onFolderPick: (scan: Boolean) -> Unit,
    onScanFoldersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
    ) {
        val context = LocalContext.current
        val isScanModeInclusive by settings.isScanModeInclusive.collectAsState()
        val scanModeOptions = remember {
            listOf(
                SettingSegmentOption(
                    icon = Icons.Rounded.Add,
                    contentDescription = context.resources.getString(R.string.inclusive),
                    onClick = {
                        settings.updateIsScanModeInclusive(true)
                    }
                ),
                SettingSegmentOption(
                    icon = Icons.Rounded.Remove,
                    contentDescription = context.resources.getString(R.string.exclusive),
                    onClick = {
                        settings.updateIsScanModeInclusive(false)
                    }
                )
            )
        }
        SettingSegmentOptions(
            title = context.resources.getString(R.string.filter_mode),
            supportingText = context.resources.getString(R.string.filter_mode_explain),
            icon = Icons.Rounded.LocationSearching,
            options = scanModeOptions,
            selectedOptionIndex = if (isScanModeInclusive) 0 else 1,
        )

        var ignoreShortTracks by remember {
            mutableStateOf(settings.ignoreShortTracks)
        }
        SettingSwitch(
            title = context.resources.getString(R.string.ignore_short_tracks),
            supportingText = context.resources.getString(R.string.ignore_short_tracks_explain),
            icon = Icons.Rounded.Timelapse,
            isChecked = ignoreShortTracks,
            onCheckedChange = {
                ignoreShortTracks = it
                settings.ignoreShortTracks = it
            },
            modifier = Modifier.fillMaxWidth()
        )

        val ignoreSingleTrackAlbums by settings.ignoreSingleTrackAlbums.collectAsState()
        SettingSwitch(
            title = context.resources.getString(R.string.ignore_single_track_albums),
            supportingText = context.resources
                .getString(R.string.ignore_single_track_albums_explain),
            icon = Icons.Rounded.Album,
            isChecked = ignoreSingleTrackAlbums,
            onCheckedChange = {
                settings.updateIgnoreSingleTrackAlbums(it)
            },
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedContent(
            targetState = isScanModeInclusive,
            label = "scan-settings-animation",
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }
        ) { state ->
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (state) {
                    true -> {
                        val isScanMusicFolderChecked by settings.scanMusicFolder.collectAsState()
                        SettingSwitch(
                            title = context.resources.getString(R.string.include_music_folder),
                            supportingText = context.resources.getString(R.string.include_music_folder_explain),
                            icon = Icons.Rounded.LibraryMusic,
                            isChecked = isScanMusicFolderChecked,
                            onCheckedChange = {
                                settings.updateScanMusicFolder(it)
                            }
                        )

                        val includedScanFolders by settings.extraScanFolders.collectAsState()
                        SettingFoldersPicked(
                            title = context.resources.getString(R.string.included_folders),
                            paths = includedScanFolders.toList(),
                            addFolderContentDescription = context.resources.getString(R.string.pick_folder_to_include_in_scanning),
                            removeFolderContentDescription = context.resources.getString(R.string.remove_folder_from_included_in_scanning),
                            onPickFolderClick = {
                                onFolderPick(false)
                            },
                            onRemoveFolderClick = {
                                settings.updateExtraScanFolders(
                                    includedScanFolders - it
                                )
                            },
                            availableOptionsTitle = context.resources.getString(R.string.folders_with_audio),
                            availableOptions = foldersWithAudio.toList(),
                            addFolderFromOptionsContentDescription = context.resources.getString(
                                R.string.include_folder_in_scanning
                            ),
                            onAddFolderFromOptionsClick = {
                                settings.updateExtraScanFolders(
                                    includedScanFolders + it
                                )
                            },
                            onScanClick = onScanFoldersClick,
                            scanContentDescription = context.resources.getString(R.string.scan_for_folders_with_audio),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    false -> {
                        val excludedScanFolders by settings.excludedScanFolders.collectAsState()
                        SettingFoldersPicked(
                            title = context.resources.getString(R.string.excluded_folders),
                            paths = excludedScanFolders.toList(),
                            addFolderContentDescription = context.resources.getString(R.string.pick_folder_to_exclude_from_scanning),
                            removeFolderContentDescription = context.resources.getString(R.string.remove_folder_from_excluded_from_scanning),
                            onPickFolderClick = {
                                onFolderPick(false)
                            },
                            onRemoveFolderClick = {
                                settings.updateExcludedScanFolders(
                                    excludedScanFolders - it
                                )
                            },
                            availableOptionsTitle = context.resources.getString(R.string.folders_with_audio),
                            availableOptions = foldersWithAudio.toList(),
                            addFolderFromOptionsContentDescription = context.resources.getString(
                                R.string.exclude_folder_from_scanning
                            ),
                            onAddFolderFromOptionsClick = {
                                settings.updateExcludedScanFolders(
                                    excludedScanFolders + it
                                )
                            },
                            onScanClick = onScanFoldersClick,
                            scanContentDescription = context.resources.getString(R.string.scan_for_folders_with_audio),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        val scanOnAppLaunch by settings.scanOnAppLaunch.collectAsState()
        SettingSwitch(
            title = context.resources.getString(R.string.refresh_on_app_launch),
            supportingText = context.resources.getString(R.string.refresh_on_app_launch_explain),
            icon = Icons.Rounded.Autorenew,
            isChecked = scanOnAppLaunch,
            onCheckedChange = settings::updateScanOnAppLaunch
        )

        val coroutineScope = rememberCoroutineScope()
        SettingIconButton(
            title = context.resources.getString(R.string.refresh),
            supportingText = context.resources.getString(R.string.refresh_explain),
            icon = Icons.Rounded.Storage,
            buttonIcon = Icons.Rounded.Refresh,
            buttonContentDescription = context.resources.getString(R.string.refresh_explain),
            onButtonClick = {
                coroutineScope.launch {
                    musicScanner.refreshMedia()
                }
            }
        )

        SettingIconButton(
            title = context.resources.getString(R.string.scan_folder),
            supportingText = context.resources.getString(R.string.scan_folder_explain),
            icon = Icons.Rounded.Folder,
            buttonIcon = Icons.Rounded.Radar,
            buttonContentDescription = context.resources.getString(R.string.scan_folder_explain),
            onButtonClick = {
                onFolderPick(true)
            }
        )
    }
}