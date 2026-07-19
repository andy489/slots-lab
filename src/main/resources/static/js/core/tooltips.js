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
      if (br.top - tipH - GAP >= 8) {
        tip.style.top = (br.top - tipH - GAP) + 'px';
      } else {
        tip.style.top = (br.bottom + GAP) + 'px';
      }
    }
  }

  function positionTip(wrap) {
    const tip = wrap.querySelector('.stat-tip-box');
    if (!tip) return;
    const badge = wrap.querySelector('.stat-info') || wrap;

    tip.style.position = 'fixed';
    tip.style.zIndex = '9999';
    tip.style.left = '-9999px';
    tip.style.top = '0';
    tip.style.bottom = '';
    tip.style.transform = '';

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

  // ── Generic panel resize factory ────────────────────────────────────────────
  function makeResizable(handleId, layoutSelector, cssVar, defaultWidth, storageKey, minWidth, maxWidth) {
    const handle = document.getElementById(handleId);
    if (!handle) return;
    const layout = handle.closest(layoutSelector);
    if (!layout) return;
    const saved = parseInt(localStorage.getItem(storageKey), 10);
    if (saved && saved >= minWidth && saved <= maxWidth) {
      layout.style.setProperty(cssVar, saved + 'px');
    }
    let startX = 0, startWidth = 0;
    handle.addEventListener('mousedown', function (e) {
      e.preventDefault();
      startX = e.clientX;
      startWidth = parseInt(getComputedStyle(layout).getPropertyValue(cssVar)) || defaultWidth;
      handle.classList.add('dragging');
      function onMove(ev) {
        const newWidth = Math.min(maxWidth, Math.max(minWidth, startWidth + (ev.clientX - startX)));
        layout.style.setProperty(cssVar, newWidth + 'px');
      }
      function onUp() {
        handle.classList.remove('dragging');
        localStorage.setItem(storageKey, parseInt(getComputedStyle(layout).getPropertyValue(cssVar)) || defaultWidth);
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
      }
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  }

  makeResizable('gen-resize-handle',  '.gen-layout',       '--gen-col-width',  500, 'gen_col_width',  280, 900);
  makeResizable('spin-resize-handle', '.spin-test-layout', '--spin-col-width', 300, 'spin_col_width', 200, 700);

  // ── RTP panel resize ────────────────────────────────────────────────────────
  (function () {
    const MIN_WIDTH = 280;
    const MAX_WIDTH = 900;
    const STORAGE_KEY = 'rtp_col_width';

    const handle = document.getElementById('rtp-resize-handle');
    if (!handle) return;

    const layout = handle.closest('.rtp-layout');
    if (!layout) return;

    const saved = parseInt(localStorage.getItem(STORAGE_KEY), 10);
    if (saved && saved >= MIN_WIDTH && saved <= MAX_WIDTH) {
      layout.style.setProperty('--rtp-col-width', saved + 'px');
    }

    let startX = 0;
    let startWidth = 0;

    handle.addEventListener('mousedown', function (e) {
      e.preventDefault();
      startX = e.clientX;
      startWidth = parseInt(getComputedStyle(layout).getPropertyValue('--rtp-col-width')) || 420;
      handle.classList.add('dragging');

      function onMove(ev) {
        const delta = ev.clientX - startX;
        const newWidth = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, startWidth + delta));
        layout.style.setProperty('--rtp-col-width', newWidth + 'px');
      }

      function onUp() {
        handle.classList.remove('dragging');
        const current = parseInt(getComputedStyle(layout).getPropertyValue('--rtp-col-width')) || 420;
        localStorage.setItem(STORAGE_KEY, current);
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
      }

      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  })();
})();
