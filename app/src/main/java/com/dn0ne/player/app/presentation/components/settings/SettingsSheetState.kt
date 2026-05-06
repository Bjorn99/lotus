package com.dn0ne.player.app.presentation.components.settings

import com.dn0ne.player.EqualizerController
import com.dn0ne.player.app.data.repository.TrackRepository
import com.dn0ne.player.app.data.repository.TrackStatsRepository
import com.dn0ne.player.core.data.MusicScanner
import com.dn0ne.player.core.data.Settings

data class SettingsSheetState(
    val settings: Settings,
    val musicScanner: MusicScanner,
    val isShown: Boolean = false,
    val equalizerController: EqualizerController,
    val trackStatsRepository: TrackStatsRepository,
    val trackRepository: TrackRepository,
    val foldersWithAudio: Set<String> = emptySet()
)
