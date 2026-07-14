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
