# Test-coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 9 test files across 3 PRs plus 2 minimal production-code extractions to create a safety net for dependency upgrades, schema migrations, and privacy-toggle regressions.

**Architecture:** Two private functions are extracted to `internal` to enable pure-JVM testing of security-critical validation logic. Six unit test files follow the existing `PlayThresholdTest` pattern (JUnit, no mocking framework, hand-written fakes). Three instrumented test files follow the `PlaylistDaoTest` pattern (in-memory Room, `runBlocking`, `AndroidJUnit4`). Finally, GMD is wired into CI so instrumented tests run on every PR.

**Tech Stack:** JUnit 4, Kotlin, Room MigrationTestHelper, Gradle Managed Devices

---

## PR 1 — Pure-JVM tests + 2 extractions

### Task 1: Extract `validateResponseSize` from PlayerModule

**Files:**
- Modify: `app/src/main/java/com/dn0ne/player/app/di/PlayerModule.kt`

- [ ] **Step 1: Extract the validation into an `internal fun`**

Replace the inline `HttpResponseValidator` block at lines 137-147 with a call to a standalone function. Add the function above the `MAX_RESPONSE_BYTES` constant:

```kotlin
// Extracted so the size-cap check can be unit-tested without a Ktor engine.
// Called by the HttpResponseValidator block in the HttpClient factory below.
internal fun validateResponseSize(contentLength: Long?, maxBytes: Long) {
    if (contentLength != null && contentLength > maxBytes) {
        throw IOException(
            "Refusing response: declared Content-Length $contentLength exceeds cap of $maxBytes"
        )
    }
}
```

And change the `HttpResponseValidator` block (lines 137-147) to:

```kotlin
HttpResponseValidator {
    validateResponse { response ->
        validateResponseSize(response.contentLength(), MAX_RESPONSE_BYTES)
    }
}
```

- [ ] **Step 2: Verify the build still compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/dn0ne/player/app/di/PlayerModule.kt
git commit -m "Extract validateResponseSize for testability"
```

### Task 2: Write ResponseSizeCapTest

**Files:**
- Create: `app/src/test/java/com/dn0ne/player/app/di/ResponseSizeCapTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package com.dn0ne.player.app.di

