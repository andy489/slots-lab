# SymbolCountInitialiser

## Purpose
Compute the base symbol count vector for each reel given the symbol list, target volatility,
and symbols-per-reel value. Senior (high-value) symbols receive fewer counts than junior
(low-value) symbols; the gap magnitude scales with volatility.

## Inputs
| Field            | Type            | Description                                               |
|------------------|-----------------|-----------------------------------------------------------|
| symbols          | List<SymbolDef> | Ordered list of symbols (id, tier, hint)                  |
| volatility       | String          | LOW / CASUAL / HIGH / VERY_HIGH / EXTREME / ULTRA_EXTREME |
| symsPerReel      | int             | Target total count per reel (e.g. 256)                    |
| symsPerReelDelta | int             | Allowed ±deviation from symsPerReel (e.g. 16)             |

## Outputs
`int[]` — base count for each symbol in the same order as `symbols`.
Sum of counts ≈ `symsPerReel` (within delta).

## Algorithm
1. Assign a weight to each tier:
   - `junior`    → 1.0
   - `senior`    → volatilityFactor (see table below)
   - `wild`      → 0.20 (fixed low, always available)
   - `scatter`   → 0.15 (fixed very low)
   - `multiwild` → 0.12 (fixed lowest)
2. Volatility factor for `senior` tier:
   - LOW          → 0.80
   - CASUAL       → 0.65
   - HIGH         → 0.45
   - VERY_HIGH    → 0.30
   - EXTREME      → 0.18
   - ULTRA_EXTREME→ 0.10
3. Raw count for symbol i = `floor(symsPerReel * weight_i / sumOfWeights)`.
4. Distribute remainder (symsPerReel − sum) to junior symbols one each until balanced.
5. Clamp every count to minimum 1.

## Constraints
- Every symbol must receive at least 1 count.
- Wilds and scatters are not affected by volatility parameter.
