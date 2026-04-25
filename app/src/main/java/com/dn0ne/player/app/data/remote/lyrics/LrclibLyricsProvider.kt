package com.dn0ne.player.app.data.remote.lyrics

import android.content.Context
import android.util.Log
import androidx.compose.ui.util.fastForEach
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.lyrics.toSyncedLyrics
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarController
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarEvent
import com.dn0ne.player.core.util.getAppVersionName
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.security.MessageDigest
import javax.net.ssl.SSLException

class LrclibLyricsProvider(
    context: Context,
    private val client: HttpClient
) : LyricsProvider {
    private val lrclibEndpoint = "https://lrclib.net/api"
    private val logTag = "LrclibLyricsProvider"
    // LRCLIB requests an identifying User-Agent with a contact URL so rate-limit
    // / abuse issues can be routed to the right place.
    private val userAgent =
        "${context.resources.getString(R.string.app_name)}/${context.getAppVersionName()} ( https://github.com/Bjorn99/lotus )"

    override suspend fun getLyrics(track: Track): Result<Lyrics, DataError.Network> {
        if (track.title == null || track.artist == null) {
            return Result.Error(DataError.Network.BadRequest)
        }

        val response = try {
            client.get(lrclibEndpoint) {
                url {
                    appendPathSegments("get")
                    parameters.append("track_name", track.title)
                    parameters.append("artist_name", track.artist)
                    track.album?.let {
                        parameters.append("album_name", it)
                    }
                    parameters.append("duration", (track.duration / 1000).toString())
                }
                headers {
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    append(HttpHeaders.UserAgent, userAgent)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return Result.Error(e.toNetworkError())
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                try {
                    val lyricsDto: LyricsDto = response.body()

                    val plainLyrics = lyricsDto.plainLyrics?.split('\n')
                    var syncedLyrics: List<Pair<Int, String>>? = null
                    try {
                        syncedLyrics = lyricsDto.syncedLyrics?.toSyncedLyrics()
                    } catch (e: IllegalArgumentException) {
                        Log.i(logTag, e.message, e)
                    }


                    return Result.Success(
                        data = Lyrics(
                            uri = track.uri.toString(),
                            plain = plainLyrics,
                            synced = syncedLyrics
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: JsonConvertException) {
                    Log.d(logTag, e.message, e)
                    return Result.Error(DataError.Network.ParseError)
                } catch (e: Throwable) {
                    Log.w(logTag, "Failed to parse LRCLIB response", e)
                    return Result.Error(DataError.Network.ParseError)
                }
            }

            HttpStatusCode.NotFound -> {
                return Result.Error(DataError.Network.NotFound)
            }

            else -> {
                return Result.Error(DataError.Network.Unknown)
            }
        }
    }

    override suspend fun postLyrics(track: Track, lyrics: Lyrics): Result<Unit, DataError.Network> {
        if (
            track.title == null ||
            track.artist == null ||
            track.album == null ||
            lyrics.plain == null
        ) {
            return Result.Error(DataError.Network.BadRequest)
        }

        var response = try {
            client.post(lrclibEndpoint) {
                url {
                    appendPathSegments("request-challenge")
                }
                headers {
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    append(HttpHeaders.UserAgent, userAgent)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return Result.Error(e.toNetworkError())
        }

        if (response.status != HttpStatusCode.OK) return Result.Error(DataError.Network.Unknown)

        val challenge = try {
            response.body<ChallengeDto>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w(logTag, "Failed to parse LRCLIB challenge", e)
            return Result.Error(DataError.Network.ParseError)
        }

        SnackbarController.sendEvent(
            SnackbarEvent(
                message = R.string.solving_challenge
            )
        )

        val nonce = solveChallenge(challenge.prefix, challenge.target)

        response = try {
            client.post(lrclibEndpoint) {
                url {
                    appendPathSegments("publish")
                }
                headers {
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    append(HttpHeaders.UserAgent, userAgent)
                    append("X-Publish-Token", "${challenge.prefix}:$nonce")
                }
                contentType(ContentType.Application.Json)
                setBody(
                    PostLyricsDto(
                        trackName = track.title,
                        artistName = track.artist,
                        albumName = track.album,
                        duration = track.duration / 1000,
                        plainLyrics = lyrics.plain.joinToString("\n"),
                        syncedLyrics = lyrics.synced?.syncedLyricsToString() ?: ""
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return Result.Error(e.toNetworkError())
        }

        return if (response.status == HttpStatusCode.Created) {
            Result.Success(Unit)
        } else {
            Log.d(logTag, "Publish failed: ${response.status} ${response.bodyAsText()}")
            Result.Error(DataError.Network.Unknown)
        }
    }

    /**
     * Maps any thrown network-side exception onto our [DataError.Network]
     * vocabulary. The previous code only caught a handful of types and
     * everything else escaped, which is why a flaky-network "post lyrics"
     * could crash the app.
     */
    private fun Throwable.toNetworkError(): DataError.Network {
        return when (this) {
            is UnresolvedAddressException, is UnknownHostException -> {
                Log.i(logTag, "DNS / address resolution failed: $message")
                DataError.Network.NoInternet
            }
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException -> {
                DataError.Network.RequestTimeout
            }
            is SSLException -> {
                Log.w(logTag, "TLS handshake / cert error: $message")
                DataError.Network.Unknown
            }
            is SocketException, is IOException -> {
                Log.w(logTag, "Network I/O error: $message")
                DataError.Network.Unknown
            }
            else -> {
                Log.w(logTag, "Unexpected network failure", this)
                DataError.Network.Unknown
            }
        }
    }

    fun verifyNonce(result: ByteArray, target: ByteArray): Boolean {
        if (result.size != target.size) {
            return false
        }

        for (i in result.indices) {
            if (result[i].toInt() and 0xFF > target[i].toInt() and 0xFF) {
                return false
            } else if (result[i].toInt() and 0xFF < target[i].toInt() and 0xFF) {
                break
            }
        }

        return true
    }

    fun solveChallenge(prefix: String, targetHex: String): String {
        val target = targetHex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        var nonce = 0L

        while (true) {
            val input = "$prefix$nonce"
            val hashed = sha256(input.toByteArray())

            if (verifyNonce(hashed, target)) {
                break
            }

            nonce++
        }

        return nonce.toString()
    }

    fun sha256(input: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input)
    }
}

@Serializable
private data class LyricsDto(
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

@Serializable
private data class ChallengeDto(
    val prefix: String,
    val target: String
)

@Serializable
private data class PostLyricsDto(
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val duration: Int,
    val plainLyrics: String,
    val syncedLyrics: String
)

private fun List<Pair<Int, String>>.syncedLyricsToString(): String {
    return buildString {
        fastForEach {
            append("${it.first.millisToTimestamp()} ${it.second}\n")
        }
    }
}

private fun Int.millisToTimestamp(): String {
    val minutes = (this / 60000).toString()
    val seconds = ((this % 60000) / 1000).toString()
    val centiseconds = ((this % 1000) / 10).toString()

    return "[${minutes.takeLast(2).padStart(2, '0')}:${
        seconds.takeLast(2).padStart(2, '0')
    }.${centiseconds.takeLast(2).padStart(2, '0')}]"
}