/* ── AI Agent Tab ── */

// Escape a string for safe interpolation into innerHTML.
function _aiEsc(s) {
  if (s == null) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ── Agent mode toggle ─────────────────────────────────────────────────────────

const AI_MODES = {
  llm: {
    maxIterations: 3,
    spinsPerIter:  1000000,
    rtpDelta:      0.20,
    hitRateDelta:  2.50,
    maxPayout:     0,
    desc: 'GPT-4o-mini drives each iteration — patches weights, paytable, or strips until target is met'
  },
  classic: {
    maxIterations: 10,
    spinsPerIter:  500000,
    rtpDelta:      0.15,
    hitRateDelta:  2.00,
    maxPayout:     0,
    desc: 'Hardcoded heuristics — brute-force weight seeding, no LLM calls'
  }
};

let _aiCurrentMode = 'llm';

function setAiMode(mode) {
  if (!AI_MODES[mode]) return;
  _aiCurrentMode = mode;
  const cfg = AI_MODES[mode];

  const otherCfg = AI_MODES[mode === 'llm' ? 'classic' : 'llm'];

  const miEl  = document.getElementById('ai-max-iterations');
  const spEl  = document.getElementById('ai-spins-per-iter');
  const rdEl  = document.getElementById('ai-rtp-delta');
  const hdEl  = document.getElementById('ai-hit-rate-delta');

  if (miEl  && (parseInt(miEl.value)   === otherCfg.maxIterations  || miEl.dataset.modeDefault  === 'true')) { miEl.value  = cfg.maxIterations;  miEl.dataset.modeDefault  = 'true'; }
  if (spEl  && (parseInt(spEl.value)   === otherCfg.spinsPerIter   || spEl.dataset.modeDefault  === 'true')) { spEl.value  = cfg.spinsPerIter;   spEl.dataset.modeDefault  = 'true'; }
  if (rdEl  && (parseFloat(rdEl.value.replace(',','.'))  === otherCfg.rtpDelta     || rdEl.dataset.modeDefault  === 'true')) { rdEl.value  = cfg.rtpDelta.toFixed(2);  rdEl.dataset.modeDefault  = 'true'; }
  if (hdEl  && (parseFloat(hdEl.value.replace(',','.'))  === otherCfg.hitRateDelta || hdEl.dataset.modeDefault  === 'true')) { hdEl.value  = cfg.hitRateDelta.toFixed(2); hdEl.dataset.modeDefault = 'true'; }

  const llmBtn = document.getElementById('ai-mode-llm-btn');
  const clsBtn = document.getElementById('ai-mode-classic-btn');
  if (llmBtn && clsBtn) {
    const activeStyle  = 'background:var(--accent,#6c63ff);color:#fff';
    const inactiveStyle = 'background:transparent;color:var(--text2)';
    llmBtn.style.cssText += ';' + (mode === 'llm'     ? activeStyle : inactiveStyle);
    clsBtn.style.cssText += ';' + (mode === 'classic' ? activeStyle : inactiveStyle);
  }

  const descEl = document.getElementById('ai-mode-desc');
  if (descEl) descEl.textContent = cfg.desc;
}

document.addEventListener('DOMContentLoaded', () => setAiMode('llm'));

const AI_LINE_STRATEGIES = new Set(['LTR', 'RTL', 'BW', 'SL', 'ADJ']);

let _aiLineCounter = 0;

function onAiStrategyChange() {
  const strat = document.getElementById('ai-strategy').value;
  const card  = document.getElementById('ai-line-defs-card');
  if (card) card.style.display = AI_LINE_STRATEGIES.has(strat) ? '' : 'none';
}

function aiAddLineDef(defaultValue) {
  const rid = _aiLineCounter++;
  const container = document.getElementById('ai-lines-list');
  const row = document.createElement('div');
  row.className = 'rtp-line-row';
  row.id = 'ai-line-' + rid;
  const w = parseInt(document.getElementById('ai-screen-width').value) || 5;
  const placeholder = Array.from({length: w}, (_, i) => i < 3 ? 1 : 0).join(', ');
  row.innerHTML = `
    <span class="rtp-line-label">#${document.querySelectorAll('#ai-lines-list .rtp-line-row').length + 1}</span>
    <input type="text" class="ai-line-input rtp-line-input array-input" value="${defaultValue || ''}" placeholder="${placeholder}"/>
    <button class="icon-btn danger" onclick="aiRemoveLineDef('ai-line-${rid}')">
      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M9 6V4h6v2"/></svg>
    </button>`;
  container.appendChild(row);
  _aiUpdateLineCount();
}

function aiRemoveLineDef(id) {
  const el = document.getElementById(id);
  if (el) el.remove();
  _aiRenumberLines();
}

function _aiRenumberLines() {
  document.querySelectorAll('#ai-lines-list .rtp-line-label').forEach((el, i) => {
    el.textContent = '#' + (i + 1);
  });
  _aiUpdateLineCount();
}

function _aiUpdateLineCount() {
  const n   = document.querySelectorAll('#ai-lines-list .rtp-line-row').length;
  const btn = document.getElementById('ai-add-payline-btn');
  if (btn) btn.textContent = `+ Add payline (${n})`;
  _aiUpdateLineDefsToggleBtn();
}

function _aiUpdateLineDefsToggleBtn() {
  const btn = document.getElementById('ai-linedefs-toggle-btn');
  if (!btn) return;
  const hasValues = Array.from(document.querySelectorAll('#ai-lines-list .ai-line-input')).some(inp => inp.value.trim() !== '');
  btn.innerHTML = hasValues
    ? `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`
    : `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>`;
  btn.title   = hasValues ? 'Clear all line definitions' : 'Fill with default lines';
  btn.classList.toggle('danger', hasValues);
}

// EGT Burning HOT line definitions (5 reels × 4 rows, 0-indexed), sorted lexicographically
// Default 20 = exactly 5 lines per first-reel value (0,1,2,3)
const AI_EGT_BURNING_HOT_LINES = [
  [0,0,0,0,0], [0,0,0,1,2], [0,0,1,0,0], [0,0,1,2,3],
  [0,1,0,1,0], [0,1,1,0,0], [0,1,2,1,0], [0,1,2,2,3],
  [0,1,2,2,3], [0,1,2,3,2], [1,0,0,0,1], [1,0,0,1,2],
  [1,0,1,0,1], [1,0,1,1,1], [1,0,1,2,3], [1,1,0,1,1],
  [1,1,1,0,1], [1,1,1,1,1], [1,1,1,2,1], [1,1,2,1,1],
  [1,2,1,1,1], [1,2,1,2,1], [1,2,2,1,0], [1,2,2,2,1],
  [1,2,3,2,1], [2,1,0,1,2], [2,1,0,1,2], [2,1,1,1,2],
  [2,1,1,1,2], [2,1,1,2,2], [2,1,2,1,2], [2,2,1,0,0],
  [2,2,1,2,2], [2,2,2,1,0], [2,2,2,2,2], [2,3,3,2,1],
  [3,2,1,0,1], [3,2,1,2,3], [3,2,2,2,3], [3,3,2,1,0],
  [3,3,3,3,3]
];
// Default selection: 5 lines per first-reel value (0,1,2,3), sorted lexicographically
const AI_DEFAULT_LINES = [
  [0,0,0,0,0], [0,0,0,1,2], [0,0,1,0,0], [0,1,0,1,0], [0,1,2,1,0],
  [1,0,0,0,1], [1,0,1,0,1], [1,0,1,2,3], [1,1,1,1,1], [1,2,1,2,1],
  [2,1,0,1,2], [2,1,1,1,2], [2,1,1,2,2], [2,1,2,1,2], [2,2,2,2,2],
  [3,2,1,0,1], [3,2,1,2,3], [3,2,2,2,3], [3,3,2,1,0], [3,3,3,3,3]
];

function aiToggleLineDefs() {
  const lineInputs = document.querySelectorAll('#ai-lines-list .ai-line-input');
  const hasValues  = Array.from(lineInputs).some(inp => inp.value.trim() !== '');
  if (hasValues) {
    lineInputs.forEach(inp => { inp.value = ''; });
    _aiUpdateLineDefsToggleBtn();
  } else {
    const container = document.getElementById('ai-lines-list');
    container.innerHTML = '';
    _aiLineCounter = 0;
    AI_DEFAULT_LINES.forEach(arr => {
      const val = arr.join(', ');
      aiAddLineDef('');
      const inputs = container.querySelectorAll('.ai-line-input');
      const inp = inputs[inputs.length - 1];
      inp.placeholder = val;
      inp.value = val;
    });
    _aiUpdateLineDefsToggleBtn();
  }
}

document.addEventListener('DOMContentLoaded', () => {
  onAiStrategyChange();
  if (AI_LINE_STRATEGIES.has(document.getElementById('ai-strategy').value)) {
    aiToggleLineDefs();
  }
});

// Normalise locale-formatted floats: replace commas with dots, strip non-numeric chars
function initAiFloatInputs() {
  document.querySelectorAll('.ai-float-input').forEach(el => {
    el.addEventListener('blur', () => {
      const v = parseFloat(el.value.replace(',', '.'));
      if (!isNaN(v)) el.value = v.toFixed(2);
    });
    el.addEventListener('keypress', e => {
      if (!/[\d.,\-]/.test(e.key) && !['Backspace','Tab','ArrowLeft','ArrowRight','Delete'].includes(e.key)) {
        e.preventDefault();
      }
    });
  });
}
document.addEventListener('DOMContentLoaded', initAiFloatInputs);

const AI_TIERS = ['senior', 'junior', 'wild', 'scatter', 'multiwild'];
const AI_TIER_LABELS = {
  senior:   'Senior (high-value)',
  junior:   'Junior (low-value)',
  wild:     'Wild',
  scatter:  'Scatter',
  multiwild:'Multiplier Wild'
};
const AI_HINT_PLACEHOLDER = {
  senior:   'e.g. high paying',
  junior:   'e.g. low paying',
  wild:     'substitutes all',
  scatter:  'e.g. 3+ pays bonus',
  multiwild:'e.g. ×2 multiplier'
};

function _aiSymTierClass(tier) {
  return 'ai-sym-tier-' + (tier === 'multiwild' ? 'multiwild' : tier);
}

function addAiSymbolRow(tier) {
  const list = document.getElementById('ai-sym-list');
  const idx  = list.children.length + 1;
  const t    = tier || 'junior';

  const row = document.createElement('div');
  row.className = 'ai-sym-row ' + _aiSymTierClass(t);
  row.dataset.tier = t;

  const tierOpts = Object.entries(AI_TIER_LABELS)
    .map(([v, l]) => `<option value="${v}"${v === t ? ' selected' : ''}>${l}</option>`)
    .join('');

  row.innerHTML = `
    <input type="number" value="${idx}" min="1" max="99" title="Symbol ID"/>
    <select onchange="onAiTierChange(this)">${tierOpts}</select>
    <input type="text" placeholder="${AI_HINT_PLACEHOLDER[t] || ''}" title="Paytable hint for the agent"/>
    <button class="icon-btn remove-btn" onclick="this.closest('.ai-sym-row').remove(); _syncAiWildCard(); _syncAiSymCount();" title="Remove">
      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
    </button>`;

  list.appendChild(row);
  _syncAiWildCard();
}

function onAiTierChange(sel) {
  const row = sel.closest('.ai-sym-row');
  const t   = sel.value;
  row.dataset.tier = t;
  // swap tier colour class
  AI_TIERS.forEach(c => row.classList.remove('ai-sym-tier-' + c));
  row.classList.remove('ai-sym-tier-multiwild');
  row.classList.add(_aiSymTierClass(t));
  // update hint placeholder
  const hint = row.querySelector('input[type="text"]');
  if (hint) hint.placeholder = AI_HINT_PLACEHOLDER[t] || '';
  _syncAiWildCard();
}

function onAiSymCountChange(val) {
  const n = Math.max(2, Math.min(20, parseInt(val) || 0));
  const list = document.getElementById('ai-sym-list');
  const current = list.children.length;
  if (n > current) {
    for (let i = current; i < n; i++) {
      const tier = i >= n - 4 ? 'senior' : 'junior';
      addAiSymbolRow(tier);
    }
    // re-evaluate existing rows: last 4 of the final list should be senior
    _aiApplySeniorToLastFour(list, n);
  } else {
    while (list.children.length > n) list.removeChild(list.lastChild);
    _aiApplySeniorToLastFour(list, n);
    _syncAiWildCard();
  }
}

function _aiApplySeniorToLastFour(list, n) {
  const rows = list.querySelectorAll('.ai-sym-row');
  rows.forEach((row, i) => {
    const isSeniorSlot = i >= n - 4;
    const sel = row.querySelector('select');
    if (!sel) return;
    // Only auto-assign if the row is still at its default tier (junior)
    // Don't overwrite explicit user choices (wild/scatter/multiwild)
    const current = sel.value;
    if (isSeniorSlot && current === 'junior') {
      sel.value = 'senior';
      onAiTierChange(sel);
    } else if (!isSeniorSlot && current === 'senior') {
      sel.value = 'junior';
      onAiTierChange(sel);
    }
  });
}

function _syncAiSymCount() {
  const list = document.getElementById('ai-sym-list');
  const inp  = document.getElementById('ai-sym-count');
  if (inp) inp.value = list.children.length;
}

function _syncAiWildCard() {
  // no-op — wild multiplier config removed; agent derives it from hints
}

document.addEventListener('DOMContentLoaded', () => {
  const countInp = document.getElementById('ai-sym-count');
  if (countInp) onAiSymCountChange(countInp.value);
});

function collectAiRequest() {
  const list = document.getElementById('ai-sym-list');
  const symbols = Array.from(list.children).map(row => {
    const inputs = row.querySelectorAll('input');
    const sel    = row.querySelector('select');
    return {
      symbolId: parseInt(inputs[0].value) || 0,
      tier:     sel ? sel.value : 'junior',
      hint:     inputs[1] ? inputs[1].value.trim() : ''
    };
  });

  return {
    targetRtp:        parseFloat(parseFloat(document.getElementById('ai-target-rtp').value.replace(',','.')).toFixed(2)) || 95.50,
    rtpDelta:         parseFloat(parseFloat(document.getElementById('ai-rtp-delta').value.replace(',','.')).toFixed(2)) || 0.15,
    targetHitRate:    parseFloat(parseFloat(document.getElementById('ai-target-hit-rate').value.replace(',','.')).toFixed(2)) || 20.00,
    hitRateDelta:     parseFloat(parseFloat(document.getElementById('ai-hit-rate-delta').value.replace(',','.')).toFixed(2)) || 2.00,
    targetVolatility: document.getElementById('ai-target-volatility').value,
    maxPayout:        parseFloat(document.getElementById('ai-max-payout').value.replace(',','.')) || 0,
    reelConfig:       null,
    parameters: {
      strategy:         document.getElementById('ai-strategy').value,
      useLlm:           _aiCurrentMode === 'llm',
      screenWidth:      parseInt(document.getElementById('ai-screen-width').value) || 5,
      screenHeight:     parseInt(document.getElementById('ai-screen-height').value) || 3,
      minMatch:         parseInt(document.getElementById('ai-min-match').value) || 3,
      symsPerReel:      parseInt(document.getElementById('ai-syms-per-reel').value) || 256,
      symsPerReelDelta: parseInt(document.getElementById('ai-syms-per-reel-delta').value) || 16,
      maxIterations:    parseInt(document.getElementById('ai-max-iterations').value) || 3,
      spinsPerIter:     parseInt(document.getElementById('ai-spins-per-iter').value) || 1000000,
      symbols,
      lines: (function() {
        const strat = document.getElementById('ai-strategy').value;
        if (!AI_LINE_STRATEGIES.has(strat)) return undefined;
        const rows = document.querySelectorAll('#ai-lines-list .ai-line-input');
        if (!rows.length) return undefined;
        return Array.from(rows).map(inp =>
          inp.value.trim().split(/[\s,]+/).map(Number).filter(n => !isNaN(n))
        ).filter(arr => arr.length > 0);
      })()
    }
  };
}

function validateAiRequest() {
  clearErrors();
  const errors = [];

  const stratEl  = document.getElementById('ai-strategy');
  const widthEl  = document.getElementById('ai-screen-width');
  const heightEl = document.getElementById('ai-screen-height');
  const mmEl     = document.getElementById('ai-min-match');
  const sprEl    = document.getElementById('ai-syms-per-reel');
  const sprDEl   = document.getElementById('ai-syms-per-reel-delta');
  const miEl     = document.getElementById('ai-max-iterations');
  const spEl     = document.getElementById('ai-spins-per-iter');
  const rtpEl    = document.getElementById('ai-target-rtp');
  const rtpDEl   = document.getElementById('ai-rtp-delta');
  const hrEl     = document.getElementById('ai-target-hit-rate');
  const hrDEl    = document.getElementById('ai-hit-rate-delta');
  const volEl    = document.getElementById('ai-target-volatility');
  const mpEl     = document.getElementById('ai-max-payout');

  const w    = parseInt(widthEl.value);
  const h    = parseInt(heightEl.value);
  const mm   = parseInt(mmEl.value);
  const spr  = parseInt(sprEl.value);
  const sprD = parseInt(sprDEl.value);
  const mi   = parseInt(miEl.value);
  const sp   = parseInt(spEl.value);

  if (!widthEl.value.trim() || isNaN(w) || w < 1 || w > 20)
    errors.push(markError(widthEl, 'Width must be between 1 and 20'));
  if (!heightEl.value.trim() || isNaN(h) || h < 1 || h > 10)
    errors.push(markError(heightEl, 'Height must be between 1 and 10'));
  if (!sprEl.value.trim() || isNaN(spr) || spr < 16 || spr > 512)
    errors.push(markError(sprEl, 'Symbols per Reel must be between 16 and 512'));
  if (sprDEl.value.trim() && (!isNaN(sprD)) && (sprD < 0 || sprD >= spr))
    errors.push(markError(sprDEl, 'Syms per Reel Delta must be ≥ 0 and < Symbols per Reel'));
  if (!mmEl.value.trim() || isNaN(mm) || mm < 1 || mm > 20)
    errors.push(markError(mmEl, 'Min Match must be between 1 and 20'));
  if (!isNaN(w) && !isNaN(mm) && mm > w)
    errors.push(markError(mmEl, 'Min Match cannot exceed Width (' + w + ')'));
  if (!miEl.value.trim() || isNaN(mi) || mi < 1 || mi > 50)
    errors.push(markError(miEl, 'Max Iterations must be between 1 and 50'));
  if (!spEl.value.trim() || isNaN(sp) || sp < 100000 || sp > 10000000)
    errors.push(markError(spEl, 'Spins per Iteration must be between 100 000 and 10 000 000'));

  const rtp  = parseFloat(rtpEl.value.replace(',', '.'));
  const rtpD = parseFloat(rtpDEl.value.replace(',', '.'));
  const hr   = parseFloat(hrEl.value.replace(',', '.'));
  const hrD  = parseFloat(hrDEl.value.replace(',', '.'));
  const mp   = parseFloat(mpEl.value.replace(',', '.'));

  if (isNaN(rtp) || rtp <= 0 || rtp > 100)
    errors.push(markError(rtpEl, 'Target RTP must be > 0 and ≤ 100'));
  if (isNaN(rtpD) || rtpD < 0 || rtpD >= rtp)
    errors.push(markError(rtpDEl, 'RTP Delta must be ≥ 0 and < Target RTP'));
  if (isNaN(hr) || hr <= 0 || hr > 100)
    errors.push(markError(hrEl, 'Target Hit Rate must be > 0 and ≤ 100'));
  if (isNaN(hrD) || hrD < 0 || hrD >= hr)
    errors.push(markError(hrDEl, 'Hit Rate Delta must be ≥ 0 and < Target Hit Rate'));
  if (isNaN(mp) || mp < 0)
    errors.push(markError(mpEl, 'Max Payout must be ≥ 0 (0 = uncapped)'));

  const VALID_VOLATILITIES = ['LOW','CASUAL','HIGH','VERY_HIGH','EXTREME','ULTRA_EXTREME'];
  if (!volEl.value.trim() || !VALID_VOLATILITIES.includes(volEl.value))
    errors.push(markError(volEl, 'Target Volatility is required'));

  const VALID_STRATEGIES = ['LTR','RTL','BW','SL','ADJ','WAYS','MEGAWAYS','SCATTERS','CLUSTERS'];
  if (!VALID_STRATEGIES.includes(stratEl.value))
    errors.push(markError(stratEl, 'Invalid strategy'));

  // Symbols
  const symRows = document.querySelectorAll('#ai-sym-list .ai-sym-row');
  if (symRows.length < 2)
    errors.push('At least 2 symbols are required');
  const symIds = new Set();
  symRows.forEach((row, i) => {
    const idInp = row.querySelector('input[type="number"]');
    const id = parseInt(idInp ? idInp.value : '');
    if (!idInp || isNaN(id) || id < 1 || id > 99)
      errors.push(markError(idInp, 'Symbol #' + (i+1) + ': ID must be 1–99'));
    else if (symIds.has(id))
      errors.push(markError(idInp, 'Symbol #' + (i+1) + ': duplicate ID ' + id));
    else
      symIds.add(id);
  });

  // Line defs (only for line-based strategies)
  if (AI_LINE_STRATEGIES.has(stratEl.value)) {
    const lineInputs = document.querySelectorAll('#ai-lines-list .ai-line-input');
    if (lineInputs.length === 0) {
      errors.push('Line definitions are required for ' + stratEl.value + ' strategy — use Fill or + Add payline');
    } else {
      const wVal = parseInt(widthEl.value) || 5;
      lineInputs.forEach((inp, i) => {
        const nums = inp.value.trim().split(/[\s,]+/).map(Number).filter(n => !isNaN(n));
        if (nums.length === 0) {
          errors.push(markError(inp, 'Line #' + (i+1) + ': cannot be empty'));
        } else if (nums.length !== wVal) {
          errors.push(markError(inp, 'Line #' + (i+1) + ': must have ' + wVal + ' values (one per reel)'));
        } else {
          const hVal = parseInt(heightEl.value) || 3;
          nums.forEach((n, ri) => {
            if (n < 0 || n >= hVal)
              errors.push(markError(inp, 'Line #' + (i+1) + ' reel ' + (ri+1) + ': row index must be 0–' + (hVal-1)));
          });
        }
      });
    }
  }

  return errors;
}

function _aiCompactJson(obj) {
  // Custom serializer: collapse leaf arrays (no nested arrays/objects) and leaf objects to one line
  function serialize(val, depth) {
    const pad  = '  '.repeat(depth);
    const pad1 = '  '.repeat(depth + 1);
    if (val === null || typeof val !== 'object') return JSON.stringify(val);
    if (Array.isArray(val)) {
      if (val.length === 0) return '[]';
      const isLeaf = val.every(v => v === null || typeof v !== 'object');
      if (isLeaf) return '[ ' + val.map(v => JSON.stringify(v)).join(', ') + ' ]';
      // array of objects or arrays — each element on its own line
      return '[\n' + val.map(v => pad1 + serialize(v, depth + 1)).join(',\n') + '\n' + pad + ']';
    }
    // plain object
    const entries = Object.entries(val);
    if (entries.length === 0) return '{}';
    const isLeaf = entries.every(([, v]) => v === null || typeof v !== 'object');
    if (isLeaf) return '{ ' + entries.map(([k, v]) => JSON.stringify(k) + ': ' + JSON.stringify(v)).join(', ') + ' }';
    return '{\n' + entries.map(([k, v]) => pad1 + JSON.stringify(k) + ': ' + serialize(v, depth + 1)).join(',\n') + '\n' + pad + '}';
  }
  return serialize(obj, 0);
}

function previewAiRequest() {
  const errors = validateAiRequest();
  if (errors.length) {
    showToast(errors[0], true);
    _aiShowMessage(
      `<span style="font-size:0.72rem;color:var(--error);display:block;margin-bottom:0.4rem">Validation errors:</span><ul style="margin:0;padding-left:1.2rem;font-size:0.72rem;color:var(--error)">${errors.map(e => `<li>${e}</li>`).join('')}</ul>`,
      true
    );
    return;
  }
  const req = collectAiRequest();
  _aiShowMessage(`<span style="font-size:0.72rem;color:var(--text2);opacity:0.7;display:block;margin-bottom:0.4rem">Request payload (preview — not sent):</span><pre style="font-size:0.72rem;color:var(--text2);white-space:pre-wrap;margin:0">${_aiCompactJson(req)}</pre>`);
}
let _aiPollInterval  = null;
let _aiOutputEditor  = null;
let _aiExecutionId   = null;
let _aiPollInFlight  = false;

function _aiSetRunning(running) {
  document.getElementById('ai-run-btn').disabled  = running;
  document.getElementById('ai-stop-btn').style.display = running ? '' : 'none';
  const previewBtn = document.getElementById('ai-preview-btn');
  if (previewBtn) previewBtn.disabled = running;
}

function _aiShowMessage(html, isError) {
  const body = document.getElementById('ai-output-body');
  const cmWrap = document.getElementById('ai-output-cm-wrap');
  const copyBtn = document.getElementById('ai-copy-btn');
  const infoBtn = document.getElementById('ai-info-btn');
  body.style.display = '';
  cmWrap.style.display = 'none';
  if (copyBtn) copyBtn.style.display = 'none';
  if (infoBtn) infoBtn.style.display = 'none';
  body.innerHTML = html;
  if (isError) body.style.color = 'var(--error)';
  else body.style.color = '';
}

function _aiShowEditor(json) {
  const body = document.getElementById('ai-output-body');
  const cmWrap = document.getElementById('ai-output-cm-wrap');
  const copyBtn = document.getElementById('ai-copy-btn');
  const infoBtn = document.getElementById('ai-info-btn');

  body.style.display = 'none';
  cmWrap.style.display = 'flex';
  if (copyBtn) copyBtn.style.display = '';
  if (infoBtn) infoBtn.style.display = '';

  const pretty = _aiCompactJson(json);

  if (_aiOutputEditor) {
    _aiOutputEditor.setValue(pretty);
    _aiOutputEditor.refresh();
  } else {
    _aiOutputEditor = CodeMirror.fromTextArea(document.getElementById('ai-output-cm'), {
      mode: 'application/json',
      theme: document.documentElement.getAttribute('data-theme') === 'dark' ? 'material-darker' : 'default',
      lineNumbers: true,
      foldGutter: true,
      gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
      matchBrackets: true,
      autoCloseBrackets: true,
      lineWrapping: false,
      readOnly: false
    });
    _aiOutputEditor.setValue(pretty);
    // fit the editor to the container
    _aiOutputEditor.setSize('100%', '100%');
  }
}

function copyAiOutput() {
  const text = _aiOutputEditor ? _aiOutputEditor.getValue()
    : document.getElementById('ai-output-body').innerText;
  navigator.clipboard.writeText(text).then(() => showToast('Copied to clipboard'));
}

async function runAiAgent() {
  const errors = validateAiRequest();
  if (errors.length) {
    showToast(errors[0], true);
    _aiShowMessage(
      `<span style="font-size:0.72rem;color:var(--error);display:block;margin-bottom:0.4rem">Validation errors:</span>` +
      `<ul style="margin:0;padding-left:1.2rem;font-size:0.72rem;color:var(--error)">${errors.map(e => `<li>${e}</li>`).join('')}</ul>`,
      true
    );
    return;
  }

  const req = collectAiRequest();
  _aiExecutionId = null; // clear any stale id from a prior run before starting a new one
  _aiSetRunning(true);
  _aiShowMessage('<span class="ai-output-empty">Submitting…</span>');
  const iterLogEl = document.getElementById('ai-iter-log');
  if (iterLogEl) { iterLogEl.style.display = 'none'; iterLogEl.innerHTML = ''; }

  try {
    const res = await fetch('/api/agent/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req)
    });
    const data = await res.json();
    if (!res.ok) {
      _aiSetRunning(false);
      _aiShowMessage(
        `<span style="font-size:0.72rem">Error ${res.status}</span><pre style="font-size:0.72rem;white-space:pre-wrap">${_aiEsc(JSON.stringify(data, null, 2))}</pre>`,
        true
      );
      return;
    }
    _aiExecutionId = data.executionId;
    _aiShowMessage(`<span class="ai-output-empty">Running… (id: ${_aiExecutionId})</span>`);
    _aiPollInterval = setInterval(_aiPoll, 800);
  } catch (e) {
    _aiSetRunning(false);
    _aiShowMessage(`<span style="font-size:0.72rem">Network error: ${_aiEsc(e.message)}</span>`, true);
  }
}

