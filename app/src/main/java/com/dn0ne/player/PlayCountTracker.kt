package com.dn0ne.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.dn0ne.player.app.data.repository.TrackStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Records per-track play/skip counts and listening-ms via a Player.Listener.
//
// Two independent paths into track_stats:
//   - addListenedMs gets called by a 10 s checkpoint while playing and at
//     the close-out of every transition. The delta is *position-progress*
//     (currentPositionMs - lastFlushedPositionMs), clamped to >= 0, so
//     replaying the same section doesn't double-count.
//   - recordPlay / recordSkip is decided once at the close-out using a
//     high-water mark (maxReachedPositionMs >= duration/2). Seeking back
//     after crossing 50 % doesn't undo the play.
//
// The tracking toggle gates DAO writes only — in-memory state keeps moving
// when off, so flipping back on doesn't flush a giant catch-up delta.
class PlayCountTracker(
    private val player: Player,
    private val repository: TrackStatsRepository,
    private val scope: CoroutineScope,
    private val isTrackingEnabled: () -> Boolean = { true },
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val checkpointIntervalMs: Long = CHECKPOINT_INTERVAL_MS,
) : Player.Listener {

    private data class Session(
        val uri: String,
        var lastFlushedPositionMs: Long = 0L,
        var maxReachedPositionMs: Long = 0L,
    )

    private var current: Session? = null
    private val window = Timeline.Window()
    private var checkpointJob: Job? = null

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // The outgoing item was finalized in onPositionDiscontinuity for
        // index-changing transitions; here we just open a fresh session.
        current = mediaItem?.mediaId?.let { Session(uri = it) }
        if (player.isPlaying && current != null) startCheckpointLoop()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (oldPosition.mediaItemIndex == newPosition.mediaItemIndex) return
        val outgoing = current ?: return
        finalizeOutgoing(
            session = outgoing,
            finalPositionMs = oldPosition.positionMs,
            mediaItemIndex = oldPosition.mediaItemIndex,
        )
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState != Player.STATE_ENDED) return
        // Queue ran out — close out the still-current track using the
        // player's own state (no transition fires for queue-end).
        val outgoing = current ?: return
        finalizeOutgoing(
            session = outgoing,
            finalPositionMs = player.currentPosition.coerceAtLeast(0L),
            mediaItemIndex = player.currentMediaItemIndex,
        )
        current = null
        stopCheckpointLoop()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (current == null) return
        if (isPlaying) startCheckpointLoop() else stopCheckpointLoop()
    }

    // Service tear-down hook — capture any progress the periodic loop hasn't.
    fun flush() {
        val session = current ?: return
        addProgressDelta(session, player.currentPosition.coerceAtLeast(0L))
        stopCheckpointLoop()
    }

    private fun finalizeOutgoing(
        session: Session,
        finalPositionMs: Long,
        mediaItemIndex: Int,
    ) {
        addProgressDelta(session, finalPositionMs)
        val durationMs = readDuration(mediaItemIndex)
        val event = classifyTransition(session.maxReachedPositionMs, durationMs)
        if (!isTrackingEnabled()) return
        val uri = session.uri
        when (event) {
            TransitionEvent.PLAY -> {
                val now = nowMs()
                scope.launch { repository.recordPlay(uri = uri, now = now) }
            }
            TransitionEvent.SKIP -> {
                scope.launch { repository.recordSkip(uri = uri) }
            }
            TransitionEvent.NONE -> Unit
        }
    }

    // Advances the session's flushed position and high-water mark, and
    // writes the position-progress delta to the repository when the toggle
    // is on. Seeking backward (delta < 0) is a no-op for everything except
    // the maxReached check, which can't go up from a backward seek anyway.
    private fun addProgressDelta(session: Session, currentPositionMs: Long) {
        if (currentPositionMs > session.maxReachedPositionMs) {
            session.maxReachedPositionMs = currentPositionMs
        }
        val delta = currentPositionMs - session.lastFlushedPositionMs
        if (delta <= 0L) return
        session.lastFlushedPositionMs = currentPositionMs
        if (!isTrackingEnabled()) return
        val uri = session.uri
        scope.launch { repository.addListenedMs(uri = uri, ms = delta) }
    }

    private fun startCheckpointLoop() {
        if (checkpointJob?.isActive == true) return
        checkpointJob = scope.launch {
            while (isActive) {
                delay(checkpointIntervalMs)
                val session = current ?: continue
                addProgressDelta(session, player.currentPosition.coerceAtLeast(0L))
            }
        }
    }

    private fun stopCheckpointLoop() {
        checkpointJob?.cancel()
        checkpointJob = null
    }

    private fun readDuration(mediaItemIndex: Int): Long {
        val timeline = player.currentTimeline
        if (mediaItemIndex < 0 || mediaItemIndex >= timeline.windowCount) return 0L
        return runCatching { timeline.getWindow(mediaItemIndex, window).durationMs }
            .getOrDefault(0L)
            .takeIf { it > 0L } ?: 0L
    }

    companion object {
        private const val CHECKPOINT_INTERVAL_MS: Long = 10_000L
    }
}
