# Slots Lab

> **Live demo:** https://slots-lab.onrender.com

A professional web application for slot machine reel strip generation, RTP (Return to Player) statistical simulation, single-spin testing, and format conversion. Built for game designers and mathematicians in the gaming industry.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Architecture](#architecture)
- [Backend — Java Package Reference](#backend--java-package-reference)
  - [com.slotslab (root)](#comslotslab-root)
  - [com.slotslab.api](#comslotslabapi)
  - [com.slotslab.reel](#comslotslabreel)
  - [com.slotslab.shuffler](#comslotslabshuffler)
  - [com.slotslab.rng](#comslotslabrng)
  - [com.slotslab.convert](#comslotslabconvert)
  - [com.slotslab.service](#comslotslabservice)
  - [com.slotslab.simulation.config](#comslotslabsimulationconfig)
  - [com.slotslab.simulation.eval](#comslotslabsimulationeval)
  - [com.slotslab.simulation.strategy](#comslotslabsimulationstrategy)
  - [com.slotslab.simulation.stats](#comslotslabsimulationstats)
  - [com.slotslab.simulation.web](#comslotslabsimulationweb)
  - [com.slotslab.dto](#comslotslabdto)
  - [com.slotslab.history](#comslotslabhistory)
  - [com.slotslab.keepalive](#comslotslabkeepalive)
  - [com.slotslab.ui](#comslotslabui)
  - [com.slotslab.wrapper](#comslotslabwrapper)
- [Frontend — JavaScript Module Reference](#frontend--javascript-module-reference)
  - [core/](#core)
  - [generate/](#generate)
  - [simulation/](#simulation)
  - [convert/](#convert)
  - [app.js](#appjs)
- [Frontend — CSS Reference](#frontend--css-reference)
- [REST API](#rest-api)
- [Configuration](#configuration)
- [Session & History](#session--history)
- [Deployment](#deployment)

---

## Overview

Slots Lab is a single-page Spring Boot application that covers the full mathematical design workflow for video slot games:

1. **Generate** — produce reel strips from symbol tile-count configurations using either a deterministic flat layout or a stochastic stacked/shuffled algorithm with distance restrictions.
2. **Simulate** — run parallel multi-million-spin RTP simulations across nine distinct payout strategies and receive detailed statistical output (RTP%, variance, volatility, hit rate, per-combination breakdown).
3. **Spin Test** — evaluate one to many individual spins with full payout detail, using fixed screens, fixed reel stops, or random draws.
4. **Convert** — transform generated reel sets between three serialization formats (JSON Array, COUNT, CSV).

---

## Features

- **Two generation strategies** — `FLAT` (deterministic symbol expansion) and `SHUFFLE` (weighted stacking + distance enforcement with automatic conflict resolution)
- **Nine payout strategies** — LTR, RTL, BW, ADJ, SL, WAYS, MEGAWAYS, SCATTERS, CLUSTERS
- **Advanced wild handling** — multiplier aggregation modes: `NONE`, `ADD`, `MULTIPLY`, `SEQUENCE`
- **Megaways support** — per-reel variable height drawn from configurable probability distributions
- **Scatter / Cluster evaluation** — interval-based paytables, named interval sets assignable per symbol, configurable adjacency offsets
- **Parallel simulation** — up to 8 threads; results are merged with statistically correct aggregation
- **Reservoir-sampling median** — accurate median win computation across tens of millions of spins without storing all values
- **History** — JSON-persisted history for both generation and simulation results (configurable max size, per-session isolated, auto-purged after 7 days of inactivity)
- **Internationalization** — EN / RU / ZH with full tooltip HTML translation
- **Dark / Light theme** — CSS custom-property based, persisted in `localStorage`
- **Keep-alive** — self-pinging daemon thread prevents Render free-tier sleep

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.5.3 |
| Web | Spring MVC, Thymeleaf |
| Build | Maven 3.9, multi-stage Docker |
| RNG | `SplittableRandom` (java.base, always available on JRE images) |
| Config | `spring-dotenv` (`.env` file support) |
| Frontend | Vanilla JS (ES6+), CSS custom properties |
| Editor | CodeMirror 5 (JSON editor) |
| Deploy | Render (Docker, free tier) |

---

## Getting Started

```bash
# Build
mvn package -DskipTests

# Run (dev mode — live reload via Spring DevTools)
mvn spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=WaysEvaluatorTest

# Run a specific test method
mvn test -Dtest=WaysEvaluatorTest#screen1_sym5_waysWin
```

The app starts on `http://localhost:8080`.

For local environment variables copy `.env.example` to `.env` and fill in your values. The `spring-dotenv` library loads it automatically on startup.

---

## Architecture

```
Browser (SPA)
    │
    │  HTTP / JSON
    ▼
Spring MVC Controllers
    ├── GeneratorController     POST /api/generate, /api/convert
    ├── RtpController           POST /api/rtp/simulate
    ├── SpinTestController      POST /api/spin-test
    ├── HistoryController       GET/POST/PUT/DELETE /api/history/{kind}
    └── UiController            GET /,  GET /keep-alive

Services / Generators
    ├── GeneratorService        → ShuffleGenerator | FlatGenerator
    ├── ConverterService        → CountConverter | JsonArrayConverter | CsvConverter
    ├── RtpSimulationService    → RtpWorker (×1-8 threads)
    └── SpinTestService         → single-spin evaluation

Evaluation Engines
    ├── LineEvaluator           LTR / RTL / ADJ / SL
    ├── WaysEvaluator           WAYS
    ├── MegawaysPayoutStrategy  MEGAWAYS
    ├── ScattersEvaluator       SCATTERS
    └── ClustersEvaluator       CLUSTERS

Persistence
    └── HistoryService          JSON files in history/generate/ and history/simulate/
```

The screen is always stored as `int[reel][row]` (column-major). Symbol ID `0` is a mask — a non-existent position used by MEGAWAYS to model variable reel heights.

---

## Backend — Java Package Reference

### `com.slotslab` (root)

| Class | Role |
|---|---|
| `SlotsLabApp` | Spring Boot entry point — starts the application context |

---

### `com.slotslab.api`

Public REST layer for reel generation and format conversion.

| Class | Role |
|---|---|
| `GeneratorController` | `POST /api/generate` — validates and delegates to `GeneratorService`; `POST /api/convert` — delegates to `ConverterService` |
| `GenerateRequest` | Request body record wrapping a `ReelSetsCollectionData` config |
| `ConvertRequest` | Request body for conversion: list of named reel sets, target format, game ID |
| `ApiResponse` | Uniform response envelope — `result` (success payload as JSON string) or `error` (message) |

---

### `com.slotslab.reel`

Domain model for reel strip configuration.

| Class | Role |
|---|---|
| `ReelSetsCollectionData` | Top-level config record — `mapName`, `gameId`, `strategy`, `output`, `resultFilePath`, `convert`, `reelSets`; extensive `@JsonAlias` annotations accept multiple field names from different upstream formats |
| `ReelSet` | One reel set: `tilesCounts` (per-reel symbol counts, accepts `counts`/`cnt`/`cnts`/`tiles`/`reelSetTileCounts` aliases) and optional `restrictions` |
| `ReelSetNamed` | Generated output record — `setName`, `reelSet` (flat strip arrays), optional `chance`, optional `reelTileChances` |
| `Restriction` | Stack stacking rule — `stackSizes` (stack heights), `stackChances` (weights), `minDistance` (min tiles between same-symbol stacks) |
| `Strategy` | Enum: `SHUFFLE`, `FLAT`, `UNKNOWN` (Jackson default) |
| `Output` | Enum: `file`, `stdout`, `UNKNOWN` |

---

### `com.slotslab.shuffler`

Reel strip generation algorithms.

| Class | Role |
|---|---|
| `FlatGenerator` | Deterministic generation — expands each symbol count into sequential tiles then concatenates. No randomness, always produces the same ordered strip. |
| `ShuffleGenerator` | Stochastic generation — for each reel, randomly places symbols in stacks drawn from the restriction's weighted distribution, then validates and enforces `minDistance`. Retries up to 8 times per reel if distance constraints cannot be satisfied. |
| `RestrictionsApplier` | Core constraint-satisfaction algorithm. Uses `Xoshiro256PlusPlus` RNG to place weighted stacks; tracks a "forbidden distance" window to prevent same-symbol proximity; falls back to insertion-at-valid-position traversal after repeated failures; post-processes head/tail wrap-around conflicts. |

**Generation flow (SHUFFLE):**
1. Build a `TreeMap<symbolId, count>` from tile counts
2. Randomly pick a symbol, draw a stack size from the weighted restriction
3. If the symbol is not forbidden by distance: append the stack, update the distance tracker
4. If forbidden: attempt to insert at a valid position in the already-built reel
5. After all tiles are placed: fix head/tail wrap conflicts so the reel loops cleanly
6. If unresolvable after 1 000 attempts: return `null` (outer loop retries up to 8×)

---

### `com.slotslab.rng`

| Class | Role |
|---|---|
| `IRNG` | Interface: `getRandInRange(l, r)`, `getDouble(l, r)`, `getWeightedRand(outcomes, chances)` |
| `RNG` | Implementation using `SplittableRandom` (always available in `java.base` — works on JRE slim images). `getWeightedRand` builds a cumulative `TreeMap` and does a single `higherEntry` lookup for O(log n) weighted selection. |

---

### `com.slotslab.convert`

Format converters — all stateless utility classes.

| Class | Role |
|---|---|
| `ConverterType` | Enum: `COUNT`, `JSON_ARRAY`, `CSV` |
| `JsonArrayConverter` | Serializes reel sets to the standard `[{setName, reelSet: [[...],...]}]` JSON format, preserving `chance` and `reelTileChances` if present |
| `CountConverter` | Converts reel strips to a count-per-symbol-per-reel format — a `TreeMap` of `{symbolId → count}` per reel, formatted as a 2D JSON array. Useful for comparing strip distributions. |
| `CsvConverter` | Emits one CSV row per tile position: `"setName","reelIndex","tileIndex","gameIdXXX"`. `XXX` is the zero-padded three-digit symbol ID. |

---

### `com.slotslab.service`

Application service layer.

| Class | Role |
|---|---|
| `GeneratorService` | Validates config via `ConfigValidator`, then dispatches to `FlatGenerator` or `ShuffleGenerator` based on `strategy` |
| `ConverterService` | Dispatches `ConvertRequest` to the appropriate converter; constructs a minimal `ReelSetsCollectionData` for CSV conversion to supply `gameId` |
| `ConfigValidator` | Validates `ReelSetsCollectionData` before generation — checks `mapName`, `gameId`, strategy presence, non-empty reel sets, non-negative tile counts, restriction count ≤ reel count, equal-length `stackSizes`/`stackChances`, positive stack sizes, non-negative chances that sum to 100, non-negative distance |

---

### `com.slotslab.simulation.config`

Immutable configuration records for the simulation engine.

| Class | Role |
|---|---|
| `SymbolConfig` | Per-symbol config — `symbolId`, `type` (NORMAL/WILD/SCATTER), `paytable` (multipliers indexed as `[matchCount − minMatch]`), `wildMultiplier`, `wildAggregation`, `wildSequence`, `contactsIntervalSetName` |
| `SymbolTable` | Wraps `List<SymbolConfig>` in a `HashMap` for O(1) lookups by ID; caches `wildId`; exposes `isWild()`, `isScatter()`, `get()`, `all()` |
| `SymbolType` | Enum: `NORMAL`, `WILD`, `SCATTER` |
| `WildMultiplierAggregation` | Enum: `NONE` (no multiplier), `ADD` (add wild count as bonus ways), `MULTIPLY` (multiply win by wild multiplier), `SEQUENCE` (cycle through a predefined multiplier sequence) |
| `ReelSetChance` | Maps a reel set index to a selection weight (percentage) |
| `AdjacencyOffset` | Relative `(x=reel, y=row)` offset defining one neighbour direction for cluster detection |
| `ScattersPaytableEntry` | Interval `[from, to]` — a contact whose tile count falls in this range maps to a paytable index |
| `ScattersIntervalSet` | Named list of `ScattersPaytableEntry` items; symbols reference a set by name via `contactsIntervalSetName` |

---

### `com.slotslab.simulation.eval`

Pure evaluation functions — no state, no Spring wiring.

| Class | Role |
|---|---|
| `LineEvaluator` | Evaluates payline-based strategies (LTR, RTL, ADJ, SL). Iterates each line definition, finds the longest winning run from the relevant side, applies wild substitution and multiplier aggregation. |
| `WaysEvaluator` | All-ways evaluation. For each normal symbol, collects matching positions per reel (including wilds), multiplies ways counts across reels. Has two entry points: `evalWays()` (full `WayLinesDto` for spin-test) and `evalWaysTracked()` (win total + hit/pay maps for RTP simulation performance). Wild-only streaks are handled as a separate fallback when no normal symbol spans `minMatch` reels. |
| `ScattersEvaluator` | Non-positional scatter evaluation. Counts all scatter-type tiles on the screen; looks up the contact count in the interval set assigned to each scatter symbol; returns payout. |
| `ClustersEvaluator` | BFS/flood-fill cluster detection. Starting from each unvisited tile, expands via the configured `adjacencyOffsets`; if a cluster reaches `minMatch` size it pays according to the symbol's interval-based paytable. |

---

### `com.slotslab.simulation.strategy`

Strategy pattern wiring.

| Class | Role |
|---|---|
| `PayoutStrategy` | Interface: `double evaluate(screen, screenWidth, symbols, lines, minMatch)` |
| `PayoutStrategyType` | Enum: `LTR`, `RTL`, `BW`, `ADJ`, `SL`, `WAYS`, `MEGAWAYS`, `SCATTERS`, `CLUSTERS` |
| `PayoutStrategyFactory` | Factory — `create(type)` returns the matching strategy instance |
| `LtrPayoutStrategy` | Left-to-right lines via `LineEvaluator` |
| `RtlPayoutStrategy` | Right-to-left lines via `LineEvaluator` |
| `BwPayoutStrategy` | Both-ways — sums LTR and RTL independently |
| `AdjPayoutStrategy` | Adjacent pays — any consecutive window of symbols on a line |
| `SlPayoutStrategy` | Super lines — evaluates both LTR and RTL on the same pass |
| `WaysPayoutStrategy` | Delegates to `WaysEvaluator.evalWaysTracked()` |
| `MegawaysPayoutStrategy` | WAYS evaluation on a variable-height screen built by masking excess rows per reel |
| `ScattersPayoutStrategy` | Delegates to `ScattersEvaluator` |
| `ClustersPayoutStrategy` | Delegates to `ClustersEvaluator` |

---

### `com.slotslab.simulation.stats`

Statistics data structures used by the simulation engine.

| Class | Role |
|---|---|
| `SpinStats` | Immutable record aggregating one worker's results — `totalWin`, `maxWin`, `sumSquaredWin`, `hitCount`, `medianTracker`, `hitCounts` (per combo), `payouts` (per combo) |
| `ComboStats` | Output record per symbol/match combination — `symbolId`, `matchCount`, `matchLabel`, `hitCount`, `totalPayout` |
| `ComboKey` | Value-based key `(symbolId, matchCount)` used in the hit/pay maps |
| `MedianTracker` | Reservoir-sampling median estimator. Maintains a fixed 100 000-element reservoir; uses random replacement for subsequent samples (Vitter's Algorithm R). `merge()` combines two trackers for thread aggregation. Used to compute median win without storing all spin results. |

---

### `com.slotslab.simulation.web`

HTTP layer for the simulation subsystem.

| Class | Role |
|---|---|
| `RtpController` | `POST /api/rtp/simulate` — deserializes `RtpRequest`, calls `RtpSimulationService`, serializes `RtpResult` to JSON string inside `ApiResponse` |
| `RtpSimulationService` | Orchestrates the full simulation: validates all parameters, converts reel set data to `int[][][]` arrays, builds cumulative chance arrays, constructs `SymbolTable` and payout strategy, partitions spins across 1–8 `RtpWorker` callables, merges `SpinStats` results, computes RTP%, volatility label, hit rate, and per-combo breakdown |
| `RtpWorker` | `Callable<SpinStats>` — processes its spin slice. On each spin: draws a reel set by chance, draws stops, builds the screen, evaluates the payout strategy, updates running totals and the `MedianTracker`. MEGAWAYS path draws reel heights from per-reel cumulative distributions before building the screen. |
| `RtpRequest` | Full simulation config record — reel sets, chances, symbols, strategy, screen dimensions, min match, line definitions, spin count, thread count, bet size, interval sets, adjacency offsets, megaways height distributions |
| `RtpResult` | Output record — RTP%, total spins, elapsed ms, bet size, avg/median/max win, std dev, volatility index/label, hit rate, combo breakdown list |
| `SpinTestController` | `POST /api/spin-test` — evaluates 1–N spins with full per-line detail |
| `SpinTestService` | Builds screens from fixed stops, fixed screen input, or random draws; calls the appropriate evaluator for detailed output; returns `List<SpinData>` |
| `SpinTestRequest` | Same config as `RtpRequest` plus: `count`, optional `reelSetIndex`, optional `stops`, optional `screen` |

**Volatility classification:**

| Index range | Label |
|---|---|
| < 3 | Low |
| 3 – 6 | Medium |
| 6 – 15 | High |
| ≥ 15 | Extreme |

---

### `com.slotslab.dto`

Data transfer objects for spin-test output.

| Package | Class | Role |
|---|---|---|
| `dto.spin` | `SpinData` | One spin result — `reelSetIndex`, `stops`, `screen`, `payouts`, `reelHeights` (MEGAWAYS) |
| `dto.spin` | `PayoutEntry` | Wraps a single payout line descriptor (polymorphic `Object lineData`) |
| `dto.lines` | `SimpleLineDto` | One payline win — line index, symbol, match count, pay amount, positions |
| `dto.lines` | `SimpleLinesDto` | List of `SimpleLineDto` with total multiplier |
| `dto.ways` | `WayLineDto` | One ways win — includes 1D and 2D position lists, ways counts, per-reel multiplied ways, total simple lines count |
| `dto.ways` | `WayLinesDto` | List of `WayLineDto` with aggregate multiplier |
| `dto.scatters` | `ContactDto` | One scatter/cluster contact result |
| `dto.scatters` | `ContactsDto` | List of contacts with total pay |

---

### `com.slotslab.history`

In-process history persistence using the local filesystem with per-session isolation.

| Class | Role |
|---|---|
| `HistoryEntry` | Record — `id` (timestamp string), `strategy`, `time` (display string), `result` (raw HTML), `config` (JSON config snapshot) |
| `HistoryService` | Persists entries as individual JSON files under `history/generate/<sessionId>/` and `history/simulate/<sessionId>/`. Supports list (sorted newest-first), save, resize (trim oldest beyond max), delete one, clear all. Max size clamped to 1–20. |
| `HistoryController` | REST endpoints at `/api/history/{kind}` — `GET` list, `POST` save, `PUT /resize?size=N` trim, `DELETE /{id}` delete one, `DELETE` clear all. All endpoints read the session cookie to scope the operation to the caller's history. |
| `HistoryCleanupService` | `@Scheduled` daily task (03:17) — walks all session subdirectories under `history/generate/` and `history/simulate/` and deletes any whose `lastModifiedTime` is older than `history.session-ttl-days` (default 7). |

---

### `com.slotslab.keepalive`

| Class | Role |
|---|---|
| `KeepAliveService` | `@Component` — on `@PostConstruct`, checks for `RENDER` env var; if present, starts a Java virtual thread that waits `initial-delay-seconds` (default 15), then pings `{RENDER_EXTERNAL_URL}/keep-alive` every `interval-seconds` (default 840 = 14 min) to prevent Render free-tier sleep. Stopped cleanly on `@PreDestroy`. Both timing values are configurable via `application.yml` under the `keep-alive` namespace. |

---

### `com.slotslab.ui`

| Class | Role |
|---|---|
| `UiController` | `GET /` — serves the Thymeleaf `index` template (calls `SessionUtil.ensureSession` to set the session cookie on first visit); `GET /keep-alive` — returns `200 ok` for the keep-alive ping |
| `SessionUtil` | Static helper for cookie-based session isolation. `ensureSession()` — sets the `slotlab-session` UUID cookie (HttpOnly, path `/`, 1-year max-age) on first visit if not already present. `readSession()` — reads the cookie value from any request; falls back to `"anonymous"`. All history operations are scoped to the resolved session ID. |

---

### `com.slotslab.wrapper`

| Class | Role |
|---|---|
| `ConvertWrapper` | Config sub-record for inline conversion during generation — `enabled`, `toCom` (target format), `gameId`, `resultFilePath` |

---

## Frontend — JavaScript Module Reference

All scripts are plain ES6 vanilla JS, loaded directly in `index.html`. There is no bundler.

### `core/`

| File | Role |
|---|---|
| `i18n.js` | Internationalization engine. Exports `t(key)` lookup function and `applyI18n()` DOM updater. Supports `data-i18n` (textContent), `data-i18n-attr="title"` (attribute), and `data-i18n-html` (innerHTML for rich tooltip HTML). Three locale objects: EN, RU, ZH. Active locale stored in `localStorage`. |
| `theme.js` | Dark/light theme toggle. Reads/writes `data-theme` on `<html>`. Persists choice in `localStorage`. Updates the theme button SVG icon. |
| `tooltips.js` | Initialises global tooltip positioning. Handles `.stat-tip-box` elements — repositions them to `tip-right` or `tip-left` based on available viewport space to prevent overflow. |
| `app-init.js` | Application bootstrap — called on `DOMContentLoaded`. Loads history, applies i18n, initialises theme, sets default form values, wires CodeMirror editor, and calls `onStrategyChange()` to render the initial UI state. |
| `ui-utils.js` | Shared utilities: `setStatus(tab, ok, msg)` (shows success/error badge), `confirmDelete(msg)` (modal confirmation), `copyToClipboard(text)`, `showToast(msg)`, `getMaxHistory()`, `addAdjacencyOffset()`, history sidebar helpers. |

### `generate/`

| File | Role |
|---|---|
| `generate.js` | All logic for the Generate tab. Builds the JSON config from form inputs (reel sets, restrictions, stack sizes/chances/distance), calls `POST /api/generate`, renders output in CodeMirror, pushes to history, and handles the Import modal. Also exports `tryLoadReelsFromEditor()` used by the Simulation tab to import generated reels. |

### `simulation/`

| File | Role |
|---|---|
| `rtp-reels.js` | Manages shared reel set state (`_latestReelSets`). `storeGeneratedReels()` — parses generated JSON, syncs reel set chances UI, megaways height inputs, and auto-populates symbol rows from detected symbol IDs. `syncRtpChances()` — rebuilds the per-reel-set chance inputs with equal initial distribution. `syncMegawaysHeights()` — rebuilds the per-reel height chance inputs preserving previously entered values. `restoreRtpForm()` — restores the full form state from a history entry's config snapshot. |
| `strategy.js` | `onStrategyChange()` — shows/hides form sections depending on selected payout strategy (paylines for LTR/RTL/BW/ADJ/SL, interval sets for SCATTERS/CLUSTERS, adjacency offsets for CLUSTERS, megaways heights for MEGAWAYS). Also calls `updateSymConfigToggleBtn()` to keep button state consistent after a strategy switch. |
| `sym-config.js` | Symbol configuration section. `addSymbolRow()` — dynamically renders a symbol row with type selector, paytable input, wild aggregation options (shown/hidden per type), and interval-set selector for scatter/cluster symbols. `onSymbolTypeChange()` — toggles visible fields when type changes. `onWildAggChange()` — shows/hides multiplier/sequence inputs. `syncRtpSymbolsFromReels()` — auto-adds rows for new symbol IDs found in reel data. `collectSymbols()` — serializes all symbol rows to `SymbolConfig` objects. |
| `line-defs.js` | Payline definition section. `addLineDef(value)` — appends a payline row with position array input and remove button. `generateDefaultLines(w, h)` — creates standard horizontal paylines for width×height. `collectLines()` — parses and validates all line inputs. `updateLineCount()` — updates the count badge. |
| `rtp-sim.js` | RTP simulation orchestration. `collectRtpRequest()` — assembles the full `RtpRequest` from all form sections. `runRtpSimulation()` — POSTs to `/api/rtp/simulate`, shows a loading spinner, then calls `renderRtpResult()`. `renderRtpResult()` — builds the full result HTML with stat cards, strategy card, volatility card, and the combo breakdown table. `_comboRender()` / `_comboSort()` / `_comboRemove()` — multi-column sortable combo table with pill-based sort UI. `pushRtpHistory()` / `loadRtpHistory()` — history persistence. |
| `spin-test.js` | Spin test tab. `buildSpinTestRequest()` — collects the shared simulation config plus test-specific fields (count, optional fixed stops/screen). `runSpinTest()` — POSTs to `/api/spin-test`, renders each `SpinData` result as a formatted DTO card with screen grid, stops, payouts. |

### `convert/`

| File | Role |
|---|---|
| `convert.js` | Format conversion tab. Reads reel set JSON from the CodeMirror editor or from the generate output, adds `gameId` from the input field, POSTs to `/api/convert`, and displays the result. Supports copying output to clipboard. |

### `app.js`

Entry point loaded last. Wires tab switching: clicking a tab button sets the active class, shows the correct panel, and restores any tab-specific UI state (e.g. re-triggering strategy visibility).

---

## Frontend — CSS Reference

All stylesheets use CSS custom properties defined in `base.css`. Themes are switched by setting `data-theme="light"` on `<html>`.

| File | Scope |
|---|---|
| `base.css` | Design tokens (`--surface`, `--accent`, `--text`, `--border`, etc.), CSS reset, root layout (`html`, `body` as flex column), typography |
| `header.css` | App header, logo gradient, title/subtitle, theme button, language switcher dropdown |
| `tabbar.css` | Tab navigation bar, active/hover tab button states |
| `layout.css` | Main content area, two-pane resizable split layout (config pane + output pane), resize drag handle, panel scroll areas |
| `forms.css` | All form controls — text inputs, number inputs, selects, buttons (primary run, icon, add, copy, toggle), status badges, label rows, array inputs |
| `generate.css` | Reel set cards, restriction cards, add/remove controls, reel row layout inside cards |
| `simulation.css` | RTP simulation tab — section panels, chance rows, megaways height grid, symbol config rows, wild aggregation extras, line definition rows, adjacency offset rows |
| `rtp-results.css` | Result stat cards grid, volatility badge, strategy card, combo breakdown table, multi-sort pill bar |
| `spin-test.css` | Spin test result cards, screen grid display, payout list, DTO label |
| `convert.css` | Convert tab layout, game ID input, format selector |
| `history.css` | History sidebar, history item rows, delete button |
| `modal.css` | Modal overlay, modal panel, close button, import/info modal content |
| `codemirror.css` | CodeMirror editor theming — matches app dark/light color scheme |
| `toast.css` | Toast notification positioning and animation |

---

## REST API

### `POST /api/generate`
Generate reel strips.

**Request body:** `ReelSetsCollectionData` JSON
**Response:** `{ "result": "[\n  {...}, ...]", "error": null }`

### `POST /api/convert`
Convert reel sets between formats.

**Request body:**
```json
{
  "reelSets": [{ "setName": "ReelSet#0", "reelSet": [[1,2,3,...]], "chance": 50.0 }],
  "toCom": "COUNT | JSON_ARRAY | CSV",
  "gameId": "GAME01"
}
```

### `POST /api/rtp/simulate`
Run a multi-threaded RTP simulation.

**Key request fields:**
```json
{
  "reelSets": [...],
  "reelSetChances": [{ "setIndex": 0, "chance": 100.0 }],
  "symbols": [{ "symbolId": 1, "type": "NORMAL", "paytable": [0.5, 1.0, 5.0] }],
  "strategy": "LTR",
  "screenWidth": 5,
  "screenHeight": 3,
  "minMatch": 3,
  "lineDefinitions": [[0,0,0,0,0], [1,1,1,1,1]],
  "spins": 10000000,
  "threadCount": 4,
  "betSize": 1.0
}
```

**Response:** `RtpResult` JSON including `rtpPercent`, `volatilityLabel`, `comboBreakdown`, etc.

### `POST /api/spin-test`
Evaluate individual spins with full detail.

**Additional fields over simulate:**
```json
{
  "count": 5,
  "reelSetIndex": 0,
  "stops": [0, 12, 7, 3, 20],
  "screen": [[1,2,3],[4,5,6],[7,8,9],[1,2,3],[4,5,6]]
}
```

### History endpoints (`/api/history/{kind}`)

`kind` is either `generate` or `simulate`.

| Method | Path | Action |
|---|---|---|
| `GET` | `/api/history/{kind}` | List all entries (newest first) |
| `POST` | `/api/history/{kind}` | Save a new entry |
| `PUT` | `/api/history/{kind}/resize?size=N` | Trim to max N entries |
| `DELETE` | `/api/history/{kind}/{id}` | Delete one entry |
| `DELETE` | `/api/history/{kind}` | Clear all entries |

---

## Configuration

### `application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: slot-lab
  thymeleaf:
    cache: false
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
    env:
      show-values: always

keep-alive:
  interval-seconds: 840    # 14 minutes
  initial-delay-seconds: 15

history:
  session-ttl-days: 7      # session directories older than this are auto-purged
```

### Environment variables (`.env`)

| Variable | Purpose |
|---|---|
| `RENDER` | Set to `true` on Render — activates the keep-alive thread |
| `RENDER_EXTERNAL_URL` | Public app URL — used to construct the ping target |
| `JAVA_TOOL_OPTIONS` | JVM flags — use `-Xmx400m` on Render free tier (512 MB RAM) |

Copy `.env.example` to `.env` for local use.

---

## Session & History

### How sessions work

On the first `GET /` request the server sets a cookie named `slotlab-session` containing a random UUID:

```
Set-Cookie: slotlab-session=<uuid>; Path=/; HttpOnly; Max-Age=31536000
```

Every subsequent request from the same browser sends this cookie automatically. All history reads and writes are scoped to the resolved session ID — concurrent users never see each other's data.

| Scenario | Result |
|---|---|
| Same browser, multiple visits | Same UUID → same history |
| Two different browsers on the same device | Different UUIDs → separate history |
| Cookie cleared | New UUID on next visit → fresh empty history |
| Incognito / private window | New UUID per session → separate history |

### Storage layout

```
history/
├── generate/
│   ├── <sessionId-A>/
│   │   ├── 2026-07-18T10-23-45.json
│   │   └── 2026-07-19T08-01-12.json
│   └── <sessionId-B>/
│       └── ...
└── simulate/
    ├── <sessionId-A>/
    │   └── ...
    └── <sessionId-B>/
        └── ...
```

Each entry is one JSON file named by timestamp. The session subdirectory is created on the first save.

### Automatic expiry

`HistoryCleanupService` runs a `@Scheduled` task daily at 03:17. It checks the `lastModifiedTime` of every session directory. Any directory that has not been written to in more than `history.session-ttl-days` days (default 7) is deleted recursively — both `generate` and `simulate` subdirectories.

The TTL is configured in `application.yml`:

```yaml
history:
  session-ttl-days: 7
```

> **Note:** On Render's free tier the filesystem is ephemeral — it is wiped on every redeploy regardless of TTL. The cleanup job matters mainly for long-running or self-hosted deployments.

---

## Deployment

The app ships with a multi-stage `Dockerfile` for Render (or any Docker host):

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/Slots_Lab-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Steps:**
1. Push to GitHub
2. Render → New → Web Service → connect repo → Runtime: **Docker** → Instance: **Free**
3. After first deploy, copy the assigned URL
4. Add environment variables in Render dashboard:
   - `RENDER = true`
   - `RENDER_EXTERNAL_URL = https://your-app.onrender.com`
5. Save → auto-redeploy triggers
6. Check Logs for `[KeepAlive] Ping OK (200)` every 14 minutes
