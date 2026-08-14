package com.dn0ne.player.app.data

import com.dn0ne.player.app.domain.metadata.Metadata
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal OGG page-level tag editor for OPUS files.
 *
 * jaudiotagger 3.0.1 has no .opus extension mapping, and its OggFileReader
 * validates for Vorbis identification headers — it rejects OpusHead. This
 * class parses the OGG container directly, modifies the OpusTags
 * (VorbisComment) packet, recalculates page CRCs, and writes back.
 *
 * OPUS tag structure (RFC 7845 §5.2):
 *   Page 0:  OpusHead (identification, untouched)
 *   Page 1+: OpusTags (VorbisComment fields — this is what we edit). The
 *            comment packet may span several pages; a well-tagged file with
 *            embedded cover art always does.
 *   Pages N+: Audio data (payload untouched, page sequence renumbered)
 *
 * Cover art is stored as a base64 METADATA_BLOCK_PICTURE comment holding a
 * FLAC picture block, which is the convention every Vorbis-comment container
 * uses (Xiph "Embedding metadata in Ogg").
 */
internal object OpusTagEditor {

    // VorbisComment has no single standard lyrics field. LYRICS is what the write
    // path below emits; UNSYNCEDLYRICS is what several other taggers produce.
    private val lyricsFieldNames = listOf("LYRICS", "UNSYNCEDLYRICS")

    private const val PICTURE_FIELD = "METADATA_BLOCK_PICTURE"

    // An OGG page carries at most 255 lacing values of 255 bytes. A page that
    // ENDS a packet needs one spare lacing value for the terminator when the
    // payload is a multiple of 255, and a page that CONTINUES a packet must
    // have every lacing value equal to 255 (any smaller value ends the packet).
    // 254 * 255 satisfies both constraints, so one bound covers both cases.
    private const val MAX_PAGE_PAYLOAD = 254 * 255

    // ---- public API ----

    /**
     * Reads embedded lyrics from the OpusTags packet.
     *
     * The write path goes through this editor because jaudiotagger cannot open
     * .opus files at all; reading needs the same treatment, or lyrics embedded in
     * an opus file stay invisible to the app.
     *
     * Returns null when the file has no tags packet or carries no lyrics field.
     */
    fun readLyrics(file: File): String? {
        val packet = readTagPacket(readOggPages(file)) ?: return null
        return lyricsFieldNames.firstNotNullOfOrNull { name ->
            packet.comments.first(name)?.takeIf { it.isNotBlank() }
        }
    }

    fun update(file: File, metadata: Metadata) {
        val pages = readOggPages(file)
        val packet = readTagPacket(pages) ?: return

        val comments = packet.comments
        metadata.title?.let { comments.set("TITLE", it) }
        metadata.album?.let { comments.set("ALBUM", it) }
        metadata.artist?.let { comments.set("ARTIST", it) }
        metadata.albumArtist?.let { comments.set("ALBUMARTIST", it) }
        metadata.genre?.let { comments.set("GENRE", it) }
        metadata.year?.let { comments.set("DATE", it) }
        metadata.trackNumber?.let { comments.set("TRACKNUMBER", it) }
        metadata.lyrics?.let { comments.set("LYRICS", it) }
        metadata.mbAlbumId?.let { comments.set("MUSICBRAINZ_RELEASEID", it) }
        metadata.mbReleaseGroupId?.let { comments.set("MUSICBRAINZ_RELEASEGROUPID", it) }
        metadata.mbAlbumArtistId?.let { comments.set("MUSICBRAINZ_ALBUMARTISTID", it) }
        metadata.coverArtBytes?.let { comments.set(PICTURE_FIELD, encodePictureBlock(it)) }

        val payload = buildOpusTagsPayload(packet.vendor, comments)
        val template = pages[packet.firstPageIndex]
        val newPages = splitIntoPages(
            payload = payload,
            serialNumber = template.serialNumber,
            startSequence = template.pageSequence,
        )

        rewrite(file, pages, packet.firstPageIndex, packet.lastPageIndex, newPages)
    }

    // ---- tag packet ----

