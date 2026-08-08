/* ── Reel Set Form Builder ── */
let reelSetCounter = 0;

function addReelSet(data, scroll) {
  const idx = reelSetCounter++;
  const id = 'rs-' + idx;

  const tc = (data && data.tilesCounts) ? data.tilesCounts : [
    null, null, null, null, null
  ];

  const restrictions = (data && data.restrictions) ? data.restrictions : [
    null
  ];

  const card = document.createElement('div');
  card.className = 'reel-set-card';
  card.id = id;

  card.innerHTML = `
    <div class="reel-set-card-header">
      <span class="reel-set-name">${t('gen.reel_set_label')}${idx}</span>
      <div style="display:flex;gap:0.35rem;align-items:center">
        <button class="icon-btn" id="${id}-toggle-btn" onclick="toggleDefaultsClear('${id}')" title="${t('gen.fill_defaults')}">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
        </button>
        <button class="icon-btn add" onclick="addReelRow('${id}-reels', null, true)" title="${t('gen.add_reel')}">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
        <button class="icon-btn danger" onclick="removeReelSet('${id}')" title="${t('gen.remove_reel_set')}">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/></svg>
        </button>
      </div>
    </div>
    <div class="reel-set-card-body">
      <div>
        <div class="reels-section-label">${t('gen.reels_tiles')}</div>
        <div class="reel-rows" id="${id}-reels"></div>
      </div>
      <div>
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:0.35rem">
          <div class="reels-section-label"><span>${t('gen.restrictions')}</span><span class="stat-tip-wrap" style="margin-left:0.3rem"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:290px">Controls how symbols are stacked on each reel.<div class="tip-rule"><strong>Stack Sizes</strong> — how many consecutive identical symbols to place (e.g. 1, 2, 3).<br><strong>Chances</strong> — weighted probability for each stack size.<br><strong>Min Distance</strong> — minimum number of <em>other</em> symbols (different tile IDs) that must appear between two stacks of the same symbol. e.g. dist=4 means after a stack of symbol A, at least 4 non-A symbols must follow before A can appear again. The gap positions are filled with symbols whose ID differs from both the symbol ending the previous stack and the symbol starting the next one.</div><div class="tip-rule"><strong>Mapping:</strong> restrictions cycle across reels. 1 restriction → same rule for every reel. 2 restrictions → first applies to reels 1, 3, 5… and second to reels 2, 4, 6… and so on for more.</div><div class="tip-rule" style="font-family:monospace;font-size:0.65rem">e.g. sizes=[1,2,3] chances=[50,30,20] dist=4<br>→ 50% single, 30% double, 20% triple<br>→ A A _ _ _ _ A A A (✓ 4 gaps between stacks)</div></span></span></div>
          <button class="icon-btn add" onclick="addRestrictionGuarded('${id}-restrictions', '${id}-reels')" title="${t('gen.add_restriction')}">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          </button>
        </div>
        <div class="restrictions-list" id="${id}-restrictions"></div>
      </div>
    </div>
  `;

  document.getElementById('reel-set-list').appendChild(card);
  if (scroll) card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

  tc.forEach((row, ri) => addReelRow(id + '-reels', row));
  restrictions.forEach(r => addRestriction(id + '-restrictions', r));

  card.addEventListener('input', () => updateToggleBtn(id));
  updateToggleBtn(id);
}

let reelRowCounter = 0;
function addReelRow(containerId, data, scroll) {
  const ri = reelRowCounter++;
  const container = document.getElementById(containerId);
  const numReels = container.querySelectorAll('.reel-row').length;
  const rowDiv = document.createElement('div');
  rowDiv.className = 'reel-row';
  rowDiv.id = 'rr-' + ri;

  const defaultRows = [
    '8, 34, 44, 16, 68, 72, 23',
    '20, 11, 44, 54, 20, 72, 76',
    '18, 34, 12, 54, 68, 24, 76',
    '22, 34, 16, 54, 68, 27, 76',
    '22, 34, 16, 54, 68, 27, 76',
  ];
  const placeholder = defaultRows[numReels % defaultRows.length];

  rowDiv.innerHTML = `
    <span class="reel-row-num">${numReels + 1}</span>
    <input type="text" class="reel-input array-input" value="${(data || []).join(', ') || ''}" placeholder="${placeholder}"/>
    <button class="icon-btn danger" onclick="removeRow('rr-${ri}', '${containerId}')">
      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M9 6V4h6v2"/></svg>
    </button>
  `;
  container.appendChild(rowDiv);
  if (scroll) rowDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  renumberReels(containerId);
  const reelsEl = document.getElementById(containerId);
  const reelSetCard = reelsEl ? reelsEl.closest('.reel-set-card') : null;
  if (reelSetCard) renumberRestrictions(reelSetCard.id + '-restrictions');
}

