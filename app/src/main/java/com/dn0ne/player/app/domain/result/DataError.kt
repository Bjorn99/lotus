package com.dn0ne.player.app.domain.result

sealed interface DataError: Error {
    enum class Network: DataError {
        BadRequest,
        Unauthorized,
        Forbidden,
        NotFound,
        RequestTimeout,

        InternalServerError,
        ServiceUnavailable,

        ParseError,
        NoInternet,
        Unknown
    }

    enum class Local: DataError {
        NoReadPermission,
        NoWritePermission,
        FailedToRead,
        FailedToWrite,
        NoLyricsFound,
        Unknown
    }
}