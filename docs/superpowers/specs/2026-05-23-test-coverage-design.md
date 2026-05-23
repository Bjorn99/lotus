# Automated test-coverage plan

Status: design approved 2026-05-23
Scope: 9 test files across 3 PRs. No functional behaviour change. Two production-code extractions to enable pure-JVM testing of security-critical validation logic.

## Goal

Add a safety net for three classes of future change that are currently untested:

1. **PlayerViewModel refactors** that touch privacy-toggle wiring — two 20-line gating classes are the only thing between the toggle and the network.
2. **Dependency upgrades** (Renovate PRs for Ktor, Room, Coil, Material3) — CI either catches regressions or ships with confidence.
3. **Room schema migrations** — a schema-hash mismatch bricks every upgrading user's install on launch. No manual test catches this for an already-upgraded device.

## What's already covered

| Feature | Test file | Type |
|---|---|---|
| Play/skip half-mark rule | `PlayThresholdTest.kt` | Unit |
| Backup merge math | `TrackStatsMergeTest.kt` | Unit |
| Shuffle engine | `ShuffleEngineTest.kt` | Unit |
| M3U export | `M3uExporterTest.kt` | Unit |
| Lyrics parsing | `LyricsParserTest.kt` | Unit |
| Playlist DAO | `PlaylistDaoTest.kt` | Instrumented |
| Lyrics DAO | `LyricsDaoTest.kt` | Instrumented |

7 test files total. The pattern is: pure functions get unit tests, DAOs get instrumented tests. No mocking framework — hand-written fakes where needed.

## What we are NOT testing (and why)

