package com.dn0ne.player.app.data.replaygain

import com.dn0ne.player.app.data.db.ReplayGainCacheDao
import com.dn0ne.player.app.domain.replaygain.TagState
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap

interface ReplayGainSource {
    /** Null when MediaStore no longer knows the file. */
    suspend fun fileInfo(mediaId: String): MediaFileInfo?

    /** [TagState.Unknown] on any failure, so the next play retries. */
    suspend fun readTags(mediaId: String, extension: String): TagState
}

/**
 * Tag state per media id: memory first, then the Room cache, then the file. The audio
 * thread only ever calls [stateFor], which never blocks.
 */
class ReplayGainStore(
    private val source: ReplayGainSource,
    private val dao: ReplayGainCacheDao,
    private val onError: (String, Throwable) -> Unit = { _, _ -> },
) {
    // The state together with the MediaStore mtime/size it was read at, so a file
    // retagged while the service is alive is noticed without a restart.
    private data class Loaded(val state: TagState, val info: MediaFileInfo)

    private val states = ConcurrentHashMap<String, Loaded>()
    private val loading: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun stateFor(mediaId: String?): TagState =
        mediaId?.let { states[it]?.state } ?: TagState.Unknown

    suspend fun ensureLoaded(mediaId: String) {
        if (!loading.add(mediaId)) return
        try {
            // One MediaStore query per call. Callers are transitions and settings
            // changes, never the audio thread.
            val info = source.fileInfo(mediaId) ?: return
            if (states[mediaId]?.info == info) return
            val cached = dao.get(mediaId)
            val state = if (cached != null && cached.isFreshFor(info)) {
                cached.toTagState()
            } else {
                source.readTags(mediaId, info.extension).also { read ->
                    read.toCacheEntity(mediaId, info)?.let { dao.upsert(it) }
                }
            }
            if (state != TagState.Unknown) states[mediaId] = Loaded(state, info)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            // A cache failure must never stop playback: the track just plays at unity.
            onError(mediaId, t)
        } finally {
            loading.remove(mediaId)
        }
    }
}
