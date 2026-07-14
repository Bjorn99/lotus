package com.dn0ne.player.app.presentation.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap

@Composable
fun CoverArt(
    uri: Uri,
    onCoverArtLoaded: (ImageBitmap?) -> Unit = {},
    modifier: Modifier = Modifier,
    trackUri: Uri? = null,
    perTrackArtwork: Boolean = false,
    crossfade: Boolean = true,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(color = MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {

        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxSize(.4f)
        )

        val context = LocalContext.current
        val model = if (perTrackArtwork && trackUri != null) {
            EmbeddedArtModel(trackUri = trackUri, fallbackUri = uri)
        } else {
            uri
        }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(crossfade)
                .build(),
            onSuccess = {
                onCoverArtLoaded(it.result.image.toBitmap().asImageBitmap())
            },
            onError = {
                onCoverArtLoaded(it.result.image?.toBitmap()?.asImageBitmap())
            },
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        )
    }
}