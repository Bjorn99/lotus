# Implementation plan: code quality + battery optimization

Branch: `nightly` (local only, never pushed to GitHub)
Spec: `docs/superpowers/specs/2026-05-23-code-quality-optimization-design.md`
Status: plan written 2026-05-23. Implementation NOT started (deferred).

## Execution order

Battery fixes first (highest user impact), then code health from lowest
risk to highest, then APK size tweaks last. Each batch must pass CI and
be tested on a real device before the next batch begins.

---

## Batch 1 — Battery fixes

### Step 1.1: Relax position polling (50ms → 200ms)

- [ ] Open `app/src/main/java/com/dn0ne/player/app/presentation/PlayerViewModel.kt`
- [ ] Find `startPositionUpdate()` method, locate `delay(50)`
- [ ] Change to `delay(200)`
- [ ] **Verify:** Play a track, watch the seek bar — should move smoothly
  with no visible jitter. Check position text updates.
- [ ] **Verify:** CI green

### Step 1.2: Replace MediaStore poll with ContentObserver

- [ ] Open `PlayerViewModel.kt`
- [ ] In the `init` block, locate the `while(true) { getTracks(); delay(5000L) }` loop
- [ ] Remove the loop
- [ ] Add a `ContentObserver` field that re-queries tracks on change
- [ ] Register it on `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` in init
- [ ] Unregister in `onCleared()` (ViewModel's cleanup method)
- [ ] Add a one-shot initial load on ViewModel creation (replaces the
  first iteration of the old loop)
- [ ] **Verify:** Add/delete a music file via file manager. Confirm the
  tracks list updates within a few seconds.
- [ ] **Verify:** CI green, all instrumented tests pass

### Step 1.3: Enable ExoPlayer audio offload

- [ ] Open `PlaybackService.kt`
- [ ] After `trackSelector` is created, call `setParameters()` with
  audio offload preferences (see spec section 1.3)
- [ ] Set `setIsGaplessSupportRequired(true)` — this app cares about
  gapless playback
- [ ] Set `setIsSpeedChangeSupportRequired(false)` — we don't change
  playback speed
- [ ] **Verify:** Play through a gapless album (e.g., Dark Side of the Moon).
  Confirm no gaps between tracks.
- [ ] **Verify:** Play an MP3, FLAC, AAC file. All should play normally.
- [ ] **Verify:** If possible, run Battery Historian and confirm "CPU Running"
  shows sparse bars during screen-off playback (DSP handling it).
- [ ] **Risk mitigation:** Test on at least 2 physical devices (different
  manufacturers if possible). If offload breaks on a device, it's likely
  DSP firmware — either blacklist that manufacturer or make offload
  opt-in via a setting.

---

## Batch 2 — Code health (lowest risk first)

### Step 2.1: Remove dead catch blocks

- [ ] Open `core/data/MusicScanner.kt`
- [ ] Delete `catch (e: java.lang.Exception)` blocks at lines ~97 and ~156
  (the Kotlin compiler warns these are unreachable)
- [ ] **Verify:** Compiles. CI green.

### Step 2.2: Split PlayerViewModel.onEvent() into handler functions

- [ ] Open `PlayerViewModel.kt`, locate `onEvent()` method
- [ ] Categorize all event types into groups:
  - Playback events (play, pause, skip, seek, shuffle mode)
  - Playlist events (create, delete, rename, add tracks, reorder)
  - UI events (sheet open/close, tab switch, sort change)
  - Navigation events (screen routes)
  - Settings events (network toggle, audio focus toggle)
  - Lyrics events (fetch, post)
  - Stats/loved-track events
- [ ] Extract each group into a private function
- [ ] The `onEvent()` body becomes a single `when` statement dispatching
  to the handler functions
- [ ] **Verify:** Compiles. CI green. No logic changes — this is pure
  code movement.

### Step 2.3: Unify GatedLyricsProvider and GatedMetadataProvider

- [ ] Create a new file: `app/data/remote/GatedProvider.kt`
- [ ] Implement generic `GatedProvider<T>` as described in spec section 2.3
- [ ] Update `GatedMetadataProvider` to delegate to `GatedProvider<MetadataProvider>`
- [ ] Update `GatedLyricsProvider` to delegate to `GatedProvider<LyricsProvider>`
- [ ] **Alternative:** If the two existing files become one-liners, delete
  them and update the Koin module to wire `GatedProvider` directly.
- [ ] **Verify:** CI green. All existing gating tests pass.
  Network-off → NoInternet. Network-on → delegates work.
- [ ] **Verify:** Toggle network setting on device, confirm lyrics and
  metadata lookups still gate correctly.

### Step 2.4: Extract TabContent composable from PlayerScreen.kt

- [ ] Open `PlayerScreen.kt`
- [ ] Study all 6 tab implementations (Tracks, Playlists, Albums, Artists,
  Genres, Folders) — catalogue what varies between them:
  - Item type and data source
  - Grid/list mode behavior
  - Selection mode behavior
  - Sort/filter options
  - Empty state messages
- [ ] Create a `TabContent` composable in a new file:
  `app/presentation/components/tabs/TabContent.kt`
- [ ] The composable takes a `TabConfig` data class with all variation points
- [ ] Migrate one tab first (e.g., Playlists — simplest), verify it works
- [ ] Migrate the remaining 5 tabs one at a time, verifying each
- [ ] Delete the old per-tab implementations
- [ ] **Note:** The Folders tab may need special handling (folder tree
  navigation). If it doesn't fit the generic pattern cleanly, keep it
  separate — don't force-fit it.
- [ ] **Verify:** Every tab renders correctly in grid, list, and selection
  modes. Sorting and filtering work. Empty states show correctly.
- [ ] **Verify:** CI green. Instrumented tests pass.

### Step 2.5: Review and potentially inline unused interfaces

- [ ] For each of the 8 single-implementation interfaces, grep for test
  usages (`grep -r "Mock.*Repository\|Fake.*Repository\|mock<.*Repository>"`)
- [ ] If an interface has zero test usages: inline the implementation into
  the interface file, delete the impl file, update Koin module
- [ ] If an interface has test usages: leave it alone
- [ ] **Verify:** CI green. All tests pass.

---

## Batch 3 — APK size (last, low priority)

### Step 3.1: Enable shrinkResources

- [ ] Open `app/build.gradle.kts`
- [ ] In `buildTypes.release`, change `isShrinkResources = false` to `true`
- [ ] Build release APK: `./gradlew assembleRelease`
- [ ] Compare APK size before/after: `ls -la app/build/outputs/apk/release/`
- [ ] **Verify:** Install the release APK on a device. All screens load.
  No missing resources at runtime. CrunchPngs stays disabled (we have no
  large PNGs to crunch).
- [ ] **Verify:** CI green (release build succeeds).

### Step 3.2: Enable obfuscation

- [ ] Open `app/proguard-rules.pro`
- [ ] Remove the `-dontobfuscate` line
- [ ] Add keep rules if needed for Kotlin serialization:
  ```
  -keepattributes *Annotation*, InnerClasses
  -keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
  ```
- [ ] Build release APK
- [ ] **Verify:** Install on device. Backup/restore works (exercises
  Kotlin serialization). Lyrics fetch works. Metadata lookup works.
- [ ] **Verify:** `./gradlew testDebugUnitTest` passes (unit tests use
  serialization for backup format tests).

### Step 3.3: Remove scrollbars dependency

- [ ] Check current Compose BOM version supports built-in scrollbars
  (Compose Foundation 1.5+ has `LazyListState` scrollbar support)
- [ ] Replace `LazyColumnScrollbar` usages with Compose Foundation's
  `Scrollbar` or the `Modifier.scrollbar` extension
- [ ] Remove `libs.scrollbars` from `gradle/libs.versions.toml`
- [ ] Remove the implementation line from `app/build.gradle.kts`
- [ ] **Verify:** Scroll indicators appear correctly on all scrollable
  lists (playlist, tracks, albums, artists, etc.).
- [ ] **Verify:** CI green.

---

## Batch summary

| Batch | Steps | Estimated effort | Cumulative effort |
|---|---|---|---|
| 1 — Battery | 1.1, 1.2, 1.3 | 2-3 days | 2-3 days |
| 2 — Code health | 2.1 through 2.5 | 3-5 days | 5-8 days |
| 3 — APK size | 3.1, 3.2, 3.3 | 1 day | 6-9 days |

## Merge strategy

```
master (stable, tagged releases)
  └── nightly (local only, all work happens here)
       ├── Batch 1 commits → test on device → merge to master when stable
       ├── Batch 2 commits → test on device → merge to master when stable
       └── Batch 3 commits → test on device → merge to master when stable
```

Each batch should be a merged as a unit. Don't merge half a batch.
After each batch merge to master, tag a new release so the APK is shippable.
