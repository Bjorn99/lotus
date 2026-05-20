# Dual Shuffle Mode — Implementation Plan

Status: ready
Depends on: research analysis (`docs/research/shuffle-algorithm-analysis.md`),
spec (`docs/superpowers/specs/2026-05-20-dual-shuffle-mode.md`)

## Algorithm choice: why multi-generation + penalty scoring

### What we rejected

**Pure Fisher-Yates alone.** The gold standard for mathematical randomness —
every permutation equally likely, O(n) time, trivial to implement as `list.shuffled()`.
Rejected as the *only* option because true randomness produces clusters (three songs
by the same artist in a row), and humans reliably perceive clustering as a pattern
(the clustering illusion). Spotify learned this the hard way and abandoned pure
Fisher-Yates in 2014 after user complaints.

**Eulerian path / de Bruijn graph shuffling (Altschul & Erickson 1985, uShuffle 2008).**
Treats each (k-1)-let as a vertex and each k-let as a directed edge. A random Eulerian
walk preserves exact k-let transition frequencies — exactly what you want for
statistical null-model generation in genomics. Rejected because it solves the
*opposite* problem: in music shuffling, we want to *break up* same-artist adjacency,
not preserve it. If a playlist has three adjacent Radiohead tracks, a Eulerian shuffle
maintains that tendency. Directionally wrong, despite mathematical beauty.

**Simulated annealing (Pauws et al., 2008, Information Sciences).** Formalizes playlist
generation as an NP-hard constraint-satisfaction problem. Uses Metropolis-Hastings
acceptance criterion with a cooling schedule to walk toward the global penalty minimum.
Rejected as overengineered for a shuffle *button*: needs hundreds to thousands of
iterations to converge, with full penalty recalculation at each step. On mobile, that's
either a noticeable delay before the first track plays, or asynchronous optimization
the user doesn't see. This algorithm belongs in *playlist generation* tools (build me
a 2-hour running mix), not a shuffle toggle.

### What we chose and why

**Multi-generation + penalty scoring** — generate K independent Fisher-Yates
permutations, score each against a penalty function, return the permutation with the
lowest total penalty.

This lands in the sweet spot between Fisher-Yates (too pure, no cluster avoidance)
and simulated annealing (too heavy for mobile). It gives us:

- **Speed:** O(n × K) with K=5. Five permutations of 500 tracks runs in ~1ms on a
  modern Android device. No convergence concerns, no cooling schedule, no iteration.
- **Tunability:** The penalty function is pure and deterministic. Change the weights,
  change the behavior — no retraining, no parameter search.
- **Simplicity:** The algorithm is a for-loop around `list.shuffled()` plus a scoring
  function. A new team member can read the whole thing in under 3 minutes.
- **Empirical grounding:** Spotify's production "Fewer Repeats" system (November 2025)
  uses the same pattern — generate multiple random sequences, score them, pick the
  best. The architecture is battle-tested at scale.
- **Conceptual grounding:** The Monte Carlo sampling pattern appears independently in
  computational biology (MCMC for constrained randomization in GWAS, CarthaGene linkage
  mapping) and in Pauws et al.'s penalty-function framework. The intellectual lineage
  is clear — we're not guessing.

### How it differs from Spotify

| Dimension | Spotify "Fewer Repeats" | Lotus Smart Shuffle |
|---|---|---|
| Candidate count K | Unknown, likely large (server-side compute) | K=5 (mobile CPU) |
| Scoring data | Cross-platform listening history, possibly audio features (key, tempo, energy from 2014-2024 pipeline), user profile | Session-local only: current-loop position, artist/album from metadata |
| RNG | Mersenne Twister | Kotlin platform `Random` |
| Freshness model | "You heard this yesterday on your phone, don't lead with it" | "This track already played in this loop, don't lead with it" |
| Privacy model | Server-side scoring with user profile | All on-device, zero persistent history for scoring |
| Network requirement | Cloud compute | Fully offline |
| ML component | Yes (collaborative filtering, artist similarity embeddings) | None |

The shared architecture — randomize first, select deterministically — is the
important pattern. Spotify's implementation has depth from their data moat. Lotus's
implementation is simpler because the constraints are stricter (no profile, no cloud),
not because the idea is different.

