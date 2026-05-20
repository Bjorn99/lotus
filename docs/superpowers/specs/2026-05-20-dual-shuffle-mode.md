# Dual shuffle mode — implementation spec

Status: draft
Scope: shuffle infrastructure, two user-facing modes, Media3 integration, tests

## Goal

Replace the single Shuffle toggle with two modes — Pure and Smart — that give the
user control over how their playlist is randomized. Pure is mathematically
unbiased. Smart trades a tiny amount of mathematical purity for perceived
fairness, using a penalty-scored multi-candidate approach.

Both modes fix the existing bug where Media3's `DefaultShuffleOrder` repeats the
same permutation on every loop cycle. Each mode gets a fresh permutation when the
queue loops.

## Modes

### Mode 1 — Pure Shuffle

Fisher-Yates (Knuth-Durstenfeld) via Kotlin stdlib `list.shuffled()`. One
permutation generated fresh at the start of each loop cycle. No history, no
weighting, no artist spacing, no metadata queries.

Properties:
- Every permutation equally likely: P(any specific order) = 1 / n!
- O(n) time, O(n) RAM for the index array
- Provably unbiased given a fair RNG source (Kotlin's `Random` uses a
  high-quality PRNG sufficient for this use case)

### Mode 2 — Smart Shuffle

Generate K=5 independent Fisher-Yates permutations, score each against a penalty
function, select the permutation with the lowest total penalty. Fresh set of K
candidates generated at the start of each loop cycle.

Properties:
- O(n × K) time, O(n × K) RAM (K=5, so ~5 arrays of indices)
- Not mathematically uniform — the penalty function biases the distribution
- Perceived as "more random" by users sensitive to same-artist/same-album
  clustering
- Penalty function is pure and deterministic: same playlist + same session state
  always produces the same winner (if RNG seeded, useful for testing)
- No ML, no network, no persistent listening history beyond the current loop

#### Penalty function

```
For a candidate permutation P of length n, with tracks T[P[i]]:

penalty = 0

// Same-artist adjacency (core penalty)
for i in 0..n-2:
    if artist(T[P[i]]) == artist(T[P[i+1]]):
        penalty += ARTIST_ADJACENCY_WEIGHT      // default: 10

// Same-album adjacency (softer penalty — albums naturally cluster)
for i in 0..n-2:
    if album(T[P[i]]) == album(T[P[i+1]]):
        penalty += ALBUM_ADJACENCY_WEIGHT        // default: 3

// Same-artist AND same-album is penalized only by the artist weight
// (avoid double-counting; artist adjacency already covers this case)

// Recently-played position penalty (deters the same tracks leading each loop)
for i in 0..n-1:
    if T[P[i]] was played in the previous loop:
        penalty += RECENT_POSITION_WEIGHT × (1.0 - i.toFloat() / n)
        // earlier positions penalized more heavily
```

Weights are constants, not user-facing. If we later expose them, that's a
settings leaf change.

#### Why K=5

Five candidates gives a ~95% chance of finding at least one sequence with zero
same-artist adjacency on a typical 50-track playlist with 15 distinct artists.
At 500 tracks, generating 5 permutations takes ~1ms on a modern Android device.
Going to K=10 doubles the time for diminishing returns — the 5th-best candidate
among 5 draws is usually close to the optimum for this penalty structure.

### Mode transition rules

- Switching from Pure to Smart (or vice versa): re-generate the current queue
  order immediately. The user explicitly asked for a different behavior.
- Looping (end of queue → restart): always generate a fresh permutation in the
  current mode. This fixes the existing bug.
- Play Next during Smart mode: same behavior as today — temporarily drops to
  Repeat mode so the "play next" track actually plays next (see explanation
  in PlayerViewModel.kt:614-633). User can tap the playback-mode button to
  return to Smart.

### How it differs from Spotify

| | Spotify "Fewer Repeats" | Lotus Smart Shuffle |
|---|---|---|
| Candidate sequences | K unknown, likely large (server-side compute) | K=5 (mobile CPU) |
| Scoring | Full cross-platform listening history, possibly audio features (key, tempo, energy from 2014-2024 pipeline) | Session-local: current-loop position, artist/album adjacency from metadata only |
| RNG | Mersenne Twister | Kotlin platform `Random` |
| Freshness | "You heard this yesterday on your phone, don't lead with it" | "This track already played in this loop, don't lead with it" |
| Privacy | Server-side scoring with user profile | All on-device, zero persistent history for scoring |
| Network | Requires cloud compute | Works fully offline |

The architecture is the same: multiple random candidates, scored, best wins.
Spotify's scoring has depth from their data moat. Lotus's scoring is simpler
because the constraints are stricter (no user profile, no cloud). But the
pattern — randomize first, select deterministically — is identical.

### Origin of the approach

The multi-candidate generation + penalty-scoring pattern appears in three
independent lineages, all of which were researched before arriving at this
design:

1. **Spotify Engineering (2025).** The "Fewer Repeats" system as described in
   their November 2025 engineering blog. This is the direct production reference.

