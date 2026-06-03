package com.dn0ne.player.app.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OpusTagEditorTest {

    // ---- segment table (buildOggPageBytes) ----

    @Test
    fun `empty payload produces single zero segment`() {
        val page = OpusTagEditor.OggPage(
            offset = 0, headerSize = 28, payload = ByteArray(0),
            serialNumber = 1, pageSequence = 1, isContinuation = false,
        )
        val bytes = OpusTagEditor.buildOggPageBytes(page)
        // 27 header + 1 segment table + 0 payload = 28 bytes
        assertEquals(28, bytes.size)
        assertEquals(1, bytes[26].toInt() and 0xFF) // numSegments = 1
        assertEquals(0, bytes[27].toInt())           // segment value = 0
    }

    @Test
    fun `payload of 255 produces segments 255,0`() {
        val page = OpusTagEditor.OggPage(
            offset = 0, headerSize = 29, payload = ByteArray(255),
            serialNumber = 1, pageSequence = 1, isContinuation = false,
        )
        val bytes = OpusTagEditor.buildOggPageBytes(page)
        // 27 header + 2 segment table + 255 payload
        assertEquals(27 + 2 + 255, bytes.size)
        assertEquals(2, bytes[26].toInt() and 0xFF) // numSegments = 2
        assertEquals(255, bytes[27].toInt() and 0xFF) // first = 255
        assertEquals(0, bytes[28].toInt() and 0xFF)   // trailing zero
    }

    @Test
    fun `payload of 256 produces segments 255,1`() {
        val page = OpusTagEditor.OggPage(
            offset = 0, headerSize = 29, payload = ByteArray(256),
            serialNumber = 1, pageSequence = 1, isContinuation = false,
        )
        val bytes = OpusTagEditor.buildOggPageBytes(page)
        assertEquals(27 + 2 + 256, bytes.size)
        assertEquals(2, bytes[26].toInt() and 0xFF)
        assertEquals(255, bytes[27].toInt() and 0xFF)
        assertEquals(1, bytes[28].toInt() and 0xFF)
    }

    @Test
    fun `payload of 510 produces segments 255,255,0`() {
        val page = OpusTagEditor.OggPage(
            offset = 0, headerSize = 30, payload = ByteArray(510),
            serialNumber = 1, pageSequence = 1, isContinuation = false,
        )
        val bytes = OpusTagEditor.buildOggPageBytes(page)
        assertEquals(3, bytes[26].toInt() and 0xFF) // numSegments = 3
        assertEquals(255, bytes[27].toInt() and 0xFF)
        assertEquals(255, bytes[28].toInt() and 0xFF)
        assertEquals(0, bytes[29].toInt() and 0xFF) // trailing zero
    }

    @Test
    fun `payload of 1 produces single segment of 1`() {
        val page = OpusTagEditor.OggPage(
            offset = 0, headerSize = 28, payload = ByteArray(1),
            serialNumber = 1, pageSequence = 1, isContinuation = false,
        )
        val bytes = OpusTagEditor.buildOggPageBytes(page)
        assertEquals(1, bytes[26].toInt() and 0xFF)
        assertEquals(1, bytes[27].toInt() and 0xFF)
    }

    @Test
    fun `round trip preserves CRC-correct bytes`() {
        val payload = ByteArray(200) { it.toByte() }
        val page = OpusTagEditor.OggPage(
            offset = 0, headerSize = 28, payload = payload,
            serialNumber = 42, pageSequence = 0, isContinuation = false,
        )
        val bytes1 = OpusTagEditor.buildOggPageBytes(page)
        val bytes2 = OpusTagEditor.buildOggPageBytes(page)
        assertArrayEquals("repeated build should be deterministic", bytes1, bytes2)

        // CRC at bytes 22-25 should be non-zero (non-empty payload)
        val crc = ((bytes1[22].toInt() and 0xFF)
            or ((bytes1[23].toInt() and 0xFF) shl 8)
            or ((bytes1[24].toInt() and 0xFF) shl 16)
            or ((bytes1[25].toInt() and 0xFF) shl 24))
        assertTrue("CRC should be non-zero for non-empty payload", crc != 0)
    }

    // ---- readOggPages ----

    @Test
    fun `readOggPages parses valid OGG file`() {
        // Build a minimal 2-page OGG file
        val page0 = OpusTagEditor.OggPage(
            offset = 0, headerSize = 28, payload = "OpusHead".toByteArray() + ByteArray(11),
            serialNumber = 1, pageSequence = 0, isContinuation = false,
        )
        val page1 = OpusTagEditor.OggPage(
            offset = 0, headerSize = 28,
            payload = buildMinimalOpusTags("TestVendor", mapOf("TITLE" to "TestTitle")),
            serialNumber = 1, pageSequence = 1, isContinuation = false,
        )

        val file = File.createTempFile("test_opus", ".opus")
        try {
            file.writeBytes(OpusTagEditor.buildOggPageBytes(page0) + OpusTagEditor.buildOggPageBytes(page1))
            val pages = OpusTagEditor.readOggPages(file)

            assertTrue("should have at least 2 pages", pages.size >= 2)
            assertEquals(0, pages[0].pageSequence)
            assertEquals(1, pages[1].pageSequence)
            assertEquals(1, pages[0].serialNumber)
            assertEquals(1, pages[1].serialNumber)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `writePage replaces page correctly`() {
        val originalPayload = buildMinimalOpusTags("Vendor", mapOf("TITLE" to "OldTitle"))
        val originalPage = OpusTagEditor.OggPage(
            offset = 0, headerSize = 28, payload = originalPayload,
            serialNumber = 1, pageSequence = 1, isContinuation = false,
        )
        val opusHeadPage = OpusTagEditor.OggPage(
            offset = 0, headerSize = 28, payload = "OpusHead".toByteArray() + ByteArray(11),
            serialNumber = 1, pageSequence = 0, isContinuation = false,
        )

        val file = File.createTempFile("test_opus_write", ".opus")
        try {
            file.writeBytes(
                OpusTagEditor.buildOggPageBytes(opusHeadPage) +
                OpusTagEditor.buildOggPageBytes(originalPage)
            )

            // Modify page 1
            val newPayload = buildMinimalOpusTags("Vendor", mapOf("TITLE" to "NewTitle"))
            val newPage = OpusTagEditor.OggPage(
                offset = 0, headerSize = 28, payload = newPayload,
                serialNumber = 1, pageSequence = 1, isContinuation = false,
            )
            OpusTagEditor.writePage(file, 1, newPage)

            // Verify the file was updated
            val pages = OpusTagEditor.readOggPages(file)
            assertTrue("should have at least 2 pages", pages.size >= 2)
            // Page 0 unchanged, page 1 updated
            assertEquals(0, pages[0].pageSequence)
            assertEquals(1, pages[1].pageSequence)
        } finally {
            file.delete()
        }
    }

    // ---- helpers ----

    private fun buildMinimalOpusTags(vendor: String, fields: Map<String, String>): ByteArray {
        val vendorBytes = vendor.toByteArray(Charsets.UTF_8)
        val commentBytes = fields.map { (k, v) ->
            "$k=$v".toByteArray(Charsets.UTF_8)
        }
        val size = 8 + 4 + vendorBytes.size + 4 + commentBytes.sumOf { 4 + it.size }
        val buf = java.nio.ByteBuffer.allocate(size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buf.put("OpusTags".toByteArray(Charsets.UTF_8))
        buf.putInt(vendorBytes.size)
        buf.put(vendorBytes)
        buf.putInt(commentBytes.size)
        for (c in commentBytes) {
            buf.putInt(c.size)
            buf.put(c)
        }
        return buf.array()
    }
}
