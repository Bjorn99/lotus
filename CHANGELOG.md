# Changelog

All notable changes to Lotus (community fork) are recorded here, newest first. For the full picture of how this fork differs from upstream `dn0ne/lotus`, see README.md.

Each release page is built from the matching section below, so the wording is aimed at the end user.

## 1.8.1

- Fixed tab names overflowing on narrow screens and top bar staying collapsed after tab switches
- Fixed per-track artwork not applying in album, playlist, and track list views — embedded art now shows everywhere when the setting is enabled
- Fixed MusicBrainz metadata search treating AND, OR, and NOT in query text as search operators instead of literal words

## 1.8.0 — Scrolling, search, and sidecar lyrics

This release is a deep polish pass. Scrolling through your library is now smooth no matter how many tracks you have. MusicBrainz search works better for obscure artists. Lotus can find lyric files next to your music automatically, and playlists imported from desktop players just work.

### Scrolling and performance

- **Player screen scroll jank eliminated.** A cross-component callback that recomputed text styles every frame has been removed. The text sizing logic now lives entirely inside the scrolling component, so the player screen no longer re-renders on every scroll pixel.
- **Tab bar text no longer wraps.** Tabs with longer names in Russian, German, and other languages now show a single line with an ellipsis instead of stacking letters vertically. The active-tab indicator is also simpler and no longer jitters during scroll.
- **Top bar now collapses properly in landscape.** When the device is rotated sideways, the top bar correctly shrinks to its compact form instead of staying at full height.
- **Inter font slimmed down.** The bundled font now covers only Latin-1 characters. Full character coverage is still available through the system font fallback.

### MusicBrainz search improvements

- **Three-stage search with smart broadening.** Lotus now tries a narrow recording search first. If that returns few results and the query is a single word, it broadens to an OR search across the recording, alias, and artist fields. If recording search still comes up short, it falls back to a release search and merges the two result sets. This catches obscure tracks where the MusicBrainz entry differs slightly from your file's tags.
- **Release search no longer returns empty results.** The release endpoint was silently dropping every result because the parser expected track-level fields that the endpoint doesn't provide. Results are now built from release-level fields, so the release search path actually works.
- **Rate limiter and retry.** MusicBrainz limits how fast you can hit their API. Lotus now paces requests at one per second and retries on server errors with exponential backoff, so bulk searches no longer fail partway through.

### Sidecar lyrics

- **Lotus now finds `.lrc` files next to your music automatically.** If you've granted access to a music folder through the folder browser, Lotus scans for lyric files matching `<songname>.lrc` and loads them before falling back to an online search. No configuration needed — drop an `.lrc` next to your music and it'll be picked up.

### Playlist improvements

- **M3U files with relative paths now import correctly.** Desktop players like VLC and foobar2000 export M3U playlists with paths relative to the playlist file. Lotus previously only understood absolute paths, so importing those files produced an empty playlist. Relative paths are now resolved against the playlist's own directory. Based on a patch by @uhrfra.
- **Album, artist, genre, and folder tabs remember their sort order.** Switch to the Artists tab, sort by Title, go to the Albums tab and sort by Year — each tab now keeps its own preference instead of sharing one global setting.

### Bug fixes

- **Fixed: premature "lyrics not found" snackbar.** When Lotus read lyrics from an embedded tag, the lyrics sheet briefly showed a misleading error message before displaying the lyrics. The error no longer appears when lyrics come from a tag.
- **Fixed: editing metadata on one track could affect another's MusicBrainz ID.** The internal key format for MusicBrainz IDs now matches across all code paths, so tag writes don't accidentally overwrite the wrong track.
- **Fixed: duplicate track keys in playlist view.** A lambda used unescaped track IDs as LazyColumn keys, which could cause crashes when two tracks had keys that looked like paths.

## 1.7.1 — Inter font, seek bar improvements, and shuffle fix

Lotus now uses the Inter font family everywhere. Inter was designed for on-screen readability, so text stays sharp whether you're browsing your library or reading synced lyrics. Nothing to configure — the font applies automatically.

### Seek bar

The animated seek bar got a handful of behind-the-scenes improvements that make it feel more responsive.

- **Drag is smoother and no longer jumps during fast scrubs.** Three separate touch handlers spread across the bar and the handle have been consolidated into one, so the handle position stays consistent no matter where your finger lands.
- **Tapping or dragging now moves the handle with a subtle spring motion.** Instead of teleporting to the new position, the handle snaps with a barely perceptible bounce that makes the seek bar feel physical rather than digital.
- **The waveform stays visible when music is paused.** Before, the wave flattened into a straight line on pause and the bar looked lifeless between songs. Now it drops to a gentle ripple so the bar stays interesting even when nothing is playing.
- **The wave animation shuts off completely when it's not shown.** If you've turned waving off in Settings, or while you're dragging the handle, the wave no longer runs invisible — no wasted CPU cycles for nothing.

### Smart shuffle

- **Fixed: tracks with missing artist metadata were treated as the same artist.** When the media database can't supply an artist name (common with untagged files or certain formats), smart shuffle used to see those unrelated tracks as being "by the same artist" and penalized them. They're now left alone, so the shuffle order isn't thrown off by missing data.

### Sorting