function renumberReels(containerId) {
  document.getElementById(containerId).querySelectorAll('.reel-row').forEach((row, i) => {
    row.querySelector('.reel-row-num').textContent = i + 1;
  });
}

function removeRow(rowId, containerId) {
  document.getElementById(rowId)?.remove();
  renumberReels(containerId);
  const reelsEl = document.getElementById(containerId);
  const reelSetCard = reelsEl ? reelsEl.closest('.reel-set-card') : null;
  if (reelSetCard) renumberRestrictions(reelSetCard.id + '-restrictions');
}

let restrictionCounter = 0;
function addRestriction(containerId, data, scroll) {
  const ri = restrictionCounter++;
  const container = document.getElementById(containerId);

  const card = document.createElement('div');
  card.className = 'restriction-card';
  card.id = 'restr-' + ri;

  const stacks  = (data && data.stackSizes)   ? data.stackSizes.join(', ')   : '';
  const chances = (data && data.stackChances) ? data.stackChances.join(', ') : '';
  const dist    = (data && data.minDistance  != null) ? data.minDistance : '';

  card.innerHTML = `
    <div class="restriction-header">
      <div class="restr-applies-wrap">
        <span class="restriction-label">#1</span>
        <span class="restr-applies" title="Reels this restriction applies to"></span>
      </div>
      <button class="icon-btn danger" onclick="removeRestriction('restr-${ri}', '${containerId}')" title="${t('gen.remove')}">
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M9 6V4h6v2"/></svg>
      </button>
    </div>
    <div class="restriction-body">
      <div class="restriction-row">
        <span class="restriction-row-label">stackSizes</span>
        <input type="text" class="array-input" value="${stacks}" placeholder="1, 2, 3"/>
      </div>
      <div class="restriction-row">
        <span class="restriction-row-label">stackChances</span>
        <input type="text" class="array-input" value="${chances}" placeholder="50, 30, 20"/>
      </div>
      <div class="restriction-row">
        <span class="restriction-row-label">minDistance</span>
        <input type="text" class="array-input dist-input" value="${dist}" placeholder="1"/>
      </div>
    </div>
  `;
  container.appendChild(card);
  if (scroll) card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  renumberRestrictions(containerId);
}

function renumberRestrictions(containerId) {
  const cards = document.querySelectorAll('#' + containerId + ' .restriction-card');
  const k = cards.length;
  const restrList = document.getElementById(containerId);
  const reelSetCard = restrList ? restrList.closest('.reel-set-card') : null;
  const reelsContainerId = reelSetCard ? reelSetCard.id + '-reels' : null;
  const reelCount = reelsContainerId
    ? document.getElementById(reelsContainerId).querySelectorAll('.reel-row').length : 0;
  cards.forEach((card, i) => {
    card.querySelector('.restriction-label').textContent = t('gen.restriction_label') + (i + 1);
    const badge = card.querySelector('.restr-applies');
    if (!badge) return;
    if (k === 0 || reelCount === 0) { badge.textContent = ''; return; }
    const reels = [];
    for (let r = 0; r < reelCount; r++) {
      if (r % k === i) reels.push('R' + (r + 1));
    }
    badge.textContent = reels.join(', ');
    badge.title = t('gen.applies_to') + reels.join(', ');
  });
}

function removeRestriction(cardId, containerId) {
  document.getElementById(cardId)?.remove();
  renumberRestrictions(containerId);
}

