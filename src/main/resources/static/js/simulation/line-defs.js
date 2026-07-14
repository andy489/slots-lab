/* ── Line definitions and toggle buttons ── */

let _lineCounter = 0;
function addLineDef(defaultValue) {
  const rid = _lineCounter++;
  const container = document.getElementById('rtp-lines-list');
  const row = document.createElement('div');
  row.className = 'rtp-line-row';
  row.id = 'rtp-line-' + rid;
  const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
  const placeholder = Array.from({length: w}, (_, i) => i < 3 ? 1 : 0).join(', ');
  row.innerHTML = `
    <span class="rtp-line-label">#${rid + 1}</span>
    <input type="text" class="rtp-line-input array-input" value="${defaultValue || ''}" placeholder="${placeholder}"/>
    <button class="icon-btn danger" onclick="removeLineDef('rtp-line-${rid}')">
      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M9 6V4h6v2"/></svg>
    </button>
  `;
  container.appendChild(row);
  updateLineCount();
}
function removeLineDef(id) {
  const el = document.getElementById(id);
  if (el) el.remove();
  renumberLines();
}
function renumberLines() {
  document.querySelectorAll('.rtp-line-row .rtp-line-label').forEach((el, i) => {
    el.textContent = '#' + (i + 1);
  });
  updateLineCount();
}
function updateLineCount() {
  const n = document.querySelectorAll('.rtp-line-row').length;
  const btn = document.getElementById('add-payline-btn');
  if (btn) btn.textContent = `+ Add payline (${n})`;
  updateLineDefsToggleBtn();
}

function updateLineDefsToggleBtn() {
  const btn = document.getElementById('linedefs-toggle-btn');
  if (!btn) return;
  const screenInputs = ['rtp-screen-width', 'rtp-screen-height', 'rtp-min-match'].map(id => document.getElementById(id));
  const hasValues = screenInputs.some(inp => inp && inp.value.trim() !== '')
    || Array.from(document.querySelectorAll('.rtp-line-input')).some(inp => inp.value.trim() !== '');
  btn.innerHTML = hasValues
    ? `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`
    : `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>`;
  btn.title = hasValues ? 'Clear all line definitions' : 'Fill with 10 default lines';
  btn.classList.toggle('danger', hasValues);
}

function toggleLineDefs() {
  const lineInputs = document.querySelectorAll('.rtp-line-input');
  const screenIds = ['rtp-screen-width', 'rtp-screen-height', 'rtp-min-match'];
  const screenInputs = screenIds.map(id => document.getElementById(id));
  const hasValues = screenInputs.some(inp => inp && inp.value.trim() !== '')
    || Array.from(lineInputs).some(inp => inp.value.trim() !== '');
  if (hasValues) {
    screenInputs.forEach(inp => { if (inp) inp.value = ''; });
    lineInputs.forEach(inp => { inp.value = ''; });
    updateLineDefsToggleBtn();
  } else {
    screenInputs.forEach(inp => { if (inp) inp.value = inp.placeholder; });
    const DEFAULT_LINES = [
      '0, 0, 0, 0, 0',
      '1, 1, 1, 1, 1',
      '2, 2, 2, 2, 2',
      '0, 1, 0, 1, 0',
      '1, 0, 1, 0, 1',
      '2, 1, 2, 1, 2',
      '1, 2, 1, 2, 1',
      '0, 1, 1, 1, 0',
      '2, 1, 1, 1, 2',
      '0, 2, 0, 2, 0',
    ];
    const container = document.getElementById('rtp-lines-list');
    container.innerHTML = '';
    _lineCounter = 0;
    DEFAULT_LINES.forEach(val => {
      addLineDef('');
      const inputs = container.querySelectorAll('.rtp-line-input');
      const inp = inputs[inputs.length - 1];
      inp.placeholder = val;
      inp.value = val;
    });
    updateLineDefsToggleBtn();
  }
}

function updateScatterDefsToggleBtn() {
  const btn = document.getElementById('scatterdefs-toggle-btn');
  if (!btn) return;
  const screenInputs = ['rtp-screen-width', 'rtp-screen-height', 'rtp-min-match'].map(id => document.getElementById(id));
  const hasValues = screenInputs.some(inp => inp && inp.value.trim() !== '')
    || Array.from(document.querySelectorAll('#interval-sets-container .scatter-from, #interval-sets-container .scatter-to')).some(inp => inp.value.trim() !== '');
  btn.innerHTML = hasValues
    ? `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`
    : `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>`;
  btn.title = hasValues ? 'Clear all values' : 'Fill interval fields with default values';
  btn.classList.toggle('danger', hasValues);
}

