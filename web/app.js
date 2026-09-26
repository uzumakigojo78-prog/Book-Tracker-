'use strict';
// Book Tracker for the web (iPhone via "Add to Home Screen"). Same data model
// as the Android app: books with a day-by-day log of the page reached.

const STORE_KEY = 'booktracker.v1';
const app = document.getElementById('app');

/* ---------- icons (Material Symbols, 24px) ---------- */
const ICONS = {
  add: 'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z',
  back: 'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',
  edit: 'M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z',
  delete: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
  close: 'M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z',
  check: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z',
  person: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z',
  event: 'M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z',
  search: 'M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
  book: 'M21 5c-1.11-.35-2.33-.5-3.5-.5-1.95 0-4.05.4-5.5 1.5-1.45-1.1-3.55-1.5-5.5-1.5S2.45 4.9 1 6v14.65c0 .25.25.5.5.5.1 0 .15-.05.25-.05C3.1 20.45 5.05 20 6.5 20c1.95 0 4.05.4 5.5 1.5 1.35-.85 3.8-1.5 5.5-1.5 1.65 0 3.35.3 4.75 1.05.1.05.15.05.25.05.25 0 .5-.25.5-.5V6c-.6-.45-1.25-.75-2-1zm0 13.5c-1.1-.35-2.3-.5-3.5-.5-1.7 0-4.15.65-5.5 1.5V8c1.35-.85 3.8-1.5 5.5-1.5 1.2 0 2.4.15 3.5.5v11.5z',
  fire: 'M13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67zM11.71 19c-1.78 0-3.22-1.4-3.22-3.14 0-1.62 1.05-2.76 2.81-3.12 1.77-.36 3.6-1.21 4.62-2.58.39 1.29.59 2.65.59 4.04 0 2.65-2.15 4.8-4.8 4.8z',
  more: 'M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z',
  sparkle: 'M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z',
};
const icon = (name, cls = '') => `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true"><path d="${ICONS[name]}"/></svg>`;

/* ---------- small helpers ---------- */
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const pad = (n) => String(n).padStart(2, '0');
const toISO = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const fromISO = (iso) => { const [y, m, d] = iso.split('-').map(Number); return new Date(y, m - 1, d); };
const today = () => toISO(new Date());
const addDays = (iso, n) => { const d = fromISO(iso); d.setDate(d.getDate() + n); return toISO(d); };
const prettyDate = (iso) => fromISO(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
const relDate = (iso) => (iso === today() ? 'Today' : iso === addDays(today(), -1) ? 'Yesterday' : prettyDate(iso));
const uuid = () => (crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36) + Math.random().toString(36).slice(2));
const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));

/* ---------- data ---------- */
let books = loadBooks();

function loadBooks() {
  try {
    const data = JSON.parse(localStorage.getItem(STORE_KEY));
    return Array.isArray(data?.books) ? data.books : [];
  } catch { return []; }
}
function saveBooks() {
  try { localStorage.setItem(STORE_KEY, JSON.stringify({ version: 1, books })); }
  catch { toast("Couldn't save. Is private browsing on?"); }
}
// Ask the browser not to evict our data.
navigator.storage?.persist?.().catch(() => {});

const sortedEntries = (b) => [...b.entries].sort((x, y) => x.date.localeCompare(y.date));
const currentPage = (b) => sortedEntries(b).at(-1)?.page ?? 0;
const progress = (b) => (b.totalPages > 0 ? clamp(currentPage(b) / b.totalPages, 0, 1) : 0);
const isFinished = (b) => b.totalPages > 0 && currentPage(b) >= b.totalPages;
/** Pages read on each logged day, newest first. */
function dailyPages(b) {
  let prev = 0;
  return sortedEntries(b).map((e) => {
    const read = Math.max(0, e.page - prev); prev = e.page;
    return { date: e.date, page: e.page, read };
  }).reverse();
}
const pagesToday = (b) => dailyPages(b).find((d) => d.date === today())?.read ?? 0;
function streak(b) {
  const days = new Set(dailyPages(b).filter((d) => d.read > 0).map((d) => d.date));
  let day = today(); if (!days.has(day)) day = addDays(day, -1);
  let n = 0; while (days.has(day)) { n++; day = addDays(day, -1); }
  return n;
}
const colorIndex = (b) => books.indexOf(b) % 3;
const findBook = (id) => books.find((b) => b.id === id);

