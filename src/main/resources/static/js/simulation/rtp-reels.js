/* ── RTP Tab — reel sets and shared state ── */

let _latestReelSets = null;
let _rtpSymbolCounter = 0;

function storeGeneratedReels(reelSetsJson) {
  try {
    _latestReelSets = JSON.parse(reelSetsJson);
    const firstRs = _latestReelSets[0];
    if (firstRs && Array.isArray(firstRs.reelSet)) {
      const wEl = document.getElementById('rtp-screen-width');
      if (wEl) wEl.value = firstRs.reelSet.length;
    }
    syncRtpChances();
    syncMegawaysHeights();
    syncRtpSymbolsFromReels();
    updateSpinTestPlaceholders();
  } catch(e) {
    // ignore parse errors (non-JSON output)
  }
}

function restoreRtpForm(payload) {
  if (!payload) return;
  const set = (id, v) => { const el = document.getElementById(id); if (el && v != null) el.value = v; };
  // 1. Set strategy + screen dims first so onStrategyChange / onScreenSizeChange render the right UI
  set('rtp-strategy', payload.strategy);
  set('rtp-screen-width', payload.screenWidth);
  set('rtp-screen-height', payload.screenHeight);
  set('rtp-min-match', payload.minMatch);
  set('rtp-threads', payload.threadCount);
  set('rtp-bet-size', payload.betSize);
  const spinsEl = document.getElementById('rtp-spins');
  if (spinsEl && payload.spins) {
    const opt = Array.from(spinsEl.options).find(o => parseInt(o.value) === payload.spins);
    if (opt) spinsEl.value = opt.value;
  }
  // 2. Reel sets (must be set before syncRtpChances)
  if (payload.reelSets) {
    _latestReelSets = payload.reelSets;
    syncRtpChances();
    syncMegawaysHeights();
  }
  if (payload.reelSetChances) {
    payload.reelSetChances.forEach((c, i) => {
      const el = document.getElementById('rtp-chance-' + i);
      if (el) el.value = c.chance;
    });
    updateChanceTotal();
  }
  if (payload.megawaysReelHeightChances) {
    payload.megawaysReelHeightChances.forEach((setData, s) => {
      if (!setData) return;
      setData.forEach((reelData, r) => {
        if (!reelData) return;
        const inp = document.getElementById('mw-h-' + s + '-' + r);
        if (inp) inp.value = reelData.join(', ');
      });
    });
  }
  // 3. Line definitions
  document.getElementById('rtp-lines-list').innerHTML = '';
  _lineCounter = 0;
  (payload.lineDefinitions || []).forEach(line => addLineDef(line.join(', ')));
  updateLineCount();
  // 4. Interval sets
  document.getElementById('interval-sets-container').innerHTML = '';
  document.getElementById('clusters-interval-sets-container').innerHTML = '';
  const isClustersRestore = payload.strategy === 'CLUSTERS';
  const setsToRestore = payload.contactsIntervalSets || payload.scattersPaytable
    ? (payload.contactsIntervalSets || [{ name: 'default', intervals: payload.scattersPaytable || [] }])
    : [];
  setsToRestore.forEach(s => addIntervalSet(s.name, s.intervals || [], isClustersRestore));
  if (isClustersRestore && payload.adjacencyOffsets && payload.adjacencyOffsets.length > 0) {
    const container = document.getElementById('adjacency-offsets-container');
    if (container) {
      container.innerHTML = '';
      payload.adjacencyOffsets.forEach(o => addAdjacencyOffset(o.x, o.y));
    }
  }
  // 5. Fire layout updates so UI sections are in the right state before symbols are written
  onScreenSizeChange();
  onStrategyChange();
  // Re-apply dims after handlers (onStrategyChange clears fields that match its defaults)
  set('rtp-screen-width', payload.screenWidth);
  set('rtp-screen-height', payload.screenHeight);
  set('rtp-min-match', payload.minMatch);
  // 6. Symbols last — populated after onStrategyChange so wild-field visibility is set but values
  //    are written by us, not reset by the strategy handler
  document.getElementById('rtp-symbol-rows').innerHTML = '';
  _symRowCounter = 0;
  (payload.symbols || []).forEach(sym => {
    const row = addSymbolRow(sym.symbolId);
    const typeSel = row.querySelector('select');
    typeSel.value = sym.type;
    onSymbolTypeChange(typeSel);
    if (sym.type !== 'SCATTER') {
      row.querySelector('.rtp-paytable-input').value = (sym.paytable || []).join(', ');
    }
    if (sym.contactsIntervalSetName) {
      const setSel = row.querySelector('.rtp-interval-set-sel');
      if (setSel) setSel.value = sym.contactsIntervalSetName;
    }
    if (sym.type === 'WILD') {
      const aggSel = row.querySelector('.rtp-wild-agg');
      aggSel.value = sym.wildAggregation || 'NONE';
      onWildAggChange(aggSel);
      row.querySelector('.rtp-wild-mult').value = sym.wildMultiplier ?? 1;
      if (sym.wildAggregation === 'SEQUENCE') {
        row.querySelector('.rtp-wild-seq').value = (sym.wildSequence || []).join(', ');
      }
    }
  });
}