function toggleScatterDefs() {
  const screenInputs = ['rtp-screen-width', 'rtp-screen-height', 'rtp-min-match'].map(id => document.getElementById(id));
  const intervalInputs = document.querySelectorAll('#interval-sets-container .scatter-from, #interval-sets-container .scatter-to');
  const hasValues = screenInputs.some(inp => inp && inp.value.trim() !== '')
    || Array.from(intervalInputs).some(inp => inp.value.trim() !== '');
  if (hasValues) {
    screenInputs.forEach(inp => { if (inp) inp.value = ''; });
    intervalInputs.forEach(inp => { inp.value = ''; inp.style.color = ''; inp.style.borderColor = ''; });
  } else {
    screenInputs.forEach(inp => { if (inp) inp.value = inp.placeholder; });
    intervalInputs.forEach(inp => { if (inp.placeholder) inp.value = inp.placeholder; });
  }
  updateScatterDefsToggleBtn();
}

function toggleClusterDefs() {
  const screenInputs = ['rtp-screen-width', 'rtp-screen-height', 'rtp-min-match'].map(id => document.getElementById(id));
  const intervalInputs = document.querySelectorAll('#clusters-interval-sets-container .scatter-from, #clusters-interval-sets-container .scatter-to');
  const hasValues = screenInputs.some(inp => inp && inp.value.trim() !== '')
    || Array.from(intervalInputs).some(inp => inp.value.trim() !== '');
  if (hasValues) {
    screenInputs.forEach(inp => { if (inp) inp.value = ''; });
    intervalInputs.forEach(inp => { inp.value = ''; inp.style.color = ''; inp.style.borderColor = ''; });
  } else {
    screenInputs.forEach(inp => { if (inp) inp.value = inp.placeholder; });
    intervalInputs.forEach(inp => { if (inp.placeholder) inp.value = inp.placeholder; });
  }
  updateScreenDimsToggleBtn();
}

function updateSymConfigToggleBtn() {
  const btn = document.getElementById('symconfig-toggle-btn');
  if (!btn) return;
  const hasValues = Array.from(document.querySelectorAll('#rtp-symbol-rows .rtp-paytable-input')).some(inp => inp.value.trim() !== '');
  btn.innerHTML = hasValues
    ? `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`
    : `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>`;
  btn.title = hasValues ? 'Clear all paytable values' : 'Fill paytable fields with default values';
  btn.classList.toggle('danger', hasValues);
}

function toggleSymConfig() {
  const inputs = document.querySelectorAll('#rtp-symbol-rows .rtp-paytable-input');
  const hasValues = Array.from(inputs).some(inp => inp.value.trim() !== '');
  if (hasValues) {
    inputs.forEach(inp => { if (!inp.disabled) inp.value = ''; });
  } else {
    inputs.forEach(inp => { if (!inp.disabled && inp.placeholder && inp.placeholder !== 'n/a') inp.value = inp.placeholder; });
  }
  updateSymConfigToggleBtn();
}

function updateScreenDimsToggleBtn() {
  const btn = document.getElementById('screendims-toggle-btn');
  if (!btn) return;
  const strat = document.getElementById('rtp-strategy')?.value;
  const ids = ['rtp-screen-width', 'rtp-screen-height', 'rtp-min-match'];
  let hasValues = ids.some(id => { const el = document.getElementById(id); return el && el.value.trim() !== ''; });
  if (strat === 'CLUSTERS') {
    hasValues = hasValues || Array.from(document.querySelectorAll('#clusters-interval-sets-container .scatter-from, #clusters-interval-sets-container .scatter-to')).some(inp => inp.value.trim() !== '');
  }
  btn.innerHTML = hasValues
    ? `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`
    : `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>`;
  btn.title = hasValues ? 'Clear screen dimension fields' : 'Fill screen dimension fields with default values';
  btn.classList.toggle('danger', hasValues);
}

function toggleScreenDims() {
  const strat = document.getElementById('rtp-strategy')?.value;
  if (strat === 'CLUSTERS') {
    toggleClusterDefs();
    return;
  }
  const ids = ['rtp-screen-width', 'rtp-screen-height', 'rtp-min-match'];
  const hasValues = ids.some(id => { const el = document.getElementById(id); return el && el.value.trim() !== ''; });
  ids.forEach(id => {
    const el = document.getElementById(id);
    if (!el) return;
    if (hasValues) {
      el.value = '';
    } else if (el.placeholder) {
      el.value = el.placeholder;
    }
  });
  updateScreenDimsToggleBtn();
}

