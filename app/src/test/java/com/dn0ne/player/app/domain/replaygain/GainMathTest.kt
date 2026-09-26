package com.dn0ne.player.app.domain.replaygain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GainMathTest {

    private fun shorts(vararg v: Int): ByteBuffer =
        ByteBuffer.allocate(v.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            v.forEach { putShort(it.toShort()) }; flip()
        }

    private fun floats(vararg v: Float): ByteBuffer =
        ByteBuffer.allocate(v.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
            v.forEach { putFloat(it) }; flip()
        }

    private fun out(bytes: Int) = ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN)

    private fun readShorts(b: ByteBuffer): List<Int> { b.flip(); return List(b.remaining() / 2) { b.getShort().toInt() } }
    private fun readFloats(b: ByteBuffer): List<Float> { b.flip(); return List(b.remaining() / 4) { b.getFloat() } }

    @Test
    fun `16-bit boost clamps instead of wrapping`() {
        // A plain (short) cast turns 65534 into -2: loud crackle. This is the
        // GainProcessor defect that made us write our own processor.
        val o = out(4)
        applyGain(shorts(32767, -32768), o, channelCount = 2, isFloat = false, ramp = GainRamp(2f))
        assertEquals(listOf(32767, -32768), readShorts(o))
    }

    @Test
    fun `16-bit attenuation rounds`() {
        val o = out(4)
        applyGain(shorts(1000, -1001), o, channelCount = 1, isFloat = false, ramp = GainRamp(0.5f))
        assertEquals(listOf(500, -500), readShorts(o))
    }

    @Test
    fun `float clamps to full scale`() {
        val o = out(8)
        applyGain(floats(0.8f, -0.8f), o, channelCount = 2, isFloat = true, ramp = GainRamp(2f))
        assertEquals(listOf(1f, -1f), readFloats(o))
    }

    @Test
    fun `one factor per frame across all channels`() {
        val ramp = GainRamp(1f).apply { rampTo(0f, 2) }
        val o = out(8)
        applyGain(shorts(1000, 1000, 1000, 1000), o, channelCount = 2, isFloat = false, ramp = ramp)
        assertEquals(listOf(500, 500, 0, 0), readShorts(o))
    }

    @Test
    fun `ramp moves linearly then holds the target exactly`() {
        val r = GainRamp(1f).apply { rampTo(0f, 4) }
        assertEquals(listOf(0.75f, 0.5f, 0.25f, 0f, 0f), List(5) { r.next() })
        assertTrue(r.isSteady)
    }

    @Test
    fun `ramp back to unity lands on exactly one`() {
        val r = GainRamp(0.3f).apply { rampTo(1f, 3) }
        repeat(3) { r.next() }
        assertEquals(1f, r.current, 0f)
    }

    @Test
    fun `snap jumps with no ramp`() {
        val r = GainRamp(1f).apply { rampTo(0f, 100); snapTo(0.5f) }
        assertEquals(0.5f, r.next(), 0f)
        assertTrue(r.isSteady)
    }

    @Test
    fun `20 ms of frames`() {
        assertEquals(882, rampFrames(44100))
        assertEquals(960, rampFrames(48000))
    }
}