async function _aiPoll() {
  if (!_aiExecutionId) return;
  if (_aiPollInFlight) return; // a previous poll is still running — skip this tick
  _aiPollInFlight = true;
  try {
    const res = await fetch(`/api/agent/executions/${_aiExecutionId}`);
    if (!res.ok) return;
    const data = await res.json();
    const status = data.status;

    // Always render iteration logs if present (works during RUNNING and terminal)
    if (data.iterations && data.iterations.length > 0) {
      _aiRenderIterations(data.iterations, status, data.initialPlan, data.initialState);
      // Keep status message visible in body while running
      if (status === 'RUNNING' && data.statusMessage) {
        _aiShowMessage(`<span class="ai-output-empty" style="font-size:0.7rem">${_aiEsc(data.statusMessage)}</span>`);
      }
    } else if (status === 'RUNNING') {
      const msg = data.statusMessage || '⏳ Starting…';
      _aiShowMessage(`<span class="ai-output-empty">${_aiEsc(msg)}</span>`);
    }

    if (status === 'RUNNING') return; // still going

    // terminal state
    clearInterval(_aiPollInterval);
    _aiPollInterval = null;
    _aiSetRunning(false);

    if (status === 'COMPLETED' && data.result) {
      try {
        _aiShowEditor(JSON.parse(data.result));
      } catch {
        _aiShowMessage(`<pre style="font-size:0.72rem;white-space:pre-wrap">${_aiEsc(data.result)}</pre>`);
      }
    } else if (status === 'CANCELLED') {
      _aiShowMessage('<span class="ai-output-empty" style="color:var(--text2)">Agent cancelled.</span>');
    } else {
      _aiShowMessage(
        `<span style="font-size:0.72rem;color:var(--error)">Status: ${_aiEsc(status)}</span>` +
        (data.error ? `<pre style="font-size:0.72rem;white-space:pre-wrap;color:var(--error)">${_aiEsc(data.error)}</pre>` : ''),
        true
      );
    }
  } catch { /* network hiccup — keep polling */ }
  finally { _aiPollInFlight = false; }
}

