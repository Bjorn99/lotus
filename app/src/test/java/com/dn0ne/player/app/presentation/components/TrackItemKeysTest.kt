package com.dn0ne.player.app.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackItemKeysTest {

    @Test
    fun `distinct uris keep their uri as key`() {
        val uris = listOf("content://media/1", "content://media/2", "content://media/3")

        assertEquals(uris, uniqueTrackKeys(uris))
    }

    @Test
    fun `repeated uri yields unique keys`() {
        val uris = listOf("content://media/1", "content://media/2", "content://media/1")

        val keys = uniqueTrackKeys(uris)

        assertEquals(uris.size, keys.toSet().size)
    }

    @Test
    fun `repeated uri is suffixed by occurrence`() {
        val uris = listOf("a", "a", "a")

        assertEquals(listOf("a", "a\u00001", "a\u00002"), uniqueTrackKeys(uris))
    }

    @Test
    fun `every key is unique for a heavily duplicated list`() {
        val uris = List(50) { "content://media/${it % 5}" }

        val keys = uniqueTrackKeys(uris)

        assertEquals(uris.size, keys.size)
        assertEquals(uris.size, keys.toSet().size)
    }

    @Test
    fun `reordering the list preserves the set of keys`() {
        val uris = listOf("a", "b", "a", "c", "a")
        val reordered = listOf("c", "a", "a", "b", "a")

        assertEquals(uniqueTrackKeys(uris).toSet(), uniqueTrackKeys(reordered).toSet())
    }

    @Test
    fun `keys are stable across repeated calls`() {
        val uris = listOf("a", "b", "a")

        assertEquals(uniqueTrackKeys(uris), uniqueTrackKeys(uris))
    }

    @Test
    fun `empty list yields no keys`() {
        assertTrue(uniqueTrackKeys(emptyList()).isEmpty())
    }

    @Test
    fun `single track yields its uri`() {
        assertEquals(listOf("only"), uniqueTrackKeys(listOf("only")))
    }

    @Test
    fun `uris containing punctuation stay unique when repeated`() {
        val uris = listOf("file:///music/a b#1.mp3", "file:///music/a b.mp3", "file:///music/a b.mp3")

        val keys = uniqueTrackKeys(uris)

        assertEquals(uris.size, keys.toSet().size)
    }
}
