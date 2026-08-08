# PaytableGenerator

## Purpose
Produce a paytable for every symbol: a mapping from match count (minMatch … screenWidth)
to a stake multiplier (multiple of 0.10, minimum 0.10).

## Inputs
| Field       | Type            | Description                                                                             |
|-------------|-----------------|-----------------------------------------------------------------------------------------|
| symbols     | List<SymbolDef> | Symbol list (id, tier, hint)                                                            |
| minMatch    | int             | Minimum match count (smallest entry in paytable)                                        |
| screenWidth | int             | Maximum match count (largest entry in paytable)                                         |
| volatility  | String          | LOW / CASUAL / HIGH / VERY_HIGH / EXTREME / ULTRA_EXTREME — controls multiplier scaling |

## Outputs
`Map<Integer, Map<Integer, Double>>` — keyed by symbolId → (matchCount → multiplier).

## Algorithm
### Base multipliers for minMatch hits
| Tier      | LOW                           | CASUAL | HIGH | VERY_HIGH | EXTREME | ULTRA_EXTREME |
|-----------|-------------------------------|--------|------|-----------|---------|---------------|
| junior    | 0.10                          | 0.10   | 0.20 | 0.30      | 0.50    | 0.80          |
| senior    | 0.40                          | 0.80   | 1.50 | 3.00      | 6.00    | 12.00         |
| wild      | same as senior rank           |
| scatter   | 1.00 fixed for any volatility |
| multiwild | 2× senior value               |

### Growth per additional match
Each additional matched reel multiplies the previous value by a growth factor:
- LOW          → 2.0×
- CASUAL       → 3.5×
- HIGH         → 5.0×
- VERY_HIGH    → 8.0×
- EXTREME      → 12.0×
- ULTRA_EXTREME→ 20.0×

### Rounding rule
Every computed multiplier is rounded to the nearest 0.10 (i.e. `round(v / 0.10) * 0.10`).
Minimum allowed value is 0.10.

### Example (CASUAL, junior, minMatch=3, screenWidth=5)
- match 3 → 0.10
- match 4 → 0.40   (0.10 × 3.5, rounded to nearest 0.10 → 0.40)
- match 5 → 1.40   (0.40 × 3.5, rounded → 1.40)

## Constraints
- Paytable must be strictly increasing: each longer match pays more than the shorter.
- All values must be positive multiples of 0.10.
