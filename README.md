# Slots Lab

## Frontend JS Structure

`app.js` (2917 lines) is split into 12 files:

| File | Contents |
|------|----------|
| `js/theme.js` | Theme toggle, CodeMirror instance creation |
| `js/ui-utils.js` | Tab switching, toast, errors, status, copy, info modal, `compactJson`, `escapeHtml`, `prettyCompact` |
| `js/generate.js` | Reel set form builder, validate, history, `runGenerate`, import counts |
| `js/convert.js` | Convert tab logic |
| `js/simulation/rtp-reels.js` | `_latestReelSets`, reel sync, RTP chances |
| `js/simulation/sym-config.js` | Symbol rows, type/agg change handlers |
| `js/simulation/line-defs.js` | Payline rows, scatter/sym/linedefs toggle buttons, spin test placeholders |
| `js/simulation/strategy.js` | `onStrategyChange`, interval sets, paytable placeholders, scatter intervals |
| `js/simulation/rtp-sim.js` | RTP request builder, run, result rendering, combo table, history |
| `js/simulation/spin-test.js` | Spin test payload, run, carousel renderer |
| `js/tooltips.js` | Tooltip positioning IIFE, panel resize handles |
| `js/app-init.js` | Bootstrap: `addReelSet()×2`, `loadHistory()`, event wires, `onStrategyChange()`, convert defaults, `initDefaultPaylines` |
