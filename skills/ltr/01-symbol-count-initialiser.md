# Skill 01 — Symbol Count Initialiser

## What it does
Assigns tile counts to each symbol on every reel.
More tiles = higher hit frequency. Fewer tiles = rarer but more exciting hits.

## Tunable parameters
| Parameter      | Range     | Effect |
|----------------|-----------|--------|
| `symsPerReel`  | 64–512    | Total tiles per reel. Increase to get smoother probability distributions. |
| `targetVolatility` | see below | Controls how many fewer tiles senior symbols get vs junior. |

## Volatility → senior tile ratio
| volatility     | seniorFactor | Effect |
|----------------|:------------:|--------|
| LOW            | 0.65         | seniors are 65% as common as juniors — small gap |
| CASUAL         | 0.50         | seniors are 50% as common — moderate gap |
| HIGH           | 0.35         | seniors noticeably rarer |
| VERY_HIGH      | 0.22         | seniors much rarer |
| EXTREME        | 0.13         | seniors very rare |
| ULTRA_EXTREME  | 0.07         | seniors near-jackpot rare |

## Within-tier rank rule (by symbolId)
- Within junior tier: **lower symbolId → more tiles** (higher hit rate).
- Within senior tier: **lower symbolId → more tiles** (higher hit rate).
- Scale range ±30% around tier base (0.70× to 1.30×), linear by rank.

## Special symbol fixed weights
- wild: 0.20 | scatter: 0.15 | multiwild: 0.12 (never changed by volatility)

## Diagnosis: if junior hit rate ≤ senior hit rate
→ Lower `targetVolatility` (increases seniorFactor) OR reduce `symsPerReel` and rely on wider weight ratios.
→ Do NOT change `lines` or `minMatch` to fix tier ordering.

## Diagnosis: if all symbols have identical hit rates
→ Symbols list may all have the same tier. Ensure junior and senior tiers are both present with different symbolIds.
