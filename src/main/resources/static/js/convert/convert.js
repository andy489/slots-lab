/* ── Convert Tab ── */

function onConvFormatChange() {
  const isCsv = document.getElementById('conv-format').value === 'CSV';
  document.getElementById('conv-gameid-group').style.display = isCsv ? '' : 'none';
  document.getElementById('conv-gameid-sep').style.display   = isCsv ? '' : 'none';
}

function detectInputFormat(text) {
  const first = text.trimStart();
  return first.startsWith('"') ? 'csv' : 'json';
}

function parseCsvInput(text) {
  const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
  if (lines.length === 0) return { ok: false, error: 'CSV input is empty' };

  const sets = new Map();

  for (let i = 0; i < lines.length; i++) {
    const row = lines[i];
    const parts = row.split('","');
    if (parts.length !== 4) return { ok: false, error: `CSV line ${i+1}: expected 4 columns, got ${parts.length}` };
    const setName  = parts[0].replace(/^"/, '');
    const reelIdx  = parseInt(parts[1], 10);
    const pos      = parseInt(parts[2], 10);
    const tileStr  = parts[3].replace(/"$/, '');

    if (isNaN(reelIdx) || isNaN(pos)) return { ok: false, error: `CSV line ${i+1}: reelIndex/position must be integers` };

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
