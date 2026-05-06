package com.dn0ne.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.dn0ne.player.app.data.repository.TrackStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Tracks per-track play/skip counts and total listening time. The two paths
// are independent on purpose:
//   - Listening time accumulates as wallclock during playing periods and is
//     flushed on pause / track change / queue end.
//   - Play vs skip is a single position read at the moment of transition,
//     using oldPosition.positionMs / outgoing track duration ≥ 0.5.
// This keeps total_listening_ms owned by one path so transitions never
// double-count what checkpoints already wrote.
class PlayCountTracker(
    private val player: Player,
    private val repository: TrackStatsRepository,
    private val scope: CoroutineScope,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) : Player.Listener {

    private data class Session(val uri: String, var startedAt: Long?)

    private var current: Session? = null
    private val window = Timeline.Window()

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // onPositionDiscontinuity has already finalized the outgoing track
        // for index-changing transitions. Open a fresh session for the new
        // item; if the player is already playing, start the listening clock.
        current = mediaItem?.mediaId?.let {
            Session(uri = it, startedAt = if (player.isPlaying) nowMs() else null)
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (oldPosition.mediaItemIndex == newPosition.mediaItemIndex) return
        val outgoing = current ?: return
        flushListening(outgoing)
        outgoing.startedAt = null

        val durationMs = readDuration(oldPosition.mediaItemIndex)
        val isPlay = if (durationMs != null && durationMs > 0) {
            oldPosition.positionMs.toDouble() / durationMs >= 0.5
        } else {
            // Unknown duration (streaming, unfinished metadata): fall back
            // to "did we listen for at least 30s." Local audio shouldn't hit
            // this branch.
            oldPosition.positionMs >= 30_000L
        }

        val uri = outgoing.uri
        val now = nowMs()
        scope.launch { repository.recordEvent(uri = uri, isPlay = isPlay, now = now) }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState != Player.STATE_ENDED) return
        // Queue ran out naturally — the still-current track played to its
        // end. No transition fires for this case, so handle it here.
        val outgoing = current ?: return
        flushListening(outgoing)
        outgoing.startedAt = null
        val uri = outgoing.uri
        val now = nowMs()
        scope.launch { repository.recordEvent(uri = uri, isPlay = true, now = now) }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val session = current ?: return
        if (isPlaying) {
            session.startedAt = nowMs()
        } else {
            flushListening(session)
            session.startedAt = null
        }
    }

    fun flush() {
        val session = current ?: return
        flushListening(session)
        session.startedAt = null
    }

    private fun flushListening(session: Session) {
        val startedAt = session.startedAt ?: return
        val delta = (nowMs() - startedAt).coerceAtLeast(0L)
        if (delta <= 0L) return
        val uri = session.uri
        scope.launch { repository.addListeningMs(uri = uri, ms = delta) }
    }

    private fun readDuration(mediaItemIndex: Int): Long? {
        val timeline = player.currentTimeline
        if (mediaItemIndex < 0 || mediaItemIndex >= timeline.windowCount) return null
        return runCatching { timeline.getWindow(mediaItemIndex, window).durationMs }
            .getOrNull()
            ?.takeIf { it > 0 }
    }
}
