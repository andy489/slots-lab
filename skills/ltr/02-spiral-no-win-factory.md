# Skill 02 — Spiral No-Win Reel Set Factory

## What it does
Produces `gapPhases` no-win reel sets (default = `minMatch`). In each no-win set, at least
one reel in every possible winning run has zero tiles for that symbol — so no LTR win of
length ≥ `minMatch` can occur.

## Tunable parameters
| Parameter     | Range | Effect |
|---------------|-------|--------|
| `screenWidth` | 3–10  | More reels = wider win check window. |
| `gapPhases`   | 1–5   | Number of no-win reel sets produced. More phases = bigger no-win pool = more weight tuning flexibility. Default = minMatch. |
| `minMatch`    | 2–5   | Shorter minMatch = easier to block wins = sparser no-win sets. |

## What you cannot tune here
This skill consumes `baseCounts` from skill 01. Its output quality depends entirely on skill 01 inputs.

## Diagnosis: if hit rate is too high
→ Increase `gapPhases` to add more no-win sets to the pool (LLM can patch this directly).
→ Or increase no-win weights in the `weights` patch.

## Diagnosis: if hit rate is too low
→ Decrease no-win weights in the `weights` patch.
→ Or decrease `gapPhases` to reduce the no-win pool.
