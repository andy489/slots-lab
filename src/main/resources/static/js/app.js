/* ── Theme ── */
let isDark = false;
function toggleTheme() {
  isDark = !isDark;
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
  document.getElementById('icon-moon').style.display = isDark  ? '' : 'none';
  document.getElementById('icon-sun').style.display  = !isDark ? '' : 'none';
  [genOutput, convInput, convOutput].forEach(cm => cm && cm.refresh());
}

/* ── CodeMirror ── */
const cmOpts = {
  mode: { name: 'javascript', json: true },
  theme: 'rsg',
  lineNumbers: true,
  matchBrackets: true,
  autoCloseBrackets: true,
  indentUnit: 2,
  tabSize: 2,
};
const genOutput      = CodeMirror.fromTextArea(document.getElementById('gen-output'),        { ...cmOpts, readOnly: true });
const convInput      = CodeMirror.fromTextArea(document.getElementById('conv-input'),        { ...cmOpts });
const convOutput     = CodeMirror.fromTextArea(document.getElementById('conv-output'),       { ...cmOpts, readOnly: true });

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
      <span class="reel-set-name">Reel Set #${idx}</span>
      <div style="display:flex;gap:0.35rem;align-items:center">
        <button class="icon-btn" id="${id}-toggle-btn" onclick="toggleDefaultsClear('${id}')" title="Fill all fields with default values">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
        </button>
        <button class="icon-btn add" onclick="addReelRow('${id}-reels', null, true)" title="Add reel">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
        <button class="icon-btn danger" onclick="removeReelSet('${id}')" title="Remove reel set">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/></svg>
        </button>
      </div>
    </div>
    <div class="reel-set-card-body">
      <div>
        <div class="reels-section-label">Reels (Tiles Count)</div>
        <div class="reel-rows" id="${id}-reels"></div>
      </div>
      <div>
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:0.35rem">
          <div class="reels-section-label">Restrictions<span class="stat-tip-wrap" style="margin-left:0.3rem"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:290px">Controls how symbols are stacked on each reel.<div class="tip-rule"><strong>Stack Sizes</strong> — how many consecutive identical symbols to place (e.g. 1, 2, 3).<br><strong>Chances</strong> — weighted probability for each stack size.<br><strong>Min Distance</strong> — minimum number of <em>other</em> symbols (different tile IDs) that must appear between two stacks of the same symbol. e.g. dist=4 means after a stack of symbol A, at least 4 non-A symbols must follow before A can appear again. The gap positions are filled with symbols whose ID differs from both the symbol ending the previous stack and the symbol starting the next one.</div><div class="tip-rule"><strong>Mapping:</strong> restrictions cycle across reels. 1 restriction → same rule for every reel. 2 restrictions → first applies to reels 1, 3, 5… and second to reels 2, 4, 6… and so on for more.</div><div class="tip-rule" style="font-family:monospace;font-size:0.65rem">e.g. sizes=[1,2,3] chances=[50,30,20] dist=4<br>→ 50% single, 30% double, 20% triple<br>→ A A _ _ _ _ A A A (✓ 4 gaps between stacks)</div></span></span></div>
          <button class="icon-btn add" onclick="addRestrictionGuarded('${id}-restrictions', '${id}-reels')" title="Add restriction">
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
  const val = data ? (Array.isArray(data) ? data.join(', ') : data) : '';
  const placeholder = defaultRows[numReels] || defaultRows[defaultRows.length - 1];
  rowDiv.innerHTML = `
    <span class="reel-row-label">R${numReels + 1}</span>
    <input type="text" class="array-input" value="${val}" placeholder="${placeholder}"/>
    <button class="icon-btn danger" onclick="removeRow('rr-${ri}', '${containerId}')" title="Remove reel">
      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M9 6V4h6v2"/></svg>
    </button>
  `;
  container.appendChild(rowDiv);
  if (scroll) rowDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  renumberReels(containerId);
}

function renumberReels(containerId) {
  const rows = document.querySelectorAll('#' + containerId + ' .reel-row-label');
  rows.forEach((el, i) => { el.textContent = 'R' + (i + 1); });
  /* recompute restriction badges — restrictions containerId is same prefix with -restrictions */
  const restrictionsId = containerId.replace(/-reels$/, '-restrictions');
  if (document.getElementById(restrictionsId)) renumberRestrictions(restrictionsId);
}

function removeRow(rowId, containerId) {
  const el = document.getElementById(rowId);
  if (el) el.remove();
  renumberReels(containerId);
}

let restrictionCounter = 0;
function addRestriction(containerId, data, scroll) {
  const ri = restrictionCounter++;
  const numR = document.getElementById(containerId).querySelectorAll('.restriction-card').length;
  const card = document.createElement('div');
  card.className = 'restriction-card';
  card.id = 'restr-' + ri;

  const stacks   = data ? (data.stackSizes   || data.stacks   || []) : [];
  const chances  = data ? (data.stackChances || data.chances  || []) : [];
  const minDist  = data ? (data.minDistance  !== undefined ? data.minDistance : (data.distance !== undefined ? data.distance : '')) : '';

  card.innerHTML = `
    <div class="restriction-header">
      <div class="restr-applies-wrap">
        <span class="restriction-label">Restriction #${numR + 1}</span>
        <span class="restr-applies" title="Reels this restriction applies to"></span>
      </div>
      <button class="icon-btn danger" onclick="removeRestriction('restr-${ri}', '${containerId}')" title="Remove">
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M9 6V4h6v2"/></svg>
      </button>
    </div>
    <div class="restriction-body">
      <div class="restriction-row">
        <span class="restriction-row-label">stackSizes</span>
        <input type="text" class="array-input" value="${stacks.length ? stacks.join(', ') : ''}" placeholder="1, 2, 3, 4, 5"/>
      </div>
      <div class="restriction-row">
        <span class="restriction-row-label">stackChances</span>
        <input type="text" class="array-input" value="${chances.length ? chances.join(', ') : ''}" placeholder="20, 32, 32, 10, 6"/>
      </div>
      <div class="restriction-row">
        <span class="restriction-row-label">minDistance</span>
        <input type="text" class="array-input dist-input" value="${minDist}" placeholder="1"/>
      </div>
    </div>
  `;
  document.getElementById(containerId).appendChild(card);
  if (scroll) card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  renumberRestrictions(containerId);
}

function renumberRestrictions(containerId) {
  const cards = document.querySelectorAll('#' + containerId + ' .restriction-card');
  const k = cards.length;

  /* find reel count from the sibling reel-rows container in the same reel-set-card */
  const restrList = document.getElementById(containerId);
  const reelSetCard = restrList.closest('.reel-set-card');
  const reelsContainerId = reelSetCard ? reelSetCard.id + '-reels' : null;
  const reelCount = reelsContainerId
    ? document.getElementById(reelsContainerId).querySelectorAll('.reel-row').length
    : 0;

  cards.forEach((card, i) => {
    card.querySelector('.restriction-label').textContent = 'Restriction #' + (i + 1);

    const badge = card.querySelector('.restr-applies');
    if (!badge) return;

    if (k === 0 || reelCount === 0) { badge.textContent = ''; return; }

    /* build list of reel numbers (1-based) that use restriction i */
    const reels = [];
    for (let r = 0; r < reelCount; r++) {
      if (r % k === i) reels.push('R' + (r + 1));
    }
    badge.textContent = reels.join(', ');
    badge.title = 'Applies to: ' + reels.join(', ');
  });
}

function removeRestriction(cardId, containerId) {
  const el = document.getElementById(cardId);
  if (el) el.remove();
  renumberRestrictions(containerId);
}

function removeReelSet(id) {
  const el = document.getElementById(id);
  if (el) el.remove();
  renumberReelSets();
}

function renumberReelSets() {
  const names = document.querySelectorAll('.reel-set-name');
  names.forEach((el, i) => { el.textContent = 'Reel Set #' + i; });
}

/* ── Guarded add restriction ── */
function addRestrictionGuarded(restrictionsId, reelsId) {
  const reelCount = document.getElementById(reelsId).querySelectorAll('.reel-row').length;
  const restrCount = document.getElementById(restrictionsId).querySelectorAll('.restriction-card').length;
  if (restrCount >= reelCount) {
    showToast('Restrictions cannot exceed reel count (' + reelCount + ')');
    return;
  }
  addRestriction(restrictionsId, null, true);
}

/* ── Parse array input ── */
function parseNumArray(str) {
  return str.split(',').map(s => s.trim()).filter(s => s !== '').map(Number);
}

/* ── Build config object from form ── */
function buildConfig() {
  const mapName = document.getElementById('f-mapname') ? document.getElementById('f-mapname').value.trim() || 'GAME' : 'GAME';
  const gameId  = document.getElementById('f-gameid')  ? document.getElementById('f-gameid').value.trim()  || '0'    : '0';
  const strategy = document.getElementById('f-strategy').value;

  const reelSetCards = document.querySelectorAll('#reel-set-list .reel-set-card');
  const reelSets = [];

  reelSetCards.forEach(card => {
    const cardId = card.id;

    const reelRows = card.querySelectorAll('.reel-rows .reel-row input');
    const tilesCounts = Array.from(reelRows).map(inp => {
      const raw = inp.value.trim() || inp.placeholder;
      return parseNumArray(raw);
    });

    const restrCards = card.querySelectorAll('.restrictions-list .restriction-card');
    const restrictions = Array.from(restrCards).map(rc => {
      const inputs = rc.querySelectorAll('input');
      const stacksRaw  = inputs[0].value.trim() || inputs[0].placeholder;
      const chancesRaw = inputs[1].value.trim() || inputs[1].placeholder;
      const distRaw    = inputs[2].value.trim()  || inputs[2].placeholder || '1';
      const stacks  = parseNumArray(stacksRaw);
      const chances = parseNumArray(chancesRaw);
      const dist    = parseInt(distRaw) || 1;
      return { stackSizes: stacks, stackChances: chances, minDistance: dist };
    });

    reelSets.push({ tilesCounts, restrictions });
  });

  return {
    mapName, gameId, strategy, output: 'stdout',
    resultFilePath: './result.txt',
    reelSets
  };
}