function removeReelSet(id) {
  document.getElementById(id)?.remove();
  renumberReelSets();
}

function renumberReelSets() {
  document.querySelectorAll('#reel-set-list .reel-set-card').forEach((card, i) => {
    card.querySelector('.reel-set-name').textContent = t('gen.reel_set_label') + i;
  });
}

function addRestrictionGuarded(restrictionsId, reelsId) {
  const reelCount   = document.getElementById(reelsId).querySelectorAll('.reel-row').length;
  const restrCount  = document.getElementById(restrictionsId).querySelectorAll('.restriction-card').length;
  if (restrCount >= reelCount) {
    showToast(t('gen.restr_exceeds_reels') + ' (' + reelCount + ')', true);
    return;
  }
  addRestriction(restrictionsId, null, true);
  const cardId = restrictionsId.replace('-restrictions', '');
  updateToggleBtn(cardId);
}

function parseNumArray(str) {
  if (!str || !str.trim()) return [];
  return str.split(',').map(s => parseFloat(s.trim()));
}

/* ── Fill/clear toggle ── */
const CLEAR_ICON = `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`;
const DEFAULTS_ICON = `<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>`;

function cardHasValues(cardId) {
  const card = document.getElementById(cardId);
  if (!card) return false;
  return Array.from(card.querySelectorAll('input[placeholder]')).some(inp => inp.value.trim() !== '');
}

function updateToggleBtn(cardId) {
  const btn = document.getElementById(cardId + '-toggle-btn');
  if (!btn) return;
  const hasValues = cardHasValues(cardId);
  btn.innerHTML = hasValues ? CLEAR_ICON : DEFAULTS_ICON;
  btn.title = hasValues ? t('gen.clear_values') : t('gen.fill_defaults');
  btn.classList.toggle('danger', hasValues);
}

function toggleDefaultsClear(cardId) {
  const card = document.getElementById(cardId);
  if (!card) return;
  if (cardHasValues(cardId)) {
    card.querySelectorAll('input[placeholder]').forEach(inp => { inp.value = ''; });
  } else {
    card.querySelectorAll('input[placeholder]').forEach(inp => { inp.value = inp.placeholder; });
  }
  updateToggleBtn(cardId);
}

/* ── Build config from form ── */
function buildConfig() {
  const strategy = document.getElementById('f-strategy').value;

  const reelSets = [];

  document.querySelectorAll('#reel-set-list .reel-set-card').forEach(card => {
    const tilesCounts = Array.from(card.querySelectorAll('.reel-rows .reel-row input')).map(inp => {
      const raw = inp.value.trim() || inp.placeholder;
      return parseNumArray(raw);
    });

    const restrictions = Array.from(card.querySelectorAll('.restrictions-list .restriction-card')).map(rc => {
      const inputs = rc.querySelectorAll('input');
      const stacksRaw  = inputs[0].value.trim() || inputs[0].placeholder;
      const chancesRaw = inputs[1].value.trim() || inputs[1].placeholder;
      const distRaw    = inputs[2].value.trim()  || inputs[2].placeholder || '1';
      return {
        stackSizes:   parseNumArray(stacksRaw),
        stackChances: parseNumArray(chancesRaw),
        minDistance:  parseInt(distRaw) || 1
      };
    });

    reelSets.push({ tilesCounts, restrictions });
  });

  return { strategy, reelSets };
}

