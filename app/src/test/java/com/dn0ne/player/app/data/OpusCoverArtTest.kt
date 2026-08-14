package com.dn0ne.player.app.data

import com.dn0ne.player.app.domain.metadata.Metadata
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Cover art, multi-page tag packets, checksums and duplicate-field handling
 * for opus files.
 *
 * Every case here failed before the OGG checksum and packet-reassembly fixes;
 * between them they cover the ways an opus tag edit used to destroy the file.
 */
class OpusCoverArtTest {

    // ---- checksum ----

    @Test
    fun `oggCrc matches the OGG direct CRC-32, not java's reflected one`() {
        // Reference values for polynomial 0x04c11db7, init 0, no reflection,
        // no final XOR. java.util.zip.CRC32 gives entirely different numbers,
        // and using it made every rewritten page fail verification.
        val check = "123456789".toByteArray()
        assertEquals(0x89a1897f.toInt(), OpusTagEditor.oggCrc(check, check.size))
        assertEquals(0, OpusTagEditor.oggCrc(ByteArray(0), 0))
        val a = "a".toByteArray()
        assertEquals(0xa864db20.toInt(), OpusTagEditor.oggCrc(a, a.size))
    }

    @Test
    fun `every page a rewrite produces carries a valid checksum`() {
        val art = ByteArray(120_000) { (it % 251).toByte() }
        val file = writeOpusFile(mapOf("TITLE" to "Old"))
        try {
            OpusTagEditor.update(file, Metadata(title = "New", coverArtBytes = art))
            assertAllPagesChecksumValid(file)
        } finally {
            file.delete()
        }
    }

    // ---- cover art ----

