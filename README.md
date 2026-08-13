<div align="center">
  <img src="fastlane/metadata/android/en-US/images/icon-fit.png" width="200px" />

  # Lotus

  ### Music player for Android
  
</div>

Lotus is a clean, offline-first music player with Material You design.
This is a community continuation of [dn0ne's original app](https://github.com/dn0ne/lotus).
I fell in love with Lotus because of what dn0ne built — the design, the
feel, the attention to detail. When upstream development paused, I chose
to maintain it so the app could keep going. All design, branding, and
prior work are theirs. Application ID:
`com.dn0ne.lotus.community`.

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

True randomness clusters — flip a coin enough times and you'll get runs of heads. A pure shuffle does the same thing with artists, and three songs by the same artist in a row feels like the app is broken even though the math is working fine.

Rather than draw an order and hope, Smart Shuffle builds one. It deals the tracks out like cards: the artist with the most tracks goes first, one into every other slot, wrapping onto the slots it skipped once the first pass runs out. Handing out the most crowded artist first is what forces its tracks furthest apart, and the result is a guarantee rather than an average — **whenever an order with no artist repeats exists at all, Smart Shuffle finds one.** The only queues it cannot solve are the ones nobody could: if a single artist holds more than half the tracks, some repeats are unavoidable, and it lands within one of the provable minimum.

A deal on its own is too tidy — with three artists it produces a metronomic `A B C A B C`. So a short second pass makes random swaps, keeping any that don't make the order worse. That breaks up the regularity, and it also picks up the two things the deal ignores: tracks from a compilation where one album spans several artists, and the tracks you just heard, which are pushed away from the front of the next queue. Each proposed swap is scored by what it changes rather than by rescoring the queue, and the number of swaps is capped, so the work stays bounded however long the queue is.

An order is scored on three criteria: same-artist adjacency (weight 10), same-album adjacency (weight 3, and only when the artists differ), and tracks carried over from the previous pass (weight 5, halving every quarter of the queue). Everything runs on-device, with no listening history and no network.

Measured against the previous approach — five random draws, keep the best — on a 60-track queue drawn from six artists: five draws left about six same-artist back-to-backs on average, and twenty-five on a two-artist queue. The current implementation leaves none in both cases.

Playlist sequencing as constrained optimisation is well-trodden ground: Pauws, Verhaegh and Vossen model it that way and solve it with an adapted simulated annealing algorithm [1]. Lotus keeps the idea of a weighted cost to minimise but reaches the answer by construction instead of by search, which is what makes the guarantee possible. One neighbouring line of work is worth naming precisely because it turned out *not* to fit: bioinformatics has a long history of sequence shuffling, such as Altschul and Erickson's Eulerian-walk permutation method and later tools like uShuffle [2]. Those solve the opposite problem — they sample permutations that *preserve* local statistics like dinucleotide and k-let counts, to build a null model for significance testing. Smart Shuffle exists to break up the adjacencies a plain shuffle leaves behind. Worth reading, not worth copying.

### References

1. Pauws, Verhaegh & Vossen, "Music playlist generation by adapted simulated annealing" (2008), *Information Sciences* 178(3):647–662. [doi:10.1016/j.ins.2007.08.019](https://doi.org/10.1016/j.ins.2007.08.019)
2. Altschul & Erickson, "Significance of nucleotide sequence alignments: a method for random sequence permutation that preserves dinucleotide and codon usage" (1985), *Mol Biol Evol* 2(6):526–538. [doi:10.1093/oxfordjournals.molbev.a040370](https://doi.org/10.1093/oxfordjournals.molbev.a040370) — Jiang, Anderson, Gillespie & Mayne, "uShuffle: a useful tool for shuffling biological sequences while preserving the k-let counts" (2008), *BMC Bioinformatics* 9:192. [doi:10.1186/1471-2105-9-192](https://doi.org/10.1186/1471-2105-9-192)

Full version history in [CHANGELOG.md](CHANGELOG.md).

## Download

[<img src="https://f-droid.org/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/com.dn0ne.lotus.community/)

F-Droid is the recommended channel — auto-updates, signature verification, and no manual APK sideloading.

The original upstream build (different application ID) is also on
[F-Droid](https://f-droid.org/packages/com.dn0ne.lotus) — that is not produced by this fork.

## Support the original author

This fork exists because of [dn0ne](https://github.com/dn0ne)'s work. If
Lotus has been useful to you, consider thanking them on
[Liberapay](https://en.liberapay.com/dn0ne/donate).

## Build

1. Clone the repository:
   ```bash
   git clone https://github.com/Bjorn99/lotus.git
   ```
2. Open the project in Android Studio.
3. Wait for Gradle sync, then click **Run** or press `Shift + F10`.

Release builds are automated via CI — see [docs/RELEASING.md](docs/RELEASING.md)
for the full process.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on bug reports, feature proposals, and pull requests.

## Credits

Some UI elements inspired by [Vanilla](https://github.com/vanilla-music/vanilla).

Lyrics UI inspired by [Beautiful Lyrics](https://github.com/surfbryce/beautiful-lyrics).

Libraries: [MaterialKolor](https://github.com/jordond/materialkolor), [kmpalette](https://github.com/jordond/kmpalette), [Reorderable](https://github.com/Calvin-LL/Reorderable), [jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger/src/master/).

## License

Lotus is licensed under [GPLv3](LICENSE.md).
