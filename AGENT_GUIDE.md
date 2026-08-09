# Agent Architecture — Guide

Here's a mental map of the agent — best read bottom-up (foundation first).

---

## Layer 1 — Data (start here)

**`agent/model/`** — records/POJOs, no logic:
- `AgentRequest` — what the UI sends (`targetRtp`, `rtpDelta`, `targetHitRate`, `hitRateDelta`, `targetVolatility`, `reelConfig`, `parameters`)
- `AgentContext` — mutable bag flowing through every node (request, selected skill, generated reels, human feedback, execution trace, attempt counter, `retryHints` map, `variables` map, `cancelled` flag)
- `GeneratedReels` — wraps the raw JSON result string + the `ReelSetsCollectionData` config used

---

## Layer 2 — Graph Engine (`agent/graph/`)

The execution backbone. Read in this order:

1. `AgentNode` — one interface: `String getId()` + `NodeResult execute(AgentContext)`
2. `NodeResult` — success/failure + outcome string + metadata map
3. `AgentEdge` — from/to node IDs + a `TransitionCondition` lambda
4. `AgentGraph` — map of nodes + list of edges + a start node
5. `GraphExecutor` — walks the graph: execute node → pick **first** matching edge → repeat until no edge matches (terminal)

**Key insight:** the graph has no hardcoded flow. The flow is entirely determined by which edges are wired in the orchestrator. Edges are evaluated in insertion order; the first matching condition wins.

---

## Layer 3 — Skills (`agent/skills/`)

### Skill definitions (Markdown files)

Skills live in `skills/<strategy>/` subdirectories. Each `.md` file describes one step of the generation algorithm for that strategy:

```
skills/
  ltr/
    01-symbol-count-initialiser.md
    02-spiral-no-win-factory.md
    03-win-reel-set-factory.md
    04-paytable-generator.md
    05-reel-set-weight-tuner.md
    06-restriction-builder.md
    07-symbol-tier-distribution.md
```

Adding skills for a new strategy requires only dropping `.md` files under `skills/<strategy>/` — no Java changes to the registry. `LlmService` concatenates **all** skill files for the strategy and injects them into every LLM prompt.

### Java classes

| Class | Role |
|---|---|
| `Skill` | Record: id, name, description, path, instructions text |
| `SkillRegistry` / `FileSystemSkillRegistry` | Walks `skills/` on startup; loads every `.md`; derives IDs as `strategy/filename`; exposes `getSkillsForStrategy(strategy)` |
| `SkillSelector` / `DefaultSkillSelector` | Validates strategy is in `SUPPORTED_STRATEGIES`; returns the first skill as representative |
| `AgentProperties` | `@ConfigurationProperties` for `agent.max-attempts`, `agent.skills-path`, `agent.human-in-the-loop.enabled` |

---

## Layer 4 — Generation Tools (`agent/tools/`)

| Interface | Implementation | Strategy |
|---|---|---|
| `AgentReelGenerationTool` | `LtrReelGenerationTool` | LTR |

