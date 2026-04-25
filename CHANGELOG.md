# Changelog

All notable changes to Lotus (community fork) are recorded in this file,
newest first. For the full picture of how this fork diverges from upstream
`dn0ne/lotus`, see the **"What this fork adds"** section in
[README.md](README.md).

Each release page on GitHub is built from the matching section below, so
the wording is deliberately aimed at the end user.

## 1.3.6 — Fix "Play next" + queue handling in shuffle mode

**Layman:** Two related queue bugs. Tapping "Play next" on a track
already in the queue moved it to the wrong position (right *before*
the currently-playing track instead of after). And in shuffle mode,
"Play next" didn't actually play the track next — it inserted the
track at a random position in the shuffle order, so it would play at
some unpredictable later time. Fixed: the move math is correct now,
and tapping "Play next" while shuffle is on switches to the
non-shuffle Repeat mode (with a snackbar) so the track reliably plays
next. Re-enable shuffle from the playback-mode toggle when you want
it back.

**Technical:**
- `OnPlayNextClick` reorder branch (track already in queue): the
  destination index passed to `OnReorderingQueue` was always
  `currentTrackIndex`. Media3's `moveMediaItem(from, newIndex)` places
  the item at `newIndex` post-move; for a track at index 5 moved to
  index 3, the previously-current item shifts to index 4, so the
  moved track ends up *before* the current one. Fixed by computing
  destination conditionally:
  - `trackIndex > currentTrackIndex` → `currentTrackIndex + 1`
  - `trackIndex < currentTrackIndex` → `currentTrackIndex` (current
    shifts down by 1 when target is removed; landing target at this
    index puts it right after the new current position)
- `OnPlayNextClick` add-new branch (track not yet in queue): in
  shuffle mode `Player.addMediaItem(currentMediaItemIndex + 1, item)`
  inserts at the timeline position, but Media3's
  `DefaultShuffleOrder.cloneAndInsert` places the new item at a
  random position in the shuffle order, defeating "play next" intent.
  Workaround: detect shuffle, switch playback mode to Repeat, persist
  via `savedPlayerState.playbackMode`, then add at `currentIndex + 1`
  as before. Snackbar `R.string.shuffle_disabled_for_play_next`
  (localised EN/RU/UK) explains the change.
- `OnAddToQueueClick` left alone — appending to the timeline + random
  shuffle position is consistent with what users mean by "add to
  queue" (the order is already random by design).

## 1.3.5 — Stop crashes during MusicBrainz / LRCLIB requests

