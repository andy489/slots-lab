/* ── RTP Simulation ── */

async function pushRtpHistory(label, resultHtml, payload, r) {
  const now = new Date();
  const time = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const id = Date.now().toString();
  await fetch('/api/history/simulate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id, strategy: label, time, result: resultHtml, config: JSON.stringify({ payload, r }) })
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
      if (!await confirmDelete(`${t('rtp.delete_entry')}${i + 1}?`)) return;
      const res = await fetch(`/api/history/simulate/${encodeURIComponent(entry.id)}`, { method: 'DELETE' });
      renderRtpHistory(await res.json());
    };
    item.onclick = () => {
      const body = document.getElementById('rtp-result-body');
      body.innerHTML = entry.result;
      _initComboSections(body);
      _rtpResultJsonMode = false;
      _rtpResultCardHtml = null;
      if (entry.config) {
        try {
          const cfg = JSON.parse(entry.config);
          // New format: { payload, r } — old format: payload directly
          const payload = cfg.payload || cfg;
          const r = cfg.r || null;
          _lastRtpResult = r ? { r, payload } : null;
          restoreRtpForm(payload);
        } catch(e) { _lastRtpResult = null; }
      }
      const jsonBtn = document.getElementById('rtp-result-json-btn');
      const cardBtn = document.getElementById('rtp-result-card-btn');
      if (jsonBtn) { jsonBtn.style.display = ''; jsonBtn.classList.remove('active'); }
      if (cardBtn) cardBtn.style.display = 'none';
      setStatus('rtp', true, t('rtp.restored'));
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
  if (!await confirmDelete(t('rtp.delete_sim_history'))) return;
  await fetch('/api/history/simulate', { method: 'DELETE' });
  renderRtpHistory([]);
}

let _lastRtpResult = null;   // { r, payload } — stored after each successful simulation
let _rtpResultJsonMode = false;
let _rtpResultCardHtml = null;

function toggleRtpResultJson() {
  if (!_lastRtpResult) { showToast('No raw result data — re-run the simulation to enable JSON view', true); return; }
  const body    = document.getElementById('rtp-result-body');
  const jsonBtn = document.getElementById('rtp-result-json-btn');
  const cardBtn = document.getElementById('rtp-result-card-btn');
  _rtpResultJsonMode = !_rtpResultJsonMode;
  if (_rtpResultJsonMode) {
    _rtpResultCardHtml = body.innerHTML;
    const { r } = _lastRtpResult;
    const out = Object.assign({}, r);
    if (Array.isArray(r.comboBreakdown)) {
      out.comboBreakdown = r.comboBreakdown
        .map(c => ({
          tileId:          c.symbolId,
          length:          c.matchLabel != null ? c.matchLabel : c.matchCount,
          hits:            c.hitCount,
          hitRate:         (c.hitCount / r.totalSpins * 100).toFixed(4) + '%',
          multiplier:      +(c.totalPayout / c.hitCount / r.betSize).toFixed(4),
          rtpContribution: (c.totalPayout / r.totalSpins * 100).toFixed(4) + '%',
        }))
        .sort((a, b) => a.tileId - b.tileId);
    }
    const json = JSON.stringify(out, null, 2);
    body.innerHTML = `
      <div class="rtp-json-toolbar">
        <button class="icon-btn" onclick="_copyRtpJson(this)" title="Copy JSON" style="display:flex;align-items:center;gap:0.3rem;width:auto;padding:0 6px;font-size:0.68rem">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
          Copy
        </button>
        <button class="icon-btn" onclick="_expandAllRtpJson(true)" title="Expand all" style="width:auto;padding:0 6px;font-size:0.68rem">Expand all</button>
        <button class="icon-btn" onclick="_expandAllRtpJson(false)" title="Collapse all" style="width:auto;padding:0 6px;font-size:0.68rem">Collapse all</button>
      </div>
      <div class="rtp-result-json-tree" id="rtp-json-tree">${_buildJsonTree(out)}</div>`;
    body.dataset.jsonRaw = json;
    if (jsonBtn) jsonBtn.style.display = 'none';
    if (cardBtn) cardBtn.style.display = '';
  } else {
    body.innerHTML = _rtpResultCardHtml;
    if (jsonBtn) jsonBtn.style.display = '';
    if (cardBtn) cardBtn.style.display = 'none';
  }
}

function _copyRtpJson(btn) {
  const body = document.getElementById('rtp-result-body');
  const json = body.dataset.jsonRaw || '';
  navigator.clipboard.writeText(json).then(() => {
    const orig = btn.innerHTML;
    btn.innerHTML = btn.innerHTML.replace('Copy', '✓ Copied');
    setTimeout(() => { btn.innerHTML = orig; }, 1500);
  });
}

function _expandAllRtpJson(expand) {
  document.querySelectorAll('#rtp-json-tree details').forEach(d => { d.open = expand; });
}

function _buildJsonTree(val, indent) {
  indent = indent || 0;
  const pad = '  '.repeat(indent);
  const padI = '  '.repeat(indent + 1);
  if (val === null) return '<span class="jt-null">null</span>';
  if (typeof val === 'boolean') return `<span class="jt-bool">${val}</span>`;
  if (typeof val === 'number') return `<span class="jt-num">${val}</span>`;
  if (typeof val === 'string') return `<span class="jt-str">"${escapeHtml(val)}"</span>`;
  if (Array.isArray(val)) {
    if (val.length === 0) return '<span class="jt-brace">[]</span>';
    const items = val.map((v, i) =>
      `<div class="jt-row">${padI}${_buildJsonTree(v, indent + 1)}${i < val.length - 1 ? '<span class="jt-punct">,</span>' : ''}</div>`
    ).join('');
    return `<details open><summary class="jt-brace">[<span class="jt-count">${val.length}</span>]</summary>${items}<span class="jt-brace">]</span></details>`;
  }
  if (typeof val === 'object') {
    const keys = Object.keys(val);
    if (keys.length === 0) return '<span class="jt-brace">{}</span>';
    const items = keys.map((k, i) =>
      `<div class="jt-row">${padI}<span class="jt-key">"${escapeHtml(k)}"</span><span class="jt-punct">: </span>${_buildJsonTree(val[k], indent + 1)}${i < keys.length - 1 ? '<span class="jt-punct">,</span>' : ''}</div>`
    ).join('');
    return `<details open><summary class="jt-brace">{<span class="jt-count">${keys.length}</span>}</summary>${items}<span class="jt-brace">}</span></details>`;
  }
  return escapeHtml(String(val));
}

function collectRtpRequest() {
  const errors = [];

  if (!_latestReelSets || _latestReelSets.length === 0) {
    tryLoadReelsFromEditor();
  }

  if (!_latestReelSets || _latestReelSets.length === 0)
    return { errors: [t('rtp.no_reels')] };

  const chances = [];
  let chanceSum = 0;
  const chancesInput = document.getElementById('rtp-chances-input');
  const chanceVals = chancesInput ? chancesInput.value.split(',').map(s => s.trim()).filter(Boolean) : [];
  if (chanceVals.length !== _latestReelSets.length) {
    errors.push(t('rtp.chances_count') || ('Expected ' + _latestReelSets.length + ' chances, got ' + chanceVals.length));
  } else {
    chanceVals.forEach((raw, i) => {
      const v = parseFloat(raw);
      if (isNaN(v) || v < 0) { errors.push('Reel set ' + i + ': ' + t('rtp.chance_gte0')); return; }
      chances.push({ setIndex: i, chance: v });
      chanceSum += v;
    });
    const roundedSum = Math.round(chanceSum * 1000000) / 1000000;
    if (roundedSum !== 100)
      errors.push(t('rtp.chances_sum') + ' (current: ' + roundedSum + '%)');
  }
  const _swEl = document.getElementById('rtp-screen-width');
  const _shEl = document.getElementById('rtp-screen-height');
  const _mmEl = document.getElementById('rtp-min-match');
  const screenWidth  = parseInt(_swEl.value  || _swEl.placeholder);
  const screenHeight = parseInt(_shEl.value  || _shEl.placeholder);
  const minMatch     = parseInt(_mmEl.value  || _mmEl.placeholder);
  if (isNaN(screenWidth)  || screenWidth  < 1) errors.push(t('rtp.width_gte1'));
  if (isNaN(screenHeight) || screenHeight < 1) errors.push(t('rtp.height_gte1'));
  if (isNaN(minMatch) || minMatch < 1) errors.push(t('rtp.min_match_gte1'));
  if (!isNaN(minMatch) && !isNaN(screenWidth) && minMatch > screenWidth)
    errors.push(t('rtp.min_match_exceeds'));

  const lineRows = document.querySelectorAll('.rtp-line-row');
  const _strategyForLineCheck = document.getElementById('rtp-strategy').value;
  const isWays = _strategyForLineCheck === 'WAYS' || _strategyForLineCheck === 'MEGAWAYS' || _strategyForLineCheck === 'SCATTERS' || _strategyForLineCheck === 'CLUSTERS';
  if (!isWays && lineRows.length === 0) errors.push(t('rtp.at_least_one_line'));
  const lineDefinitions = [];
  const parsedLines = [];

  lineRows.forEach((row, li) => {
    const inp = row.querySelector('.rtp-line-input');
    const raw = inp.value.trim();
    if (raw === '') {
      parsedLines.push(null);
      return;
    }
    if (!/^[\d ,]+$/.test(raw)) {
      errors.push('Line ' + (li+1) + ': ' + t('rtp.line_must_be_numbers'));
      parsedLines.push(null);
      return;
    }
    const nums = raw.split(',').map(s => parseInt(s.trim(), 10));
    if (nums.some(isNaN)) {
      errors.push('Line ' + (li+1) + ': ' + t('rtp.line_must_be_numbers'));
      parsedLines.push(null);
      return;
    }
    parsedLines.push(nums);
  });

  if (!isWays && parsedLines.every(n => n === null) && lineRows.length > 0)
    errors.push(t('rtp.line_must_be_filled'));

  parsedLines.forEach((nums, li) => {
    if (nums === null) return;
    if (!isNaN(screenWidth) && nums.length !== screenWidth)
      errors.push('Line ' + (li+1) + ': ' + t('rtp.line_must_have_positions') + ' ' + screenWidth + ' ' + 'positions');
    if (!isNaN(screenHeight) && nums.some(n => n < 0 || n >= screenHeight))
      errors.push('Line ' + (li+1) + ': ' + t('rtp.line_positions_range') + (screenHeight-1));
  });

  const seenLines = new Set();
  parsedLines.forEach((nums, li) => {
    if (nums === null) return;
    const key = JSON.stringify(nums);
    if (seenLines.has(key)) errors.push('Line ' + (li+1) + ': ' + t('rtp.line_duplicate'));
    else seenLines.add(key);
    lineDefinitions.push(nums);
  });

  const symbols = [];
  if (_rtpSymJsonMode && _rtpSymJsonCm) {
    try {
      const parsed = JSON.parse(_rtpSymJsonCm.getValue());
      const norm = _normalizeRtpPayload(Object.assign({ screenWidth, minMatch }, parsed));
      (norm.symbols || []).forEach(s => symbols.push(s));
    } catch(e) {
      errors.push('Symbol Config JSON parse error: ' + e.message);
    }
  } else {
  document.querySelectorAll('.rtp-sym-row').forEach(row => {
    const symId = parseInt(row.dataset.symId);
    const type = row.querySelector('select').value;
    const ptRaw = row.querySelector('.rtp-paytable-input').value.trim().replace(/^n\/a$/i, '');
    const paytable = ptRaw ? ptRaw.split(',').map(s => parseFloat(s.trim())) : [];
    const wildMult = parseFloat(row.querySelector('.rtp-wild-mult')?.value) || 1.0;
    const wildAgg  = row.querySelector('.rtp-wild-agg')?.value || 'ADD';
    const seqRaw   = row.querySelector('.rtp-wild-seq')?.value.trim() || '';
    const wildSequence = (wildAgg === 'SEQUENCE' && seqRaw)
      ? seqRaw.split(',').map(s => Math.round(parseFloat(s.trim()) * 10) / 10)
      : [];
    if (type === 'NORMAL' && paytable.length === 0)
      errors.push('Symbol ' + symId + ': ' + t('rtp.paytable_required'));
    if (paytable.some(isNaN))
      errors.push('Symbol ' + symId + ': ' + t('rtp.paytable_invalid'));
    if (paytable.some(v => !isNaN(v) && Math.round(v * 10) !== v * 10))
      errors.push('Symbol ' + symId + ': ' + t('rtp.paytable_multiples'));
    const setSel = row.querySelector('.rtp-interval-set-sel');
    const contactsIntervalSetName = setSel ? (setSel.value || null) : null;
    if (paytable.length > 0 && (type === 'NORMAL' || type === 'WILD')) {
      const _strat = document.getElementById('rtp-strategy').value;
      let required, requiredDesc;
      if (_strat === 'SCATTERS' || _strat === 'CLUSTERS') {
        const setName = contactsIntervalSetName;
        const containerId = _strat === 'CLUSTERS' ? 'clusters-interval-sets-container' : 'interval-sets-container';
        const card = setName
          ? Array.from(document.querySelectorAll('#' + containerId + ' .interval-set-card')).find(c => (c.dataset.setName || 'default') === setName)
          : document.querySelector('#' + containerId + ' .interval-set-card');
        required = card ? card.querySelectorAll('.scatter-interval-row').length : 0;
        requiredDesc = required + ' (one per interval in set "' + (setName || 'default') + '")';
      } else {
        required = Math.max(1, screenWidth - minMatch + 1);
        requiredDesc = required + ' (screenWidth − minMatch + 1 = ' + screenWidth + ' − ' + minMatch + ' + 1)';
      }
      if (!isNaN(screenWidth) && !isNaN(minMatch) && paytable.length !== required)
        errors.push('Symbol ' + symId + ': ' + t('rtp.paytable_count') + ' ' + requiredDesc + ' ' + t('rtp.value_per_reel'));
    }
    if (type === 'WILD' && wildAgg !== 'SEQUENCE' && wildAgg !== 'NONE' && (isNaN(wildMult) || wildMult <= 0))
      errors.push('Symbol ' + symId + ': ' + t('rtp.wild_mult_gt0'));
    if (wildAgg === 'SEQUENCE') {
      if (wildSequence.some(isNaN))
        errors.push('Symbol ' + symId + ': ' + t('rtp.seq_invalid'));
      if (wildSequence.length !== screenWidth)
        errors.push('Symbol ' + symId + ': ' + t('rtp.seq_length') + ' ' + screenWidth + ' ' + t('rtp.value_per_reel'));
    }
    symbols.push({ symbolId: symId, type, paytable, wildMultiplier: wildMult, wildAggregation: wildAgg, wildSequence, contactsIntervalSetName });
  });
  }

  if (symbols.length === 0) errors.push(t('rtp.at_least_one_symbol'));

  if (!symbols.some(s => s.type === 'NORMAL' && s.paytable.length > 0))
    errors.push(t('rtp.at_least_one_normal'));

  const strategy    = document.getElementById('rtp-strategy').value;
  const spins       = parseInt(document.getElementById('rtp-spins').value);
  const threadCount = parseInt(document.getElementById('rtp-threads').value);
  const betSize     = parseFloat(document.getElementById('rtp-bet-size').value);

  if (isNaN(spins) || spins <= 0) errors.push(t('rtp.spins_required'));
  if (isNaN(threadCount) || threadCount < 1 || threadCount > 8)
    errors.push(t('rtp.threads_range'));
  if (isNaN(betSize) || betSize < 0.1 || betSize > 200.0)
    errors.push(t('rtp.bet_range'));
  else if (Math.round(betSize * 10) !== betSize * 10)
    errors.push(t('rtp.bet_multiple'));

  let contactsIntervalSets = null;
  if (strategy === 'SCATTERS') {
    const sc = collectContactsIntervalSets();
    sc.errors.forEach(e => errors.push(e));
    contactsIntervalSets = sc.contactsIntervalSets;
  }
  if (strategy === 'CLUSTERS') {
    const sc = collectClustersIntervalSets();
    sc.errors.forEach(e => errors.push(e));
    contactsIntervalSets = sc.contactsIntervalSets;
  }
  let adjacencyOffsets = null;
  if (strategy === 'CLUSTERS') {
    adjacencyOffsets = collectAdjacencyOffsets();
    if (!adjacencyOffsets || adjacencyOffsets.length === 0)
      errors.push(t('rtp.adj_offset_required'));
  }

  let megawaysReelHeightChances = null;
  if (strategy === 'MEGAWAYS') {
    megawaysReelHeightChances = collectMegawaysHeightChances();
    if (megawaysReelHeightChances) {
      megawaysReelHeightChances.forEach((setData, s) => {
        (setData || []).forEach((reelData, r) => {
          if (!reelData) return;
          if (reelData.length !== 6)
            errors.push('Reel Tiles Count Chances: set ' + s + ' reel ' + (r+1) + ' ' + t('rtp.megaways_6_values'));
          else if (reelData.some(isNaN) || reelData.some(v => v < 0))
            errors.push('Reel Tiles Count Chances: set ' + s + ' reel ' + (r+1) + ' ' + t('rtp.megaways_invalid'));
          else {
            const sum = reelData.reduce((a, b) => a + b, 0);
            if (Math.abs(sum - 100) > 0.1)
              errors.push('Reel Tiles Count Chances: set ' + s + ' reel ' + (r+1) + ' ' + t('rtp.megaways_sum100') + ' (current: ' + sum.toFixed(1) + ')');
          }
        });
      });
    }
  }

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
      betSize,
      contactsIntervalSets,
      adjacencyOffsets,
      megawaysReelHeightChances
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
  setStatus('rtp', null, t('rtp.running'));
  _rtpResultJsonMode = false;
  const jsonBtn = document.getElementById('rtp-result-json-btn');
  const cardBtn = document.getElementById('rtp-result-card-btn');
  if (jsonBtn) jsonBtn.style.display = 'none';
  if (cardBtn) cardBtn.style.display = 'none';

  const body = document.getElementById('rtp-result-body');
  body.innerHTML = `
    <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;flex:1;gap:1rem;color:var(--text3)">
      <span style="font-size:0.82rem">${t('rtp.simulating')} ${payload.spins.toLocaleString()} ${t('rtp.spins_on')} ${payload.threadCount} ${t('rtp.threads_suffix')}</span>
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
      _lastRtpResult = { r, payload };
      _rtpResultJsonMode = false;
      _rtpResultCardHtml = null;
      renderRtpResult(body, r, payload);
      const jsonBtn = document.getElementById('rtp-result-json-btn');
      const cardBtn = document.getElementById('rtp-result-card-btn');
      if (jsonBtn) { jsonBtn.style.display = ''; }
      if (cardBtn) cardBtn.style.display = 'none';
      setStatus('rtp', true, t('rtp.done'));
      const label = `${r.rtpPercent.toFixed(2)}% · ${payload.strategy} · ${(r.totalSpins/1e6).toFixed(1)}M`;
      await pushRtpHistory(label, body.innerHTML, payload, r);
    }
  } catch(e) {
    setStatus('rtp', false, t('rtp.network_error'));
    body.innerHTML = `<div class="rtp-placeholder"><span style="color:var(--error)">${t('rtp.network_error')}</span></div>`;
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

  const _stratName = {
    LTR: t('result.strategy_ltr'), RTL: t('result.strategy_rtl'),
    BW: t('result.strategy_bw'), ADJ: t('result.strategy_adj'),
    SL: t('result.strategy_sl'), WAYS: t('result.strategy_ways'),
    MEGAWAYS: t('result.strategy_megaways'), SCATTERS: t('result.strategy_scatters'),
    CLUSTERS: t('result.strategy_clusters')
  };
  const _stratTip = {
    LTR: t('result.strategy_ltr_tip'), RTL: t('result.strategy_rtl_tip'),
    BW: t('result.strategy_bw_tip'), ADJ: t('result.strategy_adj_tip'),
    SL: t('result.strategy_sl_tip'), WAYS: t('result.strategy_ways_tip'),
    MEGAWAYS: t('result.strategy_megaways_tip'), SCATTERS: t('result.strategy_scatters_tip'),
    CLUSTERS: t('result.strategy_clusters_tip')
  };
  const _volLabel = {
    'Low': t('result.volatility_low'), 'Medium': t('result.volatility_medium'),
    'High': t('result.volatility_high'), 'Extreme': t('result.volatility_extreme')
  };
  const volLabelTr = _volLabel[r.volatilityLabel] || r.volatilityLabel;

  const _volTip = t('result.vol_index_tip_prefix') +
    `<div class="tip-row"><span>${t('result.volatility_low')}</span><span>&lt; 2.0</span></div>` +
    `<div class="tip-row"><span>${t('result.volatility_medium')}</span><span>2.0 – 5.0</span></div>` +
    `<div class="tip-row"><span>${t('result.volatility_high')}</span><span>5.0 – 10.0</span></div>` +
    `<div class="tip-row"><span>${t('result.volatility_extreme')}</span><span>≥ 10.0</span></div>` +
    t('result.vol_index_tip_suffix');

  container.innerHTML = `
    <div class="rtp-result-card">
      <span class="rtp-result-label">${t('result.return_to_player')}</span>
      <span class="rtp-big-number">${rtp}%</span>
      <span class="rtp-meta">${t('result.strategy')}: ${payload.strategy} &nbsp;·&nbsp; ${spins} ${t('result.spins')} &nbsp;·&nbsp; ${payload.threadCount} ${t('result.threads')} &nbsp;·&nbsp; ${elapsed}</span>
      <span class="rtp-note">${t('result.stats_note')}</span>
    </div>
    <div class="rtp-stats-grid">

      <div class="rtp-stat-group">
        <span class="rtp-stat-group-title">${t('result.simulation')}</span>
        <div class="rtp-stat-row">
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.spins_simulated')}</span>
            <span class="rtp-stat-value">${spins}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.elapsed_time')}</span>
            <span class="rtp-stat-value">${elapsed}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.bet_size')}</span>
            <span class="rtp-stat-value">${fmt2(r.betSize)}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.hit_rate')}</span>
            <span class="rtp-stat-value">${fmt2(r.hitRatePct)}%</span>
          </div>
        </div>
      </div>

      <div class="rtp-stat-group">
        <span class="rtp-stat-group-title">${t('result.distribution')}</span>
        <div class="rtp-stat-row">
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.avg_win')}<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:200px">${t('result.avg_win_tip')}</span></span></span>
            <span class="rtp-stat-value">${fmt4(r.avgWin)}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.median')}<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:200px">${t('result.median_tip')}</span></span></span>
            <span class="rtp-stat-value">${fmt1(r.medianWin)}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.max_win')}<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:210px">${t('result.max_win_tip')}</span></span></span>
            <span class="rtp-stat-value">${fmt1(r.maxWin)}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.std_dev')}<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:220px">${t('result.std_dev_tip')}</span></span></span>
            <span class="rtp-stat-value">${fmt4(r.stdDev)}</span>
          </div>
        </div>
      </div>

      <div class="rtp-stat-group">
        <span class="rtp-stat-group-title">${t('result.volatility_group')}</span>
        <div class="rtp-stat-row">
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.volatility_index')}<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:220px">${_volTip}</span></span></span>
            <span class="rtp-stat-value">${fmt2(r.volatilityIndex)}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.volatility')}</span>
            <span class="rtp-stat-value">${volLabelTr}</span>
          </div>
        </div>
      </div>

      <div class="rtp-stat-group">
        <span class="rtp-stat-group-title">${t('result.configuration')}</span>
        <div class="rtp-stat-row">
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.payout_strategy')}<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:230px">${(_stratTip[payload.strategy] || payload.strategy)}<div class="tip-rule">${t('result.strategy_payline_note')}</div></span></span></span>
            <span class="rtp-stat-value">${_stratName[payload.strategy] || payload.strategy}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('result.screen_size')}<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:210px">${t('result.screen_size_tip')}</span></span></span>
            <span class="rtp-stat-value">${payload.screenWidth}×${payload.screenHeight}</span>
          </div>
          <div class="rtp-stat-card">
            <span class="rtp-stat-label">${t('rtp.min_match')}<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:210px">${t('result.min_match_tip')}</span></span></span>
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
            <td>${c.matchLabel ?? c.matchCount}</td>
            <td>${c.hitCount.toLocaleString()}</td>
            <td>${hitRate}%</td>
            <td>${avgPayout}</td>
            <td class="combo-bar-cell"><div class="combo-bar-wrap"><div class="combo-bar"><div class="combo-bar-fill" style="width:${pct}%"></div></div><span class="combo-bar-pct">${rtpContrib}%</span></div></td>
          </tr>`;
        }).join('');
        const uid  = 'cb' + Date.now();
        const tid  = uid + 't';
        const cols = [t('result.col_symbol'),t('result.col_length'),t('result.col_hits'),t('result.col_hit_rate'),t('result.col_multiplier'),t('result.col_rtp_contrib')];
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
            ${t('result.combo_breakdown')} <span style="opacity:0.5;font-weight:400;text-transform:none;letter-spacing:0">(${combos.length} ${t('result.combinations')})</span>
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
    tbl._cols       = [t('result.col_symbol'),t('result.col_length'),t('result.col_hits'),t('result.col_hit_rate'),t('result.col_multiplier'),t('result.col_rtp_contrib')];
    _comboRender(tbl);
  });
}

/* ── Combo table multi-sort ── */
function _comboInfoBtn() {
  return `<span class="stat-tip-wrap"><i class="stat-info">i</i><span class="stat-tip-box tip-right" style="width:280px;text-transform:none;letter-spacing:0;font-weight:400">${t('result.sort_tip_html')}</span></span>`;
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
    keys.push({ col: colIdx, asc: true });
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

  const bar = tbl._barEl;
  if (bar) {
    if (keys.length === 0) {
      bar.innerHTML = `<span class="combo-sort-bar-label">${t('result.sort_by')}</span>` + _comboInfoBtn() + `<span style="font-size:0.65rem;color:var(--text3);font-style:italic">${t('result.sort_click_header')}</span>`;
    } else {
      const pills = keys.map((k, i) =>
        `<span class="combo-sort-pill">
          <span style="opacity:0.6;font-size:0.55rem;margin-right:0.1rem">${i+1}.</span>${cols[k.col]}
          <span class="combo-sort-pill-dir" onclick="_comboToggleDir('${tid}',${k.col})" title="${t('result.sort_toggle_dir')}">${k.asc ? '▲' : '▼'}</span>
          <span class="combo-sort-pill-rm" onclick="_comboRemove('${tid}',${k.col})" title="${t('result.sort_remove')}">×</span>
        </span>`
      ).join('');
      bar.innerHTML = `<span class="combo-sort-bar-label">${t('result.sort_by')}</span>${_comboInfoBtn()}${pills}<button class="combo-sort-clear" onclick="_comboClear('${tid}')">${t('result.sort_clear')}</button>`;
    }
  }

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

  const sorted = [...data].sort((a, b) => {
    for (const { col, asc } of keys) {
      const va = _comboVal(a, col, totalSpins, betSize);
      const vb = _comboVal(b, col, totalSpins, betSize);
      if (va < vb) return asc ? -1 : 1;
      if (va > vb) return asc ? 1 : -1;
    }
    return 0;
  });

  const tbody = tbl.querySelector('tbody');
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
      <td>${c.matchLabel ?? c.matchCount}</td>
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

/* ── Screen & Paytable JSON mode ── */
let _rtpSimJsonMode = false;
let _rtpSimJsonCm = null;

function _getRtpSimJsonCm() {
  if (_rtpSimJsonCm) return _rtpSimJsonCm;
  const el = document.getElementById('rtp-sim-json-editor');
  _rtpSimJsonCm = CodeMirror(el, {
    mode: { name: 'javascript', json: true },
    theme: 'rsg',
    lineNumbers: true,
    matchBrackets: true,
    autoCloseBrackets: true,
    styleActiveLine: true,
    styleSelectedText: true,
    indentUnit: 2,
    tabSize: 2,
    extraKeys: {
      'Cmd-S':  cm => cm.setCursor({ line: cm.getCursor().line, ch: 0 }),
      'Ctrl-S': cm => cm.setCursor({ line: cm.getCursor().line, ch: 0 }),
      'Cmd-E':  cm => { const l = cm.getCursor().line; cm.setCursor({ line: l, ch: cm.getLine(l).length }); },
      'Ctrl-E': cm => { const l = cm.getCursor().line; cm.setCursor({ line: l, ch: cm.getLine(l).length }); },
      'Cmd-X': cm => {
        if (cm.getSelection()) return CodeMirror.Pass;
        const line = cm.getCursor().line;
        const from = { line, ch: 0 };
        const to   = line < cm.lastLine() ? { line: line + 1, ch: 0 } : { line, ch: cm.getLine(line).length };
        cm.replaceRange('', from, to);
      },
      'Ctrl-X': cm => {
        if (cm.getSelection()) return CodeMirror.Pass;
        const line = cm.getCursor().line;
        const from = { line, ch: 0 };
        const to   = line < cm.lastLine() ? { line: line + 1, ch: 0 } : { line, ch: cm.getLine(line).length };
        cm.replaceRange('', from, to);
      },
      'Cmd-C': cm => {
        if (cm.getSelection()) return CodeMirror.Pass;
        navigator.clipboard.writeText(cm.getLine(cm.getCursor().line));
      },
      'Ctrl-C': cm => {
        if (cm.getSelection()) return CodeMirror.Pass;
        navigator.clipboard.writeText(cm.getLine(cm.getCursor().line));
      },
    },
  });
  return _rtpSimJsonCm;
}

function _buildRtpSimConfigFromForm() {
  const screenWidth = parseInt(document.getElementById('rtp-screen-width')?.value) || null;
  const screenHeight= parseInt(document.getElementById('rtp-screen-height')?.value) || null;
  const minMatch    = parseInt(document.getElementById('rtp-min-match')?.value) || null;

  const lineDefinitions = [];
  document.querySelectorAll('.rtp-line-row').forEach(row => {
    const inp = row.querySelector('.rtp-line-input');
    const raw = inp ? inp.value.trim() : '';
    if (raw) lineDefinitions.push(raw.split(',').map(s => parseInt(s.trim(), 10)));
  });

  const cfg = {
    screen: { cols: screenWidth, rows: screenHeight },
    minMatch,
    lineDefinitions,
  };

  return cfg;
}

function _prettyRtpSimConfig(cfg) {
  // Serialize lineDefinitions 2 per row
  const lineDefs = cfg.lineDefinitions;
  const lineDefsPlaceholder = '__LINE_DEFS__';
  const cfgCopy = Object.assign({}, cfg, {lineDefinitions: lineDefsPlaceholder});
  let text = JSON.stringify(cfgCopy, null, 2);
  // Build compact inner items then group 2 per line
  const items = lineDefs.map(line => '[' + line.join(',') + ']');
  const rows = [];
  for (let i = 0; i < items.length; i += 2) rows.push(items.slice(i, i + 2).join(', '));
  const compact = rows.length === 0 ? '[]'
    : '[\n    ' + rows.join(',\n    ') + '\n  ]';
  text = text.replace('"' + lineDefsPlaceholder + '"', compact);
  return text;
}

function toggleRtpSimJsonMode() {
  _rtpSimJsonMode = !_rtpSimJsonMode;
  const formBody  = document.getElementById('rtp-sim-form-body');
  const editorEl  = document.getElementById('rtp-sim-json-editor');
  const errEl     = document.getElementById('rtp-sim-json-error');
  const jsonBtn   = document.getElementById('rtp-sim-json-mode-btn');
  const formBtn   = document.getElementById('rtp-sim-form-mode-btn');
  const formOnlyBtns = document.getElementById('line-defs-header-btns');

  if (_rtpSimJsonMode) {
    const cfg = _buildRtpSimConfigFromForm();
    const text = _prettyRtpSimConfig(cfg);
    formBody.style.display  = 'none';
    editorEl.style.display  = '';
    errEl.style.display     = 'none';
    jsonBtn.style.display   = 'none';
    formBtn.style.display   = '';
    _getRtpSimJsonCm().setValue(text);
    // Guard: ensure reel-json-active (generate tab's expand class) is not accidentally
    // applied to this card — it collapses the card height to 2px in a flex column
    const _simCard = document.getElementById('line-defs-card');
    if (_simCard) _simCard.classList.remove('reel-json-active');
    setTimeout(() => {
      _getRtpSimJsonCm().refresh();
      const scrollContainer = document.querySelector('.form-scroll');
      const card = document.getElementById('line-defs-card');
      if (card) card.classList.remove('reel-json-active');
      if (scrollContainer && card) {
        scrollContainer.scrollTop = card.offsetTop - 8;
      }
    }, 50);
    // Hide other header buttons when in JSON mode
    if (formOnlyBtns) Array.from(formOnlyBtns.children).forEach(btn => {
      if (btn.id !== 'rtp-sim-json-mode-btn' && btn.id !== 'rtp-sim-form-mode-btn') {
        btn.dataset._jsHide = btn.style.display || '';
        btn.style.display = 'none';
      }
    });
  } else {
    const raw = _rtpSimJsonCm ? _rtpSimJsonCm.getValue().trim() : '';
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        if (typeof parsed !== 'object' || Array.isArray(parsed))
          throw new Error('Expected a JSON object');
        restoreRtpForm(_normalizeRtpPayload(parsed));
      } catch (e) {
        const { loc, hint } = _jsonParseError(raw, e);
        errEl.textContent = 'JSON parse error' + loc + hint + ': ' + e.message;
        errEl.style.display = '';
        _rtpSimJsonMode = true;
        return;
      }
    }
    formBody.style.display  = '';
    editorEl.style.display  = 'none';
    errEl.style.display     = 'none';
    jsonBtn.style.display   = '';
    formBtn.style.display   = 'none';
    if (formOnlyBtns) Array.from(formOnlyBtns.children).forEach(btn => {
      if (btn.id !== 'rtp-sim-json-mode-btn' && btn.id !== 'rtp-sim-form-mode-btn') {
        const prev = btn.dataset._jsHide;
        if (prev !== undefined) { btn.style.display = prev; delete btn.dataset._jsHide; }
      }
    });
  }
}

/* ── Symbol Config JSON mode ── */
let _rtpSymJsonMode = false;
let _rtpSymJsonCm = null;
// Round-trip-preserved metadata fields (not consumed by the sim engine)
let _symJsonMeta = {};  // symbolNames, paytableType, symbols.blank, symbols.locked

function _getRtpSymJsonCm() {
  if (_rtpSymJsonCm) return _rtpSymJsonCm;
  const el = document.getElementById('rtp-sym-json-editor');
  _rtpSymJsonCm = CodeMirror(el, {
    mode: { name: 'javascript', json: true },
    theme: 'rsg',
    lineNumbers: true,
    matchBrackets: true,
    autoCloseBrackets: true,
    styleActiveLine: true,
    styleSelectedText: true,
    indentUnit: 2,
    tabSize: 2,
    extraKeys: {
      'Cmd-S':  cm => cm.setCursor({ line: cm.getCursor().line, ch: 0 }),
      'Ctrl-S': cm => cm.setCursor({ line: cm.getCursor().line, ch: 0 }),
      'Cmd-E':  cm => { const l = cm.getCursor().line; cm.setCursor({ line: l, ch: cm.getLine(l).length }); },
      'Ctrl-E': cm => { const l = cm.getCursor().line; cm.setCursor({ line: l, ch: cm.getLine(l).length }); },
      'Cmd-X': cm => {
        if (cm.getSelection()) return CodeMirror.Pass;
        const line = cm.getCursor().line;
        const from = { line, ch: 0 };
        const to   = line < cm.lastLine() ? { line: line + 1, ch: 0 } : { line, ch: cm.getLine(line).length };
        cm.replaceRange('', from, to);
      },
      'Ctrl-X': cm => {
        if (cm.getSelection()) return CodeMirror.Pass;
        const line = cm.getCursor().line;
        const from = { line, ch: 0 };
        const to   = line < cm.lastLine() ? { line: line + 1, ch: 0 } : { line, ch: cm.getLine(line).length };
        cm.replaceRange('', from, to);
      },
      'Cmd-C': cm => {
        if (cm.getSelection()) return CodeMirror.Pass;
        navigator.clipboard.writeText(cm.getLine(cm.getCursor().line));
      },
      'Ctrl-C': cm => {
        if (cm.getSelection()) return CodeMirror.Pass;
        navigator.clipboard.writeText(cm.getLine(cm.getCursor().line));
      },
    },
  });
  return _rtpSymJsonCm;
}

function _buildSymConfigFromForm() {
  const normal = [], wild = [], scatter = [];
  const payTable = {}, wildMultipliers = {}, wildMultipliersAggregations = {};

  document.querySelectorAll('.rtp-sym-row').forEach(row => {
    const symId = parseInt(row.dataset.symId);
    const type  = row.querySelector('select').value;

    if (type === 'NORMAL')  normal.push(symId);
    else if (type === 'WILD')    wild.push(symId);
    else if (type === 'SCATTER') scatter.push(symId);

    if (type !== 'SCATTER') {
      const ptRaw = row.querySelector('.rtp-paytable-input').value.trim();
      if (ptRaw && !/^n\/a$/i.test(ptRaw)) {
        const vals = ptRaw.split(',').map(s => parseFloat(s.trim())).filter(v => !isNaN(v));
        if (vals.length > 0) {
          const fmt = v => Math.round(v * 10) / 10;
          const strat = document.getElementById('rtp-strategy')?.value || 'LTR';
          const isIntervalBased = strat === 'SCATTERS' || strat === 'CLUSTERS';
          if (isIntervalBased) {
            const mEl = document.getElementById('rtp-min-match');
            const minMatch = parseInt((mEl && (mEl.value || mEl.placeholder)) || 3);
            const entry = {};
            vals.forEach((v, i) => { entry[String(minMatch + i)] = fmt(v); });
            payTable[String(symId)] = entry;
          } else {
            payTable[String(symId)] = vals.map(fmt);
          }
        }
      }
    }

    if (type === 'WILD') {
      const aggSel = row.querySelector('.rtp-wild-agg');
      const agg = aggSel ? aggSel.value : 'NONE';
      if (agg === 'SEQUENCE') {
        const seqRaw = row.querySelector('.rtp-wild-seq').value.trim();
        if (seqRaw) {
          const seq = seqRaw.split(',').map(s => parseFloat(s.trim())).filter(v => !isNaN(v));
          wildMultipliers[String(symId)] = seq;
        } else {
          wildMultipliers[String(symId)] = 0.0;
        }
      } else {
        wildMultipliers[String(symId)] = agg === 'NONE' ? 0.0 : (parseFloat(row.querySelector('.rtp-wild-mult').value) || 1.0);
      }
      const aggExport = agg === 'ADD' ? 'ADDITIVE' : agg === 'MULTIPLY' ? 'MULTIPLICATIVE' : agg;
      wildMultipliersAggregations[String(symId)] = aggExport;
    }
  });

  const strat = document.getElementById('rtp-strategy')?.value || 'LTR';
  const paytableType = (strat === 'SCATTERS' || strat === 'CLUSTERS') ? 'INTERVAL_BASED' : 'STRICT';

  const meta = _symJsonMeta || {};
  const obj = {
    symbols: {
      normal,
      wild,
      scatter,
      blank:  meta.blank  || [],
      locked: meta.locked || [],
    },
  };
  if (meta.symbolNames && Object.keys(meta.symbolNames).length > 0) {
    obj.symbolNames = meta.symbolNames;
  }
  if (Object.keys(wildMultipliers).length > 0) {
    obj.wildMultipliers = wildMultipliers;
    obj.wildMultipliersAggregations = wildMultipliersAggregations;
  }
  obj.paytableType = paytableType;
  if (Object.keys(payTable).length > 0) obj.payTable = payTable;

  return obj;
}

function _prettySymConfig(obj) {
  // Emit compact arrays for symbol id lists; collapse payTable entry objects to one line
  let text = JSON.stringify(obj, null, 2);
  // Collapse numeric arrays (symbol id arrays) onto one line
  text = text.replace(/\[\s*\n(\s*\d+,?\s*\n)+\s*\]/g, m => {
    const items = m.slice(1, -1).trim().split(/\s*,?\s*\n\s*/).map(s => s.replace(/,+$/, '').trim()).filter(Boolean);
    return '[' + items.join(', ') + ']';
  });
  // Collapse payTable STRICT array entries ("1": [0.5, 2.0, ...]) onto one line with their key
  text = text.replace(/"(\d+)":\s*\[\s*\n([\s\S]*?)\]/g, (m, key, inner) => {
    const items = inner.trim().split(/\s*,?\s*\n\s*/).map(s => s.replace(/,+$/, '').trim()).filter(Boolean);
    return '"' + key + '": [' + items.join(', ') + ']';
  });
  // Collapse payTable INTERVAL_BASED entry objects { "3": 2.0, ... } onto one line
  text = text.replace(/\{[\s\n]*("\d+":\s*[\d.]+[\s\n,]*)+\}/g, m =>
    '{' + m.slice(1,-1).trim().replace(/\s*\n\s*/g, ' ').replace(/\s{2,}/g, ' ') + '}');
  // Format all payout numbers inside payTable entries to 1 decimal place (e.g. 5 → 5.0, 2.5 → 2.5)
  // Match only unquoted numbers (values, not JSON string keys)
  text = text.replace(/("payTable"\s*:\s*\{)([\s\S]*?)(\n\s*\})/g, (_, open, body, close) => {
    const formatted = body.replace(/(?<!")(\b\d+(?:\.\d+)?\b)(?!")/g, (n) => {
      const f = parseFloat(n);
      return isNaN(f) ? n : f.toFixed(1);
    });
    return open + formatted + close;
  });
  return text;
}

function toggleRtpSymJsonMode() {
  _rtpSymJsonMode = !_rtpSymJsonMode;
  const formBody  = document.getElementById('rtp-sym-form-body');
  const editorEl  = document.getElementById('rtp-sym-json-editor');
  const errEl     = document.getElementById('rtp-sym-json-error');
  const jsonBtn   = document.getElementById('rtp-sym-json-mode-btn');
  const formBtn   = document.getElementById('rtp-sym-form-mode-btn');
  const formOnlyBtns = document.getElementById('sym-config-header-btns');

  if (_rtpSymJsonMode) {
    const symbols = _buildSymConfigFromForm();
    const text = _prettySymConfig(symbols);
    formBody.style.display  = 'none';
    editorEl.style.display  = '';
    errEl.style.display     = 'none';
    jsonBtn.style.display   = 'none';
    formBtn.style.display   = '';
    _getRtpSymJsonCm().setValue(text);
    setTimeout(() => {
      _getRtpSymJsonCm().refresh();
      const scrollContainer = document.querySelector('.form-scroll');
      const card = document.getElementById('sym-config-card');
      if (scrollContainer && card) scrollContainer.scrollTop = card.offsetTop - 8;
    }, 50);
    if (formOnlyBtns) Array.from(formOnlyBtns.children).forEach(btn => {
      if (btn.id !== 'rtp-sym-json-mode-btn' && btn.id !== 'rtp-sym-form-mode-btn') {
        btn.dataset._jsHide = btn.style.display || '';
        btn.style.display = 'none';
      }
    });
  } else {
    const raw = _rtpSymJsonCm ? _rtpSymJsonCm.getValue().trim() : '';
    if (raw) {
      try {
        const parsed = JSON.parse(raw);

        if (Array.isArray(parsed)) {
          // Legacy flat-array format
          document.getElementById('rtp-symbol-rows').innerHTML = '';
          _symRowCounter = 0;
          parsed.forEach(sym => {
            const row = addSymbolRow(sym.symbolId);
            const typeSel = row.querySelector('select');
            typeSel.value = sym.type || 'NORMAL';
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
        } else if (parsed && typeof parsed === 'object' && parsed.symbols) {
          // New structured format: {symbols, symbolNames, wildMultipliers, payTable, paytableType}
          _symJsonMeta = {
            blank:  (parsed.symbols.blank  || []).slice(),
            locked: (parsed.symbols.locked || []).slice(),
            symbolNames:  parsed.symbolNames  || null,
            paytableType: parsed.paytableType || null,
          };
          // Delegate to _normalizeRtpPayload which already handles this exact shape
          const mEl = document.getElementById('rtp-min-match');
          const normalized = _normalizeRtpPayload({
            symbols: parsed.symbols,
            payTable: parsed.payTable || {},
            wildMultipliers: parsed.wildMultipliers || {},
            wildMultipliersAggregations: parsed.wildMultipliersAggregations || {},
            minMatch: parseInt((mEl && (mEl.value || mEl.placeholder)) || 3),
            screenWidth: null,  // let paytable key range determine maxCount, not screen width
          });
          document.getElementById('rtp-symbol-rows').innerHTML = '';
          _symRowCounter = 0;
          (normalized.symbols || []).forEach(sym => {
            const row = addSymbolRow(sym.symbolId);
            const typeSel = row.querySelector('select');
            typeSel.value = sym.type || 'NORMAL';
            onSymbolTypeChange(typeSel);
            if (sym.type !== 'SCATTER') {
              row.querySelector('.rtp-paytable-input').value = (sym.paytable || []).join(', ');
            }
            if (sym.type === 'WILD') {
              const aggSel = row.querySelector('.rtp-wild-agg');
              const aggMap = { 'NONE': 'NONE', 'ADD': 'ADD', 'ADDITIVE': 'ADD', 'MULTIPLY': 'MULTIPLY', 'MULTIPLICATIVE': 'MULTIPLY', 'SEQUENCE': 'SEQUENCE' };
              aggSel.value = aggMap[sym.wildAggregation] ?? 'NONE';
              onWildAggChange(aggSel);
              if (aggSel.value !== 'NONE' && aggSel.value !== 'SEQUENCE') {
                row.querySelector('.rtp-wild-mult').value = sym.wildMultiplier ?? 1;
              }
            }
          });
        } else {
          throw new Error('Expected a JSON array of symbol objects or a structured symbol config object');
        }
        updateSymConfigToggleBtn();
      } catch (e) {
        const { loc, hint } = _jsonParseError(raw, e);
        errEl.textContent = 'JSON parse error' + loc + hint + ': ' + e.message;
        errEl.style.display = '';
        _rtpSymJsonMode = true;
        return;
      }
    }
    formBody.style.display  = '';
    editorEl.style.display  = 'none';
    errEl.style.display     = 'none';
    jsonBtn.style.display   = '';
    formBtn.style.display   = 'none';
    if (formOnlyBtns) Array.from(formOnlyBtns.children).forEach(btn => {
      if (btn.id !== 'rtp-sym-json-mode-btn' && btn.id !== 'rtp-sym-form-mode-btn') {
        const prev = btn.dataset._jsHide;
        if (prev !== undefined) { btn.style.display = prev; delete btn.dataset._jsHide; }
      }
    });
  }
}

// ── GDK config normalizer ────────────────────────────────────────────────────
// Converts a GDK-style BaseSlotConfig JSON into a Slots Lab restoreRtpForm payload.
// Also passes through payloads that are already in Slots Lab format unchanged.
function _normalizeRtpPayload(p) {
  const out = Object.assign({}, p);

  // screen: {cols, rows} → screenWidth / screenHeight
  if (p.screen && typeof p.screen === 'object' && !Array.isArray(p.screen)) {
    if (out.screenWidth  == null) out.screenWidth  = p.screen.cols ?? p.screen.width  ?? null;
    if (out.screenHeight == null) out.screenHeight = p.screen.rows ?? p.screen.height ?? null;
  }

  // strategy: GDK uses e.g. "CLUSTERS_PAY" — map to Slots Lab names
  if (out.strategy) {
    const stratMap = {
      'LINES_PAY': 'LTR', 'LINES_PAY_LTR': 'LTR', 'LINES_PAY_RTL': 'RTL',
      'LINES_PAY_BW': 'BW', 'LINES_PAY_SL': 'SL', 'LINES_PAY_ADJ': 'ADJ',
      'WAYS_PAY': 'WAYS', 'MEGAWAYS_PAY': 'MEGAWAYS',
      'SCATTERS_PAY': 'SCATTERS', 'CLUSTERS_PAY': 'CLUSTERS',
    };
    out.strategy = stratMap[out.strategy] ?? out.strategy;
  }

  // symbols: GDK uses {"normal": [1,2,3], "wild": [10], "scatter": [14]}
  // + separate payTable, wildMultipliers, wildMultipliersAggregations maps
  if (p.symbols && !Array.isArray(p.symbols) && typeof p.symbols === 'object') {
    const normalIds  = (p.symbols.normal  || []).map(Number);
    const wildIds    = (p.symbols.wild    || []).map(Number);
    const scatterIds = (p.symbols.scatter || []).map(Number);

    const minMatch = parseInt(out.minMatch) || 3;
    const screenWidth = parseInt(out.screenWidth) || null;

    // payTable can be STRICT: {symId: [m1, m2, ...]}  or  INTERVAL_BASED: {symId: {count: mult}}
    const rawPayTable = p.payTable || p.paytable || {};

    function resolvePaytable(symId, type) {
      const entry = rawPayTable[symId] ?? rawPayTable[String(symId)];
      if (!entry) return [];
      if (Array.isArray(entry)) return entry.map(Number);  // STRICT: already a flat array
      if (typeof entry === 'object') {
        // INTERVAL_BASED: {count: mult} — convert to dense array from minMatch
        const maxCount = screenWidth || Math.max(...Object.keys(entry).map(Number));
        const arr = [];
        for (let c = minMatch; c <= maxCount; c++) {
          const v = entry[c] ?? entry[String(c)] ?? null;
          if (v !== null) arr.push(Number(v));
          else if (arr.length > 0) arr.push(0);
        }
        // Trim trailing zeros
        while (arr.length && arr[arr.length - 1] === 0) arr.pop();
        return arr;
      }
      return [];
    }

    const syms = [];
    const allIds = [...new Set([...normalIds, ...wildIds, ...scatterIds])].sort((a, b) => a - b);
    for (const id of allIds) {
      let type = 'NORMAL';
      if (wildIds.includes(id)) type = 'WILD';
      else if (scatterIds.includes(id)) type = 'SCATTER';
      const sym = { symbolId: id, type, paytable: resolvePaytable(id, type) };
      if (type === 'WILD') {
        const wm = p.wildMultipliers;
        const wa = p.wildMultipliersAggregations;
        sym.wildMultiplier  = (wm && (wm[id] ?? wm[String(id)])) ?? 1;
        const aggRaw = wa && (wa[id] ?? wa[String(id)]);
        const aggMap = { 'NONE': 'NONE', 'ADD': 'ADD', 'ADDITIVE': 'ADD', 'MULTIPLY': 'MULTIPLY', 'MULTIPLICATIVE': 'MULTIPLY', 'SEQUENCE': 'SEQUENCE' };
        sym.wildAggregation = aggRaw ? (aggMap[aggRaw] ?? 'NONE') : 'NONE';
      }
      syms.push(sym);
    }
    out.symbols = syms;
  } else if (Array.isArray(p.symbols)) {
    // Slots Lab native format — normalise paytable entries if any are interval-based objects
    const minMatch = parseInt(out.minMatch) || 3;
    const screenWidth = parseInt(out.screenWidth) || null;
    out.symbols = p.symbols.map(sym => {
      if (sym.paytable && typeof sym.paytable === 'object' && !Array.isArray(sym.paytable)) {
        const entry = sym.paytable;
        const maxCount = screenWidth || Math.max(...Object.keys(entry).map(Number));
        const arr = [];
        for (let c = minMatch; c <= maxCount; c++) {
          const v = entry[c] ?? entry[String(c)] ?? null;
          if (v !== null) arr.push(Number(v));
          else if (arr.length > 0) arr.push(0);
        }
        while (arr.length && arr[arr.length - 1] === 0) arr.pop();
        return Object.assign({}, sym, { paytable: arr });
      }
      return sym;
    });
  }

  return out;
}
