/* ── Import / Export Tab ── */

let _lastExportedPayload = null;

function _hasOnlyPlaceholders() {
  const inputs = document.querySelectorAll('#reel-set-list .reel-set-card .reel-rows .reel-row input');
  if (inputs.length === 0) return true;
  const allPlaceholder = Array.from(inputs).every(inp => inp.value.trim() === '');
  if (!allPlaceholder) return false;
  // Allow export when Generate has already produced output even if inputs still show placeholders
  return genOutput.getValue().trim() === '';
}

function _paytableToIntervals(paytable, minMatch) {
  if (!paytable || paytable.length === 0) return {};
  const obj = {};
  paytable.forEach((v, i) => { obj[String(minMatch + i)] = v; });
  return obj;
}

function _intervalsToPaytable(intervals, minMatch) {
  if (!intervals || typeof intervals !== 'object' || Array.isArray(intervals)) return intervals || [];
  return Object.keys(intervals)
    .map(Number)
    .sort((a, b) => a - b)
    .map(k => intervals[String(k)]);
}

function _flattenReelSetChances(reelSetChances) {
  if (!reelSetChances || reelSetChances.length === 0) return {};
  // Already flat arrays (old export backwards compat): no-op
  if (!reelSetChances[0] || typeof reelSetChances[0] !== 'object') return {};
  return {
    reelSetIndexes: reelSetChances.map(c => c.setIndex),
    reelSetChances: reelSetChances.map(c => c.chance)
  };
}

function _expandReelSetChances(cfg) {
  // Convert flat arrays back to [{setIndex, chance}] for restoreRtpForm
  if (!cfg) return cfg;
  if (Array.isArray(cfg.reelSetIndexes) && Array.isArray(cfg.reelSetChances)) {
    const out = { ...cfg };
    out.reelSetChances = cfg.reelSetIndexes.map((idx, i) => ({ setIndex: idx, chance: cfg.reelSetChances[i] }));
    delete out.reelSetIndexes;
    return out;
  }
  return cfg;
}

function _exportSymbols(symList, minMatch) {
  const groups = { normal: [], wild: [], scatter: [], blank: [] };
  const payTable = {};
  const wildMultipliers = {};
  const wildMultipliersAggregations = {};

  (symList || []).forEach(sym => {
    const id = sym.symbolId;
    const idStr = String(id);
    const t = sym.type;
    if (t === 'NORMAL')        groups.normal.push(id);
    else if (t === 'WILD')     groups.wild.push(id);
    else if (t === 'SCATTER')  groups.scatter.push(id);
    else                       groups.blank.push(id);

    if (sym.paytable && sym.paytable.length > 0) {
      payTable[idStr] = _paytableToIntervals(sym.paytable, minMatch);
    }
    if (t === 'WILD') {
      wildMultipliers[idStr] = sym.wildMultiplier ?? 1.0;
      wildMultipliersAggregations[idStr] = sym.wildAggregation || 'NONE';
    }
  });

  return { groups, payTable, wildMultipliers, wildMultipliersAggregations };
}

function _importSymbols(cfg) {
  // Convert cherrybreeze-style symbols back to [{symbolId, type, paytable, ...}] array
  if (!cfg) return cfg;
  if (!cfg.symbols || Array.isArray(cfg.symbols)) return cfg; // already array format
  const out = { ...cfg };
  const symGroups = cfg.symbols;
  const payTable = cfg.payTable || {};
  const wildMult = cfg.wildMultipliers || {};
  const wildAgg  = cfg.wildMultipliersAggregations || {};
  const minMatch = cfg.minMatch || 1;

  const syms = [];
  const addGroup = (ids, type) => (ids || []).forEach(id => {
    const idStr = String(id);
    const intervals = payTable[idStr];
    syms.push({
      symbolId: id,
      type,
      paytable: intervals ? _intervalsToPaytable(intervals, minMatch) : [],
      wildMultiplier: parseFloat(wildMult[idStr]) || 1.0,
      wildAggregation: wildAgg[idStr] || (type === 'WILD' ? 'NONE' : 'ADD'),
      wildSequence: []
    });
  });
  addGroup(symGroups.normal,  'NORMAL');
  addGroup(symGroups.wild,    'WILD');
  addGroup(symGroups.scatter, 'SCATTER');
  addGroup(symGroups.blank,   'NORMAL');

  out.symbols = syms;
  delete out.payTable;
  delete out.wildMultipliers;
  delete out.wildMultipliersAggregations;
  return out;
}

function _exportSimConfig(payload) {
  if (!payload) return null;
  const minMatch = payload.minMatch || 1;
  const { reelSets, reelSetChances, symbols, ...rest } = payload;
  const { groups, payTable, wildMultipliers, wildMultipliersAggregations } = _exportSymbols(symbols, minMatch);
  return {
    ...rest,
    ..._flattenReelSetChances(reelSetChances),
    symbols: groups,
    payTable,
    wildMultipliers,
    wildMultipliersAggregations
  };
}

