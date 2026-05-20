# Play-count infrastructure

Status: design approved 2026-05-06
Scope: storage, recording, privacy toggle, backup integration. No UI surfaces — the stats page lands in a separate PR that reads from this layer.

## Goal

Record per-track listening signals locally so a future stats page can answer "what do I listen to most", "what do I skip", and "how long have I had this track in rotation". Trunk-level work: touches the Room schema, the playback service, the backup format. Build it once, build it boring, leave a clean surface for the leaf-level UI to read from.

## Definition of a play

A track counts as +1 play when the playback head reaches 50% of the track's duration. Position-based, not time-based: if the user seeks past the halfway mark, that's a play. Time-based accounting (sum actually-listened ms, compare to duration / 2) was rejected — it requires tracking seek events and pause windows, which is state we don't otherwise need and a category of bug we don't want.

A track counts as +1 skip when it transitions away (next, previous, or stop) without crossing 50%.

Track ends naturally past 50% → +1 play, no skip.

## Schema

DB version 2 → 3. New entity, new DAO. No data backfill.

```kotlin
@Entity(tableName = "track_stats")
data class TrackStatsEntity(
    @PrimaryKey @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "play_count") val playCount: Int,
    @ColumnInfo(name = "skip_count") val skipCount: Int,
    @ColumnInfo(name = "first_played_at") val firstPlayedAt: Long,
    @ColumnInfo(name = "last_played_at") val lastPlayedAt: Long,
    @ColumnInfo(name = "total_listening_ms") val totalListeningMs: Long,
)
```

URI as primary key matches `loved_tracks`. Tracks that have never registered a play or skip have no row — the "0" case is a missing row, which keeps the table small and avoids inserting on every transient transition.

Migration: `CREATE TABLE track_stats(...)` only. Wrapped in a `Migration(2, 3)` and added to the `LotusDatabase` builder. No `fallbackToDestructiveMigration` — losing playlists or loved tracks during a play-count migration would be unacceptable.

## Recording

Lives in `PlaybackService`, not `PlayerViewModel`. The service owns the player's lifecycle and survives the UI being killed; the ViewModel can be GC'd while playback continues in the background.

A new `Player.Listener` (separate from the equalizer one — single-responsibility) tracks the active media item. Internal state held by the listener:

