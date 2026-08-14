package com.dn0ne.player.app.domain.track

/**
 * Hides albums holding a single track from the Albums tab.
 *
 * Libraries assembled from loose files end up with hundreds of one-track
 * "albums" that bury the real ones. This only changes what the Albums tab
 * lists - the tracks themselves stay in the library, in search, and in
 * "go to album".
 */
fun List<Playlist>.withoutSingleTrackAlbums(enabled: Boolean): List<Playlist> =
    if (enabled) filter { it.trackList.size > 1 } else this
