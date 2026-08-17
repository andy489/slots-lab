/* ── Strategy change and paytable management ── */

function onStrategyChange() {
  const strat = document.getElementById('rtp-strategy').value;
  const isWays = strat === 'WAYS';
  const isMegaways = strat === 'MEGAWAYS';
  const isContacts = strat === 'SCATTERS';
  const isClusters = strat === 'CLUSTERS';
  const isSuperLines = strat === 'SL';
  const isAdj = strat === 'ADJ';
  const noLines = isWays || isMegaways || isContacts || isClusters;

  // Snapshot BEFORE any DOM changes (addIntervalSet → updatePaytablePlaceholders fires below)
  const _ptSnapshotInputs = document.querySelectorAll('.rtp-paytable-input:not([disabled])');
  const _ptOldPlaceholders = new Map();
  _ptSnapshotInputs.forEach(inp => _ptOldPlaceholders.set(inp, inp.placeholder));

  const section = document.getElementById('line-defs-section');
  const addBtn = document.getElementById('linedef-add-btn');
  const lineToggleBtn = document.getElementById('linedefs-toggle-btn');
  const scatterToggleBtn = document.getElementById('scatterdefs-toggle-btn');
  const screenDimsToggleBtn = document.getElementById('screendims-toggle-btn');
  if (section) section.style.display = noLines ? 'none' : '';
  if (addBtn) addBtn.style.display = noLines ? 'none' : '';
  if (lineToggleBtn) lineToggleBtn.style.display = noLines ? 'none' : '';
  if (scatterToggleBtn) scatterToggleBtn.style.display = isContacts ? '' : 'none';
  if (screenDimsToggleBtn) screenDimsToggleBtn.style.display = (isWays || isMegaways || isClusters) ? '' : 'none';

  // Show/hide MEGAWAYS reel tiles count chances subsection
  const megawaysCard = document.getElementById('megaways-heights-card');
  if (megawaysCard) megawaysCard.style.display = isMegaways ? 'flex' : 'none';

  const scattersSection = document.getElementById('scatters-paytable-section');
  if (scattersSection) {
    scattersSection.style.display = isContacts ? 'flex' : 'none';
    if (isContacts && document.getElementById('interval-sets-container').children.length === 0) {
      addIntervalSet(null, [{ from: 8, to: 10 }, { from: 11, to: 14 }, { from: 15, to: 19 }, { from: 20, to: 25 }]);
    }
  }

  const clustersSection = document.getElementById('clusters-adjacency-section');
  if (clustersSection) {
    clustersSection.style.display = isClusters ? 'flex' : 'none';
    if (isClusters && document.getElementById('clusters-interval-sets-container').children.length === 0) {
      addIntervalSet(null, [{ from: 5, to: 7 }, { from: 8, to: 10 }, { from: 11, to: 15 }, { from: 16, to: 25 }], true);
    }
    if (isClusters && document.getElementById('adjacency-offsets-container').children.length === 0) {
      setAdjacencyPreset(4);
    }
  }


  const wEl = document.getElementById('rtp-screen-width');
  const hEl = document.getElementById('rtp-screen-height');
  const mEl = document.getElementById('rtp-min-match');
  const newW = isMegaways ? '6' : '5';
  const newH = isMegaways ? '7' : (isContacts || isClusters) ? '5' : '3';
  const newM = isMegaways ? '3' : (isContacts || isClusters) ? '5' : '3';
  const oldW = isMegaways ? '5' : '6';
  const oldH = isMegaways ? '3' : (isContacts || isClusters) ? '3' : '7';
  const oldM = isMegaways ? '3' : (isContacts || isClusters) ? '3' : '7';
  if (wEl) {
    wEl.placeholder = newW;
    // Only auto-clear/reset width when no reel data is loaded — if reels are present, width is owned by them
    const reelWidth = (_latestReelSets && _latestReelSets[0] && Array.isArray(_latestReelSets[0].reelSet))
      ? _latestReelSets[0].reelSet.length : null;
    if (!reelWidth) {
      if (!wEl.value || wEl.value === oldW || wEl.value === newW) wEl.value = '';
    }
  }
  if (hEl) {
    hEl.placeholder = newH;
    if (!hEl.value || hEl.value === oldH || hEl.value === newH) hEl.value = '';
  }
  if (mEl) {
    mEl.placeholder = newM;
    if (!mEl.value || mEl.value === oldM || mEl.value === newM) mEl.value = '';
  }

  const isContactsLike = isContacts || isClusters;

  document.querySelectorAll('.rtp-sym-row').forEach(row => {
    const typeSel = row.querySelector('select');
    const setSel = row.querySelector('.rtp-interval-set-sel');
    const isWild = typeSel && typeSel.value === 'WILD';
    const isScatter = typeSel && typeSel.value === 'SCATTER';
    const ptInput = row.querySelector('.rtp-paytable-input');

    if ((isScatter || (isWild && (isContactsLike || isWays || isMegaways))) && ptInput) {
      ptInput.disabled = true;
      ptInput.placeholder = 'n/a';
      ptInput.value = '';
      if (setSel) setSel.style.display = 'none';
    } else {
      if (ptInput && !isScatter) ptInput.disabled = false;
      if (setSel) setSel.style.display = (isContactsLike && !isScatter && !isWild) ? '' : 'none';
    }

    if (!typeSel || !isWild) return;
    const wildFields = row.querySelector('.rtp-wild-fields');
    if (wildFields) wildFields.style.display = (isWays || isMegaways) ? 'none' : '';
    const aggSel = row.querySelector('.rtp-wild-agg');
    if (aggSel) { aggSel.value = 'NONE'; onWildAggChange(aggSel); }
  });
  if (isContactsLike) refreshIntervalSetDropdowns();
  else {
    const wEl2 = document.getElementById('rtp-screen-width');
    const mEl2 = document.getElementById('rtp-min-match');
    const w = parseInt((wEl2 && (wEl2.value || wEl2.placeholder)) || 5);
    const m = parseInt((mEl2 && (mEl2.value || mEl2.placeholder)) || 3);
    updatePaytablePlaceholders(w, m);
  }

  document.querySelectorAll('.rtp-paytable-input:not([disabled])').forEach(inp => {
    const old = _ptOldPlaceholders.get(inp);
    if (old && inp.value && inp.value === old) inp.value = '';
  });

  document.querySelectorAll('.rtp-line-input').forEach(inp => {
    if (inp.value && inp.placeholder && inp.value === inp.placeholder) inp.value = '';
  });
  updateLineDefsToggleBtn();

  document.querySelectorAll('.scatter-from, .scatter-to').forEach(inp => {
    if (inp.value && inp.placeholder && inp.value === inp.placeholder) inp.value = '';
  });
  updateScatterDefsToggleBtn();
  updateScreenDimsToggleBtn();

  const tip = document.getElementById('screen-paytable-tip');
  if (tip) {
    const gridRows =
      '<div class="tip-rule"><div class="tip-row"><span>Width</span><span>Number of reels (columns)</span></div>' +
      '<div class="tip-row"><span>Height</span><span>Visible rows per reel</span></div>' +
      '<div class="tip-row"><span>Min Match</span><span>Consecutive symbols needed for a win</span></div></div>';
    if (isContacts) {
      tip.innerHTML = 'Defines the grid and contacts paytable.' + gridRows +
        '<div class="tip-rule">Contacts paytable — interval sets: each set maps a range of matching symbol counts to a payout multiplier. ' +
        'A symbol wins when its total count on the screen falls within an interval; the interval boundaries are <strong>inclusive</strong> ' +
        '(e.g. from 3 to 5 triggers for counts 3, 4, or 5). Each symbol row can be assigned to a specific interval set via the dropdown.</div>';
    } else if (isClusters) {
      tip.innerHTML = 'Defines the grid and contacts paytable.' + gridRows +
        '<div class="tip-rule">Contacts paytable — interval sets: same as Scatters Pay, but only connected clusters count. ' +
        'Two tiles are connected if they are adjacent according to the configured neighbour offsets.</div>' +
        '<div class="tip-rule">Adjacency — choose 4-direction (up/down/left/right), 8-direction (all neighbours including diagonals), or define custom offsets (x=reel, y=row).</div>';
    } else if (isWays) {
      tip.innerHTML = 'Defines the grid.' + gridRows +
        '<div class="tip-rule">WAYS pays for every combination of matching symbols across consecutive reels — no fixed paylines required.</div>';
    } else if (isMegaways) {
      tip.innerHTML = 'Defines the grid (default 6×7).' + gridRows +
        '<div class="tip-rule"><strong>MEGAWAYS:</strong> Each reel independently draws a visible height (2–7 rows) from the configured probability distribution. ' +
        'Remaining rows are filled with a mask symbol (id=0) and do not participate in wins. ' +
        'Payout is identical to WAYS — all symbol combinations across consecutive reels are awarded.</div>' +
        '<div class="tip-rule">Configure per-reel tile count probabilities in the <strong>Reel Tiles Count Chances</strong> section.</div>';
    } else if (isSuperLines) {
      tip.innerHTML = 'Defines the grid and winning lines.' + gridRows +
        '<div class="tip-rule">Line definitions: each payline is a list of row indices (0-based), one per reel. ' +
        'e.g. <span style="font-family:monospace">[1,1,1,1,1]</span> = middle row across all 5 reels. ' +
        'Only symbols landing on a defined payline can contribute to a win.</div>' +
        '<div class="tip-rule"><strong>SL:</strong> Matching symbols on the same payline win even when they are not adjacent — gaps between them are allowed. ' +
        'Think of every regular symbol as a scatter, but <em>only within its payline</em>.</div>';
    } else if (isAdj) {
      tip.innerHTML = 'Defines the grid and winning lines.' + gridRows +
        '<div class="tip-rule">Line definitions: each payline is a list of row indices (0-based), one per reel. ' +
        'e.g. <span style="font-family:monospace">[1,1,1,1,1]</span> = middle row across all 5 reels. ' +
        'Only symbols landing on a defined payline can contribute to a win.</div>' +
        '<div class="tip-rule"><strong>ADJ:</strong> Symbols must be <strong>consecutive and adjacent</strong> on the same payline, ' +
        'but the streak can start from any reel — the best-paying window per payline is awarded.</div>';
    } else {
      tip.innerHTML = 'Defines the grid and winning lines.' + gridRows +
        '<div class="tip-rule">Line definitions: each payline is a list of row indices (0-based), one per reel. ' +
        'e.g. <span style="font-family:monospace">[1,1,1,1,1]</span> = middle row across all 5 reels (only valid when width=5, height=3). ' +
        'Only symbols landing on a defined payline can contribute to a win.</div>';
    }
  }
  const symTip = document.getElementById('symconfig-tip');
  if (symTip) {
    const paytableExample =
      'Paytable multipliers apply to the total stake per screen (bet size).' +
      '<div class="tip-rule"><div style="opacity:0.55;margin-bottom:0.2rem;font-size:0.67rem">e.g. bet = 1.00, paytable = [1.0, 3.0, 10.0]</div>' +
      '<div class="tip-row"><span>x3 match</span><span>1.0 × 1.00 = 1.00</span></div>' +
      '<div class="tip-row"><span>x4 match</span><span>3.0 × 1.00 = 3.00</span></div>' +
      '<div class="tip-row"><span>x5 match</span><span>10.0 × 1.00 = 10.00</span></div></div>' +
      '<div class="tip-rule">values count = screenWidth − minMatch + 1</div>';
    const wildBase = '<div class="tip-rule">At least one <strong>NORMAL</strong> symbol with a paytable is required. ' +
      'A <strong>WILD</strong> without a paytable automatically pays the highest NORMAL symbol\'s payout for a wild-only streak.</div>';
    if (isContacts) {
      symTip.innerHTML = paytableExample + wildBase +
        '<div class="tip-rule"><strong>SCATTERS strategy:</strong> Each NORMAL symbol\'s paytable entries correspond to the interval set assigned to it — ' +
        'the first entry pays when the contact count falls in the first interval, the second entry for the second interval, and so on. ' +
        'SCATTER symbols act as blockers (no payout). WILD symbols inherit the payout of the highest-paying NORMAL symbol for the same contact count.</div>';
    } else if (isClusters) {
      symTip.innerHTML = paytableExample + wildBase +
        '<div class="tip-rule"><strong>CLUSTERS strategy:</strong> Same interval-based paytable as Scatters Pay, but only connected clusters of symbols count. ' +
        'Connectivity is determined by the configured adjacency offsets. SCATTER symbols act as blockers. WILD symbols extend clusters.</div>';
    } else if (isWays || isMegaways) {
      symTip.innerHTML = paytableExample + wildBase +
        '<div class="tip-rule"><strong>' + strat + ' strategy:</strong> ADD/MULTIPLY/SEQUENCE wild multipliers are disabled — ' +
        'they would double-count the wild\'s contribution, which is already reflected in the ways count. ' +
        '<em>Ways-with-ways-multipliers</em> (wild expands ways per reel) is a separate, orthogonal feature and is supported.</div>';
    } else if (isSuperLines) {
      symTip.innerHTML = paytableExample + wildBase +
        '<div class="tip-rule"><strong>SL strategy:</strong> Identical paytable structure to LTR — each entry corresponds to a match count starting from Min Match. ' +
        'Symbols do <em>not</em> need to be consecutive on the payline; gaps are ignored and all matching symbols on the same line count together.</div>';
    } else {
      symTip.innerHTML = paytableExample + wildBase;
    }
  }
  updateSimSettingsTip();
  updateSymConfigToggleBtn();
  const injectBtn = document.getElementById('inject-chances-btn');
  if (injectBtn) {
    injectBtn.title = strat === 'MEGAWAYS'
      ? 'Add reel set chances and tiles count chances to Generate tab result'
      : 'Add reel set chances to Generate tab result';
  }
  syncMegawaysHeights();
}

