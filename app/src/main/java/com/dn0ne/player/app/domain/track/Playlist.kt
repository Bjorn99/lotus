package com.dn0ne.player.app.domain.track

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Playlist(
    val name: String?,
    val trackList: List<Track>
)