function logPage(b, date, page) {
  b.entries = b.entries.filter((e) => e.date !== date);
  b.entries.push({ date, page: clamp(page, 0, b.totalPages) });
  saveBooks();
}

/* ---------- navigation (hash routes) ---------- */
// #/  |  #/add  |  #/book/<id>  |  #/book/<id>/edit
let navDepth = 0;
let lastRouteDepth = 0;
const depthOf = (parts) => (parts[0] === 'book' ? (parts[2] === 'edit' ? 2 : 1) : parts[0] === 'add' ? 1 : 0);
function go(path) { navDepth++; location.hash = path; }
function back(parent) {
  if (navDepth > 0) { navDepth--; history.back(); } else { location.replace('#' + parent); }
}
window.addEventListener('hashchange', render);

function render() {
  closeOverlays();
  const parts = location.hash.replace(/^#\/?/, '').split('/').filter(Boolean);
  const depth = depthOf(parts);
  const dir = depth < lastRouteDepth ? 'back' : '';
  lastRouteDepth = depth;
  window.scrollTo(0, 0);
  if (parts[0] === 'add') return renderEditor(null, dir);
  if (parts[0] === 'book') {
    const book = findBook(parts[1]);
    if (!book) { location.replace('#/'); return; }
    return parts[2] === 'edit' ? renderEditor(book, dir) : renderDetail(book, dir);
  }
  renderLibrary(dir);
}

/* ---------- shared pieces ---------- */
function badge(b, idx, size = '') {
  const letter = esc((b.title || '?').trim().charAt(0).toUpperCase() || '?');
  const img = b.coverUrl
    ? `<img src="${esc(b.coverUrl)}" alt="" loading="lazy" onload="this.classList.add('loaded')" onerror="this.remove()">`
    : '';
  return `<div class="badge ${size} k${idx}" style="background:var(--accent);color:var(--on-accent)">${letter}${img}</div>`;
}

/** M3 Expressive wavy linear progress. */
function wavyLine(p) {
  const W = 300, mid = 7, amp = 3, wave = 22, gap = 8;
  const end = clamp(p, 0, 1) * W;
  let d = '';
  if (end > 0.5) {
    d = `M0 ${mid}`;
    for (let x = 2; x <= end; x += 2) d += ` L${x} ${(mid + amp * Math.sin((x / wave) * 2 * Math.PI)).toFixed(2)}`;
  }
  const trackStart = end > 0.5 ? end + gap : 0;
  return `<svg class="wavy" viewBox="0 0 ${W} 14" preserveAspectRatio="none" aria-hidden="true">
    ${trackStart < W - 4 ? `<line x1="${trackStart}" y1="${mid}" x2="${W - 2}" y2="${mid}" style="stroke:currentColor;stroke-opacity:.18" stroke-width="5" stroke-linecap="round" vector-effect="non-scaling-stroke"/>` : ''}
    ${d ? `<path class="draw" pathLength="1" d="${d}" fill="none" style="stroke:var(--accent)" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" vector-effect="non-scaling-stroke"/>` : ''}
  </svg>`;
}

/** M3 Expressive wavy circular progress. */
function wavyRing(p) {
  const c = 85, r = 70, amp = 2.6, waves = 14, gapDeg = 12;
  const endDeg = clamp(p, 0, 1) * 360;
  const pt = (deg, wobble) => {
    const t = ((deg - 90) * Math.PI) / 180;
    const rr = r + (wobble ? amp * Math.sin(waves * t) : 0);
    return `${(c + rr * Math.cos(t)).toFixed(2)} ${(c + rr * Math.sin(t)).toFixed(2)}`;
  };
  const arc = (from, to, wobble) => {
    let d = `M${pt(from, wobble)}`;
    for (let a = from + 2; a < to; a += 2) d += ` L${pt(a, wobble)}`;
    return d + ` L${pt(to, wobble)}`;
  };
  const hasProgress = endDeg > 1;
  const full = endDeg >= 359;
  const trackFrom = hasProgress ? endDeg + gapDeg : 0;
  const trackTo = hasProgress ? 360 - gapDeg : 360;
  return `<svg viewBox="0 0 170 170" aria-hidden="true">
    ${!full && trackTo > trackFrom ? `<path d="${arc(trackFrom, trackTo, false)}" fill="none" style="stroke:currentColor;stroke-opacity:.18" stroke-width="14" stroke-linecap="round"/>` : ''}
    ${hasProgress ? `<path class="draw" pathLength="1" d="${arc(0, Math.min(endDeg, 360), true)}" fill="none" style="stroke:var(--accent)" stroke-width="14" stroke-linecap="round" stroke-linejoin="round"/>` : ''}
  </svg>`;
}

function toast(msg) {
  const t = document.getElementById('toast');
  t.textContent = msg; t.classList.add('show');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => t.classList.remove('show'), 2600);
}

function closeOverlays() { document.querySelectorAll('.scrim, .menu').forEach((el) => el.remove()); }

function confirmDialog({ title, text, confirm, danger }) {
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

/* ---------- library ---------- */
function renderLibrary(dir) {
  const reading = books.filter((b) => !isFinished(b)).length;
  const finished = books.length - reading;
  const todayPages = books.reduce((n, b) => n + pagesToday(b), 0);
  // Unfinished first, newest first.
  const ordered = [...books].sort((a, b) => (isFinished(a) - isFinished(b)) || (b.createdAt - a.createdAt));

  app.innerHTML = `<section class="screen ${dir}">
    <header class="bar" style="margin-left:0">
      <h1 class="display grow">My Books</h1>
      <button class="icon-btn" data-menu aria-label="More options">${icon('more')}</button>
    </header>
    ${books.length === 0 ? `
      <div class="empty">
        <div class="blob">${icon('book')}</div>
        <h2 class="headline">No books yet</h2>
        <p class="muted">Tap “Add book” to start tracking what you read, day by day.</p>
      </div>` : `
      <div class="stats">
        <div class="stat c0"><b>${reading}</b><span>Reading</span></div>
        <div class="stat c1" style="animation-delay:.05s"><b>${todayPages}</b><span>Pages today</span></div>
        <div class="stat c2" style="animation-delay:.1s"><b>${finished}</b><span>Finished</span></div>
      </div>
      <div class="list">${ordered.map((b, i) => bookCard(b, i)).join('')}</div>`}
    <button class="fab" data-add>${icon('add')}Add book</button>
  </section>`;

  app.querySelector('[data-add]').onclick = () => go('/add');
  app.querySelector('[data-menu]').onclick = openLibraryMenu;
  app.querySelectorAll('[data-book]').forEach((el) => { el.onclick = () => go('/book/' + el.dataset.book); });
}

function bookCard(b, i) {
  const idx = colorIndex(b);
  const pct = Math.floor(progress(b) * 100);
  return `<button class="card k${idx}" data-book="${esc(b.id)}" style="animation-delay:${Math.min(i, 8) * 0.04}s">
    <div class="row">
      ${badge(b, idx)}
      <div class="grow">
        <h3 class="title clamp2">${esc(b.title)}</h3>
        ${b.author ? `<div class="ellipsis" style="font-size:17px">by ${esc(b.author)}</div>` : ''}
        ${b.releaseDate ? `<div style="font-size:13px">Released ${esc(prettyDate(b.releaseDate))}</div>` : ''}
      </div>
      ${isFinished(b) ? icon('check', 'check') : ''}
    </div>
    ${wavyLine(progress(b))}
    <div class="progress-row"><span class="title-m grow">Page ${currentPage(b)} of ${b.totalPages}</span><span class="pct">${pct}%</span></div>
  </button>`;
}

function openLibraryMenu() {
  closeOverlays();
  const menu = document.createElement('div');
  menu.className = 'menu';
  menu.innerHTML = `<button data-export>Back up books</button><button data-csv>Export CSV (spreadsheet)</button><button data-import>Restore from backup</button>`;
  const scrim = document.createElement('div');
  scrim.className = 'scrim'; scrim.style.background = 'transparent';
  scrim.onclick = closeOverlays;
  document.body.append(scrim, menu);

  menu.querySelector('[data-export]').onclick = () => {
    closeOverlays();
    download(JSON.stringify({ version: 1, books }, null, 2), `book-tracker-backup-${today()}.json`, 'application/json');
  };
  menu.querySelector('[data-csv]').onclick = () => {
    closeOverlays();
    download(booksToCsv(books), `booktracker-backup-${today()}.csv`, 'text/csv');
  };
  menu.querySelector('[data-import]').onclick = () => {
    closeOverlays();
    const input = document.createElement('input');
    input.type = 'file'; input.accept = 'application/json,.json';
    input.onchange = async () => {
      try {
        const data = JSON.parse(await input.files[0].text());
        if (!Array.isArray(data?.books)) throw new Error('bad file');
        const ok = await confirmDialog({ title: 'Restore backup?', text: `This replaces your current books with ${data.books.length} from the backup.`, confirm: 'Restore' });
        if (!ok) return;
        books = data.books.map((b) => ({ entries: [], createdAt: 0, coverUrl: null, releaseDate: null, author: '', ...b }));
        saveBooks(); render(); toast('Backup restored');
      } catch { toast("That doesn't look like a Book Tracker backup"); }
    };
    input.click();
  };
}

function download(text, filename, type) {
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([text], { type }));
  a.download = filename;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 5000);
}

/** Same CSV layout as the Android app's backups: one row per logged day. */
function booksToCsv(list) {
  const cell = (v) => { const s = String(v ?? ''); return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s; };
  const rows = [['book_id', 'title', 'author', 'release_date', 'total_pages', 'cover_url', 'added_at', 'log_date', 'page_reached', 'pages_read']];
  for (const b of list) {
    const base = [b.id, b.title, b.author, b.releaseDate, b.totalPages, b.coverUrl, b.createdAt];
    const days = dailyPages(b).reverse();
    if (days.length === 0) rows.push([...base, '', '', '']);
    days.forEach((d) => rows.push([...base, d.date, d.page, d.read]));
  }
  return '\uFEFF' + rows.map((r) => r.map(cell).join(',')).join('\r\n') + '\r\n';
}

/* ---------- book detail ---------- */
function renderDetail(b, dir) {
  const idx = colorIndex(b);
  const pct = Math.floor(progress(b) * 100);
  const days = dailyPages(b);
  const s = streak(b);

  app.innerHTML = `<section class="screen ${dir}">
    <header class="bar">
      <button class="icon-btn" data-back aria-label="Back">${icon('back')}</button>
      <span class="grow"></span>
      <button class="icon-btn" data-edit aria-label="Edit book">${icon('edit')}</button>
      <button class="icon-btn" data-delete aria-label="Delete book">${icon('delete')}</button>
    </header>

    <div class="hero k${idx}">
      <div class="row" style="align-items:flex-start">
        ${b.coverUrl ? badge(b, idx, 'big') : ''}
        <div class="grow">
          <h1 class="${b.coverUrl ? 'headline' : 'display'}">${esc(b.title)}</h1>
          <div style="margin-top:10px">
            ${b.author ? `<div class="info">${icon('person')}${esc(b.author)}</div>` : ''}
            <div class="info">${icon('event')}${b.releaseDate ? 'Released ' + esc(prettyDate(b.releaseDate)) : 'Release date unknown'}</div>
          </div>
        </div>
      </div>
      <div class="row" style="margin-top:24px;gap:20px">
        <div class="ring-wrap">${wavyRing(progress(b))}
          <div class="ring-center"><span class="hero-num">${pct}%</span>${isFinished(b) ? '<span class="label">Finished!</span>' : ''}</div>
        </div>
        <div class="stack">
          <div class="big-stat"><b>${currentPage(b)}</b><span>of ${b.totalPages} pages</span></div>
          <div class="big-stat"><b>${Math.max(0, b.totalPages - currentPage(b))}</b><span>pages left</span></div>
          ${s > 0 ? `<div class="info" style="font-size:15px"><svg viewBox="0 0 24 24" style="fill:var(--accent)"><path d="${ICONS.fire}"/></svg>${s} day streak</div>` : ''}
        </div>
      </div>
    </div>

    <div class="panel">
      <h2 class="headline-s">Log your reading</h2>
      <div style="margin-top:16px">
        <label class="field">
          <input type="date" data-date value="${today()}" max="${today()}" required>
          <span class="lbl">Day</span>${icon('event', 'lead')}
        </label>
      </div>
      <div style="margin-top:12px">
        <label class="field">
          <input data-page inputmode="numeric" pattern="[0-9]*" enterkeyhint="done" autocomplete="off">
          <span class="lbl">Page I'm on</span>${icon('book', 'lead')}<span class="suffix">/ ${b.totalPages}</span>
        </label>
        <div class="help" data-help></div>
      </div>
      <div class="chips">${[1, 5, 10, 25].map((n) => `<button class="btn" data-bump="${n}">+${n}</button>`).join('')}</div>
      <div class="row" style="margin-top:16px;gap:12px">
        <button class="btn big filled grow" data-save>Save page</button>
        <button class="btn" data-end style="height:64px;width:64px;border-radius:20px;padding:0">END</button>
      </div>
    </div>

    <div class="panel k${idx}" style="background:var(--surface-container-high);color:var(--on-bg)">
      <div class="row" style="align-items:baseline">
        <h2 class="headline-s grow">Last 14 days</h2><span class="title-m" style="color:var(--accent)" data-total></span>
      </div>
      <div class="chart" data-chart></div>
      <div class="days" data-days></div>
    </div>

    <h2 class="headline section-title">Daily log</h2>
    ${days.length === 0 ? `<p class="muted" style="margin:0 4px">Nothing logged yet. Save the page you reached today to start your log.</p>` :
      days.map((d, i) => `<div class="entry" style="animation-delay:${Math.min(i, 10) * 0.03}s">
        <div class="amount k${idx}">+${d.read}</div>
        <div class="grow"><div class="title-m">${esc(relDate(d.date))}</div><div class="muted" style="font-size:14px">Reached page ${d.page}</div></div>
        <button class="icon-btn" data-remove="${d.date}" aria-label="Remove entry">${icon('close')}</button>
      </div>`).join('')}
  </section>`;

  const $ = (sel) => app.querySelector(sel);
  const dateInput = $('[data-date]'), pageInput = $('[data-page]'), help = $('[data-help]'), saveBtn = $('[data-save]');

  const entryFor = (date) => b.entries.find((e) => e.date === date);
  const resetPage = () => { pageInput.value = String(entryFor(dateInput.value)?.page ?? currentPage(b)); update(); };
  function update() {
    const raw = pageInput.value.replace(/\D/g, '').slice(0, 6);
    if (raw !== pageInput.value) pageInput.value = raw;
    const page = raw === '' ? null : Number(raw);
    const valid = page !== null && page <= b.totalPages;
    const prev = sortedEntries(b).filter((e) => e.date < dateInput.value).at(-1)?.page ?? 0;
    pageInput.parentElement.classList.toggle('error', raw !== '' && !valid);
    help.classList.toggle('error', raw !== '' && !valid);
    help.textContent = raw !== '' && !valid ? `Enter a page between 0 and ${b.totalPages}`
      : valid && page - prev > 0 ? `That's ${page - prev} pages read on this day` : 'The page number you reached';
    saveBtn.disabled = !valid || !dateInput.value;
  }
  pageInput.oninput = update;
  dateInput.onchange = () => { if (dateInput.value > today()) dateInput.value = today(); resetPage(); };
  app.querySelectorAll('[data-bump]').forEach((btn) => {
    btn.onclick = () => {
      const base = pageInput.value === '' ? currentPage(b) : Number(pageInput.value);
      pageInput.value = String(clamp(base + Number(btn.dataset.bump), 0, b.totalPages)); update();
    };
  });
  $('[data-end]').onclick = () => { pageInput.value = String(b.totalPages); update(); };
  saveBtn.onclick = () => {
    const page = Number(pageInput.value), date = dateInput.value;
    logPage(b, date, page);
    toast(`Saved page ${page} · ${relDate(date)}`);
    renderDetail(b, 'none');
  };
  pageInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') pageInput.blur(); });
  resetPage();

  // 14-day chart
  const byDate = Object.fromEntries(days.map((d) => [d.date, d.read]));
  const range = Array.from({ length: 14 }, (_, i) => addDays(today(), i - 13));
  const values = range.map((d) => byDate[d] ?? 0);
  const max = Math.max(1, ...values);
  $('[data-total]').textContent = `${values.reduce((a, v) => a + v, 0)} pages`;
  $('[data-chart]').innerHTML = range.map((d, i) => `<i class="${values[i] === 0 ? 'zero' : ''} ${d === today() && values[i] > 0 ? 'today' : ''}"></i>`).join('');
  $('[data-days]').innerHTML = range.map((d) => `<span class="${d === today() ? 'today' : ''}">${fromISO(d).toLocaleDateString(undefined, { weekday: 'narrow' })}</span>`).join('');
  requestAnimationFrame(() => requestAnimationFrame(() => {
    app.querySelectorAll('[data-chart] i').forEach((bar, i) => {
      const v = values[i];
      bar.style.height = (v === 0 ? 4 : Math.max(8, (v / max) * 100)) + '%';
    });
  }));

  $('[data-back]').onclick = () => back('/');
  $('[data-edit]').onclick = () => go(`/book/${b.id}/edit`);
  $('[data-delete]').onclick = async () => {
    const ok = await confirmDialog({ title: 'Delete this book?', text: `“${b.title}” and its whole reading log will be removed.`, confirm: 'Delete', danger: true });
    if (!ok) return;
    books = books.filter((x) => x !== b); saveBooks();
    navDepth = 0; location.replace('#/');
  };
  app.querySelectorAll('[data-remove]').forEach((btn) => {
    btn.onclick = () => { b.entries = b.entries.filter((e) => e.date !== btn.dataset.remove); saveBooks(); renderDetail(b, 'none'); };
  });
}

