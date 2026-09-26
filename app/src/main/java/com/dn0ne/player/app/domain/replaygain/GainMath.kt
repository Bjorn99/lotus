package com.dn0ne.player.app.domain.replaygain

import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Per-frame gain with a linear ramp, so a settings change never clicks. Track changes
 * [snapTo] instead: the sink drains the old stream before flushing, so nothing is left
 * to smooth over.
 */
class GainRamp(initial: Float = 1f) {
    var current: Float = initial
        private set
    private var target = initial
    private var step = 0f
    private var framesLeft = 0

    val isSteady: Boolean get() = framesLeft == 0

    fun snapTo(factor: Float) {
        current = factor
        target = factor
        step = 0f
        framesLeft = 0
    }

    fun rampTo(factor: Float, frames: Int) {
        if (frames <= 0) return snapTo(factor)
        target = factor
        step = (factor - current) / frames
        framesLeft = frames
    }

    fun next(): Float {
        if (framesLeft > 0) {
            framesLeft--
            // Land on the target exactly, so unity can take the copy-through fast path.
            current = if (framesLeft == 0) target else current + step
        }
        return current
    }
}

fun rampFrames(sampleRate: Int, durationMs: Int = 20): Int = sampleRate * durationMs / 1000

/**
 * Multiplies interleaved PCM from [input] into [output], one [GainRamp.next] per frame.
 * 16-bit samples are rounded and clamped: a plain cast would wrap a boosted peak into
 * loud crackle. Float samples are clamped to full scale. Both buffers must share a byte
 * order (Media3 uses native order for both).
 */
fun applyGain(
    input: ByteBuffer,
    output: ByteBuffer,
    channelCount: Int,
    isFloat: Boolean,
    ramp: GainRamp,
) {
    val frameBytes = channelCount * if (isFloat) 4 else 2
    while (input.remaining() >= frameBytes) {
        val gain = ramp.next()
        repeat(channelCount) {
            if (isFloat) {
                output.putFloat((input.getFloat() * gain).coerceIn(-1f, 1f))
            } else {
                output.putShort(
                    (input.getShort() * gain).roundToInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                )
            }
        }
    }
}
