package com.dn0ne.player.app.presentation.components

import android.content.Context
import android.content.Intent
import android.util.Log
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.track.Track

private const val LOG_TAG = "ShareTrack"

fun shareTrack(context: Context, track: Track) {
    // MediaStore content URIs already grant read when passed through an
    // Intent with FLAG_GRANT_READ_URI_PERMISSION, so we don't need FileProvider.
    val mime = context.contentResolver.getType(track.uri) ?: "audio/*"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, track.uri)
        track.title?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, context.getString(R.string.share_track))
    try {
        context.startActivity(chooser)
    } catch (t: Throwable) {
        // No share targets / activity not started — don't crash.
        Log.w(LOG_TAG, "Failed to launch share chooser for ${track.uri}", t)
    }
}
