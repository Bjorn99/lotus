package com.dn0ne.player.app.data

import android.content.IntentSender
import com.dn0ne.player.app.domain.metadata.Metadata
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track

interface MetadataWriter {
    val unsupportedWriteFormats: List<String>
    fun writeMetadata(track: Track, metadata: Metadata, onSecurityError: (IntentSender) -> Unit): Result<Unit, DataError.Local>
}