- **PlayerViewModel event-handler logic** — too tangled to test cleanly. Extract concerns first (Phase 3 #3), test after.
- **Compose UI screens** — separate test runner, lower ROI than layers below.
- **End-to-end backup-restore round trips** — merge math already tested; integration would confirm glue which manual testing catches.
- **Real network calls** — flaky, slow, not a regression target for our code.
- **Reproducible-build verification** — F-Droid handles externally.
- **Lint/detekt/ktlint baseline burn-down** — separate work, separate plan.

## Production-code changes (minimal, to enable testing)

Two private functions need extraction to `internal` so tests can reach pure validation logic without spinning up HTTP stacks:

### 1. Redirect validation extraction

`MusicBrainzMetadataProvider.followCoverArtRedirect()` (private, lines 192-238) does two things: validate the redirect target, then fetch from it. Extract the validation half:

```kotlin
internal fun validateCoverArtRedirect(
    location: String?,
    allowedHosts: List<String>,
): Result<String, DataError.Network> {
    if (location.isNullOrBlank()) {
        return Result.Error(DataError.Network.Unknown)
    }
    if (!location.startsWith("https://")) {
        return Result.Error(DataError.Network.Unknown)
    }
    val host = Url(location).host
    if (allowedHosts.none { host == it || host.endsWith(".$it") }) {
        return Result.Error(DataError.Network.Unknown)
    }
    return Result.Success(location)
}
```

The hosting method calls this, then fetches on success. The extraction also fixes a substring-matching bug: `host.contains("archive.org")` matches `archive.org.evil.com` and `myarchive.org`. The replacement uses exact match or domain-suffix match.

The `ia800`-`ia905` entries are removed from the allow-list — they are hostnames of Internet Archive cluster nodes that are all subdomains of `archive.org`, so the corrected `archive.org` entry already covers them.

### 2. Response-size validation extraction

The `HttpResponseValidator` lambda at `PlayerModule.kt:137-147` reads `response.contentLength()` and compares to a cap. Extract for testability:

```kotlin
internal fun validateResponseSize(contentLength: Long?, maxBytes: Long) {
    if (contentLength != null && contentLength > maxBytes) {
        throw IOException(
            "Refusing response: declared Content-Length $contentLength exceeds cap of $maxBytes"
        )
    }
}
```

The `HttpResponseValidator` block calls this. Tests pass in `Long?` and `Long` directly — no Ktor types needed.

## Test files

### PR 1 — Pure-JVM tests (6 files)

All under `app/src/test/java/com/dn0ne/player/app/`. No Android framework, no emulator. Run with existing `testDebugUnitTest` CI step.

**GatedLyricsProviderTest** — `data/remote/lyrics/`
- Fake delegate that tracks whether it was called and throws if called when it shouldn't be
- `isEnabled = { false }` → returns `NotFound`, delegate untouched
- `isEnabled = { true }` → delegate called, result passed through
- `postLyrics` always delegates (not gated)

**GatedMetadataProviderTest** — `data/remote/metadata/`
- Same pattern as lyrics gating test
- `isEnabled = { false }` → `searchMetadata` returns `NoInternet`, `getCoverArtBytes` returns `NoInternet`
- `isEnabled = { true }` → delegate called for both methods

**ChainLyricsProviderTest** — `data/remote/lyrics/`
- Fake provider that can be configured to return success, NotFound, or BadRequest per call
- First provider success → returned immediately, second never called
- First NotFound + second Success → second's result returned
- All NotFound → returns NotFound with last error preserved
- Any BadRequest → short-circuits chain immediately (doesn't ask next provider)
- Empty provider list → `IllegalArgumentException` from `require()` in init

**CoverArtRedirectTest** — `data/remote/metadata/`
- Tests `validateCoverArtRedirect()` directly — pure function, no fakes
- HTTPS archive.org URL → success, URL returned
- HTTP scheme → rejected
- Missing/blank Location → rejected
- Non-allow-listed host → rejected
- Subdomain of allow-listed host (e.g. `ia800.us.archive.org`) → allowed
- Substring-attack host (`archive.org.evil.com`) → rejected
- Malformed URL → rejected (wraps `Url()` parse exception)

**ResponseSizeCapTest** — `di/`
- Tests `validateResponseSize()` directly
- Content-Length at exactly cap (5,242,880) → no throw
- Content-Length at cap + 1 → `IOException` thrown
- Null Content-Length → no throw (pass-through, timeout-based protection)

**BackupDataJsonShapeTest** — `data/backup/`
- v1-schema JSON (no `trackStats` field) deserializes with `trackStats == emptyList()`
- v2 backup round-trips: serialize → deserialize → fields equal
- JSON property names are byte-stable (no accidental `@SerialName` drift): verify serialized JSON contains `"schemaVersion"`, `"exportedAt"`, `"playlists"`, `"lovedTracks"`, `"trackStats"`
- Nullable fields (`firstPlayedAt`, `lastPlayedAt`) serialize correctly when null vs when set

### PR 2 — Instrumented tests (3 files)

All under `app/src/androidTest/java/com/dn0ne/player/app/data/db/`. Follow the `PlaylistDaoTest` / `LyricsDaoTest` pattern: in-memory Room database, `runBlocking`, `@RunWith(AndroidJUnit4::class)`.

**LovedTrackDaoTest**
- insert → `isLoved` returns true
- delete → `isLoved` returns false
- `observeUris` Flow emits updated set on insert/delete
- Insert same URI twice (idempotent — `OnConflictStrategy.IGNORE`)
- `observeUris` ordering: most recently added first

**TrackStatsDaoTest**
- `recordPlay` increments `play_count`, sets `first_played_at` and `last_played_at`
- Second `recordPlay` increments count, updates `last_played_at`, leaves `first_played_at` alone
- `recordSkip` increments `skip_count`, does NOT touch play timestamps
- `addListenedMs` sums correctly across multiple calls
- `observeTopByPlayCount(limit)` returns N rows ordered by play_count DESC
- `observeRecentlyPlayed(limit)` returns rows with non-null `last_played_at`, ordered by that column DESC
- `observeAll` Flow emits on insert/update
- `upsertReplacing` overwrites existing row

**LotusDatabaseMigrationTest**
- Uses Room's `MigrationTestHelper` (already in dependencies via `androidx.room.testing`)
- Create v1 schema DB → run `MIGRATION_1_2` → `loved_tracks` table exists with correct columns
- Create v2 schema DB → run `MIGRATION_2_3` → `track_stats` table exists with correct columns
- Room's `validateMigration()` catches schema-hash mismatches automatically
- Migration from v1 → v3 (both migrations applied in sequence) produces the same schema as a fresh v3 create

### PR 3 — Wire GMD into CI

Add `managedDevices` block to `app/build.gradle.kts`:

```kotlin
testOptions {
    managedDevices {
        devices {
            pixel6Api34(com.android.build.api.dsl.ManagedVirtualDevice) {
                device = "Pixel 6"
                apiLevel = 34
                systemImageSource = "aosp"
            }
        }
    }
}
```

Add CI step after "Unit tests":

```yaml
- name: Instrumented tests
  run: ./gradlew pixel6Api34DebugAndroidTest -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

Expected: ~90s boot + test execution, ~2 min total added to CI. Free on GitHub runners.

## Phased rollout

| PR | Contents | Version tag | CI impact |
|----|----------|-------------|-----------|
| 1 | 6 pure-JVM test files + 2 extractions | v1.5.9 | Zero — slots into existing `testDebugUnitTest` |
| 2 | 3 instrumented test files | v1.5.10 | Zero — added tests, no CI change |
| 3 | GMD CI wiring | v1.5.11 | +~2 min per PR, new "Instrumented tests" check |

Each PR is independently reviewable and revertible. Any one PR landing improves coverage immediately.

## What success looks like

- A Renovate Ktor/Room/Coil bump either passes the new tests (ship) or fails (you know what broke)
- A `PlayerViewModel` refactor that mis-wires the privacy toggle is caught by `GatedLyricsProviderTest` / `GatedMetadataProviderTest`
- An entity-field change that drifts from migration SQL fails `LotusDatabaseMigrationTest` on PR — fixed before merge, zero bricked installs
- New features continue the pattern: pure logic gets unit tests, DAOs get instrumented tests

## Deliberately deferred

- `PlayerViewModel` tests — extract concerns first, test after
- Compose UI tests — separate plan, lower ROI
- Hostile-input fuzzing on the redirect validator — the 7 test cases cover the known attack surface; fuzzing is a follow-up if the allow-list grows
- Dynamic allow-list (reading hosts from a config file) — adds complexity without a demonstrated need; the static list has been stable for years