const _simStrategyDescriptions = {
  LTR:      { label: 'Left to Right', desc: 'Symbols pay on consecutive adjacent reels starting from the <strong>leftmost reel</strong> and running right. The streak breaks as soon as a non-matching symbol (or scatter) is encountered.' },
  RTL:      { label: 'Right to Left', desc: 'Mirror of LTR — streak starts from the <strong>rightmost reel</strong> and runs left. Useful for both-ways games when combined with LTR.' },
  BW:       { label: 'Both Ways', desc: 'Evaluates both <strong>LTR and RTL</strong> independently and awards the combined total. Each payline is scored in both directions; wins from both are summed.' },
  SL:       { label: 'Super Lines', desc: 'Like LTR but <strong>gaps are allowed</strong>. All matching symbols on the same payline count together regardless of adjacency — every symbol behaves like a scatter within its own payline.' },
  ADJ:      { label: 'Adjacent', desc: 'Symbols pay on <strong>consecutive adjacent reels</strong> within the same payline definition. Unlike LTR the streak can start from any reel — the best-paying window on each payline is awarded.' },
  WAYS:     { label: 'All Ways', desc: 'No paylines needed. Symbols pay for <strong>every combination</strong> of matching positions across consecutive reels — the ways count multiplies the payout.' },
  MEGAWAYS: { label: 'Megaways™', desc: 'Like WAYS but each reel draws a <strong>random visible height</strong> (2–7 rows) per spin according to a configurable probability distribution. Masked positions do not participate in wins.' },
  SCATTERS: { label: 'Scatters Pay', desc: 'No paylines needed. Symbols pay based on their <strong>total tile count anywhere on the screen</strong>. Payout is determined by interval sets assigned per symbol.' },
  CLUSTERS: { label: 'Clusters Pay', desc: 'No paylines needed. Symbols pay based on the size of <strong>connected clusters</strong>. Two tiles connect when they are adjacent according to the configured neighbour offsets.' },
};

