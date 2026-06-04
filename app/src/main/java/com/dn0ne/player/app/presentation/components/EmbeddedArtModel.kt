package com.dn0ne.player.app.presentation.components

import android.net.Uri
import coil3.key.Keyer

data class EmbeddedArtModel(
    val trackUri: Uri,
    val fallbackUri: Uri,
)

class EmbeddedArtKeyer : Keyer<EmbeddedArtModel> {
    override fun key(data: EmbeddedArtModel, options: coil3.request.Options): String {
        return data.trackUri.toString()
    }
}