function _aiRenderIterations(iterations, status, initialPlan, initialState) {
  const iterLog = document.getElementById('ai-iter-log');
  if (!iterLog) return;
  iterLog.style.display = '';
  const running = status === 'RUNNING';
  const last = iterations[iterations.length - 1];
  let html = `<div style="font-size:0.7rem;padding:0.4rem 0">`;

  // ── Initial simulation state ────────────────────────────────────────────
  if (initialState && typeof initialState === 'object') {
    const scalarKeys = ['screenWidth','screenHeight','minMatch','symsPerReel','targetVolatility','winVecDecay','noWinSetCount','winSetCount'];
    const scalarPairs = scalarKeys
      .filter(k => initialState[k] !== undefined)
      .map(k => `<span style="margin-right:10px"><span style="color:var(--text2)">${_aiEsc(k)}:</span> <strong>${_aiEsc(String(initialState[k]))}</strong></span>`)
      .join('');
    const weightsJson = initialState.weights ? JSON.stringify(initialState.weights) : null;
    const paytableJson = initialState.paytable ? JSON.stringify(initialState.paytable, null, 2) : null;
    const detailId = 'ai-init-state-detail';
    html += `<div style="padding:4px 6px;margin-bottom:6px;background:var(--surface2,var(--border));border-radius:4px;font-size:0.67rem">
      <div style="display:flex;align-items:center;gap:6px;line-height:1.8;flex-wrap:wrap">
        <span style="color:var(--text2);font-size:0.63rem;font-weight:600;text-transform:uppercase;letter-spacing:0.04em;flex-shrink:0">Initial state</span>
        ${scalarPairs}
        <button class="icon-btn" style="width:auto;height:16px;padding:0 5px;font-size:0.6rem;flex-shrink:0" onclick="(function(btn){var d=document.getElementById('${detailId}');var open=d.style.display!=='none';d.style.display=open?'none':'block';btn.textContent=open?'▶ weights/paytable':'▼ weights/paytable';})(this)">▶ weights/paytable</button>
      </div>
      <div id="${detailId}" style="display:none;margin-top:4px">
        ${weightsJson ? `<div style="margin-bottom:4px"><span style="color:var(--text2)">weights:</span> <code style="font-size:0.65rem;word-break:break-all">${_aiEsc(weightsJson)}</code></div>` : ''}
        ${paytableJson ? `<div><span style="color:var(--text2)">paytable:</span><pre style="margin:2px 0 0;font-size:0.63rem;line-height:1.4;overflow-x:auto;white-space:pre-wrap">${_aiEsc(paytableJson)}</pre></div>` : ''}
      </div>
    </div>`;
  }

  if (running) {
    html += `<div style="color:var(--text2);margin-bottom:0.35rem">⏳ Iteration <strong>${last.iteration}</strong> / ${last.maxIterations} running…</div>`;
  }
  html += `<table style="width:100%;border-collapse:collapse;font-size:0.68rem;table-layout:fixed">`;
  html += `<thead><tr style="color:var(--text2);border-bottom:1px solid var(--border)">
    <th style="text-align:left;padding:2px 5px;width:2.5rem">#</th>
    <th style="text-align:right;padding:2px 5px;width:4rem">RTP %</th>
    <th style="text-align:right;padding:2px 5px;width:4rem">HR %</th>
    <th style="text-align:right;padding:2px 5px;width:4.5rem">Std Dev</th>
    <th style="text-align:center;padding:2px 5px;width:3.5rem">Combo</th>
    <th style="text-align:left;padding:2px 5px">Agent tools</th>
  </tr></thead><tbody>`;
  _aiPatchMap = {};
  _aiDistMap = {};
  for (const it of iterations) {
    const rowStyle = it.converged ? 'color:var(--success,#4caf50)' : '';
    const patchKeys = it.llmPatch
      ? (it.llmPatch.toolCalls ? it.llmPatch.toolCalls.join(', ') : Object.keys(it.llmPatch).filter(k => k !== 'totalCalls' && k !== 'toolCallDetails').join(', ')) || '—'
      : '—';
    if (it.llmPatch) _aiPatchMap[it.iteration] = JSON.stringify(it.llmPatch, null, 2);
    const hasDist = it.hitDistribution && Object.keys(it.hitDistribution).length > 0;
    if (hasDist) _aiDistMap[it.iteration] = it.hitDistribution;
    // Every iteration (including #0 plan) has an LLM call → always show 📋
    const debugBtn = `<button class="icon-btn" title="Inspect LLM prompt+response" style="width:auto;height:18px;padding:0 4px;font-size:0.65rem;flex-shrink:0" onclick="_aiShowDebug('${_aiExecutionId}',${it.iteration})">📋</button>`;
    const patchCell = it.iteration === 0
      ? `<div style="display:flex;align-items:center;gap:3px;min-width:0">
           <span style="color:var(--text2);font-size:0.65rem;flex:1">initial plan</span>${debugBtn}
         </div>`
      : it.llmPatch
        ? `<div style="display:flex;align-items:center;gap:3px;min-width:0">
             <button class="icon-btn" style="width:auto;height:auto;padding:2px 5px;font-size:0.65rem;text-align:left;border-bottom:1px dashed var(--text2);border-radius:0;max-width:100%;white-space:normal;word-break:break-word;flex:1;min-width:0" title="${_aiEsc(patchKeys)}" onclick="_aiShowPatch(${it.iteration})">${_aiEsc(patchKeys)}</button>${debugBtn}
           </div>`
        : `<div style="display:flex;align-items:center;gap:3px;min-width:0"><span style="color:var(--text2);flex:1">—</span>${debugBtn}</div>`;
    const stdDev = (typeof it.stdDev === 'number') ? it.stdDev.toFixed(2) : '—';
    const comboCell = hasDist
      ? `<button class="icon-btn" title="Reveal win-combo distribution" style="width:auto;height:18px;padding:0 5px;font-size:0.7rem" onclick="_aiShowDist(${it.iteration})">📊</button>`
      : `<span style="color:var(--text2)">—</span>`;
    html += `<tr style="${rowStyle};border-bottom:1px solid var(--border)">
      <td style="padding:2px 5px;font-weight:600">${it.converged ? '✓ ' : ''}${it.iteration}</td>
      <td style="text-align:right;padding:2px 5px">${it.rtp.toFixed(2)}</td>
      <td style="text-align:right;padding:2px 5px">${it.hitRate.toFixed(2)}</td>
      <td style="text-align:right;padding:2px 5px">${stdDev}</td>
      <td style="text-align:center;padding:2px 5px">${comboCell}</td>
      <td style="padding:2px 5px">${patchCell}</td>
    </tr>`;
  }
  html += `</tbody></table></div>`;
  iterLog.innerHTML = html;
  // scroll to bottom to show latest
  iterLog.scrollTop = iterLog.scrollHeight;
}