import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class ResponseSizeCapTest {

    @Test
    fun `content length at cap passes`() {
        validateResponseSize(contentLength = 5_242_880L, maxBytes = 5_242_880L)
    }

    @Test
    fun `content length below cap passes`() {
        validateResponseSize(contentLength = 1_000_000L, maxBytes = 5_242_880L)
    }

    @Test
    fun `content length one byte over cap throws IOException`() {
        assertThrows(IOException::class.java) {
            validateResponseSize(contentLength = 5_242_881L, maxBytes = 5_242_880L)
        }
    }

    @Test
    fun `null content length passes through`() {
        validateResponseSize(contentLength = null, maxBytes = 5_242_880L)
    }

    @Test
    fun `zero content length passes`() {
        validateResponseSize(contentLength = 0L, maxBytes = 5_242_880L)
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dn0ne.player.app.di.ResponseSizeCapTest"`
Expected: BUILD SUCCESSFUL, 5 tests passed

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/dn0ne/player/app/di/ResponseSizeCapTest.kt
git commit -m "Add response-size cap unit tests"
```

### Task 3: Extract `validateCoverArtRedirect` from MusicBrainzMetadataProvider

**Files:**
- Modify: `app/src/main/java/com/dn0ne/player/app/data/remote/metadata/MusicBrainzMetadataProvider.kt`

- [ ] **Step 1: Extract the validation function and fix the host matching**

Add this function above the `followCoverArtRedirect` method (before line 192):

```kotlin
// Extracted from followCoverArtRedirect so the allow-list logic can be
// unit-tested without a Ktor engine. Validates the Location header of a
// 30x response before we follow it — this is the only redirect path in
// the app, so the validation must be strict.
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
    val host = io.ktor.http.Url(location).host
    if (allowedHosts.none { host == it || host.endsWith(".$it") }) {
        return Result.Error(DataError.Network.Unknown)
    }
    return Result.Success(location)
}
```

- [ ] **Step 2: Rewrite `followCoverArtRedirect` to use the extracted function**

Replace the validation logic in `followCoverArtRedirect` (lines 192-212) with a call to the extracted function:

```kotlin
private suspend fun followCoverArtRedirect(
    response: io.ktor.client.statement.HttpResponse,
): Result<ByteArray, DataError> {
    val validation = validateCoverArtRedirect(
        location = response.headers[HttpHeaders.Location],
        allowedHosts = listOf("archive.org"),
    )
    if (validation is Result.Error) return validation

    val location = (validation as Result.Success).data
    // ... rest of the method unchanged from line 214 onward (val final = try { ... })
}
```

Remove the `ia800`-`ia905` host entries from the old allow-list — the corrected `archive.org` entry with `endsWith(".$it")` already covers all subdomains like `ia800.us.archive.org`.

- [ ] **Step 3: Verify the build still compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dn0ne/player/app/data/remote/metadata/MusicBrainzMetadataProvider.kt
git commit -m "Extract validateCoverArtRedirect and fix host allow-list matching"
```

### Task 4: Write CoverArtRedirectTest

**Files:**
- Create: `app/src/test/java/com/dn0ne/player/app/data/remote/metadata/CoverArtRedirectTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package com.dn0ne.player.app.data.remote.metadata

import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverArtRedirectTest {

    private val allowed = listOf("archive.org")

    @Test
    fun `HTTPS archive org URL is accepted`() {
        val result = validateCoverArtRedirect(
            location = "https://archive.org/download/mbid-abc123/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Success)
        assertEquals(
            "https://archive.org/download/mbid-abc123/front.jpg",
            (result as Result.Success).data,
        )
    }

    @Test
    fun `HTTPS subdomain of archive org is accepted`() {
        val result = validateCoverArtRedirect(
            location = "https://ia800.us.archive.org/download/mbid-abc123/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Success)
    }

    @Test
    fun `HTTP scheme is rejected`() {
        val result = validateCoverArtRedirect(
            location = "http://archive.org/download/mbid-abc123/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `missing Location header is rejected`() {
        val result = validateCoverArtRedirect(
            location = null,
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `blank Location header is rejected`() {
        val result = validateCoverArtRedirect(
            location = "   ",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `non-allow-listed host is rejected`() {
        val result = validateCoverArtRedirect(
            location = "https://evil.com/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `substring attack is rejected`() {
        // archive.org.evil.com must NOT pass because it "contains" archive.org
        val result = validateCoverArtRedirect(
            location = "https://archive.org.evil.com/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `closely named domain is rejected`() {
        // myarchive.org contains "archive.org" as a substring but is not a subdomain
        val result = validateCoverArtRedirect(
            location = "https://myarchive.org/front.jpg",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.Unknown, (result as Result.Error).error)
    }

    @Test
    fun `malformed URL is rejected`() {
        // Ktor's Url() throws on malformed input — the function must not crash
        val result = validateCoverArtRedirect(
            location = "not a url at all !!!",
            allowedHosts = allowed,
        )
        assertTrue(result is Result.Error)
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dn0ne.player.app.data.remote.metadata.CoverArtRedirectTest"`
Expected: BUILD SUCCESSFUL, 9 tests passed

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/dn0ne/player/app/data/remote/metadata/CoverArtRedirectTest.kt
git commit -m "Add cover-art redirect allow-list validation tests"
```

### Task 5: Write GatedLyricsProviderTest

**Files:**
- Create: `app/src/test/java/com/dn0ne/player/app/data/remote/lyrics/GatedLyricsProviderTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package com.dn0ne.player.app.data.remote.lyrics

import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GatedLyricsProviderTest {

    // Fake that records whether it was called. Throws if getLyrics is
    // invoked — the gating test should never reach the delegate when
    // disabled, so a throw proves short-circuit happened.
    private class FakeLyricsProvider(
        var getLyricsCallCount: Int = 0,
    ) : LyricsProvider {
        override suspend fun getLyrics(track: Track): Result<Lyrics, DataError.Network> {
            getLyricsCallCount++
            return Result.Error(DataError.Network.NotFound)
        }

        override suspend fun postLyrics(
            track: Track,
            lyrics: Lyrics,
        ): Result<Unit, DataError.Network> {
            return Result.Success(Unit)
        }
    }

    private val dummyTrack = Track(
        uri = android.net.Uri.EMPTY,
        mediaItem = androidx.media3.common.MediaItem.EMPTY,
        coverArtUri = android.net.Uri.EMPTY,
        duration = 0,
        size = 0L,
        dateModified = 0L,
        data = "",
    )

    @Test
    fun `disabled gate returns NotFound without calling delegate`() = runBlocking {
        val delegate = FakeLyricsProvider()
        val gated = GatedLyricsProvider(delegate, isEnabled = { false })

        val result = gated.getLyrics(dummyTrack)

        assertEquals(0, delegate.getLyricsCallCount)
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.NotFound, (result as Result.Error).error)
    }

    @Test
    fun `enabled gate calls delegate and passes result through`() = runBlocking {
        val delegate = FakeLyricsProvider()
        val gated = GatedLyricsProvider(delegate, isEnabled = { true })

        val result = gated.getLyrics(dummyTrack)

        assertEquals(1, delegate.getLyricsCallCount)
        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.NotFound, (result as Result.Error).error)
    }

    @Test
    fun `postLyrics always delegates regardless of gate`() = runBlocking {
        val delegate = FakeLyricsProvider()
        val gated = GatedLyricsProvider(delegate, isEnabled = { false })

        val result = gated.postLyrics(dummyTrack, Lyrics(uri = "test"))

        assertTrue(result is Result.Success)
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dn0ne.player.app.data.remote.lyrics.GatedLyricsProviderTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/dn0ne/player/app/data/remote/lyrics/GatedLyricsProviderTest.kt
git commit -m "Add gated lyrics provider unit tests"
```

### Task 6: Write GatedMetadataProviderTest

**Files:**
- Create: `app/src/test/java/com/dn0ne/player/app/data/remote/metadata/GatedMetadataProviderTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package com.dn0ne.player.app.data.remote.metadata

import com.dn0ne.player.app.domain.metadata.MetadataSearchResult
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GatedMetadataProviderTest {

    private class FakeMetadataProvider(
        var searchCallCount: Int = 0,
        var coverArtCallCount: Int = 0,
    ) : MetadataProvider {
        override suspend fun searchMetadata(
            query: String,
            trackDuration: Long,
        ): Result<List<MetadataSearchResult>, DataError> {
            searchCallCount++
            return Result.Error(DataError.Network.NotFound)
        }

        override suspend fun getCoverArtBytes(
            searchResult: MetadataSearchResult,
        ): Result<ByteArray, DataError> {
            coverArtCallCount++
            return Result.Error(DataError.Network.NotFound)
        }
    }

    @Test
    fun `disabled gate returns NoInternet for searchMetadata without calling delegate`() =
        runBlocking {
            val delegate = FakeMetadataProvider()
            val gated = GatedMetadataProvider(delegate, isEnabled = { false })

            val result = gated.searchMetadata("query", 0L)

            assertEquals(0, delegate.searchCallCount)
            assertTrue(result is Result.Error)
            assertEquals(
                DataError.Network.NoInternet,
                (result as Result.Error).error,
            )
        }

    @Test
    fun `disabled gate returns NoInternet for getCoverArtBytes without calling delegate`() =
        runBlocking {
            val delegate = FakeMetadataProvider()
            val gated = GatedMetadataProvider(delegate, isEnabled = { false })

            val result = gated.getCoverArtBytes(
                MetadataSearchResult(
                    id = "id",
                    title = "title",
                    artist = "artist",
                    albumId = "albumId",
                    album = "album",
                    albumArtist = "albumArtist",
                )
            )

            assertEquals(0, delegate.coverArtCallCount)
            assertTrue(result is Result.Error)
            assertEquals(
                DataError.Network.NoInternet,
                (result as Result.Error).error,
            )
        }

    @Test
    fun `enabled gate calls delegate and passes result through`() = runBlocking {
        val delegate = FakeMetadataProvider()
        val gated = GatedMetadataProvider(delegate, isEnabled = { true })

        val result = gated.searchMetadata("query", 0L)

        assertEquals(1, delegate.searchCallCount)
        assertTrue(result is Result.Error)
        assertEquals(
            DataError.Network.NotFound,
            (result as Result.Error).error,
        )
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dn0ne.player.app.data.remote.metadata.GatedMetadataProviderTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/dn0ne/player/app/data/remote/metadata/GatedMetadataProviderTest.kt
git commit -m "Add gated metadata provider unit tests"
```

### Task 7: Write ChainLyricsProviderTest

**Files:**
- Create: `app/src/test/java/com/dn0ne/player/app/data/remote/lyrics/ChainLyricsProviderTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package com.dn0ne.player.app.data.remote.lyrics

import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChainLyricsProviderTest {

    // Fake provider that returns a pre-configured result and records
    // whether getLyrics was called — used to verify short-circuit.
    private class FakeLyricsProvider(
        private val result: Result<Lyrics, DataError.Network>,
        var callCount: Int = 0,
    ) : LyricsProvider {
        override suspend fun getLyrics(track: Track): Result<Lyrics, DataError.Network> {
            callCount++
            return result
        }

        override suspend fun postLyrics(
            track: Track,
            lyrics: Lyrics,
        ): Result<Unit, DataError.Network> = Result.Success(Unit)
    }

    private val dummyTrack = Track(
        uri = android.net.Uri.EMPTY,
        mediaItem = androidx.media3.common.MediaItem.EMPTY,
        coverArtUri = android.net.Uri.EMPTY,
        duration = 0,
        size = 0L,
        dateModified = 0L,
        data = "",
    )

    private val dummyLyrics = Lyrics(uri = "test", plain = listOf("line"))

    @Test
    fun `first provider success returns immediately`() = runBlocking {
        val first = FakeLyricsProvider(Result.Success(dummyLyrics))
        val second = FakeLyricsProvider(Result.Error(DataError.Network.NotFound))
        val chain = ChainLyricsProvider(listOf(first, second))

        val result = chain.getLyrics(dummyTrack)

        assertTrue(result is Result.Success)
        assertEquals(1, first.callCount)
        assertEquals(0, second.callCount)
    }

    @Test
    fun `first NotFound second Success returns second result`() = runBlocking {
        val first = FakeLyricsProvider(Result.Error(DataError.Network.NotFound))
        val second = FakeLyricsProvider(Result.Success(dummyLyrics))
        val chain = ChainLyricsProvider(listOf(first, second))

        val result = chain.getLyrics(dummyTrack)

        assertTrue(result is Result.Success)
        assertEquals(1, first.callCount)
        assertEquals(1, second.callCount)
    }

    @Test
    fun `all NotFound returns NotFound with last error`() = runBlocking {
        val first = FakeLyricsProvider(Result.Error(DataError.Network.NotFound))
        val second = FakeLyricsProvider(
            Result.Error(DataError.Network.ServiceUnavailable)
        )
        val chain = ChainLyricsProvider(listOf(first, second))

        val result = chain.getLyrics(dummyTrack)

        assertTrue(result is Result.Error)
        assertEquals(
            DataError.Network.ServiceUnavailable,
            (result as Result.Error).error,
        )
    }

    @Test
    fun `BadRequest short-circuits immediately`() = runBlocking {
        val first = FakeLyricsProvider(Result.Error(DataError.Network.BadRequest))
        val second = FakeLyricsProvider(Result.Success(dummyLyrics))
        val chain = ChainLyricsProvider(listOf(first, second))

        val result = chain.getLyrics(dummyTrack)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.BadRequest, (result as Result.Error).error)
        assertEquals(0, second.callCount)
    }

    @Test
    fun `BadRequest from second provider stops chain`() = runBlocking {
        val first = FakeLyricsProvider(Result.Error(DataError.Network.NotFound))
        val second = FakeLyricsProvider(Result.Error(DataError.Network.BadRequest))
        val third = FakeLyricsProvider(Result.Success(dummyLyrics))
        val chain = ChainLyricsProvider(listOf(first, second, third))

        val result = chain.getLyrics(dummyTrack)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Network.BadRequest, (result as Result.Error).error)
        assertEquals(0, third.callCount)
    }

    @Test
    fun `empty provider list throws on construction`() {
        try {
            ChainLyricsProvider(emptyList())
            // If we get here, the require() didn't fire — fail the test
            assertTrue("Expected IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `postLyrics delegates to first provider`() = runBlocking {
        val first = FakeLyricsProvider(Result.Error(DataError.Network.NotFound))
        val second = FakeLyricsProvider(Result.Success(dummyLyrics))
        val chain = ChainLyricsProvider(listOf(first, second))

        val result = chain.postLyrics(dummyTrack, dummyLyrics)

        assertTrue(result is Result.Success)
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dn0ne.player.app.data.remote.lyrics.ChainLyricsProviderTest"`
Expected: BUILD SUCCESSFUL, 7 tests passed

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/dn0ne/player/app/data/remote/lyrics/ChainLyricsProviderTest.kt
git commit -m "Add chain lyrics provider unit tests"
```

### Task 8: Write BackupDataJsonShapeTest

**Files:**
- Create: `app/src/test/java/com/dn0ne/player/app/data/backup/BackupDataJsonShapeTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package com.dn0ne.player.app.data.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDataJsonShapeTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `v1 backup without trackStats deserializes with empty list`() {
        val v1Json = """
            {
                "schemaVersion": 1,
                "exportedAt": 1717000000000,
                "appVersionName": "1.5.5-community",
                "playlists": [],
                "lovedTracks": []
            }
        """.trimIndent()

        val parsed = json.decodeFromString<BackupData>(v1Json)

        assertEquals(1, parsed.schemaVersion)
        assertEquals(1717000000000L, parsed.exportedAt)
        assertEquals(emptyList<BackupTrackStats>(), parsed.trackStats)
    }

    @Test
    fun `v2 backup round-trips faithfully`() {
        val original = BackupData(
            schemaVersion = 2,
            exportedAt = 1717000000000L,
            appVersionName = "1.5.8-community",
            playlists = listOf(
                BackupPlaylist(
                    name = "mix",
                    tracks = listOf(
                        BackupTrackRef(uri = "content://a", data = "/sdcard/a.mp3")
                    ),
                )
            ),
            lovedTracks = listOf(
                BackupTrackRef(uri = "content://b", data = "/sdcard/b.mp3")
            ),
            trackStats = listOf(
                BackupTrackStats(
                    uri = "content://c",
                    data = "/sdcard/c.mp3",
                    playCount = 5,
                    skipCount = 2,
                    firstPlayedAt = 1000L,
                    lastPlayedAt = 9000L,
                    totalListeningMs = 120_000L,
                )
            ),
        )

        val serialized = json.encodeToString(BackupData.serializer(), original)
        val deserialized = json.decodeFromString<BackupData>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `serialized field names are byte-stable`() {
        val data = BackupData(
            exportedAt = 0L,
            appVersionName = "test",
            playlists = emptyList(),
            lovedTracks = emptyList(),
        )
        val serialized = json.encodeToString(BackupData.serializer(), data)

        assertTrue(
            "Missing schemaVersion key in: $serialized",
            serialized.contains("\"schemaVersion\"")
        )
        assertTrue(
            "Missing exportedAt key in: $serialized",
            serialized.contains("\"exportedAt\"")
        )
        assertTrue(
            "Missing playlists key in: $serialized",
            serialized.contains("\"playlists\"")
        )
        assertTrue(
            "Missing lovedTracks key in: $serialized",
            serialized.contains("\"lovedTracks\"")
        )
        assertTrue(
            "Missing trackStats key in: $serialized",
            serialized.contains("\"trackStats\"")
        )
    }

    @Test
    fun `nullable timestamps serialize correctly`() {
        val withTimestamps = BackupTrackStats(
            uri = "u",
            data = "d",
            playCount = 1,
            skipCount = 0,
            firstPlayedAt = 1000L,
            lastPlayedAt = 5000L,
            totalListeningMs = 30000L,
        )
        val serializedWith = json.encodeToString(
            BackupTrackStats.serializer(), withTimestamps
        )
        assertTrue(serializedWith.contains("\"firstPlayedAt\":1000"))
        assertTrue(serializedWith.contains("\"lastPlayedAt\":5000"))

        val withoutTimestamps = BackupTrackStats(
            uri = "u",
            data = "d",
            playCount = 1,
            skipCount = 0,
            firstPlayedAt = null,
            lastPlayedAt = null,
            totalListeningMs = 0L,
        )
        val serializedWithout = json.encodeToString(
            BackupTrackStats.serializer(), withoutTimestamps
        )
        assertTrue(serializedWithout.contains("\"firstPlayedAt\":null"))
        assertTrue(serializedWithout.contains("\"lastPlayedAt\":null"))
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dn0ne.player.app.data.backup.BackupDataJsonShapeTest"`
Expected: BUILD SUCCESSFUL, 4 tests passed

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/dn0ne/player/app/data/backup/BackupDataJsonShapeTest.kt
git commit -m "Add backup JSON wire-format shape tests"
```

### Task 9: Run full unit test suite and verify PR 1 readiness

- [ ] **Step 1: Run all unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (existing + 6 new test files)

- [ ] **Step 2: Verify no production behaviour change**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

---

## PR 2 — Instrumented tests (3 files)

### Task 10: Write LovedTrackDaoTest

**Files:**
- Create: `app/src/androidTest/java/com/dn0ne/player/app/data/db/LovedTrackDaoTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package com.dn0ne.player.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LovedTrackDaoTest {

    private lateinit var db: LotusDatabase
    private lateinit var dao: LovedTrackDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, LotusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.lovedTrackDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_then_isLoved_returns_true() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://song/1", addedAt = 1000L))

        assertTrue(dao.isLoved("content://song/1"))
    }

    @Test
    fun delete_then_isLoved_returns_false() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://song/1", addedAt = 1000L))
        dao.deleteByUri("content://song/1")

        assertFalse(dao.isLoved("content://song/1"))
    }

    @Test
    fun insert_same_uri_twice_is_idempotent() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://dup", addedAt = 1000L))
        dao.insert(LovedTrackEntity(uri = "content://dup", addedAt = 2000L))

        assertTrue(dao.isLoved("content://dup"))
        // OnConflictStrategy.IGNORE means the first insert wins;
        // the second is a no-op
    }

    @Test
    fun observeUris_emits_on_insert() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://a", addedAt = 2000L))
        dao.insert(LovedTrackEntity(uri = "content://b", addedAt = 1000L))

        val uris = dao.observeUris().first()
        assertEquals(listOf("content://a", "content://b"), uris)
    }

    @Test
    fun observeUris_emits_on_delete() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://a", addedAt = 1000L))
        dao.insert(LovedTrackEntity(uri = "content://b", addedAt = 2000L))
        dao.deleteByUri("content://b")

        val uris = dao.observeUris().first()
        assertEquals(listOf("content://a"), uris)
    }

    @Test
    fun observeUris_returns_most_recently_added_first() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://first", addedAt = 1000L))
        dao.insert(LovedTrackEntity(uri = "content://second", addedAt = 2000L))
        dao.insert(LovedTrackEntity(uri = "content://third", addedAt = 3000L))

        val uris = dao.observeUris().first()
        assertEquals(
            listOf("content://third", "content://second", "content://first"),
            uris,
        )
    }
}
```

- [ ] **Step 2: Run the test on a connected device/emulator**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "com.dn0ne.player.app.data.db.LovedTrackDaoTest"`
Expected: BUILD SUCCESSFUL, 6 tests passed

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/com/dn0ne/player/app/data/db/LovedTrackDaoTest.kt
git commit -m "Add LovedTrackDao instrumented tests"
```

### Task 11: Write TrackStatsDaoTest

**Files:**
- Create: `app/src/androidTest/java/com/dn0ne/player/app/data/db/TrackStatsDaoTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package com.dn0ne.player.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackStatsDaoTest {

    private lateinit var db: LotusDatabase
    private lateinit var dao: TrackStatsDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, LotusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.trackStatsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun recordPlay_creates_row_and_sets_timestamps() = runBlocking {
        dao.recordPlay(uri = "content://a", now = 5000L)

        val row = dao.getByUri("content://a")
        assertNotNull(row)
        assertEquals(1, row!!.playCount)
        assertEquals(5000L, row.firstPlayedAt)
        assertEquals(5000L, row.lastPlayedAt)
    }

    @Test
    fun second_play_increments_count_and_updates_last_played() = runBlocking {
        dao.recordPlay(uri = "content://a", now = 5000L)
        dao.recordPlay(uri = "content://a", now = 10000L)

        val row = dao.getByUri("content://a")
        assertNotNull(row)
        assertEquals(2, row!!.playCount)
        assertEquals(5000L, row.firstPlayedAt)
        assertEquals(10000L, row.lastPlayedAt)
    }

    @Test
    fun recordSkip_increments_skip_count_does_not_touch_timestamps() = runBlocking {
        dao.recordPlay(uri = "content://a", now = 5000L)
        dao.recordSkip(uri = "content://a")

        val row = dao.getByUri("content://a")
        assertNotNull(row)
        assertEquals(1, row!!.skipCount)
        assertEquals(5000L, row.firstPlayedAt)
        assertEquals(5000L, row.lastPlayedAt)
    }

    @Test
    fun addListenedMs_sums_correctly() = runBlocking {
        dao.addListenedMs(uri = "content://a", ms = 30000L)
        dao.addListenedMs(uri = "content://a", ms = 15000L)

        val row = dao.getByUri("content://a")
        assertNotNull(row)
        assertEquals(45000L, row!!.totalListeningMs)
    }

    @Test
    fun addListenedMs_with_zero_or_negative_is_no_op() = runBlocking {
        dao.addListenedMs(uri = "content://a", ms = 0L)
        dao.addListenedMs(uri = "content://a", ms = -1L)

        assertNull(dao.getByUri("content://a"))
    }

    @Test
    fun observeTopByPlayCount_returns_ordered_by_play_count_desc() = runBlocking {
        dao.recordPlay(uri = "c", now = 1000L)
        dao.recordPlay(uri = "a", now = 1000L)
        dao.recordPlay(uri = "a", now = 2000L)
        dao.recordPlay(uri = "b", now = 1000L)
        dao.recordPlay(uri = "a", now = 3000L)
        dao.recordPlay(uri = "c", now = 2000L)

        val top = dao.observeTopByPlayCount(limit = 10).first()
        assertEquals(listOf("a", "c", "b"), top.map { it.uri })
        assertEquals(3, top[0].playCount)
        assertEquals(2, top[1].playCount)
        assertEquals(1, top[2].playCount)
    }

    @Test
    fun observeTopByPlayCount_respects_limit() = runBlocking {
        dao.recordPlay(uri = "a", now = 1000L)
        dao.recordPlay(uri = "b", now = 1000L)
        dao.recordPlay(uri = "c", now = 1000L)

        val top = dao.observeTopByPlayCount(limit = 2).first()
        assertEquals(2, top.size)
    }

    @Test
    fun observeRecentlyPlayed_returns_non_null_last_played_ordered_desc() = runBlocking {
        dao.recordPlay(uri = "a", now = 1000L)
        dao.recordPlay(uri = "b", now = 5000L)
        dao.recordPlay(uri = "c", now = 3000L)

        val recent = dao.observeRecentlyPlayed(limit = 10).first()
        assertEquals(listOf("b", "c", "a"), recent.map { it.uri })
    }

    @Test
    fun observeAll_emits_on_insert_and_update() = runBlocking {
        dao.recordPlay(uri = "a", now = 1000L)
        dao.recordPlay(uri = "b", now = 2000L)

        val all = dao.observeAll().first()
        assertEquals(2, all.size)
    }

    @Test
    fun upsertReplacing_overwrites_existing_row() = runBlocking {
        dao.recordPlay(uri = "a", now = 1000L)
        dao.upsertReplacing(
            TrackStatsEntity(
                uri = "a",
                playCount = 99,
                skipCount = 1,
                totalListeningMs = 999_000L,
                firstPlayedAt = 100L,
                lastPlayedAt = 200L,
            )
        )

        val row = dao.getByUri("a")
        assertNotNull(row)
        assertEquals(99, row!!.playCount)
        assertEquals(1, row.skipCount)
        assertEquals(999_000L, row.totalListeningMs)
        assertEquals(100L, row.firstPlayedAt)
        assertEquals(200L, row.lastPlayedAt)
    }

    @Test
    fun clearAll_removes_all_rows() = runBlocking {
        dao.recordPlay(uri = "a", now = 1000L)
        dao.recordPlay(uri = "b", now = 2000L)
        dao.clearAll()

        assertEquals(0, dao.observeAll().first().size)
    }
}
```

- [ ] **Step 2: Run the test on a connected device/emulator**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "com.dn0ne.player.app.data.db.TrackStatsDaoTest"`
Expected: BUILD SUCCESSFUL, 11 tests passed

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/com/dn0ne/player/app/data/db/TrackStatsDaoTest.kt
git commit -m "Add TrackStatsDao instrumented tests"
```

### Task 12: Write LotusDatabaseMigrationTest

**Files:**
- Create: `app/src/androidTest/java/com/dn0ne/player/app/data/db/LotusDatabaseMigrationTest.kt`

- [ ] **Step 1: Write the test file**

```kotlin
package com.dn0ne.player.app.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LotusDatabaseMigrationTest {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LotusDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate_1_to_2_creates_loved_tracks_table() {
        val db = helper.createDatabase(LotusDatabase.NAME, 1)
        // v1 schema: no loved_tracks table
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            LotusDatabase.NAME,
            2,
            true,
            MIGRATION_1_2,
        )

        // Verify the loved_tracks table exists with correct columns
        val cursor = migrated.query(
            "SELECT uri, added_at FROM loved_tracks"
        )
        assertEquals(2, cursor.columnCount)
        assertEquals("uri", cursor.getColumnName(0))
        assertEquals("added_at", cursor.getColumnName(1))
        cursor.close()
        migrated.close()
    }

    @Test
    fun migrate_2_to_3_creates_track_stats_table() {
        val db = helper.createDatabase(LotusDatabase.NAME, 2)
        // v2 schema: has loved_tracks, no track_stats
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            LotusDatabase.NAME,
            3,
            true,
            MIGRATION_2_3,
        )

        val cursor = migrated.query(
            "SELECT uri, play_count, skip_count, total_listening_ms, " +
                "first_played_at, last_played_at FROM track_stats"
        )
        assertEquals(6, cursor.columnCount)
        assertEquals("uri", cursor.getColumnName(0))
        assertEquals("play_count", cursor.getColumnName(1))
        assertEquals("skip_count", cursor.getColumnName(2))
        assertEquals("total_listening_ms", cursor.getColumnName(3))
        assertEquals("first_played_at", cursor.getColumnName(4))
        assertEquals("last_played_at", cursor.getColumnName(5))
        cursor.close()
        migrated.close()
    }

    @Test
    fun migrate_1_to_3_applies_both_migrations() {
        val db = helper.createDatabase(LotusDatabase.NAME, 1)
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            LotusDatabase.NAME,
            3,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
        )

        // loved_tracks from 1→2
        val lovedCursor = migrated.query("SELECT uri, added_at FROM loved_tracks")
        assertEquals(2, lovedCursor.columnCount)
        lovedCursor.close()

        // track_stats from 2→3
        val statsCursor = migrated.query(
            "SELECT uri, play_count, skip_count, total_listening_ms, " +
                "first_played_at, last_played_at FROM track_stats"
        )
        assertEquals(6, statsCursor.columnCount)
        statsCursor.close()

        migrated.close()
    }
}
```

But `MIGRATION_1_2` and `MIGRATION_2_3` are currently `private` in `PlayerModule.kt`. They need to become `internal` for the test to reference them.

- [ ] **Step 2: Make the migration constants accessible**

In `app/src/main/java/com/dn0ne/player/app/di/PlayerModule.kt`, change `private val MIGRATION_1_2` and `private val MIGRATION_2_3` to `internal val`:

```kotlin
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    // ... unchanged
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    // ... unchanged
}
```

- [ ] **Step 3: Verify the build still compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run the migration tests on a connected device/emulator**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "com.dn0ne.player.app.data.db.LotusDatabaseMigrationTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed

- [ ] **Step 5: Commit**

```bash
git add app/src/androidTest/java/com/dn0ne/player/app/data/db/LotusDatabaseMigrationTest.kt
git add app/src/main/java/com/dn0ne/player/app/di/PlayerModule.kt
git commit -m "Add Room migration tests and expose migration constants"
```

### Task 13: Run full instrumented test suite and verify PR 2 readiness

- [ ] **Step 1: Run all instrumented tests**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: BUILD SUCCESSFUL, all tests pass (existing DAO tests + 3 new test files)

- [ ] **Step 2: Verify APK still builds**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

---

## PR 3 — GMD CI wiring

### Task 14: Add managedDevices to build.gradle.kts

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add the managedDevices block**

Add inside the `android` block, after the `lint` block (around line 149, before the closing `}` of `android`):

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

- [ ] **Step 2: Verify the build config still parses**

Run: `./gradlew :app:tasks --group "verification" 2>&1 | grep -i "pixel\|managed\|AndroidTest"`
Expected: Lists `pixel6Api34DebugAndroidTest` among available tasks

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "Add Gradle Managed Devices configuration for CI instrumented tests"
```