function updateSpinTestPlaceholders() {
  const wEl = document.getElementById('rtp-screen-width');
  const hEl = document.getElementById('rtp-screen-height');
  const w = parseInt((wEl && (wEl.value || wEl.placeholder)) || 5);
  const h = parseInt((hEl && (hEl.value || hEl.placeholder)) || 3);
  const screenEl = document.getElementById('spin-test-screen');
  const stopsEl  = document.getElementById('spin-test-stops');
  if (stopsEl) {
    stopsEl.placeholder = Array.from({length: w}, () => 0).join(', ');
  }
  if (screenEl) {
    const rs0 = _latestReelSets && _latestReelSets[0];
    if (rs0 && rs0.reelSet && rs0.reelSet.length > 0) {
      const cols = [];
      for (let c = 0; c < w; c++) {
        const reel = rs0.reelSet[c] || [];
        const rows = [];
        for (let r = 0; r < h; r++) rows.push(reel[r] != null ? reel[r] : 0);
        cols.push('[' + rows.join(',') + ']');
      }
      screenEl.placeholder = '[' + cols.join(',') + ']';
    } else {
      const symRows = document.querySelectorAll('.rtp-sym-row');
      const symIds = symRows.length > 0
        ? Array.from(symRows).map(r => parseInt(r.dataset.symId))
        : Array.from({ length: w * h }, (_, i) => i + 1);
      let idx = 0;
      const cols = [];
      for (let c = 0; c < w; c++) {
        const rows = [];
        for (let r = 0; r < h; r++) rows.push(symIds[idx++ % symIds.length]);
        cols.push('[' + rows.join(',') + ']');
      }
      screenEl.placeholder = '[' + cols.join(',') + ']';
    }
  }
}

function updateSpinScreenFillBtn() {
  const btn = document.getElementById('spin-screen-fill-btn');
  if (!btn) return;
  const screenEl = document.getElementById('spin-test-screen');
  const hasValues = screenEl && screenEl.value.trim() !== '';
  btn.innerHTML = hasValues
    ? `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`
    : `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>`;
  btn.title = hasValues ? 'Clear screen override' : 'Use placeholders as defaults';
  btn.classList.toggle('danger', hasValues);
}

function fillSpinTestDefaults() {
  const screenEl = document.getElementById('spin-test-screen');
  const hasValues = screenEl && screenEl.value.trim() !== '';

  if (hasValues) {
    if (screenEl) screenEl.value = '';
    updateSpinScreenFillBtn();
    return;
  }

  const strat = document.getElementById('rtp-strategy')?.value || 'LTR';
  const isContacts = strat === 'SCATTERS';
  const isClusters = strat === 'CLUSTERS';
  const isWays = strat === 'WAYS';

  if (isContacts) {
    const scatterHasValues = (() => {
      const screenInputs = ['rtp-screen-width','rtp-screen-height','rtp-min-match'].map(id => document.getElementById(id));
      const intervalInputs = document.querySelectorAll('#interval-sets-container .scatter-from, #interval-sets-container .scatter-to');
      return screenInputs.some(inp => inp && inp.value.trim() !== '')
        || Array.from(intervalInputs).some(inp => inp.value.trim() !== '');
    })();
    if (!scatterHasValues) toggleScatterDefs();
  } else if (isClusters) {
    const clusterHasValues = (() => {
      const screenInputs = ['rtp-screen-width','rtp-screen-height','rtp-min-match'].map(id => document.getElementById(id));
      const intervalInputs = document.querySelectorAll('#clusters-interval-sets-container .scatter-from, #clusters-interval-sets-container .scatter-to');
      return screenInputs.some(inp => inp && inp.value.trim() !== '')
        || Array.from(intervalInputs).some(inp => inp.value.trim() !== '');
    })();
    if (!clusterHasValues) toggleClusterDefs();
  } else if (!isWays) {
    const lineHasValues = (() => {
      const screenInputs = ['rtp-screen-width','rtp-screen-height','rtp-min-match'].map(id => document.getElementById(id));
      const lineInputs = document.querySelectorAll('.rtp-line-input');
      return screenInputs.some(inp => inp && inp.value.trim() !== '')
        || Array.from(lineInputs).some(inp => inp.value.trim() !== '');
    })();
    if (!lineHasValues) toggleLineDefs();
  } else {
    ['rtp-screen-width','rtp-screen-height','rtp-min-match'].forEach(id => {
      const inp = document.getElementById(id);
      if (inp && !inp.value.trim()) inp.value = inp.placeholder;
    });
  }

  const symHasValues = Array.from(document.querySelectorAll('#rtp-symbol-rows .rtp-paytable-input')).some(inp => inp.value.trim() !== '');
  if (!symHasValues) toggleSymConfig();

  updateSpinTestPlaceholders();
  if (screenEl && !screenEl.value.trim() && screenEl.placeholder) {
    screenEl.value = screenEl.placeholder;
  }
  updateSpinScreenFillBtn();
}
