package com.dn0ne.player.app.domain.replaygain

import kotlin.math.abs

private const val MAX_ABS_GAIN_DB = 60f

// ReplayGain 2.0 targets -18 LUFS, Opus R128 tags target -23 LUFS (rsgain README, RFC 7845).
// Adding 5 dB puts R128 values on the ReplayGain scale. Without it every Opus track plays
// about 5 dB quieter than the rest of the library. Do not "clean this up".
private const val R128_TO_REPLAYGAIN_OFFSET_DB = 5f

private val DECIMAL = Regex("""[+-]?(\d+(\.\d*)?|\.\d+)""")
private val DB_SUFFIX = Regex("""\s*db\s*$""", RegexOption.IGNORE_CASE)

/** `"-7.85 dB"`, `"+3.2dB"`, `"-7.85"`, `"0,5 dB"`. Null for anything else or beyond ±60 dB. */
fun parseRgGainDb(raw: String): Float? {
    val s = raw.trim().replace(',', '.').replace(DB_SUFFIX, "").trim()
    if (!DECIMAL.matches(s)) return null
    return s.toFloatOrNull()?.takeIf { it.isFinite() && abs(it) <= MAX_ABS_GAIN_DB }
}

/** Unitless sample peak, 1.0 = full scale. Values above 1 are valid (float sources). */
fun parseRgPeak(raw: String): Float? {
    val s = raw.trim().replace(',', '.')
    if (!DECIMAL.matches(s)) return null
    return s.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }
}

/** Opus `R128_*_GAIN`: a Q7.8 integer (value / 256 = dB), converted to the ReplayGain scale. */
fun parseR128GainDb(raw: String): Float? {
    val q78 = raw.trim().toIntOrNull() ?: return null
    if (q78 !in Short.MIN_VALUE..Short.MAX_VALUE) return null
    return (q78 / 256f + R128_TO_REPLAYGAIN_OFFSET_DB).takeIf { abs(it) <= MAX_ABS_GAIN_DB }
}

/**
 * Builds a [TagState] from raw tag fields in file order. A key may carry a container
 * prefix (`TXXX:`, `----:com.apple.iTunes:`); only the text after the last `:` counts,
 * case-insensitively. The first valid value for a key wins. R128 beats ReplayGain for
 * the gain. The peak always comes from the same tier as the gain.
 */
fun tagStateFromFields(fields: List<Pair<String, String>>): TagState {
    fun firstValid(key: String, parse: (String) -> Float?): Float? =
        fields.asSequence()
            .filter { (k, _) -> k.substringAfterLast(':').equals(key, ignoreCase = true) }
            .firstNotNullOfOrNull { (_, v) -> parse(v) }

    fun tier(name: String): GainPeak? {
        val gain = firstValid("R128_${name}_GAIN", ::parseR128GainDb)
            ?: firstValid("REPLAYGAIN_${name}_GAIN", ::parseRgGainDb)
            ?: return null
        return GainPeak(gain, firstValid("REPLAYGAIN_${name}_PEAK", ::parseRgPeak))
    }

    val track = tier("TRACK")
    val album = tier("ALBUM")
    return if (track == null && album == null) {
        TagState.Untagged
    } else {
        TagState.Tagged(ReplayGainTags(track, album))
    }
}