/* ── Validate ── */
function validateConfig() {
  clearErrors();
  const errors = [];

  const reelSetCards = document.querySelectorAll('#reel-set-list .reel-set-card');
  if (reelSetCards.length === 0) {
    errors.push(t('gen.at_least_one_reel_set'));
  }

  reelSetCards.forEach((card, si) => {
    const reelInputs = card.querySelectorAll('.reel-rows .reel-row input');
    if (reelInputs.length === 0) {
      errors.push(t('gen.reel_set_label') + si + ': ' + t('gen.at_least_one_reel'));
    }
    reelInputs.forEach((inp, ri) => {
      const raw = inp.value.trim() || inp.placeholder;
      const nums = parseNumArray(raw);
      if (nums.length === 0) {
        errors.push(markError(inp, t('gen.reel_set_label') + si + ' R' + (ri+1) + ': ' + t('gen.tile_counts_empty')));
      } else if (nums.some(isNaN)) {
        errors.push(markError(inp, t('gen.reel_set_label') + si + ' R' + (ri+1) + ': ' + t('gen.tile_counts_must_be_numbers')));
      } else if (nums.some(n => n < 0)) {
        errors.push(markError(inp, t('gen.reel_set_label') + si + ' R' + (ri+1) + ': ' + t('gen.tile_counts_must_be_gte0')));
      }
    });

    const restrCards = card.querySelectorAll('.restrictions-list .restriction-card');
    if (restrCards.length > reelInputs.length) {
      errors.push(t('gen.reel_set_label') + si + ': ' + t('gen.restr_exceeds_reel_count') + ' (' + restrCards.length + ' > ' + reelInputs.length + ')');
    }

    restrCards.forEach((rc, ri) => {
      const inputs = rc.querySelectorAll('input');
      const stacksEl   = inputs[0];
      const chancesEl  = inputs[1];
      const distEl     = inputs[2];

      const stacks  = parseNumArray(stacksEl.value.trim()  || stacksEl.placeholder);
      const chances = parseNumArray(chancesEl.value.trim() || chancesEl.placeholder);

      if (stacks.length === 0) {
        errors.push(markError(stacksEl, t('gen.reel_set_label') + si + ' ' + t('gen.restriction_label') + (ri+1) + ': ' + t('gen.stack_sizes_empty')));
      } else if (stacks.some(isNaN) || stacks.some(n => n <= 0)) {
        errors.push(markError(stacksEl, t('gen.reel_set_label') + si + ' ' + t('gen.restriction_label') + (ri+1) + ': ' + t('gen.stack_sizes_positive')));
      }

      if (chances.length === 0) {
        errors.push(markError(chancesEl, t('gen.reel_set_label') + si + ' ' + t('gen.restriction_label') + (ri+1) + ': ' + t('gen.stack_chances_empty')));
      } else if (chances.some(isNaN) || chances.some(n => n < 0)) {
        errors.push(markError(chancesEl, t('gen.reel_set_label') + si + ' ' + t('gen.restriction_label') + (ri+1) + ': ' + t('gen.stack_chances_gte0')));
      } else {
        const chanceSum = chances.reduce((a, b) => a + b, 0);
        if (Math.abs(chanceSum - 100) > 0.05) {
          errors.push(markError(chancesEl, t('gen.reel_set_label') + si + ' ' + t('gen.restriction_label') + (ri+1) + ': ' + t('gen.stack_chances_sum100') + ' (current: ' + chanceSum.toFixed(1) + ')'));
        }
      }

      if (stacks.length > 0 && chances.length > 0 && stacks.length !== chances.length) {
        errors.push(markError(chancesEl, t('gen.reel_set_label') + si + ' ' + t('gen.restriction_label') + (ri+1) + ': ' + t('gen.stack_equal_length')));
      }

      const dist = parseInt(distEl.value.trim() || distEl.placeholder || '1');
      if (isNaN(dist) || dist < 0) {
        errors.push(markError(distEl, t('gen.reel_set_label') + si + ' ' + t('gen.restriction_label') + (ri+1) + ': ' + t('gen.min_dist_gte0')));
      }
    });
  });

  return errors;
}

/* ── History ── */
function getMaxHistory() {
  const v = parseInt(document.getElementById('f-history-size')?.value, 10);
  return ([1, 3, 5, 10, 20].includes(v)) ? v : 5;
}

async function pushHistory(result, config) {
  const now = new Date();
  const time = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const id = Date.now().toString();
  await fetch('/api/history/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id, strategy: config.strategy, time, result, config: JSON.stringify(config) })
  });
  await fetch(`/api/history/generate/resize?size=${getMaxHistory()}`, { method: 'PUT' });
  await loadHistory();
}

async function loadHistory() {
  const res = await fetch('/api/history/generate');
  const entries = await res.json();
  renderHistory(entries);
}

