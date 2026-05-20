package com.dn0ne.player.app.domain.playback

import kotlin.random.Random

class ShuffleEngine(private val random: Random = Random) {

    fun generateOrder(
        trackCount: Int,
        mode: PlaybackMode,
        artistForIndex: (Int) -> String = { "" },
        albumForIndex: (Int) -> String = { "" },
        previousLoopIndices: Set<Int> = emptySet(),
    ): IntArray {
        return when (mode) {
            PlaybackMode.Shuffle -> generatePureOrder(trackCount)
            PlaybackMode.SmartShuffle -> generateSmartOrder(
                trackCount, artistForIndex, albumForIndex, previousLoopIndices
            )
            else -> IntArray(trackCount) { it }
        }
    }

    private fun generatePureOrder(trackCount: Int): IntArray {
        if (trackCount == 0) return IntArray(0)
        val indices = IntArray(trackCount) { it }
        indices.shuffle(random)
        return indices
    }

    private fun generateSmartOrder(
        trackCount: Int,
        artistForIndex: (Int) -> String,
        albumForIndex: (Int) -> String,
        previousLoopIndices: Set<Int>,
    ): IntArray {
        var bestOrder: IntArray? = null
        var bestPenalty = Int.MAX_VALUE

        repeat(CANDIDATE_COUNT) {
            val candidate = generatePureOrder(trackCount)
            val penalty = calculatePenalty(
                candidate, artistForIndex, albumForIndex, previousLoopIndices
            )
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestOrder = candidate
            }
        }

        return bestOrder ?: generatePureOrder(trackCount)
    }

    internal fun selectBestCandidate(
        candidates: List<IntArray>,
        artistForIndex: (Int) -> String,
        albumForIndex: (Int) -> String,
        previousLoopIndices: Set<Int>,
    ): IntArray {
        return candidates.minBy {
            calculatePenalty(it, artistForIndex, albumForIndex, previousLoopIndices)
        }
    }

    internal fun calculatePenalty(
        order: IntArray,
        artistForIndex: (Int) -> String,
        albumForIndex: (Int) -> String,
        previousLoopIndices: Set<Int>,
    ): Int {
        var penalty = 0
        val n = order.size
        if (n < 2) return 0

        for (i in 0 until n - 1) {
            val current = order[i]
            val next = order[i + 1]
            val sameArtist = artistForIndex(current) == artistForIndex(next)
            if (sameArtist) {
                penalty += ARTIST_ADJACENCY_WEIGHT
            } else {
                val sameAlbum = albumForIndex(current) == albumForIndex(next)
                if (sameAlbum) {
                    penalty += ALBUM_ADJACENCY_WEIGHT
                }
            }
        }

        for (i in 0 until n) {
            if (order[i] in previousLoopIndices) {
                penalty += (RECENT_POSITION_WEIGHT * (1.0 - i.toDouble() / n)).toInt()
            }
        }

        return penalty
    }

    companion object {
        private const val CANDIDATE_COUNT = 5
        private const val ARTIST_ADJACENCY_WEIGHT = 10
        private const val ALBUM_ADJACENCY_WEIGHT = 3
        private const val RECENT_POSITION_WEIGHT = 5
    }
}
