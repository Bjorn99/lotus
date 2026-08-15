<div align="center">
  <img src="fastlane/metadata/android/en-US/images/icon-fit.png" width="200px" />

  # Lotus

  ### Music player for Android
  
</div>

Lotus is a clean, offline-first music player with Material You design. This is a community continuation of [dn0ne's original app](https://github.com/dn0ne/lotus).

I fell in love with Lotus because of what dn0ne built — the design, the feel, the attention to detail. When upstream development paused, I chose to maintain it so the app could keep going. All design, branding, and prior work are theirs. Application ID: `com.dn0ne.lotus.community`.

## Screenshots

<div align="center">
  <div>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/0.1.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="24%" />
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" width="24%" />
  </div>
</div>

## Features

- Play MP3, FLAC, OGG, OPUS, WAV, and more
- Browse tracks, albums, artists, genres, and custom playlists
- **Dual shuffle mode** — Pure (unbiased Fisher-Yates) and Smart (penalty-scored artist/album separation)
- **Per-track artwork** — display embedded cover art from individual audio files (opt-in)
- Synchronized lyrics from [LRCLIB](https://lrclib.net/), plus publish your own
- Edit track metadata or fetch it from [MusicBrainz](https://musicbrainz.org/)
- **Global library search** across tracks, albums, artists, genres, and playlists
- **Smart playlists** — Recently Added and Random Mix, auto-generated
- **Import and export M3U playlists** — compatible with VLC, foobar2000, and other desktop players
- **Backup and restore** — save your playlists and loved tracks to a JSON file
- **Loved tracks** — mark tracks as loved, browse them as a playlist
- **Sleep timer** — presets (15/30/45/60/90 min) with optional finish-current-track
- **Share track** — send any audio file via the Android share sheet
- **Listening stats** — top played, top listened, recently played, per-artist breakdowns
- Material You dynamic color palettes
- **Available in English, Spanish, Simplified Chinese, Russian, and Ukrainian**
- **Privacy-first** — network off by default, no telemetry, no analytics, no tracking

## What sets this fork apart

- **Room storage** — migrated from Realm to Android's official Room library, dropping ~10 MB from the APK
- **Dual shuffle mode** — Pure (unbiased Fisher-Yates) and Smart (penalty-scored artist/album separation), all on-device
- **Per-track artwork** — display cover art embedded in individual audio files, with album-art fallback
- **Improved lyrics and metadata** — multi-source fetching from LRCLIB, MusicBrainz, and sidecar `.lrc` files next to your music; hardened network layer; embedded LRC parsing; plus publish your own lyrics
- **Global library search** — single search field across tracks, albums, artists, genres, and playlists
- **Backup and restore** — export playlists and loved tracks to JSON, restore on any device
- **CI pipeline** — unit tests, linting (detekt + ktlint + Android lint), and signed release builds on every tag
- **Crash logging** — uncaught exceptions written to a private log, shareable from the About page
- **Network hardening** — HTTPS-only, zero redirects without a host allow-list, response size caps
- **Listening stats** — play/skip counts and top charts, with a privacy toggle that stops counting and clears data
- **Player performance** — scroll jank eliminated, Inter font subset to Latin-1 for smaller APK, Compose strong skipping via @Stable annotations

## Smart Shuffle

True randomness clusters. Flip a coin enough times and you get runs of heads; a pure shuffle does the same with artists, and three songs by one artist in a row feels broken even though the maths is fine.

So Smart Shuffle builds an order rather than drawing one. It deals tracks out like cards — the artist with the most tracks first, one into every other slot — which forces the most crowded artist as far apart as it can go. Whenever a repeat-free order exists, the deal finds one. A short second pass then makes random swaps and keeps only those that don't make things worse, which breaks up the regularity and also separates albums and anything you just heard. It all runs on-device, with no listening history and no network.

Same-artist back-to-backs against a pure shuffle, 500 queues per row:

| Queue | Pure shuffle | Smart Shuffle |
|---|---|---|
| 60 tracks, 6 artists | 8.9 | 0.0 |
| 60 tracks, 3 artists | 19.0 | 0.0 |
| 60 tracks, 2 artists | 28.9 | 0.0 |
| 200 tracks, 12 artists | 15.7 | 0.0 |

The second pass scores the queue as a whole, so on compilation-shaped libraries it can occasionally trade one artist repeat for better album spacing — under 1% of queues when measured, and never more than one repeat.

## Download

[<img src="https://f-droid.org/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/com.dn0ne.lotus.community/)

F-Droid is the recommended channel — auto-updates, signature verification, and no manual APK sideloading. Tagged releases are also on the [releases page](https://github.com/Bjorn99/lotus/releases); if you sideload, take `universal` unless you know your device's architecture, and check the download against `SHA256SUMS.txt`.

The original upstream build (different application ID) is also on [F-Droid](https://f-droid.org/packages/com.dn0ne.lotus) — that one is not produced by this fork.

Full version history is in [CHANGELOG.md](CHANGELOG.md).

## Support the original author

This fork exists because of [dn0ne](https://github.com/dn0ne)'s work. If Lotus has been useful to you, consider thanking them on [Liberapay](https://en.liberapay.com/dn0ne/donate).

## Build

1. Clone the repository:
   ```bash
   git clone https://github.com/Bjorn99/lotus.git
   ```
2. Open the project in Android Studio.
3. Wait for Gradle sync, then click **Run** or press `Shift + F10`.

Release builds are automated via CI — see [docs/RELEASING.md](docs/RELEASING.md) for the full process.

## Contributors

Lotus is kept going by people who file good bug reports, translate it, and send patches. Thank you.

- **[@uhrfra](https://github.com/uhrfra)** — relative-path support in M3U playlist import, based on their [#73](https://github.com/Bjorn99/lotus/pull/73) and shipped in v1.8.0; on-device testing of the v1.8.2 fixes
- **[@bxdxnn](https://github.com/bxdxnn)** — media notification icon ([#118](https://github.com/Bjorn99/lotus/pull/118)) and the track menu in global search ([#131](https://github.com/Bjorn99/lotus/pull/131))
- **[@LunaticWolfOk-py](https://github.com/LunaticWolfOk-py)** — Spanish translation ([#138](https://github.com/Bjorn99/lotus/issues/138))
- **[@MCfool](https://github.com/MCfool)** — Simplified Chinese translation ([#143](https://github.com/Bjorn99/lotus/issues/143))

Translations are especially welcome, and you don't need Android tooling to write one — see [CONTRIBUTING.md](CONTRIBUTING.md#translations).

## Contributing

Bug reports, feature proposals, and pull requests are all welcome. [CONTRIBUTING.md](CONTRIBUTING.md) covers the workflow and what makes a report easy to act on.

## Credits

Lotus was created by [dn0ne](https://github.com/dn0ne). This fork carries their design and branding forward.

Some UI elements inspired by [Vanilla](https://github.com/vanilla-music/vanilla). Lyrics UI inspired by [Beautiful Lyrics](https://github.com/surfbryce/beautiful-lyrics).

Libraries: [MaterialKolor](https://github.com/jordond/materialkolor), [kmpalette](https://github.com/jordond/kmpalette), [Reorderable](https://github.com/Calvin-LL/Reorderable), [jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger/src/master/).

## License

Lotus is licensed under [GPLv3](LICENSE.md).
