package com.dn0ne.player.app.data.remote.lyrics

import android.util.Log
import com.dn0ne.player.app.domain.result.DataError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import javax.net.ssl.SSLException

internal fun Throwable.toNetworkError(logTag: String): DataError.Network {
    return when (this) {
        is UnresolvedAddressException, is UnknownHostException -> {
            Log.i(logTag, "DNS / address resolution failed: $message")
            DataError.Network.NoInternet
        }
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException -> {
            DataError.Network.RequestTimeout
        }
        is SSLException -> {
            Log.w(logTag, "TLS handshake / cert error: $message")
            DataError.Network.Unknown
        }
        is SocketException, is IOException -> {
            Log.w(logTag, "Network I/O error: $message")
            DataError.Network.Unknown
        }
        else -> {
            Log.w(logTag, "Unexpected network failure", this)
            DataError.Network.Unknown
        }
    }
}
