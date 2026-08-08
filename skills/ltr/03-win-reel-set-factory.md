# WinReelSetFactory

## Purpose
For each payout symbol, produce one winning reel set by injecting a decreasing
win-count vector into the symbol's column across reels, derived from one of the
no-win reel sets.

## Inputs
| Field         | Type            | Description                                                                           |
|---------------|-----------------|---------------------------------------------------------------------------------------|
| noWinReelSets | List<int[][]>   | Output of SpiralNoWinReelSetFactory                                                   |
| symbols       | List<SymbolDef> | Symbol list (id, tier, hint)                                                          |
| baseCounts    | int[]           | Base counts (for residual fill values)                                                |
| screenWidth   | int             | Number of reels                                                                       |
| volatility    | String          | LOW / CASUAL / HIGH / VERY_HIGH / EXTREME / ULTRA_EXTREME — controls win vector slope |
| residualFill  | int             | Count to substitute for other zeroes not part of the win (default 8)                  |

## Outputs
`List<int[][]>` — one winning reel set per non-special symbol (junior + senior tiers).
Wilds and scatters do not get dedicated winning reel sets.

## Algorithm
### Win vector
For symbol with index i across screenWidth reels, generate vector W of length screenWidth:
- W[0] = winPeak (highest count, on reel 1 — the trigger reel for LTR)
- W[r] = W[r−1] * decayFactor, rounded to nearest int, minimum 1
- decayFactor per volatility:
  - LOW          → 0.94
  - CASUAL       → 0.88
  - HIGH         → 0.78
  - VERY_HIGH    → 0.68
  - EXTREME      → 0.55
  - ULTRA_EXTREME→ 0.45
- winPeak for `senior` symbols = baseCounts[i] * 1.5 (rounded)
- winPeak for `junior` symbols = baseCounts[i] * 1.2 (rounded)

### Injection
1. Select the no-win reel set at index `i mod noWinReelSets.size()`.
2. For each reel r: set `reelSet[r][symbolIndex] = W[r]`.
3. For every other symbol S ≠ the payout symbol: if `reelSet[r][S] == 0`, replace
   it with `residualFill` to avoid dead reels for other symbols.

## Constraints
- The win vector always decreases left-to-right (LTR decay), matching LTR payout logic.
- residualFill must be > 0 and < min(baseCounts) so residual symbols do not dominate.