/* ---------- add / edit with autofill ---------- */
function renderEditor(book, dir) {
  const form = {
    title: book?.title ?? '', author: book?.author ?? '', releaseDate: book?.releaseDate ?? '',
    pages: book ? String(book.totalPages) : '', coverUrl: book?.coverUrl ?? null,
  };
  let filledTitle = book?.title ?? null; // typing this exact title again won't re-search
  let autoFilled = false;
  let triedSave = false;
  let timer = null, controller = null, results = [];

  app.innerHTML = `<section class="screen ${dir}">
    <header class="bar">
      <button class="icon-btn" data-close aria-label="Close">${icon('close')}</button>
      <h1 class="headline-s grow" style="font-weight:900">${book ? 'Edit book' : 'Add a book'}</h1>
    </header>
    <div class="stack">
      ${book ? '' : '<p class="muted" style="margin:0 4px">Start typing the title and pick your book. We’ll fill in the rest.</p>'}
      <div>
        <label class="field" data-title-field>
          <input data-title value="${esc(form.title)}" autocomplete="off" autocapitalize="words" enterkeyhint="search" spellcheck="false">
          <span class="lbl">Book name</span>${icon('search', 'lead')}
          <span class="trail" data-trail></span>
        </label>
        <div class="help" data-title-help></div>
      </div>
      <div data-suggestions></div>
      <div data-filled></div>
      <label class="field">
        <input data-author value="${esc(form.author)}" autocomplete="off" autocapitalize="words" enterkeyhint="next">
        <span class="lbl">Author</span>${icon('person', 'lead')}
      </label>
      <label class="field">
        <input type="date" data-release value="${esc(form.releaseDate)}">
        <span class="lbl">Release date</span>${icon('event', 'lead')}
      </label>
      <div>
        <label class="field" data-pages-field>
          <input data-pages value="${esc(form.pages)}" inputmode="numeric" pattern="[0-9]*" enterkeyhint="done" autocomplete="off">
          <span class="lbl">Total pages</span>${icon('book', 'lead')}
        </label>
        <div class="help" data-pages-help></div>
      </div>
      <button class="btn big filled" data-save style="margin-top:8px">${book ? 'Save changes' : 'Add book'}</button>
    </div>
  </section>`;

  const $ = (sel) => app.querySelector(sel);
  const titleIn = $('[data-title]'), authorIn = $('[data-author]'), releaseIn = $('[data-release]'), pagesIn = $('[data-pages]');
  const trail = $('[data-trail]'), sugBox = $('[data-suggestions]'), filledBox = $('[data-filled]');

  function setTrail(state) {
    trail.innerHTML = state === 'loading' ? '<div class="loader" aria-label="Searching"></div>'
      : titleIn.value ? `<button class="icon-btn" data-clear aria-label="Clear title">${icon('close')}</button>` : '';
    trail.querySelector('[data-clear]')?.addEventListener('click', () => {
      titleIn.value = ''; autoFilled = false; onTitle(); titleIn.focus();
    });
  }

  function showSuggestions(list, query) {
    results = list;
    if (!query) { sugBox.innerHTML = ''; return; }
    if (list.length === 0) {
      sugBox.innerHTML = `<p class="muted" style="margin:0 4px;font-size:14px">No matches found (or you're offline). You can fill in the details yourself.</p>`;
      return;
    }
    sugBox.innerHTML = `<div class="suggestions">${list.map((s, i) => `
      <button class="suggestion" data-pick="${i}">
        ${badge(s, i % 3, 'small')}
        <div class="grow">
          <div class="title-m clamp2">${esc(s.title)}</div>
          ${s.author ? `<div class="ellipsis" style="font-size:14px">${esc(s.author)}</div>` : ''}
          <div class="meta">${[s.releaseDate?.slice(0, 4), s.pages ? s.pages + ' pages' : ''].filter(Boolean).join(' · ')}</div>
        </div>
      </button>`).join('')}</div>`;
    sugBox.querySelectorAll('[data-pick]').forEach((el) => { el.onclick = () => fill(results[Number(el.dataset.pick)]); });
  }

  function showFilled() {
    if (!autoFilled && !form.coverUrl) { filledBox.innerHTML = ''; return; }
    filledBox.innerHTML = `<div class="filled">
      ${form.coverUrl ? badge({ title: titleIn.value, coverUrl: form.coverUrl }, 0, 'big') : ''}
      <div class="grow">
        ${autoFilled ? `<div class="filled-title">${icon('sparkle')}Details filled in</div>
          <div class="muted" style="font-size:14px">Double-check them below. Edition page counts can vary.</div>` : ''}
        ${form.coverUrl ? '<button class="btn text" data-uncover style="padding:0;height:40px">Remove cover</button>' : ''}
      </div></div>`;
    filledBox.querySelector('[data-uncover]')?.addEventListener('click', () => { form.coverUrl = null; showFilled(); });
  }

  function onTitle() {
    clearTimeout(timer); controller?.abort();
    const q = titleIn.value.trim();
    if (q.length < 3 || titleIn.value === filledTitle) { setTrail(); showSuggestions([], null); return; }
    setTrail('loading');
    timer = setTimeout(async () => {
      controller = new AbortController();
      const mine = controller;
      const list = await searchBooks(q, mine.signal);
      if (mine.signal.aborted) return;
      setTrail(); showSuggestions(list, q);
    }, 350);
  }

  function fill(s) {
    if (!s) return;
    clearTimeout(timer); controller?.abort();
    filledTitle = s.title;
    titleIn.value = s.title; authorIn.value = s.author; releaseIn.value = s.releaseDate ?? '';
    if (s.pages) pagesIn.value = String(s.pages);
    form.coverUrl = s.coverUrl; autoFilled = true;
    showSuggestions([], null); setTrail(); showFilled(); validate();
    if (!s.pages) pagesIn.focus(); else titleIn.blur();
  }

  function validate() {
    const pages = Number(pagesIn.value);
    const titleBad = triedSave && !titleIn.value.trim();
    const pagesBad = triedSave && !(pages > 0);
    $('[data-title-field]').classList.toggle('error', titleBad);
    $('[data-title-help]').className = 'help' + (titleBad ? ' error' : '');
    $('[data-title-help]').textContent = titleBad ? 'Give your book a name' : '';
    $('[data-pages-field]').classList.toggle('error', pagesBad);
    $('[data-pages-help]').className = 'help' + (pagesBad ? ' error' : '');
    $('[data-pages-help]').textContent = pagesBad ? 'How many pages does it have?' : '';
    return !!titleIn.value.trim() && pages > 0;
  }

  titleIn.addEventListener('input', () => { autoFilled = false; showFilled(); onTitle(); validate(); });
  titleIn.addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); results[0] ? fill(results[0]) : authorIn.focus(); } });
  authorIn.addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); pagesIn.focus(); } });
  pagesIn.addEventListener('input', () => { pagesIn.value = pagesIn.value.replace(/\D/g, '').slice(0, 6); validate(); });
  pagesIn.addEventListener('keydown', (e) => { if (e.key === 'Enter') pagesIn.blur(); });

  $('[data-close]').onclick = () => back(book ? `/book/${book.id}` : '/');
  $('[data-save]').onclick = () => {
    triedSave = true;
    if (!validate()) return;
    const details = {
      title: titleIn.value.trim(), author: authorIn.value.trim(),
      releaseDate: releaseIn.value || null, totalPages: Number(pagesIn.value), coverUrl: form.coverUrl,
    };
    if (book) {
      Object.assign(book, details); saveBooks();
      back(`/book/${book.id}`);
    } else {
      const b = { id: uuid(), ...details, entries: [], createdAt: Date.now() };
      books.push(b); saveBooks();
      location.replace(`#/book/${b.id}`); // replace the form so Back goes to the library
    }
  };

  setTrail(); showFilled();
  if (!book) titleIn.focus();
}