function renderHistory(entries) {
  const list = document.getElementById('history-list');
  const empty = document.getElementById('history-empty');
  list.querySelectorAll('.history-item').forEach(el => el.remove());
  if (!entries || entries.length === 0) {
    empty.style.display = '';
    return;
  }
  empty.style.display = 'none';
  entries.forEach((entry, i) => {
    const item = document.createElement('div');
    item.className = 'history-item';
    item.title = t('history.click_restore');
    item.innerHTML = `
      <button class="history-item-del" title="${t('history.delete_entry')}">
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M9 6V4h6v2"/></svg>
      </button>
      <span class="history-item-label">#${i + 1} &middot; ${entry.strategy}</span>
      <span class="history-item-meta">${entry.time}</span>
    `;
    item.querySelector('.history-item-del').onclick = async (e) => {
      e.stopPropagation();
      if (!await confirmDelete(t('gen.delete_entry') + (i + 1) + '?')) return;
      const res = await fetch(`/api/history/generate/${encodeURIComponent(entry.id)}`, { method: 'DELETE' });
      renderHistory(await res.json());
    };
    item.onclick = () => {
      genOutput.setValue(entry.result);
      storeGeneratedReels(entry.result);
      if (entry.config) {
        try { restoreGenerateForm(JSON.parse(entry.config)); } catch(e) {}
      }
      setStatus('gen', true, t('gen.restored'));
    };
    list.appendChild(item);
  });
}

async function onHistorySizeChange() {
  const size = getMaxHistory();
  const [genRes, simRes] = await Promise.all([
    fetch(`/api/history/generate/resize?size=${size}`, { method: 'PUT' }),
    fetch(`/api/history/simulate/resize?size=${size}`, { method: 'PUT' })
  ]);
  renderHistory(await genRes.json());
  renderRtpHistory(await simRes.json());
}

async function clearHistory() {
  if (!await confirmDelete(t('gen.delete_history'))) return;
  await fetch('/api/history/generate', { method: 'DELETE' });
  renderHistory([]);
}

/* ── Restore form from saved config ── */
function restoreGenerateForm(config) {
  if (!config) return;
  const strat = document.getElementById('f-strategy');
  if (strat && config.strategy) strat.value = config.strategy;
  if (config.reelSets && config.reelSets.length > 0) {
    document.getElementById('reel-set-list').innerHTML = '';
    reelSetCounter = 0;
    config.reelSets.forEach(rs => addReelSet(rs));
  }
}

function clearGenerateResult() {
  genOutput.setValue('');
  setStatus('gen', false, '');
  _latestReelSets = null;
  document.getElementById('rtp-chances-list').innerHTML =
    '<span style="font-size:0.72rem;color:var(--text3);font-style:italic">' + t('rtp.generate_reels_first') + '</span>';
  document.getElementById('rtp-chance-total').textContent = '';
  document.getElementById('rtp-chance-total').className = 'rtp-chance-total';
  document.getElementById('rtp-symbol-rows').innerHTML = '';
  document.getElementById('rtp-lines-list').innerHTML = '';
  updateLineCount();
  _symRowCounter = 0;
  _lineCounter = 0;
  setStatus('rtp', false, '');
  const resultBody = document.getElementById('rtp-result-body');
  if (resultBody) {
    resultBody.innerHTML = `
      <div class="rtp-empty">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color:var(--text3)"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <span>${t('rtp.configure_run')}</span>
      </div>`;
  }
}

/* ── Copy from Generate → Convert input ── */
function copyFromGenerate() {
  if (!_latestReelSets || _latestReelSets.length === 0) {
    showToast(t('gen.no_reels_for_convert'), true);
    return;
  }
  const isMegaways = document.getElementById('rtp-strategy')?.value === 'MEGAWAYS';
  let sets = _latestReelSets;
  if (isMegaways) {
    const heightChances = collectMegawaysHeightChances();
    sets = _latestReelSets.map((rs, s) => {
      const reelChances = heightChances && heightChances[s]
        ? heightChances[s].map(v => v !== null ? v : null)
        : null;
      if (reelChances && reelChances.some(v => v !== null)) {
        return { setName: rs.setName, reelSet: rs.reelSet, reelTileChances: reelChances };
      }
      return rs;
    });
  }
  const json = JSON.stringify(sets, null, 2)
    .replace(/\[\s*([\d,\s]+?)\s*\]/g, m => '[' + m.slice(1, -1).trim().replace(/\s+/g, ' ') + ']');
  convInput.setValue(json);
  showToast(t('gen.copied_from_generate'));
}

