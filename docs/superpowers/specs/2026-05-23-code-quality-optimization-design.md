# Code quality and battery optimization design

Status: design approved 2026-05-23. Implementation not yet scheduled.
Scope: battery fixes, code health refactors, APK size reductions.
All work happens on `nightly` branch — never pushed until stable.

## Goal

Three objectives, ordered by impact:

1. **Battery fixes** — eliminate the two wasteful polling loops and enable
   audio offload so the app doesn't drain battery during playback.
2. **Code health** — reduce the largest files to maintainable size and remove
   duplicated patterns that make future changes riskier.
3. **APK size** — small wins that don't compromise anything.

No new features. No UI changes visible to the user (except the existing UI
updating slightly less often, which no one will notice). No dependency
upgrades beyond removing packages we no longer need.

## What we are NOT doing

- Replacing Room, Ktor, or Coil — the byte savings aren't worth losing type
  safety, migration tooling, or HTTP content negotiation.
- Dynamic feature modules or Android App Bundle — we distribute APKs via
  GitHub and F-Droid, not Google Play.
- Full ViewModel rewrite — too risky, too many touchpoints. Extract and
  simplify incrementally.
- Replacing Material3 icons-extended with hand-picked icons — the R8
  tree-shaker already strips unused icons. The residual is small.

## Tier 1 — Battery fixes

### 1.1 Replace 5-second MediaStore poll with ContentObserver

**Current state** (PlayerViewModel.kt init, lines 296-326):
```kotlin
viewModelScope.launch(Dispatchers.IO) {
    while (true) {
        val tracks = trackRepository.getTracks()
        // compare and update state...
        delay(5000L)
    }
}
```
This runs a full ContentResolver query against MediaStore every 5 seconds
for the entire app session, even when the user is on a different tab.
ContentResolver queries on the main MediaStore tables are not cheap — they
scan the external storage database and return cursor results.

**Fix:** Register a `ContentObserver` on `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
Android's media scanner sends a notification whenever audio files are added,
removed, or modified. The observer fires only when something actually changes,
eliminating 99.9% of the wasted queries.

```kotlin
private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            val tracks = trackRepository.getTracks()
            // compare and update state...
        }
    }
}

init {
    context.contentResolver.registerContentObserver(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        true,  // notify for descendants
        contentObserver
    )
}
```

**Verification:** After the change, `dumpsys batterystats` should show zero
background ContentResolver queries when the app is idle. The tracks list
still updates when music is added or removed — verified by adding a file
via adb push and confirming it appears.

### 1.2 Relax position polling from 50ms to 200ms

**Current state** (PlayerViewModel.kt, lines 1421-1434):
```kotlin
while (_playbackState.value.isPlaying) {
    _playbackState.update {
        it.copy(position = player.currentPosition)
    }
    delay(50)  // 20 times per second
}
```
This triggers Compose recomposition 20 times per second during playback for
the seek bar and position display. `player.currentPosition` is a cheap call
(just reads ExoPlayer's internal clock), but the StateFlow update and
subsequent Compose recomposition tree diff is not free.

**Fix:** Change `delay(50)` to `delay(200)` — 5 updates per second instead
of 20. The seek bar updates every 200ms which is still visually smooth
(human eyes perceive motion at ~10-15 fps; a seek bar doesn't move fast
enough to need more).

No architectural change needed. One number.

**Verification:** The seek bar and position text should appear identically
smooth. If 200ms shows visible jitter on the seek bar, adjust to 150ms.
Measure CPU usage during playback with `top` or Battery Historian — the
PlayerViewModel coroutine should consume proportionally less CPU time.

### 1.3 Enable ExoPlayer audio offload

**Current state:** ExoPlayer is configured with default track selection
parameters. Audio offload (DSP decoding) is not explicitly requested.
Without offload, the CPU decodes every audio frame, keeping one core awake
for the entire duration of playback.

**Fix:** Set audio offload preferences in the track selector:
```kotlin
val params = trackSelector.parameters
    .buildUpon()
    .setAudioOffloadPreferences(
        AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
            .setIsGaplessSupportRequired(true)
            .setIsSpeedChangeSupportRequired(false)
            .build()
    )
    .build()
