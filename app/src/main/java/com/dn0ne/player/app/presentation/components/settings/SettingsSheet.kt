package com.dn0ne.player.app.presentation.components.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dn0ne.player.R
import com.dn0ne.player.app.data.backup.ExportResult
import com.dn0ne.player.app.data.backup.ImportResult
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import com.kmpalette.DominantColorState
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    state: SettingsSheetState,
    onFolderPick: (scan: Boolean) -> Unit,
    onPlaylistPick: () -> Unit,
    onScanFoldersClick: () -> Unit,
    onCloseClick: () -> Unit,
    onBackupExport: (Uri, (ExportResult) -> Unit) -> Unit,
    onBackupImport: (Uri, (ImportResult) -> Unit) -> Unit,
    dominantColorState: DominantColorState<ImageBitmap>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isShown,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + slideInVertically(
            initialOffsetY = { it / 10 }),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + slideOutVertically(
            targetOffsetY = { it / 10 }),
    ) {
        BackHandler {
            onCloseClick()
        }

        Box(
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .clickable(
                    enabled = false,
                    onClick = {}
                )
        ) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = SettingsRoutes.Main,
                enterTransition = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                            slideInHorizontally(initialOffsetX = { it / 5 })
                },
                exitTransition = {
                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                            slideOutHorizontally(targetOffsetX = { -it / 5 })
                },
                popEnterTransition = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                            slideInHorizontally(initialOffsetX = { -it / 5 })
                },
                popExitTransition = {
                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                            slideOutHorizontally(targetOffsetX = { it / 5 })
                }
            ) {
                composable<SettingsRoutes.Main> {
                    val context = LocalContext.current
                    var collapseFraction by remember {
                        mutableFloatStateOf(0f)
                    }

                    ColumnWithCollapsibleTopBar(
                        topBarContent = {
                            IconButton(
                                onClick = onCloseClick,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBackIosNew,
                                    contentDescription = context.resources.getString(R.string.close_settings_sheet)
                                )
                            }

                            Text(
                                text = context.resources.getString(R.string.settings),
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
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        contentHorizontalAlignment = Alignment.CenterHorizontally,
                        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    ) {
                        val settings = remember {
                            listOf(
                                SettingsItem(
                                    title = context.resources.getString(R.string.playback),
                                    supportingText = context.resources.getString(R.string.playback_supporting_text),
                                    icon = Icons.Rounded.MusicNote,
                                    onClick = {
                                        navController.navigate(SettingsRoutes.Playback)
                                    }
                                ),
                                SettingsItem(
                                    title = context.resources.getString(R.string.music_scan),
                                    supportingText = context.resources.getString(R.string.music_scan_supporting_text),
                                    icon = Icons.Rounded.Radar,
                                    onClick = {
                                        navController.navigate(SettingsRoutes.MusicScan)
                                    }
                                ),
                                SettingsItem(
                                    title = context.resources.getString(R.string.tabs),
                                    supportingText = context.resources.getString(R.string.tabs_supporting_text),
                                    icon = Icons.Rounded.TableChart,
                                    onClick = {
                                        navController.navigate(SettingsRoutes.Tabs)
                                    }
                                ),
                                SettingsItem(
                                    title = context.resources.getString(R.string.playlists),
                                    supportingText = context.resources.getString(R.string.playlists_supporting_text),
                                    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                    onClick = {
                                        navController.navigate(SettingsRoutes.Playlists)
                                    }
                                ),
                                SettingsItem(
                                    title = context.resources.getString(R.string.theme),
                                    supportingText = context.resources.getString(R.string.theme_supporting_text),
                                    icon = Icons.Rounded.ColorLens,
                                    onClick = {
                                        navController.navigate(SettingsRoutes.Theme)
                                    }
                                ),
                                SettingsItem(
                                    title = context.resources.getString(R.string.lyrics),
                                    supportingText = context.resources.getString(R.string.lyrics_supporting_text),
                                    icon = Icons.Rounded.Lyrics,
                                    onClick = {
                                        navController.navigate(SettingsRoutes.Lyrics)
                                    }
                                ),
                                SettingsItem(
                                    title = context.resources.getString(R.string.privacy),
                                    supportingText = context.resources.getString(R.string.privacy_supporting_text),
                                    icon = Icons.Rounded.Lock,
                                    onClick = {
                                        navController.navigate(SettingsRoutes.Privacy)
                                    }
                                ),
                                SettingsItem(
                                    title = context.resources.getString(R.string.backup),
                                    supportingText = context.resources.getString(R.string.backup_supporting_text),
                                    icon = Icons.Rounded.Backup,
                                    onClick = {
                                        navController.navigate(SettingsRoutes.Backup)
                                    }
                                ),
                                SettingsItem(
                                    title = context.resources.getString(R.string.stats_title),
                                    supportingText = context.resources.getString(R.string.stats_supporting_text),
                                    icon = Icons.Rounded.Insights,
                                    onClick = {
                                        navController.navigate(SettingsRoutes.Stats)
                                    }
                                )
                            )
                        }

                        SettingsGroup(
                            items = settings
                        )

                        SettingsGroup(
                            items = listOf(
                                SettingsItem(
                                    title = context.resources.getString(R.string.about_app),
                                    supportingText = context.resources.getString(R.string.about_explain),
                                    icon = Icons.Rounded.Info,
                                    onClick = {
                                        navController.navigate(SettingsRoutes.About)
                                    }
                                )
                            )
                        )
                    }
                }

                composable<SettingsRoutes.Playback> {
                    PlaybackSettings(
                        settings = state.settings,
                        equalizerController = state.equalizerController,
                        onBackClick = {
                            navController.navigateUp()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable<SettingsRoutes.MusicScan> {
                    MusicScanSettings(
                        settings = state.settings,
                        musicScanner = state.musicScanner,
                        foldersWithAudio = state.foldersWithAudio,
                        onFolderPick = onFolderPick,
                        onScanFoldersClick = onScanFoldersClick,
                        onBackClick = {
                            navController.navigateUp()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable<SettingsRoutes.Tabs> {
                    TabsSettings(
                        settings = state.settings,
                        onBackClick = {
                            navController.navigateUp()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable<SettingsRoutes.Playlists> {
                    PlaylistsSettings(
                        settings = state.settings,
                        onPlaylistPick = onPlaylistPick,
                        onBackClick = {
                            navController.navigateUp()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable<SettingsRoutes.Theme> {
                    ThemeSettings(
                        settings = state.settings,
                        onBackClick = {
                            navController.navigateUp()
                        },
                        dominantColorState = dominantColorState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable<SettingsRoutes.Lyrics> {
                    LyricsSettings(
                        settings = state.settings,
                        onBackClick = {
                            navController.navigateUp()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable<SettingsRoutes.Privacy> {
                    PrivacySettings(
                        settings = state.settings,
                        onBackClick = {
                            navController.navigateUp()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable<SettingsRoutes.Backup> {
                    BackupSettings(
                        onBackClick = {
                            navController.navigateUp()
                        },
                        onExport = onBackupExport,
                        onImport = onBackupImport,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable<SettingsRoutes.About> {
                    AboutPage(
                        onBackClick = {
                            navController.navigateUp()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable<SettingsRoutes.Stats> {
                    LibraryStatsScreen(
                        settings = state.settings,
                        statsRepository = state.trackStatsRepository,
                        trackRepository = state.trackRepository,
                        onBackClick = {
                            navController.navigateUp()
                        },
                        onOpenPrivacyClick = {
                            navController.navigate(SettingsRoutes.Privacy)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Serializable
sealed interface SettingsRoutes {
    @Serializable
    data object Main : SettingsRoutes

    @Serializable
    data object Playback : SettingsRoutes

    @Serializable
    data object MusicScan : SettingsRoutes

    @Serializable
    data object Tabs : SettingsRoutes

    @Serializable
    data object Playlists : SettingsRoutes

    @Serializable
    data object Theme : SettingsRoutes

    @Serializable
    data object Lyrics : SettingsRoutes

    @Serializable
    data object Privacy : SettingsRoutes

    @Serializable
    data object Backup : SettingsRoutes

    @Serializable
    data object About : SettingsRoutes

    @Serializable
    data object Stats : SettingsRoutes
}