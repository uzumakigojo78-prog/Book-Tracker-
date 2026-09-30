// Small shared helpers: escaping, dates, icons, toasts, dialogs, sheets.

export const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
export const pad = (n) => String(n).padStart(2, '0');
export const toISO = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
export const fromISO = (iso) => { const [y, m, d] = iso.split('-').map(Number); return new Date(y, m - 1, d); };
export const today = () => toISO(new Date());
export const addDays = (iso, n) => { const d = fromISO(iso); d.setDate(d.getDate() + n); return toISO(d); };
export const prettyDate = (iso) => fromISO(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
export const relDate = (iso) => (iso === today() ? 'Today' : iso === addDays(today(), -1) ? 'Yesterday' : prettyDate(iso));
export const uuid = () => (crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36) + Math.random().toString(36).slice(2));
export const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));

/** A Material Symbols icon; the font (outlined/rounded/sharp/filled) follows Settings → Style. */
export const icon = (name, cls = '') => `<span class="ms ${cls}" aria-hidden="true">${name}</span>`;

export function toast(msg) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => t.classList.remove('show'), 2600);
}

export function download(text, filename, type) {
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([text], { type }));
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 5000);
}

export function closeOverlays() {
  document.querySelectorAll('.scrim').forEach((el) => el.remove());
}

export function confirmDialog({ title, text, confirm, danger }) {
  return new Promise((resolve) => {
    const scrim = document.createElement('div');
    scrim.className = 'scrim';
    scrim.innerHTML = `<div class="dialog" role="alertdialog" aria-modal="true">
      <h2 class="headline-s">${esc(title)}</h2><p class="muted">${esc(text)}</p>
      <div class="actions"><button class="btn text" data-no>Cancel</button>
      <button class="btn ${danger ? 'danger' : 'filled'}" data-yes>${esc(confirm)}</button></div></div>`;
    const done = (v) => { scrim.remove(); resolve(v); };
    scrim.addEventListener('click', (e) => { if (e.target === scrim || e.target.closest('[data-no]')) done(false); });
    scrim.querySelector('[data-yes]').addEventListener('click', () => done(true));
    document.body.appendChild(scrim);
  });
}

/** Slides a bottom sheet up; returns its content element. */
export function openSheet(html) {
  closeOverlays();
  const scrim = document.createElement('div');
  scrim.className = 'scrim sheet-scrim';
  scrim.innerHTML = `<div class="sheet" role="dialog" aria-modal="true"><div class="sheet-handle"></div><div class="sheet-body">${html}</div></div>`;
  scrim.addEventListener('click', (e) => { if (e.target === scrim) closeSheet(scrim); });
  document.body.appendChild(scrim);
  return scrim.querySelector('.sheet-body');
}

export function closeSheet(scrim = document.querySelector('.sheet-scrim')) {
  if (!scrim) return;
  scrim.classList.add('closing');
  setTimeout(() => scrim.remove(), 220);
}

export const store = {
  get(key, fallback = null) {
    try { const v = localStorage.getItem(key); return v === null ? fallback : JSON.parse(v); } catch { return fallback; }
  },
  set(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); return true; } catch { return false; }
  },
  remove(key) { try { localStorage.removeItem(key); } catch { /* ignore */ } },
};
