/* ── RTP Tab — reel sets and shared state ── */

let _latestReelSets = null;
let _rtpSymbolCounter = 0;

function storeGeneratedReels(reelSetsJson) {
  try {
    _latestReelSets = JSON.parse(reelSetsJson);
    syncRtpChances();
    syncRtpSymbolsFromReels();
    updateSpinTestPlaceholders();
  } catch(e) {
    // ignore parse errors (non-JSON output)
  }
}

function restoreRtpForm(payload) {
  if (!payload) return;
  const set = (id, v) => { const el = document.getElementById(id); if (el && v != null) el.value = v; };
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
  if (payload.reelSets) {
    _latestReelSets = payload.reelSets;
    syncRtpChances();
  }
  if (payload.reelSetChances) {
    payload.reelSetChances.forEach((c, i) => {
      const el = document.getElementById('rtp-chance-' + i);
      if (el) el.value = c.chance;
    });
    updateChanceTotal();
  }
  document.getElementById('rtp-lines-list').innerHTML = '';
  _lineCounter = 0;
  (payload.lineDefinitions || []).forEach(line => addLineDef(line.join(', ')));
  updateLineCount();
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
  onScreenSizeChange();
  onStrategyChange();
}

function syncRtpChances() {
  const container = document.getElementById('rtp-chances-list');
  if (!_latestReelSets || _latestReelSets.length === 0) {
    container.innerHTML = '<span style="font-size:0.72rem;color:var(--text3);font-style:italic">Generate reels first</span>';
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