### Task 15: Add instrumented test step to CI workflow

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: Add the instrumented test step**

Add after the "Unit tests" step (after line 41, before "Assemble debug APK"):

```yaml
      - name: Instrumented tests
        run: ./gradlew pixel6Api34DebugAndroidTest -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "Wire Gradle Managed Devices instrumented tests into CI"
```

### Task 16: Verify CI workflow completeness

- [ ] **Step 1: Review the final CI file structure**

Run: `grep "^      - name:" .github/workflows/ci.yml`
Expected output shows steps in order:
```
      - name: Validate Gradle wrapper
      - name: Set up JDK 17
      - name: Set up Gradle
      - name: Gradle wrapper executable
      - name: Unit tests
      - name: Instrumented tests
      - name: Dump unit test reports on failure
      - name: Assemble debug APK
      - name: Assemble release APK
      - name: Android lint
      - name: Kotlin static analysis (detekt + ktlint)
      ... (artifact upload steps)
```

- [ ] **Step 2: Run unit tests one final time**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass

---

## Self-Review

### Spec coverage
- [x] GatedLyricsProviderTest → Tasks 5
- [x] GatedMetadataProviderTest → Tasks 6
- [x] ChainLyricsProviderTest → Tasks 7
- [x] CoverArtRedirectTest → Tasks 4
- [x] ResponseSizeCapTest → Tasks 2
- [x] BackupDataJsonShapeTest → Tasks 8
- [x] LovedTrackDaoTest → Tasks 10
- [x] TrackStatsDaoTest → Tasks 11
- [x] LotusDatabaseMigrationTest → Tasks 12
- [x] validateResponseSize extraction → Tasks 1
- [x] validateCoverArtRedirect extraction + fix → Tasks 3
- [x] GMD build config → Tasks 14
- [x] GMD CI step → Tasks 15
- [x] Visibility change for MIGRATION constants → Tasks 12 step 2

### Placeholder scan
No TBDs, TODOs, or vague directives. All code is fully specified.

### Type consistency
- `validateResponseSize(contentLength: Long?, maxBytes: Long)` — consistent across Tasks 1 and 2
- `validateCoverArtRedirect(location: String?, allowedHosts: List<String>)` — consistent across Tasks 3 and 4
- `MIGRATION_1_2`, `MIGRATION_2_3` — changed from `private val` to `internal val` in Task 12 step 2, referenced by name in Task 12 step 1
- `LotusDatabase.NAME` — used in Task 12, matches the companion object in `LotusDatabase.kt`
- `FakeLyricsProvider`, `FakeMetadataProvider` — each task defines its own fake, no cross-task coupling