/* ── Fill placeholder values into empty fields for a reel set card ── */
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
  btn.title = hasValues ? 'Clear all values' : 'Fill all fields with default values';
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

/* ── Init default reel set ── */
addReelSet();
addReelSet();
loadHistory();
loadRtpHistory();

document.getElementById('rtp-symbol-rows').addEventListener('input', updateSymConfigToggleBtn);
document.getElementById('rtp-lines-list').addEventListener('input', updateLineDefsToggleBtn);
onStrategyChange();
onConvFormatChange();

/* ── Convert tab defaults ── */
const r1 = [1,2,3,1,2,2,2,4], r2 = [2,3,1,2,3], r3 = [3,1,2,3,1];
function prettyCompact(obj) {
  return JSON.stringify(obj, null, 2).replace(
    /\[[\d\s,.+-]+\]/g,
    m => '[' + m.slice(1,-1).trim().replace(/\s*,\s*/g,', ').replace(/\s+/g,' ') + ']'
  );
}
convInput.setValue(prettyCompact([{ setName: 'ReelSet#0', reelSet: [r1, r2, r3] }]));
setTimeout(() => { genOutput.refresh(); convInput.refresh(); convOutput.refresh(); }, 0);

/* ── Tabs ── */
function switchTab(name, btn) {
  _activeTab = name;
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('tab-' + name).classList.add('active');
  btn.classList.add('active');
  setTimeout(() => {
    if (name === 'generate') genOutput.refresh();
    else if (name === 'convert') { convInput.refresh(); convOutput.refresh(); }
    else if (name === 'rtp') tryLoadReelsFromEditor();
    else if (name === 'spin-test') { tryLoadReelsFromEditor(); }
  }, 0);
}

// Attempt to populate _latestReelSets from whatever is currently in the Generate result editor
function tryLoadReelsFromEditor() {
  if (_latestReelSets && _latestReelSets.length > 0) return; // already loaded
  const raw = genOutput.getValue().trim();
  if (!raw) return;
  storeGeneratedReels(raw);
}

/* ── Toast ── */
let toastTimer;
function showToast(msg, isError) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'show' + (isError ? ' toast-err' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.className = ''; }, 2800);
}

/* ── Frontend validation ── */
function clearErrors() {
  document.querySelectorAll('.field-error').forEach(el => el.classList.remove('field-error'));
}

function markError(el, msg) {
  el.classList.add('field-error');
  return msg;
}

function validateConfig() {
  clearErrors();
  const errors = [];

  const reelSetCards = document.querySelectorAll('#reel-set-list .reel-set-card');
  if (reelSetCards.length === 0) {
    errors.push('At least one Reel Set is required');
  }

  reelSetCards.forEach((card, si) => {
    const reelInputs = card.querySelectorAll('.reel-rows .reel-row input');
    if (reelInputs.length === 0) {
      errors.push('Reel Set #' + si + ': at least one reel row is required');
    }
    reelInputs.forEach((inp, ri) => {
      const raw = inp.value.trim() || inp.placeholder;
      const nums = parseNumArray(raw);
      if (nums.length === 0) {
        errors.push(markError(inp, 'Reel Set #' + si + ' R' + (ri+1) + ': tile counts cannot be empty'));
      } else if (nums.some(isNaN)) {
        errors.push(markError(inp, 'Reel Set #' + si + ' R' + (ri+1) + ': all values must be numbers'));
      } else if (nums.some(n => n < 0)) {
        errors.push(markError(inp, 'Reel Set #' + si + ' R' + (ri+1) + ': counts must be >= 0'));
      }
    });

    const restrCards = card.querySelectorAll('.restrictions-list .restriction-card');
    if (restrCards.length > reelInputs.length) {
      errors.push('Reel Set #' + si + ': restrictions count (' + restrCards.length + ') exceeds reel count (' + reelInputs.length + ')');
    }

    restrCards.forEach((rc, ri) => {
      const inputs = rc.querySelectorAll('input');
      const stacksEl   = inputs[0];
      const chancesEl  = inputs[1];
      const distEl     = inputs[2];

      const stacks  = parseNumArray(stacksEl.value.trim()  || stacksEl.placeholder);
      const chances = parseNumArray(chancesEl.value.trim() || chancesEl.placeholder);

      if (stacks.length === 0) {
        errors.push(markError(stacksEl, 'Reel Set #' + si + ' Restriction #' + (ri+1) + ': stackSizes cannot be empty'));
      } else if (stacks.some(isNaN) || stacks.some(n => n <= 0)) {
        errors.push(markError(stacksEl, 'Reel Set #' + si + ' Restriction #' + (ri+1) + ': stackSizes must be positive integers'));
      }

      if (chances.length === 0) {
        errors.push(markError(chancesEl, 'Reel Set #' + si + ' Restriction #' + (ri+1) + ': stackChances cannot be empty'));
      } else if (chances.some(isNaN) || chances.some(n => n < 0)) {
        errors.push(markError(chancesEl, 'Reel Set #' + si + ' Restriction #' + (ri+1) + ': stackChances must be >= 0'));
      }

      if (stacks.length > 0 && chances.length > 0 && stacks.length !== chances.length) {
        errors.push(markError(chancesEl, 'Reel Set #' + si + ' Restriction #' + (ri+1) + ': stackSizes and stackChances must have equal length'));
      }

      const dist = parseInt(distEl.value);
      if (isNaN(dist) || dist < 0) {
        errors.push(markError(distEl, 'Reel Set #' + si + ' Restriction #' + (ri+1) + ': minDistance must be >= 0'));
      }
    });
  });

  return errors;
}

/* ── History (file-backed) ── */
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
    item.title = 'Click to restore';
    item.innerHTML = `
      <button class="history-item-del" title="Delete this entry">
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M9 6V4h6v2"/></svg>
      </button>
      <span class="history-item-label">#${i + 1} &middot; ${entry.strategy}</span>
      <span class="history-item-meta">${entry.time}</span>
    `;
    item.querySelector('.history-item-del').onclick = async (e) => {
      e.stopPropagation();
      if (!await confirmDelete(`Delete entry #${i + 1}?`)) return;
      const res = await fetch(`/api/history/generate/${encodeURIComponent(entry.id)}`, { method: 'DELETE' });
      renderHistory(await res.json());
    };
    item.onclick = () => {
      genOutput.setValue(entry.result);
      storeGeneratedReels(entry.result);
      if (entry.config) {
        try { restoreGenerateForm(JSON.parse(entry.config)); } catch(e) {}
      }
      setStatus('gen', true, 'Restored');
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

/* ── Confirm modal helper ── */
let _confirmResolve = null;
function _confirmReject() {
  document.getElementById('confirm-modal').classList.remove('open');
  if (_confirmResolve) { _confirmResolve(false); _confirmResolve = null; }
}
function confirmDelete(msg) {
  return new Promise(resolve => {
    _confirmResolve = resolve;
    document.getElementById('confirm-modal-msg').textContent = msg;
    document.getElementById('confirm-modal-ok').onclick = () => {
      document.getElementById('confirm-modal').classList.remove('open');
      _confirmResolve = null;
      resolve(true);
    };
    document.getElementById('confirm-modal').classList.add('open');
  });
}

async function clearHistory() {
  if (!await confirmDelete('Delete all generation history?')) return;
  await fetch('/api/history/generate', { method: 'DELETE' });
  renderHistory([]);
}

/* ── Simulation (RTP) history — server-backed ── */

async function pushRtpHistory(label, resultHtml, payload) {
  const now = new Date();
  const time = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const id = Date.now().toString();
  await fetch('/api/history/simulate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id, strategy: label, time, result: resultHtml, config: JSON.stringify(payload) })
  });
  await fetch(`/api/history/simulate/resize?size=${getMaxHistory()}`, { method: 'PUT' });
  await loadRtpHistory();
}

async function loadRtpHistory() {
  const res = await fetch('/api/history/simulate');
  const entries = await res.json();
  renderRtpHistory(entries);
}

function renderRtpHistory(entries) {
  const list  = document.getElementById('rtp-history-list');
  const empty = document.getElementById('rtp-history-empty');
  list.querySelectorAll('.history-item').forEach(el => el.remove());
  if (!entries || entries.length === 0) { empty.style.display = ''; return; }
  empty.style.display = 'none';
  entries.forEach((entry, i) => {
    const item = document.createElement('div');
    item.className = 'history-item';
    item.title = 'Click to restore';
    item.innerHTML = `
      <button class="history-item-del" title="Delete this entry">
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M9 6V4h6v2"/></svg>
      </button>
      <span class="history-item-label">#${i + 1} &middot; ${entry.strategy}</span>
      <span class="history-item-meta">${entry.time}</span>
    `;
    item.querySelector('.history-item-del').onclick = async (e) => {
      e.stopPropagation();
      if (!await confirmDelete(`Delete entry #${i + 1}?`)) return;
      const res = await fetch(`/api/history/simulate/${encodeURIComponent(entry.id)}`, { method: 'DELETE' });
      renderRtpHistory(await res.json());
    };
    item.onclick = () => {
      const body = document.getElementById('rtp-result-body');
      body.innerHTML = entry.result;
      _initComboSections(body);
      if (entry.config) {
        try { restoreRtpForm(JSON.parse(entry.config)); } catch(e) {}
      }
      setStatus('rtp', true, 'Restored');
    };
    list.appendChild(item);
  });
}

async function onRtpHistorySizeChange() {
  const size = getMaxHistory();
  const res = await fetch(`/api/history/simulate/resize?size=${size}`, { method: 'PUT' });
  const entries = await res.json();
  renderRtpHistory(entries);
}

async function clearRtpHistory() {
  if (!await confirmDelete('Delete all simulation history?')) return;
  await fetch('/api/history/simulate', { method: 'DELETE' });
  renderRtpHistory([]);
}

