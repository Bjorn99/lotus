package com.dn0ne.player.app.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDataJsonShapeTest {

    // Matches the production Json config in BackupManager, plus strict
    // unknown-key rejection so we catch schema drift.
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    @Test
    fun `v1 schema with no trackStats field deserializes with empty trackStats list`() {
        val v1Json = """
            {
              "schemaVersion": 1,
              "exportedAt": 1740500000000,
              "appVersionName": "1.5.5",
              "playlists": [],
              "lovedTracks": []
            }
        """.trimIndent()

        val data = json.decodeFromString<BackupData>(v1Json)

        assertEquals(1, data.schemaVersion)
        assertEquals(1740500000000L, data.exportedAt)
        assertEquals("1.5.5", data.appVersionName)
        assertEquals(emptyList<BackupTrackStats>(), data.trackStats)
    }

    @Test
    fun `v2 backup round-trips fields unchanged`() {
        val original = BackupData(
            schemaVersion = 2,
            exportedAt = 1740500000000L,
            appVersionName = "1.5.6",
            playlists = listOf(
                BackupPlaylist(
                    name = "My Playlist",
                    tracks = listOf(
                        BackupTrackRef(uri = "content://media/external/audio/1", data = "/sdcard/Music/a.mp3"),
                        BackupTrackRef(uri = "content://media/external/audio/2", data = "/sdcard/Music/b.mp3"),
                    ),
                ),
            ),
            lovedTracks = listOf(
                BackupTrackRef(uri = "content://media/external/audio/1", data = "/sdcard/Music/a.mp3"),
            ),
            trackStats = listOf(
                BackupTrackStats(
                    uri = "content://media/external/audio/1",
                    data = "/sdcard/Music/a.mp3",
                    playCount = 5,
                    skipCount = 1,
                    firstPlayedAt = 1740490000000L,
                    lastPlayedAt = 1740500000000L,
                    totalListeningMs = 300_000L,
                ),
            ),
        )

        val serialized = json.encodeToString(BackupData.serializer(), original)
        val deserialized = json.decodeFromString<BackupData>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `JSON property names are byte-stable`() {
        val data = BackupData(
            exportedAt = 1740500000000L,
            appVersionName = "1.5.6",
            playlists = emptyList(),
            lovedTracks = emptyList(),
            trackStats = emptyList(),
        )

        val serialized = json.encodeToString(BackupData.serializer(), data)

        assertTrue("Missing schemaVersion key", """"schemaVersion"""" in serialized)
        assertTrue("Missing exportedAt key", """"exportedAt"""" in serialized)
        assertTrue("Missing playlists key", """"playlists"""" in serialized)
        assertTrue("Missing lovedTracks key", """"lovedTracks"""" in serialized)
        assertTrue("Missing trackStats key", """"trackStats"""" in serialized)
    }

    @Test
    fun `nullable timestamps serialize as null when absent and as number when set`() {
        val statsWithNulls = BackupTrackStats(
            uri = "content://test",
            data = "/sdcard/test.mp3",
            playCount = 1,
            skipCount = 0,
            firstPlayedAt = null,
            lastPlayedAt = null,
            totalListeningMs = 0L,
        )

        val nullsJson = json.encodeToString(BackupTrackStats.serializer(), statsWithNulls)
        val nullsElement = json.parseToJsonElement(nullsJson).jsonObject

        // With encodeDefaults=true, null values are serialized as explicit JSON null
        assertTrue(nullsElement["firstPlayedAt"] is JsonNull)
        assertTrue(nullsElement["lastPlayedAt"] is JsonNull)

        val statsWithTimestamps = BackupTrackStats(
            uri = "content://test",
            data = "/sdcard/test.mp3",
            playCount = 1,
            skipCount = 0,
            firstPlayedAt = 1740490000000L,
            lastPlayedAt = 1740500000000L,
            totalListeningMs = 120_000L,
        )

        val timestampsJson = json.encodeToString(BackupTrackStats.serializer(), statsWithTimestamps)
        val timestampsElement = json.parseToJsonElement(timestampsJson).jsonObject

        assertNotNull(timestampsElement["firstPlayedAt"])
        assertEquals(1740490000000L, timestampsElement["firstPlayedAt"]!!.jsonPrimitive.long)
        assertNotNull(timestampsElement["lastPlayedAt"])
        assertEquals(1740500000000L, timestampsElement["lastPlayedAt"]!!.jsonPrimitive.long)
    }
}
