# LLM Knowledge Base — All Prompts, Skills, and Context Sent to the Model

This document contains every piece of text sent to the LLM (GPT-4o-mini) during a generation run:
the plan prompt template, the iterate prompt template, and all 8 skill documentation files.

---

## 0. File Locations

**Project root:** `/Users/I777690/repos/Slots_Lab/`

| File | Path |
|------|------|
| Plan prompt builder | `src/main/java/com/slotslab/agent/prompts/PlanPromptBuilder.java` |
| Iterate prompt builder | `src/main/java/com/slotslab/agent/prompts/IteratePromptBuilder.java` |
| LLM service (model init, plan/iterate calls) | `src/main/java/com/slotslab/agent/llm/LlmService.java` |
| Skills registry (loads .md files at startup) | `src/main/java/com/slotslab/agent/skills/FileSystemSkillRegistry.java` |
| Skills root path config | `src/main/resources/application.yml` → `agent.skills-path: skills/` |
| Skill 01 — Symbol Count Initialiser | `skills/ltr/01-symbol-count-initialiser.md` |
| Skill 02 — Spiral No-Win Reel Set Factory | `skills/ltr/02-spiral-no-win-factory.md` |
| Skill 03 — Win Reel Set Factory | `skills/ltr/03-win-reel-set-factory.md` |
| Skill 04 — Paytable Generator | `skills/ltr/04-paytable-generator.md` |
| Skill 05 — Reel Set Weight Seeder | `skills/ltr/05-reel-set-weight-tuner.md` |
| Skill 06 — Restriction Builder | `skills/ltr/06-restriction-builder.md` |
| Skill 07 — Symbol Tier Distribution | `skills/ltr/07-symbol-tier-distribution.md` |
| Skill 08 — LLM Iterate Loop | `skills/ltr/08-llm-iterate-loop.md` |
| Agent orchestration tool | `src/main/java/com/slotslab/agent/tools/LtrReelGenerationTool.java` |
| Agent context + trace | `src/main/java/com/slotslab/agent/execution/ExecutionTrace.java` |
| Debug output directory | `/tmp/agent-debug/<executionId>-iter<N>.json` |

---

## 1. Plan Prompt Template

**Sent once at the start of a run.**  
Built by `PlanPromptBuilder.build()`. Placeholders filled from `AgentRequest` + computed values.

```
You are a slot game math expert. Your job is to choose the best initial generation
parameters for an LTR (left-to-right) reel pipeline based on the targets below.

## Target objectives
- targetRtp:       {targetRtp}%
- rtpDelta:        ±{rtpDelta}%
- targetHitRate:   {targetHitRate}%
- hitRateDelta:    ±{hitRateDelta}%
- targetVolatility: {targetVolatility}
- maxPayout:       {maxPayout}  (0 = uncapped; otherwise no single multiplier may exceed this value)

## User parameters (JSON — copy "symbols" and "lines" exactly if present)
{paramsJson}

## Skill documentation
{skillDocs}

## Bisection rule for maxIterations (MUST follow)
Non-special symbol count (junior + senior) detected: {winSymCount}
Suggested maxIterations: {suggestedIter}  (= symbolCount × 20, clamped 80–200)
Use this value — do NOT go lower.

## RTP math
RTP ≈ Σ(weight[win_set_i] × avg_payout_i)
More win sets → each starts with a smaller share → more iterations needed.
For targetRtp > 90%, use symsPerReel >= 128.

## Paytable design rules
- Symbols are tiered: junior = common, low pay; senior = rare, high pay.
- The paytable must be keyed by symbolId (integer as string) → { matchCount: multiplier }.
- matchCount values must cover minMatch through screenWidth (e.g. 3,4,5 for minMatch=3, width=5).
- Senior multipliers MUST be larger than junior multipliers at every matchCount.
- For targetRtp {targetRtp}% with volatility {targetVolatility}, anchor 3-of-a-kind (minMatch) multipliers roughly:
    Junior  (tier="junior"): 0.3×–2.0× the bet per line
    Senior  (tier="senior"): 3×–10× the bet per line (higher for EXTREME/ULTRA_EXTREME)
- Multiply by ~3× per extra reel for CASUAL, ~4–5× for HIGH/VERY_HIGH.
- If maxPayout > 0, cap every multiplier at maxPayout.

## Initial weights design rules
The weights array has length = noWinSetCount + winSetCount.
noWinSetCount = minMatch (fixed — see spiral no-win factory).
winSetCount   = number of junior + senior symbols.
- No-win share ≈ clamp(1 - targetRtp/100, 0.03, 0.55). Split equally across no-win sets.
- Win share = 1 − no-win share. Distribute across win sets: junior sets get more weight
  (they pay less), senior sets get less weight (they pay more). Use inverse rank scaling.
- All weights > 0, sum = 1.0.

## Your task
Output ONLY a valid JSON object (no markdown, no explanation) with ALL of these keys:
{
  "strategy":        "LTR",
  "screenWidth":     <int, default 5>,
  "screenHeight":    <int, default 3>,
  "minMatch":        <int, default 3>,
  "symsPerReel":     <int, 64–512>,
  "maxIterations":   <int, prescribed above>,
  "spinsPerIter":    <long, default 1000000>,
  "targetVolatility":"<LOW|CASUAL|HIGH|VERY_HIGH|EXTREME|ULTRA_EXTREME>",
  "winVecDecay":     <double, 0.40–0.95, null = use volatility default>,
  "symbols":         [{"symbolId":<int>,"tier":"<junior|senior|wild|scatter|multiwild>","hint":"<string>"}],
  "lines":           [[<row indices per reel>], ...],
  "paytable":        {"<symbolId>": {"<matchCount>": <multiplier>, ...}, ...},
  "weights":         [<double>, ...]
}
The number of no-win reel sets is fixed at minMatch (the distinct spiral break phases) — it is not configurable.
"winVecDecay" overrides the volatility-derived decay factor for win reel strips.
"paytable" and "weights" are your initial guess — they will be refined by the iterate loop.
```