/* ---------- book lookup (Open Library + Google Books, no key) ---------- */
async function getJSON(url, signal) {
  const res = await fetch(url, { signal });
  if (!res.ok) throw new Error('HTTP ' + res.status);
  return res.json();
}

const MONTHS = { jan: 1, feb: 2, mar: 3, apr: 4, may: 5, jun: 6, jul: 7, aug: 8, sep: 9, sept: 9, oct: 10, nov: 11, dec: 12 };
/** Parses "May 04, 2021", "4 May 2021", "2021-05-04" etc. into ISO, or null. */
function parseLooseDate(s) {
  s = String(s).trim();
  let m = s.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (m) return `${m[1]}-${m[2]}-${m[3]}`;
  m = s.match(/^([A-Za-z]+)\.? (\d{1,2}),? (\d{4})$/);
  if (m && MONTHS[m[1].slice(0, 3).toLowerCase()]) return `${m[3]}-${pad(MONTHS[m[1].slice(0, 3).toLowerCase()])}-${pad(m[2])}`;
  m = s.match(/^(\d{1,2}) ([A-Za-z]+)\.? (\d{4})$/);
  if (m && MONTHS[m[2].slice(0, 3).toLowerCase()]) return `${m[3]}-${pad(MONTHS[m[2].slice(0, 3).toLowerCase()])}-${pad(m[1])}`;
  return null;
}
/** Google's "2021", "2021-05" or "2021-05-04". */
function parseIsoPartial(s) {
  if (/^\d{4}$/.test(s)) return `${s}-01-01`;
  if (/^\d{4}-\d{2}$/.test(s)) return `${s}-01`;
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10);
  return null;
}