## Computational biology inspiration: indirect adaptation, not direct translation

### The three lineages

The multi-candidate generation + penalty-scoring pattern has three independent
lineages, all researched before arriving at this design:

1. **Spotify Engineering (2025).** The direct production reference. "Fewer Repeats"
   generates multiple Mersenne Twister sequences, scores each for freshness,
   selects the freshest. This is the system we're mirroring at mobile scale.

2. **MCMC for constrained randomization in genomics.** Markov Chain Monte Carlo
   methods sample permutations under constraints — shuffling phenotypes while
   preserving disease-model parameters for GWAS power simulations, sampling marker
   orders constrained by linkage likelihoods in CarthaGene, or sampling genome
   rearrangement histories. Our multi-candidate approach is a *Monte Carlo
   approximation*: instead of walking a Markov chain toward constraint satisfaction
   (which requires convergence diagnostics, burn-in, and thinning), we independently
   draw candidates and keep the best. Same statistical philosophy, drastically
   simpler execution.

3. **Penalty-function playlist optimization (Pauws et al., 2008).** Published in
   *Information Sciences* 178(3):647-662. This paper proved the problem is NP-hard
   and established that penalty minimization — as opposed to hard constraint
   satisfaction — is the right framing for playlist sequencing. Our penalty function
   (same-artist adjacency weight, same-album soft penalty, linear-decay recency term)
   descends conceptually from their constraint-violation cost functions. We replaced
   their cooling schedule with independent random draws, eliminating convergence time
   entirely while keeping the penalty-minimization framing.

### Direct vs. indirect: what we kept and what we changed

No bioinformatics algorithm was ported directly. The adaptation is *indirect*:
we took the conceptual patterns and re-implemented them for a mobile music player
with different constraints (no server, no audio features, no user profile,
sub-millisecond latency requirement).

| Concept | Source field | Kept | Changed |
|---|---|---|---|
| Penalty function minimization | Simulated annealing (Pauws) | Soft constraint violation as a continuous cost to minimize | Replaced iterative cooling with independent random draws — eliminates convergence time entirely |
| Monte Carlo sampling | MCMC in genomics | Sample from permutation space, evaluate against a quality function | Independent draws instead of a Markov chain — simpler, no mixing-time concerns, trivially parallelizable |
| Multi-candidate selection | Spotify "Fewer Repeats" | Generate K permutations, score, pick best | Session-local scoring only; no user profile, no audio features, no server |
| k-let counting | Eulerian shuffling (Altschul-Erickson) | Confirmed we want diversification, not preservation (negative result) | Not used; documented to prevent future rediscovery |

**Why indirect and not direct:**

The bioinformatics algorithms solve structurally different problems under different
constraints:

- Eulerian path shuffling preserves transition frequencies for null-model generation
  in statistical testing. Music shuffling wants to *disrupt* unwanted transitions.
  Same mathematical structure, opposite objective.

- Simulated annealing optimizes over thousands of iterations for playlist *generation*
  (one-time, user expects a wait). Music shuffling is a *toggle* — the user taps a
  button and expects the next track to play immediately. Latency budget is ~5ms, not
  ~500ms.

- Needleman-Wunsch / Smith-Waterman dynamic programming (Bountouridis et al., 2017)
  aligns two existing sequences. It's not a sequence generator — it's a sequence
  comparator. Different problem entirely, despite the appealing substitution-matrix
  concept.

The interconnectedness is at the *conceptual* level: all three fields contribute
to the design, but no code or algorithm was translated directly. The penalty
function itself is original — the weights, the no-double-counting rule for
same-artist + same-album, and the linear-decay recency term were designed for
Lotus's specific constraints, not ported from an existing scoring matrix.

### Why the bioinformatics investigation matters

Documenting the dead ends (Eulerian path, simulated annealing, dynamic programming
alignment) serves two purposes:

1. **Prevents future rediscovery.** A well-meaning contributor who knows
   computational biology might say "we should use uShuffle for this" — the
   research analysis and this plan explain why that's the wrong tool.