function _exportSpinConfig(payload) {
  if (!payload) return null;
  const { reelSetIndex, stops, screen } = payload;
  const cfg = { spinCount: payload.count || 1 };
  if (reelSetIndex != null) cfg.reelSetIndex = reelSetIndex;
  if (stops != null)        cfg.stops = stops;
  if (screen != null)       cfg.screen = screen;
  return cfg;
}

function _stripNullsAndKeys(obj, dropKeys) {
  if (Array.isArray(obj)) return obj.map(v => _stripNullsAndKeys(v, dropKeys));
  if (obj !== null && typeof obj === 'object') {
    const out = {};
    for (const [k, v] of Object.entries(obj)) {
      if (dropKeys && dropKeys.has(k)) continue;
      if (v === null || v === undefined) continue;
      if (Array.isArray(v) && v.length === 0) { out[k] = v; continue; }
      out[k] = _stripNullsAndKeys(v, dropKeys);
    }
    return out;
  }
  return obj;
}

const _EXPORT_DROP_KEYS = new Set([
  'mapName', 'gameId', 'output', 'resultFilePath',
  'count'
]);

function _applyFormatRules(s) {
  // Collapse numeric arrays onto one line
  s = s.replace(/\[[\d\s,.+-]+\]/g, m => '[' + m.slice(1,-1).trim().replace(/\s*,\s*/g,', ').replace(/\s+/g,' ') + ']');
  // Collapse payTable inner objects onto one line: {"3": 2.0, "4": 4.0, ...}
  s = s.replace(/\{\s*("[\d]+":\s*[\d.]+,?\s*)+\}/g, m => '{' + m.slice(1,-1).trim().replace(/\s*\n\s*/g, ' ').replace(/\s{2,}/g, ' ') + '}');
  // Collapse restriction objects onto one line: {"stackSizes":…,"stackChances":…,"minDistance":…}
  s = s.replace(/\{\s*"stackSizes":[^}]+\}/g, m => m.replace(/\s*\n\s*/g, ' ').replace(/\s{2,}/g, ' '));
  // Collapse tilesCounts arrays: one reel per line
  s = s.replace(/"tilesCounts":\s*\[([\s\S]*?)\n(\s*)\]/g, (_, inner, closingIndent) => {
    const rows = inner.match(/\[[^\]]*\]/g);
    if (!rows) return '"tilesCounts": [' + inner + '\n' + closingIndent + ']';
    const indent = closingIndent + '  ';
    return '"tilesCounts": [\n' + rows.map(r => indent + r).join(',\n') + '\n' + closingIndent + ']';
  });
  // Collapse reelSet arrays: one reel strip per line
  s = s.replace(/"reelSet":\s*\[([\s\S]*?)\n(\s*)\]/g, (_, inner, closingIndent) => {
    const rows = inner.match(/\[[^\]]*\]/g);
    if (!rows) return '"reelSet": [' + inner + '\n' + closingIndent + ']';
    const indent = closingIndent + '  ';
    return '"reelSet": [\n' + rows.map(r => indent + r).join(',\n') + '\n' + closingIndent + ']';
  });
  // Group lineDefinitions inner arrays 4 per line (inner arrays are single-line after numeric collapse above)
  s = s.replace(/"lineDefinitions":\s*\[([\s\S]*?)\n(\s*)\]/g, (_, inner, closingIndent) => {
    const rows = inner.match(/\[[^\]]*\]/g);
    if (!rows) return '"lineDefinitions": [' + inner + '\n' + closingIndent + ']';
    const indent = closingIndent + '  ';
    const lines = [];
    for (let i = 0; i < rows.length; i += 4) {
      lines.push(indent + rows.slice(i, i + 4).join(', '));
    }
    return '"lineDefinitions": [\n' + lines.join(',\n') + '\n' + closingIndent + ']';
  });
  return s;
}

function _formatExport(obj) {
  const stripped = _stripNullsAndKeys(obj, _EXPORT_DROP_KEYS);
  return _applyFormatRules(JSON.stringify(stripped, null, 2));
}

function _formatImportPreview(obj) {
  return _applyFormatRules(JSON.stringify(obj, null, 2));
}