function updateSimSettingsTip() {
  const tip = document.getElementById('sim-settings-tip');
  if (!tip) return;
  const strat = document.getElementById('rtp-strategy')?.value;
  const info = _simStrategyDescriptions[strat];
  const stratLine = info
    ? `<div class="tip-rule"><strong>${strat} — ${info.label}:</strong> ${info.desc}</div>`
    : '';
  tip.innerHTML =
    'Core simulation parameters.' +
    '<div class="tip-rule">' +
      '<div class="tip-row"><span>Strategy</span><span>Win evaluation method</span></div>' +
      '<div class="tip-row"><span>Spins</span><span>Total spin count to simulate</span></div>' +
      '<div class="tip-row"><span>Threads</span><span>1–8 worker threads (parallel spins)</span></div>' +
      '<div class="tip-row"><span>Bet Size</span><span>Stake per spin · 0.10–200.00, step 0.1</span></div>' +
    '</div>' +
    stratLine +
    '<div class="tip-rule">More threads = faster run, but results are statistically equivalent regardless of thread count.</div>';
}

let _setCardCounter = 0;
function addIntervalSet(name, intervals, forClusters) {
  const cid = _setCardCounter++;
  const containerId = forClusters ? 'clusters-interval-sets-container' : 'interval-sets-container';
  const container = document.getElementById(containerId);
  const card = document.createElement('div');
  card.className = 'interval-set-card';
  card.dataset.setId = cid;
  card.dataset.forClusters = forClusters ? '1' : '';
  card.style.cssText = 'border:1px solid var(--border);border-radius:6px;padding:0.5rem 0.6rem;display:flex;flex-direction:column;gap:0.3rem';
  card.innerHTML =
    '<div style="display:flex;align-items:center;gap:0.5rem;margin-bottom:0.2rem">' +
      '<span class="interval-set-card-label" style="font-size:0.78rem;opacity:0.8;font-weight:500;flex:1"></span>' +
      '<button class="icon-btn remove" onclick="removeIntervalSet(this)" title="Remove set" style="margin-left:auto">✕</button>' +
    '</div>' +
    '<div class="interval-rows" style="display:grid;grid-template-columns:1fr 1fr;gap:0.3rem"></div>' +
    '<button class="add-btn" style="font-size:0.72rem;padding:0.15rem 0.5rem;margin-top:0.15rem" onclick="addScatterInterval(this.closest(\'.interval-set-card\'))">+ Add interval</button>';
  container.appendChild(card);
  refreshIntervalSetCardLabels();
  (intervals || [{ from: undefined, to: undefined }]).forEach(e => addScatterInterval(card, e.from, e.to));
  refreshIntervalSetDropdowns();
}

