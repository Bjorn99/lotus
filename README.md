<div align="center">
  <img src="fastlane/metadata/android/en-US/images/icon-fit.png" width="200px" />

  # Lotus

  ### Music player for Android
  
</div>

## About this fork
This is a community continuation of [Lotus](https://github.com/dn0ne/lotus) by
**[dn0ne](https://github.com/dn0ne)**, who built the original app. All design,
branding, and prior work are theirs — huge thanks. Upstream is no longer
actively maintained, so this fork picks up bug fixes, stability work, and
small features while keeping the app true to its original spirit.

- Upstream repository: https://github.com/dn0ne/lotus
- Upstream license: GPLv3 (preserved)
- Application ID: `com.dn0ne.lotus.community` (so it can coexist with the
  upstream build if you already have it installed)

If you are the original author and would prefer any change here, please open
an issue — we'll respect your wishes.

## Screenshots

<div align="center">
  <div>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8.png" width="24%" />
  </div>
</div>

## Features
- Enjoy your favorite music in a variety of formats, including MP3, FLAC, OGG, WAV, and more
- Easily browse tracks, albums, artists, and genres, and create custom playlists
- Enhance your listening experience with synchronized lyrics from [LRCLIB](https://lrclib.net/)
- Manually update track details or fetch accurate info from [MusicBrainz](https://musicbrainz.org/)
- Designed with [Material You](https://m3.material.io/) and supports dynamic color palettes

## What this fork adds (vs upstream dn0ne/lotus)

Each item appears in the order it was introduced. Per-version notes live
in [CHANGELOG.md](CHANGELOG.md); summaries below are cumulative.

### Stability and infrastructure

- **Continuous Integration pipeline** — every pull request runs unit tests,
  builds debug + release APKs, runs Android lint + detekt + ktlint, and
  uploads reports as artifacts. Broken code never reaches master.
- **Signed release pipeline** — pushing a `v*` tag triggers a GitHub
  Actions workflow that builds per-ABI + universal APKs, signs them with
  the release keystore, verifies with `apksigner`, generates
  `SHA256SUMS.txt`, and publishes everything to a GitHub Release.
- **Room-only storage (was Realm)** — the upstream app used Realm (an
  embedded mobile database that had been deprecated). v1.2.0 migrated
  to Android's official [Room](https://developer.android.com/training/data-storage/room)
  via a one-shot in-place migrator; v1.3.0 dropped the Realm dependency
  and migrator entirely. Result: ~10–15 MB smaller APK per ABI, faster
  cold start, and no deprecated SDKs in the build graph.
- **Crash reporter** — uncaught exceptions are written to a private log
  file instead of just killing the process. From the About page you can
  share the most recent crash log via the Android share sheet, which
  makes "it crashed for me" bug reports actually actionable. No network,
  no analytics, no telemetry.
- **Lyrics reader hardening** — the upstream reader could crash on
  malformed files and also nuked the shared cache directory (which Coil
  uses for album-art thumbnails) on every read. Lotus catches the
  jaudiotagger exceptions, deletes only its own temp file, and logs
  rather than crashing. Fewer crashes, no more surprise re-downloaded
  artwork.
- **Release-build lint posture flipped** — Android lint and the two
  Kotlin analysers (detekt + ktlint) run on every PR in "report-only"
  mode. Findings surface as downloadable HTML reports without gating the
  build, so the backlog can be burned down deliberately.

### Features

- **Share track** — the track-dropdown menu has a "Share" entry that
  sends the current track to any app via Android's share sheet (audio
  file + title subject line). Works from the track list and from the
  now-playing sheet.
- **Global library search** — a magnifying-globe icon in the top bar
  opens a single search field that queries across tracks, albums,
  artists, genres, and playlists at once. Results are grouped by
  category; tap anything to jump to it. The existing per-tab search
  still works unchanged.
- **Export playlist to M3U** — a Download icon on every playlist view
  (user playlists plus album/artist/genre views) writes the playlist as
  a standard `.m3u8` file to a folder you pick. Open the file in VLC,
  PowerAmp, Musicolet, Foobar2000, or anywhere else M3U is supported.
- **Smart playlists** — the Playlists tab now shows auto-generated
  lists at the top: **Recently added** (files modified within the last
  30 days) and **Random mix** (up to 100 tracks shuffled). They feel
  like any other playlist — tap to open, play from, or export to M3U.

### Housekeeping

- **Fork rebrand** — `applicationId` is `com.dn0ne.lotus.community`, so
  the community build coexists with the original upstream build side-by-side
  on the same device.
- **In-app links point at the right place** — Repository / Feedback
  buttons open this fork's GitHub repo and issue tracker, not the
  upstream author's personal email.
- **MusicBrainz / LRCLIB contact** — the User-Agent sent with those
  API calls identifies this fork, so rate-limit or abuse reports reach
  us rather than the upstream author.
- **Zero telemetry, zero analytics, phone-only** — no network calls
  except LRCLIB (lyrics on demand) and MusicBrainz (optional metadata
  search). No crash reporting SDKs, no analytics, no server.

## Download

Community builds of this fork are published as signed APKs on the
[Bjorn99/lotus releases page](https://github.com/Bjorn99/lotus/releases/latest).
Each release includes per-ABI APKs plus a universal APK, and a
`SHA256SUMS.txt` you can verify against.

F-Droid and itch.io distribution channels are documented in
[`docs/DISTRIBUTION.md`](docs/DISTRIBUTION.md). For users who want
auto-updates, the F-Droid client is the recommended channel once we
land on f-droid.org.

The original upstream build (different application ID) is still on
[F-Droid](https://f-droid.org/packages/com.dn0ne.lotus) and the
[upstream releases page](https://github.com/dn0ne/lotus/releases/latest) —
those are *not* produced by this fork.

## Support the original author

This community fork exists because of the original Lotus by **dn0ne**. If
you'd like to thank them for the underlying app, you can do so on
[Liberapay](https://en.liberapay.com/dn0ne/donate). This fork does not
solicit donations of its own.

## Build
1. **Get the Source Code**  
   - Clone the repository or download the source code:
     ```bash
     git clone https://github.com/Bjorn99/lotus.git
     ```

2. **Open project in Android Studio**  
   - Launch Android Studio.  
   - Select **File > Open** and navigate to the project's folder.  
   - Click **OK** to open the project.

3. **Run the project**  
   - Wait for the project to sync and build (Gradle sync may take some time).  
   - Ensure a device or emulator is connected.  
   - Click the **Run** button or press `Shift + F10` to build and launch the app.  

That's it! The app should now be running.

## Release builds

### Local signed builds

For local use, create `keystore.properties` at the repo root (gitignored):

```properties
storeFile=/absolute/path/to/lotus-release.jks
storePassword=...
keyAlias=lotus
keyPassword=...
```

Or set the same values as environment variables:
`LOTUS_KEYSTORE_FILE`, `LOTUS_KEYSTORE_PASSWORD`, `LOTUS_KEY_ALIAS`, `LOTUS_KEY_PASSWORD`.

If neither is configured, `assembleRelease` still works but falls back to the
debug keystore with a visible warning — **do not distribute those APKs**.

### Cutting a public release

Public releases are built and published by
[`.github/workflows/release.yml`](.github/workflows/release.yml) whenever a
`v*` tag is pushed. One-time setup:

1. Generate a release keystore (keep the file off GitHub):

   ```bash
   keytool -genkeypair -v \
     -keystore lotus-release.jks \
     -alias lotus \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Add four repo secrets under **Settings → Secrets and variables → Actions**:

   | Secret | Value |
   | --- | --- |
   | `LOTUS_KEYSTORE_BASE64` | `base64 -w0 lotus-release.jks` (the keystore file, base64-encoded) |
   | `LOTUS_KEYSTORE_PASSWORD` | keystore password |
   | `LOTUS_KEY_ALIAS` | key alias (`lotus` above) |
   | `LOTUS_KEY_PASSWORD` | key password |

3. Bump `versionCode` / `versionName` in `app/build.gradle.kts`, commit, and
   tag:

   ```bash
   git tag -a v1.2.0 -m "Lotus 1.2.0"
   git push origin v1.2.0
   ```

The workflow runs unit tests, assembles per-ABI + universal APKs, verifies
they are signed with the release key, publishes them to a new GitHub Release
with autogenerated notes, and attaches a `SHA256SUMS.txt` for verification.
The decoded keystore is scrubbed from the runner after the job.

## Credits
Some UI elements are inspired by [Vanilla](https://github.com/vanilla-music/vanilla)

Lyrics UI is inspired by [Beautiful Lyrics](https://github.com/surfbryce/beautiful-lyrics)

[MaterialKolor](https://github.com/jordond/materialkolor)

[kmpalette](https://github.com/jordond/kmpalette)

[Reorderable](https://github.com/Calvin-LL/Reorderable)

[jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger/src/master/)

## License
Lotus is licensed under [GPLv3](LICENSE.md)
