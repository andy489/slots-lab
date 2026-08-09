# Skill 04 — Paytable Generator

## What it does
Assigns a stake multiplier to every (symbol, match-length) pair. All multipliers are multiples of 0.10, minimum 0.10.

## Tunable parameters
| Parameter          | Effect |
|--------------------|--------|
| `targetVolatility` | Sets both the base multiplier and the growth per additional match. Higher volatility = bigger gap between junior and senior AND steeper climb per extra reel match. |
| `minMatch`         | The smallest match length that pays. Determines the first entry in each symbol's paytable. |
| `screenWidth`      | The largest match length (5-of-a-kind on a 5-reel game). |
| `maxPayout`        | Hard cap on any single multiplier (0 = uncapped). The engine enforces this automatically; never patch a value above it. |

## Junior base multipliers (at minMatch)
Junior base is seeded lower when `targetHitRate` is high — sub-1x values are intentional and correct:
| volatility     | junior base at HR=5% | junior base at HR=50% |
|----------------|:--------------------:|:---------------------:|
| LOW            | 0.80                 | 0.10                  |
| CASUAL         | 1.20                 | 0.10                  |
| HIGH           | 2.00                 | 0.10                  |
| VERY_HIGH      | 3.50                 | 0.10                  |
| EXTREME        | 6.00                 | 0.10                  |
| ULTRA_EXTREME  | 10.00                | 0.10                  |

Values between HR=5% and HR=50% are linearly interpolated.  
**Key insight:** sub-1x junior multipliers (0.1, 0.2, 0.5) allow juniors to win frequently without inflating RTP — a junior win returns less than the bet, so high hit rate is achievable without blowing the RTP budget.

## Senior base multipliers (at minMatch)
| volatility     | senior base | wild (×1.5) | multiwild (×2.0) |
|----------------|:-----------:|:-----------:|:----------------:|
| LOW            | 2.50        | 3.75        | 5.00             |
| CASUAL         | 5.00        | 7.50        | 10.00            |
| HIGH           | 10.00       | 15.00       | 20.00            |
| VERY_HIGH      | 20.00       | 30.00       | 40.00            |
| EXTREME        | 40.00       | 60.00       | 80.00            |
| ULTRA_EXTREME  | 80.00       | 120.00      | 160.00           |

Scatter is always 2.00 fixed.

## Growth factor per additional matched reel
| volatility     | growthFactor | Effect |
|----------------|:------------:|--------|
| LOW            | 2.5×         | Mild jump from 3x to 5x wins |
| CASUAL         | 3.0×         | Moderate |
| HIGH           | 4.0×         | Noticeable escalation |
| VERY_HIGH      | 6.0×         | Rapid — 5-of-a-kind dwarfs 3-of-a-kind |
| EXTREME        | 10.0×        | Very steep — 5x win is 100× the 3x base |
| ULTRA_EXTREME  | 16.0×        | Extreme spike at 5-of-a-kind |

Senior symbols must increase **faster** per extra reel than juniors. At higher volatility the growth factor itself is larger, amplifying this effect.

## Within-tier rank rule (by symbolId)
Within each tier, symbolId rank determines the actual base multiplier:
- Rank 0 (lowest id) → base × 0.70
- Rank N-1 (highest id) → base × 1.30
- Linear interpolation between extremes.

Full ordering: junior-id1 (cheapest) < junior-id2 < ... < senior-id1 < ... < senior-idN (most expensive).

## Invariants (must always hold)
- `payout(senior, k) > payout(junior, k)` for all match lengths k (I-3)
- `payout(sym, k+1) > payout(sym, k)` for all symbols (I-4)
- Senior-to-junior gap must widen as volatility increases — never narrow it when patching.

## Paytable patch guidance for the LLM
- **Hit rate too low, RTP on target** → lower junior multipliers toward 0.10–0.50. Sub-1x juniors are valid and correct — a win that returns 0.2× the bet still counts as a hit. This is the primary lever for increasing hit rate without raising RTP.
- **Hit rate too high, RTP too low** → raise junior multipliers back toward 1x–2x.
- **RTP too high, senior symbols dominant** → reduce senior multipliers. Do NOT raise junior to compensate — that inflates hit-rate RTP.
- **RTP too low, want bigger wins** → raise senior 4x/5x multipliers using the growth factor as a guide.
- **Volatility feels low despite VERY_HIGH setting** → widen the senior-junior gap: raise senior base, lower junior base.
- **Never** patch junior multipliers above senior multipliers at any match length.
- **Sub-1x junior example** (high hit rate target):
  ```json
  "paytable": { "1": {"3": 0.2, "4": 0.5, "5": 1.0}, "2": {"3": 0.3, "4": 0.7, "5": 1.5} }
  ```

## Diagnosis: if RTP is too high despite low hit rate
→ Senior payouts are too large. Lower `targetVolatility` by one level.
→ Or reduce `screenWidth` (fewer reels = no 5-of-a-kind tier).

## Diagnosis: if senior and junior multipliers are identical
→ Symbols may all have the same tier. Verify symbols list has both "junior" and "senior" tier entries.