    @Test
    fun `cover art is written and reads back byte-identical`() {
        val file = writeOpusFile(mapOf("TITLE" to "Song"))
        try {
            val art = jpeg(width = 640, height = 480, payload = 4_000)
            OpusTagEditor.update(file, Metadata(title = "Song", coverArtBytes = art))

            val block = pictureBlockOf(file)
            assertArrayEquals("embedded art must survive the round trip", art, block.data)
            assertEquals("image/jpeg", block.mimeType)
            assertEquals(3, block.pictureType)
            assertEquals(640, block.width)
            assertEquals(480, block.height)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `cover art large enough to span pages round-trips`() {
        val file = writeOpusFile(mapOf("TITLE" to "Song"))
        try {
            val art = jpeg(width = 1000, height = 1000, payload = 300_000)
            OpusTagEditor.update(file, Metadata(coverArtBytes = art))

            assertTrue("art this size must occupy several pages",
                OpusTagEditor.readOggPages(file).size > 3)
            assertArrayEquals(art, pictureBlockOf(file).data)
            assertAllPagesChecksumValid(file)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `replacing cover art leaves exactly one picture field`() {
        val file = writeOpusFile(mapOf("TITLE" to "Song"))
        try {
            OpusTagEditor.update(file, Metadata(coverArtBytes = jpeg(100, 100, 2_000)))
            val second = jpeg(200, 200, 3_000)
            OpusTagEditor.update(file, Metadata(coverArtBytes = second))

            val pictures = rawCommentsOf(file).filter { it.startsWith("METADATA_BLOCK_PICTURE=") }
            assertEquals("a second write must replace, not append", 1, pictures.size)
            assertArrayEquals(second, pictureBlockOf(file).data)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `existing cover art survives an unrelated field edit`() {
        val file = writeOpusFile(mapOf("TITLE" to "Old"))
        try {
            val art = jpeg(500, 500, 120_000)
            OpusTagEditor.update(file, Metadata(coverArtBytes = art))

            OpusTagEditor.update(file, Metadata(title = "New"))

            assertEquals("New", fieldsOf(file)["TITLE"])
            assertArrayEquals(
                "editing the title must not disturb the artwork",
                art,
                pictureBlockOf(file).data,
            )
            assertAllPagesChecksumValid(file)
        } finally {
            file.delete()
        }
    }

    // ---- multi-page tag packets ----

    @Test
    fun `readLyrics finds lyrics that art pushed onto a later page`() {
        val file = writeOpusFile(mapOf("TITLE" to "Song"))
        try {
            OpusTagEditor.update(
                file,
                Metadata(coverArtBytes = jpeg(800, 800, 150_000), lyrics = "the words"),
            )
            assertTrue("tags must span pages for this to mean anything",
                OpusTagEditor.readOggPages(file).size > 3)
            assertEquals("the words", OpusTagEditor.readLyrics(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `audio pages are renumbered when the tag packet changes size`() {
        val file = writeOpusFile(mapOf("TITLE" to "Song"), audioPages = 3)
        try {
            OpusTagEditor.update(file, Metadata(coverArtBytes = jpeg(600, 600, 140_000)))

            val pages = OpusTagEditor.readOggPages(file)
            val sequences = pages.map { it.pageSequence }
            assertEquals(
                "page sequence numbers must stay consecutive",
                (0 until pages.size).toList(),
                sequences,
            )
            assertAllPagesChecksumValid(file)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `audio payloads are preserved verbatim across a tag rewrite`() {
        val file = writeOpusFile(mapOf("TITLE" to "Song"), audioPages = 3)
        try {
            val before = OpusTagEditor.readOggPages(file)
                .filter { String(it.payload.copyOfRange(0, 5)) == "AUDIO" }
                .map { it.payload.toList() }

            OpusTagEditor.update(file, Metadata(coverArtBytes = jpeg(600, 600, 140_000)))

            val after = OpusTagEditor.readOggPages(file)
                .filter { it.payload.size >= 5 && String(it.payload.copyOfRange(0, 5)) == "AUDIO" }
                .map { it.payload.toList() }

            assertEquals("no audio page may be lost", before.size, after.size)
            assertEquals("audio must be bit-identical", before, after)
        } finally {
            file.delete()
        }
    }

    // ---- duplicate fields ----

    @Test
    fun `repeated fields survive an unrelated edit`() {
        val file = rawCommentOpusFile(listOf("ARTIST=First", "ARTIST=Second", "TITLE=Song"))
        try {
            OpusTagEditor.update(file, Metadata(title = "New"))
            val after = rawCommentsOf(file)
            assertEquals(
                "VorbisComment permits repeated keys; both must survive",
                listOf("ARTIST=First", "ARTIST=Second"),
                after.filter { it.startsWith("ARTIST=") },
            )
            assertEquals("New", fieldsOf(file)["TITLE"])
        } finally {
            file.delete()
        }
    }

    @Test
    fun `setting a repeated field collapses it to the new value`() {
        val file = rawCommentOpusFile(listOf("ARTIST=First", "ARTIST=Second"))
        try {
            OpusTagEditor.update(file, Metadata(artist = "Only"))
            assertEquals(listOf("ARTIST=Only"), rawCommentsOf(file).filter { it.startsWith("ARTIST=") })
        } finally {
            file.delete()
        }
    }

    @Test
    fun `unknown fields pass through untouched`() {
        val file = rawCommentOpusFile(listOf("TITLE=Song", "REPLAYGAIN_TRACK_GAIN=-7.5 dB"))
        try {
            OpusTagEditor.update(file, Metadata(title = "New"))
            assertTrue(
                "third-party fields must not be dropped",
                rawCommentsOf(file).contains("REPLAYGAIN_TRACK_GAIN=-7.5 dB"),
            )
        } finally {
            file.delete()
        }
    }

    // ---- picture block encoding ----

    @Test
    fun `base64Encode matches the standard alphabet and padding`() {
        assertEquals("", OpusTagEditor.base64Encode(ByteArray(0)))
        assertEquals("Zg==", OpusTagEditor.base64Encode("f".toByteArray()))
        assertEquals("Zm8=", OpusTagEditor.base64Encode("fo".toByteArray()))
        assertEquals("Zm9v", OpusTagEditor.base64Encode("foo".toByteArray()))
        assertEquals("Zm9vYmFy", OpusTagEditor.base64Encode("foobar".toByteArray()))
    }

    @Test
    fun `mime type is sniffed from the binary signature`() {
        assertEquals("image/jpeg", OpusTagEditor.sniffMimeType(jpeg(1, 1, 32)))
        assertEquals("image/png", OpusTagEditor.sniffMimeType(png(1, 1)))
        assertEquals(null, OpusTagEditor.sniffMimeType("not an image".toByteArray()))
    }

    @Test
    fun `dimensions are read from png and jpeg headers`() {
        assertEquals(1200 to 800, OpusTagEditor.sniffDimensions(png(1200, 800)))
        assertEquals(640 to 480, OpusTagEditor.sniffDimensions(jpeg(640, 480, 64)))
        assertEquals(0 to 0, OpusTagEditor.sniffDimensions("not an image".toByteArray()))
    }

    // ---- helpers ----

    private class PictureBlock(
        val pictureType: Int,
        val mimeType: String,
        val width: Int,
        val height: Int,
        val data: ByteArray,
    )

    private fun pictureBlockOf(file: File): PictureBlock {
        val encoded = fieldsOf(file)["METADATA_BLOCK_PICTURE"]
            ?: throw AssertionError("no METADATA_BLOCK_PICTURE field present")
        val raw = base64Decode(encoded)
        val buf = ByteBuffer.wrap(raw) // picture blocks are big-endian
        val type = buf.int
        val mime = ByteArray(buf.int).also { buf.get(it) }
        val descriptionLength = buf.int
        buf.position(buf.position() + descriptionLength)
        val width = buf.int
        val height = buf.int
        buf.int // colour depth
        buf.int // indexed colours
        val data = ByteArray(buf.int).also { buf.get(it) }
        return PictureBlock(type, String(mime, Charsets.US_ASCII), width, height, data)
    }

    private fun base64Decode(s: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val out = ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (c in s) {
            if (c == '=') break
            val v = alphabet.indexOf(c)
            require(v >= 0) { "bad base64 char: $c" }
            buffer = (buffer shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }

    /** Verifies each page's stored checksum against a freshly computed one. */
    private fun assertAllPagesChecksumValid(file: File) {
        val bytes = file.readBytes()
        var offset = 0
        var page = 0
        while (offset + 27 <= bytes.size && String(bytes, offset, 4, Charsets.US_ASCII) == "OggS") {
            val numSegments = bytes[offset + 26].toInt() and 0xFF
            var payload = 0
            for (i in 0 until numSegments) payload += bytes[offset + 27 + i].toInt() and 0xFF
            val size = 27 + numSegments + payload

            val copy = bytes.copyOfRange(offset, offset + size)
            val stored = ByteBuffer.wrap(copy, 22, 4).order(ByteOrder.LITTLE_ENDIAN).int
            for (i in 22..25) copy[i] = 0
            val computed = OpusTagEditor.oggCrc(copy, copy.size)

            assertEquals("page $page has a bad checksum", stored, computed)
            offset += size
            page++
        }
        assertTrue("no pages examined", page > 0)
        assertEquals("trailing bytes after last page", bytes.size, offset)
    }

    private fun fieldsOf(file: File): Map<String, String> =
        rawCommentsOf(file).mapNotNull { entry ->
            val eq = entry.indexOf('=')
            if (eq > 0) entry.substring(0, eq).uppercase() to entry.substring(eq + 1) else null
        }.toMap()

    /** Reassembles the OpusTags packet across continuation pages, as a decoder does. */
    private fun rawCommentsOf(file: File): List<String> {
        val pages = OpusTagEditor.readOggPages(file)
        if (pages.size < 2) return emptyList()

        val joined = ByteArrayOutputStream()
        joined.write(pages[1].payload)
        for (p in pages.drop(2)) {
            if (!p.isContinuation) break
            joined.write(p.payload)
        }
        val buf = ByteBuffer.wrap(joined.toByteArray()).order(ByteOrder.LITTLE_ENDIAN)

        val magic = ByteArray(8)
        buf.get(magic)
        if (String(magic, Charsets.US_ASCII) != "OpusTags") return emptyList()
        val vendorLength = buf.int
        buf.position(buf.position() + vendorLength)
        val numComments = buf.int

        val out = mutableListOf<String>()
        repeat(numComments) {
            if (buf.remaining() < 4) return@repeat
            val len = buf.int
            if (len < 0 || buf.remaining() < len) return@repeat
            val raw = ByteArray(len)
            buf.get(raw)
            out += String(raw, Charsets.UTF_8)
        }
        return out
    }

    private fun writeOpusFile(fields: Map<String, String>, audioPages: Int = 1): File =
        rawCommentOpusFile(fields.map { (k, v) -> "$k=$v" }, audioPages)

    private fun rawCommentOpusFile(comments: List<String>, audioPages: Int = 1): File {
        val out = ByteArrayOutputStream()
        out.write(
            OpusTagEditor.buildOggPageBytes(
                OpusTagEditor.OggPage(
                    offset = 0, headerSize = 28,
                    payload = "OpusHead".toByteArray() + ByteArray(11),
                    serialNumber = 1, pageSequence = 0, isContinuation = false,
                )
            )
        )
        out.write(
            OpusTagEditor.buildOggPageBytes(
                OpusTagEditor.OggPage(
                    offset = 0, headerSize = 28,
                    payload = buildOpusTagsPayload("TestVendor", comments),
                    serialNumber = 1, pageSequence = 1, isContinuation = false,
                )
            )
        )
        repeat(audioPages) { i ->
            out.write(
                OpusTagEditor.buildOggPageBytes(
                    OpusTagEditor.OggPage(
                        offset = 0, headerSize = 28,
                        payload = "AUDIO".toByteArray() + ByteArray(200) { (i + it).toByte() },
                        serialNumber = 1, pageSequence = 2 + i, isContinuation = false,
                    )
                )
            )
        }
        val file = File.createTempFile("opus_cover_test", ".opus")
        file.writeBytes(out.toByteArray())
        return file
    }

    private fun buildOpusTagsPayload(vendor: String, comments: List<String>): ByteArray {
        val vendorBytes = vendor.toByteArray(Charsets.UTF_8)
        val commentBytes = comments.map { it.toByteArray(Charsets.UTF_8) }
        val size = 8 + 4 + vendorBytes.size + 4 + commentBytes.sumOf { 4 + it.size }
        val buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("OpusTags".toByteArray(Charsets.US_ASCII))
        buf.putInt(vendorBytes.size)
        buf.put(vendorBytes)
        buf.putInt(commentBytes.size)
        for (c in commentBytes) {
            buf.putInt(c.size)
            buf.put(c)
        }
        return buf.array()
    }

    /** A JPEG with a real SOF0 frame header, padded to [payload] bytes. */
    private fun jpeg(width: Int, height: Int, payload: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) // SOI
        out.write(byteArrayOf(0xFF.toByte(), 0xC0.toByte())) // SOF0
        out.write(byteArrayOf(0x00, 0x11))                   // length 17
        out.write(8)                                          // sample precision
        out.write(byteArrayOf((height shr 8).toByte(), height.toByte()))
        out.write(byteArrayOf((width shr 8).toByte(), width.toByte()))
        out.write(ByteArray(6))                               // component spec
        val header = out.toByteArray()
        return header + ByteArray(maxOf(0, payload - header.size)) { (it % 253).toByte() }
    }

    /** A PNG header with a real IHDR chunk. */
    private fun png(width: Int, height: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()))
        out.write(byteArrayOf(0x0D, 0x0A, 0x1A, 0x0A))
        out.write(byteArrayOf(0, 0, 0, 0x0D))                 // IHDR length
        out.write("IHDR".toByteArray())
        out.write(
            byteArrayOf(
                (width shr 24).toByte(), (width shr 16).toByte(),
                (width shr 8).toByte(), width.toByte(),
                (height shr 24).toByte(), (height shr 16).toByte(),
                (height shr 8).toByte(), height.toByte(),
            )
        )
        out.write(ByteArray(9))
        return out.toByteArray()
    }
}