/* ── Copy from Generate → Convert input ── */
function copyFromGenerate() {
  if (!_latestReelSets || _latestReelSets.length === 0) {
    showToast('No generated reels available — run Generate first', true);
    return;
  }
  const json = JSON.stringify(_latestReelSets, null, 2)
    .replace(/\[\s*([\d,\s]+?)\s*\]/g, m => '[' + m.slice(1, -1).trim().replace(/\s+/g, ' ') + ']');
  convInput.setValue(json);
  showToast('Copied from Generate tab');
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
  setStatus('gen', null, 'Running…');
  const config = buildConfig();
  try {
    const res = await fetch('/api/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ config })
    });
    const d = await res.json();
    if (d.error) { setStatus('gen', false, d.error); genOutput.setValue(''); }
    else         { setStatus('gen', true, 'Done');   genOutput.setValue(d.result); storeGeneratedReels(d.result); await pushHistory(d.result, config); }
  } catch(e) { setStatus('gen', false, 'Network error'); }
  finally { btn.disabled = false; }
}

function onConvFormatChange() {
  const isCsv = document.getElementById('conv-format').value === 'CSV';
  document.getElementById('conv-gameid-group').style.display = isCsv ? '' : 'none';
  document.getElementById('conv-gameid-sep').style.display   = isCsv ? '' : 'none';
}

/* Detects whether input text looks like CSV (first non-blank line starts with a quoted field).
   Returns 'csv' or 'json'. */
function detectInputFormat(text) {
  const first = text.trimStart();
  return first.startsWith('"') ? 'csv' : 'json';
}

/* Parses CSV produced by CsvConverter into [{setName, reelSet}].
   Each row: "setName","reelIndex","position","gameIdTile"
   Returns {ok: true, reelSets} or {ok: false, error} */