let _aiDebugData = null;
let _aiPatchMap = {};
let _aiDistMap = {};
let _aiDebugReqToken = 0;

const _AI_PATCH_KEY_DOCS = {
  weights: 'Probability of picking each reel set on a spin (win sets + no-win sets). Values sum to ~1.0; raising a win set’s share pushes RTP up, raising a no-win set’s share pushes it down.',
  paytable: 'Payout multipliers per symbol, keyed as symbolId → { matchCount: multiplier }. A value of 4.0 at matchCount 3 means "3-in-a-row of this symbol pays 4× the bet". Bigger multipliers = higher RTP and volatility.',
  winVecDecay: 'How fast a symbol thins out along a winning strip (0.40–0.95). Lower = symbols cluster more → longer/bigger wins → higher RTP & volatility. Changing it rebuilds the win strips.',
  symsPerReel: 'Total tiles per reel (64–512). More tiles = finer probability control but rarer exact combos. Changing it rebuilds the strips.',
  targetVolatility: 'Volatility band (LOW … ULTRA_EXTREME). Changing it triggers a full rebuild of paytable + strips to match the new risk profile.',
  seed: 'RNG seed for the next simulation — changes which random spins are drawn, not the math. Not shown in the summary column.',
  maxIterations: 'Hard cap on loop iterations (set in the UI, not tunable by the LLM). Not shown in the summary column.',
  error: 'The LLM call failed this iteration; the message is the raw error. No patch was applied.'
};

