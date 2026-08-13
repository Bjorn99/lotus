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
import com.dn0ne.player.core.util.RateLimiter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import javax.net.ssl.SSLException
import com.dn0ne.player.R
import com.dn0ne.player.core.util.getAppVersionName

// CoverArtArchive path prefixes. Named so the two lookups can't be mistyped
// into each other at the call site.
internal const val RELEASE_PATH = "release"
internal const val RELEASE_GROUP_PATH = "release-group"

// Whether a failed release-level cover-art lookup is worth retrying against
// the release-group.
//
// ONLY a 404 qualifies. A 404 means "this pressing has no uploaded art", which
// a sibling release in the same album may well have. Every other failure —
// timeout, no internet, 503, a parse failure on the body — says nothing about
// the group's art and would just spend a second request to fail the same way,
// so those propagate unchanged. Kept pure and separate from the Ktor call so
// the branch can be tested without an HTTP engine.
internal fun shouldRetryWithReleaseGroup(
    error: DataError,
    releaseGroupId: String?,
): Boolean = error == DataError.Network.NotFound && !releaseGroupId.isNullOrBlank()

internal val MBID_REGEX =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
internal val LUCENE_SPECIAL =
    Regex("(&&|\\|\\||[+\\-!(){}\\[\\]^\"~*?:\\\\/])")
// The same set minus ':' and '"'. A structured query is built out of exactly
// those two characters, so escaping them is what silently broke field search:
// `artist:"Chevelle"` went out as `artist\:\"Chevelle\"`, which is not a field
// query at all but the literal words `artist` and `Chevelle`. That both loses
// the field restriction and drags in every recording containing the word
// "artist". Everything else is still escaped, so a stray brace or tilde can't
// reach the parser.
internal val LUCENE_SPECIAL_STRUCTURED =
    Regex("(&&|\\|\\||[+\\-!(){}\\[\\]^~*?\\\\/])")
// Lucene word-level boolean operators — uppercase only, must be
// standalone tokens (word-boundary delimited). Lowercasing them
// preserves the user's intent while preventing Lucene from
// interpreting them as operators that break the query.
internal val AND_OR_NOT = Regex("\\b(AND|OR|NOT)\\b")
internal val HAS_LUCENE_SYNTAX = Regex("\"")
internal val FIELD_NORMALIZE =
    Regex("""\b(Artist|Release|Recording|Track|Dur|Tag|Alias|Arid|Reid|Rgid):""")

internal fun escapeLuceneQuery(query: String): String {
    return query.replace(LUCENE_SPECIAL, "\\\\$1")
}

// Escapes everything [escapeLuceneQuery] does except the two characters that
// carry structure. Only ever applied to a query that passed
// [hasStructuredSyntax].
internal fun escapeStructuredQuery(query: String): String {
    return query.replace(LUCENE_SPECIAL_STRUCTURED, "\\\\$1")
}

// Whether the user is writing a structured query rather than plain text.
//
// A double quote is the signal, but only a BALANCED pair can be parsed. An odd
// count means a stray quote — someone typing an inch mark or a smart-quote
// mismatch — and handing that to Lucene unescaped earns a 400 and a "query was
// corrupted" snackbar. Those fall back to the plain-text path, where the quote
// is escaped and the search still works.
internal fun hasStructuredSyntax(query: String): Boolean {
    val quotes = query.count { it == '"' }
    return quotes >= 2 && quotes % 2 == 0
}

// Normalizes a user query for MusicBrainz Lucene search.
//
// Two paths:
// 1. Plain text (no balanced quotes) — escape every special char AND lowercase
//    AND/OR/NOT.  "Roses AND Thorns" → literal "and".
// 2. Structured (balanced quotes present) — the user is writing Lucene.
//    Lowercase field names (Artist: → artist:) so capitalisation doesn't
//    matter, keep ':' and '"' intact so the query stays a query, and preserve
//    AND/OR/NOT as boolean operators.
internal fun normalizeQuery(query: String): String {
    return if (hasStructuredSyntax(query)) {
        escapeStructuredQuery(
            query.replace(FIELD_NORMALIZE) { it.value.lowercase() }
        )
    } else {
        // Plain text — AND/OR/NOT are likely literal words, lowercase them
        escapeLuceneQuery(query).replace(AND_OR_NOT) { it.value.lowercase() }
    }
}

private fun buildBroadenedQuery(query: String): String {
    // Single-word input: escape special chars and lowercase stray
    // boolean operators so they don't break the constructed query.
    val term = escapeLuceneQuery(query)
        .replace(AND_OR_NOT) { it.value.lowercase() }
    return "recording:\"$term\" OR alias:\"$term\" OR artist:\"$term\""
}