- **Album, artist, genre, and folder tabs now sort tracks properly within each group.** Albums are ordered by track number, artists by album then track number, genres by artist then album, and folders by title. Before, tracks inside a group inherited whatever global sort was active on the tracks tab — tap an album while browsing tracks by Title and you'd get alphabetical order instead of album order.
- **Album tabs can now be sorted by Artist and Year** in addition to the existing Title and Track Count options.

## 1.7.0 — Per-track artwork and lyrics fixes

Lotus can now display the cover art embedded inside each audio file instead of the album-level cover from Android's media database. The feature is off by default — turn it on in Settings → Theme.

### How it works

Most audio formats (MP3, FLAC, M4A, OPUS) let you store a cover image inside the file itself. Until now, Lotus ignored these and showed the same album cover for every track in an album. That's the conventional default and it's fine for most libraries.

Turning on **Per-track artwork** changes that. The player reads the embedded image directly from each file. If a track has no embedded art (or the format doesn't support it), the album cover is shown instead — you never see a blank square.

This matters for:
- DJ mixes and compilations where individual tracks carry custom art
- Audiobooks and podcasts saved as music files
- Libraries where songs in the same album folder have different covers

### Lyrics fixes

- **Fixed: importing an LRC file saved in Latin-1 showed garbled text.** The manual lyrics importer now detects when a file isn't valid UTF-8 and falls back to ISO-8859-1, so LRC files saved by older Windows editors display correctly.
- **Fixed: "Failed to read file" shown when a track simply has no lyrics.** Tracks that don't carry an embedded lyrics tag now show "No lyrics found for this track" instead of the misleading error message.

### Technical notes

The per-track artwork feature uses Android's built-in `MediaMetadataRetriever` (no new dependencies) and Coil's Fetcher + Interceptor pipeline. Extraction takes ~5–30ms per track and results are memory-cached. The fallback path is identical to the pre-1.7 code path, so nothing changes for tracks without embedded art or when the toggle is off.

## 1.6.1 — MusicBrainz search, OPUS editing, embedded lyrics, and more

### MusicBrainz search — five fixes

MusicBrainz lookup from the track info sheet was returning empty results for obscure artists, even when the song existed in MusicBrainz and you pasted its exact MusicBrainz ID. Five layered bugs were responsible.

- **Fixed: duration filter was silent and always-on.** A ±5 second duration filter was applied to every search even though a setting existed to toggle it. The filter was never wired to anything — no UI toggle was rendered, and the provider ignored the setting completely. For small-label recordings that lack duration metadata on MusicBrainz, this silently dropped every result. The filter is now off by default (widened to ±15 seconds when enabled) and a "Match duration" toggle is available in the search overflow menu.
- **Fixed: MusicBrainz IDs broken.** Pasting a MusicBrainz UUID into the search field returned nothing because Lucene's query parser treats hyphens as NOT operators. UUIDs are now detected and routed directly to MusicBrainz's recording lookup endpoint — exact match, no Lucene.
- **Fixed: special characters in track and artist names produced empty results.** Characters like `:`, `-`, `(`, `)` and others were passed straight to Lucene's query parser unescaped. Plain queries are now escaped automatically; quoted queries (from the tips dialog examples) pass through so field syntax continues to work.
- **Fixed: valid results silently dropped.** The response parser required release track-listing data to be present, but MusicBrainz's search endpoint often omits it — especially for singles and demos. Missing media/track data no longer drops the result.
- **Fixed: default 25-result limit.** MusicBrainz now returns up to 50 results per search, giving more headroom alongside the other fixes.

### Lyrics fixes

- **Fixed: embedded LRC lyrics were shown as raw text.** Files tagged with LRC-format lyrics (timestamped lines like `[00:12.50]Lyric text`, common in J-pop, K-pop, C-pop, and vocaloid music) were displayed as raw timestamps instead of karaoke-style timed lyrics. The embedded lyrics reader now parses LRC content the same way the network provider does.
- **Fixed: compilation albums showed phantom artist entries.** The Artists tab and artist navigation now use the Album Artist tag with a fallback to Artist, so compilations don't shatter into one-entry per-track artist listings.
- **Fixed: lyric sync scrolled to the wrong line.** The lyrics sheet now scrolls to the currently-synced line rather than the next one, so you can read what's being sung right now.

### OPUS metadata editing

- **Fixed: OPUS (.opus) files could not be edited.** The metadata editor (jaudiotagger) has no support for the OPUS format and rejected files with "No reader associated with this extension:opus." A custom OGG container-level tag editor now handles OPUS files directly — it modifies the OpusTags (VorbisComment) packet in-place without touching the audio data. Title, album, artist, album artist, genre, year, track number, and lyrics can all be written. Cover art editing is not yet supported for OPUS.

### MusicBrainz field qualifier case normalization

- **Fixed: capitalized field names like `Artist:` or `Release:` were treated as literal text** because MusicBrainz's Lucene field names are case-sensitive. The search provider now automatically lowercases recognized field qualifiers (`Artist:` → `artist:`, `Release:` → `release:`, etc.) so the tips-dialog syntax works regardless of capitalization.

### UI and usability improvements

- **"Match duration" toggle added to the search overflow menu.** The duration filter (which restricts MusicBrainz results to tracks within ±15 seconds of your file) was previously hidden and always-on. It is now off by default, and a toggle sits in the three-dot menu of the info-search screen.
- **Search tips dialog expanded.** Now covers MusicBrainz ID lookup (paste a UUID to find the exact recording), duration matching (what the toggle does and when to turn it off), and the existing quote and field-syntax guidance (`artist:"Name"`, `release:"Title"`, combined with `AND`).
- **Overwrite button shows a disabled state for unsupported formats.** If a file format can't be written to, the Changes screen now shows a greyed-out "This file format is not supported for metadata editing" button instead of letting you tap through to an error.

## 1.6.0 — Privacy gate fix

- **Fixed: publish-lyrics bypassed the network toggle.** When network
  lookups were turned off in Settings → Privacy, the app correctly
  blocked lyrics lookups but still allowed publishing lyrics to LRCLIB.
  Now the publish path is gated the same way — with network off, no
  outbound traffic of any kind reaches LRCLIB. The in-app privacy
  disclosure has been updated to describe both paths.

## 1.5.9 — Smart shuffle, lyrics fixes, and test safety net

Three layered bugs in lyrics fetching are fixed, a new Smart Shuffle
mode joins the classic Pure Shuffle, and the app now has a proper
automated test suite so regressions are caught before they reach you.

### Lyrics fetching — three fixes

Lyrics lookups from LRCLIB were broken in several ways at once. If you
enabled network lookups and tapped the lyrics button, the app would
often show "lyrics not found" even for songs that exist on LRCLIB.

- **Fixed: too-strict API endpoint.** The app was asking LRCLIB for an
  exact match on four pieces of metadata (track, artist, album, duration
  in seconds). If the duration in your file was off by more than two
  seconds from what LRCLIB had, or if the file was missing an album tag,
  the request returned 404. Now uses LRCLIB's fuzzy search, which needs
  only track name and artist.
- **Fixed: HTTP engine TLS failures.** The underlying HTTP engine (CIO)
  bypasses Android's TLS stack and has known certificate-validation bugs
  on some devices. If your device was affected, no HTTPS request could
  complete — lyrics and MusicBrainz would both silently fail. Switched
  to OkHttp, which delegates TLS to the OS and respects Android's
  network security policy.
- **Fixed: re-fetch blocked after embedded-lyrics fallback.** If a track
  had lyrics embedded in its ID3 tags, and you opened the lyrics sheet
  while network was off, the app would show the embedded lyrics and then
  permanently skip network lookup for that track even after you enabled
  networking. Now the app distinguishes "came from the network" from
  "came from the file" and only skips re-fetch in the first case.

The upshot: enable network lookups in Settings → Privacy, tap the lyrics
button, and it should just work — for any track that exists on LRCLIB,
with whatever metadata your files happen to have.

### Dual shuffle mode — Pure and Smart

The shuffle button now toggles between two modes instead of playing a
single fixed random order. Tap the shuffle icon in the player sheet to
cycle through Pure, Smart, and off.

- **Pure Shuffle** is what shuffling should always have been —
  mathematically unbiased. Every possible track ordering is equally
  likely. Fresh permutation generated from scratch each time the
  playlist loops around. Uses Fisher-Yates shuffle (the gold standard
  for uniform random permutations).
- **Smart Shuffle** tries to keep the playlist feeling varied without
  needing an internet connection or a listening-history profile. It
  generates five random permutations, scores each one on three penalties
  — same-artist adjacency, same-album adjacency, and how recently a
  track played — then plays the cleanest one. All computation stays
  on-device. No ML, no server, no tracking.

Backed by 25 unit tests covering uniformity, determinism, penalty math,
and a 10,000-track benchmark that completes in under 50ms. The approach
mirrors Spotify's two-mode model but uses only local metadata and
session-local recency.

### Test safety net

The lyrics gating, provider chain, redirect validation, response-size
cap, backup schema compatibility, loved-tracks DAO, track-stats DAO, and
Room database migrations now all have automated tests. CI runs them on
every pull request. A future dependency upgrade or refactor that breaks
any of these will be caught before merge.

### Other changes

- **HTTP engine: CIO → OkHttp.** Besides fixing TLS, OkHttp properly
  enforces the app's `network_security_config.xml` (HTTPS-only, system
  CAs only) — the old CIO engine was bypassing those rules entirely.