**Layman:** Searching online for track metadata (the "Track info →
Search" flow) and fetching lyrics from LRCLIB used to crash the app
on flaky networks — DNS hiccups, TLS / certificate weirdness, slow
connections. Now those failures pop a snackbar with a helpful message
instead of taking the app down. Also: the request timeout dropped
from **3 minutes** to **20 seconds**, so a slow lookup gives you an
error long before you'd reach for the back button.

**Technical:**
- Phase 4, item 2. The metadata search feature was kept (it's
  legitimately useful for fixing badly-tagged music) and hardened
  rather than removed.
- Three crash-paths fixed across `MusicBrainzMetadataProvider` (2
  network calls) and `LrclibLyricsProvider` (3 network calls). Each
  call previously caught only a narrow set: `UnresolvedAddressException`,
  `HttpRequestTimeoutException`, sometimes `SocketException`. Anything
  else escaped uncaught:
  - `ConnectTimeoutException` / `SocketTimeoutException` — connection
    + read timeouts (distinct from `HttpRequestTimeoutException`)
  - `UnknownHostException` — DNS failures on some Android stacks
  - `SSLException` — TLS handshake / cert pinning errors
  - any other `IOException` — generic socket / EOF problems
- Both providers now use a single `Throwable.toNetworkError()`
  classifier and a `Throwable` fallback at the end of each `catch`
  block. `CancellationException` is explicitly re-thrown so coroutine
  cancellation still unwinds correctly (catching it would break
  structured concurrency).
- `body()` parse paths gain the same `Throwable` fallback, mapping
  to `DataError.Network.ParseError`.
- HTTP timeouts in `PlayerModule.kt` retuned:
  - `requestTimeoutMillis 180_000 → 20_000` (3 min → 20 s)
  - new `connectTimeoutMillis = 10_000`
  - new `socketTimeoutMillis = 15_000`
- Dead code removed in `getCoverArtBytes`: a `Log.d(...)` after a
  `return` was unreachable. Moved before the return so 307 redirect
  responses (which Ktor follows automatically — should never reach
  this branch in practice) actually log when they do.
- `println("RESPONSE BODY: ...")` left over from earlier debugging in
  `LrclibLyricsProvider.postLyrics` replaced with a proper
  `Log.d(logTag, ...)` call.

## 1.3.4 — Fix media notification: show song title, artist, and artwork

**Layman:** When you play a song, the system notification (and the
lock screen, and Android Auto, and your watch's media tile) used to
say just "Lotus is playing" instead of the actual song title.
Embarrassing. Fixed: every track now carries its title, artist, album,
album art, year, and track number into the notification, so you
finally see what's playing.

**Technical:**
- Phase 4, item 1. Stability + modernization fix.
- Root cause: both Track → MediaItem conversion sites
  (`TrackRepositoryImpl` at scan time and `TrackSerializer` at saved-
  state restore time) called `MediaItem.fromUri(uri)`, which builds a
  `MediaItem` with **no** `MediaMetadata`. Media3's automatic
  notification reads `mediaMetadata.title` / `displayTitle`, so when
  both are null the system fell back to the app label.
- New helper: `com.dn0ne.player.app.domain.track.buildMediaItem(uri,
  title, artist, album, albumArtist, genre, year, trackNumber,
  coverArtUri)`. Single source of truth for Track → MediaItem with
  full `MediaMetadata` populated:
  - `title`, `artist`, `albumTitle`, `albumArtist`, `genre`
  - `releaseYear`, `trackNumber` (parsed via `?.toIntOrNull()` so
    weirdly-formatted tags don't crash)
  - `artworkUri` from the MediaStore album-art URI (system-loaded —
    no manual bitmap decode)
  - `displayTitle` + `subtitle` set explicitly because different
    OEMs prefer different fields for the notification's primary /
    secondary lines
  - stable `mediaId` = uri string, plus `RequestMetadata.mediaUri`
    for MediaSession correlation across queue reorders + process
    restarts
- Two call sites migrated to `buildMediaItem(...)`. Unused
  `androidx.media3.common.MediaItem` import removed from
  `TrackSerializer`.
- Modernisation: `POST_NOTIFICATIONS` permission added to the
  manifest. Required on Android 13+ for foreground-service media
  notifications to be reliably visible. The system handles the runtime
  prompt automatically when the foreground service starts.

## 1.3.3 — Extract PlaylistEditor out of PlayerViewModel

**Layman:** Second step in the PlayerViewModel cleanup begun in 1.3.2.
All the "create / rename / delete / add / remove / reorder" playlist
operations move into their own class. No visible change on your phone
— same behaviour, same snackbars, same results.

**Technical:**
- Phase 3 cleanup, item 3 of 4 (second pass). Another pure extraction.
- New `com.dn0ne.player.app.domain.playlist.PlaylistEditor` class with
  six `suspend` methods — `create`, `rename`, `delete`, `addTracks`,
  `removeTracks`, `reorder`. Constructor takes `PlaylistRepository`.
- The name-collision check (previously `playlists.value.map { it.name
  }.contains(event.name)`) now takes an explicit `existingNames`
  list. Name checks, snackbars, and the "add tracks that are already
  present still adds them" quirk are all preserved exactly.
- Six `PlayerScreenEvent` branches in the VM — `OnCreatePlaylistClick`,
  `OnRenamePlaylistClick`, `OnDeletePlaylistClick`, `OnAddToPlaylist`,
  `OnRemoveFromPlaylist`, `OnPlaylistReorder` — now delegate to the
  editor. `_selectedPlaylist` mutations stay in the branches because
  they're VM-local state.
- `parseM3U` still calls `playlistRepository.insertPlaylist` directly
  — its behaviour (no name-collision check) differs from the editor
  and is kept identical to avoid a behavioural change in this
  refactor.
- Net: `PlayerViewModel.kt` **1439 → 1412 lines** (−27). Combined with
  v1.3.2 that's −129 lines total out of the 1541-line original.

## 1.3.2 — Extract LyricsFetcher out of PlayerViewModel

**Layman:** Code cleanup with no visible changes. `PlayerViewModel`
has grown into a 1541-line file handling playback, metadata,
playlists, lyrics, and more — all mixed together. This release
extracts the lyrics fetching logic into its own focused class so
future work on lyrics doesn't drag the whole file into every diff.
First step in a multi-release split; the behaviour on your phone is
unchanged.

**Technical:**
- Phase 3 cleanup, item 3 of 4 (first pass). Pure extraction — no
  state-coupling changes, no behaviour changes, no public API changes.
- New `com.dn0ne.player.app.domain.lyrics.LyricsFetcher` class owns
  the two formerly private methods `readFromTag` and `fetchFromRemote`
  (previously `readLyricsFromTag` / `fetchLyrics` inside the VM).
  Constructor takes `LyricsReader`, `LyricsProvider`, `LyricsRepository`.
- `PlayerViewModel` now holds a private `lyricsFetcher` field and
  delegates at the three call sites (`OnLyricsControlClick`,
  `OnFetchLyricsFromRemoteClick`, `loadLyrics`). `readFromTag` takes
  the coroutine scope explicitly so snackbar errors keep launching
  off `viewModelScope` exactly as before.
- Net: `-102 lines` in `PlayerViewModel.kt` (1541 → 1439). Remaining
  slices — metadata search, playlist CRUD, queue ops, lyrics control
  sheet state — stay in the VM for now; they'll be pulled out in
  follow-up PRs once this pattern is proven in production.

## 1.3.1 — Stability: remove force-unwraps from playback UI

**Layman:** Pure stability fix, no visible changes. The player sheet
and track-info screens used to assume there was always a current track
when they were drawn. Most of the time that's true, but during certain
state transitions (clearing the queue, stopping playback, app coming
back from background) the assumption could briefly fail and crash the
app. The screens now skip drawing for that one frame instead.

**Technical:**
- Phase 3 cleanup, item 2 of 4. Replaces every `!!` non-null assertion
  in `app/src/main/java/com/dn0ne/player/` (six sites total) with a
  null-safe pattern. Zero `!!` operators remain in main sources.
- `BottomPlayer`, `ExpandedPlayer` (two inner scopes), and
  `PlaybackControl` in `PlayerSheet.kt`: each `derivedStateOf {
  playbackState.currentTrack!! }` is split into a nullable
  `derivedStateOf { playbackState.currentTrack }` plus an explicit
  `?: return@<scope>` guard. The remaining body still uses a non-null
  `currentTrack` (via local shadow), so no downstream call sites
  changed.
- `TrackInfoSheet.kt` `Changes` and `ManualEditing` child routes: the
  `state.track!!` arg is replaced with `val track = state.track ?:
  return@composable` followed by `track = track`. If the navigation
  child renders before its parent populated the track (a state race),
  the route no-ops instead of NPE-ing.
- The parent `PlayerScreen` already gates the entire player sheet via
  `AnimatedVisibility(visible = currentTrack != null, …)`, but
  `AnimatedVisibility` keeps composing the child for the duration of
  the exit animation — that's the gap these guards close.

## 1.3.0 — Drop Realm, Room-only storage

**Layman:** A long-overdue cleanup with no visible behaviour change on
your phone. Older versions of this app used a database library called
Realm for your playlists and lyrics. v1.2.0 switched to a modern one
(Room) and ran a migration so your data carried over. That migration
has now had a release to complete on every device, so this version
rips Realm out entirely. The APK is meaningfully smaller and the app
starts up a little faster. If you're installing Lotus for the first
time on v1.3.0 it doesn't matter — nothing to migrate, nothing
missing.

**Technical:**
- Removed: `RealmPlaylistRepository`, `RealmLyricsRepository` (with
  their embedded `PlaylistJson` / `LyricsJson` `RealmObject` schemas),
  `RealmToRoomMigrator` + its instrumented test, and the
  `realmToRoomMigrationDone` flag in `Settings`.
- Build: dropped `io.realm.kotlin` plugin from root +
  `app/build.gradle.kts`, `libs.realm.library.base` from
  dependencies, and the `realm` entries (version, library, plugin)
  from `libs.versions.toml`.
- DI: removed the `single<RealmToRoomMigrator>` Koin binding and the
  associated import from `PlayerModule.kt`.
- Application class: removed the `realmToRoomMigrator` inject and the
  `applicationScope.launch { migrator.migrateIfNeeded() }` call from
  `PlayerApp.onCreate()`. The crash reporter install remains first.
- Room DAOs / entities / repositories are unchanged and remain the
  sole persistence layer for playlists + cached lyrics.
- APK size shrinks by ~10–15 MB per ABI (Realm's native libraries +
  its KMP runtime are gone).
- No schema change on the Room side; upgrading users keep their data.

## 1.2.3 — Smart playlists + release notes overhaul

**Layman:** The Playlists tab now has a "Smart" section at the top with
two auto-filled lists — **Recently added** (files added to your library
in the last 30 days) and **Random mix** (up to 100 tracks shuffled).
They behave like any other playlist: tap to play, export to M3U, etc.
Release pages on GitHub now include a human-readable description of
what's in the release plus a summary of how this fork differs from
upstream — no more "click through and guess what changed."

**Technical:**
- `SmartPlaylists` domain object — pure Kotlin builders for `recentlyAdded`
  (`dateModified > now - 30d`, capped at 100, sorted desc) and `randomMix`
  (seeded shuffle, capped at 100). Derived in `PlayerScreen` composable
  layer so the localised names come from `stringResource` without forcing
  `Context` into the `PlayerViewModel` constructor.
- `Tab.Playlists` branch now renders a two-section layout: Smart (header
  + cards/rows, hidden when empty or in selection mode) then user
  playlists. `GridItemSpan(maxLineSpan)` lets the section headers span
  the full grid width.
- Smart-list taps route through `onAlbumPlaylistSelection` → immutable
  `PlayerRoutes.Playlist` view (can't be renamed or reordered). Long-press
  is a no-op — smart lists aren't part of bulk selection.
- `.github/workflows/release.yml` now extracts the matching section from
  `CHANGELOG.md` and uses it as the GitHub Release body instead of
  `--generate-notes`. Each release page also gets a stable fork-summary
  preamble.
- Release workflow gains a preflight step that fails fast when the tag
  name (`v1.2.3`) does not match the committed `versionName`
  (`1.2.3-community`). Prevents the "tag pushed before version bump
  merged" mis-release mode we hit twice.

## 1.2.2 — Export to M3U + global library search + maintenance

**Layman:** Two new features. Every playlist view has a new Download
icon that saves the playlist as a standard `.m3u8` file to any folder
you pick — playable in VLC, Musicolet, PowerAmp, Foobar2000, and
almost every other music app. A new magnifying-globe icon in the top
bar opens one search box that looks through everything at once: tracks,
albums, artists, genres, and playlists. Also fixed: the About page's
Repository and Feedback buttons now open *this* fork's links (they
previously pointed at the upstream author's repo and personal email,
so tapping "Feedback" was emailing a stranger).

**Technical:**
- **M3U export** — `M3uExporter` domain object, pure Kotlin formatter for
  Extended M3U8 (UTF-8): `#EXTM3U` header + `#EXTINF:<seconds>,<artist>
  - <title>` + absolute path per track. Covered by 9 JVM unit tests.
  `rememberM3uExport()` composable wraps
  `ActivityResultContracts.CreateDocument("audio/x-mpegurl")`; writes via
  `ContentResolver.openOutputStream`; success/failure toast. Uses
  absolute paths (`Track.data`) because SAF hides the destination folder.
  Download icon added on top bar of both `MutablePlaylist` (user
  playlists) and `Playlist` (album/artist/genre) views; disabled when
  the playlist is empty.
- **Global library search** — `GlobalSearchSheet`: full-screen `Dialog`,
  single `SearchField`, five grouped result sections (tracks top-8,
  playlists / albums / artists / genres top-5 each). Empty sections
  collapse; blank/no-match states render distinct hints. Reuses existing
  `filterTracks` / `filterPlaylists` helpers — no ViewModel or
  event-channel changes. Navigation dispatches the existing
  `onPlaylistSelection` / `onAlbumPlaylistSelection` callbacks. Query
  state local to the sheet; dropped on dismiss.
- **Upstream references removed from the in-app UI:**
  - `repo_url` → `https://github.com/Bjorn99/lotus`
    (was `dn0ne/lotus`)
  - `feedback_url` → `https://github.com/Bjorn99/lotus/issues/new`
    (was `mailto:dev.dn0ne@gmail.com`)
  - MusicBrainz + LRCLIB `User-Agent` contact → fork repo URL
    (URL form is spec-compliant; rate-limit / abuse reports now route
    to this fork)
- README `## Support` section rewritten to credit the upstream author
  and link to their Liberapay; this fork does not solicit donations.

## 1.2.0 — First community release

**Layman:** The foundational release. Everything needed to keep the
app stable and maintainable going forward. The app no longer crashes
silently when lyrics reading hits a weird file, and if something does
go wrong, crashes are saved to a log file you can share. The
underlying database was modernised. You can share any track directly
to other apps. The release itself is signed and published through an
automated pipeline.

**Technical:**
- **Realm → Room migration** with one-shot `RealmToRoomMigrator`
  (legacy store read once on first launch; Realm + Room DAOs run
  side-by-side for one release so the legacy store can be removed in
  a later pass).
- **Crash reporter** (`CrashReporter`) writes uncaught-exception stack
  traces to private app files, exposed via `FileProvider` authority
  `${applicationId}.crashlogs` for share-sheet delivery. No network,
  no analytics, no telemetry.
- **`LyricsReaderImpl` hardened:** `jaudiotagger` exceptions are caught
  and logged (wide-net `Throwable` — jaudiotagger throws many checked
  types); the temp file is cleaned up in `finally`; no longer nukes
  the shared cache directory (Coil's image cache lives there).
- **CI pipeline** (`.github/workflows/ci.yml`): JDK 17, unit tests,
  `assembleDebug` / `assembleRelease` / `lintRelease`, detekt + ktlint;
  artifact uploads for APKs + reports; diagnostic dump on failure.
- Lint / detekt / ktlint all in "report-only" mode (`abortOnError =
  false` / `ignoreFailures = true`) so legacy findings don't block CI;
  findings surface via the uploaded HTML reports.
- **Release signing pipeline**
  (`.github/workflows/release.yml`): triggers on `v*` tag push, decodes
  keystore from `LOTUS_KEYSTORE_BASE64`, runs `assembleRelease`,
  verifies with `apksigner`, publishes per-ABI + universal APKs and
  `SHA256SUMS.txt` to a GitHub Release.
- **Share track** — `ACTION_SEND` intent with MediaStore content URI,
  MIME from `ContentResolver.getType()` (`audio/*` fallback), title as
  `EXTRA_SUBJECT`, `FLAG_GRANT_READ_URI_PERMISSION`; dropdown entry in
  `TrackMenu` wired from `TrackListItem` + both `PlayerSheet` call sites.
- README retargeted from upstream `dn0ne/lotus` to this fork's releases.
- **Fork rebrand**: `applicationId = com.dn0ne.lotus.community` so the
  community build coexists with the original on the same device.