---

## 2. Iterate Prompt Template

**Sent once per non-converged iteration (iterations #1+).**  
Built by `IteratePromptBuilder.build()`. All live values are injected at call time.

```
You are a slot game math expert analysing iteration {iteration} of a reel generation run.
Study the skill docs, original targets, current state, simulation output, and PRIOR ITERATION HISTORY.
Output a JSON patch — only the keys that need to change.

## Original targets (fixed — never output these as patch keys)
- targetRtp:        {targetRtp}%  (±{rtpDelta}%)
- targetHitRate:    {targetHitRate}%  (±{hitRateDelta}%)
- targetVolatility: {targetVolatility}
- maxPayout:        {maxPayout}  (hard cap — no multiplier may exceed this)

## ⚠️  CURRENT GAP DIAGNOSIS — READ CAREFULLY BEFORE PATCHING
- actualRtp detected : {actualRtp}%
- targetRtp          : {targetRtp}%
- absolute gap       : {|rtpGap|}% → {gapLabel}
{urgentDiagnosis}

## Current mutable state
{stateJson}

## Violations detected
{violations}

## Last simulation output
{simJson}

## Prior iteration history (most recent first — learn from what did NOT work)
{historySection}

## Skill documentation
{skillDocs}

## Iteration budget (fixed by the user — you CANNOT change these)
- maxIterations: {nextIterHint}   (hard cap on total iterations — set in the UI, not patchable)
- seed:          {nextSeed}

## Patchable keys (output ONLY the keys you want to change)
| Key             | Type            | Effect |
|-----------------|-----------------|--------|
| weights         | array<double>   | Probability of each reel set. Must sum to ~1.0. Length must match current weights array. |
| paytable        | object          | symbolId → { matchCount: multiplier }. Patch individual symbols. Sub-1x junior multipliers (0.1–0.9) are valid — they create frequent cheap wins without inflating RTP. |
| winVecDecay     | double (0.4–0.95)| Win strip tile decay per reel. Change triggers strip rebuild. |
| symsPerReel     | int (64–512)    | Total tiles per reel. Change triggers strip rebuild. |
| targetVolatility| string          | Volatility level. Change triggers full rebuild (paytable + strips). |

## Mandatory rules
1. For LARGE gap (>20% of target): you MUST patch BOTH paytable AND weights. A weights-only patch is FORBIDDEN.
2. For MEDIUM gap (5–20%): patch paytable AND weights together for fastest convergence.
3. For SMALL gap (≤5%): weights alone or minor paytable tweak is acceptable.
4. Never output targetRtp, rtpDelta, targetHitRate, hitRateDelta, symbols, lines, maxIterations, or seed.
5. Weights patch: recalculate the full array so all values > 0 and sum = 1.0.
6. If maxPayout > 0: never patch any multiplier above maxPayout.
7. Senior multipliers must always exceed junior multipliers at every match length.
8. Do NOT repeat a patch that failed in a prior iteration — look at history and try a DIFFERENT approach.

Output ONLY a valid JSON object. No markdown, no explanation.
```

### Gap diagnosis injected values

**LARGE gap (>20% of target) — RTP too low:**
```
⚠️  CRITICAL: RTP IS {|rtpGap|}% BELOW TARGET ({actualRtp}% vs {targetRtp}%).
Shuffling weights ALONE cannot bridge a {|rtpGap|}% gap — you MUST act on paytable AND weights:
1. RAISE all senior symbol multipliers by 2×–5× (e.g. if 3x=2.0 → set to 4.0–10.0).
2. RAISE junior 5-of-a-kind multipliers to be at least 1× the bet.
3. SHIFT weights heavily toward win reel sets (win weights ≥ 0.60 of total).
4. If still stuck after 2 iterations: lower winVecDecay to 0.50.
DO NOT output only a weights patch — that is INSUFFICIENT for this gap size.
```

**LARGE gap — RTP too high:**
```
⚠️  RTP IS {|rtpGap|}% ABOVE TARGET ({actualRtp}% vs {targetRtp}%).
1. CUT all senior multipliers by 50%.
2. SHIFT weights toward no-win reel sets (no-win weights ≥ 0.60 of total).
3. If still stuck: raise winVecDecay toward 0.90 to thin out win strips.
```

**MEDIUM gap (5–20%) — RTP too low:**
```
RTP is {|rtpGap|}% below target. Raise paytable multipliers for senior symbols by ~1.5×
AND shift weights toward win sets by 20%.
```

**MEDIUM gap — RTP too high:**
```
RTP is {|rtpGap|}% above target. Lower senior multipliers slightly AND shift weights toward no-win sets.
```

**SMALL gap (≤5%):**
```
Gap is small — minor weight redistribution or paytable microadjustment is sufficient.
```

---

## 3. Skill Documentation

The following 8 Markdown files are loaded from `skills/ltr/` at startup and concatenated
into **both** the plan prompt and every iterate prompt under `## Skill documentation`.

---

### Skill 01 — Symbol Count Initialiser
(`skills/ltr/01-symbol-count-initialiser.md`)

Assigns tile counts to each symbol on every reel.
More tiles = higher hit frequency. Fewer tiles = rarer but more exciting hits.

**Tunable parameters**
| Parameter      | Range     | Effect |
|----------------|-----------|--------|
| `symsPerReel`  | 64–512    | Total tiles per reel. Increase to get smoother probability distributions. |
| `targetVolatility` | see below | Controls how many fewer tiles senior symbols get vs junior. |

**Volatility → senior tile ratio**
| volatility     | seniorFactor | Effect |
|----------------|:------------:|--------|
| LOW            | 0.65         | seniors are 65% as common as juniors — small gap |
| CASUAL         | 0.50         | seniors are 50% as common — moderate gap |
| HIGH           | 0.35         | seniors noticeably rarer |
| VERY_HIGH      | 0.22         | seniors much rarer |
| EXTREME        | 0.13         | seniors very rare |
| ULTRA_EXTREME  | 0.07         | seniors near-jackpot rare |

**Within-tier rank rule (by symbolId)**
- Within junior tier: lower symbolId → more tiles (higher hit rate).
- Within senior tier: lower symbolId → more tiles (higher hit rate).
- Scale range ±30% around tier base (0.70× to 1.30×), linear by rank.

**Special symbol fixed weights:** wild: 0.20 | scatter: 0.15 | multiwild: 0.12 (never changed by volatility)

**Diagnosis: if junior hit rate ≤ senior hit rate**  
→ Lower `targetVolatility` (increases seniorFactor) OR reduce `symsPerReel` and rely on wider weight ratios.  
→ Do NOT change `lines` or `minMatch` to fix tier ordering.

**Diagnosis: if all symbols have identical hit rates**  
→ Symbols list may all have the same tier. Ensure junior and senior tiers are both present with different symbolIds.

---

### Skill 02 — Spiral No-Win Reel Set Factory
(`skills/ltr/02-spiral-no-win-factory.md`)

Produces `minMatch` no-win reel sets. In each no-win set, at least one reel in every
possible winning run has zero tiles for that symbol — so no LTR win of length ≥ `minMatch` can occur.

**Tunable parameters**
| Parameter     | Range | Effect |
|---------------|-------|--------|
| `screenWidth` | 3–10  | More reels = wider win check window. |
| `minMatch`    | 2–5   | Shorter minMatch = easier to block wins = sparser no-win sets. |

The number of no-win sets produced is fixed at `min(minMatch, screenWidth)` — it is NOT configurable.

**Diagnosis: if hit rate is too high**  
→ Increase no-win weights in the `weights` patch.

**Diagnosis: if hit rate is too low**  
→ Decrease no-win weights in the `weights` patch.

---

### Skill 03 — Win Reel Set Factory
(`skills/ltr/03-win-reel-set-factory.md`)

Produces one dedicated winning reel set per non-special symbol (junior + senior). Each win reel set
boosts the target symbol's tile counts left-to-right across reels, making LTR wins more likely when
that reel set is chosen.

**Tunable parameters**
| Parameter          | Range    | Effect |
|--------------------|----------|--------|
| `targetVolatility` | see below | Default decay factor source. Higher volatility = steeper decay = more 3-of-a-kind, fewer 5-of-a-kind. |
| `winVecDecay`      | 0.40–0.95 | Direct override of the decay factor. Takes precedence over volatility. |

**Decay factor by volatility**
| volatility     | decayFactor | Effect |
|----------------|:-----------:|--------|
| LOW            | 0.94        | Counts drop slowly — 5-of-a-kind more likely |
| CASUAL         | 0.88        | Mild decay |
| HIGH           | 0.78        | Noticeable drop-off after reel 3 |
| VERY_HIGH      | 0.68        | Strong left-bias |
| EXTREME        | 0.55        | Very steep — rare 5-of-a-kind |
| ULTRA_EXTREME  | 0.45        | Almost always 3-of-a-kind |

**Win peak (how many tiles on reel 1)**
- senior symbol: `baseCounts[i] × 1.5`
- junior symbol: `baseCounts[i] × 1.2`

**Diagnosis: if 5-of-a-kind hits are too rare**  
→ Lower `targetVolatility` to increase decay factor. OR increase `symsPerReel`.

**Diagnosis: if wins occur only for some symbols**  
→ Ensure all non-special symbols are present in the symbols list with distinct IDs.

---

### Skill 04 — Paytable Generator
(`skills/ltr/04-paytable-generator.md`)

Assigns a stake multiplier to every (symbol, match-length) pair. All multipliers are multiples of 0.10, minimum 0.10.

**Tunable parameters**
| Parameter          | Effect |
|--------------------|--------|
| `targetVolatility` | Sets both the base multiplier and the growth per additional match. |
| `minMatch`         | The smallest match length that pays. |
| `screenWidth`      | The largest match length (e.g. 5-of-a-kind on a 5-reel game). |
| `maxPayout`        | Hard cap on any single multiplier (0 = uncapped). |

**Junior base multipliers (at minMatch)**
| volatility     | junior base at HR=5% | junior base at HR=50% |
|----------------|:--------------------:|:---------------------:|
| LOW            | 0.80                 | 0.10                  |
| CASUAL         | 1.20                 | 0.10                  |
| HIGH           | 2.00                 | 0.10                  |
| VERY_HIGH      | 3.50                 | 0.10                  |
| EXTREME        | 6.00                 | 0.10                  |
| ULTRA_EXTREME  | 10.00                | 0.10                  |

Sub-1x junior multipliers (0.1–0.9) are valid and intentional — a win that returns 0.2× the bet still counts as a hit.

**Senior base multipliers (at minMatch)**
| volatility     | senior base | wild (×1.5) | multiwild (×2.0) |
|----------------|:-----------:|:-----------:|:----------------:|
| LOW            | 2.50        | 3.75        | 5.00             |
| CASUAL         | 5.00        | 7.50        | 10.00            |
| HIGH           | 10.00       | 15.00       | 20.00            |
| VERY_HIGH      | 20.00       | 30.00       | 40.00            |
| EXTREME        | 40.00       | 60.00       | 80.00            |
| ULTRA_EXTREME  | 80.00       | 120.00      | 160.00           |

Scatter is always 2.00 fixed.

**Growth factor per additional matched reel**
| volatility     | growthFactor |
|----------------|:------------:|
| LOW            | 2.5×         |
| CASUAL         | 3.0×         |
| HIGH           | 4.0×         |
| VERY_HIGH      | 6.0×         |
| EXTREME        | 10.0×        |
| ULTRA_EXTREME  | 16.0×        |

Seniors grow faster than juniors: at CASUAL seniors use growth × 1.15; at ULTRA_EXTREME × 1.35.

**Within-tier rank rule:** rank 0 (lowest symbolId) → base × 0.70; rank N-1 → base × 1.30; linear between.

**Invariants**
- `payout(senior, k) > payout(junior, k)` for all k
- `payout(sym, k+1) > payout(sym, k)` for all sym

**Paytable patch guidance**
- Hit rate too low, RTP on target → lower junior multipliers toward 0.10–0.50.
- Hit rate too high, RTP too low → raise junior multipliers back toward 1×–2×.
- RTP too high, senior dominant → reduce senior multipliers.
- RTP too low, want bigger wins → raise senior 4×/5× multipliers.
- Never patch junior multipliers above senior multipliers at any match length.

---

### Skill 05 — Reel Set Weight Seeder
(`skills/ltr/05-reel-set-weight-tuner.md`)

Seeds the initial probability distribution over all reel sets (no-win + per-symbol win sets).

**Seeding formula**
```
noWinShare = clamp(1 - targetRtp/100, 0.03, 0.55)
winShare   = 1 - noWinShare
```

Within win sets: junior symbols → higher initial weight; lower symbolId → higher initial weight.

**Simulation:** `simulate()` runs N spins (default 1,000,000) and returns: rtp, hitRate, maxWin,
stdDev, volatilityIndex, volatilityLabel, hitDistribution.

**Valid weights patch rules**
- Length must match the current `weights` array (noWinCount + winCount).
- All values must be > 0.
- Values are normalised to sum = 1.0 automatically.

**Diagnosis: RTP converges but hit rate does not**  
→ No-win weights too low. Increase no-win entries in the weights array.

**Diagnosis: neither RTP nor hit rate improves**  
→ Try: increase `symsPerReel`. Try: change `targetVolatility`.

---

### Skill 06 — Restriction Builder
(`skills/ltr/06-restriction-builder.md`)

Sets stack sizes and stack probabilities for each reel set's physical reel strip.

**Tunable parameters**
| Parameter       | Range  | Effect |
|-----------------|--------|--------|
| `screenHeight`  | 2–6    | Determines max stack size (= screenHeight + 2). |
| `targetHitRate` | 5–50   | Controls stack bias for both no-win and win sets. |

**Stack bias rules**
- No-win sets: lower HR → smaller stacks → fewer accidental wins.
- Win sets: higher HR → smaller stacks (more frequent hits); lower HR → larger stacks (rarer multi-row wins).

**Do NOT use this skill to fix RTP, tier hit-rate ordering, or convergence failures.**

---

### Skill 07 — Symbol Tier Distribution (Invariants)
(`skills/ltr/07-symbol-tier-distribution.md`)

**Core invariants — ALWAYS check in simulation output**

- **I-1:** `hitRatePct(junior_sym, k) > hitRatePct(senior_sym, k)` for all k
- **I-2:** `hitRatePct(sym, k) > hitRatePct(sym, k+1)` for all sym, all k
- **I-3:** `payout(senior_sym, k) > payout(junior_sym, k)` for all k (guaranteed by paytable generator)
- **I-4:** `payout(sym, k+1) > payout(sym, k)` for all sym (guaranteed by growth factor)
- **I-5:** Neither tier should contribute 0% to total RTP.

**Volatility gap table (payout ratio senior/junior at minMatch)**
| volatility     | approx gap |
|----------------|:----------:|
| LOW            | 2.5×       |
| CASUAL         | ~2.8×      |
| HIGH           | ~3.3×      |
| VERY_HIGH      | 4.0×       |
| EXTREME        | ~4.4×      |
| ULTRA_EXTREME  | ~5.3×      |

The hit-rate gap must mirror the payout gap. If senior pays 4× more, it must hit ~4× less often.

**Quick diagnosis checklist**
| Symptom | Root cause | Fix |
|---------|-----------|-----|
| All symbols identical hit rate | Tier gap too small | Increase `targetVolatility` level |
| Senior hits as often as junior | `seniorFactor` too high | Increase `targetVolatility` |
| Junior pays same as senior | Same tier or duplicate symbolIds | Check symbols list |
| Converged=false | Too few iterations | maxIterations += 40 (cap 200) |
| RTP fine but hit rate off | No-win pool too small | Change `seed`, relax `hitRateDelta` |

---

### Skill 08 — LLM Iterate Loop
(`skills/ltr/08-llm-iterate-loop.md`)

After each simulation run, the iterate prompt asks the LLM to patch the current mutable state
to move closer to the targets.

**Patchable keys (per-iteration JSON patch)**
| Key              | Type             | Cost      | Effect |
|------------------|------------------|-----------|--------|
| `weights`        | array\<double\>  | instant   | Probability of each reel set. Must be same length as current weights array. |
| `paytable`       | object           | instant   | Patch per-symbol multipliers: `{ symbolId: { matchCount: multiplier } }` |
| `winVecDecay`    | double (0.4–0.95)| rebuild   | Win strip tile decay per reel. Lower = steeper → more 3x, fewer 5x wins. |
| `symsPerReel`    | int (64–512)     | rebuild   | Total tiles per reel. Higher → finer weight granularity. |
| `targetVolatility`| string          | rebuild   | Changes paytable scale and win strip decay simultaneously. |
| `maxIterations`  | int              | none      | Use bisection rule to set next iteration's cap. |
| `seed`           | int              | none      | Change to explore a different RNG region. |

"Instant" = no strip rebuild. "Rebuild" = all reel strips + paytable + weights regenerated from scratch.

**Strategy: prefer instant patches first**
1. Weights too low for wins → increase win-set weights, decrease no-win weights.
2. RTP too high → increase no-win weights OR lower paytable multipliers for senior symbols.
3. Hit rate too low → decrease no-win weights.
4. Hit rate too high → increase no-win weights.
5. All wins concentrated in 3x → lower `winVecDecay` (rebuilds) or lower `targetVolatility`.
6. Cannot converge after 3 instant iterations → request structural rebuild via `symsPerReel`.

**Bisection rule for maxIterations (always apply)**
```
gapRatio = |actualRtp - targetRtp| / targetRtp

if gapRatio > 0.20:  maxIterations = min(current × 2, 200)
elif gapRatio > 0.05: maxIterations = min(current + 40, 200)
else:                 maxIterations = min(current + 20, 200)
```

**Rules**
- Never output `targetRtp`, `rtpDelta`, `targetHitRate`, `hitRateDelta`, `symbols`, or `lines`.
- If `maxPayout > 0`: never patch any multiplier above `maxPayout`.
- Weights patch: recalculate the full array so all values > 0 and sum = 1.0.
- Paytable patch: only include symbols you actually want to change.
- Structural patches (rebuild) should be used sparingly — prefer weight/paytable first.

**Simulation output fields available**
```
simulation.rtp               — actual RTP achieved
simulation.hitRate           — actual hit rate achieved
simulation.maxWin            — largest single spin win
simulation.volatilityIndex   — stdDev/mean (dimensionless)
simulation.volatilityLabel   — Low / Casual / High / Very High / Extreme / Ultra Extreme
simulation.hitDistribution   — per symbol, per match length: hits + hitRatePct
```

---

## 4. Runtime Context Injected Per Iteration

These values are computed at runtime and injected into the iterate prompt on every call:

| Placeholder       | Source | Description |
|-------------------|--------|-------------|
| `{stateJson}`     | `LtrReelGenerationTool.buildStateMap(state)` | Current symsPerReel, volatility, winVecDecay, noWinSetCount, winSetCount, weights (rounded), paytable |
| `{simJson}`       | `buildSimJson(stats, symbols)` | Last sim: rtp, hitRate, maxWin, stdDev, volatilityIndex, volatilityLabel, hitDistribution |
| `{violations}`    | `buildViolations(stats, request)` | Invariant breaches (e.g. RTP gap, hit rate gap) |
| `{historySection}`| `context.getLlmHistory()` last 3 entries | LLM responses from last 3 iterations (truncated to 800 chars each) |
| `{actualRtp}`     | `LlmService.extractRtp(simJson)` | RTP from the last sim |
| `{gapRatio}`      | `|actualRtp - targetRtp| / targetRtp` | Relative gap used for diagnosis |
| `{nextIterHint}`  | bisection formula applied in `LlmService.iterate()` | Suggested next maxIterations value |
| `{nextSeed}`      | `iteration × 137` | Suggested next seed |