- **Cover-art redirect validation hardened.** The redirect allow-list
  check now properly validates the host portion of the URL, preventing a
  potential open-redirect through the cover-art proxy path. Backed by
  unit tests.
- **Dead domain removed** from network security config (`music.163.com`,
  the NetEase service removed in v1.5.8).
- **CI now runs instrumented tests** on a virtual Pixel 6 device, so
  database migrations and DAO queries are verified in an Android
  environment, not just on the desktop JVM.

## 1.5.8 — Privacy tightening

- Network lookups now **off by default** (opt in via Settings → Privacy).
- **NetEase lyrics removed** — the fallback scraped music.163.com with a
  spoofed User-Agent. LRCLIB remains and now handles all lyrics lookups.
- Category corrected to **Local Media Player** (was Multimedia).

## 1.5.7 — Listening stats screen

The numbers Lotus has been quietly recording since 1.5.6 now have a
home. Settings → **Listening stats** shows four sections plus a summary
card.

The summary card across the top: total time you've spent listening to
local music in this app, the running tally of plays vs skips, and how
many distinct tracks have stats yet.

**Most played** is the top ten tracks by play count. **Most listened to**
is the top ten by time spent — different from "most played" if a single
long track has only been heard once or twice but for hours combined.
**Recently played** is the last ten you crossed the halfway mark on,
newest first. **Top artists** rolls all your tracks up by artist tag and
sorts by total listening time, so the artists you keep coming back to
surface even if no single track of theirs is in the top-ten.

