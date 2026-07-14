/* ── Strategy change and paytable management ── */

function onStrategyChange() {
  const strat = document.getElementById('rtp-strategy').value;
  const isWays = strat === 'WAYS';
  const isContacts = strat === 'SCATTERS';
  const noLines = isWays || isContacts;

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
  if (screenDimsToggleBtn) screenDimsToggleBtn.style.display = isWays ? '' : 'none';

  const wEl = document.getElementById('rtp-screen-width');
  const hEl = document.getElementById('rtp-screen-height');
  const mEl = document.getElementById('rtp-min-match');
  const newH = isContacts ? '5' : '3';
  const newM = isContacts ? '5' : '3';
  const oldH = isContacts ? '3' : '5';
  const oldM = isContacts ? '3' : '5';
  if (wEl) { wEl.placeholder = '5'; if (wEl.value === '5') wEl.value = ''; }
  if (hEl) {
    hEl.placeholder = newH;
    if (!hEl.value || hEl.value === oldH || hEl.value === newH) hEl.value = '';
  }
  if (mEl) {
    mEl.placeholder = newM;
    if (!mEl.value || mEl.value === oldM || mEl.value === newM) mEl.value = '';
  }

  const scattersSection = document.getElementById('scatters-paytable-section');
  if (scattersSection) {
    scattersSection.style.display = isContacts ? 'flex' : 'none';
    if (isContacts && document.getElementById('interval-sets-container').children.length === 0) {
      addIntervalSet(null, [{ from: 8, to: 10 }, { from: 11, to: 14 }, { from: 15, to: 19 }, { from: 20, to: 25 }]);
    }
  }

  if (isContacts) {
    document.querySelectorAll('.rtp-paytable-input').forEach(inp => {
      delete inp.dataset.fixedPlaceholder;
    });
  }

  document.querySelectorAll('.rtp-sym-row').forEach(row => {
    const typeSel = row.querySelector('select');
    const setSel = row.querySelector('.rtp-interval-set-sel');
    const isWild = typeSel && typeSel.value === 'WILD';
    const isScatter = typeSel && typeSel.value === 'SCATTER';
    const ptInput = row.querySelector('.rtp-paytable-input');

    if ((isScatter || (isWild && isContacts)) && ptInput) {
      ptInput.disabled = true;
      ptInput.placeholder = 'n/a';
      ptInput.value = '';
      if (setSel) setSel.style.display = 'none';
    } else {
      if (ptInput && !isScatter) ptInput.disabled = false;
      if (setSel) setSel.style.display = (isContacts && !isScatter && !isWild) ? '' : 'none';
    }

    if (!typeSel || !isWild) return;
    const wildFields = row.querySelector('.rtp-wild-fields');
    if (wildFields) wildFields.style.display = isWays ? 'none' : '';
    const aggSel = row.querySelector('.rtp-wild-agg');
    if (isWays || isContacts) {
      if (aggSel) { aggSel.value = 'NONE'; onWildAggChange(aggSel); }
    } else {
      if (aggSel && aggSel.value === 'NONE') { aggSel.value = 'ADD'; }
      if (aggSel) onWildAggChange(aggSel);
    }
  });
  if (isContacts) refreshIntervalSetDropdowns();
  else {
    const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
    const m = parseInt(document.getElementById('rtp-min-match').value) || 3;
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
    } else if (isWays) {
      tip.innerHTML = 'Defines the grid.' + gridRows +
        '<div class="tip-rule">WAYS pays for every combination of matching symbols across consecutive reels — no fixed paylines required.</div>';
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
    } else if (isWays) {
      symTip.innerHTML = paytableExample + wildBase +
        '<div class="tip-rule"><strong>WAYS strategy:</strong> ADD/MULTIPLY/SEQUENCE wild multipliers are disabled — ' +
        'they would double-count the wild\'s contribution, which is already reflected in the ways count. ' +
        '<em>Ways-with-ways-multipliers</em> (wild expands ways per reel) is a separate, orthogonal feature and is supported.</div>';
    } else {
      symTip.innerHTML = paytableExample + wildBase;
    }
  }
  updateSymConfigToggleBtn();
}

let _setCardCounter = 0;
function addIntervalSet(name, intervals) {
  const cid = _setCardCounter++;
  const container = document.getElementById('interval-sets-container');
  const card = document.createElement('div');
  card.className = 'interval-set-card';
  card.dataset.setId = cid;
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
  document.querySelectorAll('.interval-set-card').forEach((card, i) => {
    const lbl = card.querySelector('.interval-set-card-label');
    const name = 'int-set-' + (i + 1);
    if (lbl) lbl.textContent = name;
    card.dataset.setName = name;
  });
}

function removeIntervalSet(btn) {
  const card = btn.closest('.interval-set-card');
  if (document.getElementById('interval-sets-container').children.length <= 1) {
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
  return Array.from(document.querySelectorAll('.interval-set-card'))
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
  const cards = document.querySelectorAll('.interval-set-card');
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
}

function updatePaytablePlaceholders(w, m) {
  const strat = document.getElementById('rtp-strategy')?.value;
  const isScatters = strat === 'SCATTERS';

  const setIntervalCounts = {};
  if (isScatters) {
    document.querySelectorAll('.interval-set-card').forEach(card => {
      const name = card.dataset.setName || 'default';
      setIntervalCounts[name] = card.querySelectorAll('.scatter-interval-row').length;
    });
  }

  const defaultIntervalCount = isScatters
    ? (Object.values(setIntervalCounts)[0] || 1)
    : 0;
  const fallbackCount = isScatters ? defaultIntervalCount : Math.max(1, w - m + 1);
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
    if (inp.dataset.fixedPlaceholder) return;
    const row = inp.closest('.rtp-sym-row');
    const symId = row ? parseInt(row.dataset.symId) : 0;
    const typeVal = row && row.querySelector('select') ? row.querySelector('select').value : 'NORMAL';
    const isWild = typeVal === 'WILD';
    const isScatterSym = typeVal === 'SCATTER';

    let count;
    if (isScatters) {
      const setSel = row ? row.querySelector('.rtp-interval-set-sel') : null;
      const selName = setSel ? setSel.value : null;
      count = (selName && setIntervalCounts[selName]) ? setIntervalCounts[selName] : fallbackCount;
    } else {
      count = fallbackCount;
    }

    let ph;
    if (isScatterSym) {
      ph = Array.from({length: count}, () => '0').join(', ');
    } else if (isScatters) {
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

    const titleSuffix = isScatters
      ? count + ' value(s) required (one per interval in selected set)'
      : count + ' value(s) required (screenWidth − minMatch + 1)';

    inp.placeholder = ph;
    inp.title = titleSuffix;
  });
}

function generateDefaultLines(width, height) {
  const lines = [];
  const mid = Math.floor(height / 2);
  lines.push(Array(width).fill(mid).join(', '));
  if (height >= 3) {
    lines.push(Array(width).fill(0).join(', '));
    lines.push(Array(width).fill(height - 1).join(', '));
  }
  return lines;
}