function injectReelSetChances() {
  if (!_latestReelSets || _latestReelSets.length === 0) {
    showToast(t('gen.no_reels_inject'), true);
    return;
  }
  const hasChances = _latestReelSets.some((_, i) => document.getElementById('rtp-chance-' + i));
  if (!hasChances) {
    showToast(t('gen.no_chances_inject'), true);
    return;
  }
  const isMegaways = document.getElementById('rtp-strategy')?.value === 'MEGAWAYS';
  const heightChances = isMegaways ? collectMegawaysHeightChances() : null;
  _latestReelSets = _latestReelSets.map((rs, i) => {
    const el = document.getElementById('rtp-chance-' + i);
    const chance = el ? parseFloat(el.value) : null;
    const result = { ...rs };
    if (chance != null && !isNaN(chance)) result.chance = chance;
    if (heightChances && heightChances[i]) {
      const reelChances = heightChances[i].map(v => v !== null ? v : null);
      if (reelChances.some(v => v !== null)) result.reelTileChances = reelChances;
    }
    return result;
  });
  const json = JSON.stringify(_latestReelSets, null, 2)
    .replace(/\[\s*([\d,\s]+?)\s*\]/g, m => '[' + m.slice(1, -1).trim().replace(/\s+/g, ' ') + ']');
  genOutput.setValue(json);
  showToast(t('gen.chances_injected'));
}

/* ── API ── */
async function runGenerate() {
  const btn = document.getElementById('gen-btn');
  const errs = validateConfig();
  if (errs.length > 0) {
    showToast(errs[0], true);
    setStatus('gen', false, errs[0]);
    return;
  }
  btn.disabled = true;
  setStatus('gen', null, t('gen.running'));
  const config = buildConfig();
  try {
    const res = await fetch('/api/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ config })
    });
    const d = await res.json();
    if (d.error) { setStatus('gen', false, d.error); genOutput.setValue(''); }
    else         { setStatus('gen', true, t('gen.done'));   genOutput.setValue(d.result); storeGeneratedReels(d.result); await pushHistory(d.result, config); }
  } catch(e) { setStatus('gen', false, t('gen.network_error')); }
  finally { btn.disabled = false; }
}

/* ── Import tile counts ── */
function openImportCounts() {
  document.getElementById('import-counts-input').value = '';
  document.getElementById('import-counts-error').style.display = 'none';
  document.getElementById('import-counts-modal').classList.add('open');
  setTimeout(() => document.getElementById('import-counts-input').focus(), 50);
}
function closeImportCounts() {
  document.getElementById('import-counts-modal').classList.remove('open');
}
function applyImportCounts() {
  const raw = document.getElementById('import-counts-input').value.trim();
  const errEl = document.getElementById('import-counts-error');
  errEl.style.display = 'none';

  let parsed;
  try { parsed = JSON.parse(raw); } catch (e) {
    errEl.textContent = t('gen.import_invalid_json') + e.message;
    errEl.style.display = '';
    return;
  }
  if (!Array.isArray(parsed) || parsed.length === 0) {
    errEl.textContent = t('gen.import_not_array');
    errEl.style.display = '';
    return;
  }
  for (const rs of parsed) {
    if (!Array.isArray(rs.reelSetTileCounts)) {
      errEl.textContent = t('gen.import_no_counts');
      errEl.style.display = '';
      return;
    }
  }

  const list = document.getElementById('reel-set-list');
  list.innerHTML = '';
  reelSetCounter = 0;
  reelRowCounter = 0;

  for (const rs of parsed) {
    addReelSet({ tilesCounts: rs.reelSetTileCounts, restrictions: [] }, false);
  }

  closeImportCounts();
}
