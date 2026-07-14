package com.dn0ne.player.app.domain.lyrics

// Ordering for lyric source resolution, extracted from PlayerViewModel so the
// precedence rules can be unit-tested without Android, Room, or the network.
// Each source is a suspend supplier returning the lyrics it found, or null on
// a miss; the first non-null result wins.
//
// Precedence:
//   1. Sidecar .lrc — only when the user has a folder configured AND the
//      feature is enabled (#106 / #107). The curated folder is the source of
//      truth and is read fresh every call, so renames/edits reflect
//      immediately and a stale cache row can never shadow it.
//   2. Cache — persisted lyrics: previously-downloaded remote lyrics (the
//      offline-first payoff) plus anything the user explicitly attached
//      (picked/pasted, or copied from the tag). Automatic embedded/sidecar
//      reads are NOT cached, so the cache never silently shadows a fresh
//      local source.
//   3. Embedded tag — lyrics stored in the file's own tags, read fresh.
//   4. Remote — network provider, GATED and LAST: reached only when every
//      local source misses. Caching a remote hit is the remote supplier's own
//      responsibility, not this function's.
suspend fun resolveLyrics(
    sidecarEnabled: Boolean,
    sidecar: suspend () -> Lyrics?,
    cache: suspend () -> Lyrics?,
    embedded: suspend () -> Lyrics?,
    remote: suspend () -> Lyrics?,
): Lyrics? {
    if (sidecarEnabled) {
        sidecar()?.let { return it }
    }
    cache()?.let { return it }
    embedded()?.let { return it }
    return remote()
}
