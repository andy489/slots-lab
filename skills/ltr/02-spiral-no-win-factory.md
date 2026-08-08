# SpiralNoWinReelSetFactory

## Purpose
Build `screenHeight` no-win reel sets from the base count vector using the spiral
zero-placement method. Each reel set guarantees that no pay line can produce a
winning combination of length greater than or equal to minMatch.

## Inputs
| Field        | Type  | Description                                              |
|--------------|-------|----------------------------------------------------------|
| baseCounts   | int[] | Base count per symbol (output of SymbolCountInitialiser) |
| screenWidth  | int   | Number of reels (columns)                                |
| screenHeight | int   | Number of visible rows                                   |
| minMatch     | int   | Minimum consecutive matching reels for a win             |

## Outputs
`List<int[][]>` — exactly `screenHeight` reel sets.
Each reel set is `int[screenWidth][numSymbols]` — counts per symbol per reel.

## Algorithm
### Zero-spacing rule
- On each reel within a reel set, every symbol that participates in potential winning
  lines has its count set to 0 at intervals spaced `minMatch − 1` reels apart.
- This ensures that no uninterrupted run of `minMatch` reels can all have that symbol
  with a non-zero count.

### Phase shift
- No-win reel set #k (k = 0 … screenHeight−1) places the first zero for reel 1 at
  position offset `k` in the reel sequence.
- Subsequent reels in the same reel set continue the spacing from that offset.

### Construction steps
1. Start with a full copy of `baseCounts` for every reel.
2. For phase k, zero out symbol positions according to the spacing rule starting at
   column k mod screenWidth.
3. Return the `screenHeight` resulting reel-count matrices.

## Constraints
- A zero for symbol S on reel R means that symbol S will never appear on reel R in
  this reel set, preventing any line win involving S passing through R.
- Non-zero counts are preserved unchanged from `baseCounts`.
