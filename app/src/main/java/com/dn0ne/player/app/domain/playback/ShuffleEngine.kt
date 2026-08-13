package com.dn0ne.player.app.domain.playback

import kotlin.math.pow
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

    /**
     * Two stages. The deal lays the tracks out so that no artist follows itself,
     * which it can always do when the library allows it at all. The polish then
     * trades that layout's regularity for something that looks shuffled, taking
     * only swaps that leave the order no worse, and picking up the album and
     * recency terms that the deal knows nothing about.
     */
    private fun generateSmartOrder(
        trackCount: Int,
        artistForIndex: (Int) -> String,
        albumForIndex: (Int) -> String,
        previousLoopIndices: Set<Int>,
    ): IntArray {
        if (trackCount < 2) return IntArray(trackCount) { it }

        val order = dealByArtistFrequency(trackCount, artistForIndex)

        repeat(polishBudget(trackCount)) {
            val i = random.nextInt(trackCount)
            // Drawn from the other positions, so a swap is never a no-op.
            var j = random.nextInt(trackCount - 1)
            if (j >= i) j++

            if (deltaPenalty(order, i, j, artistForIndex, albumForIndex, previousLoopIndices) <= 0) {
                swap(order, i, j)
            }
        }

        return order
    }

    /**
     * Deals the tracks out like cards: biggest artist first, one at a time into
     * every other slot, wrapping onto the odd slots once the even ones are full.
     * Handing out the most crowded artist first is what guarantees its tracks end
     * up furthest apart, and no artist can land next to itself unless it holds
     * more than half the queue.
     *
     * A blank artist tag means unknown rather than shared, so those tracks are
     * dealt individually instead of being herded together as one artist. That is
     * a correctness point about what a missing tag means, not a measurable one:
     * grouping them scores the same, because blank pairs carry no penalty either
     * way.
     */
    private fun dealByArtistFrequency(
        trackCount: Int,
        artistForIndex: (Int) -> String,
    ): IntArray {
        val groupOfTrack = IntArray(trackCount)
        val groupIndexByArtist = HashMap<String, Int>()
        var groupCount = 0

        for (track in 0 until trackCount) {
            val artist = artistForIndex(track)
            groupOfTrack[track] = if (artist.isBlank()) {
                groupCount++
            } else {
                groupIndexByArtist.getOrPut(artist) { groupCount++ }
            }
        }

        val sizes = IntArray(groupCount)
        for (track in 0 until trackCount) sizes[groupOfTrack[track]]++

        // Shuffled first so that equal-sized artists are dealt in a different
        // order each time, then sorted by size - a stable sort keeps that shuffle
        // as the tie-break.
        val groupsBySize = IntArray(groupCount) { it }
        groupsBySize.shuffle(random)
        val sorted = groupsBySize.toTypedArray()
        sorted.sortByDescending { sizes[it] }

        // Tracks bucketed by group, each bucket shuffled, then read out in
        // descending-size order to give the deal sequence.
        val bucketStart = IntArray(groupCount + 1)
        for (group in 0 until groupCount) bucketStart[group + 1] = bucketStart[group] + sizes[group]
        val cursor = bucketStart.copyOf()
        val bucketed = IntArray(trackCount)
        for (track in 0 until trackCount) {
            val group = groupOfTrack[track]
            bucketed[cursor[group]++] = track
        }
        for (group in 0 until groupCount) {
            shuffleRange(bucketed, bucketStart[group], bucketStart[group + 1])
        }

        val result = IntArray(trackCount)
        val evenSlots = (trackCount + 1) / 2
        var dealt = 0
        for (group in sorted) {
            for (position in bucketStart[group] until bucketStart[group + 1]) {
                val slot = if (dealt < evenSlots) {
                    2 * dealt
                } else {
                    2 * (dealt - evenSlots) + 1
                }
                result[slot] = bucketed[position]
                dealt++
            }
        }

        return result
    }

    private fun shuffleRange(array: IntArray, fromIndex: Int, toIndex: Int) {
        for (i in toIndex - 1 downTo fromIndex + 1) {
            val j = fromIndex + random.nextInt(i - fromIndex + 1)
            val temp = array[i]
            array[i] = array[j]
            array[j] = temp
        }
    }

    /**
     * Enough swaps to shake the regularity out of the deal, capped so a very long
     * queue does not pay for scrambling nobody can perceive at that length.
     */
    private fun polishBudget(trackCount: Int): Int =
        (POLISH_SWAPS_PER_TRACK * trackCount).coerceAtMost(MAX_POLISH_SWAPS)

    /**
     * What swapping positions [i] and [j] would do to [calculatePenalty], without
     * rescoring the whole order. Only the pairs touching those two positions and
     * the recency weights of the two moved tracks can change.
     *
     * Leaves [order] as it found it.
     */
    internal fun deltaPenalty(
        order: IntArray,
        i: Int,
        j: Int,
        artistForIndex: (Int) -> String,
        albumForIndex: (Int) -> String,
        previousLoopIndices: Set<Int>,
    ): Int {
        if (i == j) return 0

        val low = minOf(i, j)
        val high = maxOf(i, j)

        val before = adjacencyAround(order, low, high, artistForIndex, albumForIndex)
        swap(order, low, high)
        val after = adjacencyAround(order, low, high, artistForIndex, albumForIndex)
        swap(order, low, high)

        var delta = after - before

        val lowWeight = recencyWeight(low, order.size)
        val highWeight = recencyWeight(high, order.size)
        if (order[low] in previousLoopIndices) delta += highWeight - lowWeight
        if (order[high] in previousLoopIndices) delta += lowWeight - highWeight

        return delta
    }

    /**
     * Adjacency penalty of every pair that positions [low] and [high] take part in.
     * The pair starting at [low] is the same pair as the one starting at
     * `high - 1` when the two positions are neighbours, so that one is counted once.
     *
     * Counting it twice would currently cancel out, because every term in
     * [pairPenalty] compares two tracks symmetrically and a neighbour swap only
     * reverses that pair. The guard is here so the sum stays a true count if a
     * term is ever added that cares which track comes first.
     */
    private fun adjacencyAround(
        order: IntArray,
        low: Int,
        high: Int,
        artistForIndex: (Int) -> String,
        albumForIndex: (Int) -> String,
    ): Int {
        var sum = pairPenaltyAt(order, low - 1, artistForIndex, albumForIndex) +
                pairPenaltyAt(order, low, artistForIndex, albumForIndex) +
                pairPenaltyAt(order, high, artistForIndex, albumForIndex)

        if (high - 1 != low) {
            sum += pairPenaltyAt(order, high - 1, artistForIndex, albumForIndex)
        }

        return sum
    }

    private fun pairPenaltyAt(
        order: IntArray,
        position: Int,
        artistForIndex: (Int) -> String,
        albumForIndex: (Int) -> String,
    ): Int {
        if (position < 0 || position + 1 >= order.size) return 0
        return pairPenalty(order[position], order[position + 1], artistForIndex, albumForIndex)
    }

    private fun pairPenalty(
        current: Int,
        next: Int,
        artistForIndex: (Int) -> String,
        albumForIndex: (Int) -> String,
    ): Int {
        val artistA = artistForIndex(current)
        val artistB = artistForIndex(next)
        if (artistA.isNotBlank() && artistA == artistB) {
            return ARTIST_ADJACENCY_WEIGHT
        }

        val albumA = albumForIndex(current)
        val albumB = albumForIndex(next)
        if (albumA.isNotBlank() && albumA == albumB) {
            return ALBUM_ADJACENCY_WEIGHT
        }

        return 0
    }

    /**
     * Pressure to keep a just-played track away from the front of the new queue,
     * halving every quarter of the list. Recognising that something played a
     * moment ago fades steeply and then flattens, and tying the half-life to the
     * queue length keeps that shape the same whatever the queue size.
     */
    private fun recencyWeight(position: Int, trackCount: Int): Int {
        val halfLife = trackCount / RECENCY_HALF_LIFE_DIVISOR
        return (RECENT_POSITION_WEIGHT * 2.0.pow(-position / halfLife)).toInt()
    }

    private fun swap(order: IntArray, i: Int, j: Int) {
        val temp = order[i]
        order[i] = order[j]
        order[j] = temp
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
            penalty += pairPenalty(order[i], order[i + 1], artistForIndex, albumForIndex)
        }

        for (i in 0 until n) {
            if (order[i] in previousLoopIndices) {
                penalty += recencyWeight(i, n)
            }
        }

        return penalty
    }

    companion object {
        private const val ARTIST_ADJACENCY_WEIGHT = 10
        private const val ALBUM_ADJACENCY_WEIGHT = 3
        private const val RECENT_POSITION_WEIGHT = 5
        private const val RECENCY_HALF_LIFE_DIVISOR = 4.0

        private const val POLISH_SWAPS_PER_TRACK = 2
        private const val MAX_POLISH_SWAPS = 4000
    }
}
