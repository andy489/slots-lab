# Agent Architecture — Guide

Here's a mental map of the agent — best read bottom-up (foundation first).

---

## Layer 1 — Data (start here)

**`agent/model/`** — records/POJOs, no logic:
- `AgentRequest` — what the UI sends (`targetRtp`, `rtpDelta`, `targetHitRate`, `hitRateDelta`, `targetVolatility`, `reelConfig`, `parameters`)
- `AgentContext` — mutable bag flowing through every node (request, selected skill, generated reels, human feedback, execution trace, attempt counter, variables map)
- `GeneratedReels` — wraps the raw JSON result string + the `ReelSetsCollectionData` config used

---

## Layer 2 — Graph Engine (`agent/graph/`)

The execution backbone. Read in this order:

1. `AgentNode` — one interface: `String getId()` + `NodeResult execute(AgentContext)`
2. `NodeResult` — success/failure + outcome string + metadata map
3. `AgentEdge` — from/to node IDs + a `TransitionCondition` lambda
4. `AgentGraph` — map of nodes + list of edges + a start node
5. `GraphExecutor` — walks the graph: execute node → pick first matching edge → repeat until no edge matches

**Key insight:** the graph has no hardcoded flow. The flow is entirely determined by which edges are wired in the orchestrator.

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
```

Adding skills for a new strategy requires only dropping `.md` files under `skills/<strategy>/` — no Java changes to the registry.

### Java classes

| Class | Role |
|---|---|
| `Skill` | Record: id, name, description, path, instructions text |
| `SkillRegistry` / `FileSystemSkillRegistry` | Walks `skills/` on startup; loads every `.md`; derives IDs as `strategy/filename` (e.g. `ltr/01-symbol-count-initialiser`); exposes `getSkillsForStrategy(strategy)` |
| `SkillSelector` / `DefaultSkillSelector` | Receives the strategy-filtered skill list from `SkillSelectionNode`; validates the strategy is in `SUPPORTED_STRATEGIES`; returns the first (lowest-numbered) skill as representative |
| `AgentProperties` | `@ConfigurationProperties` for `agent.max-attempts`, `agent.skills-path`, `agent.human-in-the-loop.enabled` |

**Key insight:** `SUPPORTED_STRATEGIES` in `DefaultSkillSelector` is the single gate. Requesting an unsupported strategy throws `UnsupportedOperationException` → HTTP 422 with `{"error": "..."}`. Currently only `LTR` is supported.

---

## Layer 4 — Generation Tools (`agent/tools/`)

Each payout strategy has a dedicated generation tool:

| Interface | Implementation | Strategy |
|---|---|---|
| `AgentReelGenerationTool` | `LtrReelGenerationTool` | LTR |

`AgentReelGenerationTool` has two methods:
- `boolean supports(String strategy)` — used by `GenerateReelsNode` to dispatch
- `GeneratedReels generate(AgentRequest request)` — runs the full pipeline

`LtrReelGenerationTool` orchestrates 5 algorithm steps in sequence (see [LTR Pipeline](#ltr-generation-pipeline) below).

---

## Layer 5 — Supporting Services

- **`validation/BasicReelValidator`** — checks generated reels are non-null and non-empty
- **`human/DefaultHumanApprovalService`** — stub storing pending approvals in a `ConcurrentHashMap`; REST endpoint injects approvals
- **`execution/`** — `AgentExecution` (record of a full run), `ExecutionTrace` (list of `NodeExecution`), `InMemoryExecutionRepository` (thread-safe map)

---

## Layer 6 — Concrete Nodes (`agent/orchestrator/nodes/`)

| Node | What it does |
|---|---|
| `AnalyzeRequestNode` | Parses request; resolves `reelConfig` from parameters if not provided; stores `strategy` in context variables |
| `SkillSelectionNode` | Calls `registry.getSkillsForStrategy(strategy)`; fails with a clear message if no skills exist; passes filtered list to selector |
| `LoadSkillNode` | Confirms the selected skill has non-empty instructions |
| `GenerateReelsNode` | Finds the `AgentReelGenerationTool` that `supports(strategy)`; fails with a clear message if none found; calls `tool.generate(request)`; increments attempt counter |
| `ValidateReelsNode` | Runs `BasicReelValidator`, returns `"VALID"` or failure |
| `HumanApprovalNode` | Polls `HumanApprovalService`; returns APPROVED / REJECTED / PENDING |
| `FinalizeNode` | Reads reels from context, returns raw JSON as outcome |

---

## Layer 7 — Orchestrator (`DefaultAgentOrchestrator`)

The only class that knows the full flow:

1. Creates `UUID` + `ExecutionTrace` + `AgentContext`
2. Calls `buildGraph()` — wires all nodes and edges with lambda conditions
3. Hands graph + context to `GraphExecutor.execute()`
4. Reads context after execution; sets `AgentExecution` status; persists it

**Graph wiring:**
```
analyze-request
    │ success
    ▼
select-skill  ──── no skills for strategy ──► [FAILED]
    │ success
    ▼
load-skill
    │
    ▼
generate-reels ◄──────────────────────────────────────────────┐
    │ success                                                  │
    ▼                                                          │
validate-reels                                                 │
    │ VALID                          │ INVALID + attempts < max│
    ▼                                └─────────────────────────┘
human-approval ◄── (if enabled)
    │ APPROVED                       │ REJECTED + attempts < max
    ▼                                └─────────────────────────┘