    /**
     * The OpusTags packet, reassembled across however many pages it occupies.
     *
     * Reading only the first page loses every field past the page boundary and,
     * far worse, makes a rewrite drop them — which is how embedded cover art
     * used to disappear the moment any other field was edited.
     */
    private class TagPacket(
        val vendor: String,
        val comments: CommentList,
        val firstPageIndex: Int,
        val lastPageIndex: Int,
    )

    private fun readTagPacket(pages: List<OggPage>): TagPacket? {
        if (pages.size < 2) return null

        // The packet starts on page 1 and runs through every following page
        // flagged as a continuation.
        var last = 1
        while (last + 1 < pages.size && pages[last + 1].isContinuation) last++

        val joined = ByteArrayOutputStream()
        for (i in 1..last) joined.write(pages[i].payload)
        val buf = ByteBuffer.wrap(joined.toByteArray()).order(ByteOrder.LITTLE_ENDIAN)

        if (buf.remaining() < 8) return null
        val magic = ByteArray(8)
        buf.get(magic)
        if (String(magic, Charsets.US_ASCII) != "OpusTags") return null

        if (buf.remaining() < 4) return null
        val vendorLength = buf.int
        if (vendorLength < 0 || buf.remaining() < vendorLength) return null
        val vendorBytes = ByteArray(vendorLength)
        buf.get(vendorBytes)

        if (buf.remaining() < 4) return null
        val numComments = buf.int

        val comments = CommentList()
        repeat(numComments.coerceAtLeast(0)) {
            if (buf.remaining() < 4) return@repeat
            val len = buf.int
            if (len < 0 || buf.remaining() < len) return@repeat
            val raw = ByteArray(len)
            buf.get(raw)
            val entry = String(raw, Charsets.UTF_8)
            val eq = entry.indexOf('=')
            if (eq > 0) comments.add(entry.substring(0, eq), entry.substring(eq + 1))
        }

        return TagPacket(
            vendor = String(vendorBytes, Charsets.UTF_8),
            comments = comments,
            firstPageIndex = 1,
            lastPageIndex = last,
        )
    }

    /**
     * Ordered VorbisComment fields.
     *
     * A plain Map cannot represent these: the format explicitly permits a key
     * to repeat (multiple ARTIST entries for a collaboration, front and back
     * cover pictures), and collapsing them silently discards user data on
     * every save. Order is preserved so untouched files round-trip byte-exact.
     */
    internal class CommentList {
        private val entries = mutableListOf<Pair<String, String>>()

        fun add(key: String, value: String) {
            entries += key.uppercase() to value
        }

        /** Replaces every existing entry for [key] with a single new value. */
        fun set(key: String, value: String) {
            val upper = key.uppercase()
            val at = entries.indexOfFirst { it.first == upper }
            entries.removeAll { it.first == upper }
            if (at >= 0) entries.add(at, upper to value) else entries += upper to value
        }

        fun first(key: String): String? {
            val upper = key.uppercase()
            return entries.firstOrNull { it.first == upper }?.second
        }

        fun all(): List<Pair<String, String>> = entries
    }

