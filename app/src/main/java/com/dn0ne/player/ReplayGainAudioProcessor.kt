package com.dn0ne.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.dn0ne.player.app.domain.replaygain.GainRamp
import com.dn0ne.player.app.domain.replaygain.applyGain
import com.dn0ne.player.app.domain.replaygain.rampFrames
import java.nio.ByteBuffer

private const val LOG_TAG = "ReplayGain"

/**
 * Applies the ReplayGain factor inside the sink's sample pipeline.
 *
 * Not Media3's GainProcessor: that one only attenuates, wraps boosted 16-bit samples
 * instead of clamping, and forbids the gain changing during playback.
 *
 * Track identity comes from the flush, never from the player. DefaultAudioSink drains the
 * old stream and then flushes with the new period, so the flush lands exactly on the
 * boundary. The player's reported transition lagged it by 0.47-0.68 s on a Pixel 4 XL.
 */
@OptIn(UnstableApi::class)
class ReplayGainAudioProcessor(
    private val factorFor: (mediaId: String?) -> Float,
) : BaseAudioProcessor() {

    private val ramp = GainRamp()
    private var mediaId: String? = null

    @Volatile
    private var retargetRequested = false

    /** Called from the main thread. The audio thread picks it up on the next buffer. */
    fun onSettingsChanged() {
        retargetRequested = true
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT -> inputAudioFormat
            // Throwing here would fail playback. NOT_SET makes this processor inactive,
            // so the audio plays untouched, just without ReplayGain.
            else -> AudioProcessor.AudioFormat.NOT_SET
        }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        // There are 2-3 flushes per boundary and the first can still carry the previous
        // period. Recompute from the id every time, never count flushes.
        mediaId = streamMetadata.mediaIdOrNull()
        retargetRequested = false
        val factor = factorFor(mediaId)
        ramp.snapTo(factor)
        Log.d(LOG_TAG, "flush id=$mediaId factor=$factor")
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        if (retargetRequested) {
            retargetRequested = false
            ramp.rampTo(factorFor(mediaId), rampFrames(inputAudioFormat.sampleRate))
        }
        val output = replaceOutputBuffer(inputBuffer.remaining())
        if (ramp.isSteady && ramp.current == 1f) {
            output.put(inputBuffer)
        } else {
            applyGain(
                input = inputBuffer,
                output = output,
                channelCount = inputAudioFormat.channelCount,
                isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT,
                ramp = ramp,
            )
        }
        output.flip()
    }

    override fun onReset() {
        mediaId = null
        ramp.snapTo(1f)
    }
}

@OptIn(UnstableApi::class)
private fun AudioProcessor.StreamMetadata.mediaIdOrNull(): String? {
    val uid = periodUid ?: return null
    return runCatching {
        val period = timeline.getPeriodByUid(uid, Timeline.Period())
        timeline.getWindow(period.windowIndex, Timeline.Window()).mediaItem.mediaId
    }.getOrNull()
}

/** DefaultRenderersFactory whose audio sink carries the ReplayGain processor. */
@OptIn(UnstableApi::class)
class ReplayGainRenderersFactory(
    context: Context,
    private val processor: ReplayGainAudioProcessor,
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setEnableFloatOutput(enableFloatOutput)
        .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
        .setAudioProcessors(arrayOf(processor))
        .build()
}
