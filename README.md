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

## Download

Community builds of this fork are published as signed APKs on the
[Bjorn99/lotus releases page](https://github.com/Bjorn99/lotus/releases/latest).
Each release includes per-ABI APKs plus a universal APK, and a
`SHA256SUMS.txt` you can verify against.

The original upstream build (different application ID) is still on
[F-Droid](https://f-droid.org/packages/com.dn0ne.lotus) and the
[upstream releases page](https://github.com/dn0ne/lotus/releases/latest) —
those are *not* produced by this fork.

## Support
If you enjoy using Lotus, consider [buying me a coffee](https://en.liberapay.com/dn0ne/donate)!

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