function refreshIntervalSetCardLabels() {
  ['interval-sets-container', 'clusters-interval-sets-container'].forEach(cid => {
    const container = document.getElementById(cid);
    if (!container) return;
    container.querySelectorAll('.interval-set-card').forEach((card, i) => {
      const lbl = card.querySelector('.interval-set-card-label');
      const name = 'int-set-' + (i + 1);
      if (lbl) lbl.textContent = name;
      card.dataset.setName = name;
    });
  });
}

function removeIntervalSet(btn) {
  const card = btn.closest('.interval-set-card');
  const forClusters = card.dataset.forClusters === '1';
  const containerId = forClusters ? 'clusters-interval-sets-container' : 'interval-sets-container';
  if (document.getElementById(containerId).children.length <= 1) {
    showToast('At least one interval set is required', true); return;
  }
  card.remove();
  refreshIntervalSetCardLabels();
  refreshIntervalSetDropdowns();
  const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
  const m = parseInt(document.getElementById('rtp-min-match').value) || 3;
  updatePaytablePlaceholders(w, m);
}

function validateIntervalField(inp) {
  const row = inp.closest('.scatter-interval-row');
  const wEl = document.getElementById('rtp-screen-width');
  const hEl = document.getElementById('rtp-screen-height');
  const w = parseInt(wEl.value || wEl.placeholder) || 5;
  const h = parseInt(hEl.value || hEl.placeholder) || 5;
  const max = w * h;
  const val = parseInt(inp.value);
  let invalid = false;
  if (inp.value === '') { invalid = false; }
  else if (isNaN(val) || val < 1 || val > max) { invalid = true; }
  else {
    const fromInp = row.querySelector('.scatter-from');
    const toInp   = row.querySelector('.scatter-to');
    if (fromInp.value && toInp.value) {
      const f = parseInt(fromInp.value), t = parseInt(toInp.value);
      if (!isNaN(f) && !isNaN(t) && f > t) {
        [fromInp, toInp].forEach(x => {
          x.style.color = 'var(--error,#e55)';
          x.style.borderColor = 'var(--error,#e55)';
        });
        return;
      }
    }
  }
  if (invalid) {
    inp.style.color = 'var(--error,#e55)';
    inp.style.borderColor = 'var(--error,#e55)';
  } else {
    inp.style.borderColor = '';
    inp.style.color = '';
  }
  const fromInp = row.querySelector('.scatter-from');
  const toInp   = row.querySelector('.scatter-to');
  if (fromInp && toInp) {
    const f = parseInt(fromInp.value), t = parseInt(toInp.value);
    if (!isNaN(f) && !isNaN(t) && f <= t) {
      [fromInp, toInp].forEach(x => {
        if (parseInt(x.value) >= 1 && parseInt(x.value) <= max) {
          x.style.borderColor = '';
          x.style.color = '';
        }
      });
    }
  }
}

