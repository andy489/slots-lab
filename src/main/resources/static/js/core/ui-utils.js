/* ── Shared UI utilities ── */

let _activeTab = 'generate';

function switchTab(name, btn) {
  _activeTab = name;
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('tab-' + name).classList.add('active');
  btn.classList.add('active');
  btn.scrollIntoView({ block: 'nearest', inline: 'nearest' });
  _updateTabbarEdges();
  setTimeout(() => {
    if (name === 'generate') genOutput.refresh();
    else if (name === 'convert') { convInput.refresh(); convOutput.refresh(); }
    else if (name === 'rtp') tryLoadReelsFromEditor();
    else if (name === 'spin-test') { tryLoadReelsFromEditor(); updateSpinTestPlaceholders(); }
    else if (name === 'io') { /* no CodeMirror editors to refresh */ }
  }, 0);
}

/* ── Tabbar scroll-edge fade indicators ── */
function _updateTabbarEdges() {
  const bar  = document.getElementById('tabbar');
  const wrap = document.getElementById('tabbar-wrap');
  if (!bar || !wrap) return;
  wrap.classList.toggle('can-scroll-left',  bar.scrollLeft > 4);
  wrap.classList.toggle('can-scroll-right', bar.scrollLeft + bar.clientWidth < bar.scrollWidth - 4);
}
document.addEventListener('DOMContentLoaded', () => {
  const bar = document.getElementById('tabbar');
  if (!bar) return;
  bar.addEventListener('scroll', _updateTabbarEdges, { passive: true });
  new ResizeObserver(_updateTabbarEdges).observe(bar);
  requestAnimationFrame(_updateTabbarEdges);
});
window.addEventListener('load', () => requestAnimationFrame(_updateTabbarEdges));

function tryLoadReelsFromEditor() {
  if (_latestReelSets && _latestReelSets.length > 0) return;
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

/* ── Frontend validation helpers ── */
function clearErrors() {
  document.querySelectorAll('.field-error').forEach(el => el.classList.remove('field-error'));
}

function markError(el, msg) {
  el.classList.add('field-error');
  return msg;
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
function openInfo() {
  const titles = { generate: t('info.title_generate'), convert: t('info.title_convert'), io: t('info.title_io') };
  document.getElementById('info-modal-title').textContent = titles[_activeTab] || t('info.title_default');
  document.querySelectorAll('#info-modal .info-section[data-tab]').forEach(s => {
    s.style.display = s.dataset.tab === _activeTab ? '' : 'none';
  });
  document.getElementById('info-modal').classList.add('open');
}
function closeInfo() { document.getElementById('info-modal').classList.remove('open'); }

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

/* ── JSON formatting helpers ── */
function prettyCompact(obj) {
  return JSON.stringify(obj, null, 2).replace(
    /\[[\d\s,.+-]+\]/g,
    m => '[' + m.slice(1,-1).trim().replace(/\s*,\s*/g,', ').replace(/\s+/g,' ') + ']'
  );
}

function compactJson(obj) {
  const raw = JSON.stringify(obj, null, 2);
  return raw.replace(/\[\s*([\d,\s-]+?)\s*\]/g, m => '[' + m.replace(/\s+/g, '').slice(1,-1).split(',').join(', ') + ']');
}

function escapeHtml(s) {
  return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}
