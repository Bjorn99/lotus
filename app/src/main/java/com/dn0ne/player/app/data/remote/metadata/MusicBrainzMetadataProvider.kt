package com.dn0ne.player.app.data.remote.metadata

import android.content.Context
import android.util.Log
import androidx.compose.ui.util.fastForEach
import com.dn0ne.player.app.domain.metadata.MetadataSearchResult
import com.dn0ne.player.app.domain.metadata.ReleaseMetadata
import com.dn0ne.player.app.domain.metadata.ReleaseTrack
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

internal val MBID_REGEX =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
internal val LUCENE_SPECIAL =
    Regex("(&&|\\|\\||[+\\-!(){}\\[\\]^\"~*?:\\\\/])")
internal val HAS_LUCENE_SYNTAX = Regex("\"")
internal val FIELD_NORMALIZE =
    Regex("""\b(Artist|Release|Recording|Track|Dur|Tag|Alias|Arid|Reid|Rgid):""")

internal fun escapeLuceneQuery(query: String): String {
    return query.replace(LUCENE_SPECIAL, "\\\\$1")
}

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
        trackDuration: Long,
        matchDuration: Boolean,
    ): Result<List<MetadataSearchResult>, DataError> {
        delay(1100)

        if (MBID_REGEX.matches(query)) {
            return lookupByMbid(query)
        }

        val escapedQuery = if (HAS_LUCENE_SYNTAX.containsMatchIn(query)) {
            query.replace(FIELD_NORMALIZE) { it.value.lowercase() }
        } else {
            escapeLuceneQuery(query)
        }

        val finalQuery = buildString {
            append(escapedQuery)
            if (matchDuration && trackDuration > 0) {
                append(" AND dur:[${trackDuration - 15000} TO ${trackDuration + 15000}]")
            }
        }

        val response = try {
            client.get(musicBrainzEndpoint) {
                url {
                    appendPathSegments("recording")
                    parameters.append("fmt", "json")
                    parameters.append("query", finalQuery)
                    parameters.append("limit", "50")
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

    private suspend fun lookupByMbid(mbid: String): Result<List<MetadataSearchResult>, DataError> {
        val response = try {
            client.get(musicBrainzEndpoint) {
                url {
                    appendPathSegments("recording", mbid)
                    parameters.append("fmt", "json")
                    parameters.append("inc", "artist-credits releases media")
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

        return when (response.status) {
            HttpStatusCode.OK -> {
                try {
                    val recording: Recording = response.body()
                    Result.Success(
                        SearchResultDto(listOf(recording)).toMetadataSearchResultList()
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: JsonConvertException) {
                    Log.d(logTag, e.message, e)
                    Result.Error(DataError.Network.ParseError)
                } catch (e: Throwable) {
                    Log.w(logTag, "Failed to parse MusicBrainz lookup response", e)
                    Result.Error(DataError.Network.ParseError)
                }
            }
            HttpStatusCode.BadRequest -> Result.Error(DataError.Network.BadRequest)
            HttpStatusCode.NotFound -> Result.Error(DataError.Network.NotFound)
            HttpStatusCode.RequestTimeout -> Result.Error(DataError.Network.RequestTimeout)
            HttpStatusCode.InternalServerError -> Result.Error(DataError.Network.InternalServerError)
            HttpStatusCode.ServiceUnavailable -> Result.Error(DataError.Network.ServiceUnavailable)
            else -> Result.Error(DataError.Network.Unknown)
        }
    }


    override suspend fun getReleaseMetadata(releaseId: String): Result<ReleaseMetadata, DataError> {
        delay(1100)
        val response = try {
            client.get(musicBrainzEndpoint) {
                url {
                    appendPathSegments("release", releaseId)
                    parameters.append("fmt", "json")
                    parameters.append("inc", "recordings+artist-credits+tags+genres")
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

        return when (response.status) {
            HttpStatusCode.OK -> {
                try {
                    val releaseDto: ReleaseDto = response.body()
                    Result.Success(releaseDto.toReleaseMetadata())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: JsonConvertException) {
                    Log.d(logTag, e.message, e)
                    Result.Error(DataError.Network.ParseError)
                } catch (e: Throwable) {
                    Log.w(logTag, "Failed to parse MusicBrainz release response", e)
                    Result.Error(DataError.Network.ParseError)
                }
            }
            HttpStatusCode.BadRequest -> Result.Error(DataError.Network.BadRequest)
            HttpStatusCode.NotFound -> Result.Error(DataError.Network.NotFound)
            HttpStatusCode.RequestTimeout -> Result.Error(DataError.Network.RequestTimeout)
            HttpStatusCode.InternalServerError -> Result.Error(DataError.Network.InternalServerError)
            HttpStatusCode.ServiceUnavailable -> Result.Error(DataError.Network.ServiceUnavailable)
            else -> Result.Error(DataError.Network.Unknown)
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
    // destination. Delegates to validateCoverArtRedirect for the allow-list
    // check, then fetches the image from the validated URL.
    // This is the only place in the app that follows a redirect — everywhere
    // else uses the strict client default.
    private suspend fun followCoverArtRedirect(
        response: io.ktor.client.statement.HttpResponse,
    ): Result<ByteArray, DataError> {
        val validation = validateCoverArtRedirect(
            location = response.headers[HttpHeaders.Location],
            allowedHosts = listOf("archive.org"),
        )
        if (validation is Result.Error) {
            Log.w(logTag, "CoverArtArchive redirect validation failed: ${validation.error}")
            return Result.Error(validation.error)
        }

        val location = (validation as Result.Success).data

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

// Extracted from followCoverArtRedirect so the allow-list logic can be
// unit-tested without a Ktor engine. Validates the Location header of a
// 30x response before we follow it — this is the only redirect path in
// the app, so the validation must be strict.
//
// The IA node hostnames (ia800–ia905) are all subdomains of archive.org,
// so the single "archive.org" entry covers them via the endsWith check.
internal fun validateCoverArtRedirect(
    location: String?,
    allowedHosts: List<String>,
): Result<String, DataError.Network> {
    if (location.isNullOrBlank()) {
        return Result.Error(DataError.Network.Unknown)
    }
    if (!location.startsWith("https://")) {
        return Result.Error(DataError.Network.Unknown)
    }
    val host = io.ktor.http.Url(location).host
    if (allowedHosts.none { host == it || host.endsWith(".$it") }) {
        return Result.Error(DataError.Network.Unknown)
    }
    return Result.Success(location)
}

@Serializable
internal data class SearchResultDto(
    val recordings: List<Recording>
)

internal fun SearchResultDto.toMetadataSearchResultList(): List<MetadataSearchResult> {
    var results = mutableListOf<MetadataSearchResult>()
    recordings.fastForEach { recording ->
        val artist = recording.artistCredit.ifEmpty { null }?.map {
            it.name + (it.joinphrase ?: "")
        }?.joinToString(separator = "") ?: ""
        val genres = recording.tags?.map { it.name }

        recording.releases?.forEach { release ->
            val albumArtist = release.artistCredit?.map {
                it.name + (it.joinphrase ?: "")
            }?.joinToString(separator = "") ?: artist
            val trackNumber = release.media?.firstOrNull()?.track?.firstOrNull()?.number
            results += MetadataSearchResult(
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
        }
    }

    return results
}

@Serializable
internal data class Recording(
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
internal data class Artist(
    val name: String,
    val joinphrase: String? = null
)

@Serializable
internal data class Release(
    val id: String,
    val title: String,
    @SerialName("artist-credit")
    val artistCredit: List<Artist>? = null,
    val media: List<Media>? = null,
    val disambiguation: String? = null
)

@Serializable
internal data class Media(
    val track: List<MediaTrack>
)

@Serializable
internal data class MediaTrack(
    val number: String? = null
)

@Serializable
internal data class Tag(
    val name: String
)
// DTOs for Release lookup (/ws/2/release/{id}?inc=recordings+artist-credits+tags+genres)
@Serializable
internal data class ReleaseDto(
    val title: String,
    val date: String? = null,
    @SerialName("artist-credit")
    val artistCredit: List<Artist> = emptyList(),
    val tags: List<Tag>? = null,
    val genres: List<GenreDto>? = null,
    @SerialName("cover-art-archive")
    val coverArtArchive: CoverArtArchiveDto? = null,
    val media: List<ReleaseMediaDto> = emptyList(),
)

@Serializable
internal data class GenreDto(
    val name: String,
)

@Serializable
internal data class CoverArtArchiveDto(
    val front: Boolean = false,
)

@Serializable
internal data class ReleaseMediaDto(
    val tracks: List<ReleaseTrackDto> = emptyList(),
)

@Serializable
internal data class ReleaseTrackDto(
    val number: String? = null,
    val title: String,
    val recording: ReleaseRecordingDto,
)

@Serializable
internal data class ReleaseRecordingDto(
    val id: String,
    val title: String,
)

internal fun ReleaseDto.toReleaseMetadata(): ReleaseMetadata {
    val artist = artistCredit.ifEmpty { null }?.map {
        it.name + (it.joinphrase ?: "")
    }?.joinToString(separator = "") ?: ""
    val genres = genres?.map { it.name }
        ?: tags?.map { it.name }

    val tracks = media.flatMap { mediaItem ->
        mediaItem.tracks.map { track ->
            ReleaseTrack(
                recordingId = track.recording.id,
                title = track.recording.title,
                trackNumber = track.number,
            )
        }
    }

    return ReleaseMetadata(
        title = title,
        artist = artist,
        date = date,
        genres = genres,
        coverArtArchiveFront = coverArtArchive?.front ?: false,
        tracks = tracks,
    )
}