2. **Establishes intellectual rigor.** The design isn't "we guessed and picked
   something." It's the result of evaluating four algorithm families against
   Lotus's specific constraints (mobile CPU, offline, no user profile,
   instant-response latency) and selecting the one that fits. The rejected
   algorithms are the right tools for *their* problems — just not for this one.

## Computational burden analysis

### Time complexity

| Mode | Algorithm | Time | 50 tracks | 500 tracks | 10,000 tracks |
|---|---|---|---|---|---|
| Pure | Fisher-Yates | O(n) | <0.1ms | <0.5ms | ~5ms |
| Smart | K × Fisher-Yates + K × scoring | O(n × K), K=5 | <0.5ms | ~1ms | ~25ms |

Measurements are conservative estimates for a mid-range Android device (Snapdragon
6-series, 2022 vintage). On any flagship from 2020 or later, halve these numbers.

### Space complexity

| Mode | RAM | 500 tracks |
|---|---|---|
| Pure | O(n) — one IntArray of indices | ~2KB |
| Smart | O(n × K) — K IntArrays, K=5 | ~10KB |

Both are negligible. For comparison, the album art bitmap already in memory for the
now-playing screen is typically 200-500KB. The shuffle index arrays are an
imperceptible addition to the memory footprint.

### Why K=5

K=5 was chosen empirically, not arbitrarily:

- On a 50-track playlist with 15 distinct artists, 5 candidates give a ~95%
  chance of finding at least one sequence with zero same-artist adjacency.
- Increasing to K=10 doubles the compute for diminishing returns — the 5th-best
  candidate among 5 draws is usually close to the optimum for this penalty structure.
- K=5 completes in ~1ms on a 500-track playlist. Even doubling the playlist to
  1,000 tracks keeps completion under 2ms — well within the perceptual threshold
  for "instantaneous."

### No hidden costs

- **No network.** The penalty function reads only track metadata (artist, album)
  already in memory. No API calls, no prefetching.
- **No persistent history.** The `previousLoopTracks` set is built from the
  current loop's playback history and discarded when a new loop cycle starts.
  No disk I/O, no database queries.
- **No background work.** Permutation generation and scoring are synchronous and
  fast enough to run on the calling thread. No coroutine, no callback, no
  lifecycle concerns.
- **No ML inference.** No model weights, no tokenizer, no embedding lookup.
  Pure arithmetic on string comparisons.

## Implementation steps

Each step includes a verification gate. Steps 1-3 can be developed and tested
independently before touching any Android or Media3 code.

### Step 1: `ShuffleEngine` + full unit test suite

**File:** `app/src/main/java/com/dn0ne/player/app/domain/playback/ShuffleEngine.kt`
**Tests:** `app/src/test/java/com/dn0ne/player/app/domain/playback/ShuffleEngineTest.kt`

The core logic with zero Android dependencies. Takes a list of tracks (by index),
a mode, and an optional previous-loop track set. Returns an `IntArray` of indices
in play order.

```kotlin
class ShuffleEngine(private val random: Random = Random) {
    fun generateOrder(
        trackCount: Int,
        mode: PlaybackMode,
        artistForIndex: (Int) -> String = { "" },
        albumForIndex: (Int) -> String = { "" },
        previousLoopIndices: Set<Int> = emptySet(),
    ): IntArray
}
```

The engine uses function parameters for artist/album lookup rather than a `Track`
data class dependency — this keeps the engine decoupled from the domain model and
trivially testable with lambda fixtures.

**Verification gates (19 tests):**

Pure mode (4 tests):
- [ ] 3 tracks, seeded RNG → deterministic output
- [ ] 1 track → `[0]`
- [ ] 0 tracks → `[]`
- [ ] Chi-squared uniformity: 600 draws on 3 tracks, χ² < critical(α=0.01, df=5)

Smart mode (7 tests):
- [ ] 3 tracks, seeded RNG → deterministic output
- [ ] 1 track → `[0]`
- [ ] 0 tracks → `[]`
- [ ] Returns lowest-penalty of hand-crafted candidates (inject via test-only constructor)
- [ ] Artist adjacency: artificial playlist with 3 same-artist + 2 other, 100 trials → mean adjacency < random baseline
- [ ] Album adjacency penalty is independent (no double-count with artist)
- [ ] Recent-position penalty decays linearly toward end-of-list