`LtrReelGenerationTool.generate(request, context)` runs the full 6-step LTR pipeline (see [LTR Generation Pipeline](#ltr-generation-pipeline) below). It reads all parameters from `request.parameters()` (which `GenerateReelsNode` populates by merging `retryHints` onto the original request parameters).

---

## Layer 5 — Supporting Services

- **`validation/BasicReelValidator`** — validates generated reels; emits violations (see [Validator](#validator-basicreelvalidator))
- **`llm/LlmService`** — wraps OpenAI `gpt-4o-mini`; loads API key from `.env` fallback; provides `plan()` and `diagnose()` — see [LLM prompts](#llm-prompts) below
- **`human/DefaultHumanApprovalService`** — stub storing pending approvals in `ConcurrentHashMap`; REST endpoint injects approvals
- **`execution/`** — `AgentExecution` (record of a full run), `ExecutionTrace` (list of `NodeExecution`), `InMemoryExecutionRepository` (thread-safe map)

---

## Layer 6 — Concrete Nodes (`agent/orchestrator/nodes/`)

| Node ID | Class | What it does |
|---|---|---|
| `analyze-request` | `AnalyzeRequestNode` | Validates request; resolves `reelConfig` from parameters; stores `strategy` in context variables |
| `llm-plan` | `LlmPlanNode` | Calls `LlmService.plan()`; parses returned JSON; writes all keys as `retryHints` for attempt 1. Gracefully skips (no-op) when LLM unavailable |
| `generate-reels` | `GenerateReelsNode` | Increments attempt counter; finds matching `AgentReelGenerationTool`; merges `retryHints` into a new request; calls `tool.generate()` |
| `validate-reels` | `ValidateReelsNode` | Runs `BasicReelValidator`; returns `"VALID"` or failure with violation list |
| `llm-diagnose` | `LlmDiagnoseNode` | Reads violations from context; calls `LlmService.diagnose()`; merges returned JSON keys as updated `retryHints`. Gracefully skips when LLM unavailable |
| `finalize` | `FinalizeNode` | Reads `context.getReels().rawJson()`; returns it as the execution result |
| `human-approval` | `HumanApprovalNode` | Polls `HumanApprovalService`; returns `APPROVED` / `REJECTED` / `PENDING` |

---

## Layer 7 — Orchestrator (`DefaultAgentOrchestrator`)

The only class that knows the full flow. `buildGraph()` branches on the `useLlm` boolean from request parameters (default: `true`).

---

### How skill docs reach the LLM

```
  skills/ltr/                          FileSystemSkillRegistry
  ├── 01-symbol-count-initialiser.md   loads all .md files at startup
  ├── 02-spiral-no-win-factory.md      into Skill records
  ├── 03-win-reel-set-factory.md       (id, name, instructions text)
  ├── 04-paytable-generator.md    ──►
  ├── 05-reel-set-weight-tuner.md      LlmService.loadSkillDocs("ltr")
  ├── 06-restriction-builder.md        concatenates all 7 files
  └── 07-symbol-tier-distribution.md  with "---" separators
                                            │
                                            ▼
                               injected into EVERY LLM prompt
                               as the "## Skill documentation" section
```

Adding a new skill = drop a `.md` file in `skills/ltr/` — no Java changes needed.

---

### Graph: LLM mode (`useLlm=true`, default)

```
  [START]  POST /api/agent/generate  ─────────────────────────────────────────────
     │     AgentRequest: targetRtp=95.5, rtpDelta=0.5, targetHitRate=20,
     │                   targetVolatility=HIGH, parameters:{symbols,lines,…}
     ▼
  ╔══════════════════════════════════════════════════════════════════════════╗
  ║  analyze-request                                                         ║
  ║  ─────────────────────────────────────────────────────────────────────   ║
  ║  • Validates request is non-null                                         ║
  ║  • Resolves reelConfig from parameters if not supplied                   ║
  ║  • Stores strategy ("LTR") → context.variables["strategy"]               ║
  ╚══════════════════════════════════════════════════════════════════════════╝
     │  success
     ▼
  ╔══════════════════════════════════════════════════════════════════════════╗
  ║  llm-plan                                         ◄── LLM CALL #1        ║
  ║  ─────────────────────────────────────────────────────────────────────   ║
  ║  Java pre-computes:                                                      ║
  ║    winSymCount = count junior+senior in request.parameters.symbols       ║
  ║    suggestedIter = clamp(winSymCount × 20, 80, 200)                      ║
  ║                                                                          ║
  ║  Prompt sent to gpt-4o-mini (temperature=0.0, json_object mode):         ║
  ║  ┌────────────────────────────────────────────────────────────────┐      ║
  ║  │ ## Target objectives                                           │      ║
  ║  │    targetRtp=95.5% rtpDelta=±0.5% hitRate=20% …                │      ║
  ║  │ ## User parameters (pretty-printed JSON)                       │      ║
  ║  │    { "symbols":[{id:1,tier:"junior"},…], "lines":[[…],…] }     │      ║
  ║  │ ## Bisection rule  ← computed in Java, injected as literal     │      ║
  ║  │    winSymCount=8 → suggestedMaxIterations=160                  │      ║
  ║  │    "Use this value. Do NOT use a lower value."                 │      ║
  ║  │ ## Skill documentation  ← all 7 .md files concatenated         │      ║
  ║  │    ### Skill 01 — Symbol Count Initialiser … (full text)       │      ║
  ║  │    --- … Skill 05 bisection strategy, RTP math … ---           │      ║
  ║  │    … Skill 07 invariants, diagnosis checklist …                │      ║
  ║  │ ## Task                                                        │      ║
  ║  │    "Output ONLY a JSON object with ALL of these keys: …"       │      ║
  ║  │    "Copy symbols and lines exactly from User parameters"       │      ║
  ║  └────────────────────────────────────────────────────────────────┘      ║
  ║                                                                          ║
  ║  LLM response → JSON parsed → all keys written to context.retryHints     ║
  ║  Example retryHints after plan:                                          ║
  ║    { strategy:"LTR", screenWidth:5, screenHeight:3, minMatch:3,          ║
  ║      symsPerReel:256, maxIterations:160, targetVolatility:"HIGH",        ║
  ║      symbols:[…copied from user…], lines:[…copied from user…] }          ║
  ║                                                                          ║
  ║  FALLBACK: LLM unavailable → no-op, user params used unchanged           ║
  ╚══════════════════════════════════════════════════════════════════════════╝
     │  unconditional
     ▼
  ╔══════════════════════════════════════════════════════════════════════════╗  ◄──────────┐
  ║  generate-reels                                                          ║             │
  ║  ─────────────────────────────────────────────────────────────────────   ║             │
  ║  • Increments context.attempts                                           ║             │
  ║  • effectiveRequest = merge(request.params, context.retryHints)          ║             │
  ║  • Dispatches to LtrReelGenerationTool.generate(effectiveRequest)        ║             │
  ║                                                                          ║             │
  ║  LTR pipeline (6 sequential steps):                                      ║             │
  ║  1. SymbolCountInitialiser                                               ║             │
  ║     symsPerReel + targetVolatility → int[] baseCounts (tiles/symbol)     ║             │
  ║  2. SpiralNoWinReelSetFactory                                            ║             │
  ║     spiral zero-placement → List<int[][]> noWinSets (screenWidth sets)   ║             │
  ║  3. WinReelSetFactory                                                    ║             │
  ║     one win reel set per non-special symbol → List<int[][]> winSets      ║             │
  ║  4. PaytableGenerator                                                    ║             │
  ║     targetVolatility → Map<symId, Map<matchLen, multiplier>>             ║             │
  ║  5. ReelSetWeightTuner  ← inner feedback loop                            ║             │
  ║     runs maxIterations rounds, 500k spins each:                          ║             │
  ║       actualRtp < target → increase win-set weights ×1.05–1.30           ║             │
  ║       actualRtp > target → decrease win-set weights                      ║             │
  ║       actualHitRate off  → scale no-win weight ×/÷1.05                   ║             │
  ║     → WeightedReelSets (weights[], converged, SimStats)                  ║             │
  ║  6. RestrictionBuilder                                                   ║             │
  ║     stack sizes/chances for ShuffleGenerator per reel set                ║             │
  ║                                                                          ║             │
  ║  context.reels = GeneratedReels(config, rawJson)                         ║             │
  ╚══════════════════════════════════════════════════════════════════════════╝             │
     │  success                                                                            │
     ▼                                                                                     │
  ╔══════════════════════════════════════════════════════════════════════════╗             │
  ║  validate-reels                                                          ║             │
  ║  ─────────────────────────────────────────────────────────────────────   ║             │
  ║  BasicReelValidator checks context.reels.rawJson in order:               ║             │
  ║                                                                          ║             │
  ║  1. Structural  reels/rawJson/reelSets non-null, non-empty               ║             │
  ║  2. Convergence converged==true                                          ║             │
  ║                → if false: retryHint(maxIterations += 40, cap 200)       ║             │
  ║  3. RTP gap     |actualRtp − targetRtp| ≤ rtpDelta                       ║             │
  ║  4. HitRate gap |actualHitRate − targetHitRate| ≤ hitRateDelta           ║             │
  ║  5. I-1         avg junior 3x hitRate > avg senior 3x hitRate            ║             │
  ║  6. I-2         hitRate(sym,k) > hitRate(sym,k+1) for all sym, k         ║             │
  ║                                                                          ║             │
  ║  All violations → context.variables["llm_violations"]                    ║             │
  ║  Seed varied:   retryHint("seed", attempt × 137)                         ║             │
  ╚══════════════════════════════════════════════════════════════════════════╝             │
     │                                       │                                             │
     │  outcome=="VALID"                     │  failure                                    │
     │  (all 6 checks pass)                  │  AND attempts < maxAttempts                 │
     ▼                                       ▼                                             │
  ╔══════════════════╗       ╔══════════════════════════════════════════════════════════╗  │
  ║  finalize        ║       ║  llm-diagnose                      ◄── LLM CALL #2+      ║  │
  ║  ──────────────  ║       ║  ────────────────────────────────────────────────────    ║  │
  ║  Returns         ║       ║  Java pre-computes bisection decision:                   ║  │
  ║  rawJson as      ║       ║    actualRtp  = parse simulation.rtp from rawJson        ║  │
  ║  execution       ║       ║    gapRatio   = |actualRtp − targetRtp| / targetRtp      ║  │
  ║  result          ║       ║    nextIter   = gapRatio>0.20 → currentIter×2            ║  │
  ║                  ║       ║                 gapRatio>0.05 → currentIter+40           ║  │
  ║  status=         ║       ║                 else          → currentIter+20           ║  │
  ║  COMPLETED       ║       ║    nextSeed   = attempt × 137                            ║  │
  ╚══════════════════╝       ║                                                          ║  │
                             ║  Prompt sent to gpt-4o-mini:                             ║  │
                             ║  ┌──────────────────────────────────────────────────┐    ║  │
                             ║  │ ## Original targets  (fixed, never change)       │    ║  │
                             ║  │ ## Current parameters (JSON, last attempt)       │    ║  │
                             ║  │ ## Violations detected  ← from validator         │    ║  │
                             ║  │    "RTP gap: actualRtp=71.50% vs target=95.50%"  │    ║  │
                             ║  │    "Weight tuner did not converge"               │    ║  │
                             ║  │ ## Simulation output (truncated to 3000 chars)   │    ║  │
                             ║  │    {converged:false, rtp:71.5, hitRate:18.2,…}   │    ║  │
                             ║  │ ## Skill documentation  ← all 7 .md files        │    ║  │
                             ║  │ ## Bisection decision  ← injected as literals    │    ║  │
                             ║  │    actualRtp: 71.50%  targetRtp: 95.50%          │    ║  │
                             ║  │    gap ratio: 25.1%  → LARGE — double maxIter    │    ║  │
                             ║  │    Prescribed maxIterations: 200                 │    ║  │
                             ║  │    Prescribed seed: 274                          │    ║  │
                             ║  │    "Always use prescribed values"                │    ║  │
                             ║  │ ## Task                                          │    ║  │
                             ║  │    "Output ONLY JSON with keys that need change" │    ║  │
                             ║  └──────────────────────────────────────────────────┘    ║  │
                             ║                                                          ║  │
                             ║  LLM response → JSON parsed → merged into retryHints     ║  │
                             ║  Example: { "maxIterations": 200, "seed": 274 }          ║  │
                             ║                                                          ║  │
                             ║  FALLBACK: LLM unavailable → keep existing hints         ║  │
                             ╚══════════════════════════════════════════════════════════╝  │
                                          │  unconditional                                 │
                                          └────────────────────────────────────────────────┘
                                                   loop back to generate-reels

  ═══ TERMINAL CONDITIONS ══════════════════════════════════════════════════════════
   validate-reels VALID                        → finalize → COMPLETED
   attempts ≥ maxAttempts AND validate fails   → no edge matches → FAILED
   generate-reels throws exception             → NodeResult.failure → FAILED
   context.isCancelled() at any node           → CancellationException → CANCELLED
  ══════════════════════════════════════════════════════════════════════════════════

  LLM call summary:
  ┌──────────┬──────────────┬──────────────────────────────────────┬────────────────────────────┐
  │ Call     │ Node         │ Skill docs used                      │ Output                     │
  ├──────────┼──────────────┼──────────────────────────────────────┼────────────────────────────┤
  │ #1 Plan  │ llm-plan     │ All 7 ltr skills (full text)         │ Full param set→ retryHints │
  │ #2+ Diag │ llm-diagnose │ All 7 ltr skills (full text)         │ Delta params → retryHints  │
  └──────────┴──────────────┴──────────────────────────────────────┴────────────────────────────┘
```

**Key design principle:** arithmetic decisions (bisection thresholds, `nextIter`, `nextSeed`) are computed in Java and injected as concrete numbers into the prompt. The LLM reads, confirms, and applies them — it does not derive them. This makes the diagnose loop deterministic and prevents the LLM from guessing values that violate the bisection rule.

---

### Graph: Classic mode (`useLlm=false`)

```
  [START]
     │
     ▼
  ╔══════════════════════════════════════════════╗
  ║  analyze-request                             ║
  ║  Validates request, stores strategy          ║
  ╚══════════════════════════════════════════════╝
     │  success
     ▼
  ╔══════════════════════════════════════════════╗  ◄──────────────────────────────────────┐
  ║  generate-reels                              ║                                         │
  ║  Attempt 1: user params used directly        ║                                         │
  ║  Retries:   retryHints from validator        ║                                         │
  ║             (maxIterations +40, seed ×137)   ║                                         │
  ║  No LLM. No skill docs injected.             ║                                         │
  ╚══════════════════════════════════════════════╝                                         │
     │  success                                                                            │
     ▼                                                                                     │
  ╔══════════════════════════════════════════════╗                                         │
  ║  validate-reels                               ║                                        │
  ║  Same 6 checks as LLM mode.                  ║                                         │
  ║  Validator writes retryHints directly.       ║                                         │
  ╚══════════════════════════════════════════════╝                                         │
     │                         │                                                           │
     │  VALID                  │  INVALID + attempts < maxAttempts                         │
     ▼                         └───────────────────────────────────────────────────────────┘
  ╔══════════════════════════════════════════════╗
  ║  finalize  →  COMPLETED                      ║
  ╚══════════════════════════════════════════════╝
```

Classic defaults: `maxAttempts=3`, `maxIterations=120`. No LLM calls; no skill docs injected. Retry strategy is mechanical only (fixed `maxIterations` escalation + seed change from validator).

---

## Validator: `BasicReelValidator`

Checks in order. Each violation is added to the list and stored in `context.variables["llm_violations"]` for `LlmDiagnoseNode` to read.

| # | Check | Condition | Violation text | Side effect |
|---|---|---|---|---|
| 1 | Structural | `reels == null` or `rawJson` blank or `reelSets` empty | Structural error message | — |
| 2 | Convergence | `converged == false` | `"Weight tuner did not converge"` | `retryHint(maxIterations, current+40, cap 200)` |
| 3 | RTP gap | `|actualRtp − targetRtp| > rtpDelta` | `"RTP gap: actualRtp=X% vs targetRtp=Y% (gap=Z%, delta=±D%)"` | — |
| 4 | HitRate gap | `|actualHitRate − targetHitRate| > hitRateDelta` | `"HitRate gap: actualHitRate=X% vs targetHitRate=Y% …"` | — |
| 5 | I-1 tier ordering | avg junior 3x hitRate ≤ avg senior 3x hitRate | `"I-1 violated: avg junior 3x hitRate (X%) <= avg senior 3x hitRate (Y%)"` | — |
| 6 | I-2 length ordering | `hitRate(sym, k+1) ≥ hitRate(sym, k)` for any k | `"I-2 violated for SYM: Kx hitRate (X%) <= (K+1)x hitRate (Y%)"` | — |

All violations → `context.variables["llm_violations"]`. Seed always varied: `retryHint("seed", attempt × 137)`.

---

## Why RTP=71.5 Instead of 95.5 — Root Cause Analysis

The weight tuner is a gradient-following feedback loop. Here is what can prevent it from reaching 95.5%:

### Root cause 1 — `noWinShare` starting point too high *(fixed)*

```
noWinShare = clamp(1 - targetRtp/100, 0.03, 0.55)
```
For targetRtp=95.5: `1 - 0.955 = 0.045`. Old clamp floor was 0.15 — tuner started with 3× too much no-win weight. Fixed to 0.03.

**If you still see RTP ≈ 43–75%**: the tuner converged to the best it could find in `maxIterations` rounds, starting too far from target. Solution: increase `maxIterations` (120+).

### Root cause 2 — Too few iterations for symbol count

Each additional pair of win symbols adds one more reel set. Each win reel set starts with a fractional share of the win budget. The more symbols, the smaller the initial per-set weight, the further from equilibrium the tuner starts, and the more iterations needed to shift weights.

**Rule of thumb:** for N non-special symbols at 95%+ RTP → `maxIterations ≥ N × 15`.
Example: 8 symbols → ≥ 120 iterations.

### Root cause 3 — LLM generates wrong `symbols` / `lines`

If the user's symbols/lines were passed as a flat Java `{key=value}` string (old code), the LLM could not parse them and generated its own symbolIds. Wrong symbolIds → paytable lookup misses → near-zero payout per win spin → tuner can never push RTP above ~50%.

**Fixed**: params now passed as pretty-printed JSON.

### Root cause 4 — LLM cannot see the RTP gap

Old violations list only contained invariant checks (I-1, I-2). The LLM's diagnose prompt showed `"violations: none"` even when actualRtp=71.5 vs targetRtp=95.5.

**Fixed**: validator now emits `"RTP gap: actualRtp=71.50% vs targetRtp=95.50% (gap=-24.00%, delta=±0.50%)"` — the LLM sees the gap and can prescribe `maxIterations` correction.

### Root cause 5 — `maxIterations` escalation cap too low

Old cap was 300 with +60 steps. Revised to cap=200 with +40 steps. The escalation is intended to push the tuner past a convergence plateau — but the plateau is best overcome by starting closer to target (root cause 1) rather than running more iterations from a bad start.

### Quick diagnosis table

| Symptom | Most likely cause | Fix |
|---|---|---|
| `converged=false`, RTP ≈ 43–75% | noWinShare floor was too high (old code) | Ensure code has `clamp(1-rtp/100, 0.03, 0.55)` |
| `converged=false`, RTP ≈ 80–93% | Not enough iterations | Increase `maxIterations` to 120–200 |
| `converged=true`, RTP correct but wrong symbols in hitDist | LLM generated its own symbolIds | Check that `symbols` in retryHints match user input |
| I-1 violated | Senior tile counts too close to junior | Increase `targetVolatility` level |
| I-2 violated | Degenerate reel (zero-count symbol block) | Increase `symsPerReel`, change `seed` |

---

## LTR Generation Pipeline

`LtrReelGenerationTool.generate()` runs 6 steps in sequence. Each step has a corresponding Markdown spec.

### Step 1 — `SymbolCountInitialiser`
**Spec:** `skills/ltr/01-symbol-count-initialiser.md`

Computes a base tile count per symbol. Total tiles per reel ≈ `symsPerReel`. Senior symbols receive fewer tiles; the gap scales with `targetVolatility`. Within each tier, lower `symbolId` → more tiles. Wilds/scatters get fixed low counts (wild=0.20, scatter=0.15, multiwild=0.12 of symsPerReel). Every symbol is clamped to a minimum of 1.

**Output:** `int[] baseCounts` — one count per symbol (index = position in symbols list).

---

### Step 2 — `SpiralNoWinReelSetFactory`
**Spec:** `skills/ltr/02-spiral-no-win-factory.md`

Builds `screenWidth` no-win reel sets using the spiral zero-placement method:
- Zeroes are inserted at intervals of `minMatch − 1` columns apart, preventing any LTR run of length ≥ `minMatch`
- Each reel set has a different phase (starting column for the first zero), offset by 1 per set

**Output:** `List<int[][]>` — `screenWidth` reel sets, each `int[screenWidth][numSymbols]`.

---

### Step 3 — `WinReelSetFactory`
**Spec:** `skills/ltr/03-win-reel-set-factory.md`

For each non-special symbol (junior + senior), produces one dedicated winning reel set:
- A win vector `W[0..screenWidth-1]` decays reel by reel with a volatility-dependent factor (LOW: 0.94 → ULTRA_EXTREME: 0.45)
- The vector is injected into the symbol's column in a copy of a no-win reel set
- Zero positions from other symbols are back-filled with a small residual count (default 8)

**Output:** `List<int[][]>` — one winning reel set per non-special symbol.

---

### Step 4 — `PaytableGenerator`
**Spec:** `skills/ltr/04-paytable-generator.md`

Produces multipliers for every (symbol, matchLength) pair:
- Base multiplier at `minMatch` depends on tier and volatility (junior LOW=1.00 → ULTRA_EXTREME=15.00; senior LOW=2.50 → ULTRA_EXTREME=80.00)
- Each additional matched reel multiplies by growthFactor (LOW: 2.5× → ULTRA_EXTREME: 16×)
- Within each tier: lower `symbolId` (rank 0) → 0.70× base; higher `symbolId` (rank N-1) → 1.30× base
- All values rounded to nearest 0.10; minimum 0.10

**Output:** `Map<Integer, Map<Integer, Double>>` — symbolId → matchCount → multiplier.

---

### Step 5 — `ReelSetWeightTuner`
**Spec:** `skills/ltr/05-reel-set-weight-tuner.md`

Iteratively adjusts probability weights across all reel sets:

**Initial seeding:**
```
noWinShare = clamp(1 - targetRtp/100, 0.03, 0.55)
winShare   = 1 - noWinShare
```
Within win sets: junior > senior initial weight; within each tier lower symbolId → higher initial weight.

**Feedback loop (each of `maxIterations` rounds = 500,000 simulated spins):**

| Condition | Action | Factor |
|---|---|---|
| `actualRtp < targetRtp − rtpDelta` && gap > 20pp | Increase win-set weights | × 1.30 |
| `actualRtp < targetRtp − rtpDelta` && gap ≤ 20pp | Increase win-set weights | × 1.05 |
| `actualRtp > targetRtp + rtpDelta` | Decrease win-set weights | ÷ 1.05 or ÷ 1.30 |
| `actualHitRate < targetHitRate − hitRateDelta` | Reduce no-win weight share | × (1/1.05) |
| `actualHitRate > targetHitRate + hitRateDelta` | Increase no-win weight share | × 1.05 |

After each adjustment, weights are re-normalised to sum to 1.0.

**RTP math note:** `RTP ≈ Σ(weight[i] × avg_payout[i])`. The tuner raises `weight[win_set_i]` when RTP is below target and lowers it when above. With many win sets (many symbols), each set starts with a smaller share and the feedback loop needs more rounds to reach equilibrium.

**Output:** `WeightedReelSets` — reel sets + weights + `converged` flag + `SimStats` (rtp, hitRate, maxWin, stdDev, volIdx, hitDistribution).

---

### Step 6 — `RestrictionBuilder`
**Spec:** `skills/ltr/06-restriction-builder.md`

Assigns a `Restriction` (stack sizes + stack chances + minDistance) to each reel set, used by `ShuffleGenerator` when building the physical reel strips:
- No-win sets: stack mass biased left (lower hit rate from shuffled position)
- Win sets: stack mass biased right (higher hit rate from shuffled position)
- `minDistance` = `screenHeight` (prevents consecutive same-symbol stacks)

**Output:** one `Restriction` per reel set.

---

### Final Output Structure

```json
{
  "converged": true,
  "simulation": {
    "rtp": 95.48,
    "hitRate": 19.83,
    "maxWin": 150.0,
    "stdDev": 2.14,
    "volatilityIndex": 7.3,
    "volatilityLabel": "High",
    "spins": 500000,
    "hitDistribution": {
      "cherry (id=1)": { "3x": { "hits": 48200, "hitRatePct": 9.64 }, "4x": {...}, "5x": {...} },
      "seven  (id=5)": { "3x": { "hits":  3100, "hitRatePct": 0.62 }, ... }
    }
  },
  "paytable": { "1": { "3": 0.10, "4": 0.40, "5": 1.40 }, "5": { "3": 5.0, ... } },
  "weights": [0.0412, 0.0721, ...],
  "reelSets": [
    { "name": "0: no-win", "tilesCounts": [[...]], "restriction": {...}, "reelStrips": [...] },
    { "name": "5: cherry (junior)", "tilesCounts": [[...]], "restriction": {...}, "reelStrips": [...] }
  ]
}
```

---

## Adding a New Strategy

1. Create `skills/<strategy>/` and add numbered `.md` spec files
2. Implement `AgentReelGenerationTool` with `supports("<STRATEGY>") = true`
3. Add `"<STRATEGY>"` to `SUPPORTED_STRATEGIES` in `DefaultSkillSelector`

No changes needed to the graph, orchestrator, registry, or API layer.

---

## Best Study Path

```
AgentRequest → AgentContext → NodeResult → AgentNode
→ GraphExecutor (understand the walk)
→ DefaultAgentOrchestrator.buildGraph() (understand the dual-path wiring)
→ LlmPlanNode + LlmDiagnoseNode (the LLM integration points)
→ BasicReelValidator (what violations trigger a retry)
→ LtrReelGenerationTool (the full LTR pipeline)
→ ReelSetWeightTuner (the convergence loop)
→ AgentController (the entry point)
```