function syncRtpChances() {
  const container = document.getElementById('rtp-chances-list');
  if (!_latestReelSets || _latestReelSets.length === 0) {
    container.innerHTML = '<span style="font-size:0.72rem;color:var(--text3);font-style:italic">' + t('rtp.generate_reels_first') + '</span>';
    return;
  }
  const n = _latestReelSets.length;
  const equalShare = Math.floor(1000 / n) / 10;
  const remainder = +(100 - equalShare * n).toFixed(1);

  container.innerHTML = '';
  _latestReelSets.forEach((rs, i) => {
    const share = i === n - 1 ? +(equalShare + remainder).toFixed(1) : equalShare;
    const row = document.createElement('div');
    row.className = 'rtp-chance-row';
    row.innerHTML = `
      <span class="rtp-chance-label">${rs.setName || 'ReelSet#' + i}</span>
      <input type="number" class="rtp-chance-input" id="rtp-chance-${i}"
             value="${share}" min="0" max="100" step="0.1"
             oninput="updateChanceTotal()"/>
      <span class="rtp-chance-pct">%</span>
    `;
    container.appendChild(row);
  });
  updateChanceTotal();
}

function updateChanceTotal() {
  if (!_latestReelSets) return;
  let sum = 0;
  _latestReelSets.forEach((_, i) => {
    const el = document.getElementById('rtp-chance-' + i);
    if (el) sum += parseFloat(el.value) || 0;
  });
  const el = document.getElementById('rtp-chance-total');
  const rounded = Math.round(sum * 10) / 10;
  el.textContent = rounded.toFixed(1) + '%';
  const ok = Math.abs(rounded - 100) < 0.05;
  el.className = 'rtp-chance-total ' + (ok ? 'ok' : 'err');
}

function syncRtpSymbolsFromReels() {
  if (!_latestReelSets || _latestReelSets.length === 0) return;
  const existingIds = new Set(
    Array.from(document.querySelectorAll('.rtp-sym-row')).map(r => parseInt(r.dataset.symId))
  );
  const foundIds = new Set();
  for (const rs of _latestReelSets) {
    for (const reel of rs.reelSet) {
      for (const sym of reel) foundIds.add(sym);
    }
  }
  const sortedNew = [...foundIds].filter(id => !existingIds.has(id)).sort((a,b) => a-b);
  for (const id of sortedNew) {
    addSymbolRow(id);
  }
  refreshSymbolRowNumbers();

  const firstRs = _latestReelSets[0];
  const w = firstRs.reelSet.length;
  if (document.querySelectorAll('.rtp-line-row').length === 0) {
    const h = parseInt(document.getElementById('rtp-screen-height').value) || 3;
    for (const line of generateDefaultLines(w, h)) {
      addLineDef(line);
    }
  }
}

// ── MEGAWAYS reel height chances ─────────────────────────────────────────────

function _mwHeightCount() {
  if (_latestReelSets && _latestReelSets[0] && Array.isArray(_latestReelSets[0].reelSet)) {
    return _latestReelSets[0].reelSet.length;
  }
  const wEl = document.getElementById('rtp-screen-width');
  return parseInt(wEl?.value) || parseInt(wEl?.placeholder) || 5;
}

