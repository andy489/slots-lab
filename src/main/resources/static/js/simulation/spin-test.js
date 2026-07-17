/* ── Spin Test Tab ── */

function buildSpinTestPayload() {
  const errors = [];

  const screenRawEarly = document.getElementById('spin-test-screen').value.trim();
  const hasFixedScreen = screenRawEarly !== '';

  if (!hasFixedScreen) {
    if (!_latestReelSets || _latestReelSets.length === 0) {
      tryLoadReelsFromEditor();
    }
    if (!_latestReelSets || _latestReelSets.length === 0)
      return { errors: ['No generated reels found. Run Generate first, or provide a Fixed Screen Override.'] };
  }

  const chances = [];
  let chanceSum = 0;
  if (!hasFixedScreen) {
    _latestReelSets.forEach((_, i) => {
      const el = document.getElementById('rtp-chance-' + i);
      const v  = parseFloat(el?.value);
      if (isNaN(v) || v < 0) { errors.push('Reel set ' + i + ': chance must be >= 0'); return; }
      chances.push({ setIndex: i, chance: v });
      chanceSum += v;
    });
    if (Math.abs(chanceSum - 100) > 0.05)
      errors.push('Reel set chances must sum to 100.0% (current: ' + chanceSum.toFixed(1) + '%). Configure in the Simulation tab.');
  }

  const _wEl = document.getElementById('rtp-screen-width');
  const _hEl = document.getElementById('rtp-screen-height');
  const _mEl = document.getElementById('rtp-min-match');
  const screenWidth  = parseInt((_wEl?.value || _wEl?.placeholder) || '');
  const screenHeight = parseInt((_hEl?.value || _hEl?.placeholder) || '');
  const minMatch     = parseInt((_mEl?.value || _mEl?.placeholder) || '');
  if (isNaN(screenWidth)  || screenWidth  < 1) errors.push('Screen width not set — configure in Simulation tab');
  if (isNaN(screenHeight) || screenHeight < 1) errors.push('Screen height not set — configure in Simulation tab');
  if (isNaN(minMatch) || minMatch < 1)         errors.push('Min Match not set — configure in Simulation tab');

  const lineDefinitions = [];
  document.querySelectorAll('.rtp-line-row').forEach((row) => {
    const nums = row.querySelector('.rtp-line-input').value.trim().split(',').map(s => parseInt(s.trim(), 10));
    if (!nums.some(isNaN)) lineDefinitions.push(nums);
  });
  const _spinTestStrat = document.getElementById('rtp-strategy')?.value;
  if (lineDefinitions.length === 0 && _spinTestStrat !== 'WAYS' && _spinTestStrat !== 'MEGAWAYS' && _spinTestStrat !== 'SCATTERS' && _spinTestStrat !== 'CLUSTERS')
    errors.push('No line definitions — configure in Simulation tab');

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
    symbols.push({ symbolId: symId, type, paytable, wildMultiplier: wildMult, wildAggregation: wildAgg, wildSequence,
      contactsIntervalSetName: (row.querySelector('.rtp-interval-set-sel')?.value || null) });
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
  if (hasFixedScreen) {
    try {
      screen = JSON.parse(screenRawEarly);
    } catch(e) {
      return { errors: ['Fixed screen: invalid JSON — ' + e.message] };
    }
  }

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

  const megawaysReelHeightChances = strategy === 'MEGAWAYS' ? collectMegawaysHeightChances() : null;

  return {
    errors: [],
    payload: {
      reelSets: hasFixedScreen ? [] : _latestReelSets,
      reelSetChances: hasFixedScreen ? [] : chances,
      symbols,
      strategy,
      screenWidth,
      screenHeight,
      minMatch,
      lineDefinitions,
      count,
      reelSetIndex,
      stops,
      screen,
      contactsIntervalSets,
      adjacencyOffsets,
      megawaysReelHeightChances
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
        const isMask    = sym === 0;
        const cls = isMask ? ' mask' : isWild ? ' wild' : isScatter ? ' scatter' : '';
        return `<div class="spin-screen-cell${cls}">${isMask ? '' : sym}</div>`;
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

  const n = spins.length;
  let current = 0;
  let highlightSpin = null;

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
  const spinJsons = dtos.map(d => compactJson(d));
  const lineRanges = [];
  let lineOffset = 1;
  for (let i = 0; i < spinJsons.length; i++) {
    const indented = spinJsons[i].split('\n').map(l => '  ' + l);
    const from = lineOffset;
    const to   = lineOffset + indented.length - 1;
    lineRanges.push({ from, to });
    lineOffset = to + 1;
  }

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
  setTimeout(() => { cm.refresh(); }, 0);

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