2. **Computational biology — MCMC for constrained randomization.** In genomics,
   Markov Chain Monte Carlo methods are used to sample permutations under
   constraints: shuffle phenotypes while preserving disease-model parameters
   (GWAS power simulations), sample marker orders constrained by linkage
   likelihoods (CarthaGene), or sample genome rearrangement histories. The
   multi-candidate approach is a Monte Carlo approximation of this — instead of
   walking a Markov chain toward constraint satisfaction, we independently draw
   candidates and keep the best.

3. **Penalty-function playlist optimization (Pauws et al., 2008).** Simulated
   annealing with penalty functions for playlist generation, published in
   Information Sciences. This work proved the problem is NP-hard and established
   that penalty minimization (as opposed to hard constraint satisfaction) is the
   right framing. Our approach is a lightweight Monte Carlo approximation of
   their full simulated-annealing optimizer.

### Why no direct algorithm translation from computational biology

Three bioinformatics algorithms were evaluated for direct translation and
rejected:

**Eulerian path / de Bruijn graph shuffling (Altschul & Erickson 1985, Wilson
1996, uShuffle 2008).** This generates uniform random sequences that exactly
preserve k-let transition frequencies. Mapped to music: artists become vertices,
artist→artist transitions become edges, and a random Eulerian walk preserves
the exact count of every artist-to-artist transition in the original playlist.

Rejected because it solves the *opposite* problem. In bioinformatics, you
preserve transition frequencies to avoid introducing bias into null models for
statistical testing. In music shuffling, we want to *break up* undesirable
transition patterns (same-artist clustering), not preserve them. A playlist
with three adjacent Radiohead tracks would have that adjacency preserved by
the Eulerian shuffle. It's mathematically elegant but directionally wrong.

**Full simulated annealing (Pauws et al., 2008).** Uses a Metropolis-Hastings
acceptance criterion and cooling schedule to walk toward the global penalty
minimum. Published in Information Sciences and implemented in the Calliope
Python library.

Rejected because it's overengineered for a shuffle button. Simulated annealing
requires hundreds to thousands of iterations to converge, with full penalty
recalculation at each step. On a mobile device, this means either a noticeable
delay before the first track plays, or asynchronous optimization that the user
doesn't see. The algorithm belongs in *playlist generation* tools (build me a
2-hour running mix with these constraints), not in a shuffle toggle that should
respond instantly.

**Needleman-Wunsch / Smith-Waterman dynamic programming (Bountouridis et al.,
2017).** Adapted sequence alignment to music similarity using substitution
matrices and affine gap penalties. Their substitution matrix concept (artist→artist
transition affinity) is intellectually interesting but designed for aligning
two existing sequences, not for generating one from scratch. The dynamic
programming approach is O(n×m) and requires a known target sequence — it's a
different problem entirely.

**What we kept and what we modified:**

| Concept | Source field | What we kept | What we changed |
|---|---|---|---|
| Penalty function minimization | Simulated annealing (Pauws) | Soft constraint violation as a continuous cost to minimize | Replaced iterative cooling with independent random draws — eliminates convergence time entirely |
| Monte Carlo sampling | MCMC in genomics | Sample from permutation space, evaluate against a quality function | Independent draws instead of a Markov chain — simpler, no mixing-time concerns, trivially parallel |
| Multi-candidate selection | Spotify "Fewer Repeats" | Generate K permutations, score, pick best | Session-local scoring only; no user profile, no audio features, no server |
| k-let counting | Eulerian shuffling (Altschul-Erickson) | Inspiration for what *not* to do — confirmed we want diversification, not preservation | Not used; documented for negative result |

The penalty function itself is original to this design: same-artist adjacency
with a single constant weight, same-album with a softer weight, and a
linear-decay recency term. No pre-existing scoring matrix was ported because
none is appropriate — the substitution matrices (BLOSUM, PAM) encode
evolutionary substitution likelihoods, which have no analogue in playlist
sequencing.

### Data model change

`PlaybackMode.kt` — add `SmartShuffle` variant:

```kotlin
enum class PlaybackMode {
    Repeat,
    RepeatOne,
    Shuffle,        // existing — becomes "Pure Shuffle" in UI
    SmartShuffle    // new
}
```

The `SavedPlayerState.playbackMode` serialization is ordinal-based, so adding
at the end is safe — existing stored ordinals don't change.

### Integration points

**PlayerViewModel.kt — `setPlayerPlaybackMode()`:**

```
PlaybackMode.Shuffle ->        shuffleModeEnabled = true,  repeatMode = REPEAT_MODE_ALL
PlaybackMode.SmartShuffle ->   shuffleModeEnabled = true,  repeatMode = REPEAT_MODE_ALL
                               // same player settings; the difference is in how
                               // the shuffled index array is generated
```

**PlayerViewModel.kt — `OnPlaybackModeClick`:** cycle order becomes
`Repeat → RepeatOne → Shuffle → SmartShuffle → Repeat`.

**New component: `ShuffleEngine`** (domain layer, no Android dependencies):