function _aiPatchLegend(patchJson) {
  let keys = [];
  try { keys = Object.keys(JSON.parse(patchJson)); } catch (e) { return ''; }
  const rows = keys.map(k => {
    const doc = _AI_PATCH_KEY_DOCS[k] || 'Unknown key.';
    return `<div style="padding:5px 0;border-bottom:1px solid var(--border)">
              <code style="color:var(--accent);font-weight:600">${_aiEsc(k)}</code>
              <div style="color:var(--text2);margin-top:2px">${_aiEsc(doc)}</div>
            </div>`;
  }).join('');
  return `
    <div style="padding:0.5rem 1rem 0.7rem;border-bottom:1px solid var(--border);flex-shrink:0">
      <div style="color:var(--text2);margin-bottom:6px">
        A <strong>patch</strong> is the minimal set of changes the AI made this iteration — only the keys below were touched.
        It was chosen <em>after</em> simulating the previous strips, then applied and re-simulated on the next pass.
      </div>
      ${rows}
    </div>`;
}

function _aiShowPatch(iteration) {
  const patch = _aiPatchMap[iteration];
  if (!patch) return;
  document.getElementById('ai-patch-modal')?.remove();

  let patchObj = {};
  try { patchObj = JSON.parse(patch); } catch (e) {}

  const details = patchObj.toolCallDetails || [];
  const totalCalls = patchObj.totalCalls || details.length;

  // Build tool call timeline
  const TOOL_COLORS = {
    runSimulation:  '#4caf50',
    tuneWeights:    '#2196f3',
    patchPaytable:  '#ff9800',
    rebuildStrips:  '#e91e63',
    getTargets:     '#9e9e9e',
    getCurrentState:'#9e9e9e'
  };

  let timelineHtml = '';
  if (details.length > 0) {
    timelineHtml = details.map(d => {
      const color = TOOL_COLORS[d.tool] || '#9e9e9e';
      const isOk  = String(d.result || '').startsWith('OK');
      const isErr = String(d.result || '').startsWith('ERROR');
      const resColor = isErr ? 'var(--danger,#f44336)' : isOk ? 'var(--success,#4caf50)' : 'var(--text2)';
      const argsHtml = d.args && d.args.trim() && d.args !== '""'
        ? `<pre style="margin:3px 0 0 0;padding:3px 6px;background:var(--bg);border-radius:4px;font-size:0.67rem;white-space:pre-wrap;word-break:break-word;color:var(--text);max-height:6rem;overflow:auto">${_aiEsc(d.args)}</pre>`
        : '';
      return `<div style="display:flex;gap:8px;padding:5px 0;border-bottom:1px solid var(--border)">
        <div style="flex-shrink:0;width:1.5rem;text-align:right;color:var(--text2);font-size:0.65rem;padding-top:2px">#${d.seq}</div>
        <div style="flex:1;min-width:0">
          <div style="display:flex;align-items:center;gap:5px">
            <span style="background:${color};color:#fff;border-radius:3px;padding:1px 5px;font-size:0.65rem;font-weight:600;flex-shrink:0">${_aiEsc(d.tool)}</span>
            <span style="font-size:0.67rem;color:${resColor};overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${_aiEsc(d.result)}">${_aiEsc(d.result)}</span>
          </div>
          ${argsHtml}
        </div>
      </div>`;
    }).join('');
  } else if (patchObj.toolCalls) {
    // Fallback: only names available
    timelineHtml = patchObj.toolCalls.map((t, i) => {
      const color = TOOL_COLORS[t] || '#9e9e9e';
      return `<div style="display:flex;gap:8px;padding:4px 0;border-bottom:1px solid var(--border)">
        <div style="flex-shrink:0;width:1.5rem;text-align:right;color:var(--text2);font-size:0.65rem;padding-top:2px">#${i+1}</div>
        <span style="background:${color};color:#fff;border-radius:3px;padding:1px 5px;font-size:0.65rem;font-weight:600">${_aiEsc(t)}</span>
      </div>`;
    }).join('');
  } else {
    timelineHtml = `<div style="color:var(--text2);font-size:0.73rem;padding:8px 0">No tool call details available.</div>`;
  }

  const modal = document.createElement('div');
  modal.id = 'ai-patch-modal';
  modal.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.55);z-index:9999;display:flex;align-items:center;justify-content:center';
  modal.innerHTML = `
    <div style="background:var(--surface);border:1px solid var(--border);border-radius:8px;width:min(80vw,640px);max-height:75vh;display:flex;flex-direction:column;overflow:hidden">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:0.5rem 0.9rem;border-bottom:1px solid var(--border);flex-shrink:0">
        <span style="font-size:0.78rem;font-weight:600;color:var(--text)">Agent Tool Calls — Iteration ${iteration} <span style="font-weight:400;color:var(--text2)">(${totalCalls} calls)</span></span>
        <button class="icon-btn danger" onclick="document.getElementById('ai-patch-modal').remove()" title="Close">✕</button>
      </div>
      <div style="padding:0.3rem 1rem 0.15rem;font-size:0.67rem;color:var(--text2);flex-shrink:0">
        Each row is one tool call. The agent calls these autonomously in a ReAct loop until convergence.
      </div>
      <div style="overflow:auto;flex:1;padding:0 1rem 0.8rem">
        ${timelineHtml}
      </div>
    </div>`;
  modal.addEventListener('click', e => { if (e.target === modal) modal.remove(); });
  document.body.appendChild(modal);
}

