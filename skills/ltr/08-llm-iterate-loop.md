# Skill 08 — LLM Iterate Loop

## What it does
After each simulation run, the iterate prompt asks the LLM to patch the current mutable state
to move closer to the targets. This skill describes what the LLM can and cannot change.

## Patchable keys (per-iteration JSON patch)
| Key              | Type             | Cost      | Effect |
|------------------|------------------|-----------|--------|
| `weights`        | array\<double\>  | instant   | Probability of each reel set. Must be same length as current weights array. |
| `paytable`       | object           | instant   | Patch per-symbol multipliers: `{ symbolId: { matchCount: multiplier } }` |
| `gapPhases`      | int (1–5)        | rebuild   | Number of no-win reel sets. More phases → more no-win pool variety. |
| `winVecDecay`    | double (0.4–0.95)| rebuild   | Win strip tile decay per reel. Lower = steeper → more 3x, fewer 5x wins. |
| `symsPerReel`    | int (64–512)     | rebuild   | Total tiles per reel. Higher → finer weight granularity. |
| `targetVolatility`| string          | rebuild   | Changes paytable scale and win strip decay simultaneously. |
| `maxIterations`  | int              | none      | Use bisection rule to set next iteration's cap. |
| `seed`           | int              | none      | Change to explore a different RNG region. |

"Instant" means no strip rebuild — weight/paytable changes take effect immediately.
"Rebuild" means all reel strips + paytable + weights are regenerated from scratch.

## Strategy: prefer instant patches first
1. **Weights too low for wins** → increase win-set weights, decrease no-win weights.
2. **RTP too high** → increase no-win weights OR lower paytable multipliers for senior symbols.
3. **Hit rate too low** → decrease no-win weights.
4. **Hit rate too high** → increase no-win weights.
5. **All wins concentrated in 3x** → lower `winVecDecay` (rebuilds) or lower `targetVolatility`.
6. **Cannot converge after 3 instant iterations** → request structural rebuild via `symsPerReel` or `gapPhases`.

## Bisection rule for maxIterations (always apply)
```
gapRatio = |actualRtp - targetRtp| / targetRtp

if gapRatio > 0.20:  maxIterations = min(current × 2, 200)
elif gapRatio > 0.05: maxIterations = min(current + 40, 200)
else:                 maxIterations = min(current + 20, 200)
```

## Rules
- Never output `targetRtp`, `rtpDelta`, `targetHitRate`, `hitRateDelta`, `symbols`, or `lines`.
- If `maxPayout > 0`: never patch any multiplier above `maxPayout`. The engine enforces the cap automatically.
- Weights patch: recalculate the full array so all values > 0 and sum = 1.0.
- Paytable patch: only include symbols you actually want to change.
- Structural patches (rebuild) should be used sparingly — prefer weight/paytable first.

## Simulation output fields available
```
simulation.rtp               — actual RTP achieved
simulation.hitRate           — actual hit rate achieved
simulation.maxWin            — largest single spin win
simulation.volatilityIndex   — stdDev/mean (dimensionless)
simulation.volatilityLabel   — Low / Casual / High / Very High / Extreme / Ultra Extreme
simulation.hitDistribution   — per symbol, per match length: hits + hitRatePct
```
