# Skill 06 — Restriction Builder

## What it does
Sets stack sizes and stack probabilities for each reel set's physical reel strip. A "stack" is a run of the same symbol appearing consecutively on the reel strip.

## Tunable parameters
| Parameter       | Range  | Effect |
|-----------------|--------|--------|
| `screenHeight`  | 2–6    | Determines max stack size (= screenHeight + 2). |
| `targetHitRate` | 5–50   | Controls stack bias for **both** no-win and win sets (see below). |

## Stack bias rules
- **No-win sets**: lower HR → smaller stacks (left-shifted) → symbol lands on fewer rows → fewer accidental wins.
- **Win sets**: HR-dependent — higher HR → smaller stacks (more individual tiles, more frequent hits); lower HR → larger stacks (symbol clusters on screen, rarer but multi-row wins).
  - At HR=5% (low): win sets use maximum right-shift (WIN_SHIFT=8.0) — large stacks.
  - At HR=50% (high): win sets use slight left-shift (WIN_SHIFT=−2.0) — small stacks.
- `minDistance = 1` always (distance between consecutive same-symbol positions).

## Diagnosis: hit rate too low despite high targetHitRate
→ Win sets may still have oversized stacks. This is now auto-corrected by `targetHitRate`.
→ If still insufficient: increase `targetHitRate` or reduce `screenHeight` (smaller max stack).

## Diagnosis: game feels too "choppy" (wins cluster then long dry spells)
→ `targetHitRate` is low, causing large win-set stacks. Raise `targetHitRate` or raise `targetVolatility`.

## You should NOT adjust this skill to fix
- RTP — fix via paytable (skill 04) or weight tuner (skill 05).
- Tier hit-rate ordering — fix via symbol counts (skill 01).
- Convergence failures — fix via maxIterations (skill 05).
