package com.dn0ne.player.app.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorKindTest {

    // ---- Specific Media3 constants ----

    @Test
    fun `ERROR_CODE_DECODING_FORMAT_UNSUPPORTED (4003) maps to UnsupportedFormat`() {
        assertEquals(PlaybackErrorKind.UnsupportedFormat, playbackErrorKind(4003))
    }

    @Test
    fun `ERROR_CODE_DECODER_INIT_FAILED (4001) maps to UnsupportedFormat`() {
        assertEquals(PlaybackErrorKind.UnsupportedFormat, playbackErrorKind(4001))
    }

    @Test
    fun `ERROR_CODE_IO_FILE_NOT_FOUND (2005) maps to FileUnreadable`() {
        assertEquals(PlaybackErrorKind.FileUnreadable, playbackErrorKind(2005))
    }

    @Test
    fun `ERROR_CODE_IO_NO_PERMISSION (2006) maps to FileUnreadable`() {
        assertEquals(PlaybackErrorKind.FileUnreadable, playbackErrorKind(2006))
    }

    // ---- Range sweeps ----

    @Test
    fun `every code in 2000-2999 maps to FileUnreadable`() {
        for (code in 2000..2999) {
            assertEquals(
                "code $code should be FileUnreadable",
                PlaybackErrorKind.FileUnreadable,
                playbackErrorKind(code)
            )
        }
    }

    @Test
    fun `every code in 3000-4999 maps to UnsupportedFormat`() {
        for (code in 3000..4999) {
            assertEquals(
                "code $code should be UnsupportedFormat",
                PlaybackErrorKind.UnsupportedFormat,
                playbackErrorKind(code)
            )
        }
    }

    // ---- Renderer range maps to Unknown (not a file problem) ----

    @Test
    fun `every code in 5000-5999 maps to Unknown`() {
        for (code in 5000..5999) {
            assertEquals(
                "code $code should be Unknown",
                PlaybackErrorKind.Unknown,
                playbackErrorKind(code)
            )
        }
    }

    // ---- DRM range maps to Unknown (not a file problem) ----

    @Test
    fun `every code in 6000-6999 maps to Unknown`() {
        for (code in 6000..6999) {
            assertEquals(
                "code $code should be Unknown",
                PlaybackErrorKind.Unknown,
                playbackErrorKind(code)
            )
        }
    }

    // ---- Out-of-range / edge cases ----

    @Test
    fun `code 0 maps to Unknown`() {
        assertEquals(PlaybackErrorKind.Unknown, playbackErrorKind(0))
    }

    @Test
    fun `negative code maps to Unknown`() {
        assertEquals(PlaybackErrorKind.Unknown, playbackErrorKind(-1))
    }

    @Test
    fun `code 1999 (below IO range) maps to Unknown`() {
        assertEquals(PlaybackErrorKind.Unknown, playbackErrorKind(1999))
    }

    @Test
    fun `code 7000 (above DRM range) maps to Unknown`() {
        assertEquals(PlaybackErrorKind.Unknown, playbackErrorKind(7000))
    }

    @Test
    fun `Int MAX_VALUE maps to Unknown`() {
        assertEquals(PlaybackErrorKind.Unknown, playbackErrorKind(Int.MAX_VALUE))
    }
}