    private fun buildOpusTagsPayload(vendor: String, comments: CommentList): ByteArray {
        val vendorBytes = vendor.toByteArray(Charsets.UTF_8)
        val commentBytes = comments.all().map { (key, value) ->
            "$key=$value".toByteArray(Charsets.UTF_8)
        }

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

    // ---- cover art ----

    /**
     * Encodes cover art as a base64 FLAC picture block, per the Xiph
     * "Embedding metadata in Ogg" note and FLAC §8.7. All integers are
     * big-endian, unlike the little-endian comment header around them.
     */
    internal fun encodePictureBlock(art: ByteArray): String {
        val mime = sniffMimeType(art) ?: "application/octet-stream"
        val (width, height) = sniffDimensions(art)

        val mimeBytes = mime.toByteArray(Charsets.US_ASCII)
        val out = ByteArrayOutputStream(art.size + mimeBytes.size + 64)
        DataOutputStream(out).use { d ->
            d.writeInt(PICTURE_TYPE_FRONT_COVER)
            d.writeInt(mimeBytes.size)
            d.write(mimeBytes)
            d.writeInt(0)          // description length; we write none
            d.writeInt(width)
            d.writeInt(height)
            d.writeInt(COLOUR_DEPTH_BITS)
            d.writeInt(0)          // indexed-palette colours; 0 for non-indexed
            d.writeInt(art.size)
            d.write(art)
        }
        return base64Encode(out.toByteArray())
    }

    private const val PICTURE_TYPE_FRONT_COVER = 3
    private const val COLOUR_DEPTH_BITS = 24

    internal fun sniffMimeType(data: ByteArray): String? = when {
        data.size >= 3 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() &&
            data[2] == 0xFF.toByte() -> "image/jpeg"

        data.size >= 8 && data[0] == 0x89.toByte() && data[1] == 'P'.code.toByte() &&
            data[2] == 'N'.code.toByte() && data[3] == 'G'.code.toByte() -> "image/png"

        data.size >= 4 && data[0] == 'G'.code.toByte() && data[1] == 'I'.code.toByte() &&
            data[2] == 'F'.code.toByte() -> "image/gif"

        data.size >= 12 && String(data, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(data, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"

        data.size >= 2 && data[0] == 'B'.code.toByte() && data[1] == 'M'.code.toByte() -> "image/bmp"

        else -> null
    }

    /**
     * Pixel dimensions, read from the image header.
     *
     * The picture block has fields for these and players do surface them, but
     * they are advisory — anything we cannot parse is written as 0, which the
     * spec permits.
     */
    internal fun sniffDimensions(data: ByteArray): Pair<Int, Int> = when {
        // PNG: IHDR is always the first chunk, width/height at fixed offsets.
        data.size >= 24 && data[0] == 0x89.toByte() && data[1] == 'P'.code.toByte() ->
            beInt(data, 16) to beInt(data, 20)

        data.size >= 3 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() ->
            jpegDimensions(data)

        else -> 0 to 0
    }

    /** Walks the JPEG marker chain to the start-of-frame, which carries the size. */
    private fun jpegDimensions(data: ByteArray): Pair<Int, Int> {
        var i = 2
        while (i + 9 < data.size) {
            if (data[i] != 0xFF.toByte()) { i++; continue }
            val marker = data[i + 1].toInt() and 0xFF
            // Standalone markers carry no length field.
            if (marker == 0xD8 || marker == 0x01 || (marker in 0xD0..0xD7)) { i += 2; continue }
            val length = ((data[i + 2].toInt() and 0xFF) shl 8) or (data[i + 3].toInt() and 0xFF)
            // SOF0-SOF15, excluding the non-frame markers DHT/JPG/DAC.
            if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                val height = ((data[i + 5].toInt() and 0xFF) shl 8) or (data[i + 6].toInt() and 0xFF)
                val width = ((data[i + 7].toInt() and 0xFF) shl 8) or (data[i + 8].toInt() and 0xFF)
                return width to height
            }
            if (length <= 0) break
            i += 2 + length
        }
        return 0 to 0
    }

    private fun beInt(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)

    private const val BASE64_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    /**
     * Base64 without line breaks.
     *
     * Hand-rolled because java.util.Base64 needs API 26 and android.util.Base64
     * is an unimplemented stub under JVM unit tests — and this class earns its
     * keep by being testable without an emulator.
     */
    internal fun base64Encode(data: ByteArray): String {
        val sb = StringBuilder((data.size + 2) / 3 * 4)
        var i = 0
        while (i + 2 < data.size) {
            val n = ((data[i].toInt() and 0xFF) shl 16) or
                ((data[i + 1].toInt() and 0xFF) shl 8) or
                (data[i + 2].toInt() and 0xFF)
            sb.append(BASE64_ALPHABET[(n ushr 18) and 0x3F])
            sb.append(BASE64_ALPHABET[(n ushr 12) and 0x3F])
            sb.append(BASE64_ALPHABET[(n ushr 6) and 0x3F])
            sb.append(BASE64_ALPHABET[n and 0x3F])
            i += 3
        }
        when (data.size - i) {
            1 -> {
                val n = (data[i].toInt() and 0xFF) shl 16
                sb.append(BASE64_ALPHABET[(n ushr 18) and 0x3F])
                sb.append(BASE64_ALPHABET[(n ushr 12) and 0x3F])
                sb.append("==")
            }
            2 -> {
                val n = ((data[i].toInt() and 0xFF) shl 16) or ((data[i + 1].toInt() and 0xFF) shl 8)
                sb.append(BASE64_ALPHABET[(n ushr 18) and 0x3F])
                sb.append(BASE64_ALPHABET[(n ushr 12) and 0x3F])
                sb.append(BASE64_ALPHABET[(n ushr 6) and 0x3F])
                sb.append('=')
            }
        }
        return sb.toString()
    }

    // ---- data types ----

    internal data class OggPage(
        val offset: Long,          // byte offset in the file where this page starts
        val headerSize: Int,       // 27 + segment table size
        val payload: ByteArray,    // decoded payload (concatenated segments)
        val serialNumber: Int,
        val pageSequence: Int,
        val isContinuation: Boolean,
    )

    // ---- OGG page parsing ----

    internal fun readOggPages(file: File): List<OggPage> {
        val pages = mutableListOf<OggPage>()
        RandomAccessFile(file, "r").use { raf ->
            while (true) {
                val offset = raf.filePointer
                val header = ByteArray(27)
                if (raf.read(header) < 27) break
                if (header[0] != 'O'.code.toByte() || header[1] != 'g'.code.toByte()
                    || header[2] != 'g'.code.toByte() || header[3] != 'S'.code.toByte()
                ) break

                val headerType = header[5].toInt() and 0xFF
                val serialNumber = ByteBuffer.wrap(header, 14, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                val pageSequence = ByteBuffer.wrap(header, 18, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                val numSegments = header[26].toInt() and 0xFF

                // readFully, not read: a short read leaves the file pointer
                // mid-page and every subsequent page silently disappears.
                val segmentTable = ByteArray(numSegments)
                try {
                    raf.readFully(segmentTable)
                } catch (_: java.io.EOFException) {
                    break
                }

                var payloadSize = 0
                for (s in segmentTable) payloadSize += (s.toInt() and 0xFF)
                val payload = ByteArray(payloadSize)
                try {
                    raf.readFully(payload)
                } catch (_: java.io.EOFException) {
                    break
                }

                pages += OggPage(
                    offset = offset,
                    headerSize = 27 + numSegments,
                    payload = payload,
                    serialNumber = serialNumber,
                    pageSequence = pageSequence,
                    isContinuation = (headerType and 0x01) != 0,
                )
            }
        }
        return pages
    }

    // ---- OGG page writing ----

    /**
     * Splits a packet across as many pages as it needs.
     *
     * Continuation pages must be filled to a multiple of 255 or the short
     * lacing value would terminate the packet early; [MAX_PAGE_PAYLOAD] keeps
     * every page on the safe side of both that rule and the 255-lacing limit.
     */
    private fun splitIntoPages(
        payload: ByteArray,
        serialNumber: Int,
        startSequence: Int,
    ): List<ByteArray> {
        val out = mutableListOf<ByteArray>()
        var offset = 0
        var sequence = startSequence
        while (true) {
            val length = minOf(MAX_PAGE_PAYLOAD, payload.size - offset)
            val chunk = payload.copyOfRange(offset, offset + length)
            val isLast = offset + length >= payload.size
            out += buildOggPageBytes(
                OggPage(
                    offset = 0,
                    headerSize = 0,
                    payload = chunk,
                    serialNumber = serialNumber,
                    pageSequence = sequence,
                    isContinuation = offset > 0,
                ),
                packetEnds = isLast,
            )
            offset += length
            sequence++
            if (isLast) break
        }
        return out
    }

    /**
     * Replaces the tag pages and renumbers everything after them.
     *
     * OGG page sequence numbers are consecutive per logical stream, so changing
     * how many pages the tags occupy invalidates every later page in that
     * stream. Their payloads are untouched; only the sequence number and the
     * checksum are rewritten.
     */
    private fun rewrite(
        file: File,
        pages: List<OggPage>,
        firstPageIndex: Int,
        lastPageIndex: Int,
        newPages: List<ByteArray>,
    ) {
        val serialNumber = pages[firstPageIndex].serialNumber
        val temp = File(file.parentFile, "${file.name}.tagtmp")

        try {
            RandomAccessFile(file, "r").use { source ->
                FileOutputStream(temp).use { out ->
                    copyRange(source, out, 0, pages[firstPageIndex].offset)

                    for (page in newPages) out.write(page)

                    var sequence = pages[firstPageIndex].pageSequence + newPages.size
                    for (i in lastPageIndex + 1 until pages.size) {
                        val page = pages[i]
                        val size = page.headerSize + page.payload.size
                        val raw = ByteArray(size)
                        source.seek(page.offset)
                        source.readFully(raw)

                        if (page.serialNumber == serialNumber) {
                            putIntLE(raw, 18, sequence)
                            putIntLE(raw, 22, 0)
                            putIntLE(raw, 22, oggCrc(raw, size))
                            sequence++
                        }
                        out.write(raw)
                    }
                }
            }

            // Overwrite in place rather than renaming: the caller owns this
            // File handle and hands the same path to the content resolver.
            temp.inputStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            temp.delete()
        }
    }

    private fun copyRange(source: RandomAccessFile, out: OutputStream, from: Long, to: Long) {
        source.seek(from)
        var remaining = to - from
        val buffer = ByteArray(64 * 1024)
        while (remaining > 0) {
            val read = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read <= 0) break
            out.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun putIntLE(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        target[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        target[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    internal fun buildOggPageBytes(page: OggPage): ByteArray =
        buildOggPageBytes(page, packetEnds = !page.isContinuation)

    internal fun buildOggPageBytes(page: OggPage, packetEnds: Boolean): ByteArray {
        // Segment table per RFC 3533: split payload into 255-byte lacing
        // values. A packet that ends on a 255-byte boundary MUST be followed
        // by a zero-length segment; a packet that continues onto the next page
        // must NOT have one, since any value below 255 ends the packet.
        val segmentTable = mutableListOf<Byte>()
        var remaining = page.payload.size
        if (remaining == 0) {
            segmentTable.add(0)
        } else {
            while (remaining > 0) {
                val len = remaining.coerceAtMost(255)
                segmentTable.add(len.toByte())
                remaining -= len
                if (len == 255 && remaining == 0 && packetEnds) {
                    segmentTable.add(0)
                }
            }
        }
        if (segmentTable.size > 255) return ByteArray(0)

        val numSegments = segmentTable.size
        val headerSize = 27 + numSegments
        val totalSize = headerSize + page.payload.size
        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        buf.put("OggS".toByteArray())
        buf.put(0)                         // version
        val headerType = when {
            page.isContinuation -> 0x01
            page.pageSequence == 0 -> 0x02
            else -> 0x00
        }
        buf.put(headerType.toByte())
        buf.putLong(0)                     // granule position
        buf.putInt(page.serialNumber)
        buf.putInt(page.pageSequence)
        buf.putInt(0)                      // CRC — zeroed for calculation
        buf.put(numSegments.toByte())
        for (s in segmentTable) buf.put(s)
        buf.put(page.payload)

        val crc = oggCrc(buf.array(), totalSize)
        putIntLE(buf.array(), 22, crc)

        return buf.array()
    }

    // ---- OGG checksum ----

    // OGG uses a direct CRC-32: polynomial 0x04c11db7, zero initial value, no
    // input/output reflection and no final XOR. java.util.zip.CRC32 is the
    // reflected IEEE 802.3 variant and produces a completely different value —
    // using it wrote a bad checksum into every page, which is why any tag edit
    // left the file undecodable ("CRC mismatch").
    private val crcTable = IntArray(256) { index ->
        var r = index shl 24
        repeat(8) {
            r = if (r and 0x80000000.toInt() != 0) (r shl 1) xor 0x04c11db7 else r shl 1
        }
        r
    }

    internal fun oggCrc(data: ByteArray, length: Int): Int {
        var crc = 0
        for (i in 0 until length) {
            val index = ((crc ushr 24) xor (data[i].toInt() and 0xFF)) and 0xFF
            crc = (crc shl 8) xor crcTable[index]
        }
        return crc
    }
}