class MusicBrainzMetadataProvider(
    context: Context,
    private val client: HttpClient,
    private val rateLimiter: RateLimiter,
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
        rateLimiter.acquire()

        Log.d(logTag, "search: query=\"$query\" matchDuration=$matchDuration trackDuration=$trackDuration")

        if (MBID_REGEX.matches(query)) {
            return lookupByMbid(query)
        }

        val normalizedQuery = normalizeQuery(query)

        // Query with the optional duration filter appended as a Lucene
        // AND clause.  We also keep the unfiltered query so we can
        // fall back if the duration filter kills all results.
        val narrowQuery = buildString {
            append(normalizedQuery)
            if (matchDuration && trackDuration > 0) {
                append(" AND dur:[${trackDuration - 15000} TO ${trackDuration + 15000}]")
            }
        }

        // Stage 1: Narrow recording search
        val recordingResult = searchRecordings(narrowQuery)
        val stage1Count = if (recordingResult is Result.Success) recordingResult.data.size else -1
        Log.d(logTag, "search: stage1 narrow count=$stage1Count query=\"$narrowQuery\"")
        when (recordingResult) {
            is Result.Error -> return recordingResult
            is Result.Success -> {
                if (recordingResult.data.size >= 1) {
                    return recordingResult
                }
            }
        }

        // If the duration filter was on and we got zero results, retry
        // without it — a mistagged duration shouldn't prevent finding
        // the track entirely.
        if (matchDuration && trackDuration > 0 && recordingResult is Result.Success && recordingResult.data.isEmpty()) {
            rateLimiter.acquire()
            Log.d(logTag, "search: retrying without duration filter, query=\"$normalizedQuery\"")
            val noDurResult = searchRecordings(normalizedQuery)
            val noDurCount = if (noDurResult is Result.Success) noDurResult.data.size else -1
            Log.d(logTag, "search: no-dur fallback count=$noDurCount")
            if (noDurResult is Result.Success && noDurResult.data.isNotEmpty()) {
                return noDurResult
            }
        }

        // Stage 2: Broadened recording search
        // Conditions: single word, no Lucene syntax, not a UUID
        val shouldBroaden = !query.contains(' ') &&
            !HAS_LUCENE_SYNTAX.containsMatchIn(query) &&
            !MBID_REGEX.matches(query)

        if (shouldBroaden) {
            rateLimiter.acquire()
            val broadenedQuery = buildString {
                append("(")
                append(buildBroadenedQuery(query))
                append(")")
                if (matchDuration && trackDuration > 0) {
                    append(" AND dur:[${trackDuration - 15000} TO ${trackDuration + 15000}]")
                }
            }
            val broadenedResult = searchRecordings(broadenedQuery)
            val stage2Count = if (broadenedResult is Result.Success) broadenedResult.data.size else -1
            Log.d(logTag, "search: stage2 broadened count=$stage2Count query=\"$broadenedQuery\"")
            when (broadenedResult) {
                is Result.Success -> {
                    if (broadenedResult.data.size >= 1) {
                        return broadenedResult
                    }
                }
                is Result.Error -> { /* fall through to stage 3 */ }
            }
        }

        Log.d(logTag, "search: final returning ${(recordingResult as? Result.Success)?.data?.size ?: "error"} results")
        return recordingResult
    }

    private suspend fun searchRecordings(
        query: String,
    ): Result<List<MetadataSearchResult>, DataError> {
        val response = try {
            client.get(musicBrainzEndpoint) {
                url {
                    appendPathSegments("recording")
                    parameters.append("fmt", "json")
                    parameters.append("query", query)
                    parameters.append("limit", "50")
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
                    // "release-groups" is NOT implied by "releases": on the
                    // lookup endpoint each linked entity needs its own inc
                    // token. Without it every release comes back with a null
                    // release-group, which would silently disable both the
                    // de-dup below and the cover-art group fallback for any
                    // track matched by MBID. Same request, no extra call.
                    parameters.append(
                        "inc",
                        "artist-credits releases release-groups media"
                    )
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

    // Cover art, with a release-group fallback.
    //
    // CoverArtArchive coverage is per-RELEASE, but a library holds one
    // particular pressing. Asking only for `release/{id}/front` therefore 404s
    // constantly on regional pressings and remasters whose album plainly does
    // have art uploaded against a sibling release. `release-group/{id}/front`
    // returns the group's representative front image and succeeds whenever ANY
    // release in the album has one, so it is a far larger hit set.
    //
    // Order matters: the release-specific image is tried FIRST because it is
    // the art for the exact pressing the user holds; the group image is only a
    // fallback for when that doesn't exist.
    override suspend fun getCoverArtBytes(searchResult: MetadataSearchResult): Result<ByteArray, DataError> {
        val fromRelease = fetchCoverArtFront(RELEASE_PATH, searchResult.albumId)
        if (fromRelease is Result.Success) return fromRelease

        val releaseGroupId = searchResult.releaseGroupId
        if (fromRelease is Result.Error &&
            releaseGroupId != null &&
            shouldRetryWithReleaseGroup(fromRelease.error, releaseGroupId)
        ) {
            Log.d(logTag, "no cover art for release; trying release-group")
            return fetchCoverArtFront(RELEASE_GROUP_PATH, releaseGroupId)
        }

        return fromRelease
    }

    // A single CoverArtArchive "front image" request. Both the release and the
    // release-group lookups go through here so they cannot drift apart: same
    // redirect handling, same status mapping, same user agent. The group
    // endpoint 307s to archive.org exactly as the release endpoint does, so
    // the existing validated-redirect path covers it unchanged — no new host,
    // no relaxation of the allow-list.
    private suspend fun fetchCoverArtFront(
        entity: String,
        id: String,
    ): Result<ByteArray, DataError> {
        // Acquired per request rather than per call, so the fallback is rate
        // limited too instead of riding in free behind the first request.
        rateLimiter.acquire()

        val response = try {
            client.get(coverArtArchiveEndpoint) {
                url {
                    appendPathSegments(entity, id, "front")
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

        return when (response.status) {
            HttpStatusCode.OK -> {
                try {
                    Result.Success(response.body<ByteArray>())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.w(logTag, "Failed to read cover-art body", e)
                    Result.Error(DataError.Network.ParseError)
                }
            }

            HttpStatusCode.TemporaryRedirect, HttpStatusCode.PermanentRedirect -> {
                // CoverArtArchive 30x-redirects to its CDN (archive.org / S3)
                // for the actual image bytes. The shared HttpClient has
                // followRedirects = false as a defence-in-depth measure, so
                // we follow this one redirect ourselves with explicit checks
                // on the destination URL.
                followCoverArtRedirect(response)
            }

            HttpStatusCode.BadRequest -> Result.Error(DataError.Network.BadRequest)
            HttpStatusCode.NotFound -> Result.Error(DataError.Network.NotFound)
            HttpStatusCode.ServiceUnavailable -> Result.Error(DataError.Network.ServiceUnavailable)
            else -> Result.Error(DataError.Network.Unknown)
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

// Flattens the search response into pickable rows, then ranks and collapses
// them.
//
// The previous version emitted one row per (recording × release) in whatever
// order MusicBrainz returned. A popular song has dozens of releases — regional
// pressings, remasters, compilations — so the canonical album ended up buried
// mid-list and the same album appeared many times over.
//
// Every signal used to fix that (score, status, date, release-group) already
// arrives in the response we were parsing, so ranking costs no extra network
// call; we were simply discarding it.
internal fun SearchResultDto.toMetadataSearchResultList(): List<MetadataSearchResult> {
    val rows = mutableListOf<RankedRow>()
    recordings.fastForEach { recording ->
        val artist = recording.artistCredit.ifEmpty { null }?.map {
            it.name + (it.joinphrase ?: "")
        }?.joinToString(separator = "") ?: ""
        val genres = recording.tags?.map { it.name }
        val score = recording.score.asIntOrNull()

        recording.releases?.forEach { release ->
            val albumArtist = release.artistCredit?.map {
                it.name + (it.joinphrase ?: "")
            }?.joinToString(separator = "") ?: artist
            val trackNumber = release.media?.firstOrNull()?.track?.firstOrNull()?.number
            rows += RankedRow(
                result = MetadataSearchResult(
                    id = recording.id,
                    title = recording.title,
                    artist = artist,
                    albumId = release.id,
                    album = release.title,
                    albumArtist = albumArtist,
                    trackNumber = trackNumber,
                    // Deliberately the RECORDING's first-release date, as
                    // before — this is the value written to the year tag and
                    // changing it is not this patch's business. The release's
                    // own date is used for ranking only, just below.
                    year = recording.firstReleaseDate,
                    genres = genres,
                    description = recording.disambiguation,
                    albumDescription = release.disambiguation,
                    releaseGroupId = release.releaseGroup?.id,
                    albumArtistId = release.artistCredit?.firstOrNull()?.id,
                ),
                score = score,
                statusRank = statusRank(release.status),
                typeRank = releaseTypeRank(
                    primaryType = release.releaseGroup?.primaryType,
                    secondaryTypes = release.releaseGroup?.secondaryTypes,
                ),
                year = releaseYear(release.date),
                ordinal = rows.size,
            )
        }
    }

    return rows
        .sortedWith(RANKED_ROW_ORDER)
        .map { it.result }
        // One row per album. Sorting first means the survivor of each group is
        // its best-ranked pressing. Releases with no group id can't be grouped,
        // so they fall back to their own release id and are always kept.
        .distinctBy { it.releaseGroupId ?: it.albumId }
}

// A row plus its precomputed sort keys. The keys are computed once, up front,
// so the comparator is a pure function of stored values — a comparator that
// recomputed or randomised anything would risk violating transitivity and
// blowing up inside sort.
internal data class RankedRow(
    val result: MetadataSearchResult,
    val score: Int?,
    val statusRank: Int,
    val typeRank: Int,
    val year: Int?,
    val ordinal: Int,
)

// Desirability order, most desirable first. Every criterion is total and
// deterministic, and `ordinal` at the end keeps the original API order as the
// final tiebreak so the result never depends on sort implementation details.
internal val RANKED_ROW_ORDER: Comparator<RankedRow> =
    compareByDescending<RankedRow> { it.score ?: Int.MIN_VALUE }
        .thenBy { it.statusRank }
        .thenBy { it.typeRank }
        .thenBy { it.year ?: Int.MAX_VALUE }
        .thenBy { it.ordinal }

// MusicBrainz sends `score` as a search-relevance percentage. It has been
// serialised both as a bare number and as a quoted string over the years, and
// the app's Json is type-strict (`ignoreUnknownKeys` only, no `isLenient`), so
// pinning the field to one of those shapes risks a parse failure that would
// take the WHOLE response down — one odd value, zero search results. Holding
// the raw primitive accepts either shape, and anything unexpected degrades to
// a null score (that row simply ranks last) instead of an error.
internal fun JsonPrimitive?.asIntOrNull(): Int? = this?.content?.toIntOrNull()

// Official pressings first, bootlegs last. Unknown/absent status sits in the
// middle: plenty of legitimate releases carry no status, so an unrecognised
// value must not be treated as worse than a known-bad one.
internal fun statusRank(status: String?): Int = when (status?.lowercase()) {
    "official" -> 0
    null -> 1
    "promotion" -> 2
    "bootleg" -> 3
    "pseudo-release" -> 4
    else -> 1
}

// Prefers the album a track actually belongs to over the ways it was later
// repackaged. Primary type orders the main kinds; any secondary type
// (Compilation, Live, Remix, Soundtrack…) demotes the release within its
// primary tier, which is what pushes greatest-hits collections below the
// original album they borrow from.
internal fun releaseTypeRank(
    primaryType: String?,
    secondaryTypes: List<String>? = null,
): Int {
    val primary = when (primaryType?.lowercase()) {
        "album" -> 0
        "ep" -> 1
        "single" -> 2
        "broadcast" -> 3
        else -> 4
    }
    val repackaged = if (secondaryTypes.isNullOrEmpty()) 0 else 1
    return primary * 2 + repackaged
}

// Year from an ISO-8601 MusicBrainz date, which may be "2016-08-26", "2016-08"
// or just "2016". Earliest wins, so an original pressing outranks its
// remasters. Anything unparseable returns null and sorts last.
internal fun releaseYear(date: String?): Int? =
    date?.take(4)?.takeIf { it.length == 4 }?.toIntOrNull()

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
    val tags: List<Tag>? = null,
    // Search relevance, 0-100. Present on search hits only — the lookup-by-MBID
    // endpoint returns a single exact match and sends no score, so this is
    // null on that path and ranking falls through to the criteria below it.
    // Held as a raw primitive rather than an Int; see asIntOrNull().
    val score: JsonPrimitive? = null,
)

@Serializable
internal data class Artist(
    val name: String,
    val joinphrase: String? = null,
    val id: String? = null,
)

@Serializable
internal data class Release(
    val id: String,
    val title: String,
    @SerialName("artist-credit")
    val artistCredit: List<Artist>? = null,
    val media: List<Media>? = null,
    val disambiguation: String? = null,
    // Ranking signals. All of these already arrive in the search response;
    // they were being parsed away before, which is why results came out
    // unordered. (`country` is deliberately NOT parsed: it arrives too, but
    // nothing ranks by it and an unused field is just clutter.)
    val status: String? = null,
    val date: String? = null,
    @SerialName("release-group")
    val releaseGroup: ReleaseGroup? = null,
)

@Serializable
internal data class ReleaseGroup(
    val id: String,
    @SerialName("primary-type")
    val primaryType: String? = null,
    @SerialName("secondary-types")
    val secondaryTypes: List<String>? = null,
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
