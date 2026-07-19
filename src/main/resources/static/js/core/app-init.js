/* ── Application bootstrap ── */

/* Insert language toggle into header */
(function() {
  const slot = document.getElementById('lang-toggle-slot');
  if (slot) slot.appendChild(_buildLangBtn());
  applyI18n();
})();

/* Init default reel sets */
addReelSet();
addReelSet();
loadHistory();
loadRtpHistory();

document.getElementById('rtp-symbol-rows').addEventListener('input', updateSymConfigToggleBtn);
document.getElementById('interval-sets-container').addEventListener('input', updateScatterDefsToggleBtn);
document.getElementById('rtp-lines-list').addEventListener('input', updateLineDefsToggleBtn);
['rtp-screen-width', 'rtp-screen-height', 'rtp-min-match'].forEach(id => {
  document.getElementById(id).addEventListener('input', () => {
    updateLineDefsToggleBtn();
    updateScatterDefsToggleBtn();
    updateSpinTestPlaceholders();
  });
});
onStrategyChange();
updateSpinTestPlaceholders();

/* Pre-populate default symbols 1–7 */
[1, 2, 3, 4, 5, 6, 7].forEach(id => addSymbolRow(id));
updateSymConfigToggleBtn();

/* Convert tab defaults */
const r1 = [1,2,3,1,2,2,2,4], r2 = [2,3,1,2,3], r3 = [3,1,2,3,1];
convInput.setValue(prettyCompact([{ setName: 'ReelSet#0', reelSet: [r1, r2, r3] }]));
setTimeout(() => { genOutput.refresh(); convInput.refresh(); convOutput.refresh(); }, 0);

/* Escape key closes modals */
document.addEventListener('keydown', e => { if (e.key === 'Escape') { closeInfo(); closeImportCounts(); } });

/* Init default payline placeholder rows without filling screen inputs */
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