- `activeUri: String?` — the URI of the currently playing item.
- `lastFlushedPositionMs: Long` — the position we've already written to `totalListeningMs` for the active item.
- `maxReachedPositionMs: Long` — high-water mark of `currentPosition` seen for the active item, used for the 50 % decision (so seeking *backward* after crossing 50 % doesn't undo the play).

Hooks:

- `onPositionDiscontinuity(oldPosition, newPosition, reason)` fires *before* the player commits to the next item and gives us the old item's final position. This is the correct hook for transitions — `onMediaItemTransition` fires after the move, when `currentPosition` reflects the new item. When the discontinuity reason is `AUTO_TRANSITION`, `SEEK`, `SEEK_ADJUSTMENT`, `INTERNAL`, or `REMOVE`, we close out the previous item: flush the listening-ms delta (`oldPosition.positionMs - lastFlushedPositionMs`) and call `recordPlay(uri)` if `maxReachedPositionMs >= duration / 2`, else `recordSkip(uri)`.
- `onPlaybackStateChanged(STATE_ENDED)` covers natural end-of-queue, where there's no discontinuity. Same close-out logic against the active item.
- A 10 s periodic checkpoint while `isPlaying` is true: compute `currentPosition - lastFlushedPositionMs`, call `addListenedMs(activeUri, delta)`, advance `lastFlushedPositionMs`. Updates `maxReachedPositionMs` along the way. This is the only path that survives a service kill mid-track.
- Setting `trackPlayStats = false` short-circuits every DAO call. The listener still updates its in-memory state so that flipping the toggle back on starts fresh from the current position rather than re-flushing old deltas.

Repository methods are split so each does one thing:

- `recordPlay(uri)` — increments `playCount`, sets `lastPlayedAt = now`, sets `firstPlayedAt = now` if absent. Does *not* touch `totalListeningMs`.
- `recordSkip(uri)` — increments `skipCount`. Does not touch `lastPlayedAt`.
- `addListenedMs(uri, deltaMs)` — adds to `totalListeningMs`. Used by both checkpoints and the close-out.

This separation removes the double-count risk: listening time only moves through `addListenedMs`, counts only move through `recordPlay`/`recordSkip`.

Edge cases:

- `duration <= 0` (live stream, broken file): no play/skip event recorded; listening-ms still accumulates if any.
- Same track plays twice in a row (loop one): each transition closes out and reopens; counts independently.
- User seeks past 50 % then back: still counts as a play (high-water mark).
- App killed mid-track before 50 %: no play/skip event. Listening-ms up to the last 10 s checkpoint is kept.

## Repository layer

Mirrors loved-tracks exactly:

```kotlin
interface TrackStatsRepository {
    fun observeTopByPlayCount(limit: Int): Flow<List<TrackStats>>
    fun observeRecentlyPlayed(limit: Int): Flow<List<TrackStats>>
    suspend fun statsFor(uri: String): TrackStats?
    suspend fun recordPlay(uri: String, listenedMs: Long)
    suspend fun recordSkip(uri: String)
    suspend fun mergeFromBackup(rows: List<TrackStats>)
    suspend fun clearAll()
}
```

`recordPlay` upserts: insert with `playCount = 1`, `firstPlayedAt = now`, `lastPlayedAt = now`, `totalListeningMs = listenedMs`, or update with `playCount + 1`, `lastPlayedAt = now`, `totalListeningMs + listenedMs` (firstPlayedAt unchanged). `recordSkip` upserts similarly: `skipCount + 1`, no `lastPlayedAt` update (a skipped track wasn't really "listened to").

`RoomTrackStatsRepository` wraps the DAO and is bound in `AppModule` alongside the existing repositories.

## Privacy toggle

- New `Settings` boolean: `trackPlayStats`, default `true`.
- New row in `PrivacySettings` (the existing settings sheet category): toggle labeled "Record listening stats" with subtext "Counts how often you play and skip tracks. Stored only on this device."
- Flipping off:
  1. Writes `false` to settings.
  2. Calls `repository.clearAll()` to drop the table contents (not the schema).
  3. The listener stops writing on the next event.
- Flipping on: starts recording from that point. No retroactive data.

## Backup integration

Bump `BackupData.SCHEMA_VERSION` from `1` to `2`. Add a defaulted field so v1 backups still parse:

```kotlin
@Serializable
data class BackupData(
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: Long,
    val appVersionName: String,
    val playlists: List<BackupPlaylist>,
    val lovedTracks: List<BackupTrackRef>,
    val trackStats: List<BackupTrackStats> = emptyList(),
)

@Serializable
data class BackupTrackStats(
    val uri: String,
    val data: String,
    val playCount: Int,
    val skipCount: Int,
    val firstPlayedAt: Long,
    val lastPlayedAt: Long,
    val totalListeningMs: Long,
)
```

Export: read all rows from `track_stats`, resolve each `uri` to its `data` path via `TrackRepository`. If the track isn't in the library at export time (e.g. SD card unmounted), drop that row from the export — we can't write a usable `data` field for it.

Import (`mergeFromBackup`): for each incoming row, resolve URI first, then fall back to data-path lookup (same pattern as playlists / loved tracks). On conflict with an existing row:

| field              | merge rule |
|--------------------|------------|
| `playCount`        | max(local, backup) |
| `skipCount`        | max(local, backup) |
| `lastPlayedAt`     | max(local, backup) |
| `totalListeningMs` | max(local, backup) |
| `firstPlayedAt`    | min(local, backup) — earliest known first-play wins |

Why max/min instead of sum: re-importing the same backup is a no-op (idempotent), and importing an older backup never erases newer activity. Sum would double-count on re-import; "backup wins" would clobber legitimate local progress.

If `trackPlayStats` is currently off, import is skipped silently with a one-line note in the result message.

## Files to touch

New:

- `app/src/main/java/com/dn0ne/player/app/data/db/TrackStatsEntity.kt`
- `app/src/main/java/com/dn0ne/player/app/data/db/TrackStatsDao.kt`
- `app/src/main/java/com/dn0ne/player/app/data/repository/TrackStatsRepository.kt`
- `app/src/main/java/com/dn0ne/player/app/data/repository/RoomTrackStatsRepository.kt`
- `app/src/main/java/com/dn0ne/player/app/domain/track/TrackStats.kt` (domain model returned by the repository)
- `app/src/test/java/com/dn0ne/player/app/data/repository/RoomTrackStatsRepositoryTest.kt`
- `app/src/test/java/com/dn0ne/player/PlayThresholdTest.kt` (the position-vs-duration helper)

Modified:

- `LotusDatabase.kt` — add entity, DAO accessor, version 3, Migration(2,3).
- `PlaybackService.kt` — add the stats listener.
- `Settings.kt` — add `trackPlayStats` flag.
- `PrivacySettings.kt` — add the toggle UI.
- `AppModule.kt` — bind the repository.
- `BackupData.kt` — add `trackStats` field, bump SCHEMA_VERSION.
- `BackupManager.kt` — wire export/import paths.
- `CHANGELOG.md` — add v1.5.6 entry.

## Testing

- DAO test: insert, increment, observe top, observe recent, clear.
- Repository test: `recordPlay` and `recordSkip` upsert correctness; first/last played semantics; max/min merge under all conflict combinations.
- Pure-Kotlin test on the threshold helper: position 0/1/duration with various durations including `duration = 0` (no-op).
- Backup round-trip test: export → import → repository state matches; export → import → import (same backup) is idempotent.
- The `PlaybackService` listener wiring is exercised manually post-implementation. Hooking ExoPlayer into a unit test isn't worth the cost; the logic that *can* be unit-tested (the threshold check, the upsert math) is extracted so the listener becomes a thin glue layer.

## Out of scope

- Stats page UI. Lands in a follow-up PR that consumes `observeTopByPlayCount` and `observeRecentlyPlayed`.
- Per-album / per-artist aggregations. Derivable in SQL or in the stats ViewModel later.
- Event log / time-series stats ("plays this week"). The aggregate schema can be extended into an event log later without breaking this one.
- Crossfade / gapless effects on the threshold semantics — current player has no crossfade, so no special-casing.

## Risk and rollback

- Trunk impact: schema migration, playback service listener, backup format. Migration is additive (new table only); rolling back the app version after a successful migration to v3 means the v2 app would refuse to open the DB. We accept that — same constraint as every prior schema bump.
- Privacy posture: data never leaves the device unless the user exports a backup. Toggle off clears the table.
- Performance: one DAO write per track transition, plus one every 10 s while playing. Both are negligible against ExoPlayer's actual workload.