function buildExportPayload() {
  const genCfg = buildConfig();

  const genResultRaw = genOutput.getValue().trim();
  let genResult = null;
  if (genResultRaw) {
    try { genResult = JSON.parse(genResultRaw); } catch (e) { genResult = genResultRaw; }
  }

  const rtpCollected = collectRtpRequest();
  const rtpPayload = rtpCollected.errors && rtpCollected.errors.length > 0 ? null : rtpCollected.payload;

  const stCollected = buildSpinTestPayload();
  const stPayload = stCollected.errors && stCollected.errors.length > 0 ? null : stCollected.payload;

  const simCfg = _exportSimConfig(rtpPayload);
  const stCfg  = _exportSpinConfig(stPayload);

  const out = {
    exportedAt: new Date().toLocaleString('sv').replace('T', ' '),
    generate: { config: genCfg, result: genResult }
  };
  if (simCfg) out.simulation = { config: simCfg };
  if (stCfg)  out.spinTest   = { config: stCfg };
  return out;
}

function _showExportPreview(formatted) {
  document.getElementById('io-export-hint').style.display = 'none';
  const pre = document.getElementById('io-export-preview');
  pre.style.display = '';
  pre.value = formatted;
  const copyBtn = document.getElementById('io-export-copy');
  if (copyBtn) copyBtn.style.display = '';
}

function previewExport() {
  if (_hasOnlyPlaceholders()) {
    showToast('Nothing to preview — fill in at least one reel first.', true);
    return;
  }
  const payload = buildExportPayload();
  _lastExportedPayload = payload;
  _showExportPreview(_formatExport(payload));
}

function runExport() {
  const pre = document.getElementById('io-export-preview');
  const existing = pre.value.trim();

  let formatted;
  if (existing) {
    formatted = existing;
  } else {
    if (_hasOnlyPlaceholders()) {
      showToast('Nothing to export — fill in at least one reel before exporting.', true);
      return;
    }
    const payload = buildExportPayload();
    _lastExportedPayload = payload;
    formatted = _formatExport(payload);
    _showExportPreview(formatted);
  }

  const blob = new Blob([formatted], { type: 'application/json' });
  const url  = URL.createObjectURL(blob);
  const ts   = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  const a    = document.createElement('a');
  a.href     = url;
  a.download = 'slot-lab-export-' + ts + '.json';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);

  showToast('Exported successfully');
}

function triggerImport() {
  document.getElementById('io-file-input').value = '';
  document.getElementById('io-file-input').click();
}

function handleImportFile(event) {
  const file = event.target.files[0];
  if (!file) return;
  _readAndApply(file);
}

function handleImportDrop(event) {
  event.preventDefault();
  const file = event.dataTransfer.files[0];
  if (!file) return;
  _readAndApply(file);
}

function _readAndApply(file) {
  const reader = new FileReader();
  reader.onload = e => {
    let data;
    try {
      data = JSON.parse(e.target.result);
    } catch (err) {
      showToast('Invalid JSON: ' + err.message, true);
      return;
    }
    applyImport(data);
  };
  reader.readAsText(file);
}

function _normaliseImportConfig(cfg, reelSets) {
  if (!cfg) return cfg;
  let out = _expandReelSetChances({ ...cfg });
  out = _importSymbols(out);
  if (reelSets && !out.reelSets) out.reelSets = reelSets;
  return out;
}

function applyImport(data) {
  try {
    if (data.generate?.config) {
      restoreGenerateForm(data.generate.config);
    }

    if (data.generate?.result) {
      const resultStr = typeof data.generate.result === 'string'
        ? data.generate.result
        : _applyFormatRules(JSON.stringify(data.generate.result, null, 2));
      if (resultStr.trim() !== '') {
        genOutput.setValue(resultStr);
        storeGeneratedReels(resultStr);
      }
    }

    const reelSets = Array.isArray(data.generate?.result) ? data.generate.result
                   : Array.isArray(data.reelSets) ? data.reelSets  // backwards compat
                   : null;
    if (data.simulation?.config) {
      restoreRtpForm(_normaliseImportConfig(data.simulation.config, reelSets));
    }

    if (data.spinTest?.config) {
      const stc = data.spinTest.config;
      const riEl = document.getElementById('spin-test-reel-idx');
      if (riEl && stc.reelSetIndex != null) riEl.value = stc.reelSetIndex;
      const stopsEl = document.getElementById('spin-test-stops');
      if (stopsEl && stc.stops) stopsEl.value = stc.stops.join(', ');
      const screenEl = document.getElementById('spin-test-screen');
      if (screenEl && stc.screen) screenEl.value = JSON.stringify(stc.screen);
    }

    updateSpinTestPlaceholders();

    document.getElementById('io-import-hint').style.display = 'none';
    const pre = document.getElementById('io-import-preview');
    pre.style.display = '';
    pre.value = _formatImportPreview(data);
    const copyBtn = document.getElementById('io-import-copy');
    if (copyBtn) copyBtn.style.display = '';

    showToast('Imported successfully');
  } catch (err) {
    showToast('Import failed: ' + err.message, true);
  }
}
