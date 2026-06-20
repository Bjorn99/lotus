package com.dn0ne.player.app.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * Minimal OGG page-level tag editor for OPUS files.
 *
 * jaudiotagger 3.0.1 has no .opus extension mapping, and its OggFileReader
 * validates for Vorbis identification headers — it rejects OpusHead. This
 * class parses the OGG container directly, modifies the OpusTags
 * (VorbisComment) packet, recalculates page CRCs, and writes back.
 *
 * OPUS tag structure (RFC 7845 §5.2):
 *   Page 0: OpusHead (identification, untouched)
 *   Page 1: OpusTags  (VorbisComment fields — this is what we edit)
 *   Pages 2+: Audio data (untouched)
 *
 * Cover art (METADATA_BLOCK_PICTURE) is not yet supported and is passed
 * through unchanged if present.
 */
internal object OpusTagEditor {

    // ---- public API ----

    fun update(file: File, metadata: com.dn0ne.player.app.domain.metadata.Metadata) {
        val pages = readOggPages(file)
        if (pages.size < 2) return

        val fields = parseOpusTagsFields(pages[1])
        metadata.title?.let { fields["TITLE"] = it }
        metadata.album?.let { fields["ALBUM"] = it }
        metadata.artist?.let { fields["ARTIST"] = it }
        metadata.albumArtist?.let { fields["ALBUMARTIST"] = it }
        metadata.genre?.let { fields["GENRE"] = it }
        metadata.year?.let { fields["DATE"] = it }
        metadata.trackNumber?.let { fields["TRACKNUMBER"] = it }
        metadata.lyrics?.let { fields["LYRICS"] = it }
        metadata.mbAlbumId?.let { fields["MUSICBRAINZ_ALBUMID"] = it }
        metadata.mbReleaseGroupId?.let { fields["MUSICBRAINZ_RELEASEGROUPID"] = it }
        metadata.mbAlbumArtistId?.let { fields["MUSICBRAINZ_ALBUMARTISTID"] = it }

        val vendorString = parseOpusTagsVendor(pages[1])
        val newPage = buildOpusTagsPage(
            vendorString = vendorString,
            fields = fields,
            serialNumber = pages[1].serialNumber,
            pageSequence = pages[1].pageSequence,
        )

        writePage(file, pageIndex = 1, newPage)
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

                val segmentTable = ByteArray(numSegments)
                raf.read(segmentTable)

                var payloadSize = 0
                for (s in segmentTable) payloadSize += (s.toInt() and 0xFF)
                val payload = ByteArray(payloadSize)
                raf.read(payload)

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

    // ---- OpusTags parsing ----

    private fun parseOpusTagsFields(page: OggPage): MutableMap<String, String> {
        val buf = ByteBuffer.wrap(page.payload).order(ByteOrder.LITTLE_ENDIAN)
        val magic = ByteArray(8)
        buf.get(magic)
        if (String(magic) != "OpusTags") return mutableMapOf()

        val vendorLength = buf.int
        buf.position(buf.position() + vendorLength)

        val numComments = buf.int
        val fields = mutableMapOf<String, String>()
        for (i in 0 until numComments.coerceAtMost(Int.MAX_VALUE.toInt())) {
            val len = buf.int
            if (len < 0 || buf.remaining() < len) break
            val raw = ByteArray(len)
            buf.get(raw)
            val entry = String(raw, Charsets.UTF_8)
            val eq = entry.indexOf('=')
            if (eq > 0) {
                fields[entry.substring(0, eq).uppercase()] = entry.substring(eq + 1)
            }
        }
        return fields
    }

    private fun parseOpusTagsVendor(page: OggPage): String {
        val buf = ByteBuffer.wrap(page.payload).order(ByteOrder.LITTLE_ENDIAN)
        val magic = ByteArray(8)
        buf.get(magic)
        if (String(magic) != "OpusTags") return ""
        val vendorLength = buf.int
        val vendorBytes = ByteArray(vendorLength)
        buf.get(vendorBytes)
        return String(vendorBytes, Charsets.UTF_8)
    }

    // ---- OpusTags page building ----

    private fun buildOpusTagsPage(
        vendorString: String,
        fields: Map<String, String>,
        serialNumber: Int,
        pageSequence: Int,
    ): OggPage {
        val vendorBytes = vendorString.toByteArray(Charsets.UTF_8)

        val commentBytes = fields.map { (key, value) ->
            val line = "${key.uppercase()}=$value"
            line.toByteArray(Charsets.UTF_8)
        }

        val payloadSize = 8 + 4 + vendorBytes.size + 4 +
            commentBytes.sumOf { 4 + it.size }

        val payload = ByteBuffer.allocate(payloadSize).order(ByteOrder.LITTLE_ENDIAN)
        payload.put("OpusTags".toByteArray(Charsets.UTF_8))
        payload.putInt(vendorBytes.size)
        payload.put(vendorBytes)
        payload.putInt(commentBytes.size)
        for (c in commentBytes) {
            payload.putInt(c.size)
            payload.put(c)
        }

        return OggPage(
            offset = 0,
            headerSize = 0, // filled by writePage
            payload = payload.array(),
            serialNumber = serialNumber,
            pageSequence = pageSequence,
            isContinuation = false,
        )
    }

    // ---- OGG page writing ----

    internal fun writePage(file: File, pageIndex: Int, newPage: OggPage) {
        // Read all pages so we can compute the new page's offset and the
        // byte range that must shift when the page size changes.
        val pages = readOggPages(file)
        if (pageIndex >= pages.size) return

        val oldPage = pages[pageIndex]
        val pageStartOffset = oldPage.offset
        val oldPageSize = oldPage.headerSize + oldPage.payload.size
        val oldPageEndOffset = pageStartOffset + oldPageSize

        // Build the new OGG page (header + segment table + payload)
        val newPageBytes = buildOggPageBytes(newPage)
        val sizeDelta = newPageBytes.size - oldPageSize

        RandomAccessFile(file, "rw").use { raf ->
            if (sizeDelta == 0) {
                // In-place replacement
                raf.seek(pageStartOffset)
                raf.write(newPageBytes)
            } else {
                // Read everything after the old page, shift by sizeDelta
                val fileLen = raf.length()
                val tailSize = (fileLen - oldPageEndOffset).toInt()
                val tail = ByteArray(tailSize)
                raf.seek(oldPageEndOffset)
                raf.read(tail)

                // Write new page at the same offset
                raf.seek(pageStartOffset)
                raf.write(newPageBytes)

                // Write the shifted tail
                raf.write(tail)
                raf.setLength(fileLen + sizeDelta)
            }
        }
    }

    internal fun buildOggPageBytes(page: OggPage): ByteArray {
        // Segment table per RFC 3533: split payload into 255-byte lacing
        // values. A 255-byte segment MUST be followed by a zero-length
        // segment to signal end-of-packet.
        val segmentTable = mutableListOf<Byte>()
        var remaining = page.payload.size
        if (remaining == 0) {
            segmentTable.add(0)
        } else {
            while (remaining > 0) {
                val len = remaining.coerceAtMost(255)
                segmentTable.add(len.toByte())
                remaining -= len
                if (len == 255 && remaining == 0) {
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
        val headerType = if (page.pageSequence == 0) 0x02 else 0x00
        buf.put(headerType.toByte())
        buf.putLong(0)                     // granule position
        buf.putInt(page.serialNumber)
        buf.putInt(page.pageSequence)
        buf.putInt(0)                      // CRC — zeroed for calculation
        buf.put(numSegments.toByte())
        for (s in segmentTable) buf.put(s)
        // Payload
        buf.put(page.payload)

        // Calculate CRC over the entire page with CRC field zeroed
        val crc = CRC32()
        crc.update(buf.array(), 0, totalSize)
        val crcValue = crc.value.toInt()

        // Write CRC at offset 22
        val crcBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(crcValue).array()
        System.arraycopy(crcBytes, 0, buf.array(), 22, 4)

        return buf.array()
    }
}