function addScatterInterval(card, from, to) {
  if (!card) {
    const cards = document.querySelectorAll('.interval-set-card');
    card = cards[cards.length - 1];
  }
  const rowsDiv = card.querySelector('.interval-rows');
  const wEl = document.getElementById('rtp-screen-width');
  const hEl = document.getElementById('rtp-screen-height');
  const w = parseInt(wEl.value || wEl.placeholder) || 5;
  const h = parseInt(hEl.value || hEl.placeholder) || 5;
  const maxContacts = w * h;

  if (from === undefined && to === undefined) {
    const existingRows = rowsDiv.querySelectorAll('.scatter-interval-row');
    if (existingRows.length > 0) {
      const lastTo = existingRows[existingRows.length - 1].querySelector('.scatter-to');
      const lastToVal = parseInt(lastTo.value || lastTo.placeholder);
      if (lastToVal === maxContacts) { from = maxContacts; to = maxContacts; }
    }
  }

  const row = document.createElement('div');
  row.className = 'scatter-interval-row';
  row.style.cssText = 'display:flex;align-items:center;gap:0.4rem';
  row.innerHTML =
    '<label style="font-size:0.72rem;opacity:0.7;min-width:2rem">from</label>' +
    '<input type="number" class="scatter-from" min="1" placeholder="3" style="width:2.6rem;text-align:center" title="Min contacts (1 – reels × rows = ' + maxContacts + ')" oninput="validateIntervalField(this)" onblur="validateIntervalField(this)"/>' +
    '<label style="font-size:0.72rem;opacity:0.7;min-width:1rem">to</label>' +
    '<input type="number" class="scatter-to" min="1" placeholder="5" style="width:2.6rem;text-align:center" title="Max contacts (1 – reels × rows = ' + maxContacts + ')" oninput="validateIntervalField(this)" onblur="validateIntervalField(this)"/>' +
    '<button class="icon-btn remove" title="Remove interval" style="margin-left:0.2rem" onclick="this.closest(\'.scatter-interval-row\').remove();refreshIntervalSetDropdowns();const w=parseInt(document.getElementById(\'rtp-screen-width\').value)||5,m=parseInt(document.getElementById(\'rtp-min-match\').value)||3;updatePaytablePlaceholders(w,m);">✕</button>';
  if (from !== undefined && from !== null) row.querySelector('.scatter-from').placeholder = from;
  if (to   !== undefined && to   !== null) row.querySelector('.scatter-to').placeholder   = to;
  rowsDiv.appendChild(row);
  const m = parseInt(document.getElementById('rtp-min-match').value) || 3;
  updatePaytablePlaceholders(w, m);
}

function getIntervalSetNames() {
  const strat = document.getElementById('rtp-strategy')?.value;
  const containerId = strat === 'CLUSTERS' ? 'clusters-interval-sets-container' : 'interval-sets-container';
  return Array.from(document.querySelectorAll('#' + containerId + ' .interval-set-card'))
    .map(card => card.dataset.setName || 'default');
}

function refreshIntervalSetDropdowns() {
  const names = getIntervalSetNames();
  document.querySelectorAll('.rtp-sym-row .rtp-interval-set-sel').forEach(sel => {
    const prev = sel.value;
    sel.innerHTML = names.map(n => `<option value="${escapeHtml(n)}">${escapeHtml(n)}</option>`).join('');
    if (names.includes(prev)) sel.value = prev;
  });
  const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
  const m = parseInt(document.getElementById('rtp-min-match').value) || 3;
  updatePaytablePlaceholders(w, m);
}

function collectContactsIntervalSets() {
  const cards = document.querySelectorAll('#interval-sets-container .interval-set-card');
  const result = [];
  const errors = [];
  const setNames = new Set();
  cards.forEach((card, si) => {
    const name = card.dataset.setName || ('interval-set-' + (si + 1));
    if (setNames.has(name)) { errors.push('Duplicate interval set name: "' + name + '"'); return; }
    setNames.add(name);
    const rows = card.querySelectorAll('.scatter-interval-row');
    const intervals = [];
    rows.forEach((row, i) => {
      const fromInp = row.querySelector('.scatter-from');
      const toInp   = row.querySelector('.scatter-to');
      const from = parseInt(fromInp.value || fromInp.placeholder);
      const to   = parseInt(toInp.value   || toInp.placeholder);
      if (isNaN(from) || isNaN(to))
        errors.push('Set "' + name + '" interval ' + (i + 1) + ': both from and to are required');
      else if (from > to)
        errors.push('Set "' + name + '" interval ' + (i + 1) + ': from (' + from + ') must be ≤ to (' + to + ')');
      else
        intervals.push({ from, to });
    });
    if (intervals.length === 0) errors.push('Set "' + name + '" must have at least one interval');
    for (let i = 0; i < intervals.length; i++) {
      for (let j = i + 1; j < intervals.length; j++) {
        if (intervals[i].from <= intervals[j].to && intervals[j].from <= intervals[i].to)
          errors.push('Set "' + name + '" intervals ' + (i + 1) + ' and ' + (j + 1) + ' overlap');
      }
    }
    result.push({ name, intervals });
  });
  return { contactsIntervalSets: result, errors };
}

