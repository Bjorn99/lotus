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
Latest stable version is available on [GitHub releases](https://github.com/dn0ne/lotus/releases/latest)

Stable releases are also available on [F-Droid](https://f-droid.org/packages/com.dn0ne.lotus)

## Support
If you enjoy using Lotus, consider [buying me a coffee](https://en.liberapay.com/dn0ne/donate)!

## Build
1. **Get the Source Code**  
   - Clone the repository or download the source code:
     ```bash
     git clone https://github.com/dn0ne/lotus.git
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

Release builds need a signing keystore. For local use, create `keystore.properties`
at the repo root (it is gitignored):

```properties
storeFile=/absolute/path/to/lotus-release.jks
storePassword=...
keyAlias=lotus
keyPassword=...
```

Or set the same values as environment variables for CI:
`LOTUS_KEYSTORE_FILE`, `LOTUS_KEYSTORE_PASSWORD`, `LOTUS_KEY_ALIAS`, `LOTUS_KEY_PASSWORD`.

If neither is configured, `assembleRelease` still works but falls back to the
debug keystore with a visible warning — **do not distribute those APKs**.

## Credits
Some UI elements are inspired by [Vanilla](https://github.com/vanilla-music/vanilla)

Lyrics UI is inspired by [Beautiful Lyrics](https://github.com/surfbryce/beautiful-lyrics)

[MaterialKolor](https://github.com/jordond/materialkolor)

[kmpalette](https://github.com/jordond/kmpalette)

[Reorderable](https://github.com/Calvin-LL/Reorderable)

[jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger/src/master/)

## License
Lotus is licensed under [GPLv3](LICENSE.md)