```
class ShuffleEngine(
    private val random: Random = Random,
) {
    fun generateOrder(
        tracks: List<Track>,
        mode: PlaybackMode,
        previousLoopTracks: Set<String> = emptySet(),
    ): IntArray
}
```

Pure mode: `IntArray(n) { it }.apply { shuffle(random) }`.
Smart mode: generate K copies, score each with penalty function, return the min.

**Media3 bridge:** Custom `ShuffleOrder` implementation that holds the
`IntArray` from `ShuffleEngine`. On each loop cycle (detected via
`STATE_ENDED` or `onMediaItemTransition` wrapping around), a new order is
generated and applied. The existing `REPEAT_MODE_OFF` + manual restart at
`STATE_ENDED` workaround documented in Media3 Issue #2334 is used — it has a
brief gap but avoids the `setShuffleOrder` mid-playback interruption bug
(Issue #1381).

**UI:** Playback mode button cycles through 4 states. Icon changes:
Repeat (loop icon), RepeatOne (loop-1 icon), Shuffle (shuffle icon),
SmartShuffle (shuffle icon with a small star/sparkle). String resources
for content descriptions.

### Testing plan

All tests in `app/src/test/` — pure JVM, no emulator needed.

#### Unit tests: ShuffleEngine

**Pure mode:**
- `pure shuffle on 3 tracks produces valid permutation` — all 6 permutations
  appear over many trials (chi-squared test with 1000 draws, α=0.01)
- `pure shuffle on 1 track returns [0]`
- `pure shuffle on empty list returns []`
- `pure shuffle with seeded RNG is deterministic` — same seed = same order

**Smart mode:**
- `smart shuffle on 3 tracks produces valid permutation` — all indices present
- `smart shuffle on 1 track returns [0]`
- `smart shuffle on empty list returns []`
- `same-artist adjacency penalty: 3 Radiohead + 2 other shuffled — Radiohead
  tracks are never adjacent` (property: over 100 trials with K=5, all
  candidates should have at least one non-adjacent arrangement on a realistic
  library)
- `smart shuffle with seeded RNG is deterministic` — same tracks + same
  previous-loop set + same seed = same winner
- `smart shuffle returns the lowest-penalty candidate` — inject hand-crafted
  candidates, verify the min-penalty one is selected

**Penalty function:**
- `zero penalty for perfect alternation` — alternating artists give penalty=0
- `artist adjacency penalty scales linearly` — 3 adjacent same-artist tracks
  contribute 2 × ARTIST_ADJACENCY_WEIGHT
- `album adjacency penalty is independent of artist penalty` — same artist,
  same album → only artist weight applies (no double count)
- `recent-position penalty is highest at position 0` — verify the
  linear-decay term
- `recent-position penalty is ~0 at last position` — verify decay

#### Statistical tests:

- `chi-squared test for pure shuffle uniformity` — 600 draws on 3-track
  playlist, 6 possible permutations, expected 100 each, χ² < critical
  value at α=0.01
- `smart shuffle artist spacing above random baseline` — measure average
  same-artist-adjacency count over 100 Smart shuffles vs 100 Pure shuffles
  on a constructed playlist with 5 artists × 10 tracks each. Smart should
  show significantly fewer adjacencies (t-test, p < 0.001)
- `smart shuffle does not degenerate to fixed order` — over 100 trials on a
  20-track playlist with 5 artists, unique-permutation rate should exceed 90%

#### Edge cases:

- All tracks same artist: penalty is irreducible, Smart falls back to any
  valid permutation (same as Pure in this degenerate case)
- All tracks different artists: Smart and Pure should both produce zero
  artist adjacency (Smart has no penalty to reduce)
- Very large playlist (10,000 tracks): completes in under 50ms on a
  representative Android CPU (benchmark test with time assertion)
- Playlist with single track: both modes return [0]

#### Regression tests:

- `OnPlaybackModeClick cycles through 4 modes` — Repeat → RepeatOne →
  Shuffle → SmartShuffle → Repeat
- `SavedPlayerState survives mode upgrade` — state saved with old 3-mode
  ordinal loads correctly (ordinal 2 = Shuffle, ordinal 3 = SmartShuffle
  maps to new enum)

### What we don't do

- No user-facing penalty weights. The three weights are constants.
- No ML model for artist similarity or genre embedding.
- No persistent shuffle history beyond the current loop. Each loop cycle
  resets the `previousLoopTracks` set.
- No network calls. The penalty function reads only track metadata already
  in memory (artist, album).
- No "Smart Shuffle inserts recommendations" — that's Spotify's separate
  Smart Shuffle feature (sparkle icon). Ours is a shuffling strategy, not
  a discovery feature.

### Implementation order

1. `ShuffleEngine` + unit tests (the pure domain logic — no Android, no Media3)
2. `PlaybackMode.SmartShuffle` enum variant + `SavedPlayerState` migration
3. `PlayerViewModel` integration (mode cycling, `setPlayerPlaybackMode`)
4. Media3 bridge (custom `ShuffleOrder` or `STATE_ENDED` workaround)
5. UI: updated playback mode button with 4-state cycle
6. String resources + content descriptions
7. Manual testing on device with real playlists
