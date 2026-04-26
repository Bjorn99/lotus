package com.dn0ne.player.app.data.remote.metadata

import android.content.Context
import android.util.Log
import androidx.compose.ui.util.fastForEach
import com.dn0ne.player.app.domain.metadata.MetadataSearchResult
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.headers
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import javax.net.ssl.SSLException
import com.dn0ne.player.R
import com.dn0ne.player.core.util.getAppVersionName

class MusicBrainzMetadataProvider(
    context: Context,
    private val client: HttpClient
) : MetadataProvider {
    private val logTag = "MBMetadataProvider"
    private val musicBrainzEndpoint = "https://musicbrainz.org/ws/2"
    private val coverArtArchiveEndpoint = "https://coverartarchive.org"
    // MusicBrainz requires an identifying User-Agent with a contact (email or URL);
    // see https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting#User-Agent
    private val userAgent =
        "${context.resources.getString(R.string.app_name)}/${context.getAppVersionName()} ( https://github.com/Bjorn99/lotus )"

    override suspend fun searchMetadata(
        query: String,
        trackDuration: Long
    ): Result<List<MetadataSearchResult>, DataError> {
        delay(1100)
        val query = query + if (trackDuration > 0) {
            " AND dur:[${trackDuration - 5000} TO ${trackDuration + 5000}]"
        } else ""

        val response = try {
            client.get(musicBrainzEndpoint) {
                url {
                    appendPathSegments("recording")
                    parameters.append("fmt", "json")
                    parameters.append("query", query)
                }
                headers {
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    append(HttpHeaders.UserAgent, userAgent)
                }
            }
        } catch (e: CancellationException) {
            // Coroutine cancellation must propagate; never swallow.
            throw e
        } catch (e: Throwable) {
            return Result.Error(e.toNetworkError())
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                try {
                    val searchResult: SearchResultDto = response.body()
                    return Result.Success(
                        data = searchResult.toMetadataSearchResultList()
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: JsonConvertException) {
                    Log.d(logTag, e.message, e)
                    return Result.Error(DataError.Network.ParseError)
                } catch (e: Throwable) {
                    // Defensive — body parsing can throw a wide range of
                    // wrapped Ktor / kotlinx.serialization exceptions.
                    Log.w(logTag, "Failed to parse MusicBrainz search response", e)
                    return Result.Error(DataError.Network.ParseError)
                }
            }

            HttpStatusCode.BadRequest -> {
                return Result.Error(DataError.Network.BadRequest)
            }

            HttpStatusCode.Unauthorized -> {
                return Result.Error(DataError.Network.Unauthorized)
            }

            HttpStatusCode.Forbidden -> {
                return Result.Error(DataError.Network.Forbidden)
            }

            HttpStatusCode.NotFound -> {
                return Result.Error(DataError.Network.NotFound)
            }

            HttpStatusCode.RequestTimeout -> {
                return Result.Error(DataError.Network.RequestTimeout)
            }

            HttpStatusCode.InternalServerError -> {
                return Result.Error(DataError.Network.InternalServerError)
            }

            HttpStatusCode.ServiceUnavailable -> {
                return Result.Error(DataError.Network.ServiceUnavailable)
            }

            else -> {
                return Result.Error(DataError.Network.Unknown)
            }
        }
    }

    override suspend fun getCoverArtBytes(searchResult: MetadataSearchResult): Result<ByteArray, DataError> {
        delay(1100)
        val response = try {
            client.get(coverArtArchiveEndpoint) {
                url {
                    appendPathSegments(
                        "release",
                        searchResult.albumId,
                        "front"
                    )
                }
                headers {
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
                    val bytes = response.body<ByteArray>()
                    return Result.Success(bytes)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.w(logTag, "Failed to read cover-art body", e)
                    return Result.Error(DataError.Network.ParseError)
                }
            }

            HttpStatusCode.TemporaryRedirect, HttpStatusCode.PermanentRedirect -> {
                // CoverArtArchive 30x-redirects to its CDN (archive.org / S3)
                // for the actual image bytes. The shared HttpClient has
                // followRedirects = false as a defence-in-depth measure, so
                // we follow this one redirect ourselves with explicit checks
                // on the destination URL.
                return followCoverArtRedirect(response)
            }

            HttpStatusCode.BadRequest -> {
                return Result.Error(DataError.Network.BadRequest)
            }

            HttpStatusCode.NotFound -> {
                return Result.Error(DataError.Network.NotFound)
            }

            HttpStatusCode.ServiceUnavailable -> {
                return Result.Error(DataError.Network.ServiceUnavailable)
            }

            else -> return Result.Error(DataError.Network.Unknown)
        }
    }

    // Follows a single 30x from CoverArtArchive after validating the
    // destination. Anything sketchy (missing Location, non-HTTPS scheme,
    // unknown host) errors out instead of being followed. This is the only
    // place in the app that follows a redirect — everywhere else uses the
    // strict client default.
    private suspend fun followCoverArtRedirect(
        response: io.ktor.client.statement.HttpResponse,
    ): Result<ByteArray, DataError> {
        val location = response.headers[HttpHeaders.Location]
        if (location.isNullOrBlank()) {
            Log.w(logTag, "CoverArtArchive redirect missing Location header")
            return Result.Error(DataError.Network.Unknown)
        }
        if (!location.startsWith("https://")) {
            Log.w(logTag, "Refusing non-HTTPS CoverArtArchive redirect: $location")
            return Result.Error(DataError.Network.Unknown)
        }
        // Allow-list of redirect targets that CoverArtArchive uses in
        // practice. If they ever add a new CDN host we'll see this fail
        // loudly rather than silently following somewhere unexpected.
        val allowed = listOf("archive.org", "ia800", "ia801", "ia802", "ia803", "ia804", "ia902", "ia903", "ia904", "ia905")
        val host = io.ktor.http.Url(location).host
        if (allowed.none { host.contains(it) }) {
            Log.w(logTag, "Refusing CoverArtArchive redirect to unexpected host: $host")
            return Result.Error(DataError.Network.Unknown)
        }

        val final = try {
            client.get(location) {
                headers { append(HttpHeaders.UserAgent, userAgent) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return Result.Error(e.toNetworkError())
        }

        return when (final.status) {
            HttpStatusCode.OK -> {
                try {
                    Result.Success(final.body<ByteArray>())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.w(logTag, "Failed to read cover-art body after redirect", e)
                    Result.Error(DataError.Network.ParseError)
                }
            }
            HttpStatusCode.NotFound -> Result.Error(DataError.Network.NotFound)
            else -> Result.Error(DataError.Network.Unknown)
        }
    }

    /**
     * Maps any thrown network-side exception onto our [DataError.Network]
     * vocabulary. The previous code only caught a handful of exception
     * types; everything else escaped and crashed the process. This is the
     * single fallback so [searchMetadata] and [getCoverArtBytes] stay
     * consistent and complete.
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
}

@Serializable
private data class SearchResultDto(
    val recordings: List<Recording>
)

private fun SearchResultDto.toMetadataSearchResultList(): List<MetadataSearchResult> {
    var results = mutableListOf<MetadataSearchResult>()
    recordings.fastForEach { recording ->
        val artist = recording.artistCredit.map {
            it.name + (it.joinphrase ?: "")
        }.joinToString(separator = "")
        val genres = recording.tags?.map { it.name }

        recording.releases?.mapNotNull { release ->
            val artistCredit = release.artistCredit ?: return@mapNotNull null
            val media = release.media ?: return@mapNotNull null
            val trackNumber = media.firstOrNull()?.track?.firstOrNull()?.number
                ?: return@mapNotNull null
            val albumArtist = artistCredit.map {
                it.name + (it.joinphrase ?: "")
            }.joinToString(separator = "")
            MetadataSearchResult(
                id = recording.id,
                title = recording.title,
                artist = artist,
                albumId = release.id,
                album = release.title,
                albumArtist = albumArtist,
                trackNumber = trackNumber,
                year = recording.firstReleaseDate,
                genres = genres,
                description = recording.disambiguation,
                albumDescription = release.disambiguation
            )
        }?.let { searchResults ->
            results += searchResults
        }
    }

    return results
}

@Serializable
private data class Recording(
    val id: String,
    val title: String,
    @SerialName("artist-credit")
    val artistCredit: List<Artist>,
    val disambiguation: String? = null,
    @SerialName("first-release-date")
    val firstReleaseDate: String? = null,
    val releases: List<Release>? = null,
    val tags: List<Tag>? = null
)

@Serializable
private data class Artist(
    val name: String,
    val joinphrase: String? = null
)

@Serializable
private data class Release(
    val id: String,
    val title: String,
    @SerialName("artist-credit")
    val artistCredit: List<Artist>? = null,
    val media: List<Media>? = null,
    val disambiguation: String? = null
)

@Serializable
private data class Media(
    val track: List<MediaTrack>
)

@Serializable
private data class MediaTrack(
    val number: String? = null
)

@Serializable
private data class Tag(
    val name: String
)