async function searchOpenLibrary(q, signal) {
  const url = `https://openlibrary.org/search.json?q=${encodeURIComponent(q)}&limit=8&fields=title,author_name,first_publish_year,number_of_pages_median,cover_i,publish_date`;
  const data = await getJSON(url, signal);
  return (data.docs || []).filter((d) => d.title).map((d) => {
    const year = d.first_publish_year;
    const exact = (d.publish_date || []).map(parseLooseDate).filter((x) => x && Number(x.slice(0, 4)) === year).sort()[0];
    return {
      title: d.title,
      author: (d.author_name || []).join(', '),
      releaseDate: exact || (year ? `${year}-01-01` : null),
      pages: d.number_of_pages_median || null,
      coverUrl: d.cover_i ? `https://covers.openlibrary.org/b/id/${d.cover_i}-M.jpg?default=false` : null,
    };
  });
}

async function searchGoogle(q, signal) {
  const url = `https://www.googleapis.com/books/v1/volumes?q=${encodeURIComponent(q)}&maxResults=10&printType=books&fields=items(volumeInfo(title,authors,publishedDate,pageCount,imageLinks/thumbnail))`;
  const data = await getJSON(url, signal);
  return (data.items || []).map((it) => it.volumeInfo).filter((v) => v?.title).map((v) => ({
    title: v.title,
    author: (v.authors || []).join(', '),
    releaseDate: v.publishedDate ? parseIsoPartial(v.publishedDate) : null,
    pages: v.pageCount || null,
    coverUrl: v.imageLinks?.thumbnail ? v.imageLinks.thumbnail.replace('http://', 'https://').replace('&edge=curl', '') : null,
  }));
}

