package com.dn0ne.player.app.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dn0ne.player.EqualizerController
import com.dn0ne.player.app.data.LyricsReader
import com.dn0ne.player.app.data.LyricsReaderImpl
import com.dn0ne.player.app.data.MetadataWriter
import com.dn0ne.player.app.data.MetadataWriterImpl
import com.dn0ne.player.app.data.SavedPlayerState
import com.dn0ne.player.app.data.backup.BackupManager
import com.dn0ne.player.app.data.db.LotusDatabase
import com.dn0ne.player.app.data.db.LovedTrackDao
import com.dn0ne.player.app.data.db.LyricsDao
import com.dn0ne.player.app.data.db.PlaylistDao
import com.dn0ne.player.app.data.db.TrackStatsDao
import com.dn0ne.player.app.data.remote.lyrics.ChainLyricsProvider
import com.dn0ne.player.app.data.remote.lyrics.GatedLyricsProvider
import com.dn0ne.player.app.data.remote.lyrics.LrclibLyricsProvider
import com.dn0ne.player.app.data.remote.lyrics.LyricsProvider
import com.dn0ne.player.core.data.Settings
import com.dn0ne.player.app.data.remote.metadata.GatedMetadataProvider
import com.dn0ne.player.app.data.remote.metadata.MetadataProvider
import com.dn0ne.player.app.data.remote.metadata.MusicBrainzMetadataProvider
import com.dn0ne.player.core.util.RateLimiter
import com.dn0ne.player.app.data.repository.LovedTracksRepository
import com.dn0ne.player.app.data.repository.LyricsRepository
import com.dn0ne.player.app.data.repository.PlaylistRepository
import com.dn0ne.player.app.data.repository.TrackRepository
import com.dn0ne.player.app.data.repository.TrackStatsRepository
import com.dn0ne.player.app.presentation.PlayerViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.contentLength
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// Extracted so the size-cap check can be unit-tested without a Ktor engine.
// Called by the HttpResponseValidator block in the HttpClient factory below.
internal fun validateResponseSize(contentLength: Long?, maxBytes: Long) {
    if (contentLength != null && contentLength > maxBytes) {
        throw IOException(
            "Refusing response: declared Content-Length $contentLength exceeds cap of $maxBytes"
        )
    }
}

// 5 MB is well above the largest legitimate response we make (album cover
// art tops out around a couple hundred KB; lyrics and JSON metadata are
// tiny). Picked to leave generous headroom while still catching the
// "firehose" attack shape.
private const val MAX_RESPONSE_BYTES = 5L * 1024 * 1024

// v1 → v2: add the loved_tracks table for the Loved-tracks feature.
// Pure additive — no existing-data transformation, safe on every device.
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `loved_tracks` (" +
                "`uri` TEXT NOT NULL, " +
                "`added_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`uri`))"
        )
    }
}

// v2 → v3: add track_stats for play/skip counts and listening time. Also
// purely additive; backfilled to empty (no historical events to recover).
// Column definitions must match exactly what Room generates from
// TrackStatsEntity — Room hashes the schema on open and rejects mismatches.
// No DEFAULT clauses here since the entity doesn't declare @ColumnInfo
// defaultValue; Kotlin-side defaults handle that at the entity level.
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `track_stats` (" +
                "`uri` TEXT NOT NULL, " +
                "`play_count` INTEGER NOT NULL, " +
                "`skip_count` INTEGER NOT NULL, " +
                "`total_listening_ms` INTEGER NOT NULL, " +
                "`first_played_at` INTEGER, " +
                "`last_played_at` INTEGER, " +
                "PRIMARY KEY(`uri`))"
        )
    }
}