function collectClustersIntervalSets() {
  const cards = document.querySelectorAll('#clusters-interval-sets-container .interval-set-card');
  const result = [];
  const errors = [];
  const setNames = new Set();
  cards.forEach((card, si) => {
    const name = card.dataset.setName || ('interval-set-' + (si + 1));
    if (setNames.has(name)) { errors.push('Duplicate interval set name: "' + name + '"'); return; }
    setNames.add(name);
    const rows = card.querySelectorAll('.scatter-interval-row');
    const intervals = [];
    rows.forEach((row, i) => {
      const fromInp = row.querySelector('.scatter-from');
      const toInp   = row.querySelector('.scatter-to');
      const from = parseInt(fromInp.value || fromInp.placeholder);
      const to   = parseInt(toInp.value   || toInp.placeholder);
      if (isNaN(from) || isNaN(to))
        errors.push('Set "' + name + '" interval ' + (i + 1) + ': both from and to are required');
      else if (from > to)
        errors.push('Set "' + name + '" interval ' + (i + 1) + ': from (' + from + ') must be ≤ to (' + to + ')');
      else
        intervals.push({ from, to });
    });
    if (intervals.length === 0) errors.push('Set "' + name + '" must have at least one interval');
    for (let i = 0; i < intervals.length; i++) {
      for (let j = i + 1; j < intervals.length; j++) {
        if (intervals[i].from <= intervals[j].to && intervals[j].from <= intervals[i].to)
          errors.push('Set "' + name + '" intervals ' + (i + 1) + ' and ' + (j + 1) + ' overlap');
      }
    }
    result.push({ name, intervals });
  });
  return { contactsIntervalSets: result, errors };
}

function setAdjacencyPreset(dirs) {
  const container = document.getElementById('adjacency-offsets-container');
  if (!container) return;
  container.innerHTML = '';
  const offsets4 = [[0, -1], [-1, 0], [1, 0], [0, 1]];
  const offsets8 = [[0, -1], [-1, 0], [1, 0], [0, 1], [-1, -1], [1, -1], [-1, 1], [1, 1]];
  const offsets = dirs === 8 ? offsets8 : offsets4;
  offsets.forEach(([x, y]) => addAdjacencyOffset(x, y));
}

function addAdjacencyOffset(x, y) {
  const container = document.getElementById('adjacency-offsets-container');
  if (!container) return;
  const row = document.createElement('div');
  row.className = 'adjacency-offset-row';
  row.style.cssText = 'display:flex;align-items:center;gap:0.4rem';
  row.innerHTML =
    '<label style="font-size:0.72rem;opacity:0.7;min-width:1.2rem">x</label>' +
    '<input type="number" class="adj-x" style="width:2.6rem;text-align:center" placeholder="0" value="' + (x !== undefined ? x : '') + '"/>' +
    '<label style="font-size:0.72rem;opacity:0.7;min-width:1.2rem">y</label>' +
    '<input type="number" class="adj-y" style="width:2.6rem;text-align:center" placeholder="0" value="' + (y !== undefined ? y : '') + '"/>' +
    '<button class="icon-btn remove" title="Remove offset" style="margin-left:0.2rem" onclick="this.closest(\'.adjacency-offset-row\').remove()">✕</button>';
  container.appendChild(row);
}

function collectAdjacencyOffsets() {
  const rows = document.querySelectorAll('.adjacency-offset-row');
  const offsets = [];
  rows.forEach(row => {
    const xInp = row.querySelector('.adj-x');
    const yInp = row.querySelector('.adj-y');
    const x = parseInt(xInp.value);
    const y = parseInt(yInp.value);
    if (!isNaN(x) && !isNaN(y)) offsets.push({ x, y });
  });
  return offsets;
}

function onScreenSizeChange() {
  const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
  const h = parseInt(document.getElementById('rtp-screen-height').value) || 3;
  const m = parseInt(document.getElementById('rtp-min-match').value) || 3;
  updatePaytablePlaceholders(w, m);
  const seqPh = Array.from({length: w}, (_, i) => (i + 1).toFixed(1)).join(', ');
  document.querySelectorAll('.rtp-wild-seq').forEach(inp => { inp.placeholder = seqPh; });
  const maxContacts = w * h;
  document.querySelectorAll('.scatter-from, .scatter-to').forEach(inp => {
    inp.title = 'Valid range: 1 – reels × rows = ' + maxContacts;
    validateIntervalField(inp);
  });
  // Adapt existing line definitions to the new width
  document.querySelectorAll('.rtp-line-input').forEach(inp => {
    const parts = inp.value.trim() ? inp.value.split(',').map(s => s.trim()) : [];
    if (parts.length === 0) return;
    if (parts.length < w) {
      while (parts.length < w) parts.push('0');
    } else if (parts.length > w) {
      parts.length = w;
    }
    inp.value = parts.join(', ');
  });
  syncMegawaysHeights();

  // Reinit payline placeholders when height changes and no values are filled
  const strat = document.getElementById('rtp-strategy')?.value;
  if (strat !== 'WAYS' && strat !== 'SCATTERS' && strat !== 'CLUSTERS' && strat !== 'MEGAWAYS') {
    const hasFilledLines = Array.from(document.querySelectorAll('.rtp-line-input')).some(inp => inp.value.trim() !== '');
    if (!hasFilledLines) reinitDefaultPaylinePlaceholders();
  }
}