const norm = (s) => String(s).toLowerCase().replace(/[^\p{L}\p{N}]/gu, '');
/** Merges results for the same book, preferring a precise date over a year-only one. */
function mergeResults(primary, secondary) {
  const out = new Map();
  for (const s of [...primary, ...secondary]) {
    const key = norm(s.title) + '|' + norm(s.author.split(',')[0]);
    const e = out.get(key);
    if (!e) { out.set(key, s); continue; }
    const yearOnly = e.releaseDate?.endsWith('-01-01');
    if (!e.releaseDate || (yearOnly && s.releaseDate && s.releaseDate.slice(0, 4) === e.releaseDate.slice(0, 4) && !s.releaseDate.endsWith('-01-01'))) e.releaseDate = s.releaseDate;
    e.pages ??= s.pages;
    e.coverUrl ??= s.coverUrl;
  }
  return [...out.values()];
}

async function searchBooks(q, signal) {
  const [ol, g] = await Promise.allSettled([searchOpenLibrary(q, signal), searchGoogle(q, signal)]);
  return mergeResults(ol.value || [], g.value || []).slice(0, 8);
}

/* ---------- start ---------- */
if ('serviceWorker' in navigator && location.protocol === 'https:') {
  navigator.serviceWorker.register('sw.js').catch(() => {});
}
render();
