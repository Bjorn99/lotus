package com.dn0ne.player.app.domain.playback

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt
import kotlin.random.Random

class ShuffleEngineTest {

    private fun artistForIndex(map: Map<Int, String>): (Int) -> String = { map[it] ?: "artist_$it" }
    private fun albumForIndex(map: Map<Int, String>): (Int) -> String = { map[it] ?: "album_$it" }

    private fun assertIsPermutation(order: IntArray, size: Int) {
        assertEquals("Wrong length", size, order.size)
        assertArrayEquals(
            "Not a permutation of 0..${size - 1}", IntArray(size) { it }, order.sortedArray()
        )
    }

    private fun countArtistAdjacencies(order: IntArray, artistForIndex: (Int) -> String): Int {
        var count = 0
        for (i in 0 until order.size - 1) {
            val artist = artistForIndex(order[i])
            if (artist.isNotBlank() && artist == artistForIndex(order[i + 1])) count++
        }
        return count
    }

    /** Provable minimum same-artist adjacencies for a library. */
    private fun adjacencyFloor(size: Int, artistForIndex: (Int) -> String): Int {
        val counts = (0 until size).map(artistForIndex).filter { it.isNotBlank() }
            .groupingBy { it }.eachCount()
        return maxOf(0, 2 * (counts.values.maxOrNull() ?: 0) - size - 1)
    }

