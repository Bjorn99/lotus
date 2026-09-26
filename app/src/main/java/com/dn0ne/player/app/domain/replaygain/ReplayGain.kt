package com.dn0ne.player.app.domain.replaygain

import kotlin.math.pow

enum class ReplayGainMode {
    OFF, TRACK, ALBUM;

    companion object {
        // Persisted by name, never ordinal. Anything unrecognised means OFF so an
        // unexpected value can never change someone's loudness.
        fun fromStoredName(name: String?): ReplayGainMode =
            entries.firstOrNull { it.name == name } ?: OFF
    }
}

/** One tier's gain and the peak measured for that same tier. The two never mix. */
data class GainPeak(val gainDb: Float, val peak: Float?)

data class ReplayGainTags(val track: GainPeak?, val album: GainPeak?)

sealed interface TagState {
    /** Not read yet. Plays at unity and must not get the untagged fallback. */
    data object Unknown : TagState

    /** Read, and the file carries no usable ReplayGain or R128 tag. */
    data object Untagged : TagState

    data class Tagged(val tags: ReplayGainTags) : TagState
}

val PREAMP_RANGE_DB = -15f..15f
val FALLBACK_RANGE_DB = -15f..0f

/** -60 dB. A corrupt tag can make a track very quiet, never silent. */
const val MIN_GAIN_FACTOR = 0.001f

fun dbToFactor(db: Float): Float = 10f.pow(db / 20f)

fun resolveGainFactor(
    state: TagState,
    mode: ReplayGainMode,
    preAmpDb: Float,
    fallbackDb: Float,
): Float {
    if (mode == ReplayGainMode.OFF || state == TagState.Unknown) return 1f

    val tier = (state as? TagState.Tagged)?.tags?.let { tags ->
        when (mode) {
            ReplayGainMode.TRACK -> tags.track ?: tags.album
            ReplayGainMode.ALBUM -> tags.album ?: tags.track
            ReplayGainMode.OFF -> null
        }
    }

    val factor = if (tier == null) {
        // The pre-amp is for tagged tracks only; untagged ones get the fallback alone
        // and it never boosts, since there is no peak to bound it.
        minOf(dbToFactor(fallbackDb), 1f)
    } else {
        // Without a peak there is no way to know a boost is safe: attenuate freely,
        // never boost blind. Every Opus file lands here (R128 has no peak tags).
        val cap = tier.peak?.takeIf { it > 0f }?.let { 1f / it } ?: 1f
        minOf(dbToFactor(tier.gainDb + preAmpDb), cap)
    }
    return factor.coerceAtLeast(MIN_GAIN_FACTOR)
}
