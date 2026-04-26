package com.dn0ne.player.app.data.remote.lyrics

import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track

// Tries each provider in order. The first one to find lyrics wins; a NotFound
// from any provider falls through to the next. BadRequest is a property of the
// query (e.g. missing title), not of the source — short-circuit it. The post
// path delegates to the first provider in the list, which is the LRCLIB one
// because that's the only source we can publish to.
class ChainLyricsProvider(
    private val providers: List<LyricsProvider>,
) : LyricsProvider {

    init {
        require(providers.isNotEmpty()) { "ChainLyricsProvider needs at least one provider" }
    }

    override suspend fun getLyrics(track: Track): Result<Lyrics, DataError.Network> {
        var lastError: DataError.Network = DataError.Network.NotFound

        for (provider in providers) {
            when (val result = provider.getLyrics(track)) {
                is Result.Success -> return result
                is Result.Error -> {
                    if (result.error == DataError.Network.BadRequest) {
                        return result
                    }
                    lastError = result.error
                }
            }
        }

        return Result.Error(lastError)
    }

    override suspend fun postLyrics(
        track: Track,
        lyrics: Lyrics,
    ): Result<Unit, DataError.Network> = providers.first().postLyrics(track, lyrics)
}
