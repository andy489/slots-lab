# Skill 03 — Win Reel Set Factory

## What it does
Produces one dedicated winning reel set per non-special symbol (junior + senior). Each win reel set boosts the target symbol's tile counts left-to-right across reels, making LTR wins more likely when that reel set is chosen.

## Tunable parameters
| Parameter          | Range    | Effect |
|--------------------|----------|--------|
| `targetVolatility` | see below | Default decay factor source. Higher volatility = steeper decay = more 3-of-a-kind, fewer 5-of-a-kind. |
| `winVecDecay`      | 0.40–0.95 | Direct override of the decay factor. Takes precedence over volatility. LLM can patch this to fine-tune win length distribution without changing the full volatility. |

## Decay factor by volatility
| volatility     | decayFactor | Effect |
|----------------|:-----------:|--------|
| LOW            | 0.94        | Counts drop slowly — 5-of-a-kind more likely |
| CASUAL         | 0.88        | Mild decay |
| HIGH           | 0.78        | Noticeable drop-off after reel 3 |
| VERY_HIGH      | 0.68        | Strong left-bias |
| EXTREME        | 0.55        | Very steep — rare 5-of-a-kind |
| ULTRA_EXTREME  | 0.45        | Almost always 3-of-a-kind |

## Win peak (how many tiles on reel 1)
- senior symbol: `baseCounts[i] × 1.5`
- junior symbol: `baseCounts[i] × 1.2`

## Within-tier rank rule
Higher symbolId within its tier → lower win peak (fewer tiles on reel 1 in win set → lower frequency win reel chosen by tuner).

## Diagnosis: if 5-of-a-kind hits are too rare
→ Increase `targetVolatility` is wrong. Instead lower `targetVolatility` to increase decay factor.
→ Or increase `symsPerReel` to raise the base counts from which win peaks are derived.

## Diagnosis: if wins occur only for some symbols
→ Ensure all non-special symbols are present in the symbols list with distinct IDs.