The screen reads from the same on-device database the recording writes
to — no network, no analytics. If you've turned the privacy toggle off
(Settings → Privacy → Record listening stats), the stats screen shows a
stub linking back to the toggle instead of an empty list.

## 1.5.6 — Listening stats (groundwork)

Lotus now records how often you play and skip each track, and how long
you've spent listening. The numbers live only on this device, in the
same SQLite database as your playlists. There's no stats screen yet —
that's the next release — but the recording happens in the background
from this version on, so by the time the screen lands you'll already
have data to look at.

A track counts as a *play* once you cross its halfway mark. Move on
before that and it's a *skip*. Seeking back after you've crossed the
halfway mark doesn't undo the play. Live streams and broken-metadata
files (where the track length is unknown) are skipped over silently.

Settings → Privacy has a new **Record listening stats** toggle. It's on
by default. Flipping it off does both halves of "stop tracking": new
events stop being written, and the existing counts are dropped. No
half-state.

Backup files (the export/restore feature from 1.5.5) now carry your
listening stats too. Restore merges them carefully: re-importing the
same backup is a no-op, and importing an older backup never rolls back
counts you've earned since. If you have the privacy toggle off when you
restore, the listening-stats portion is skipped silently and the rest of
the backup imports as before.

## 1.5.5 — Backup and restore

A new **Backup** entry in Settings. Two buttons: save your data to a
file, or restore from a file you saved earlier.

The file is plain JSON. It contains your playlists (names and the tracks
in them) and your loved tracks. Open it in any text editor if you want
to see exactly what's in it. Settings, lyrics cache, and crash logs
aren't part of it — those are easy to re-set or re-download.

Where the file goes is up to you. The export button opens the system
file picker; pick a folder on your device, in your cloud drive, or
anywhere else you want it. Restore reads from the same kind of picker.

Restore is additive on purpose. If a playlist with the same name already
exists, the imported one is skipped. Loved tracks merge into your
current set. Anything you've created since the backup stays where it is.

For tracks whose files have moved or aren't on this device any more, the
restore tells you how many couldn't be matched — those get skipped
rather than left as dangling references.

## 1.5.4 — Loved tracks

Open the menu on any track (the dots on the right) and you'll see a new
**Love** entry at the top. Tap it once to mark the track, tap it again
to unmark. Loved tracks show up as a new **Loved tracks** section at the
top of the Playlists tab, alongside Recently added and Random mix.

The "Love" toggle is also available on the now-playing sheet menu, so
you can mark a track without going back to the list.

The state is local — nothing is sent anywhere — and it's stored in the
same on-device database as your playlists. Existing data isn't touched;
the database picks up a new table on first launch via a small Room
migration.

## 1.5.3 — Quick Settings tile for play/pause

Drag a **Play / Pause** tile into the system Quick Settings panel (swipe
down twice → pencil icon → drag from "available tiles") and you can
pause or resume Lotus without unlocking the phone.

The tile reflects the current state — shows a play icon when nothing is
playing, a pause icon while a track is going. Tapping it does the
obvious thing.

If Lotus has never started a playback session since the last reboot, the
tile won't have anything to control yet; open the app and start a track
once and it picks up from there.

## 1.5.2 — Sleep timer: presets and finish-current-track

Two small additions to the existing sleep timer.

Preset chips above the slider — 15, 30, 45, 60, 90 minutes. The slider
still works for anything in between; the chips are there for the common
cases so you don't have to fiddle when the answer is "thirty."

A **Finish current track** toggle below the slider. When it's on, the
timer doesn't stop the player the moment it hits zero. It waits for the
song that's already playing to end naturally and stops then, so you
don't get cut off mid-song.

The slider is also disabled while the timer is running, which fixes a
small rough edge where dragging it mid-countdown would flicker the
displayed minutes without changing the actual expiry time.

## 1.5.1 — Privacy disclosure on the Privacy screen

The Privacy screen added in 1.5.0 had only the master kill switch on it.
This release fills out the rest: a plain-English breakdown of what the
app sends, where it goes, and what stays local.

**What leaves your device** lists the three outbound features:

- LRCLIB sends track title, artist, album if known, and duration when
  you open the lyrics view for a track that doesn't have lyrics cached.
- NetEase sends track title and artist as a fallback when LRCLIB has
  nothing, only if you have the NetEase fallback enabled.
- MusicBrainz sends the search query you type plus the track's duration,
  only when you tap Search on the metadata screen.

**What stays on your device** lists the local data: lyrics cache, scan
results, settings, and crash logs. Sharing a crash log is a manual
action through the system share sheet — nothing is sent on its own.

A final card states what isn't there: no analytics, no usage tracking,
no diagnostic uploads.