function _aiShowIterInfo() {
  document.getElementById('ai-iter-info-modal')?.remove();
  const modal = document.createElement('div');
  modal.id = 'ai-iter-info-modal';
  modal.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.55);z-index:9999;display:flex;align-items:center;justify-content:center';

  const sections = [
    {
      col: '#',
      src: 'Java: <code>IterationLog.iteration</code>',
      desc: 'The loop counter. Row <strong>#0</strong> is the initial simulation of the LLM-planned paytable and weights — before any iterate patch is applied. Rows #1+ are the tune-loop iterations. A ✓ prefix means that iteration reached the target RTP and hit-rate within tolerance — the run converged and stopped early.'
    },
    {
      col: 'RTP %',
      src: 'Java: <code>IterationLog.rtp</code> ← <code>SimStats.rtp()</code>',
      desc: 'Return-to-Player measured by the simulation on this iteration\'s reel strips. Computed as total payout ÷ total bet across <em>spinsPerIter</em> random spins (default 1,000,000). This is the actual RTP of the current strips, not the target. Green row = within target ± delta.'
    },
    {
      col: 'HR %',
      src: 'Java: <code>IterationLog.hitRate</code> ← <code>SimStats.hitRate()</code>',
      desc: 'Hit-rate: percentage of spins that produced at least one winning line. Also measured from the same simulation run. A hit-rate of 30% means 1 in ~3 spins returns something.'
    },
    {
      col: 'Std Dev',
      src: 'Java: <code>IterationLog.stdDev</code> ← <code>SimStats.stdDev()</code>',
      desc: 'Standard deviation of the per-spin payout distribution, expressed as a multiple of the bet. Higher = more volatile (bigger swings). A value of 5.0 means typical per-spin payout varies by ±5× the bet around the mean. Computed as √(variance) from the same simulation batch.'
    },
    {
      col: 'Combo 📊',
      src: 'Java: <code>IterationLog.hitDistribution</code> ← <code>SimStats.hitDistribution()</code>',
      desc: 'Per-symbol, per-match-length win breakdown from the simulation. Click 📊 to expand. Shows for each symbol how often each length (3-of-a-kind, 4-of-a-kind, …) hit, as a percentage of all spins and raw count. Useful for spotting if a single combo dominates RTP. "—" means no wins were recorded.'
    },
    {
      col: 'Agent tools',
      src: 'Java: <code>IterationLog.llmPatch.toolCallDetails</code> ← <code>LtrTuningTools</code> @Tool calls via LangChain4j AiServices',
      desc: `The sequence of tool calls the agent made in this iteration's ReAct loop. Click the cell to see a timeline — each call shows the tool name, the arguments passed, and the result returned.
             <br><br>
             <strong>How it works:</strong> one <code>TuningAgent</code> instance is created per outer iteration, wired to a stateful <code>LtrTuningTools</code> that holds the live <code>MutableState</code>. The agent calls tools autonomously until it decides convergence is reached or it has exhausted its reasoning budget (~8 cycles).
             <br><br>
             <strong>Available tools and what they do:</strong>
             <ul style="margin:6px 0 0 0;padding-left:1.2em">
               <li><code style="color:#4caf50">runSimulation</code> — runs N spins and returns rtp, hitRate, maxWin, volatilityLabel. The agent calls this after every adjustment to verify the effect.</li>
               <li><code style="color:#2196f3">tuneWeights</code> — replaces the full reel-set weight array. Values are normalised to sum=1.0. Raising win-set weights → higher RTP; raising no-win weights → lower RTP / lower hit-rate.</li>
               <li><code style="color:#ff9800">patchPaytable</code> — updates multipliers for specific symbols: <code>{ symbolId: { matchCount: multiplier } }</code>. Each value is the new absolute payout for that combo. Only changed symbols/lengths need to be included.</li>
               <li><code style="color:#e91e63">rebuildStrips</code> — triggers a full reel strip rebuild with new symsPerReel, winVecDecay, or targetVolatility. Expensive — agent uses this only when weights+paytable adjustments are stuck.</li>
               <li><code style="color:#9e9e9e">getTargets</code> / <code style="color:#9e9e9e">getCurrentState</code> — read-only tools the agent uses to inspect current state before deciding what to change.</li>
             </ul>
             <br>
             <strong>"—"</strong> means iteration 0 (initial plan), or the LLM was unavailable, or no tool calls were recorded.`
    }
  ];

  let rows = sections.map(s => `
    <div style="padding:8px 0;border-bottom:1px solid var(--border)">
      <div style="display:flex;align-items:baseline;gap:8px;margin-bottom:3px">
        <code style="color:var(--accent);font-weight:700;font-size:0.78rem">${s.col}</code>
        <span style="color:var(--text2);font-size:0.65rem">${s.src}</span>
      </div>
      <div style="color:var(--text);font-size:0.7rem;line-height:1.5">${s.desc}</div>
    </div>`).join('');

  modal.innerHTML = `
    <div style="background:var(--surface);border:1px solid var(--border);border-radius:8px;width:min(90vw,680px);max-height:80vh;display:flex;flex-direction:column;overflow:hidden">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:0.5rem 0.9rem;border-bottom:1px solid var(--border);flex-shrink:0">
        <span style="font-size:0.78rem;font-weight:600;color:var(--text)">Agent output — column guide</span>
        <button class="icon-btn danger" onclick="document.getElementById('ai-iter-info-modal').remove()" title="Close">✕</button>
      </div>
      <div style="padding:0.5rem 1rem 1rem;overflow:auto;flex:1">${rows}</div>
    </div>`;
  modal.addEventListener('click', e => { if (e.target === modal) modal.remove(); });
  document.body.appendChild(modal);
}