function updatePaytablePlaceholders(w, m) {
  const strat = document.getElementById('rtp-strategy')?.value;
  const isScatters = strat === 'SCATTERS';
  const isClustersStrat = strat === 'CLUSTERS';
  const isIntervalBased = isScatters || isClustersStrat;

  const setIntervalCounts = {};
  if (isIntervalBased) {
    const containerId = isClustersStrat ? 'clusters-interval-sets-container' : 'interval-sets-container';
    document.querySelectorAll('#' + containerId + ' .interval-set-card').forEach(card => {
      const name = card.dataset.setName || 'default';
      setIntervalCounts[name] = card.querySelectorAll('.scatter-interval-row').length;
    });
  }

  const defaultIntervalCount = isIntervalBased
    ? (Object.values(setIntervalCounts)[0] || 1)
    : 0;
  const fallbackCount = isIntervalBased ? defaultIntervalCount : Math.max(1, w - m + 1);
  const SYM3_TABLE = {
    1: '5.0',
    2: '2.0, 5.0',
    3: '1.0, 2.0, 5.0',
    4: '0.5, 1.0, 2.0, 5.0',
    5: '0.2, 0.5, 1.0, 2.0, 5.0',
    6: '0.1, 0.2, 0.5, 1.0, 2.0, 5.0',
    7: '0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0',
  };
  const SYM4_TABLE = {
    1: '2.5',
    2: '1.0, 2.5',
    3: '0.5, 1.0, 2.5',
    4: '0.2, 0.5, 1.0, 2.5',
    5: '0.1, 0.2, 0.5, 1.0, 2.5',
    6: '0.1, 0.1, 0.2, 0.5, 1.0, 2.5',
    7: '0.1, 0.1, 0.2, 0.5, 1.0, 2.5, 5.0',
  };
  const SYM5_TABLE = {
    1: '1.5',
    2: '0.5, 1.5',
    3: '0.2, 0.5, 1.5',
    4: '0.1, 0.2, 0.5, 1.5',
    5: '0.1, 0.1, 0.2, 0.5, 1.5',
    6: '0.1, 0.1, 0.1, 0.2, 0.5, 1.5',
    7: '0.1, 0.1, 0.1, 0.2, 0.5, 1.5, 3.0',
  };
  const SYM6_TABLE = {
    1: '1.0',
    2: '0.3, 1.0',
    3: '0.1, 0.3, 1.0',
    4: '0.1, 0.1, 0.3, 1.0',
    5: '0.1, 0.1, 0.1, 0.3, 1.0',
    6: '0.1, 0.1, 0.1, 0.1, 0.3, 1.0',
    7: '0.1, 0.1, 0.1, 0.1, 0.3, 1.0, 2.0',
  };
  const SYM7_TABLE = {
    1: '0.5',
    2: '0.2, 0.5',
    3: '0.1, 0.2, 0.5',
    4: '0.1, 0.1, 0.2, 0.5',
    5: '0.1, 0.1, 0.1, 0.2, 0.5',
    6: '0.1, 0.1, 0.1, 0.1, 0.2, 0.5',
    7: '0.1, 0.1, 0.1, 0.1, 0.2, 0.5, 1.0',
  };
  const WILD_TABLE = {
    1: '50.0',
    2: '10.0, 50.0',
    3: '5.0, 10.0, 50.0',
    4: '2.0, 5.0, 10.0, 50.0',
    5: '1.0, 2.0, 5.0, 10.0, 50.0',
    6: '0.5, 1.0, 2.0, 5.0, 10.0, 50.0',
    7: '0.2, 0.5, 1.0, 2.0, 5.0, 10.0, 50.0',
  };

  document.querySelectorAll('.rtp-paytable-input').forEach(inp => {
    if (inp.disabled) return;
    const row = inp.closest('.rtp-sym-row');
    const symId = row ? parseInt(row.dataset.symId) : 0;
    const typeVal = row && row.querySelector('select') ? row.querySelector('select').value : 'NORMAL';
    const isWild = typeVal === 'WILD';
    const isScatterSym = typeVal === 'SCATTER';

    let count;
    if (isIntervalBased) {
      const setSel = row ? row.querySelector('.rtp-interval-set-sel') : null;
      const selName = setSel ? setSel.value : null;
      count = (selName && setIntervalCounts[selName]) ? setIntervalCounts[selName] : fallbackCount;
    } else {
      count = fallbackCount;
    }

    let ph;
    if (isScatterSym) {
      ph = Array.from({length: count}, () => '0').join(', ');
    } else if (isIntervalBased) {
      let v = 0.1;
      const vals = [];
      for (let i = 0; i < count; i++) {
        vals.push(parseFloat(v.toPrecision(10)));
        v *= 2;
      }
      ph = vals.map(x => {
        const s = x.toString();
        return s.includes('.') ? s : s + '.0';
      }).join(', ');
    } else if (isWild) {
      ph = WILD_TABLE[count] || Array.from({length: count}, (_, i) => ((i + 1) * 2).toFixed(1)).join(', ');
    } else {
      const symTable = symId <= 3 ? SYM3_TABLE
                     : symId === 4 ? SYM4_TABLE
                     : symId === 5 ? SYM5_TABLE
                     : symId === 6 ? SYM6_TABLE
                     : SYM7_TABLE;
      ph = symTable[count] || Array.from({length: count}, (_, i) => ((count - i) * 0.1).toFixed(1)).join(', ');
    }

    const titleSuffix = isIntervalBased
      ? count + ' value(s) required (one per interval in selected set)'
      : count + ' value(s) required (screenWidth − minMatch + 1)';

    inp.placeholder = ph;
    inp.title = titleSuffix;

    // Adapt filled value to the new entry count
    if (inp.value.trim() && !inp.dataset.fixedPlaceholder) {
      const parts = inp.value.split(',').map(s => s.trim()).filter(s => s !== '');
      if (parts.length !== count) {
        if (parts.length > count) {
          parts.length = count;
        } else {
          // Extend by appending values from the new placeholder
          const phParts = ph.split(',').map(s => s.trim());
          while (parts.length < count) parts.push(phParts[parts.length] || '0');
        }
        inp.value = parts.join(', ');
      }
    }
  });
}