The disclosure exists so you don't have to read the source to answer
"what does this app do with my data". The text is deliberately short —
if you want the full picture, the source is on GitHub.

## 1.5.0 — Privacy and network hardening

A few changes to how the app handles network calls and what data can
leave your device. Nothing visible during normal use, but worth walking
through if you care about that side of things.

A new **Privacy** section in Settings, with one switch at the top —
**Allow network lookups**. Turning it off puts the app into a strict
offline mode: no LRCLIB, no NetEase, no MusicBrainz. The rest of the
app keeps working from your local library.

The HTTP client is now stricter about a few things:

- **HTTPS only**, enforced at the OS level via a network security
  config. Even a hypothetical bug that tried to hit a plain HTTP URL
  would be refused.
- **System CAs only**. User-installed certificate authorities are not
  trusted. This blocks corporate MITM proxies and any custom CA on the
  device from intercepting Lotus's traffic. Debug builds still allow
  user CAs so developers can run local proxies; release builds don't.
- **No automatic redirect-following.** A poisoned DNS response or a
  misbehaving upstream that tries to 30x-redirect somewhere unexpected
  now fails loudly instead of silently following. The one place that
  legitimately needs to follow a redirect (CoverArtArchive, which serves
  album art via a CDN) does so explicitly, with the destination URL
  validated against an allow-list.
- **Response size cap of 5 MB.** A malicious or buggy upstream can't
  send a multi-gigabyte body that fills memory. Generous enough that no
  real lookup hits it.

Backup and device-transfer rules are now explicit. Your settings (theme,
tab order, sort prefs, etc.) still survive a reinstall. The lyrics cache
and crash logs do not — listening history is the kind of thing that
shouldn't be quietly riding to Google's servers along with the rest of
your Auto Backup.

None of this changes how the app behaves day-to-day. The provider
endpoints are the same, the data sent to them is the same, and the
default state for upgrading users is unchanged. The tightening is
underneath.

## 1.4.1 — Small Compose performance + manifest fixes

A handful of real fixes pulled out of the Android lint report. None of
these are visible as new features, but a couple were affecting playback
and the lyrics screen in subtle ways.

- The synced-lyrics view was creating a new Kotlin Flow on every
  recomposition (one for each visible line, every time a state change
  triggered a redraw). The `collectAsState` reading from that flow was
  effectively being reset on each pass, so position updates could be
  missed and the line-highlight could feel a beat behind on busy songs.
  The flow is now built once and reused — same data, much less churn.
- Three places used `Modifier.offset(x = state)` with state-backed
  values: the lyrics-mode capsule indicator, the segment-options capsule
  in Settings, and the seek-bar handle position. Each one was triggering
  a recomposition every animation frame instead of just relayout.
  Switched to the lambda overload (`Modifier.offset { IntOffset(...) }`)
  which defers the offset read to the layout phase. Smoother animation,
  lower CPU during playback.
- The manifest declared `READ_EXTERNAL_STORAGE` and
  `WRITE_EXTERNAL_STORAGE` without a max-SDK cap. On Android 13+ these
  are deprecated and not granted at runtime — `READ_MEDIA_AUDIO` takes
  their place. They're now scoped with `android:maxSdkVersion="32"` so
  they only register on devices that actually use them. No behaviour
  change on any version, just cleaner manifest output.

## 1.4.0 — NetEase as a lyrics backup, plus a cache fix

If LRCLIB doesn't have the lyrics you're looking for, the app now asks
NetEase Cloud Music as a fallback. NetEase has a much wider catalogue
(especially for Asian and indie pop), so the "lyrics not found"
snackbar should show up a lot less often. The fallback is on by default
and there's a switch in Settings → Lyrics if you'd rather keep things
LRCLIB-only.

There's also a small but slightly embarrassing fix: when you opened a
track you'd already pulled lyrics for, the app was hitting the network
anyway and only checking the local cache as a side effect of the view
model. The cache lookup now lives where it should — inside the lyrics
fetcher itself — so cached lyrics show instantly and the re-fetch button
still bypasses the cache the way you'd expect.

A couple of things worth knowing:

- NetEase is a Chinese service; the request goes to music.163.com and
  includes the track title and artist (no other metadata). If that's not
  OK for your threat model, flip the switch off.
- NetEase doesn't accept lyric submissions, so the "publish to remote"
  button still goes to LRCLIB only.
- The `lyrics_not_found` message no longer says "on LRCLIB" since it now
  means "we tried every source you've enabled and nothing matched."

Under the hood: `ChainLyricsProvider` composes an ordered list of
sources and returns the first hit. NetEase is wrapped in a
`GatedLyricsProvider` that consults the settings flag at call time, so
toggling the switch takes effect without an app restart. The shared
`Throwable.toNetworkError()` mapping moved out of LRCLIB into its own
file because both providers need it.

## 1.3.6 — Fix "Play next" + queue handling in shuffle mode

**Layman:** Two related queue bugs. Tapping "Play next" on a track
already in the queue moved it to the wrong position (right *before* the
currently-playing track instead of after). And in shuffle mode, "Play
next" didn't actually play the track next — it inserted the track at a
random position in the shuffle order, so it would play at some
unpredictable later time. Fixed: the move math is correct now, and
tapping "Play next" while shuffle is on switches to the non-shuffle
Repeat mode (with a snackbar) so the track reliably plays next.
Re-enable shuffle from the playback-mode toggle when you want it back.