function parseCsvInput(text) {
  const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
  if (lines.length === 0) return { ok: false, error: 'CSV input is empty' };

  // map: setName -> map: reelIndex -> array of tiles (sparse by position)
  const sets = new Map();

  for (let i = 0; i < lines.length; i++) {
    const row = lines[i];
    // split on comma outside quotes: simple split since fields are always quoted
    const parts = row.split('","');
    if (parts.length !== 4) return { ok: false, error: `CSV line ${i+1}: expected 4 columns, got ${parts.length}` };
    const setName  = parts[0].replace(/^"/, '');
    const reelIdx  = parseInt(parts[1], 10);
    const pos      = parseInt(parts[2], 10);
    const tileStr  = parts[3].replace(/"$/, '');

    if (isNaN(reelIdx) || isNaN(pos)) return { ok: false, error: `CSV line ${i+1}: reelIndex/position must be integers` };

    // Extract symbol from gameIdTile: last 3 digits, then parseInt to drop leading zeros
    if (tileStr.length < 3) return { ok: false, error: `CSV line ${i+1}: gameIdTile "${tileStr}" too short` };
    const symbol = parseInt(tileStr.slice(-3), 10);
    if (isNaN(symbol) || symbol < 1) return { ok: false, error: `CSV line ${i+1}: cannot parse symbol from "${tileStr}"` };

    if (!sets.has(setName)) sets.set(setName, new Map());
    const reels = sets.get(setName);
    if (!reels.has(reelIdx)) reels.set(reelIdx, []);
    const reel = reels.get(reelIdx);
    reel[pos] = symbol;
  }

  const reelSets = [];
  for (const [setName, reels] of sets) {
    const reelSet = [];
    const maxIdx = Math.max(...reels.keys());
    for (let j = 0; j <= maxIdx; j++) {
      const reel = reels.get(j);
      if (!reel) return { ok: false, error: `Reel index ${j} is missing in set "${setName}"` };
      reelSet.push(reel);
    }
    reelSets.push({ setName, reelSet });
  }
  return { ok: true, reelSets };
}

/* Parses and validates JSON array input [{setName, reelSet}].
   Returns {ok: true, reelSets} or {ok: false, error} */
function parseJsonInput(text) {
  let parsed;
  try { parsed = JSON.parse(text); }
  catch(e) { return { ok: false, error: 'Invalid JSON: ' + e.message }; }
  if (!Array.isArray(parsed)) return { ok: false, error: 'Input must be a JSON array' };
  for (let i = 0; i < parsed.length; i++) {
    const rs = parsed[i];
    if (typeof rs !== 'object' || !rs.setName || !Array.isArray(rs.reelSet))
      return { ok: false, error: `Item ${i}: must have "setName" (string) and "reelSet" (array)` };
    for (let j = 0; j < rs.reelSet.length; j++) {
      if (!Array.isArray(rs.reelSet[j]))
        return { ok: false, error: `Item ${i} reelSet[${j}]: must be an array of integers` };
      if (!rs.reelSet[j].every(v => Number.isInteger(v) && v >= 1))
        return { ok: false, error: `Item ${i} reelSet[${j}]: all values must be integers >= 1` };
    }
  }
  return { ok: true, reelSets: parsed };
}

async function runConvert() {
  const btn = document.getElementById('conv-btn');
  btn.disabled = true;
  setStatus('conv', null, 'Running…');

  const raw = convInput.getValue().trim();
  if (!raw) { setStatus('conv', false, 'Input is empty'); btn.disabled = false; return; }

  const fmt = detectInputFormat(raw);
  const parsed = fmt === 'csv' ? parseCsvInput(raw) : parseJsonInput(raw);
  if (!parsed.ok) { setStatus('conv', false, parsed.error); btn.disabled = false; return; }

  const body = {
    reelSets: parsed.reelSets,
    toCom:    document.getElementById('conv-format').value,
    gameId:   document.getElementById('conv-gameid').value || '0'
  };
  try {
    const res = await fetch('/api/convert', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const d = await res.json();
    if (d.error) { setStatus('conv', false, d.error); convOutput.setValue(''); }
    else {
      convOutput.setValue(d.result);
      if (body.toCom === 'COUNT') {
        setStatus('conv', true, 'Done — these are tile counts per reel. Paste them into the Generate tab as tilesCounts to regenerate reel strips.');
      } else {
        setStatus('conv', true, 'Done');
      }
    }
  } catch(e) { setStatus('conv', false, 'Network error'); }
  finally { btn.disabled = false; }
}

function setStatus(prefix, ok, msg) {
  const el = document.getElementById(prefix + '-status');
  el.textContent = msg;
  el.className = 'status' + (ok === true ? ' ok' : ok === false ? ' err' : ' pending');
}

function copyResult(id, btn) {
  const cm = id === 'gen-output' ? genOutput : convOutput;
  navigator.clipboard.writeText(cm.getValue()).then(() => {
    btn.classList.add('copied');
    const orig = btn.innerHTML;
    btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
    setTimeout(() => { btn.classList.remove('copied'); btn.innerHTML = orig; }, 1800);
  });
}

/* ── Info modal ── */
let _activeTab = 'generate';

function openInfo() {
  const titles = { generate: 'Generate tab — reference', convert: 'Convert tab — reference' };
  document.getElementById('info-modal-title').textContent = titles[_activeTab] || 'Help';
  document.querySelectorAll('#info-modal .info-section[data-tab]').forEach(s => {
    s.style.display = s.dataset.tab === _activeTab ? '' : 'none';
  });
  document.getElementById('info-modal').classList.add('open');
}
function closeInfo() { document.getElementById('info-modal').classList.remove('open'); }
document.addEventListener('keydown', e => { if (e.key === 'Escape') { closeInfo(); closeImportCounts(); } });

/* ── RTP Tab ── */

let _latestReelSets = null;   // [{setName, reelSet}]
let _rtpSymbolCounter = 0;

// Called after a successful generation — store latest reels
function storeGeneratedReels(reelSetsJson) {
  try {
    _latestReelSets = JSON.parse(reelSetsJson);
    syncRtpChances();
    syncRtpSymbolsFromReels();
  } catch(e) {
    // ignore parse errors (non-JSON output)
  }
}

function restoreGenerateForm(config) {
  if (!config) return;
  // Strategy
  const strat = document.getElementById('f-strategy');
  if (strat && config.strategy) strat.value = config.strategy;
  // Rebuild reel sets
  if (config.reelSets && config.reelSets.length > 0) {
    document.getElementById('reel-set-list').innerHTML = '';
    reelSetCounter = 0;
    config.reelSets.forEach(rs => addReelSet(rs));
  }
}

function restoreRtpForm(payload) {
  if (!payload) return;
  // Simple fields
  const set = (id, v) => { const el = document.getElementById(id); if (el && v != null) el.value = v; };
  set('rtp-strategy', payload.strategy);
  set('rtp-screen-width', payload.screenWidth);
  set('rtp-screen-height', payload.screenHeight);
  set('rtp-min-match', payload.minMatch);
  set('rtp-threads', payload.threadCount);
  set('rtp-bet-size', payload.betSize);
  // Spins — match option value
  const spinsEl = document.getElementById('rtp-spins');
  if (spinsEl && payload.spins) {
    const opt = Array.from(spinsEl.options).find(o => parseInt(o.value) === payload.spins);
    if (opt) spinsEl.value = opt.value;
  }
  // Reel sets
  if (payload.reelSets) {
    _latestReelSets = payload.reelSets;
    syncRtpChances();
  }
  // Reel set chances
  if (payload.reelSetChances) {
    payload.reelSetChances.forEach((c, i) => {
      const el = document.getElementById('rtp-chance-' + i);
      if (el) el.value = c.chance;
    });
    updateChanceTotal();
  }
  // Line definitions
  document.getElementById('rtp-lines-list').innerHTML = '';
  _lineCounter = 0;
  (payload.lineDefinitions || []).forEach(line => addLineDef(line.join(', ')));
  updateLineCount();
  // Symbols
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

function clearGenerateResult() {
  genOutput.setValue('');
  setStatus('gen', false, '');
  _latestReelSets = null;
  // Reset RTP tab
  document.getElementById('rtp-chances-list').innerHTML =
    '<span style="font-size:0.72rem;color:var(--text3);font-style:italic">Generate reels first</span>';
  document.getElementById('rtp-chance-total').textContent = '';
  document.getElementById('rtp-chance-total').className = 'rtp-chance-total';
  document.getElementById('rtp-symbol-rows').innerHTML = '';
  document.getElementById('rtp-lines-list').innerHTML = '';
  updateLineCount();
  _symRowCounter = 0;
  _lineCounter = 0;
  setStatus('rtp', false, '');
  // Reset RTP result panel
  const resultBody = document.getElementById('rtp-result-body');
  if (resultBody) {
    resultBody.innerHTML = `
      <div class="rtp-empty">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color:var(--text3)"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <span>Configure symbols and run the simulation</span>
      </div>`;
  }
}

/* Build reel set chance rows from latest reels */
function syncRtpChances() {
  const container = document.getElementById('rtp-chances-list');
  if (!_latestReelSets || _latestReelSets.length === 0) {
    container.innerHTML = '<span style="font-size:0.72rem;color:var(--text3);font-style:italic">Generate reels first</span>';
    return;
  }
  const n = _latestReelSets.length;
  const equalShare = Math.floor(1000 / n) / 10; // 1 decimal
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

/* Infer symbol IDs from generated reels and build default symbol rows */
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
  // Add missing symbols
  const sortedNew = [...foundIds].filter(id => !existingIds.has(id)).sort((a,b) => a-b);
  for (const id of sortedNew) {
    addSymbolRow(id);
  }
  refreshSymbolRowNumbers();

  // Populate default lines if empty
  const firstRs = _latestReelSets[0];
  const w = firstRs.reelSet.length;
  if (document.querySelectorAll('.rtp-line-row').length === 0) {
    const h = parseInt(document.getElementById('rtp-screen-height').value) || 3;
    for (const line of generateDefaultLines(w, h)) {
      addLineDef(line);
    }
  }
}

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
    <input type="text" class="array-input rtp-paytable-input" placeholder="0, 0, 1.5, 3.0, 10.0" value=""/>
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
  // Sync paytable placeholder for the new row (generic tier-based)
  const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
  const m = parseInt(document.getElementById('rtp-min-match').value) || 3;
  // Auto-set type and default paytable placeholder for well-known default symbol IDs
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
      if (aggSel) { aggSel.value = def.agg; onWildAggChange(aggSel); }
    }
    // Apply specific placeholder after generic update so it wins
    updatePaytablePlaceholders(w, m);
    if (def.paytable) {
      const ptInput = row.querySelector('.rtp-paytable-input');
      if (ptInput) { ptInput.placeholder = def.paytable; ptInput.dataset.fixedPlaceholder = '1'; }
    }
  } else {
    updatePaytablePlaceholders(w, m);
  }
  return row;
}

function onSymbolTypeChange(sel) {
  const row = sel.closest('.rtp-sym-row');
  const ptInput = row.querySelector('input[type=text]');
  const wildFields = row.querySelector('.rtp-wild-fields');
  const isWild = sel.value === 'WILD';
  const isScatter = sel.value === 'SCATTER';

  ptInput.disabled = isScatter;
  if (isScatter) {
    ptInput.placeholder = 'n/a';
    ptInput.value = '';
  } else {
    const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
    const m = parseInt(document.getElementById('rtp-min-match').value) || 3;
    updatePaytablePlaceholders(w, m);
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
    // Reset to NONE and hide sequence/mult when switching away from Wild
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
}

function refreshSymbolRowNumbers() {
  document.querySelectorAll('.rtp-sym-row').forEach(row => {
    const lbl = row.querySelector('.rtp-sym-id');
    if (lbl) lbl.textContent = row.dataset.symId;
  });
}

/* Line definitions */
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
    // Fill screen defaults from placeholders
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
function onStrategyChange() {
  const isWays = document.getElementById('rtp-strategy').value === 'WAYS';
  const section = document.getElementById('line-defs-section');
  const addBtn = document.getElementById('linedef-add-btn');
  if (section) section.style.display = isWays ? 'none' : '';
  if (addBtn) addBtn.style.display = isWays ? 'none' : '';
  document.querySelectorAll('.rtp-sym-row').forEach(row => {
    const typeSel = row.querySelector('select');
    if (!typeSel || typeSel.value !== 'WILD') return;
    const wildFields = row.querySelector('.rtp-wild-fields');
    if (wildFields) wildFields.style.display = isWays ? 'none' : '';
    if (isWays) {
      const aggSel = row.querySelector('.rtp-wild-agg');
      if (aggSel) { aggSel.value = 'NONE'; onWildAggChange(aggSel); }
    }
  });
}
function onScreenSizeChange() {
  const w = parseInt(document.getElementById('rtp-screen-width').value) || 5;
  const m = parseInt(document.getElementById('rtp-min-match').value) || 3;
  updatePaytablePlaceholders(w, m);
  // Refresh sequence placeholders for any SEQUENCE wilds
  const seqPh = Array.from({length: w}, (_, i) => (i + 1).toFixed(1)).join(', ');
  document.querySelectorAll('.rtp-wild-seq').forEach(inp => { inp.placeholder = seqPh; });
}

function updatePaytablePlaceholders(w, m) {
  const count = Math.max(1, w - m + 1);
  const NORMAL_TABLE = {
    1: '1.0',
    2: '1.0, 2.0',
    3: '1.0, 2.0, 4.0',
    4: '0.5, 1.0, 2.0, 4.0',
    5: '0.2, 0.5, 1.0, 2.0, 4.0',
    6: '0.1, 0.2, 0.5, 1.0, 2.0, 4.0',
    7: '0.1, 0.2, 0.5, 1.0, 2.0, 4.0, 8.0',
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
  const normalPh = NORMAL_TABLE[count] || Array.from({length: count}, (_, i) => (i + 1).toFixed(1)).join(', ');
  const wildPh   = WILD_TABLE[count]   || Array.from({length: count}, (_, i) => ((i + 1) * 2).toFixed(1)).join(', ');
  const LOW_TABLE = {
    1: '2.0',
    2: '1.0, 2.0',
    3: '0.5, 1.0, 2.0',
    4: '0.2, 0.5, 1.0, 2.0',
    5: '0.1, 0.2, 0.5, 1.0, 2.0',
    6: '0.1, 0.1, 0.2, 0.5, 1.0, 2.0',
    7: '0.1, 0.1, 0.2, 0.5, 1.0, 2.0, 4.0',
  };
  const lowPh = LOW_TABLE[count] || Array.from({length: count}, (_, i) => ((i + 1) * 0.5).toFixed(1)).join(', ');
  document.querySelectorAll('.rtp-paytable-input').forEach(inp => {
    if (inp.disabled) return; // scatter — keep n/a
    if (inp.dataset.fixedPlaceholder) return; // SYM_DEFAULTS placeholder — don't overwrite
    const row = inp.closest('.rtp-sym-row');
    const symId = row ? parseInt(row.dataset.symId) : 0;
    const isWild = row && row.querySelector('select') && row.querySelector('select').value === 'WILD';
    let ph;
    if (isWild)          ph = wildPh;
    else if (symId <= 3) ph = normalPh;
    else                 ph = lowPh;
    inp.placeholder = ph;
    inp.title = count + ' value(s) required (screenWidth − minMatch + 1)';
  });
}

// Generate default paylines for a new screen size
function generateDefaultLines(width, height) {
  const lines = [];
  const mid = Math.floor(height / 2);
  // Middle row
  lines.push(Array(width).fill(mid).join(', '));
  if (height >= 3) {
    lines.push(Array(width).fill(0).join(', '));
    lines.push(Array(width).fill(height - 1).join(', '));
  }
  return lines;
}

function collectRtpRequest() {
  const errors = [];

  // Try to load from result editor if not already loaded
  if (!_latestReelSets || _latestReelSets.length === 0) {
    tryLoadReelsFromEditor();
  }

  if (!_latestReelSets || _latestReelSets.length === 0)
    return { errors: ['No generated reels found. Run Generate first.'] };

  // Reel set chances
  const chances = [];
  let chanceSum = 0;
  _latestReelSets.forEach((_, i) => {
    const el = document.getElementById('rtp-chance-' + i);
    const v = parseFloat(el?.value);
    if (isNaN(v) || v < 0) { errors.push('Reel set ' + i + ': chance must be >= 0'); return; }
    chances.push({ setIndex: i, chance: v });
    chanceSum += v;
  });
  if (Math.abs(chanceSum - 100) > 0.05)
    errors.push('Reel set chances must sum to 100.0% (current: ' + chanceSum.toFixed(1) + '%)');

  // Screen config
  const screenWidth  = parseInt(document.getElementById('rtp-screen-width').value);
  const screenHeight = parseInt(document.getElementById('rtp-screen-height').value);
  const minMatch     = parseInt(document.getElementById('rtp-min-match').value);
  if (isNaN(screenWidth)  || screenWidth  < 1) errors.push('Screen width must be >= 1');
  if (isNaN(screenHeight) || screenHeight < 1) errors.push('Screen height must be >= 1');
  if (isNaN(minMatch) || minMatch < 1) errors.push('Min Match must be >= 1');
  if (!isNaN(minMatch) && !isNaN(screenWidth) && minMatch > screenWidth)
    errors.push('Min Match (' + minMatch + ') cannot exceed screen width (' + screenWidth + ')');

  // Line definitions — three-pass validation: format → positions → duplicates
  // WAYS strategy does not use paylines — skip line validation entirely
  const lineRows = document.querySelectorAll('.rtp-line-row');
  const _strategyForLineCheck = document.getElementById('rtp-strategy').value;
  const isWays = _strategyForLineCheck === 'WAYS';
  if (!isWays && lineRows.length === 0) errors.push('At least one line definition is required');
  const lineDefinitions = [];
  const parsedLines = [];   // store per-row parse results for later passes

  // Pass 1: format (only digits, commas, spaces) — placeholder-only lines are skipped (treated as not entered)
  lineRows.forEach((row, li) => {
    const inp = row.querySelector('.rtp-line-input');
    const raw = inp.value.trim();
    if (raw === '') {
      parsedLines.push(null);
      return;
    }
    if (!/^[\d ,]+$/.test(raw)) {
      errors.push('Line ' + (li+1) + ': must contain only numbers separated by commas');
      parsedLines.push(null);
      return;
    }
    const nums = raw.split(',').map(s => parseInt(s.trim(), 10));
    if (nums.some(isNaN)) {
      errors.push('Line ' + (li+1) + ': must contain only numbers separated by commas');
      parsedLines.push(null);
      return;
    }
    parsedLines.push(nums);
  });

  if (!isWays && parsedLines.every(n => n === null) && lineRows.length > 0)
    errors.push('At least one line definition must be filled in');

  // Pass 2: length and position range
  parsedLines.forEach((nums, li) => {
    if (nums === null) return;
    if (!isNaN(screenWidth) && nums.length !== screenWidth)
      errors.push('Line ' + (li+1) + ': must have exactly ' + screenWidth + ' positions');
    if (!isNaN(screenHeight) && nums.some(n => n < 0 || n >= screenHeight))
      errors.push('Line ' + (li+1) + ': positions must be 0–' + (screenHeight-1));
  });

  // Pass 3: duplicates (only among valid lines)
  const seenLines = new Set();
  parsedLines.forEach((nums, li) => {
    if (nums === null) return;
    const key = JSON.stringify(nums);
    if (seenLines.has(key)) errors.push('Line ' + (li+1) + ': duplicate payline');
    else seenLines.add(key);
    lineDefinitions.push(nums);
  });

  // Symbols
  const symbols = [];
  document.querySelectorAll('.rtp-sym-row').forEach(row => {
    const symId = parseInt(row.dataset.symId);
    const type = row.querySelector('select').value;
    const ptRaw = row.querySelector('input[type=text]').value.trim().replace(/^n\/a$/i, '');
    const paytable = ptRaw ? ptRaw.split(',').map(s => parseFloat(s.trim())) : [];
    const wildMult = parseFloat(row.querySelector('.rtp-wild-mult')?.value) || 1.0;
    const wildAgg  = row.querySelector('.rtp-wild-agg')?.value || 'ADD';
    const seqRaw   = row.querySelector('.rtp-wild-seq')?.value.trim() || '';
    const wildSequence = (wildAgg === 'SEQUENCE' && seqRaw)
      ? seqRaw.split(',').map(s => Math.round(parseFloat(s.trim()) * 10) / 10)
      : [];
    if (type === 'NORMAL' && paytable.length === 0)
      errors.push('Symbol ' + symId + ': paytable is required for NORMAL symbols');
    if (paytable.some(isNaN))
      errors.push('Symbol ' + symId + ': paytable contains invalid numbers');
    if (paytable.some(v => !isNaN(v) && Math.round(v * 10) !== v * 10))
      errors.push('Symbol ' + symId + ': paytable values must be multiples of 0.1');
    if (paytable.length > 0 && (type === 'NORMAL' || type === 'WILD')) {
      const required = Math.max(1, screenWidth - minMatch + 1);
      if (!isNaN(screenWidth) && !isNaN(minMatch) && paytable.length !== required)
        errors.push('Symbol ' + symId + ': paytable must have exactly ' + required + ' value(s) (screenWidth − minMatch + 1 = ' + screenWidth + ' − ' + minMatch + ' + 1)');
    }
    if (type === 'WILD' && wildAgg !== 'SEQUENCE' && wildAgg !== 'NONE' && (isNaN(wildMult) || wildMult <= 0))
      errors.push('Symbol ' + symId + ': wild multiplier must be > 0');
    if (wildAgg === 'SEQUENCE') {
      if (wildSequence.some(isNaN))
        errors.push('Symbol ' + symId + ': sequence contains invalid numbers');
      if (wildSequence.length !== screenWidth)
        errors.push('Symbol ' + symId + ': sequence must have exactly ' + screenWidth + ' value(s) (one per reel)');
    }
    symbols.push({ symbolId: symId, type, paytable, wildMultiplier: wildMult, wildAggregation: wildAgg, wildSequence });
  });

  if (symbols.length === 0) errors.push('At least one symbol must be configured');

  if (!symbols.some(s => s.type === 'NORMAL' && s.paytable.length > 0))
    errors.push('At least one NORMAL symbol with a paytable is required');


  // Settings
  const strategy    = document.getElementById('rtp-strategy').value;
  const spins       = parseInt(document.getElementById('rtp-spins').value);
  const threadCount = parseInt(document.getElementById('rtp-threads').value);
  const betSize     = parseFloat(document.getElementById('rtp-bet-size').value);

  if (isNaN(spins) || spins <= 0) errors.push('Spins must be selected');
  if (isNaN(threadCount) || threadCount < 1 || threadCount > 8)
    errors.push('Thread count must be between 1 and 8');
  if (isNaN(betSize) || betSize < 0.1 || betSize > 200.0)
    errors.push('Bet size must be between 0.10 and 200.00');
  else if (Math.round(betSize * 10) !== betSize * 10)
    errors.push('Bet size must be a multiple of 0.1 (min bet)');

  if (errors.length > 0) return { errors };

  return {
    errors: [],
    payload: {
      reelSets: _latestReelSets,
      reelSetChances: chances,
      symbols,
      strategy,
      screenWidth,
      screenHeight,
      minMatch,
      lineDefinitions,
      spins,
      threadCount,
      betSize
    }
  };
}

async function runRtp() {
  const btn = document.getElementById('rtp-run-btn');
  const { errors, payload } = collectRtpRequest();
  if (errors.length > 0) {
    showToast(errors[0], true);
    setStatus('rtp', false, errors[0]);
    return;
  }

  btn.disabled = true;
  setStatus('rtp', null, 'Running…');

  const body = document.getElementById('rtp-result-body');
  body.innerHTML = `
    <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;flex:1;gap:1rem;color:var(--text3)">
      <span style="font-size:0.82rem">Simulating ${payload.spins.toLocaleString()} spins on ${payload.threadCount} threads…</span>
      <div class="rtp-progress"><div class="rtp-progress-bar" id="rtp-pb" style="width:60%;animation:none;background:var(--accent);opacity:.6"></div></div>
    </div>`;

  try {
    const res = await fetch('/api/rtp/simulate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const d = await res.json();
    if (d.error) {
      setStatus('rtp', false, d.error);
      body.innerHTML = `<div class="rtp-placeholder"><span style="color:var(--error)">${escapeHtml(d.error)}</span></div>`;
    } else {
      const r = JSON.parse(d.result);
      renderRtpResult(body, r, payload);
      setStatus('rtp', true, 'Done');
      const label = `${r.rtpPercent.toFixed(2)}% · ${payload.strategy} · ${(r.totalSpins/1e6).toFixed(1)}M`;
      await pushRtpHistory(label, body.innerHTML, payload);
    }
  } catch(e) {
    setStatus('rtp', false, 'Network error');
    body.innerHTML = `<div class="rtp-placeholder"><span style="color:var(--error)">Network error</span></div>`;
  } finally {
    btn.disabled = false;
  }
}

function renderRtpResult(container, r, payload) {
  const rtp     = r.rtpPercent.toFixed(4);
  const spins   = r.totalSpins.toLocaleString();
  const elapsed = r.elapsedMs >= 1000
    ? (r.elapsedMs / 1000).toFixed(1) + 's'
    : r.elapsedMs + 'ms';

  const fmt1 = v => v.toFixed(1);
  const fmt2 = v => v.toFixed(2);
  const fmt4 = v => v.toFixed(4);

  container.innerHTML = `
    <div class="rtp-result-card">
      <span class="rtp-result-label">Return to Player</span>
      <span class="rtp-big-number">${rtp}%</span>
      <span class="rtp-meta">Strategy: ${payload.strategy} &nbsp;·&nbsp; ${spins} spins &nbsp;·&nbsp; ${payload.threadCount} threads &nbsp;·&nbsp; ${elapsed}</span>
      <span class="rtp-note">Statistics model the stake multiplier as a discrete random variable — each spin yields a multiplier; all metrics below describe its distribution.</span>
    </div>
    <div class="rtp-stats-grid">

      <div class="rtp-stat-group">
        <span class="rtp-stat-group-title">Simulation</span>
        <div class="rtp-stat-row">
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Spins simulated</span>
            <span class="rtp-stat-value">${spins}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Elapsed time</span>
            <span class="rtp-stat-value">${elapsed}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Bet size</span>
            <span class="rtp-stat-value">${fmt2(r.betSize)}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Hit rate</span>
            <span class="rtp-stat-value">${fmt2(r.hitRatePct)}%</span>
          </div>
        </div>
      </div>

      <div class="rtp-stat-group">
        <span class="rtp-stat-group-title">Distribution</span>
        <div class="rtp-stat-row">
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Avg win<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:200px">Average payout per winning spin only. Zero-win spins are excluded.<div class="tip-rule">totalWin ÷ hitCount × betSize</div></span></span></span>
            <span class="rtp-stat-value">${fmt4(r.avgWin)}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Median<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:200px">Median payout of winning spins only. Zero-win spins are excluded.<div class="tip-rule">Knuth reservoir sampling · 100k sample</div></span></span></span>
            <span class="rtp-stat-value">${fmt1(r.medianWin)}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Max win<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:210px">Highest stake multiplier recorded in this simulation — the largest single-spin paytable value hit.</span></span></span>
            <span class="rtp-stat-value">${fmt1(r.maxWin)}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Std deviation<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:220px">Measures how spread out payouts are around the average. A high value means wins vary widely in size; a low value means they cluster near the mean.<div class="tip-rule">√(E[win²] − E[win]²) × betSize</div></span></span></span>
            <span class="rtp-stat-value">${fmt4(r.stdDev)}</span>
          </div>
        </div>
      </div>

      <div class="rtp-stat-group">
        <span class="rtp-stat-group-title">Volatility</span>
        <div class="rtp-stat-row">
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Volatility index<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:220px">Risk level indicator — how wildly payouts swing relative to the average. Higher = rare but bigger wins, longer dry spells.<div class="tip-rule"><div style="opacity:0.55;margin-bottom:0.25rem;font-size:0.67rem">stdDev ÷ avgWinPerSpin</div><div class="tip-row"><span>Low</span><span>&lt; 2.0</span></div><div class="tip-row"><span>Medium</span><span>2.0 – 5.0</span></div><div class="tip-row"><span>High</span><span>5.0 – 10.0</span></div><div class="tip-row"><span>Extreme</span><span>≥ 10.0</span></div></div></span></span></span>
            <span class="rtp-stat-value">${fmt2(r.volatilityIndex)}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Volatility</span>
            <span class="rtp-stat-value">${r.volatilityLabel}</span>
          </div>
        </div>
      </div>

      <div class="rtp-stat-group">
        <span class="rtp-stat-group-title">Configuration</span>
        <div class="rtp-stat-row">
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Payout strategy<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:230px">${{
              LTR: 'Left to Right — all symbols pay on adjacent reels starting from the leftmost reel.',
              RTL: 'Right to Left — all symbols pay on adjacent reels starting from the rightmost reel.',
              BW:  'Both Ways — all symbols pay on adjacent reels starting from either the leftmost or the rightmost reel. Both directions are evaluated and the total of both is awarded.',
              ADJ: 'Adjacent — symbols pay on consecutive adjacent reels starting from any valid reel, not only the leftmost.'
            }[payload.strategy] || payload.strategy}<div class="tip-rule">Symbols must land on a defined payline (line definition) to count as a win.</div></span></span></span>
            <span class="rtp-stat-value">${{'LTR':'Left to Right','RTL':'Right to Left','BW':'Both Ways','ADJ':'Adjacent'}[payload.strategy] || payload.strategy}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Screen size<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:210px">Width × Height of the visible symbol grid. Width = number of reels; Height = number of visible rows per reel.<div class="tip-rule">e.g. 5×3 = 5 reels, 3 rows each</div></span></span></span>
            <span class="rtp-stat-value">${payload.screenWidth}×${payload.screenHeight}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">Min Match<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:210px">Minimum number of consecutive identical symbols on a payline required to count as a win.<div class="tip-rule">e.g. Min Match 3 on a 5-reel game → paytable has 3 entries: x3, x4, x5</div></span></span></span>
            <span class="rtp-stat-value">${payload.minMatch}</span>
          </div>
        </div>
      </div>

      ${(() => {
        const combos = r.comboBreakdown;
        if (!combos || combos.length === 0) return '';
        const maxPay = combos[0].totalPayout;
        const rows = combos.map(c => {
          const hitRate    = (c.hitCount / r.totalSpins * 100).toFixed(3);
          const rtpContrib = (c.totalPayout / r.totalSpins * 100).toFixed(4);
          const avgPayout  = (c.totalPayout * payload.betSize / c.hitCount).toFixed(4);
          const pct = maxPay > 0 ? Math.round(c.totalPayout / maxPay * 100) : 0;
          return `<tr>
            <td><span class="combo-sym-badge">S${c.symbolId}</span></td>
            <td>${c.matchCount}</td>
            <td>${c.hitCount.toLocaleString()}</td>
            <td>${hitRate}%</td>
            <td>${avgPayout}</td>
            <td class="combo-bar-cell"><div class="combo-bar-wrap"><div class="combo-bar"><div class="combo-bar-fill" style="width:${pct}%"></div></div><span class="combo-bar-pct">${rtpContrib}%</span></div></td>
          </tr>`;
        }).join('');
        const uid  = 'cb' + Date.now();
        const tid  = uid + 't';
        const cols = ['Symbol','Length','Hits','Hit Rate','Multiplier','RTP Contrib'];
        const ths  = cols.map((label, i) =>
          `<th><button class="combo-th-btn" onclick="_comboSort('${tid}',${i})">${label} <span class="combo-sort-arrow"><svg width="9" height="9" viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M3 4h18l-7 9v7l-4-2v-5z"/></svg></span></button></th>`
        ).join('');
        const dataJson = JSON.stringify(combos).replace(/</g,'\\u003c');
        return `<div class="combo-section" data-combo-tid="${tid}" data-combo-json='${dataJson}' data-combo-spins="${r.totalSpins}" data-combo-bet="${payload.betSize}">
          <button class="combo-toggle" onclick="(function(btn){
            const wrap=document.getElementById('${uid}');
            const sb=document.getElementById('${uid}sb');
            const chev=btn.querySelector('.combo-chevron');
            const open=wrap.classList.toggle('open');
            chev.classList.toggle('open',open);
            if(sb) sb.style.display=open?'flex':'none';
          })(this)">
            <span class="combo-chevron">&#9658;</span>
            Combination Breakdown <span style="opacity:0.5;font-weight:400;text-transform:none;letter-spacing:0">(${combos.length} combinations)</span>
          </button>
          <div class="combo-sort-bar" id="${uid}sb"></div>
          <div class="combo-table-wrap" id="${uid}">
            <table class="combo-table" id="${tid}">
              <thead><tr>${ths}</tr></thead>
              <tbody>${rows}</tbody>
            </table>
          </div>
        </div>`;
      })()}

    </div>
  `;
  // Init combo tables after innerHTML is set (inline <script> doesn't execute via innerHTML)
  _initComboSections(container);
}

function _initComboSections(container) {
  container.querySelectorAll('.combo-section[data-combo-tid]').forEach(section => {
    const tid   = section.dataset.comboTid;
    const tbl   = document.getElementById(tid);
    if (!tbl) return;
    tbl._data       = JSON.parse(section.dataset.comboJson);
    tbl._totalSpins = parseInt(section.dataset.comboSpins);
    tbl._betSize    = parseFloat(section.dataset.comboBet);
    tbl._sortKeys   = [{col: 0, asc: true}, {col: 1, asc: true}];
    tbl._barEl      = document.getElementById(tid.replace(/t$/, '') + 'sb');
    tbl._cols       = ['Symbol','Length','Hits','Hit Rate','Avg Payout','RTP Contrib'];
    _comboRender(tbl);
  });
}

function escapeHtml(s) {
  return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

/* ── Combo table multi-sort ── */
function _comboInfoBtn() {
  return `<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:280px;text-transform:none;letter-spacing:0;font-weight:400">Multi-column sort for the combination breakdown table.<div class="tip-rule"><strong>Add criterion</strong> — click any column header. It is appended as the lowest priority.</div><div class="tip-rule"><strong>Toggle direction</strong> — click ▲ or ▼ on a pill to flip between ascending and descending.</div><div class="tip-rule"><strong>Remove one</strong> — click × on a pill to drop that column from the sort.</div><div class="tip-rule"><strong>Clear all</strong> — click the Clear button to remove all criteria. Rows stay in their last sorted order.</div><div class="tip-rule">Priority goes left → right. The leftmost pill is the primary sort; ties are broken by the next pill, and so on.</div></span></span>`;
}