function syncMegawaysHeights() {
  const container = document.getElementById('megaways-heights-list');
  if (!container) return;
  if (!_latestReelSets || _latestReelSets.length === 0) {
    container.innerHTML = '<span style="font-size:0.72rem;color:var(--text3);font-style:italic">' + t('rtp.generate_reels_first') + '</span>';
    return;
  }
  const setCount = _latestReelSets.length;
  const reelCount = _latestReelSets[0].reelSet.length;
  const n = _mwHeightCount();
  const placeholder = _mwDefaultValues(n).join(', ');
  const titleAttr = n + ' values (height chances per reel) — must sum to 100';

  // Preserve existing values keyed by id before rebuild
  const saved = {};
  container.querySelectorAll('.mw-height-input').forEach(inp => { saved[inp.id] = inp.value; });

  container.innerHTML = '';
  for (let s = 0; s < setCount; s++) {
    const setName = _latestReelSets[s].setName || ('ReelSet#' + s);
    const header = document.createElement('div');
    header.style.cssText = 'font-size:0.7rem;font-weight:600;color:var(--text2);margin-bottom:0.25rem;margin-top:' + (s > 0 ? '0.6rem' : '0');
    header.textContent = setName;
    container.appendChild(header);
    for (let r = 0; r < reelCount; r++) {
      const id = 'mw-h-' + s + '-' + r;
      const row = document.createElement('div');
      row.className = 'rtp-chance-row';
      row.style.cssText = 'display:flex;align-items:center;gap:0.4rem;margin-bottom:0.2rem';
      row.innerHTML =
        '<span style="font-size:0.68rem;color:var(--text3);min-width:3.5rem">Reel ' + (r+1) + '</span>' +
        '<input type="text" class="array-input mw-height-input" id="' + id + '" ' +
        'placeholder="' + placeholder + '" style="flex:1;font-size:0.7rem" ' +
        'title="' + titleAttr + '"/>';
      container.appendChild(row);
      // Restore saved value if count matches; otherwise clear so placeholder shows
      if (saved[id]) {
        const parts = saved[id].split(',').map(v => v.trim()).filter(Boolean);
        const inp = row.querySelector('.mw-height-input');
        if (parts.length === n) inp.value = saved[id];
      }
    }
  }
}

function _mwDefaultValues(n) {
  // Distribute 100 across n buckets biased toward lower counts
  if (n === 1) return [100];
  // Use the original 6-value profile scaled/truncated to n
  const base = [35, 35, 20, 5, 3, 2];
  if (n === base.length) return base;
  if (n < base.length) {
    const slice = base.slice(0, n);
    const sum = slice.reduce((a, b) => a + b, 0);
    const scaled = slice.map(v => Math.round(v * 100 / sum));
    // Fix rounding to exactly 100
    const diff = 100 - scaled.reduce((a, b) => a + b, 0);
    scaled[0] += diff;
    return scaled;
  }
  // n > 6: pad with small values
  const extra = n - base.length;
  const padVal = Math.max(1, Math.floor(2 / extra));
  const result = [...base];
  for (let i = 0; i < extra; i++) result.push(padVal);
  const sum = result.reduce((a, b) => a + b, 0);
  const scaled = result.map(v => Math.round(v * 100 / sum));
  const diff = 100 - scaled.reduce((a, b) => a + b, 0);
  scaled[0] += diff;
  return scaled;
}

function fillMegawaysHeightDefaults() {
  const n = _mwHeightCount();
  const defaults = _mwDefaultValues(n).join(', ');
  document.querySelectorAll('.mw-height-input').forEach(inp => {
    if (!inp.value.trim()) inp.value = defaults;
  });
}

function collectMegawaysHeightChances() {
  if (!_latestReelSets || _latestReelSets.length === 0) return null;
  const setCount = _latestReelSets.length;
  const reelCount = _latestReelSets[0].reelSet.length;
  const result = [];
  for (let s = 0; s < setCount; s++) {
    const reelData = [];
    for (let r = 0; r < reelCount; r++) {
      const inp = document.getElementById('mw-h-' + s + '-' + r);
      const raw = inp ? inp.value.trim() : '';
      if (!raw) {
        reelData.push(null);
      } else {
        const vals = raw.split(',').map(v => parseFloat(v.trim()));
        reelData.push(vals);
      }
    }
    result.push(reelData);
  }
  return result;
}