function generateDefaultLines(width, height) {
  if (height < 2) return [Array(width).fill(0).join(', ')];

  const n = height;
  const linesCount = (n - 2) * 10;
  if (linesCount <= 0) return [];

  const base = Math.floor(linesCount / n);
  const remainder = linesCount % n;
  // rowQuota[r] = how many lines start with row r
  const rowQuota = Array.from({length: n}, (_, r) => base + (r === n - 1 ? remainder : 0));

  // Generate varied patterns for each starting row
  function patternsForRow(startRow, quota) {
    const result = [];
    const seen = new Set();
    // Candidate generators (each produces a length-width array of row indices)
    const candidates = [
      // straight
      () => Array(width).fill(startRow),
      // zigzag with neighbours
      () => Array.from({length: width}, (_, i) => {
        const delta = i % 2 === 0 ? 0 : (startRow < n - 1 ? 1 : -1);
        return Math.min(n - 1, Math.max(0, startRow + delta));
      }),
      () => Array.from({length: width}, (_, i) => {
        const delta = i % 2 === 0 ? 0 : (startRow > 0 ? -1 : 1);
        return Math.min(n - 1, Math.max(0, startRow + delta));
      }),
      // wave: alternates between startRow and startRow±1
      () => Array.from({length: width}, (_, i) => {
        const offset = (i % 2 === 0) ? 0 : ((startRow === 0) ? 1 : -1);
        return Math.min(n - 1, Math.max(0, startRow + offset));
      }),
      // edges pinned, middle varies
      () => { const a = Array(width).fill(startRow); if (width > 2) { const mid = Math.floor(width / 2); a[mid] = Math.min(n - 1, startRow + 1); } return a; },
      () => { const a = Array(width).fill(startRow); if (width > 2) { const mid = Math.floor(width / 2); a[mid] = Math.max(0, startRow - 1); } return a; },
      // ascending / descending drift
      () => Array.from({length: width}, (_, i) => Math.min(n - 1, startRow + Math.floor(i * (n - 1 - startRow) / Math.max(1, width - 1)))),
      () => Array.from({length: width}, (_, i) => Math.max(0, startRow - Math.floor(i * startRow / Math.max(1, width - 1)))),
      // chevron up
      () => { const a = []; const half = Math.ceil(width / 2); for (let i = 0; i < width; i++) { const d = i < half ? i : width - 1 - i; a.push(Math.min(n - 1, startRow + d)); } return a; },
      // chevron down
      () => { const a = []; const half = Math.ceil(width / 2); for (let i = 0; i < width; i++) { const d = i < half ? i : width - 1 - i; a.push(Math.max(0, startRow - d)); } return a; },
      // staircase right
      () => Array.from({length: width}, (_, i) => Math.min(n - 1, startRow + (i % 2))),
      // staircase left
      () => Array.from({length: width}, (_, i) => Math.max(0, startRow - (i % 2))),
    ];
    for (const gen of candidates) {
      if (result.length >= quota) break;
      const arr = gen();
      if (!arr.every(v => v >= 0 && v < n)) continue;
      const s = arr.join(', ');
      if (!seen.has(s)) { seen.add(s); result.push(s); }
    }
    // If still short, fill with straight line duplicates aren't possible so pad with straight
    while (result.length < quota) {
      const fallback = Array(width).fill(startRow).join(', ');
      if (!seen.has(fallback)) { seen.add(fallback); result.push(fallback); }
      else break;
    }
    return result;
  }

  const lines = [];
  for (let r = 0; r < n; r++) {
    const quota = rowQuota[r];
    if (quota > 0) lines.push(...patternsForRow(r, quota));
  }
  return lines;
}
