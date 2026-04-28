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
import com.dn0ne.player.app.data.remote.lyrics.ChainLyricsProvider
import com.dn0ne.player.app.data.remote.lyrics.GatedLyricsProvider
import com.dn0ne.player.app.data.remote.lyrics.LrclibLyricsProvider
import com.dn0ne.player.app.data.remote.lyrics.LyricsProvider
import com.dn0ne.player.app.data.remote.lyrics.NetEaseLyricsProvider
import com.dn0ne.player.core.data.Settings
import com.dn0ne.player.app.data.remote.metadata.GatedMetadataProvider
import com.dn0ne.player.app.data.remote.metadata.MetadataProvider
import com.dn0ne.player.app.data.remote.metadata.MusicBrainzMetadataProvider
import com.dn0ne.player.app.data.repository.LovedTracksRepository
import com.dn0ne.player.app.data.repository.LyricsRepository
import com.dn0ne.player.app.data.repository.PlaylistRepository
import com.dn0ne.player.app.data.repository.RoomLovedTracksRepository
import com.dn0ne.player.app.data.repository.RoomLyricsRepository
import com.dn0ne.player.app.data.repository.RoomPlaylistRepository
import com.dn0ne.player.app.data.repository.TrackRepository
import com.dn0ne.player.app.data.repository.TrackRepositoryImpl
import com.dn0ne.player.app.presentation.PlayerViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.contentLength
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// 5 MB is well above the largest legitimate response we make (album cover
// art tops out around a couple hundred KB; lyrics and JSON metadata are
// tiny). Picked to leave generous headroom while still catching the
// "firehose" attack shape.
private const val MAX_RESPONSE_BYTES = 5L * 1024 * 1024

// v1 → v2: add the loved_tracks table for the Loved-tracks feature.
// Pure additive — no existing-data transformation, safe on every device.
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `loved_tracks` (" +
                "`uri` TEXT NOT NULL, " +
                "`added_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`uri`))"
        )
    }
}

val playerModule = module {

    single<TrackRepository> {
        TrackRepositoryImpl(
            context = androidContext(),
            settings = get()
        )
    }

    single<SavedPlayerState> {
        SavedPlayerState(
            context = androidContext()
        )
    }

    single<HttpClient> {
        HttpClient(CIO) {
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

            // Cap response size so a malicious or buggy upstream can't fill
            // memory with a multi-GB body. Headers-phase only — checks the
            // declared Content-Length before we consume the body. Responses
            // that omit Content-Length still get killed by socketTimeout
            // long before they fill anything significant.
            HttpResponseValidator {
                validateResponse { response ->
                    val length = response.contentLength()
                    if (length != null && length > MAX_RESPONSE_BYTES) {
                        throw IOException(
                            "Refusing response: declared Content-Length " +
                                "$length exceeds cap of $MAX_RESPONSE_BYTES",
                        )
                    }
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
        val netEase = GatedLyricsProvider(
            delegate = NetEaseLyricsProvider(client = get()),
            isEnabled = { settings.useNetEaseLyricsFallback },
        )
        // Outer gate: master switch for all lyrics network calls. When the
        // user has disabled network lookups in Settings → Privacy, the
        // chain short-circuits before consulting either provider.
        GatedLyricsProvider(
            delegate = ChainLyricsProvider(listOf(lrclib, netEase)),
            isEnabled = { settings.networkLookupsEnabled },
        )
    }

    single<LotusDatabase> {
        Room.databaseBuilder(
            androidContext(),
            LotusDatabase::class.java,
            LotusDatabase.NAME,
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    single<PlaylistDao> { get<LotusDatabase>().playlistDao() }
    single<LyricsDao> { get<LotusDatabase>().lyricsDao() }
    single<LovedTrackDao> { get<LotusDatabase>().lovedTrackDao() }

    single<LyricsRepository> {
        RoomLyricsRepository(dao = get())
    }

    single<PlaylistRepository> {
        RoomPlaylistRepository(dao = get())
    }

    single<LovedTracksRepository> {
        RoomLovedTracksRepository(dao = get())
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
            trackRepository = get(),
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
            backupManager = get(),
            unsupportedArtworkEditFormats = get<MetadataWriter>().unsupportedArtworkEditFormats,
            settings = get(),
            musicScanner = get(),
            equalizerController = get()
        )
    }
}