function _aiShowDist(iteration) {
  const dist = _aiDistMap[iteration];
  if (!dist) return;
  document.getElementById('ai-dist-modal')?.remove();

  let rows = '';
  for (const [symLabel, byCount] of Object.entries(dist)) {
    const counts = Object.entries(byCount)
      .map(([cnt, v]) => {
        const pct = (v && typeof v.hitRatePct === 'number') ? v.hitRatePct : 0;
        const hits = (v && typeof v.hits === 'number') ? v.hits : 0;
        return `<span style="display:inline-block;margin:1px 6px 1px 0;white-space:nowrap">
                  <strong>${_aiEsc(cnt)}</strong>: ${pct.toFixed(4)}%<span style="color:var(--text2)"> (${hits})</span>
                </span>`;
      })
      .join('');
    rows += `<div style="padding:4px 0;border-bottom:1px solid var(--border)">
               <div style="font-weight:600;color:var(--text);margin-bottom:2px">${_aiEsc(symLabel)}</div>
               <div style="font-size:0.7rem">${counts || '<span style="color:var(--text2)">—</span>'}</div>
             </div>`;
  }

  const modal = document.createElement('div');
  modal.id = 'ai-dist-modal';
  modal.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.55);z-index:9999;display:flex;align-items:center;justify-content:center';
  modal.innerHTML = `
    <div style="background:var(--surface);border:1px solid var(--border);border-radius:8px;width:min(80vw,640px);max-height:70vh;display:flex;flex-direction:column;overflow:hidden">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:0.5rem 0.9rem;border-bottom:1px solid var(--border);flex-shrink:0">
        <span style="font-size:0.78rem;font-weight:600;color:var(--text)">Win-combo distribution — Iteration ${iteration}</span>
        <button class="icon-btn danger" onclick="document.getElementById('ai-dist-modal').remove()" title="Close">✕</button>
      </div>
      <div style="margin:0;padding:0.6rem 1rem;overflow:auto;flex:1;color:var(--text)">${rows || '<span style="color:var(--text2)">No distribution data.</span>'}</div>
    </div>`;
  modal.addEventListener('click', e => { if (e.target === modal) modal.remove(); });
  document.body.appendChild(modal);
}