finalize
```

**Stopping conditions:**
- The inner RTP/hit-rate tuning loop stops when both tolerances are met, or after `DEFAULT_ITER` (50) iterations
- The outer retry loop stops when validation passes and human approves, or after `agent.max-attempts` (default 3) total generation attempts
- Unsupported strategy → immediate failure, no retries

---

## Layer 8 — REST API (`agent/api/AgentController`)

Three endpoints:

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/agent/generate` | Maps `AgentRunRequest` → `AgentRequest`, runs orchestrator, returns execution with trace |
| `GET` | `/api/agent/executions/{id}` | Fetches execution record from repository |
| `POST` | `/api/agent/executions/{id}/approval` | Injects human approval decision |

### Why `executionId` exists

Every run is assigned a `UUID` on receipt of `POST /api/agent/generate`. It serves three purposes:

1. **Async polling** — the endpoint returns immediately with `executionId`; client polls `GET /api/agent/executions/{id}` for status/result
2. **Human-in-the-loop** — when `HumanApprovalNode` pauses execution, the human submits their decision to `POST /api/agent/executions/{id}/approval`; the ID routes the approval to the correct waiting execution
3. **Observability** — every log line and every `NodeExecution` in `ExecutionTrace` is linked to the `executionId`

---

## LTR Generation Pipeline

`LtrReelGenerationTool` runs 5 Java skill classes in sequence. Each has a corresponding Markdown spec in `skills/ltr/`:

### Step 1 — `SymbolCountInitialiser`
**Spec:** `skills/ltr/01-symbol-count-initialiser.md`

Computes a base count per symbol from `symsPerReel` and volatility. Senior symbols receive
fewer tiles; the gap scales with volatility (LOW → ULTRA_EXTREME). Wilds/scatters get fixed
low counts regardless of volatility. Every symbol is clamped to a minimum of 1.

**Output:** `int[] baseCounts` — one count per symbol.

---

### Step 2 — `SpiralNoWinReelSetFactory`
**Spec:** `skills/ltr/02-spiral-no-win-factory.md`

Builds `screenWidth` no-win reel sets using the **spiral zero-placement method**:
- Zeroes are inserted into each reel at intervals of `minMatch − 1` columns apart
- Each reel set has a different **phase** (starting column for the first zero), offset by 1 per set
- This guarantees that no LTR run of length ≥ `minMatch` can exist across all reels in any set

**Output:** `List<int[][]>` — `screenWidth` reel sets, each `int[screenWidth][numSymbols]`.

---

### Step 3 — `WinReelSetFactory`
**Spec:** `skills/ltr/03-win-reel-set-factory.md`

For each non-special symbol (junior + senior), produces one dedicated winning reel set:
- A **win vector** `W[0..screenWidth-1]` is generated with `W[0]` = win peak, decaying each
  reel by a volatility-dependent factor (LOW: 0.94 → ULTRA_EXTREME: 0.45)
- The vector is injected into the symbol's column in a copy of a no-win reel set
- Zero positions left by other symbols are back-filled with a small residual count (default 8)
  to prevent dead reels

**Output:** `List<int[][]>` — one winning reel set per non-special symbol.

---

### Step 4 — `PaytableGenerator`
**Spec:** `skills/ltr/04-paytable-generator.md`

Produces multipliers for every symbol at every match length from `minMatch` to `screenWidth`:
- Base multiplier at `minMatch` hits depends on tier and volatility
- Each additional matched reel multiplies by a growth factor (LOW: 2.0× → ULTRA_EXTREME: 20.0×)
- All values rounded to the nearest 0.10; minimum value is 0.10

**Output:** `Map<Integer, Map<Integer, Double>>` — symbolId → (matchCount → multiplier).

---

### Step 5 — `ReelSetWeightTuner`
**Spec:** `skills/ltr/05-reel-set-weight-tuner.md`

Iteratively assigns and adjusts probability weights across all reel sets until the
simulated RTP and hit rate converge to within their target tolerances:

| Signal | Action |
|---|---|
| `actualRtp < targetRtp − rtpDelta` | Increase total weight of winning sets × 1.05 |
| `actualRtp > targetRtp + rtpDelta` | Decrease winning sets weight ÷ 1.05 |
| `actualHitRate < targetHitRate − hitRateDelta` | Reduce no-win set weight share |
| `actualHitRate > targetHitRate + hitRateDelta` | Increase no-win set weight share |

**Thresholds:**

| Parameter | Source | Default |
|---|---|---|
| Max tuning iterations | `DEFAULT_ITER` constant | 50 |
| Spins per evaluation | `DEFAULT_SPINS` constant | 500,000 |
| Min weight per reel set | `MIN_WEIGHT` constant | 0.001 |

If convergence is not reached within `maxIterations`, the best weights found (lowest
combined distance to both targets) are returned with `converged = false`.

**Output:** `WeightedReelSets` — all reel sets with normalised weights summing to 1.0,
plus `achievedRtp`, `achievedHitRate`, and `converged` flag.

---

### Final Output Structure

```json
{
  "converged": true,
  "achievedRtp": 95.48,
  "achievedHitRate": 19.83,
  "paytable": {
    "1": { "3": 0.10, "4": 0.40, "5": 1.40 },
    "2": { "3": 0.80, "4": 2.80, "5": 9.80 }
  },
  "reelSets": [
    { "index": 0, "type": "no-win", "weight": 0.0412, "tilesCounts": [[...]] },
    { "index": 5, "type": "win",    "weight": 0.0721, "tilesCounts": [[...]] }
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
→ DefaultAgentOrchestrator.buildGraph() (understand the wiring)
→ each Node class (7 files, ~30 lines each)
→ FileSystemSkillRegistry (skill loading + ID derivation)
→ LtrReelGenerationTool (the full LTR pipeline)
→ AgentController (the entry point)
```