trackSelector.setParameters(params)
```

This lets the DSP decode audio while the CPU sleeps, which is the single
largest battery win for any music player.

**Known risk:** Audio offload is fragmented across OEMs. Some devices
(Fairphone, older Samsung) have buggy DSP firmware that can cause playback
stops, incorrect gapless transitions, or Dirac effects disabling offload.
Mitigation: wrap in a try-catch and fall back to CPU decoding. Consider
making offload opt-in via a setting if field reports show issues.

**Verification:** In Battery Historian, the "CPU Running" graph should
show sparse short bars (CPU waking briefly to fill DSP buffer) instead of
a continuous solid bar during playback. Playback must still be gapless
(tested with an album that has continuous track boundaries).

## Tier 2 — Code health

### 2.1 Extract TabContent composable from PlayerScreen.kt

**Current state:** `PlayerScreen.kt` (1874 lines) has 6 tabs — Tracks,
Playlists, Albums, Artists, Genres, Folders — each with near-identical
blocks for grid mode, list mode, selection mode, sorting, and filtering.
This is ~430 lines of copy-pasted code with minor variations.

**Fix:** Create a `TabContent` composable that takes a configuration object:
```kotlin
data class TabConfig(
    val title: String,
    val items: List<Any>,
    val gridMode: Boolean,
    val selectionMode: Boolean,
    val sortOptions: List<SortOption>,
    val onItemClick: (Any) -> Unit,
    val onItemLongPress: (Any) -> Unit,
)
```
Each tab becomes a call site that provides its config. The 430 lines of
duplicated code collapse to ~80 lines of configuration + the single shared
implementation.

**Risk:** Touch-heavy UI. Test every tab in grid, list, and selection mode
on device before merging. Pay special attention to the Folders tab — it has
folder-specific behavior that may not fit the generic pattern.

### 2.2 Split PlayerViewModel.onEvent() into handler functions

**Current state:** `onEvent()` is 925 lines of when-branches handling every
player event. Reading it requires holding the entire method in your head.

**Fix:** Extract each event-category into a private handler function:
```kotlin
fun onEvent(event: PlayerEvent) {
    when (event) {
        is PlayerEvent.Playback -> handlePlaybackEvent(event)
        is PlayerEvent.Playlist -> handlePlaylistEvent(event)
        is PlayerEvent.UI -> handleUIEvent(event)
        is PlayerEvent.Navigation -> handleNavigationEvent(event)
        // ... etc
    }
}
```
Zero behavior change. Each handler is independently readable and testable.

### 2.3 Unify GatedLyricsProvider and GatedMetadataProvider

**Current state:** Two files with structurally identical code — both wrap
a delegate with an `isEnabled` lambda that checks SharedPreferences.
The only difference is the interface type of the delegate.

**Fix:** Create a single generic implementation:
```kotlin
class GatedProvider<T>(
    private val delegate: T,
    private val isEnabled: () -> Boolean,
) {
    fun <R> call(block: (T) -> R): Result<R, DataError.Network> {
        if (!isEnabled()) return Result.Error(DataError.Network.NoInternet)
        return block(delegate)
    }
}
```
The two existing files become thin typealias wrappers or are deleted entirely.

### 2.4 Remove dead catch blocks in MusicScanner.kt

**Current state:** `MusicScanner.kt` has `catch (e: java.lang.Exception)`
blocks on lines 97 and 156 that follow `catch (e: Exception)` blocks.
In Kotlin, `Exception` and `java.lang.Exception` are the same type — the
second catch will never execute. This is dead code.

**Fix:** Delete the duplicate catch blocks.

### 2.5 Evaluate single-implementation interfaces

**Current state:** 6 repository interfaces + `LyricsReader` +
`MetadataWriter` each have exactly one implementation. They exist for
testability via Koin mock injection but add indirection without value
for production code.

**Decision: keep them.** The indirection cost is minimal (one extra file
per interface, no runtime overhead), and they make unit testing possible
without Mockito or reflection. Removing them would require rewriting tests
to use fake Room databases or Mockito spies, which is more complexity than
the interfaces themselves.

Exception: if an interface has zero test usages (check with grep), it can
be inlined.

## Tier 3 — APK size (low urgency)

### 3.1 Enable resource shrinking

```kotlin
// app/build.gradle.kts, release build type
isShrinkResources = true  // was false
```

This works with R8 — after dead code is removed, any resources only
referenced by that dead code are also stripped. Typical saving: 100-300 KB.
Risk is very low since we have no reflective resource access.

### 3.2 Enable obfuscation

Remove `-dontobfuscate` from `proguard-rules.pro`. This shortens class
and method names, recovering ~100-200 KB. Must verify that Kotlin
serialization works correctly after obfuscation (the `@Serializable`
annotation should generate keep rules automatically via KSP, but test it).

### 3.3 Remove scrollbars dependency

The `scrollbars` library (LazyColumnScrollbar) can be replaced with
Compose Foundation's built-in scrollbar support available since
Compose 1.5. This removes one external dependency.

---

## Tier summary

| Tier | Items | Effort | Impact | Risk |
|---|---|---|---|---|
| 1 — Battery | 3 fixes | 2-3 days | High (user-visible battery improvement) | Low |
| 2 — Code health | 5 refactors | 3-5 days | Medium (maintainability) | Low-Medium |
| 3 — APK size | 3 tweaks | 1 day | Low (already at 4.6 MB) | Very Low |

## Success criteria

- CI stays green on all commits
- No user-visible behavior change (except battery drain reduced)
- APK stays at or below current 4.6 MB
- No new dependencies added; 1 dependency removed (scrollbars)
- All instrumented tests pass on GMD emulator