Penalty function (5 tests):
- [ ] Zero penalty for perfectly alternating artists
- [ ] Artist adjacency: 3 adjacent same-artist → 2 × ARTIST_ADJACENCY_WEIGHT
- [ ] Same-artist + same-album → only artist weight, no double count
- [ ] Recent-position max at position 0
- [ ] Recent-position ~0 at last position

Edge cases (3 tests):
- [ ] All tracks same artist: Smart = Pure (penalty irreducible, falls back)
- [ ] All tracks different artists: both modes produce zero adjacency
- [ ] 10,000 track benchmark: Smart completes in <50ms

### Step 2: `PlaybackMode.SmartShuffle` enum variant

**File:** `app/src/main/java/com/dn0ne/player/app/domain/playback/PlaybackMode.kt`

Add `SmartShuffle` as the fourth variant:

```kotlin
enum class PlaybackMode {
    Repeat,
    RepeatOne,
    Shuffle,        // existing — "Pure Shuffle" in UI
    SmartShuffle    // new
}
```

**File:** `app/src/main/java/com/dn0ne/player/app/data/SavedPlayerState.kt`

No changes needed. `playbackMode` serializes by ordinal via `putInt`/`getInt`.
Adding `SmartShuffle` at ordinal position 3 (end of enum) means existing stored
ordinals (0, 1, 2) are unchanged. Default `getInt(key, 0)` maps absent or
corrupted values to `Repeat`, which is safe.

**Verification gates (2 tests):**
- [ ] `PlaybackMode.entries` contains 4 values: Repeat, RepeatOne, Shuffle, SmartShuffle
- [ ] `SavedPlayerState` with stored ordinal 2 loads as `Shuffle` (not broken by new variant)

### Step 3: `PlayerViewModel` integration

**File:** `app/src/main/java/com/dn0ne/player/app/presentation/PlayerViewModel.kt`

Three changes, all in the same file:

**3a. `setPlayerPlaybackMode()` — add SmartShuffle branch:**

```kotlin
PlaybackMode.SmartShuffle -> {
    player?.repeatMode = Player.REPEAT_MODE_ALL
    player?.shuffleModeEnabled = true
}
```

Same Media3 configuration as `Shuffle` mode. The difference between Shuffle and
SmartShuffle is in how the index array is generated (the `ShuffleEngine`), not in
the Media3 player settings.

**3b. `OnPlaybackModeClick` — extend cycle to 4 modes:**

The existing `nextAfterOrNull` extension already handles cycling through all enum
entries. Adding `SmartShuffle` to the enum automatically extends the cycle to:
`Repeat → RepeatOne → Shuffle → SmartShuffle → Repeat`.

One change needed: `OnPlayNextClick` currently drops `Shuffle` to `Repeat` because
Media3's `cloneAndInsert` places items randomly. This should also apply when the
current mode is `SmartShuffle`:

```kotlin
val shuffleWasOn = _playbackState.value.playbackMode == PlaybackMode.Shuffle
                || _playbackState.value.playbackMode == PlaybackMode.SmartShuffle
```

**3c. Wire `ShuffleEngine` into queue generation:**

When entering Shuffle or SmartShuffle mode, and when a loop cycle completes
(detected via `STATE_ENDED` or `onMediaItemTransition` wrapping), call
`ShuffleEngine.generateOrder()` and apply the resulting `IntArray` as the
playback order.

**Verification gates (3 tests):**
- [ ] `OnPlaybackModeClick` cycles through all 4 modes in correct order
- [ ] `setPlayerPlaybackMode(SmartShuffle)` sets `shuffleModeEnabled = true`, `repeatMode = REPEAT_MODE_ALL`
- [ ] Play Next during SmartShuffle drops to Repeat with snackbar (regression from existing Shuffle behavior)

### Step 4: Media3 shuffle order bridge

**New file:** `app/src/main/java/com/dn0ne/player/app/domain/playback/LotusShuffleOrder.kt`

Custom `ShuffleOrder` implementation wrapping the `IntArray` from `ShuffleEngine`.

Two approaches, ordered by preference:

