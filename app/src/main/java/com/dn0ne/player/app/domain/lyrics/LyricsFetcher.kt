package com.dn0ne.player.app.domain.lyrics

import com.dn0ne.player.R
import com.dn0ne.player.app.data.LyricsReader
import com.dn0ne.player.app.data.remote.lyrics.LyricsProvider
import com.dn0ne.player.app.data.repository.LyricsRepository
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarController
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Small facade around the three lyrics data sources. Extracted from
 * `PlayerViewModel` to keep the VM focused on playback + navigation state;
 * user-visible snackbar handling is preserved exactly as the VM had it so
 * this is a pure move, not a behaviour change.
 */
class LyricsFetcher(
    private val lyricsReader: LyricsReader,
    private val lyricsProvider: LyricsProvider,
    private val lyricsRepository: LyricsRepository,
) {

    /**
     * Reads embedded lyrics from the file's ID3/Vorbis tags. Synchronous —
     * `LyricsReader` already handles the heavy I/O. Errors surface as a
     * snackbar launched on [scope] so callers don't block on the UI thread.
     */
    fun readFromTag(track: Track, scope: CoroutineScope): Lyrics? {
        val readResult = lyricsReader.readFromTag(track)
        return when (readResult) {
            is Result.Success -> readResult.data
            is Result.Error -> {
                val messageRes = when (readResult.error) {
                    DataError.Local.NoReadPermission -> R.string.no_read_permission
                    DataError.Local.FailedToRead -> R.string.failed_to_read
                    else -> R.string.unknown_error_occurred
                }
                scope.launch {
                    SnackbarController.sendEvent(SnackbarEvent(message = messageRes))
                }
                null
            }
        }
    }

    /**
     * Fetches lyrics from the configured remote provider (LRCLIB). Caches a
     * successful result in the local repository. Network / parse errors
     * surface as snackbars; no exception escapes.
     */
    suspend fun fetchFromRemote(track: Track): Lyrics? = withContext(Dispatchers.IO) {
        if (track.title == null || track.artist == null) {
            SnackbarController.sendEvent(
                SnackbarEvent(message = R.string.cant_look_for_lyrics_title_or_artist_is_missing)
            )
            return@withContext null
        }

        when (val result = lyricsProvider.getLyrics(track)) {
            is Result.Success -> {
                val lyrics = result.data
                lyricsRepository.insertLyrics(lyrics)
                lyrics
            }
            is Result.Error -> {
                val messageRes = when (result.error) {
                    DataError.Network.BadRequest ->
                        R.string.cant_look_for_lyrics_title_or_artist_is_missing
                    DataError.Network.NotFound -> R.string.lyrics_not_found
                    DataError.Network.ParseError -> R.string.failed_to_parse_response
                    DataError.Network.NoInternet -> R.string.no_internet
                    else -> R.string.unknown_error_occurred
                }
                SnackbarController.sendEvent(SnackbarEvent(message = messageRes))
                null
            }
        }
    }
}