function _comboSort(tid, colIdx) {
  const tbl = document.getElementById(tid);
  if (!tbl) return;
  const keys = tbl._sortKeys || [];
  const existing = keys.findIndex(k => k.col === colIdx);
  if (existing === 0) {
    keys[0].asc = !keys[0].asc;
  } else if (existing > 0) {
    const [k] = keys.splice(existing, 1);
    keys.unshift(k);
  } else {
    keys.push({ col: colIdx, asc: true });  // add as lowest priority
  }
  tbl._sortKeys = keys;
  _comboRender(tbl);
}

function _comboRemove(tid, colIdx) {
  const tbl = document.getElementById(tid);
  if (!tbl) return;
  tbl._sortKeys = (tbl._sortKeys || []).filter(k => k.col !== colIdx);
  _comboRender(tbl);
}

function _comboClear(tid) {
  const tbl = document.getElementById(tid);
  if (!tbl) return;
  tbl._sortKeys = [];
  _comboRender(tbl);
}

function _comboToggleDir(tid, colIdx) {
  const tbl = document.getElementById(tid);
  if (!tbl) return;
  const k = (tbl._sortKeys || []).find(k => k.col === colIdx);
  if (k) k.asc = !k.asc;
  _comboRender(tbl);
}

function _comboRender(tbl) {
  const keys       = tbl._sortKeys || [];
  const data       = tbl._data;
  const totalSpins = tbl._totalSpins;
  const betSize    = tbl._betSize;
  const cols       = tbl._cols || ['Symbol','Length','Hits','Hit Rate','Avg Payout','RTP Contrib'];
  const tid        = tbl.id;
  const maxPay     = Math.max(...data.map(c => c.totalPayout));

  // Update pill bar
  const bar = tbl._barEl;
  if (bar) {
    if (keys.length === 0) {
      bar.innerHTML = '<span class="combo-sort-bar-label">Sort by:</span>' + _comboInfoBtn() + '<span style="font-size:0.65rem;color:var(--text3);font-style:italic">click a column header</span>';
    } else {
      const pills = keys.map((k, i) =>
        `<span class="combo-sort-pill">
          <span style="opacity:0.6;font-size:0.55rem;margin-right:0.1rem">${i+1}.</span>${cols[k.col]}
          <span class="combo-sort-pill-dir" onclick="_comboToggleDir('${tid}',${k.col})" title="Toggle direction">${k.asc ? '▲' : '▼'}</span>
          <span class="combo-sort-pill-rm" onclick="_comboRemove('${tid}',${k.col})" title="Remove">×</span>
        </span>`
      ).join('');
      bar.innerHTML = `<span class="combo-sort-bar-label">Sort by:</span>${_comboInfoBtn()}${pills}<button class="combo-sort-clear" onclick="_comboClear('${tid}')">Clear</button>`;
    }
  }

  // Update column header active state
  tbl.querySelectorAll('th').forEach((th, i) => {
    const btn = th.querySelector('.combo-th-btn');
    if (!btn) return;
    const ki = keys.findIndex(k => k.col === i);
    const arrEl = btn.querySelector('.combo-sort-arrow');
    if (ki >= 0) {
      btn.classList.add('active');
      arrEl.innerHTML = keys[ki].asc ? '▲' : '▼';
    } else {
      btn.classList.remove('active');
      arrEl.innerHTML = '<svg width="9" height="9" viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M3 4h18l-7 9v7l-4-2v-5z"/></svg>';
    }
  });

  // Sort a copy
  const sorted = [...data].sort((a, b) => {
    for (const { col, asc } of keys) {
      const va = _comboVal(a, col, totalSpins, betSize);
      const vb = _comboVal(b, col, totalSpins, betSize);
      if (va < vb) return asc ? -1 : 1;
      if (va > vb) return asc ? 1 : -1;
    }
    return 0;
  });

  // Re-render tbody
  const tbody = tbl.querySelector('tbody');
  // Build a stable odd/even group map so visual grouping survives any sort order
  const symIds = [...new Set(sorted.map(c => c.symbolId))];
  const symParity = new Map(symIds.map((id, i) => [id, i % 2]));
  tbody.innerHTML = sorted.map(c => {
    const hitRate    = (c.hitCount / totalSpins * 100).toFixed(3);
    const rtpContrib = (c.totalPayout / totalSpins * 100).toFixed(4);
    const avgPayout  = (c.totalPayout * betSize / c.hitCount).toFixed(1);
    const pct = maxPay > 0 ? Math.round(c.totalPayout / maxPay * 100) : 0;
    const cls = symParity.get(c.symbolId) === 1 ? ' class="combo-sym-odd"' : '';
    return `<tr${cls}>
      <td><span class="combo-sym-badge">S${c.symbolId}</span></td>
      <td>${c.matchCount}</td>
      <td>${c.hitCount.toLocaleString()}</td>
      <td>${hitRate}%</td>
      <td>${avgPayout}</td>
      <td class="combo-bar-cell"><div class="combo-bar-wrap"><div class="combo-bar"><div class="combo-bar-fill" style="width:${pct}%"></div></div><span class="combo-bar-pct">${rtpContrib}%</span></div></td>
    </tr>`;
  }).join('');
}

