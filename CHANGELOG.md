# Changelog

All notable changes to Lotus (community fork) are recorded in this file,
newest first. For the full picture of how this fork diverges from upstream
`dn0ne/lotus`, see the **"What this fork adds"** section in
[README.md](README.md).

Each release page on GitHub is built from the matching section below, so
the wording is deliberately aimed at the end user.

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
