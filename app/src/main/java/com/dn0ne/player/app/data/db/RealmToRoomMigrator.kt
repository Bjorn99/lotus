package com.dn0ne.player.app.data.db

import android.content.Context
import android.util.Log
import com.dn0ne.player.app.data.repository.LyricsJson
import com.dn0ne.player.app.data.repository.PlaylistJson
import com.dn0ne.player.core.data.Settings
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import java.io.File

/**
 * One-shot copy of the legacy Realm stores (PlaylistJson, LyricsJson) into
 * the Room equivalents. The JSON-blob shape is identical on both sides so
 * this is purely a row copy — no decoding / re-encoding of domain objects.
 *
 * Runs off the main thread from PlayerApp.onCreate. Failures are logged and
 * swallowed so a corrupt Realm file can't block app startup; worst case the
 * user loses their playlists and lyrics cache (both re-creatable) instead of
 * the app being bricked.
 */
class RealmToRoomMigrator(
    private val context: Context,
    private val database: LotusDatabase,
    private val settings: Settings,
) {

    suspend fun migrateIfNeeded() {
        if (settings.realmToRoomMigrationDone) return

        // Fresh installs have no Realm file; don't create one just to read
        // zero rows out of it. Mark done and move on.
        if (!realmFileExists()) {
            settings.realmToRoomMigrationDone = true
            return
        }

        var realm: Realm? = null
        try {
            val configuration = RealmConfiguration.create(
                schema = setOf(LyricsJson::class, PlaylistJson::class),
            )
            realm = Realm.open(configuration)

            val playlists = realm.query<PlaylistJson>().find()
                .map { PlaylistEntity(name = it.name, json = it.json) }
            if (playlists.isNotEmpty()) {
                database.playlistDao().upsertAll(playlists)
            }

            val lyrics = realm.query<LyricsJson>().find()
                .map { LyricsEntity(uri = it.uri, json = it.json) }
            if (lyrics.isNotEmpty()) {
                database.lyricsDao().upsertAll(lyrics)
            }

            settings.realmToRoomMigrationDone = true
            Log.i(TAG, "Migrated ${playlists.size} playlists and ${lyrics.size} lyrics entries.")
        } catch (t: Throwable) {
            Log.e(TAG, "Realm → Room migration failed; will retry on next launch.", t)
        } finally {
            realm?.close()
        }
    }

    private fun realmFileExists(): Boolean =
        File(context.filesDir, DEFAULT_REALM_NAME).exists()

    private companion object {
        const val TAG = "RealmToRoomMigrator"
        // Realm Kotlin SDK's default filename when no `name` is set on the
        // RealmConfiguration.
        const val DEFAULT_REALM_NAME = "default.realm"
    }
}
