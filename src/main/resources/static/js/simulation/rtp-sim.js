/* ── RTP Simulation ── */

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

function collectRtpRequest() {
  const errors = [];

  if (!_latestReelSets || _latestReelSets.length === 0) {
    tryLoadReelsFromEditor();
  }

  if (!_latestReelSets || _latestReelSets.length === 0)
    return { errors: ['No generated reels found. Run Generate first.'] };

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

  const screenWidth  = parseInt(document.getElementById('rtp-screen-width').value);
  const screenHeight = parseInt(document.getElementById('rtp-screen-height').value);
  const minMatch     = parseInt(document.getElementById('rtp-min-match').value);
  if (isNaN(screenWidth)  || screenWidth  < 1) errors.push('Screen width must be >= 1');
  if (isNaN(screenHeight) || screenHeight < 1) errors.push('Screen height must be >= 1');
  if (isNaN(minMatch) || minMatch < 1) errors.push('Min Match must be >= 1');
  if (!isNaN(minMatch) && !isNaN(screenWidth) && minMatch > screenWidth)
    errors.push('Min Match (' + minMatch + ') cannot exceed screen width (' + screenWidth + ')');

  const lineRows = document.querySelectorAll('.rtp-line-row');
  const _strategyForLineCheck = document.getElementById('rtp-strategy').value;
  const isWays = _strategyForLineCheck === 'WAYS' || _strategyForLineCheck === 'SCATTERS' || _strategyForLineCheck === 'CLUSTERS';
  if (!isWays && lineRows.length === 0) errors.push('At least one line definition is required');
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

  parsedLines.forEach((nums, li) => {
    if (nums === null) return;
    if (!isNaN(screenWidth) && nums.length !== screenWidth)
      errors.push('Line ' + (li+1) + ': must have exactly ' + screenWidth + ' positions');
    if (!isNaN(screenHeight) && nums.some(n => n < 0 || n >= screenHeight))
      errors.push('Line ' + (li+1) + ': positions must be 0–' + (screenHeight-1));
  });

  const seenLines = new Set();
  parsedLines.forEach((nums, li) => {
    if (nums === null) return;
    const key = JSON.stringify(nums);
    if (seenLines.has(key)) errors.push('Line ' + (li+1) + ': duplicate payline');
    else seenLines.add(key);
    lineDefinitions.push(nums);
  });

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
        errors.push('Symbol ' + symId + ': paytable must have exactly ' + requiredDesc + ' value(s)');
    }
    if (type === 'WILD' && wildAgg !== 'SEQUENCE' && wildAgg !== 'NONE' && (isNaN(wildMult) || wildMult <= 0))
      errors.push('Symbol ' + symId + ': wild multiplier must be > 0');
    if (wildAgg === 'SEQUENCE') {
      if (wildSequence.some(isNaN))
        errors.push('Symbol ' + symId + ': sequence contains invalid numbers');
      if (wildSequence.length !== screenWidth)
        errors.push('Symbol ' + symId + ': sequence must have exactly ' + screenWidth + ' value(s) (one per reel)');
    }
    symbols.push({ symbolId: symId, type, paytable, wildMultiplier: wildMult, wildAggregation: wildAgg, wildSequence, contactsIntervalSetName });
  });

  if (symbols.length === 0) errors.push('At least one symbol must be configured');

  if (!symbols.some(s => s.type === 'NORMAL' && s.paytable.length > 0))
    errors.push('At least one NORMAL symbol with a paytable is required');

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
      errors.push('At least one adjacency offset is required for CLUSTERS strategy');
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
      adjacencyOffsets
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
              LTR:      'Left to Right — all symbols pay on adjacent reels starting from the leftmost reel.',
              RTL:      'Right to Left — all symbols pay on adjacent reels starting from the rightmost reel.',
              BW:       'Both Ways — all symbols pay on adjacent reels starting from either the leftmost or the rightmost reel. Both directions are evaluated and the total of both is awarded.',
              ADJ:      'Adjacent — symbols pay on consecutive adjacent reels starting from any valid reel, not only the leftmost.',
              WAYS:     'All Ways — symbols pay on any row combination across consecutive reels. No paylines needed.',
              SCATTERS: 'Scatters Pay — symbols pay based on total tile count anywhere on the screen. No paylines needed.',
              CLUSTERS: 'Clusters Pay — symbols pay based on the size of connected clusters. Two tiles connect if adjacent according to configured offsets.'
            }[payload.strategy] || payload.strategy}<div class="tip-rule">Symbols must land on a defined payline (line definition) to count as a win.</div></span></span></span>
            <span class="rtp-stat-value">${{'LTR':'Left to Right','RTL':'Right to Left','BW':'Both Ways','ADJ':'Adjacent','WAYS':'All Ways','SCATTERS':'Scatters Pay','CLUSTERS':'Clusters Pay'}[payload.strategy] || payload.strategy}</span>
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
            <td>${c.matchLabel ?? c.matchCount}</td>
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
