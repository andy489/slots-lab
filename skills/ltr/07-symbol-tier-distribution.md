# Skill 07 — Symbol Tier Distribution (Invariants)

## Core invariants — ALWAYS check these in simulation output

### I-1 — Junior hits more than senior (at every match length)
```
hitRatePct(junior_sym, k) > hitRatePct(senior_sym, k)   for all k
```
**If violated**: junior and senior tile counts are too close.
→ Fix: use a lower `targetVolatility` level (increases seniorFactor gap).
→ Or: explicitly set `targetVolatility` to VERY_HIGH or EXTREME.

### I-2 — Shorter match hits more than longer match (for same symbol)
```
hitRatePct(sym, k) > hitRatePct(sym, k+1)   for all sym, all k
```
**If violated**: reel structure is degenerate (very unlikely with correct skill 01 + 02).
→ Fix: increase `symsPerReel` and change `seed`.

### I-3 — Senior pays more than junior (at every match length)
```
payout(senior_sym, k) > payout(junior_sym, k)   for all k
```
**Guaranteed by paytable generator** if both tiers are present with distinct symbolIds.

### I-4 — Longer match always pays more
```
payout(sym, k+1) > payout(sym, k)   for all sym, all k
```
**Guaranteed by paytable generator** via growthFactor > 1.

### I-5 — Rough RTP balance across tiers
Junior symbols provide background/frequent RTP.
Senior symbols provide excitement peaks.
Neither tier should contribute 0% to the total RTP.

## Volatility gap table (payout ratio senior/junior at minMatch)
| volatility     | approx gap |
|----------------|:----------:|
| LOW            | 2.5×       |
| CASUAL         | ~2.8×      |
| HIGH           | ~3.3×      |
| VERY_HIGH      | 4.0×       |
| EXTREME        | ~4.4×      |
| ULTRA_EXTREME  | ~5.3×      |

The **hit-rate gap must mirror the payout gap**. If senior pays 4× more, it must hit ~4× less often (per I-5).

## Rank within tier (by symbolId, ascending = lower rank)
- Lower symbolId in tier → more tiles → higher hit rate → lower pay.
- Higher symbolId in tier → fewer tiles → lower hit rate → higher pay.
- Scale ±30% around tier average (0.70× to 1.30×), linear between extremes.

## Quick diagnosis checklist
| Symptom | Root cause | Fix |
|---------|-----------|-----|
| All symbols identical hit rate | Tier gap too small | Increase `targetVolatility` level |
| Senior hits as often as junior | `seniorFactor` too high | Increase `targetVolatility` |
| Junior pays same as senior | Same tier or duplicate symbolIds | Check symbols list |
| Converged=false | Too few iterations | maxIterations += 40 (cap 200) |
| RTP fine but hit rate off | No-win pool too small | Change `seed`, relax `hitRateDelta` |