function _comboVal(c, col, totalSpins, betSize) {
  switch (col) {
    case 0: return c.symbolId;
    case 1: return c.matchCount;
    case 2: return c.hitCount;
    case 3: return c.hitCount / totalSpins;
    case 4: return c.totalPayout * betSize / c.hitCount;
    case 5: return c.totalPayout / totalSpins;
    default: return 0;
  }
}

/* ── Import from COUNT ── */
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
    errEl.textContent = 'Invalid JSON: ' + e.message;
    errEl.style.display = '';
    return;
  }
  if (!Array.isArray(parsed) || parsed.length === 0) {
    errEl.textContent = 'Expected a non-empty JSON array.';
    errEl.style.display = '';
    return;
  }
  for (const rs of parsed) {
    if (!Array.isArray(rs.reelSetTileCounts)) {
      errEl.textContent = 'Each entry must have a "reelSetTileCounts" array.';
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

// Init: add default payline placeholder rows without filling screen inputs
(function initDefaultPaylines() {
  const DEFAULT_LINES = [
    '0, 0, 0, 0, 0', '1, 1, 1, 1, 1', '2, 2, 2, 2, 2',
    '0, 1, 0, 1, 0', '1, 0, 1, 0, 1', '2, 1, 2, 1, 2',
    '1, 2, 1, 2, 1', '0, 1, 1, 1, 0', '2, 1, 1, 1, 2', '0, 2, 0, 2, 0',
  ];
  const container = document.getElementById('rtp-lines-list');
  container.innerHTML = '';
  _lineCounter = 0;
  DEFAULT_LINES.forEach(ph => {
    addLineDef('');
    const rows = container.querySelectorAll('.rtp-line-input');
    rows[rows.length - 1].placeholder = ph;
  });
  updateLineDefsToggleBtn();
})();

/* ── Spin Test Tab ── */

function buildSpinTestPayload() {
  const errors = [];

  if (!_latestReelSets || _latestReelSets.length === 0) {
    tryLoadReelsFromEditor();
  }
  if (!_latestReelSets || _latestReelSets.length === 0)
    return { errors: ['No generated reels found. Run Generate first.'] };

  // Reuse rtp form fields for the shared configuration
  const chances = [];
  let chanceSum = 0;
  _latestReelSets.forEach((_, i) => {
    const el = document.getElementById('rtp-chance-' + i);
    const v  = parseFloat(el?.value);
    if (isNaN(v) || v < 0) { errors.push('Reel set ' + i + ': chance must be >= 0'); return; }
    chances.push({ setIndex: i, chance: v });
    chanceSum += v;
  });
  if (Math.abs(chanceSum - 100) > 0.05)
    errors.push('Reel set chances must sum to 100.0% (current: ' + chanceSum.toFixed(1) + '%). Configure in the Simulation tab.');

  const screenWidth  = parseInt(document.getElementById('rtp-screen-width')?.value);
  const screenHeight = parseInt(document.getElementById('rtp-screen-height')?.value);
  const minMatch     = parseInt(document.getElementById('rtp-min-match')?.value);
  if (isNaN(screenWidth)  || screenWidth  < 1) errors.push('Screen width not set — configure in Simulation tab');
  if (isNaN(screenHeight) || screenHeight < 1) errors.push('Screen height not set — configure in Simulation tab');
  if (isNaN(minMatch) || minMatch < 1)         errors.push('Min Match not set — configure in Simulation tab');

  const lineDefinitions = [];
  document.querySelectorAll('.rtp-line-row').forEach((row) => {
    const nums = row.querySelector('.rtp-line-input').value.trim().split(',').map(s => parseInt(s.trim(), 10));
    if (!nums.some(isNaN)) lineDefinitions.push(nums);
  });
  if (lineDefinitions.length === 0) errors.push('No line definitions — configure in Simulation tab');

  const symbols = [];
  document.querySelectorAll('.rtp-sym-row').forEach(row => {
    const symId  = parseInt(row.dataset.symId);
    const type   = row.querySelector('select').value;
    const ptRaw  = (row.querySelector('input[type=text]').value.trim() || row.querySelector('input[type=text]').placeholder.trim()).replace(/^n\/a$/i, '');
    const paytable = ptRaw ? ptRaw.split(',').map(s => parseFloat(s.trim())) : [];
    const wildMult = parseFloat(row.querySelector('.rtp-wild-mult')?.value) || 1.0;
    const wildAgg  = row.querySelector('.rtp-wild-agg')?.value || 'ADD';
    const seqRaw   = row.querySelector('.rtp-wild-seq')?.value.trim() || '';
    const wildSequence = (wildAgg === 'SEQUENCE' && seqRaw) ? seqRaw.split(',').map(s => parseFloat(s.trim())) : [];
    symbols.push({ symbolId: symId, type, paytable, wildMultiplier: wildMult, wildAggregation: wildAgg, wildSequence });
  });
  if (symbols.length === 0) errors.push('No symbols configured — configure in Simulation tab');

  if (errors.length > 0) return { errors };

  const strategy = document.getElementById('rtp-strategy')?.value || 'LTR';
  const count    = parseInt(document.querySelector('input[name="spin-count"]:checked')?.value) || 1;

  const reelIdxRaw = document.getElementById('spin-test-reel-idx').value.trim();
  const reelSetIndex = reelIdxRaw !== '' ? parseInt(reelIdxRaw) : null;

  const stopsRaw = document.getElementById('spin-test-stops').value.trim();
  const stops = stopsRaw ? stopsRaw.split(',').map(s => parseInt(s.trim(), 10)) : null;

  let screen = null;
  const screenRaw = document.getElementById('spin-test-screen').value.trim();
  if (screenRaw) {
    try {
      screen = JSON.parse(screenRaw);
    } catch(e) {
      return { errors: ['Fixed screen: invalid JSON — ' + e.message] };
    }
  }

  return {
    errors: [],
    payload: {
      reelSets: _latestReelSets,
      reelSetChances: chances,
      symbols,
      strategy,
      screenWidth,
      screenHeight,
      minMatch,
      lineDefinitions,
      count,
      reelSetIndex,
      stops,
      screen
    }
  };
}

async function runSpinTest() {
  const btn = document.getElementById('spin-test-run-btn');
  const { errors, payload } = buildSpinTestPayload();
  if (errors.length > 0) {
    showToast(errors[0], true);
    setStatus('spin-test', false, errors[0]);
    return;
  }

  btn.disabled = true;
  setStatus('spin-test', null, 'Running…');

  const body = document.getElementById('spin-test-results-body');
  body.innerHTML = `<div class="rtp-placeholder"><span>Generating spins…</span></div>`;

  try {
    const res = await fetch('/api/spin-test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const d = await res.json();
    if (d.error) {
      setStatus('spin-test', false, d.error);
      body.innerHTML = `<div class="rtp-placeholder"><span style="color:var(--error)">${escapeHtml(d.error)}</span></div>`;
    } else {
      const spins = JSON.parse(d.result);
      renderSpinTestResults(body, spins, payload);
      setStatus('spin-test', true, 'Done');
    }
  } catch(e) {
    setStatus('spin-test', false, 'Network error');
    body.innerHTML = `<div class="rtp-placeholder"><span style="color:var(--error)">Network error</span></div>`;
  } finally {
    btn.disabled = false;
  }
}

function renderSpinTestResults(container, spins, payload) {
  if (!spins || spins.length === 0) {
    container.innerHTML = `<div class="rtp-placeholder"><span>No results</span></div>`;
    return;
  }

  const wildIds = new Set(
    (payload.symbols || []).filter(s => s.type === 'WILD').map(s => s.symbolId)
  );
  const scatterIds = new Set(
    (payload.symbols || []).filter(s => s.type === 'SCATTER').map(s => s.symbolId)
  );

  function buildCardHtml(spin, idx) {
    const setLabel = spin.reelSetIndex >= 0 ? `ReelSet #${spin.reelSetIndex}` : 'Fixed Screen';
    const stopsStr = spin.reelsStopPositions && spin.reelsStopPositions.length > 0 ? `[${spin.reelsStopPositions.join(', ')}]` : '—';
    const totalWin = (spin.payoutData || []).reduce((s, e) => s + e.winAmount, 0);
    const screenHtml = (spin.screen || []).map(col => {
      const cells = col.map(sym => {
        const isWild    = wildIds.has(sym);
        const isScatter = scatterIds.has(sym);
        const cls = isWild ? ' wild' : isScatter ? ' scatter' : '';
        return `<div class="spin-screen-cell${cls}">${sym}</div>`;
      }).join('');
      return `<div class="spin-screen-col">${cells}</div>`;
    }).join('');
    let payoutHtml;
    if (!spin.payoutData || spin.payoutData.length === 0) {
      payoutHtml = `<span class="spin-no-win">No winning combinations</span>`;
    } else {
      const isWaysWrapper = w => w._className && w._className.includes('WayLinesDto');
      const rows = spin.payoutData.flatMap(wrapper => {
        if (isWaysWrapper(wrapper)) {
          return (wrapper.wayLines || []).map(e => {
            const ways        = e.ways        ? `[${e.ways.join(', ')}]`                 : '—';
            const waysWithMul = e.waysWithWaysMultipliers ? `[${e.waysWithWaysMultipliers.join(', ')}]` : '—';
            return `<tr>
              <td>${e.floatId}</td>
              <td>WAYS</td>
              <td>${e.lineSize}</td>
              <td>0</td>
              <td>${e.payoutSymbolId}</td>
              <td>${ways}</td>
              <td>${waysWithMul}</td>
              <td>${e.totalSimpleLines}</td>
              <td style="color:var(--success);font-weight:700">${e.winAmount.toFixed(2)}</td>
            </tr>`;
          });
        }
        return (wrapper.lines || []).map(e => {
          const lineDef = e.lineDefinition ? `[${e.lineDefinition.join(', ')}]` : '—';
          const lineSymbols = e.lineSymbols ? `[${e.lineSymbols.join(', ')}]` : '—';
          return `<tr>
            <td>${e.lineId}</td>
            <td>${e.matchType}</td>
            <td>${e.lineSize}</td>
            <td>${e.lineStart}</td>
            <td>${e.payoutSymbolId}</td>
            <td>${lineDef}</td>
            <td>${lineSymbols}</td>
            <td>${e.lineMultiplier}</td>
            <td style="color:var(--success);font-weight:700">${e.winAmount.toFixed(2)}</td>
          </tr>`;
        });
      }).join('');
      const hasWays = spin.payoutData.some(isWaysWrapper);
      const headers = hasWays
        ? `<tr><th>Line ID</th><th>Match</th><th>Line Size</th><th>Start Reel</th><th>Tile ID</th><th>Ways</th><th>Ways×Mult</th><th>Simple Lines</th><th>Win</th></tr>`
        : `<tr><th>Line ID</th><th>Match</th><th>Line Size</th><th>Start Reel</th><th>Tile ID</th><th>Line Definition</th><th>Line Symbols</th><th>Multiplier</th><th>Win</th></tr>`;
      payoutHtml = `
        <table class="spin-payout-table">
          <thead>${headers}</thead>
          <tbody>${rows}</tbody>
        </table>`;
    }
    return `
      <div class="spin-result-card-header">
        <span class="spin-result-card-title">Spin ${idx + 1}</span>
        <span class="spin-result-card-meta">${setLabel} &nbsp;·&nbsp; stops: ${stopsStr} &nbsp;·&nbsp; total win: <strong style="color:var(--accent)">${totalWin.toFixed(2)}</strong></span>
      </div>
      <div class="spin-result-card-body">
        <div class="spin-screen-grid">${screenHtml}</div>
        ${payoutHtml}
      </div>`;
  }

  container.innerHTML = '';

  // ── Carousel ──
  const n = spins.length;
  let current = 0;
  let highlightSpin = null; // assigned after cm is created

  const carousel = document.createElement('div');
  carousel.className = 'spin-carousel';

  const navBar = document.createElement('div');
  navBar.className = 'spin-carousel-nav';

  const counter = document.createElement('span');
  counter.className = 'spin-carousel-counter';

  const btnPrev = document.createElement('button');
  btnPrev.className = 'spin-carousel-btn';
  btnPrev.title = 'Previous spin';
  btnPrev.innerHTML = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>';

  const btnNext = document.createElement('button');
  btnNext.className = 'spin-carousel-btn';
  btnNext.title = 'Next spin';
  btnNext.innerHTML = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>';

  const btns = document.createElement('div');
  btns.className = 'spin-carousel-btns';
  btns.appendChild(btnPrev);
  btns.appendChild(btnNext);

  navBar.appendChild(counter);
  navBar.appendChild(btns);

  const slide = document.createElement('div');
  slide.className = 'spin-carousel-slide';

  carousel.appendChild(navBar);
  carousel.appendChild(slide);

  function showSlide(idx) {
    current = Math.max(0, Math.min(n - 1, idx));
    counter.textContent = `Spin ${current + 1} / ${n}`;
    slide.innerHTML = buildCardHtml(spins[current], current);
    btnPrev.disabled = current === 0;
    btnNext.disabled = current === n - 1;
    if (highlightSpin) highlightSpin(current);
  }

  btnPrev.addEventListener('click', () => showSlide(current - 1));
  btnNext.addEventListener('click', () => showSlide(current + 1));
  showSlide(0);

  container.appendChild(carousel);

  // ── JSON DTO pane ──
  const copyBtnHtml = `<button class="copy-btn" title="Copy JSON">
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
  </button>`;
  const pane = document.createElement('div');
  pane.className = 'spin-test-json-pane';
  pane.innerHTML = `
    <div class="pane-header">
      <span class="pane-title">JSON DTO</span>
      ${copyBtnHtml}
    </div>
    <div class="pane-body"></div>`;
  container.appendChild(pane);

  const dtos = spins;

  // Compute per-spin line ranges by serialising each DTO individually first
  const spinJsons = dtos.map(d => compactJson(d));
  const lineRanges = [];
  let lineOffset = 1; // line 0 = "["
  for (let i = 0; i < spinJsons.length; i++) {
    const indented = spinJsons[i].split('\n').map(l => '  ' + l);
    const from = lineOffset;
    const to   = lineOffset + indented.length - 1;
    lineRanges.push({ from, to });
    lineOffset = to + 1; // next spin starts on the very next line (comma is on the last line of current)
  }

  // Build full JSON: "[" + indented items joined by "," + "]"
  const json = '[\n' + spinJsons.map(j => j.split('\n').map(l => '  ' + l).join('\n')).join(',\n') + '\n]';

  const cmContainer = pane.querySelector('.pane-body');
  const cm = CodeMirror(cmContainer, {
    ...cmOpts,
    readOnly: true,
    value: json,
    foldGutter: true,
    gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
    extraKeys: { 'Ctrl-Q': cm => cm.foldCode(cm.getCursor()) }
  });
  setTimeout(() => {
    cm.refresh();
  }, 0);

  // Wire carousel ↔ JSON highlight
  let prevHighlightRange = null;
  highlightSpin = function(idx) {
    if (prevHighlightRange) {
      for (let l = prevHighlightRange.from; l <= prevHighlightRange.to; l++)
        cm.removeLineClass(l, 'background', 'spin-dto-highlight');
    }
    const r = lineRanges[idx];
    if (r) {
      for (let l = r.from; l <= r.to; l++)
        cm.addLineClass(l, 'background', 'spin-dto-highlight');
      cm.scrollIntoView({ line: r.from, ch: 0 }, 40);
      prevHighlightRange = r;
    }
  };
  highlightSpin(0);

  pane.querySelector('.copy-btn').addEventListener('click', function() {
    navigator.clipboard.writeText(cm.getValue()).then(() => {
      const btn = this;
      btn.classList.add('copied');
      const orig = btn.innerHTML;
      btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
      setTimeout(() => { btn.classList.remove('copied'); btn.innerHTML = orig; }, 1800);
    });
  });
}

function compactJson(obj) {
  // Serialize with 2-space indent, then collapse arrays of primitives onto one line
  const raw = JSON.stringify(obj, null, 2);
  return raw.replace(/\[\s*([\d,\s-]+?)\s*\]/g, m => '[' + m.replace(/\s+/g, '').slice(1,-1).split(',').join(', ') + ']');
}

/* ── Tooltip fixed-position positioning ── */
(function () {
  const GAP = 8;

  function applyPosition(tip, badge) {
    const br = badge.getBoundingClientRect();
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const tipW = tip.offsetWidth || parseInt(tip.style.width) || 260;
    const tipH = tip.offsetHeight || 100;

    const rightEdge = br.right + GAP + tipW;
    if (rightEdge <= vw) {
      tip.style.left = (br.right + GAP) + 'px';
      const centeredTop = br.top + br.height / 2 - tipH / 2;
      tip.style.top = Math.max(8, Math.min(vh - tipH - 8, centeredTop)) + 'px';
    } else {
      tip.style.left = Math.max(8, Math.min(vw - tipW - 8, br.left + br.width / 2 - tipW / 2)) + 'px';
      tip.style.top = Math.max(8, br.top - tipH - GAP) + 'px';
    }
  }

  function positionTip(wrap) {
    const tip = wrap.querySelector('.stat-tip-box');
    if (!tip) return;
    const badge = wrap.querySelector('.stat-info') || wrap;

    // Apply fixed positioning immediately so overflow clipping can't hide it
    tip.style.position = 'fixed';
    tip.style.zIndex = '9999';
    tip.style.left = '-9999px'; // park off-screen until measured
    tip.style.top = '0';
    tip.style.bottom = '';
    tip.style.transform = '';

    // Defer measurement until after :hover CSS has rendered and element is visible
    requestAnimationFrame(() => {
      requestAnimationFrame(() => applyPosition(tip, badge));
    });
  }

  function resetTip(wrap) {
    const tip = wrap.querySelector('.stat-tip-box');
    if (!tip) return;
    tip.style.position = '';
    tip.style.zIndex = '';
    tip.style.left = '';
    tip.style.top = '';
    tip.style.bottom = '';
    tip.style.transform = '';
  }

  document.addEventListener('mouseover', function (e) {
    const wrap = e.target.closest('.stat-tip-wrap');
    if (!wrap) return;
    positionTip(wrap);
  });

  document.addEventListener('mouseout', function (e) {
    const wrap = e.target.closest('.stat-tip-wrap');
    if (!wrap) return;
    if (!wrap.contains(e.relatedTarget)) resetTip(wrap);
  });
})();