    /** Spread of the distances between repeats of one artist. A mechanical,
     *  evenly-spaced layout scores near zero. */
    private fun gapSpread(order: IntArray, artistForIndex: (Int) -> String): Double {
        val lastSeen = HashMap<String, Int>()
        val gaps = ArrayList<Int>()
        for (position in order.indices) {
            val artist = artistForIndex(order[position])
            if (artist.isBlank()) continue
            lastSeen[artist]?.let { gaps.add(position - it) }
            lastSeen[artist] = position
        }
        if (gaps.size < 2) return 0.0
        val mean = gaps.average()
        return sqrt(gaps.sumOf { (it - mean) * (it - mean) } / gaps.size)
    }

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
        assertArrayEquals(intArrayOf(0), engine.generateOrder(1, PlaybackMode.Shuffle))
    }

    @Test
    fun `pure shuffle on 0 tracks returns empty`() {
        val engine = ShuffleEngine(Random(42))
        assertEquals(0, engine.generateOrder(0, PlaybackMode.Shuffle).size)
    }

    @Test
    fun `pure shuffle on 3 tracks produces all indices`() {
        val engine = ShuffleEngine(Random(42))
        assertIsPermutation(engine.generateOrder(3, PlaybackMode.Shuffle), 3)
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

    // ---- Smart mode, basics ----

    @Test
    fun `smart shuffle with seeded RNG is deterministic`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "B", 2 to "C"))
        val albums = albumForIndex(mapOf(0 to "a1", 1 to "b1", 2 to "c1"))

        val result1 = ShuffleEngine(Random(42)).generateOrder(3, PlaybackMode.SmartShuffle, artists, albums)
        val result2 = ShuffleEngine(Random(42)).generateOrder(3, PlaybackMode.SmartShuffle, artists, albums)

        assertArrayEquals(result1, result2)
    }

    @Test
    fun `smart shuffle on 1 track returns single index`() {
        val engine = ShuffleEngine(Random(42))
        assertArrayEquals(intArrayOf(0), engine.generateOrder(1, PlaybackMode.SmartShuffle))
    }

    @Test
    fun `smart shuffle on 0 tracks returns empty`() {
        val engine = ShuffleEngine(Random(42))
        assertEquals(0, engine.generateOrder(0, PlaybackMode.SmartShuffle).size)
    }

    @Test
    fun `smart shuffle on 3 tracks produces all indices`() {
        val engine = ShuffleEngine(Random(42))
        assertIsPermutation(engine.generateOrder(3, PlaybackMode.SmartShuffle), 3)
    }

    @Test
    fun `smart shuffle reduces artist adjacency vs pure shuffle`() {
        val artists = artistForIndex((0..49).associateWith { ('A' + it / 10).toString() })
        val albums = albumForIndex(emptyMap())
        val trials = 200

        var pureTotal = 0
        var smartTotal = 0
        for (i in 0 until trials) {
            pureTotal += countArtistAdjacencies(
                ShuffleEngine(Random(i * 2)).generateOrder(50, PlaybackMode.Shuffle), artists
            )
            smartTotal += countArtistAdjacencies(
                ShuffleEngine(Random(i * 2 + 1))
                    .generateOrder(50, PlaybackMode.SmartShuffle, artists, albums),
                artists
            )
        }

        assertTrue(
            "Smart mean (${smartTotal / 200.0}) should beat pure mean (${pureTotal / 200.0})",
            smartTotal < pureTotal
        )
    }

    // ---- The deal reaches the provable floor ----

    @Test
    fun `named library shapes reach the adjacency floor`() {
        val albums = albumForIndex(emptyMap())
        data class Shape(val label: String, val size: Int, val artists: (Int) -> String)

        val shapes = listOf(
            Shape("6 artists x 10", 60) { "A${it % 6}" },
            Shape("3 artists x 20", 60) { "A${it % 3}" },
            Shape("2 artists x 30", 60) { "A${it % 2}" },
            Shape("half the queue is one artist", 60) { if (it < 30) "DOM" else "S${it % 7}" },
            // Exactly ceil(n/2) by one artist: the tightest solvable case, and the
            // one that fails if the deal hands out anything before the biggest artist.
            Shape("one artist holds exactly half", 61) { if (it < 31) "DOM" else "S${it % 9}" },
            Shape("five-track queue, three by one artist", 5) { if (it < 3) "DOM" else "S$it" },
            Shape("long tail of singles", 120) { if (it < 20) "BIG${it % 2}" else "S$it" },
            Shape("all distinct", 40) { "A$it" },
        )

        for (shape in shapes) {
            val floor = adjacencyFloor(shape.size, shape.artists)
            for (seed in 0 until 50) {
                val order = ShuffleEngine(Random(seed))
                    .generateOrder(shape.size, PlaybackMode.SmartShuffle, shape.artists, albums)
                assertIsPermutation(order, shape.size)
                assertEquals(
                    "${shape.label}, seed $seed",
                    floor, countArtistAdjacencies(order, shape.artists)
                )
            }
        }
    }

    @Test
    fun `a clash-free order is always found when one exists`() {
        // The guarantee that matters, over randomly shaped libraries rather than
        // hand-picked ones. Where no artist holds more than half the queue, a
        // clash-free order exists and must be found. Where one does, the floor is
        // unreachable and the result must stay within one of it.
        val albums = albumForIndex(emptyMap())
        var feasible = 0
        var infeasible = 0

        for (seed in 0 until 2000) {
            val random = Random(seed)
            val size = 2 + random.nextInt(59)
            val artistCount = 1 + random.nextInt(size)
            val labels = Array(size) { "A${random.nextInt(artistCount)}" }
            val artists: (Int) -> String = { labels[it] }

            val floor = adjacencyFloor(size, artists)
            val order = ShuffleEngine(Random(seed))
                .generateOrder(size, PlaybackMode.SmartShuffle, artists, albums)
            assertIsPermutation(order, size)
            val clashes = countArtistAdjacencies(order, artists)

            val counts = labels.toList().groupingBy { it }.eachCount().values.sortedDescending()
            if (floor == 0) {
                feasible++
                assertEquals("seed $seed, n=$size, counts=$counts", 0, clashes)
            } else {
                infeasible++
                assertTrue("seed $seed, n=$size, counts=$counts: $clashes vs floor $floor",
                    clashes in floor..(floor + 1))
            }
        }

        assertTrue("expected a spread of shapes", feasible > 1000 && infeasible > 50)
    }

    // ---- Blank artist tags mean unknown, not shared ----

    @Test
    fun `libraries with missing artist tags still reach the floor`() {
        // Untagged tracks must not constrain each other. Randomised because a
        // hand-picked blank/tagged split is easy to get accidentally right.
        val albums = albumForIndex(emptyMap())

        for (seed in 0 until 400) {
            val random = Random(seed)
            val size = 4 + random.nextInt(60)
            val blankShare = random.nextDouble()
            val labels = Array(size) {
                if (random.nextDouble() < blankShare) "" else "A${random.nextInt(1 + size / 6)}"
            }
            val artists: (Int) -> String = { labels[it] }

            val order = ShuffleEngine(Random(seed))
                .generateOrder(size, PlaybackMode.SmartShuffle, artists, albums)
            assertIsPermutation(order, size)

            val floor = adjacencyFloor(size, artists)
            val clashes = countArtistAdjacencies(order, artists)
            assertTrue(
                "seed $seed, n=$size: $clashes clashes against floor $floor",
                clashes in floor..(floor + 1)
            )
            if (floor == 0) assertEquals("seed $seed, n=$size", 0, clashes)
        }
    }

    @Test
    fun `library with no artist tags at all produces a valid order`() {
        val size = 50
        val artists = artistForIndex((0 until size).associateWith { "" })
        val albums = albumForIndex((0 until size).associateWith { "" })

        for (seed in 0 until 20) {
            val order = ShuffleEngine(Random(seed))
                .generateOrder(size, PlaybackMode.SmartShuffle, artists, albums)
            assertIsPermutation(order, size)
            assertEquals(0, ShuffleEngine().calculatePenalty(order, artists, albums, emptySet()))
        }
    }

    // ---- Compilations: the only case where the album term can fire ----

    @Test
    fun `compilation album spanning several artists gets separated`() {
        val size = 120
        // Tracks 0..59 share one album but have distinct artists, so only the
        // album term can tell them apart.
        val artists = artistForIndex((0 until size).associateWith { if (it < 60) "VA_$it" else "A${it % 10}" })
        val albums = albumForIndex((0 until size).associateWith { if (it < 60) "VA" else "L${it % 20}" })

        fun albumClashes(order: IntArray): Int {
            var count = 0
            for (i in 0 until order.size - 1) {
                val artistA = artists(order[i])
                val sameArtist = artistA.isNotBlank() && artistA == artists(order[i + 1])
                if (!sameArtist && albums(order[i]) == albums(order[i + 1])) count++
            }
            return count
        }

        var worst = 0
        for (seed in 0 until 30) {
            val order = ShuffleEngine(Random(seed))
                .generateOrder(size, PlaybackMode.SmartShuffle, artists, albums)
            assertIsPermutation(order, size)
            worst = maxOf(worst, albumClashes(order))
        }
        // A plain shuffle of this library averages well over twenty.
        assertTrue("worst album clashes was $worst", worst <= 2)
    }

    // ---- The polish must leave a shuffled-looking order ----

    @Test
    fun `result is not a mechanically regular pattern`() {
        // A raw frequency deal spaces every artist at a constant interval, giving
        // a gap spread of zero. The polish has to break that up.
        val size = 60
        val artists = artistForIndex((0 until size).associateWith { "A${it % 6}" })
        val albums = albumForIndex(emptyMap())

        var spread = 0.0
        val distinct = HashSet<List<Int>>()
        for (seed in 0 until 50) {
            val order = ShuffleEngine(Random(seed))
                .generateOrder(size, PlaybackMode.SmartShuffle, artists, albums)
            spread += gapSpread(order, artists)
            distinct.add(order.toList())
        }

        assertTrue("Mean gap spread ${spread / 50} looks mechanical", spread / 50 > 1.5)
        assertEquals("Orders should differ per seed", 50, distinct.size)
    }

    @Test
    fun `one artist's tracks do not come out in library order`() {
        // The deal hands out an artist's tracks as a block, so without shuffling
        // inside that block they would play in album order. The queue has to be
        // long enough that the capped polish cannot undo it on its own - at a few
        // thousand tracks the polish still reaches everything and hides the fault.
        val size = 10_000
        val artists = artistForIndex((0 until size).associateWith { "A${it % 50}" })
        val albums = albumForIndex(emptyMap())

        var ascending = 0
        var pairs = 0
        for (seed in 0 until 3) {
            val order = ShuffleEngine(Random(seed))
                .generateOrder(size, PlaybackMode.SmartShuffle, artists, albums)
            assertIsPermutation(order, size)

            val lastTrackOfArtist = HashMap<String, Int>()
            for (position in order.indices) {
                val track = order[position]
                val artist = artists(track)
                lastTrackOfArtist[artist]?.let {
                    pairs++
                    if (track > it) ascending++
                }
                lastTrackOfArtist[artist] = track
            }
        }

        // A shuffled block sits at about half; an unshuffled one measures 0.72 here.
        val ascendingShare = ascending.toDouble() / pairs
        assertTrue(
            "Successive tracks by one artist ascend $ascendingShare of the time - library order leaked",
            ascendingShare in 0.42..0.58
        )
    }

    @Test
    fun `smart shuffle does not degenerate to a fixed order`() {
        val artists = artistForIndex((0..19).associateWith { ('A' + it / 4).toString() })
        val albums = albumForIndex(emptyMap())

        val results = mutableSetOf<List<Int>>()
        for (i in 0 until 100) {
            results.add(
                ShuffleEngine(Random(i * 7 + 13))
                    .generateOrder(20, PlaybackMode.SmartShuffle, artists, albums).toList()
            )
        }

        assertTrue("Only ${results.size}/100 unique permutations (need >90)", results.size > 90)
    }

    // ---- Penalty function ----

    @Test
    fun `zero penalty for perfect artist alternation`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "B", 2 to "A", 3 to "B"))
        val albums = albumForIndex(emptyMap())
        assertEquals(
            0, ShuffleEngine().calculatePenalty(intArrayOf(0, 1, 2, 3), artists, albums, emptySet())
        )
    }

    @Test
    fun `artist adjacency penalty scales linearly`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "A", 2 to "A"))
        val albums = albumForIndex(emptyMap())
        // Positions (0,1) and (1,2) = 2 × 10
        assertEquals(
            20, ShuffleEngine().calculatePenalty(intArrayOf(0, 1, 2), artists, albums, emptySet())
        )
    }

    @Test
    fun `same artist same album avoids double count`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "A", 2 to "A"))
        val albums = albumForIndex(mapOf(0 to "x", 1 to "x", 2 to "y"))
        // Both pairs charge the artist weight only: 20, not 10 + 3 + 10
        assertEquals(
            20, ShuffleEngine().calculatePenalty(intArrayOf(0, 1, 2), artists, albums, emptySet())
        )
    }

    @Test
    fun `album adjacency penalty applied when artists differ`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "B"))
        val albums = albumForIndex(mapOf(0 to "x", 1 to "x"))
        assertEquals(
            3, ShuffleEngine().calculatePenalty(intArrayOf(0, 1), artists, albums, emptySet())
        )
    }

    @Test
    fun `blank artists not treated as same-artist adjacency`() {
        val artists = artistForIndex(mapOf(0 to "", 1 to "", 2 to "", 3 to ""))
        val albums = albumForIndex(emptyMap())
        assertEquals(
            0, ShuffleEngine().calculatePenalty(intArrayOf(0, 1, 2, 3), artists, albums, emptySet())
        )
    }

    @Test
    fun `mixed blank and known artists only penalize known matches`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "", 2 to "A", 3 to ""))
        val albums = albumForIndex(emptyMap())
        assertEquals(
            0, ShuffleEngine().calculatePenalty(intArrayOf(0, 1, 2, 3), artists, albums, emptySet())
        )
    }

    // ---- Recency ----

    @Test
    fun `recency penalty is highest at the front and decays`() {
        val artists = artistForIndex(emptyMap())
        val albums = albumForIndex(emptyMap())
        val engine = ShuffleEngine()
        val n = 40 // half-life = 10

        fun penaltyWithTrack0At(position: Int): Int {
            val order = IntArray(n) { it }
            order[0] = order[position]
            order[position] = 0
            return engine.calculatePenalty(order, artists, albums, setOf(0))
        }

        // 5 × 2^(-p/10), truncated
        assertEquals(5, penaltyWithTrack0At(0))
        assertEquals(2, penaltyWithTrack0At(10))
        assertEquals(1, penaltyWithTrack0At(20))
        assertEquals(0, penaltyWithTrack0At(30))

        var previous = Int.MAX_VALUE
        for (position in 0 until n) {
            val current = penaltyWithTrack0At(position)
            assertTrue("Recency penalty rose at position $position", current <= previous)
            previous = current
        }
    }

    @Test
    fun `recency ranks orders differently when only some tracks are recent`() {
        // Regression test for a real defect: production used to pass the WHOLE
        // previous order, which marks every track recent. The penalty then adds
        // the same constant to every arrangement and ranks nothing.
        val n = 60
        val artists = artistForIndex((0 until n).associateWith { "A${it % 6}" })
        val albums = albumForIndex(emptyMap())
        val engine = ShuffleEngine()

        val everyTrack = (0 until n).toSet()
        val recentTail = (n - n / 4 until n).toSet()

        val fromEveryTrack = HashSet<Int>()
        val fromRecentTail = HashSet<Int>()
        for (seed in 0 until 10) {
            val order = IntArray(n) { it }.also { it.shuffle(Random(seed)) }
            val base = engine.calculatePenalty(order, artists, albums, emptySet())
            fromEveryTrack.add(engine.calculatePenalty(order, artists, albums, everyTrack) - base)
            fromRecentTail.add(engine.calculatePenalty(order, artists, albums, recentTail) - base)
        }

        assertEquals("Marking every track recent contributes a constant", 1, fromEveryTrack.size)
        assertTrue("A partial recent set must vary by arrangement", fromRecentTail.size > 1)
    }

    // ---- Delta evaluation ----
    //
    // The polish never rescores a whole order; it scores what a swap would
    // change. If the two disagree the search optimises a phantom score, so these
    // pin them together exhaustively.

    @Test
    fun `delta matches full rescore for every swap on small orders`() {
        val engine = ShuffleEngine()

        for (n in 2..8) {
            val artists = artistForIndex((0 until n).associateWith { "A${it % 3}" })
            val albums = albumForIndex((0 until n).associateWith { "L${it % 2}" })

            for (seed in 0 until 5) {
                val previousIndices =
                    (0 until n).filter { Random(seed * 100 + it).nextBoolean() }.toSet()
                val base = IntArray(n) { it }.also { it.shuffle(Random(seed)) }

                for (i in 0 until n) {
                    for (j in i + 1 until n) {
                        val order = base.copyOf()
                        val before = engine.calculatePenalty(order, artists, albums, previousIndices)

                        val delta = engine.deltaPenalty(order, i, j, artists, albums, previousIndices)
                        assertArrayEquals(
                            "deltaPenalty mutated the order (n=$n seed=$seed i=$i j=$j)", base, order
                        )

                        val temp = order[i]
                        order[i] = order[j]
                        order[j] = temp
                        val after = engine.calculatePenalty(order, artists, albums, previousIndices)

                        assertEquals("n=$n seed=$seed i=$i j=$j", after - before, delta)
                    }
                }
            }
        }
    }

    @Test
    fun `delta matches full rescore on larger random orders`() {
        val engine = ShuffleEngine()

        for (seed in 0 until 300) {
            val random = Random(seed)
            val n = 9 + random.nextInt(40)
            val artists = artistForIndex((0 until n).associateWith { "A${it % 7}" })
            val albums = albumForIndex((0 until n).associateWith { "L${it % 5}" })
            val previousIndices = (0 until n).filter { random.nextBoolean() }.toSet()

            val order = IntArray(n) { it }.also { it.shuffle(random) }
            val i = random.nextInt(n)
            var j = random.nextInt(n - 1)
            if (j >= i) j++

            val before = engine.calculatePenalty(order, artists, albums, previousIndices)
            val delta = engine.deltaPenalty(order, i, j, artists, albums, previousIndices)

            val temp = order[i]
            order[i] = order[j]
            order[j] = temp
            val after = engine.calculatePenalty(order, artists, albums, previousIndices)

            assertEquals("seed=$seed n=$n i=$i j=$j", after - before, delta)
        }
    }

    @Test
    fun `delta of a swap with itself is zero`() {
        val engine = ShuffleEngine()
        val artists = artistForIndex((0 until 6).associateWith { "A${it % 2}" })
        val albums = albumForIndex(emptyMap())

        for (i in 0 until 6) {
            assertEquals(
                0,
                engine.deltaPenalty(intArrayOf(3, 1, 4, 0, 5, 2), i, i, artists, albums, setOf(1, 4))
            )
        }
    }

    // ---- Edge cases ----

    @Test
    fun `smart shuffle on 2 tracks produces valid permutation`() {
        val artists = artistForIndex(mapOf(0 to "A", 1 to "A"))
        val albums = albumForIndex(mapOf(0 to "x", 1 to "x"))

        for (seed in 0 until 10) {
            assertIsPermutation(
                ShuffleEngine(Random(seed))
                    .generateOrder(2, PlaybackMode.SmartShuffle, artists, albums, setOf(0)),
                2
            )
        }
    }

    @Test
    fun `all tracks same artist cannot escape the structural floor`() {
        val n = 30
        val artists = artistForIndex((0 until n).associateWith { "A" })
        val albums = albumForIndex((0 until n).associateWith { "L" })

        val order = ShuffleEngine(Random(7))
            .generateOrder(n, PlaybackMode.SmartShuffle, artists, albums, (0 until n / 4).toSet())
        assertIsPermutation(order, n)
        assertEquals(n - 1, countArtistAdjacencies(order, artists))
    }

    @Test(timeout = 5000)
    fun `large playlist stays within budget`() {
        val n = 10_000
        val artists = artistForIndex((0 until n).associateWith { "Artist${it % 500}" })
        val albums = albumForIndex((0 until n).associateWith { "Album${it % 200}" })
        val previousIndices = (n - n / 4 until n).toSet()

        val start = System.nanoTime()
        val result = ShuffleEngine(Random(42))
            .generateOrder(n, PlaybackMode.SmartShuffle, artists, albums, previousIndices)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0

        assertIsPermutation(result, n)
        assertEquals(0, countArtistAdjacencies(result, artists))
        assertTrue("Elapsed ${elapsedMs}ms exceeds the 200ms ceiling", elapsedMs < 200.0)
    }

    // ---- Repeat mode (non-shuffle fallback) ----

    @Test
    fun `repeat mode returns sequential order`() {
        val engine = ShuffleEngine(Random(42))
        assertArrayEquals(intArrayOf(0, 1, 2, 3, 4), engine.generateOrder(5, PlaybackMode.Repeat))
    }

    @Test
    fun `repeat one mode returns sequential order`() {
        val engine = ShuffleEngine(Random(42))
        assertArrayEquals(intArrayOf(0, 1, 2, 3, 4), engine.generateOrder(5, PlaybackMode.RepeatOne))
    }
}