**Technical:**
- `OnPlayNextClick` reorder branch (track already in queue): the
  destination index passed to `OnReorderingQueue` was always
  `currentTrackIndex`. Media3's `moveMediaItem(from, newIndex)` places
  the item at `newIndex` post-move; for a track at index 5 moved to
  index 3, the previously-current item shifts to index 4, so the moved
  track ends up *before* the current one. Fixed by computing destination
  conditionally:
  - `trackIndex > currentTrackIndex` → `currentTrackIndex + 1`
  - `trackIndex < currentTrackIndex` → `currentTrackIndex` (current
    shifts down by 1 when target is removed; landing target at this
    index puts it right after the new current position)
- `OnPlayNextClick` add-new branch (track not yet in queue): in shuffle
  mode `Player.addMediaItem(currentMediaItemIndex + 1, item)` inserts at
  the timeline position, but Media3's `DefaultShuffleOrder.cloneAndInsert`
  places the new item at a random position in the shuffle order,
  defeating "play next" intent. Workaround: detect shuffle, switch
  playback mode to Repeat, persist via `savedPlayerState.playbackMode`,
  then add at `currentIndex + 1` as before. Snackbar explains the change.
- `OnAddToQueueClick` left alone — appending to the timeline + random
  shuffle position is consistent with what users mean by "add to queue"
  (the order is already random by design).

## 1.3.5 — Stop crashes during MusicBrainz / LRCLIB requests

