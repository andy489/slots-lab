# Skill 05 — Reel Set Weight Seeder

## What it does
Seeds the initial probability distribution over all reel sets (no-win + per-symbol win sets).
The weights are a `double[]` array whose values sum to 1.0.

This skill no longer runs a convergence loop. Weights are seeded once per build and then
refined each iteration by the LLM using the iterate prompt.

## Seeding formula
```
noWinShare = clamp(1 - targetRtp/100, 0.03, 0.55)
winShare   = 1 - noWinShare
```

- Example: targetRtp=95.5 → noWinShare=0.045, winShare=0.955
- Example: targetRtp=70   → noWinShare=0.30,  winShare=0.70

Within win sets, weights are distributed by tier and inverse rank:
- Junior symbols: higher initial weight than senior.
- Within each tier: lower symbolId → higher initial weight (more tiles → more frequent wins).

## Simulation
Provides a `simulate()` method used by the iterate loop. Each call runs N spins (default
1 000 000) and returns: `rtp`, `hitRate`, `maxWin`, `stdDev`, `volatilityIndex`,
`volatilityLabel`, `hitDistribution`.

## What the LLM can patch
The LLM iterate call may patch weights directly in its response:
```json
{
  "weights": [0.02, 0.02, 0.01, 0.18, 0.22, 0.19, 0.17, 0.19]
}
```
Rules for a valid weights patch:
- Length must match the current `weights` array (noWinCount + winCount).
- All values must be > 0.
- Values will be normalised to sum = 1.0 automatically.

## Diagnosis: if RTP converges but hit rate does not
→ No-win weights are too low. Increase no-win entries in the weights array.
→ Or increase `gapPhases` (adds more no-win reel sets to the pool).

## Diagnosis: if neither RTP nor hit rate improves after multiple iterations
→ The seeded starting point may be too far from the target.
→ Try: increase `symsPerReel` (finer granularity).
→ Try: change `targetVolatility` (affects paytable and win-vec decay).
