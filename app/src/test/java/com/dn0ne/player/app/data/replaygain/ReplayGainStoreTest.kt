package com.dn0ne.player.app.data.replaygain

import com.dn0ne.player.app.data.db.ReplayGainCacheDao
import com.dn0ne.player.app.data.db.ReplayGainCacheEntity
import com.dn0ne.player.app.domain.replaygain.GainPeak
import com.dn0ne.player.app.domain.replaygain.ReplayGainTags
import com.dn0ne.player.app.domain.replaygain.TagState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ReplayGainStoreTest {

    private val id = "content://media/external/audio/media/1"
    private val info = MediaFileInfo(dateModified = 100, size = 5000, extension = "flac")
    private val tagged = TagState.Tagged(ReplayGainTags(GainPeak(-7f, 0.88f), null))

    private class FakeSource(
        var info: MediaFileInfo?,
        var result: TagState,
    ) : ReplayGainSource {
        var reads = 0
        override suspend fun fileInfo(mediaId: String) = info
        override suspend fun readTags(mediaId: String, extension: String): TagState {
            reads++
            return result
        }
    }

    private class FakeDao(var row: ReplayGainCacheEntity? = null, val failing: Boolean = false) : ReplayGainCacheDao {
        val writes = mutableListOf<ReplayGainCacheEntity>()
        override suspend fun get(uri: String): ReplayGainCacheEntity? {
            if (failing) throw IOException("disk")
            return row
        }
        override suspend fun upsert(entity: ReplayGainCacheEntity) {
            if (failing) throw IOException("disk")
            writes += entity
            row = entity
        }
    }

    @Test
    fun `nothing loaded is unknown`() {
        val store = ReplayGainStore(FakeSource(info, tagged), FakeDao())
        assertEquals(TagState.Unknown, store.stateFor(id))
        assertEquals(TagState.Unknown, store.stateFor(null))
    }

    @Test
    fun `first load reads the file and caches the result`() = runBlocking {
        val source = FakeSource(info, tagged)
        val dao = FakeDao()
        val store = ReplayGainStore(source, dao)
        store.ensureLoaded(id)
        assertEquals(tagged, store.stateFor(id))
        assertEquals(1, source.reads)
        assertEquals(tagged.toCacheEntity(id, info), dao.writes.single())
    }

    @Test
    fun `fresh cache row skips the file read`() = runBlocking {
        val source = FakeSource(info, TagState.Untagged)
        val store = ReplayGainStore(source, FakeDao(row = tagged.toCacheEntity(id, info)))
        store.ensureLoaded(id)
        assertEquals(tagged, store.stateFor(id))
        assertEquals(0, source.reads)
    }

    @Test
    fun `stale cache row is re-read and replaced`() = runBlocking {
        val source = FakeSource(info, tagged)
        val dao = FakeDao(row = TagState.Untagged.toCacheEntity(id, info.copy(size = 1)))
        val store = ReplayGainStore(source, dao)
        store.ensureLoaded(id)
        assertEquals(tagged, store.stateFor(id))
        assertEquals(1, source.reads)
    }

    @Test
    fun `untagged files are cached so they are not copied again`() = runBlocking {
        val source = FakeSource(info, TagState.Untagged)
        val dao = FakeDao()
        ReplayGainStore(source, dao).ensureLoaded(id)
        val second = ReplayGainStore(source, dao)
        second.ensureLoaded(id)
        assertEquals(TagState.Untagged, second.stateFor(id))
        assertEquals(1, source.reads)
    }

    @Test
    fun `a failed read is not cached and is retried next time`() = runBlocking {
        val source = FakeSource(info, TagState.Unknown)
        val dao = FakeDao()
        val store = ReplayGainStore(source, dao)
        store.ensureLoaded(id)
        assertEquals(TagState.Unknown, store.stateFor(id))
        assertTrue(dao.writes.isEmpty())
        source.result = tagged
        store.ensureLoaded(id)
        assertEquals(tagged, store.stateFor(id))
    }

    @Test
    fun `a vanished file stays unknown`() = runBlocking {
        val source = FakeSource(info = null, result = tagged)
        val store = ReplayGainStore(source, FakeDao())
        store.ensureLoaded(id)
        assertEquals(TagState.Unknown, store.stateFor(id))
        assertEquals(0, source.reads)
    }

    @Test
    fun `a database failure leaves the track unknown and is reported`() = runBlocking {
        val errors = mutableListOf<Throwable>()
        val store = ReplayGainStore(FakeSource(info, tagged), FakeDao(failing = true)) { _, t -> errors += t }
        store.ensureLoaded(id)
        assertEquals(TagState.Unknown, store.stateFor(id))
        assertEquals(1, errors.size)
    }

    @Test
    fun `a loaded track is not read again`() = runBlocking {
        val source = FakeSource(info, tagged)
        val store = ReplayGainStore(source, FakeDao())
        store.ensureLoaded(id)
        store.ensureLoaded(id)
        assertEquals(1, source.reads)
    }
}
