package com.dn0ne.player.app.di

import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class ResponseSizeCapTest {

    @Test
    fun `content length at cap passes`() {
        validateResponseSize(contentLength = 5_242_880L, maxBytes = 5_242_880L)
    }

    @Test
    fun `content length below cap passes`() {
        validateResponseSize(contentLength = 1_000_000L, maxBytes = 5_242_880L)
    }

    @Test
    fun `content length one byte over cap throws IOException`() {
        assertThrows(IOException::class.java) {
            validateResponseSize(contentLength = 5_242_881L, maxBytes = 5_242_880L)
        }
    }

    @Test
    fun `null content length passes through`() {
        validateResponseSize(contentLength = null, maxBytes = 5_242_880L)
    }

    @Test
    fun `zero content length passes`() {
        validateResponseSize(contentLength = 0L, maxBytes = 5_242_880L)
    }
}
