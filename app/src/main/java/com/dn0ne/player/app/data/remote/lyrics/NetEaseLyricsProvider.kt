package com.dn0ne.player.app.data.remote.lyrics

import android.util.Log
import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.lyrics.toSyncedLyrics
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

// Backup lyrics source. NetEase has a wide catalogue (a lot of pop the LRCLIB
// crowd doesn't bother with) and the search/lyric pair below is the same one
// every other open-source music app on Android uses. The endpoints are
// undocumented but stable for years; the Referer header is the one bit of
// "looks like a browser" we have to send or the API returns 502.
class NetEaseLyricsProvider(
    private val client: HttpClient,
) : LyricsProvider {
    private val searchEndpoint = "https://music.163.com/api/search/get/web"
    private val lyricEndpoint = "https://music.163.com/api/song/lyric"
    private val logTag = "NetEaseLyricsProvider"

    override suspend fun getLyrics(track: Track): Result<Lyrics, DataError.Network> {
        if (track.title == null || track.artist == null) {
            return Result.Error(DataError.Network.BadRequest)
        }

        val songIdResult = search(track) ?: return Result.Error(DataError.Network.NotFound)
        val songId = when (songIdResult) {
            is Result.Success -> songIdResult.data
            is Result.Error -> return Result.Error(songIdResult.error)
        }

        return fetchLyric(songId, track)
    }

    // NetEase is read-only — there's no public publish endpoint, and we don't
    // want to pretend otherwise.
    override suspend fun postLyrics(
        track: Track,
        lyrics: Lyrics,
    ): Result<Unit, DataError.Network> = Result.Error(DataError.Network.BadRequest)

    private suspend fun search(track: Track): Result<Long, DataError.Network>? {
        val query = "${track.artist} ${track.title}"
        val response = try {
            client.get(searchEndpoint) {
                url {
                    parameters.append("s", query)
                    parameters.append("type", "1")
                    parameters.append("limit", "10")
                    parameters.append("offset", "0")
                }
                headers {
                    append(HttpHeaders.Referrer, "https://music.163.com/")
                    append(HttpHeaders.UserAgent, BROWSER_UA)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return Result.Error(e.toNetworkError(logTag))
        }

        if (response.status != HttpStatusCode.OK) {
            return Result.Error(DataError.Network.Unknown)
        }

        val dto = try {
            response.body<SearchResponse>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: JsonConvertException) {
            Log.d(logTag, "Failed to parse NetEase search response", e)
            return Result.Error(DataError.Network.ParseError)
        } catch (e: Throwable) {
            Log.w(logTag, "Failed to parse NetEase search response", e)
            return Result.Error(DataError.Network.ParseError)
        }

        val songs = dto.result?.songs.orEmpty()
        if (songs.isEmpty()) return null

        val expectedDurationMs = track.duration
        val pick = songs.minByOrNull { song ->
            // Lower score = better match. Duration distance dominates; missing
            // duration falls back to "first result", which is what the search
            // endpoint already ranks by relevance.
            val durationDelta = song.duration?.let { abs(it - expectedDurationMs) } ?: Int.MAX_VALUE
            durationDelta
        } ?: return null

        return Result.Success(pick.id)
    }

    private suspend fun fetchLyric(
        songId: Long,
        track: Track,
    ): Result<Lyrics, DataError.Network> {
        val response = try {
            client.get(lyricEndpoint) {
                url {
                    parameters.append("id", songId.toString())
                    parameters.append("lv", "1")
                    parameters.append("kv", "1")
                    parameters.append("tv", "-1")
                }
                headers {
                    append(HttpHeaders.Referrer, "https://music.163.com/")
                    append(HttpHeaders.UserAgent, BROWSER_UA)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return Result.Error(e.toNetworkError(logTag))
        }

        if (response.status != HttpStatusCode.OK) {
            return Result.Error(DataError.Network.Unknown)
        }

        val dto = try {
            response.body<LyricResponse>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: JsonConvertException) {
            Log.d(logTag, "Failed to parse NetEase lyric response", e)
            return Result.Error(DataError.Network.ParseError)
        } catch (e: Throwable) {
            Log.w(logTag, "Failed to parse NetEase lyric response", e)
            return Result.Error(DataError.Network.ParseError)
        }

        val raw = dto.lrc?.lyric?.takeIf { it.isNotBlank() }
            ?: return Result.Error(DataError.Network.NotFound)

        val syncedLines = try {
            raw.toSyncedLyrics()
        } catch (e: IllegalArgumentException) {
            // No timestamps. Fall back to plain text — split on lines and
            // strip any stray bracket-prefixed metadata ([ar:], [ti:], …).
            null
        }

        val plainLines = raw.split('\n')
            .map { line -> line.replace(LRC_LEADING_TAGS, "").trim() }
            .filter { it.isNotEmpty() }

        return Result.Success(
            Lyrics(
                uri = track.uri.toString(),
                plain = plainLines.takeIf { it.isNotEmpty() },
                synced = syncedLines,
            )
        )
    }

    @Serializable
    private data class SearchResponse(
        val result: SearchResult? = null,
        val code: Int = 0,
    )

    @Serializable
    private data class SearchResult(
        val songs: List<SearchSong>? = null,
    )

    @Serializable
    private data class SearchSong(
        val id: Long,
        val name: String? = null,
        @SerialName("duration") val duration: Int? = null,
    )

    @Serializable
    private data class LyricResponse(
        val lrc: LyricBody? = null,
        val code: Int = 0,
    )

    @Serializable
    private data class LyricBody(
        val lyric: String? = null,
    )

    private companion object {
        // A plain "ktor-client" UA gets blocked by NetEase's edge. Any
        // browser-shaped UA works; this one is generic and doesn't lie about
        // a specific Chrome version that'll get stale.
        const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Mobile"

        // Strip every leading [mm:ss.xx] timestamp (lines can have several)
        // and standalone metadata tags ([ar:], [ti:], [by:]) so the plain-text
        // view doesn't leak them.
        val LRC_LEADING_TAGS = Regex("""^(\[[^\]]+\])+""")
    }
}
