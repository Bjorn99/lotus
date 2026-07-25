package com.dn0ne.player.app.data

import android.content.Context
import com.dn0ne.player.app.domain.playback.PlaybackMode
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SavedPlayerState(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("saved-player-state", Context.MODE_PRIVATE)

    private val playlistKey = "playlist"
    private val playbackModeKey = "playback-mode"
    private val trackKey = "track"

    // Getters are defensive: a corrupt or version-incompatible saved value
    // (e.g. a Track wire-format change failing TrackSerializer's require(), or
    // a playback-mode ordinal out of range) would otherwise throw out of the
    // restore coroutine and crash on entry to the player, with no recovery. On
    // a decode failure we clear the bad key and fall back to the "nothing
    // saved" default so playback just starts fresh.
    var playlist: Playlist?
        get() = runCatching {
            sharedPreferences.getString(playlistKey, null)?.let { Json.decodeFromString<Playlist>(it) }
        }.getOrElse {
            sharedPreferences.edit().remove(playlistKey).apply()
            null
        }
        set(value) {
            with(sharedPreferences.edit()) {
                putString(playlistKey, Json.encodeToString(value))
                apply()
            }
        }

    var playbackMode: PlaybackMode
        get() = runCatching {
            PlaybackMode.entries[sharedPreferences.getInt(playbackModeKey, 0)]
        }.getOrElse {
            sharedPreferences.edit().remove(playbackModeKey).apply()
            PlaybackMode.entries[0]
        }
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(playbackModeKey, value.ordinal)
                apply()
            }
        }

    var track: Track?
        get() = runCatching {
            sharedPreferences.getString(trackKey, null)?.let { Json.decodeFromString<Track>(it) }
        }.getOrElse {
            sharedPreferences.edit().remove(trackKey).apply()
            null
        }
        set(value) {
            with(sharedPreferences.edit()) {
                putString(trackKey, Json.encodeToString(value))
                apply()
            }
        }
}