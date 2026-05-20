package com.dn0ne.player.app.domain.playback

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ShuffleEngineTest {

    private fun artistForIndex(map: Map<Int, String>): (Int) -> String = { map[it] ?: "artist_$it" }
    private fun albumForIndex(map: Map<Int, String>): (Int) -> String = { map[it] ?: "album_$it" }

    // ---- Pure mode ----

    @Test
    fun `pure shuffle with seeded RNG is deterministic`() {
        val engine1 = ShuffleEngine(Random(42))
        val result1 = engine1.generateOrder(3, PlaybackMode.Shuffle)
        val engine2 = ShuffleEngine(Random(42))
        val result2 = engine2.generateOrder(3, PlaybackMode.Shuffle)
        assertArrayEquals(result1, result2)
    }

    @Test
    fun `pure shuffle on 1 track returns single index`() {
        val engine = ShuffleEngine(Random(42))
        val result = engine.generateOrder(1, PlaybackMode.Shuffle)
        assertArrayEquals(intArrayOf(0), result)
    }

    @Test
    fun `pure shuffle on 0 tracks returns empty`() {
        val engine = ShuffleEngine(Random(42))
        val result = engine.generateOrder(0, PlaybackMode.Shuffle)
        assertEquals(0, result.size)
    }

    @Test
    fun `pure shuffle on 3 tracks produces all indices`() {
        val engine = ShuffleEngine(Random(42))
        val result = engine.generateOrder(3, PlaybackMode.Shuffle)
        assertEquals(3, result.size)
        val sorted = result.sortedArray()
        assertArrayEquals(intArrayOf(0, 1, 2), sorted)
    }

    @Test
    fun `chi-squared test for pure shuffle uniformity on 3 tracks`() {
        val n = 3
        val trials = 600
        val expected = trials / 6 // 6 possible permutations of 3 items
        val counts = mutableMapOf<List<Int>, Int>()

        for (i in 0 until trials) {
            val engine = ShuffleEngine(Random(i))
            val result = engine.generateOrder(n, PlaybackMode.Shuffle).toList()
            counts[result] = (counts[result] ?: 0) + 1
        }

        val chiSq = counts.values.sumOf { count ->
            val diff = count - expected
            diff.toDouble() * diff / expected
        }

        // Critical value for χ² with df=5 at α=0.01 is 15.086
        assertTrue("χ² = $chiSq exceeds critical value 15.086", chiSq < 15.086)
    }

    // ---- Smart mode ----

    @Test
    fun `smart shuffle with seeded RNG is deterministic`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "B", 2 to "C"))
        val albums = albumForIndex(mapOf(0 to "a1", 1 to "b1", 2 to "c1"))

        val engine1 = ShuffleEngine(Random(42))
        val result1 = engine1.generateOrder(3, PlaybackMode.SmartShuffle, artists, albums)
        val engine2 = ShuffleEngine(Random(42))
        val result2 = engine2.generateOrder(3, PlaybackMode.SmartShuffle, artists, albums)

        assertArrayEquals(result1, result2)
    }

    @Test
    fun `smart shuffle on 1 track returns single index`() {
        val engine = ShuffleEngine(Random(42))
        val result = engine.generateOrder(1, PlaybackMode.SmartShuffle)
        assertArrayEquals(intArrayOf(0), result)
    }

    @Test
    fun `smart shuffle on 0 tracks returns empty`() {
        val engine = ShuffleEngine(Random(42))
        val result = engine.generateOrder(0, PlaybackMode.SmartShuffle)
        assertEquals(0, result.size)
    }

    @Test
    fun `smart shuffle on 3 tracks produces all indices`() {
        val engine = ShuffleEngine(Random(42))
        val result = engine.generateOrder(3, PlaybackMode.SmartShuffle)
        assertEquals(3, result.size)
        val sorted = result.sortedArray()
        assertArrayEquals(intArrayOf(0, 1, 2), sorted)
    }

    @Test
    fun `smart shuffle selects lowest-penalty candidate`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "A", 2 to "B", 3 to "B"))
        val albums = albumForIndex(emptyMap())
        val engine = ShuffleEngine()

        // Candidate 1: A A B B — two same-artist adjacencies (positions 0-1, 2-3) = 20
        val c1 = intArrayOf(0, 1, 2, 3)
        // Candidate 2: A B A B — zero same-artist adjacencies = 0
        val c2 = intArrayOf(0, 2, 1, 3)
        // Candidate 3: A B B A — one same-artist adjacency (positions 1-2) = 10
        val c3 = intArrayOf(0, 2, 3, 1)

        val best = engine.selectBestCandidate(listOf(c1, c2, c3), artists, albums, emptySet())
        assertArrayEquals(intArrayOf(0, 2, 1, 3), best)
    }

    @Test
    fun `smart shuffle reduces artist adjacency vs pure shuffle`() {
        // 5 artists, 10 tracks each = 50 tracks
        val artists = artistForIndex(
            (0..49).associateWith { ('A' + it / 10).toString() }
        )
        val albums = albumForIndex(emptyMap())
        val trials = 200

        fun countArtistAdjacencies(order: IntArray): Int {
            var count = 0
            for (i in 0 until order.size - 1) {
                if (artists(order[i]) == artists(order[i + 1])) count++
            }
            return count
        }

        val pureAdjacencies = mutableListOf<Int>()
        val smartAdjacencies = mutableListOf<Int>()

        for (i in 0 until trials) {
            val seed = i * 2
            val pureEngine = ShuffleEngine(Random(seed))
            val smartEngine = ShuffleEngine(Random(seed + 1))

            val pure = pureEngine.generateOrder(50, PlaybackMode.Shuffle)
            val smart = smartEngine.generateOrder(50, PlaybackMode.SmartShuffle, artists, albums)

            pureAdjacencies.add(countArtistAdjacencies(pure))
            smartAdjacencies.add(countArtistAdjacencies(smart))
        }

        val pureMean = pureAdjacencies.average()
        val smartMean = smartAdjacencies.average()

        assertTrue(
            "Smart mean ($smartMean) should be less than pure mean ($pureMean)",
            smartMean < pureMean
        )
    }

    @Test
    fun `smart shuffle album penalty independent of artist penalty`() {
        // Two tracks: same artist AND same album
        // Penalty should only be ARTIST_ADJACENCY_WEIGHT (10), not 10 + 3
        val artists = artistForIndex(mapOf(0 to "X", 1 to "X"))
        val albums = albumForIndex(mapOf(0 to "y", 1 to "y"))
        val engine = ShuffleEngine()

        val penalty = engine.calculatePenalty(intArrayOf(0, 1), artists, albums, emptySet())
        assertEquals(10, penalty)
    }

    @Test
    fun `smart shuffle recent position penalty decays toward end`() {
        val artists = artistForIndex(emptyMap())
        val albums = albumForIndex(emptyMap())
        val previousIndices = setOf(0)
        val engine = ShuffleEngine()

        // Track 0 at position 0: penalty = 5 * (1.0 - 0.0/3) = 5
        val penaltyStart = engine.calculatePenalty(intArrayOf(0, 1, 2), artists, albums, previousIndices)
        // Track 0 at position 2: penalty = 5 * (1.0 - 2.0/3) = 5 * 0.333... = 1
        val penaltyEnd = engine.calculatePenalty(intArrayOf(1, 2, 0), artists, albums, previousIndices)

        assertTrue("Penalty at position 0 should be higher", penaltyStart > penaltyEnd)
    }

    // ---- Penalty function ----

    @Test
    fun `zero penalty for perfect artist alternation`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "B", 2 to "A", 3 to "B"))
        val albums = albumForIndex(emptyMap())
        val engine = ShuffleEngine()

        val penalty = engine.calculatePenalty(intArrayOf(0, 1, 2, 3), artists, albums, emptySet())
        assertEquals(0, penalty)
    }

    @Test
    fun `artist adjacency penalty scales linearly`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "A", 2 to "A"))
        val albums = albumForIndex(emptyMap())
        val engine = ShuffleEngine()

        // Three adjacent same-artist tracks: positions (0,1) and (1,2) = 2 × 10 = 20
        val penalty = engine.calculatePenalty(intArrayOf(0, 1, 2), artists, albums, emptySet())
        assertEquals(20, penalty)
    }

    @Test
    fun `same artist same album avoids double count`() {
        // 3 tracks: all same artist, first two same album
        val artists = artistForIndex(mapOf(0 to "A", 1 to "A", 2 to "A"))
        val albums = albumForIndex(mapOf(0 to "x", 1 to "x", 2 to "y"))
        val engine = ShuffleEngine()

        // Positions (0,1): same artist AND same album → artist weight only = 10
        // Positions (1,2): same artist, different album → artist weight only = 10
        // Total = 20, NOT 10 + 3 + 10 = 23
        val penalty = engine.calculatePenalty(intArrayOf(0, 1, 2), artists, albums, emptySet())
        assertEquals(20, penalty)
    }

    @Test
    fun `album adjacency penalty applied when artists differ`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "B"))
        val albums = albumForIndex(mapOf(0 to "x", 1 to "x"))
        val engine = ShuffleEngine()

        // Different artists, same album → album weight only = 3
        val penalty = engine.calculatePenalty(intArrayOf(0, 1), artists, albums, emptySet())
        assertEquals(3, penalty)
    }

    @Test
    fun `recent position penalty is highest at position 0`() {
        val artists = artistForIndex(emptyMap())
        val albums = albumForIndex(emptyMap())
        val previousIndices = setOf(0)
        val engine = ShuffleEngine()

        val penaltyAt0 = engine.calculatePenalty(intArrayOf(0, 1), artists, albums, previousIndices)
        // Track 0 at position 0: 5 * (1.0 - 0/2) = 5
        assertEquals(5, penaltyAt0)
    }

    @Test
    fun `recent position penalty near zero at last position`() {
        val artists = artistForIndex(emptyMap())
        val albums = albumForIndex(emptyMap())
        val previousIndices = setOf(0)
        val engine = ShuffleEngine()

        val penalty = engine.calculatePenalty(intArrayOf(1, 0), artists, albums, previousIndices)
        // Track 0 at position 1 of 2: 5 * (1.0 - 1.0/2) = 5 * 0.5 = 2
        assertEquals(2, penalty)
    }

    // ---- Edge cases ----

    @Test
    fun `all tracks same artist smart mode produces valid permutation`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "A", 2 to "A", 3 to "A"))
        val albums = albumForIndex(emptyMap())
        val engine = ShuffleEngine(Random(42))

        val result = engine.generateOrder(4, PlaybackMode.SmartShuffle, artists, albums)
        assertEquals(4, result.size)
        val sorted = result.sortedArray()
        assertArrayEquals(intArrayOf(0, 1, 2, 3), sorted)
    }

    @Test
    fun `all tracks different artists both modes zero adjacency`() {
        val artists = artistForIndex(
            (0..9).associateWith { ('A' + it).toString() }
        )
        val albums = albumForIndex(emptyMap())
        val engine = ShuffleEngine(Random(42))

        val result = engine.generateOrder(10, PlaybackMode.SmartShuffle, artists, albums)
        assertEquals(10, result.size)

        // Verify no artist adjacencies in the result
        for (i in 0 until result.size - 1) {
            assertNotEquals(
                "Adjacent artists at position $i",
                artists(result[i]), artists(result[i + 1])
            )
        }
    }

    @Test(timeout = 2000)
    fun `large playlist benchmark completes under 50ms`() {
        val n = 10_000
        val artists = artistForIndex((0 until n).associateWith { "Artist${it % 500}" })
        val albums = albumForIndex((0 until n).associateWith { "Album${it % 200}" })
        val previousIndices = (0 until n step 3).toSet()

        val engine = ShuffleEngine(Random(42))

        val start = System.nanoTime()
        val result = engine.generateOrder(n, PlaybackMode.SmartShuffle, artists, albums, previousIndices)
        val elapsed = System.nanoTime() - start

        assertEquals(n, result.size)
        val sorted = result.sortedArray()
        // Verify all indices present
        for (i in 0 until n) {
            assertEquals(i, sorted[i])
        }

        val elapsedMs = elapsed / 1_000_000.0
        assertTrue("Elapsed ${elapsedMs}ms exceeds 200ms budget", elapsedMs < 200.0)
    }

    @Test
    fun `smart shuffle does not degenerate to fixed order`() {
        val artists = artistForIndex(
            (0..19).associateWith { ('A' + it / 4).toString() }
        )
        val albums = albumForIndex(emptyMap())
        val trials = 100

        val results = mutableSetOf<List<Int>>()
        for (i in 0 until trials) {
            val engine = ShuffleEngine(Random(i * 7 + 13))
            val order = engine.generateOrder(20, PlaybackMode.SmartShuffle, artists, albums)
            results.add(order.toList())
        }

        // At least 90 unique permutations out of 100 trials
        assertTrue(
            "Only ${results.size}/$trials unique permutations (need >90)",
            results.size > 90
        )
    }

    // ---- Repeat mode (non-shuffle fallback) ----

    @Test
    fun `repeat mode returns sequential order`() {
        val engine = ShuffleEngine(Random(42))
        val result = engine.generateOrder(5, PlaybackMode.Repeat)
        assertArrayEquals(intArrayOf(0, 1, 2, 3, 4), result)
    }

    @Test
    fun `repeat one mode returns sequential order`() {
        val engine = ShuffleEngine(Random(42))
        val result = engine.generateOrder(5, PlaybackMode.RepeatOne)
        assertArrayEquals(intArrayOf(0, 1, 2, 3, 4), result)
    }
}