async function _aiShowDebug(executionId, iteration) {
  let modal = document.getElementById('ai-debug-modal');
  if (!modal) {
    modal = document.createElement('div');
    modal.id = 'ai-debug-modal';
    modal.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.65);z-index:9999;display:flex;align-items:center;justify-content:center';
    modal.innerHTML = `
      <div style="background:var(--surface);border:1px solid var(--border);border-radius:8px;width:min(96vw,1100px);height:min(92vh,860px);display:flex;flex-direction:column;overflow:hidden">
        <div style="display:flex;align-items:center;justify-content:space-between;padding:0.6rem 0.9rem;border-bottom:1px solid var(--border);flex-shrink:0">
          <span id="ai-debug-title" style="font-size:0.8rem;font-weight:600;color:var(--text)"></span>
          <button class="icon-btn danger" onclick="document.getElementById('ai-debug-modal').remove()" title="Close">✕</button>
        </div>
        <div style="padding:0.5rem 0.9rem;border-bottom:1px solid var(--border);display:flex;gap:0.5rem;flex-shrink:0">
          <button id="ai-debug-tab-prompt" class="icon-btn" style="width:auto;height:auto;padding:0.25rem 0.7rem;font-size:0.72rem" onclick="_aiDebugTab('prompt')">Prompt</button>
          <button id="ai-debug-tab-response" class="icon-btn" style="width:auto;height:auto;padding:0.25rem 0.7rem;font-size:0.72rem" onclick="_aiDebugTab('response')">Response</button>
        </div>
        <pre id="ai-debug-body" style="margin:0;padding:0.8rem 1rem;font-size:0.73rem;line-height:1.5;overflow:auto;flex:1;white-space:pre-wrap;word-break:break-word;color:var(--text);tab-size:2">Loading…</pre>
      </div>`;
    modal.addEventListener('click', e => { if (e.target === modal) modal.remove(); });
    document.body.appendChild(modal);
  }

  document.getElementById('ai-debug-title').textContent = `LLM Debug — Iteration ${iteration}`;
  document.getElementById('ai-debug-body').textContent = 'Loading…';
  modal.style.display = 'flex';
  _aiDebugData = null;
  const reqToken = ++_aiDebugReqToken;

  try {
    const res = await fetch(`/api/agent/executions/${executionId}/debug/${iteration}`);
    if (reqToken !== _aiDebugReqToken) return; // a newer open superseded this one — ignore
    if (!res.ok) { document.getElementById('ai-debug-body').textContent = 'Debug file not found.'; return; }
    const parsed = await res.json();
    if (reqToken !== _aiDebugReqToken) return; // superseded while awaiting body
    _aiDebugData = parsed;
    _aiDebugTab('prompt');
  } catch (e) {
    if (reqToken !== _aiDebugReqToken) return;
    document.getElementById('ai-debug-body').textContent = 'Error: ' + e.message;
  }
}

function _aiDebugTab(tab) {
  if (!_aiDebugData) return;
  const body = document.getElementById('ai-debug-body');
  const promptBtn = document.getElementById('ai-debug-tab-prompt');
  const responseBtn = document.getElementById('ai-debug-tab-response');
  if (tab === 'prompt') {
    body.textContent = _aiDebugData.prompt || '(empty)';
    promptBtn.style.background = 'var(--accent,#6c63ff)';  promptBtn.style.color = '#fff';
    responseBtn.style.background = '';  responseBtn.style.color = '';
  } else {
    let responseText = _aiDebugData.response || '(empty)';
    try { responseText = JSON.stringify(JSON.parse(responseText), null, 2); } catch { /* not JSON */ }
    body.textContent = responseText;
    responseBtn.style.background = 'var(--accent,#6c63ff)';  responseBtn.style.color = '#fff';
    promptBtn.style.background = '';  promptBtn.style.color = '';
  }
}

async function cancelAiAgent() {
  if (!_aiExecutionId) return;
  clearInterval(_aiPollInterval);
  _aiPollInterval = null;
  try {
    await fetch(`/api/agent/executions/${_aiExecutionId}/cancel`, { method: 'POST' });
  } catch { /* fire and forget */ }
  _aiSetRunning(false);
  _aiShowMessage('<span class="ai-output-empty" style="color:var(--text2)">Cancelling…</span>');
  // poll once more after a short delay to pick up CANCELLED status
  setTimeout(_aiPoll, 800);
}
