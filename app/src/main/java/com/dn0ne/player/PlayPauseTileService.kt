package com.dn0ne.player

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class PlayPauseTileService : TileService() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var playerListener: Player.Listener? = null

    override fun onStartListening() {
        super.onStartListening()
        connect()
    }

    override fun onStopListening() {
        super.onStopListening()
        disconnect()
    }

    override fun onClick() {
        super.onClick()
        val c = controller ?: return
        val nowPlaying = c.isPlaying
        if (nowPlaying) c.pause() else c.play()
        // Optimistic tile update; the listener will reconcile if play() no-ops
        // (e.g. empty queue) on the next state change.
        renderTile(!nowPlaying)
    }

    private fun connect() {
        val token = SessionToken(
            applicationContext,
            ComponentName(applicationContext, PlaybackService::class.java)
        )
        val future = MediaController.Builder(applicationContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                if (future.isCancelled) return@addListener
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        renderTile(isPlaying)
                    }
                }
                playerListener = listener
                c.addListener(listener)
                renderTile(c.isPlaying)
            },
            ContextCompat.getMainExecutor(applicationContext)
        )
    }

    private fun disconnect() {
        playerListener?.let { controller?.removeListener(it) }
        playerListener = null
        controller?.release()
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    private fun renderTile(isPlaying: Boolean) {
        val tile = qsTile ?: return
        if (isPlaying) {
            tile.icon = Icon.createWithResource(this, android.R.drawable.ic_media_pause)
            tile.label = getString(R.string.tile_label_pause)
            tile.state = Tile.STATE_ACTIVE
        } else {
            tile.icon = Icon.createWithResource(this, android.R.drawable.ic_media_play)
            tile.label = getString(R.string.tile_label_play)
            tile.state = Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }
}