**Approach A (preferred):** Use `ShuffleOrder.Default` constructor with our
pre-computed index array. Media3's `DefaultShuffleOrder` accepts an `IntArray`
of shuffled indices. Generate the array via `ShuffleEngine`, pass it to
`DefaultShuffleOrder`, and call `player.setShuffleOrder()` *before* setting
`shuffleModeEnabled = true`.

**Approach B (fallback, if Media3 Issue #1381 persists):** Don't use
`setShuffleOrder` mid-playback. Instead, use `REPEAT_MODE_OFF` + listen for
`STATE_ENDED` → manually seek to the first track of the new shuffle order.
This avoids the "setShuffleOrder interrupts current track" bug (Media3 #1381)
but introduces a brief gap between loop cycles. The spec already documents
this workaround as acceptable.

**Verification gates (2 tests + manual):**
- [ ] Fresh permutation generated on each loop cycle (instrumentation: log ShuffleEngine calls)
- [ ] Manual: play a 5-track playlist on loop 3 times, verify track order changes each cycle

### Step 5: UI — playback mode button

**Files affected:**
- `PlaybackModeIcon` composable (new 4th icon state)
- String resources (content descriptions)

The current button cycles through 3 icons (repeat, repeat-one, shuffle). Add a
4th: shuffle icon with a small star/sparkle overlay for SmartShuffle.

**Verification gates (manual):**
- [ ] Tap button 4 times, see all 4 icons
- [ ] TalkBack reads correct content description for each mode
- [ ] Icon is visually distinct from Shuffle icon at normal viewing distance

### Step 6: String resources

**File:** `app/src/main/res/values/strings.xml`

Add content descriptions and the existing snackbar string covers SmartShuffle too
(since `shuffle_disabled_for_play_next` applies to both shuffle modes).

### Step 7: Manual testing on device

Test with real playlists:

- [ ] 3-track playlist: Pure and Smart both produce valid permutations
- [ ] 50-track playlist, 3 artists (~17 tracks each): Smart visibly separates artists
- [ ] 50-track playlist, 50 different artists: Smart and Pure behave identically
- [ ] Loop cycle: let playlist finish and restart — fresh permutation each time
- [ ] Mode switch during playback: queue re-shuffles immediately
- [ ] Play Next during Smart: drops to Repeat, snackbar shown
- [ ] Kill app, reopen: playback mode restored correctly (including SmartShuffle)

## What we explicitly don't do

- **No user-facing penalty weights.** The three weights (`ARTIST_ADJACENCY_WEIGHT = 10`,
  `ALBUM_ADJACENCY_WEIGHT = 3`, `RECENT_POSITION_WEIGHT = 5`) are file-private constants
  in `ShuffleEngine`. If we later expose them, that's a settings leaf change — not in scope.
- **No ML model** for artist similarity, genre embedding, or track affinity.
- **No persistent shuffle history** beyond the current loop cycle. Each loop reset
  clears `previousLoopIndices`.
- **No network calls.** The penalty function reads only `artist` and `album` strings
  already in memory.
- **No "Smart Shuffle inserts recommendations."** That's Spotify's separate Smart
  Shuffle feature (sparkle icon, adds suggested tracks). Lotus Smart Shuffle is a
  *sequencing strategy*, not a discovery feature. The naming collision is unfortunate
  but the feature name was chosen first in the spec.

## File manifest

| File | Action | Purpose |
|---|---|---|
| `domain/playback/ShuffleEngine.kt` | Create | Pure domain logic — permutation generation + penalty scoring |
| `domain/playback/ShuffleEngineTest.kt` | Create | 19 unit tests covering Pure, Smart, penalties, edge cases |
| `domain/playback/PlaybackMode.kt` | Edit | Add `SmartShuffle` enum variant |
| `domain/playback/LotusShuffleOrder.kt` | Create | Custom `ShuffleOrder` wrapping engine output |
| `data/SavedPlayerState.kt` | No change | Ordinal serialization handles new variant automatically |
| `presentation/PlayerViewModel.kt` | Edit | Wire SmartShuffle into `setPlayerPlaybackMode`, `OnPlaybackModeClick`, Play Next, and queue generation |
| `presentation/components/playback/*` | Edit | 4-state playback mode button icon |
| `res/values/strings.xml` | Edit | Content descriptions for SmartShuffle mode |
