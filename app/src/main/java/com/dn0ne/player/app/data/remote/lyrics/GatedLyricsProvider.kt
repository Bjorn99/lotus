package com.dn0ne.player.app.data.remote.lyrics

import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track

// Tiny wrapper that lets a chain skip a provider at runtime based on a
// settings flag. Disabled providers report NotFound so the chain falls
// through to the next entry instead of treating the disable as an error.
class GatedLyricsProvider(
    private val delegate: LyricsProvider,
    private val isEnabled: () -> Boolean,
) : LyricsProvider {
    override suspend fun getLyrics(track: Track): Result<Lyrics, DataError.Network> =
        if (isEnabled()) delegate.getLyrics(track) else Result.Error(DataError.Network.NotFound)

    override suspend fun postLyrics(
        track: Track,
        lyrics: Lyrics,
    ): Result<Unit, DataError.Network> = delegate.postLyrics(track, lyrics)
}
