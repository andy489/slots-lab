/* ── AI Agent Tab ── */

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
    reelConfig:       null,
    parameters: {
      strategy:         document.getElementById('ai-strategy').value,
      screenWidth:      parseInt(document.getElementById('ai-screen-width').value) || 5,
      screenHeight:     parseInt(document.getElementById('ai-screen-height').value) || 3,
      minMatch:         parseInt(document.getElementById('ai-min-match').value) || 3,
      symsPerReel:      parseInt(document.getElementById('ai-syms-per-reel').value) || 256,
      symsPerReelDelta: parseInt(document.getElementById('ai-syms-per-reel-delta').value) || 16,
      maxAttempts:      parseInt(document.getElementById('ai-max-attempts').value) || 3,
      maxIterations:    parseInt(document.getElementById('ai-max-iterations').value) || 80,
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
  const maEl     = document.getElementById('ai-max-attempts');
  const miEl     = document.getElementById('ai-max-iterations');
  const rtpEl    = document.getElementById('ai-target-rtp');
  const rtpDEl   = document.getElementById('ai-rtp-delta');
  const hrEl     = document.getElementById('ai-target-hit-rate');
  const hrDEl    = document.getElementById('ai-hit-rate-delta');
  const volEl    = document.getElementById('ai-target-volatility');

  const w   = parseInt(widthEl.value);
  const h   = parseInt(heightEl.value);
  const mm  = parseInt(mmEl.value);
  const spr = parseInt(sprEl.value);
  const sprD = parseInt(sprDEl.value);
  const ma  = parseInt(maEl.value);
  const mi  = parseInt(miEl.value);

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
  if (!maEl.value.trim() || isNaN(ma) || ma < 1 || ma > 20)
    errors.push(markError(maEl, 'Max Attempts must be between 1 and 20'));
  if (!miEl.value.trim() || isNaN(mi) || mi < 10 || mi > 500)
    errors.push(markError(miEl, 'Max Iterations must be between 10 and 500'));

  const rtp  = parseFloat(rtpEl.value.replace(',', '.'));
  const rtpD = parseFloat(rtpDEl.value.replace(',', '.'));
  const hr   = parseFloat(hrEl.value.replace(',', '.'));
  const hrD  = parseFloat(hrDEl.value.replace(',', '.'));

  if (isNaN(rtp) || rtp <= 0 || rtp > 100)
    errors.push(markError(rtpEl, 'Target RTP must be > 0 and ≤ 100'));
  if (isNaN(rtpD) || rtpD < 0 || rtpD >= rtp)
    errors.push(markError(rtpDEl, 'RTP Delta must be ≥ 0 and < Target RTP'));
  if (isNaN(hr) || hr <= 0 || hr > 100)
    errors.push(markError(hrEl, 'Target Hit Rate must be > 0 and ≤ 100'));
  if (isNaN(hrD) || hrD < 0 || hrD >= hr)
    errors.push(markError(hrDEl, 'Hit Rate Delta must be ≥ 0 and < Target Hit Rate'));

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
  body.style.display = '';
  cmWrap.style.display = 'none';
  if (copyBtn) copyBtn.style.display = 'none';
  body.innerHTML = html;
  if (isError) body.style.color = 'var(--error)';
  else body.style.color = '';
}

function _aiShowEditor(json) {
  const body = document.getElementById('ai-output-body');
  const cmWrap = document.getElementById('ai-output-cm-wrap');
  const copyBtn = document.getElementById('ai-copy-btn');

  body.style.display = 'none';
  cmWrap.style.display = 'flex';
  if (copyBtn) copyBtn.style.display = '';

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
  _aiSetRunning(true);
  _aiShowMessage('<span class="ai-output-empty">Submitting…</span>');

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
        `<span style="font-size:0.72rem">Error ${res.status}</span><pre style="font-size:0.72rem;white-space:pre-wrap">${JSON.stringify(data, null, 2)}</pre>`,
        true
      );
      return;
    }
    _aiExecutionId = data.executionId;
    _aiShowMessage(`<span class="ai-output-empty">Running… (id: ${_aiExecutionId})</span>`);
    _aiPollInterval = setInterval(_aiPoll, 1500);
  } catch (e) {
    _aiSetRunning(false);
    _aiShowMessage(`<span style="font-size:0.72rem">Network error: ${e.message}</span>`, true);
  }
}

async function _aiPoll() {
  if (!_aiExecutionId) return;
  try {
    const res = await fetch(`/api/agent/executions/${_aiExecutionId}`);
    if (!res.ok) return;
    const data = await res.json();
    const status = data.status;
    if (status === 'RUNNING') return; // still going

    // terminal state
    clearInterval(_aiPollInterval);
    _aiPollInterval = null;
    _aiSetRunning(false);

    if (status === 'COMPLETED' && data.result) {
      try {
        _aiShowEditor(JSON.parse(data.result));
      } catch {
        _aiShowMessage(`<pre style="font-size:0.72rem;white-space:pre-wrap">${data.result}</pre>`);
      }
    } else if (status === 'CANCELLED') {
      _aiShowMessage('<span class="ai-output-empty" style="color:var(--text2)">Agent cancelled.</span>');
    } else {
      _aiShowMessage(
        `<span style="font-size:0.72rem;color:var(--error)">Status: ${status}</span>` +
        (data.error ? `<pre style="font-size:0.72rem;white-space:pre-wrap;color:var(--error)">${data.error}</pre>` : ''),
        true
      );
    }
  } catch { /* network hiccup — keep polling */ }
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