val playerModule = module {

    single {
        TrackRepository(
            context = androidContext(),
            settings = get()
        )
    }

    single<SavedPlayerState> {
        SavedPlayerState(
            context = androidContext()
        )
    }

    single {
        RateLimiter().also {
            it.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        }
    }

    single<HttpClient> {
        HttpClient(OkHttp) {
            // Strict redirect policy: don't auto-follow. A poisoned DNS
            // response or a misbehaving upstream that returns a 30x can
            // divert the request anywhere; we'd rather see that as an error
            // and decide what to do. The one place that legitimately needs
            // a redirect (CoverArtArchive → S3) handles its own 307 with
            // explicit URL validation.
            followRedirects = false

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(HttpTimeout) {
                // Snappy enough that a flaky network produces a user-visible
                // error inside the attention span of someone who just tapped
                // "Search" — the previous 3-minute total was effectively
                // "the app is hung".
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }

            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                retryOnException(maxRetries = 3, retryOnTimeout = true)
                exponentialDelay(
                    base = 2.0,
                    maxDelayMs = 60_000L,
                    randomizationMs = 1000,
                )
            }

            // Cap response size so a malicious or buggy upstream can't fill
            // memory with a multi-GB body. Headers-phase only — checks the
            // declared Content-Length before we consume the body. Responses
            // that omit Content-Length still get killed by socketTimeout
            // long before they fill anything significant.
            HttpResponseValidator {
                validateResponse { response ->
                    validateResponseSize(response.contentLength(), MAX_RESPONSE_BYTES)
                }
            }
        }
    }

    single<MetadataProvider> {
        val settings = get<Settings>()
        GatedMetadataProvider(
            delegate = MusicBrainzMetadataProvider(
                context = androidContext(),
                client = get(),
                rateLimiter = get(),
            ),
            isEnabled = { settings.networkLookupsEnabled },
        )
    }

    single<MetadataWriter> {
        MetadataWriterImpl(context = androidContext())
    }

    single<LyricsProvider> {
        val settings = get<Settings>()
        val lrclib = LrclibLyricsProvider(
            context = androidContext(),
            client = get(),
        )
        GatedLyricsProvider(
            delegate = ChainLyricsProvider(listOf(lrclib)),
            isEnabled = { settings.networkLookupsEnabled },
        )
    }

    single<LotusDatabase> {
        Room.databaseBuilder(
            androidContext(),
            LotusDatabase::class.java,
            LotusDatabase.NAME,
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }
    single<PlaylistDao> { get<LotusDatabase>().playlistDao() }
    single<LyricsDao> { get<LotusDatabase>().lyricsDao() }
    single<LovedTrackDao> { get<LotusDatabase>().lovedTrackDao() }
    single<TrackStatsDao> { get<LotusDatabase>().trackStatsDao() }

    single {
        LyricsRepository(dao = get())
    }

    single {
        PlaylistRepository(dao = get())
    }

    single {
        LovedTracksRepository(dao = get())
    }

    single {
        TrackStatsRepository(dao = get())
    }

    single<BackupManager> {
        val ctx = androidContext()
        val versionName = try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "unknown"
        } catch (t: Throwable) {
            "unknown"
        }
        BackupManager(
            context = ctx,
            playlistRepository = get(),
            lovedTracksRepository = get(),
            trackStatsRepository = get(),
            trackRepository = get(),
            settings = get(),
            appVersionName = versionName,
        )
    }

    single<EqualizerController> {
        EqualizerController(
            context = androidContext()
        )
    }

    single<LyricsReader> {
        LyricsReaderImpl(
            context = androidContext()
        )
    }

    viewModel<PlayerViewModel> {
        PlayerViewModel(
            savedPlayerState = get(),
            trackRepository = get(),
            metadataProvider = get(),
            lyricsProvider = get(),
            lyricsRepository = get(),
            lyricsReader = get(),
            playlistRepository = get(),
            lovedTracksRepository = get(),
            trackStatsRepository = get(),
            backupManager = get(),
            unsupportedWriteFormats = get<MetadataWriter>().unsupportedWriteFormats,
            unsupportedCoverArtFormats = (get<MetadataWriter>() as MetadataWriterImpl).unsupportedCoverArtFormats,

            settings = get(),
            musicScanner = get(),
            equalizerController = get()
        )
    }
}
