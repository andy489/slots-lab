# RestrictionBuilder

## Purpose
Compute a `Restriction` (stack sizes, stack chances, min distance) for a reel set given
the screen height, target hit rate, and whether the reel set is a win-type or no-win-type.
The restriction drives `RestrictionsApplier` inside `ShuffleGenerator` to produce
physically realistic stacked reel strips.

## Inputs
| Field         | Type    | Description                                                   |
|---------------|---------|---------------------------------------------------------------|
| screenHeight  | int     | Number of visible rows (determines max stack size)            |
| targetHitRate | double  | Target hit rate in % (0–100)                                  |
| isWinSet      | boolean | true = win reel set (favour larger stacks); false = no-win    |

## Outputs
`Restriction` — three fields:
- `stacks`    : `List<Integer>` — stack sizes `[1, 2, …, screenHeight+2]`
- `chances`   : `List<Double>`  — probability weights (sum = 100), one per stack size
- `distance`  : `int`           — min distance between consecutive occurrences of same symbol

## Stack Sizes
Max stack size = `screenHeight + 2`.

For `screenHeight = 3`: sizes = `[1, 2, 3, 4, 5]`
For `screenHeight = 4`: sizes = `[1, 2, 3, 4, 5, 6]`

## Base Chance Vector
A symmetric bell-shaped default, chosen so the modal stack size is approximately
`screenHeight / 2 + 1` (middle of the range), summing to exactly 100.

Reference base vectors (rounded integers, sum = 100):

| screenHeight | Stack sizes | Base chances                      |
|-------------|-------------|-----------------------------------|
| 3           | 1–5         | 20, 28, 26, 16, 10                |
| 4           | 1–6         | 14, 22, 26, 20, 11, 7             |
| 5           | 1–7         | 10, 18, 24, 22, 14, 8, 4          |
| 6           | 1–8         | 8, 14, 20, 22, 18, 10, 5, 3       |

For other heights: construct a discrete bell peaked at index `ceil(n/2)` (1-based)
where `n = screenHeight + 2`, taper symmetrically, then normalise to sum 100.

## Hit-Rate Shift
Define normalised hit-rate position `t = clamp(targetHitRate, 5, 50) − 5) / 45`
so that `t=0` is very low HR, `t=1` is high HR.

**No-win sets (`isWinSet = false`)**:
- Low HR (`t → 0`): shift mass **left** — increase weights of smaller stacks (1, 2),
  decrease weights of larger stacks. This reduces the chance of long same-symbol runs
  appearing consecutively on the reel, lowering actual hit frequency.
- High HR (`t → 1`): use base vector unchanged (or minor right shift).

Shift formula for the k-th chance (0-indexed from left):
```
shifted[k] = base[k] + shift_amount * direction[k]
```
where `direction[k] = (n/2 - 1 - k)` (positive for left side, negative for right),
`shift_amount = (1 - t) * maxShift`, `maxShift = 6`.

After shifting, clamp all values to minimum 1, then renormalise to sum 100.

**Win sets (`isWinSet = true`)**:
- Always bias toward larger stacks regardless of target HR: shift mass **right** by a
  fixed amount (`shift_amount = 8`). This clusters the favoured symbol into longer runs,
  increasing the probability of winning combinations landing on paylines.

## Min Distance
`minDistance = 1` in almost all cases.

> **Last-resort rule (almost never used):** If after weight adjustment the simulation
> still cannot converge to the target hit rate, the orchestrator MAY increase
> `minDistance` by 1 (up to `screenHeight`). Avoid this — it significantly alters
> feel and is a blunt instrument. Prefer weight tuning over distance increases.

## Constraints
- `chances` must sum to exactly 100 (after rounding, add/subtract remainder from the
  modal bucket to maintain sum).
- All individual chances ≥ 1.
- `minDistance` ∈ [1, screenHeight].
- Stack sizes are always the full range `[1 … screenHeight+2]`; no sizes are removed.

## Example (screenHeight=3, targetHitRate=15%, isWinSet=false)
```
t = (15 - 5) / 45 = 0.222
shift_amount = (1 - 0.222) * 6 = 4.67 ≈ 5
base     = [20, 28, 26, 16, 10]
direction= [ 2,  1,  0, -1, -2]
shifted  = [30, 33, 26, 11,  5]  (clamped, renormalised to 100, remainder to peak)
→ Restriction([1,2,3,4,5], [30.0,33.0,26.0,11.0,5.0], 1)  // approx, adjust to sum 100
```

## Example (screenHeight=3, isWinSet=true)
```
base     = [20, 28, 26, 16, 10]
direction= [ 2,  1,  0, -1, -2]  (right shift: negate direction)
shift_amount = 8
shifted  = [4, 20, 26, 24, 26]  → renormalise → Restriction([1,2,3,4,5], [4.0,20.0,26.0,24.0,26.0], 1)
```

## Relationship to Volatility
Larger average stack size → slightly more volatile game:
- More large stacks = longer same-symbol runs = higher individual win multipliers when
  wins hit, but fewer distinct win positions = lower hit rate.
- This is a secondary effect; primary volatility control is via the paytable and
  weight tuner. The restriction builder only coarsely nudges volatility.