**Layman:** Searching online for track metadata (the "Track info →
Search" flow) and fetching lyrics from LRCLIB used to crash the app on
flaky networks — DNS hiccups, TLS / certificate weirdness, slow
connections. Now those failures pop a snackbar with a helpful message
instead of taking the app down. Also: the request timeout dropped from
**3 minutes** to **20 seconds**, so a slow lookup gives you an error
long before you'd reach for the back button.

**Technical:**
- Three crash-paths fixed across `MusicBrainzMetadataProvider` (2
  network calls) and `LrclibLyricsProvider` (3 network calls). Each call
  previously caught only a narrow set: `UnresolvedAddressException`,
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
  cancellation still unwinds correctly.
- `body()` parse paths gain the same `Throwable` fallback, mapping to
  `DataError.Network.ParseError`.
- HTTP timeouts in `PlayerModule.kt` retuned:
  - `requestTimeoutMillis 180_000 → 20_000` (3 min → 20 s)
  - new `connectTimeoutMillis = 10_000`
  - new `socketTimeoutMillis = 15_000`
- Dead code removed in `getCoverArtBytes`: a `Log.d(...)` after a
  `return` was unreachable. Moved before the return so 307 redirect
  responses (which Ktor follows automatically — should never reach this
  branch in practice) actually log when they do.
- `println("RESPONSE BODY: ...")` left over from earlier debugging in
  `LrclibLyricsProvider.postLyrics` replaced with a proper `Log.d` call.

## 1.3.4 — Fix media notification: show song title, artist, and artwork

**Layman:** When you play a song, the system notification (and the lock
screen, and Android Auto, and your watch's media tile) used to say just
"Lotus is playing" instead of the actual song title. Fixed: every track
now carries its title, artist, album, album art, year, and track number
into the notification, so you finally see what's playing.

**Technical:**
- Root cause: both Track → MediaItem conversion sites
  (`TrackRepositoryImpl` at scan time and `TrackSerializer` at
  saved-state restore time) called `MediaItem.fromUri(uri)`, which
  builds a `MediaItem` with no `MediaMetadata`. Media3's automatic
  notification reads `mediaMetadata.title` / `displayTitle`, so when
  both are null the system fell back to the app label.
- New helper: `buildMediaItem(uri, title, artist, album, albumArtist,
  genre, year, trackNumber, coverArtUri)`. Single source of truth for
  Track → MediaItem with full `MediaMetadata` populated:
  - `title`, `artist`, `albumTitle`, `albumArtist`, `genre`
  - `releaseYear`, `trackNumber` (parsed via `?.toIntOrNull()` so
    weirdly-formatted tags don't crash)
  - `artworkUri` from the MediaStore album-art URI (system-loaded — no
    manual bitmap decode)
  - `displayTitle` + `subtitle` set explicitly because different OEMs
    prefer different fields for notification primary / secondary lines
  - stable `mediaId` = uri string, plus `RequestMetadata.mediaUri` for
    MediaSession correlation across queue reorders + process restarts
- Two call sites migrated to `buildMediaItem(...)`.
- Modernisation: `POST_NOTIFICATIONS` permission added to the manifest.
  Required on Android 13+ for foreground-service media notifications to
  be reliably visible.

## 1.3.3 — Extract PlaylistEditor out of PlayerViewModel

**Layman:** Second step in the PlayerViewModel cleanup begun in 1.3.2.
All the "create / rename / delete / add / remove / reorder" playlist
operations move into their own class. No visible change — same
behaviour, same snackbars, same results.

**Technical:**
- Phase 3 cleanup, item 3 of 4 (second pass). Another pure extraction.
- New `PlaylistEditor` class with six `suspend` methods — `create`,
  `rename`, `delete`, `addTracks`, `removeTracks`, `reorder`. Constructor
  takes `PlaylistRepository`.
- The name-collision check now takes an explicit `existingNames` list.
  Name checks, snackbars, and the "add tracks that are already present
  still adds them" quirk are all preserved exactly.
- Six `PlayerScreenEvent` branches in the VM now delegate to the editor.
  `_selectedPlaylist` mutations stay in the branches because they're
  VM-local state.
- `parseM3U` still calls `playlistRepository.insertPlaylist` directly —
  its behaviour (no name-collision check) differs from the editor and is
  kept identical to avoid a behavioural change in this refactor.
- Net: `PlayerViewModel.kt` **1439 → 1412 lines** (−27). Combined with
  v1.3.2 that's −129 lines total out of the 1541-line original.

## 1.3.2 — Extract LyricsFetcher out of PlayerViewModel

**Layman:** Code cleanup with no visible changes. `PlayerViewModel` has
grown into a 1541-line file handling playback, metadata, playlists,
lyrics, and more — all mixed together. This release extracts the lyrics
fetching logic into its own focused class so future work on lyrics
doesn't drag the whole file into every diff. First step in a multi-
release split; behaviour on your phone is unchanged.

**Technical:**
- Phase 3 cleanup, item 3 of 4 (first pass). Pure extraction — no
  state-coupling changes, no behaviour changes, no public API changes.
- New `LyricsFetcher` class owns the two formerly private methods
  `readFromTag` and `fetchFromRemote` (previously inside the VM).
  Constructor takes `LyricsReader`, `LyricsProvider`,
  `LyricsRepository`.
- `PlayerViewModel` now holds a private `lyricsFetcher` field and
  delegates at the three call sites. `readFromTag` takes the coroutine
  scope explicitly so snackbar errors keep launching off `viewModelScope`
  exactly as before.
- Net: `-102 lines` in `PlayerViewModel.kt` (1541 → 1439).

## 1.3.1 — Stability: remove force-unwraps from playback UI

**Layman:** Pure stability fix, no visible changes. The player sheet and
track-info screens used to assume there was always a current track when
they were drawn. Most of the time that's true, but during certain state
transitions (clearing the queue, stopping playback, app coming back from
background) the assumption could briefly fail and crash the app. The
screens now skip drawing for that one frame instead.

**Technical:**
- Replaces every `!!` non-null assertion in main sources (six sites
  total) with a null-safe pattern. Zero `!!` operators remain.
- `BottomPlayer`, `ExpandedPlayer`, and `PlaybackControl` in
  `PlayerSheet.kt`: each `derivedStateOf { playbackState.currentTrack!! }`
  is split into a nullable `derivedStateOf { playbackState.currentTrack }`
  plus an explicit `?: return@<scope>` guard. Downstream call sites
  unchanged.
- `TrackInfoSheet.kt` child routes: `state.track!!` replaced with
  `val track = state.track ?: return@composable`. If the navigation
  child renders before its parent populated the track, the route no-ops
  instead of NPE-ing.
- `AnimatedVisibility` keeps composing the child for the duration of the
  exit animation — that's the gap these guards close.

## 1.3.0 — Drop Realm, Room-only storage

**Layman:** A long-overdue cleanup with no visible behaviour change.
Older versions of this app used a database library called Realm for your
playlists and lyrics. v1.2.0 switched to Room and ran a migration so
your data carried over. That migration has now had a release to complete
on every device, so this version rips Realm out entirely. The APK is
meaningfully smaller and the app starts up a little faster.

**Technical:**
- Removed: `RealmPlaylistRepository`, `RealmLyricsRepository` (with
  their embedded `RealmObject` schemas), `RealmToRoomMigrator` + its
  instrumented test, and the `realmToRoomMigrationDone` flag in
  `Settings`.
- Build: dropped `io.realm.kotlin` plugin from root +
  `app/build.gradle.kts`, `libs.realm.library.base` from dependencies,
  and the `realm` entries from `libs.versions.toml`.
- DI: removed the `single<RealmToRoomMigrator>` Koin binding and the
  associated import from `PlayerModule.kt`.
- Application class: removed the `realmToRoomMigrator` inject and the
  `applicationScope.launch { migrator.migrateIfNeeded() }` call from
  `PlayerApp.onCreate()`.
- Room DAOs / entities / repositories are unchanged and remain the sole
  persistence layer for playlists + cached lyrics.
- APK size shrinks by ~10–15 MB per ABI.
- No schema change on the Room side; upgrading users keep their data.

## 1.2.3 — Smart playlists + release notes overhaul

**Layman:** The Playlists tab now has a "Smart" section at the top with
two auto-filled lists — **Recently added** (files added to your library
in the last 30 days) and **Random mix** (up to 100 tracks shuffled).
They behave like any other playlist: tap to play, export to M3U, etc.
Release pages on GitHub now include a human-readable description of
what's in the release plus a summary of how this fork differs from
upstream.

**Technical:**
- `SmartPlaylists` domain object — pure Kotlin builders for
  `recentlyAdded` (`dateModified > now - 30d`, capped at 100, sorted
  desc) and `randomMix` (seeded shuffle, capped at 100). Derived in
  `PlayerScreen` composable layer so the localised names come from
  `stringResource` without forcing `Context` into the `PlayerViewModel`
  constructor.
- `Tab.Playlists` branch now renders a two-section layout: Smart (header
  + cards/rows, hidden when empty or in selection mode) then user
  playlists.
- Smart-list taps route through `onAlbumPlaylistSelection` → immutable
  `PlayerRoutes.Playlist` view (can't be renamed or reordered).
  Long-press is a no-op — smart lists aren't part of bulk selection.
- `.github/workflows/release.yml` now extracts the matching section from
  `CHANGELOG.md` and uses it as the GitHub Release body. Each release
  page also gets a stable fork-summary preamble.
- Release workflow gains a preflight step that fails fast when the tag
  name does not match the committed `versionName`. Prevents the "tag
  pushed before version bump merged" mis-release mode.

## 1.2.2 — Export to M3U + global library search + maintenance

**Layman:** Two new features. Every playlist view has a new Download
icon that saves the playlist as a standard `.m3u8` file to any folder
you pick — playable in VLC, Musicolet, PowerAmp, Foobar2000, and almost
every other music app. A new magnifying-globe icon in the top bar opens
one search box that looks through everything at once: tracks, albums,
artists, genres, and playlists. Also fixed: the About page's Repository
and Feedback buttons now open *this* fork's links (they previously
pointed at the upstream author's repo and personal email).

**Technical:**
- **M3U export** — `M3uExporter` domain object, pure Kotlin formatter
  for Extended M3U8 (UTF-8): `#EXTM3U` header + `#EXTINF:<seconds>,
  <artist> - <title>` + absolute path per track. Covered by 9 JVM unit
  tests. `rememberM3uExport()` composable wraps
  `ActivityResultContracts.CreateDocument("audio/x-mpegurl")`; writes
  via `ContentResolver.openOutputStream`; success/failure toast. Uses
  absolute paths because SAF hides the destination folder. Download icon
  added on top bar of both `MutablePlaylist` and `Playlist` views;
  disabled when the playlist is empty.
- **Global library search** — `GlobalSearchSheet`: full-screen `Dialog`,
  single `SearchField`, five grouped result sections (tracks top-8,
  playlists / albums / artists / genres top-5 each). Empty sections
  collapse; blank/no-match states render distinct hints. Reuses existing
  `filterTracks` / `filterPlaylists` helpers — no ViewModel changes.
  Query state local to the sheet; dropped on dismiss.
- **Upstream references removed from in-app UI:**
  - `repo_url` → `https://github.com/Bjorn99/lotus` (was `dn0ne/lotus`)
  - `feedback_url` → `https://github.com/Bjorn99/lotus/issues/new` (was
    `mailto:dev.dn0ne@gmail.com`)
  - MusicBrainz + LRCLIB `User-Agent` contact → fork repo URL
- README `## Support` section rewritten to credit the upstream author
  and link to their Liberapay.

## 1.2.0 — First community release

**Layman:** The foundational release. Everything needed to keep the app
stable and maintainable going forward. The app no longer crashes
silently when lyrics reading hits a weird file, and if something does go
wrong, crashes are saved to a log file you can share. The underlying
database was modernised. You can share any track directly to other apps.
The release itself is signed and published through an automated pipeline.

**Technical:**
- **Realm → Room migration** with one-shot `RealmToRoomMigrator`
  (legacy store read once on first launch; Realm + Room DAOs run
  side-by-side for one release so the legacy store can be removed in a
  later pass).
- **Crash reporter** (`CrashReporter`) writes uncaught-exception stack
  traces to private app files, exposed via `FileProvider` authority
  `${applicationId}.crashlogs` for share-sheet delivery. No network, no
  analytics, no telemetry.
- **`LyricsReaderImpl` hardened:** `jaudiotagger` exceptions are caught
  and logged (wide-net `Throwable` — jaudiotagger throws many checked
  types); the temp file is cleaned up in `finally`; no longer nukes the
  shared cache directory (Coil's image cache lives there).
- **CI pipeline** (`.github/workflows/ci.yml`): JDK 17, unit tests,
  `assembleDebug` / `assembleRelease` / `lintRelease`, detekt + ktlint;
  artifact uploads for APKs + reports; diagnostic dump on failure.
- Lint / detekt / ktlint all in "report-only" mode (`abortOnError =
  false` / `ignoreFailures = true`) so legacy findings don't block CI;
  findings surface via the uploaded HTML reports.
- **Release signing pipeline** (`.github/workflows/release.yml`):
  triggers on `v*` tag push, decodes keystore from
  `LOTUS_KEYSTORE_BASE64`, runs `assembleRelease`, verifies with
  `apksigner`, publishes per-ABI + universal APKs and `SHA256SUMS.txt`
  to a GitHub Release.
- **Share track** — `ACTION_SEND` intent with MediaStore content URI,
  MIME from `ContentResolver.getType()` (`audio/*` fallback), title as
  `EXTRA_SUBJECT`, `FLAG_GRANT_READ_URI_PERMISSION`; dropdown entry in
  `TrackMenu` wired from `TrackListItem` + both `PlayerSheet` call
  sites.
- **Fork rebrand**: `applicationId = com.dn0ne.lotus.community` so the
  community build coexists with the original on the same device.
- README retargeted from upstream `dn0ne/lotus` to this fork's releases.
