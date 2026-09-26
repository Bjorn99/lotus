package com.dn0ne.player.app.data.replaygain

import com.dn0ne.player.app.data.db.ReplayGainCacheEntity
import com.dn0ne.player.app.domain.replaygain.GainPeak
import com.dn0ne.player.app.domain.replaygain.ReplayGainTags
import com.dn0ne.player.app.domain.replaygain.TagState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayGainCacheMappingTest {

    private val info = MediaFileInfo(dateModified = 1_700_000_000, size = 1234, extension = "flac")

    @Test
    fun `tagged state round trips through the entity`() {
        val state = TagState.Tagged(ReplayGainTags(GainPeak(-7f, 0.88f), GainPeak(-6f, null)))
        assertEquals(state, state.toCacheEntity("u", info)!!.toTagState())
    }

    @Test
    fun `album-only state round trips`() {
        val state = TagState.Tagged(ReplayGainTags(null, GainPeak(-6f, 0.9f)))
        assertEquals(state, state.toCacheEntity("u", info)!!.toTagState())
    }

    @Test
    fun `untagged is cached as an all-null row`() {
        val row = TagState.Untagged.toCacheEntity("u", info)!!
        assertEquals(ReplayGainCacheEntity("u", info.dateModified, info.size), row)
        assertEquals(TagState.Untagged, row.toTagState())
    }

    @Test
    fun `unknown is never cached`() {
        assertNull(TagState.Unknown.toCacheEntity("u", info))
    }

    @Test
    fun `row is fresh only while mtime and size match`() {
        val row = TagState.Untagged.toCacheEntity("u", info)!!
        assertTrue(row.isFreshFor(info))
        assertFalse(row.isFreshFor(info.copy(size = 1235)))
        assertFalse(row.isFreshFor(info.copy(dateModified = 1_700_000_001)))
    }
}
