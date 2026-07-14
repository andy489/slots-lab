/* ── Symbol configuration ── */

let _symRowCounter = 0;
function addSymbolRow(symbolId) {
  const rid = _symRowCounter++;
  const id = symbolId != null ? symbolId : (rid + 1);
  const container = document.getElementById('rtp-symbol-rows');
  const row = document.createElement('div');
  row.className = 'rtp-sym-row';
  row.dataset.symId = id;
  row.id = 'rtp-sym-' + rid;
  row.innerHTML = `
    <span class="rtp-sym-id">${id}</span>
    <select onchange="onSymbolTypeChange(this)">
      <option value="NORMAL">Normal</option>
      <option value="WILD">Wild</option>
      <option value="SCATTER">Scatter</option>
    </select>
    <div class="rtp-paytable-cell">
      <select class="rtp-interval-set-sel" style="display:none;font-size:0.78rem" title="Interval set for this symbol" onchange="const ww=parseInt(document.getElementById('rtp-screen-width').value)||5,mm=parseInt(document.getElementById('rtp-min-match').value)||3;updatePaytablePlaceholders(ww,mm)"></select>
      <input type="text" class="array-input rtp-paytable-input" placeholder="0.5, 2.0, 5.0" value=""/>
    </div>
    <button class="icon-btn danger" onclick="removeSymbolRow('rtp-sym-${rid}')">
      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M9 6V4h6v2"/></svg>
    </button>
    <div class="rtp-wild-fields">
      <div class="rtp-wild-top-row">
        <label>Agg</label>
        <select class="rtp-wild-agg" title="Multiplier aggregation" onchange="onWildAggChange(this)">
          <option value="NONE">None</option>
          <option value="ADD">Additive</option>
          <option value="MULTIPLY">Multiplicative</option>
          <option value="SEQUENCE">Sequence</option>
        </select>
        <div class="rtp-wild-mult-group">
          <label>Wild ×</label>
          <input type="number" class="rtp-wild-mult" value="1" min="0.01" step="0.01" title="Wild multiplier"/>
        </div>
      </div>
      <div class="rtp-seq-row">
        <label class="rtp-seq-label">Seq</label>
        <input type="text" class="rtp-wild-seq array-input" placeholder="1.0, 2.0, 3.0" title="Multiplier per wild count (max entries = screen width)"/>
      </div>
    </div>
  `;
  container.appendChild(row);
  const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
  const m = parseInt(document.getElementById('rtp-min-match').value) || 3;
  const SYM_DEFAULTS = {
    1: { type: 'SCATTER', paytable: '' },
    2: { type: 'WILD',    paytable: '5.0, 10.0, 50.0', agg: 'NONE' },
    3: { type: 'NORMAL',  paytable: '2.5, 5.0, 20.0' },
    4: { type: 'NORMAL',  paytable: '1.0, 3.0, 10.0' },
    5: { type: 'NORMAL',  paytable: '0.5, 2.0, 5.0' },
    6: { type: 'NORMAL',  paytable: '0.2, 1.0, 2.5' },
    7: { type: 'NORMAL',  paytable: '0.1, 0.5, 1.5' },
  };
  if (SYM_DEFAULTS[id]) {
    const def = SYM_DEFAULTS[id];
    const sel = row.querySelector('select');
    sel.value = def.type;
    onSymbolTypeChange(sel);
    if (def.agg) {
      const aggSel = row.querySelector('.rtp-wild-agg');
      if (aggSel) {
        const strat = document.getElementById('rtp-strategy')?.value;
        const resolvedAgg = (def.agg === 'NONE' && strat !== 'WAYS' && strat !== 'SCATTERS') ? 'ADD' : def.agg;
        aggSel.value = resolvedAgg;
        onWildAggChange(aggSel);
      }
    }
    updatePaytablePlaceholders(w, m);
    const strat2 = document.getElementById('rtp-strategy')?.value;
    if (def.paytable && strat2 !== 'SCATTERS') {
      const ptInput = row.querySelector('.rtp-paytable-input');
      if (ptInput) { ptInput.placeholder = def.paytable; ptInput.dataset.fixedPlaceholder = '1'; }
    }
  } else {
    updatePaytablePlaceholders(w, m);
  }
  const isContacts = document.getElementById('rtp-strategy')?.value === 'SCATTERS';
  if (isContacts) {
    const setSel = row.querySelector('.rtp-interval-set-sel');
    const typeSel = row.querySelector('select');
    const typeVal = typeSel ? typeSel.value : 'NORMAL';
    if (setSel) {
      const names = getIntervalSetNames();
      setSel.innerHTML = names.map(n => `<option value="${escapeHtml(n)}">${escapeHtml(n)}</option>`).join('');
      setSel.style.display = (typeVal === 'WILD' || typeVal === 'SCATTER') ? 'none' : '';
    }
  }
  updateSpinTestPlaceholders();
  return row;
}

function onSymbolTypeChange(sel) {
  const row = sel.closest('.rtp-sym-row');
  const ptInput = row.querySelector('input[type=text]');
  const wildFields = row.querySelector('.rtp-wild-fields');
  const setSel = row.querySelector('.rtp-interval-set-sel');
  const isWild = sel.value === 'WILD';
  const isScatter = sel.value === 'SCATTER';
  const isContacts = document.getElementById('rtp-strategy')?.value === 'SCATTERS';

  if (isScatter || (isWild && isContacts)) {
    ptInput.disabled = true;
    ptInput.placeholder = 'n/a';
    ptInput.value = '';
    if (setSel) setSel.style.display = 'none';
  } else if (isScatter) {
    ptInput.disabled = true;
    ptInput.placeholder = 'n/a';
    ptInput.value = '';
    if (setSel) setSel.style.display = 'none';
  } else {
    ptInput.disabled = false;
    const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
    const m = parseInt(document.getElementById('rtp-min-match').value) || 3;
    updatePaytablePlaceholders(w, m);
    if (setSel) setSel.style.display = (isContacts && !isWild) ? '' : 'none';
  }

  wildFields.classList.toggle('visible', isWild);
  const aggSel = row.querySelector('.rtp-wild-agg');
  if (isWild) {
    if (aggSel) onWildAggChange(aggSel);
    const isWays = document.getElementById('rtp-strategy').value === 'WAYS';
    if (isWays) {
      wildFields.style.display = 'none';
      if (aggSel) { aggSel.value = 'NONE'; onWildAggChange(aggSel); }
    }
  } else {
    if (aggSel) { aggSel.value = 'NONE'; onWildAggChange(aggSel); }
  }
}

function onWildAggChange(aggSel) {
  const row = aggSel.closest('.rtp-sym-row');
  const val = aggSel.value;
  const isSeq = val === 'SEQUENCE';
  const hideMultiplier = val === 'NONE' || isSeq;
  row.querySelector('.rtp-seq-row').classList.toggle('visible', isSeq);
  row.querySelector('.rtp-wild-mult-group').classList.toggle('hidden', hideMultiplier);
  if (isSeq) {
    const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
    const seqInput = row.querySelector('.rtp-wild-seq');
    seqInput.placeholder = Array.from({length: w}, (_, i) => (i + 1).toFixed(1)).join(', ');
  }
}

function removeSymbolRow(rowId) {
  const el = document.getElementById(rowId);
  if (el) el.remove();
  updateSymConfigToggleBtn();
  updateSpinTestPlaceholders();
}

function refreshSymbolRowNumbers() {
  document.querySelectorAll('.rtp-sym-row').forEach(row => {
    const lbl = row.querySelector('.rtp-sym-id');
    if (lbl) lbl.textContent = row.dataset.symId;
  });
}
