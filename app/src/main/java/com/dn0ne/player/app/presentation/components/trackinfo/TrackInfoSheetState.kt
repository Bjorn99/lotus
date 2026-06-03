package com.dn0ne.player.app.presentation.components.trackinfo

import com.dn0ne.player.app.domain.track.Track

data class TrackInfoSheetState(
    val isShown: Boolean = false,
    val track: Track? = null,
    val showRisksOfMetadataEditingDialog: Boolean = true,
    val isMetadataWritable: Boolean = true,
    val infoSearchSheetState: InfoSearchSheetState = InfoSearchSheetState(),
    val changesSheetState: ChangesSheetState = ChangesSheetState(),
    val manualInfoEditSheetState: ManualInfoEditSheetState = ManualInfoEditSheetState(),
    val lyricsControlSheetState: LyricsControlSheetState = LyricsControlSheetState()
)
