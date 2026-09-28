// Book Tracker for the web (iPhone via "Add to Home Screen"). Same features and data
// model as the Android app: tabs for Books, Genres, Stats and Settings.
import {
  addDays, clamp, closeOverlays, closeSheet, confirmDialog, download, esc, fromISO, icon, openSheet,
  prettyDate, relDate, store, toast, today, uuid,
} from './js/util.js';
import {
  books, booksFromBackup, booksToCsv, colorIndex, currentPage, dailyPages, findBook, isFinished, logPage,
  pagesToday, progress, replaceBooks, saveBooks, sortedEntries, streak, streakOf,
} from './js/data.js';
import {
  CORNERS, FONTS, ICON_STYLES, PALETTES, TEXT_SIZES, THEME_MODES, appearance, applyAppearance, isDark,
  loadFonts, paletteSwatch, setAppearance,
} from './js/theme.js';
import { GENRES, PROVIDERS, aiSettings, analyzeWithAi, getConfig, librarySignature, providerById } from './js/ai.js';
import { analyzeBasic, catalogDetails, catalogGenres, catalogSearch, copiesFrom, findCopies, searchBooks } from './js/lookup.js';

const app = document.getElementById('app');
const tabsBar = document.getElementById('tabs');
const VERSION = '2.0';

// Ask the browser not to evict our data.
navigator.storage?.persist?.().catch(() => {});

/* ---------- navigation (hash routes) ---------- */
// Tabs: #/  #/genres  #/stats  #/settings
// Pages: #/add[/<title>]  #/book/<id>[/log|/history]  #/book/<id>/edit  #/settings/<page>
const TABS = [['', 'Books', 'menu_book'], ['genres', 'Genres', 'category'], ['stats', 'Stats', 'bar_chart'], ['settings', 'Settings', 'settings']];
let navDepth = 0;
let lastDepth = 0;
let lastTab = '';

const parts = () => location.hash.replace(/^#\/?/, '').split('/').filter(Boolean).map(decodeURIComponent);
const isTabRoute = (p) => p.length === 0 || (p.length === 1 && TABS.some(([r]) => r === p[0]));
const depthOf = (p) => (isTabRoute(p) ? 0 : p[0] === 'book' && p[2] === 'edit' ? 2 : 1);

function go(path) { navDepth++; location.hash = path; }
function back(parent) {
  if (navDepth > 0) { navDepth--; history.back(); } else { location.replace('#' + parent); }
}
window.addEventListener('hashchange', render);

function render() {
  closeOverlays();
  const p = parts();
  const depth = depthOf(p);
  const tab = isTabRoute(p) ? (p[0] || '') : null;
  // Tab switches fade; pushing a page slides in; going back slides the other way.
  const dir = tab !== null && lastDepth === 0 ? 'fade' : depth < lastDepth ? 'back' : '';
  const sameBook = p[0] === 'book' && app.dataset.book === p[1] && p[2] !== 'edit' && lastDepth === 1;
  lastDepth = depth;
  if (!sameBook) window.scrollTo(0, 0);

  tabsBar.hidden = tab === null;
  document.body.classList.toggle('has-tabs', tab !== null);
  if (tab !== null) {
    lastTab = tab;
    tabsBar.innerHTML = TABS.map(([r, label, ic]) => `<a href="#/${r}" class="${r === tab ? 'active' : ''}" ${r === tab ? 'aria-current="page"' : ''}>
      <span class="pill">${icon(ic)}</span><span>${label}</span></a>`).join('');
  }
  app.dataset.book = p[0] === 'book' ? p[1] : '';

  if (p[0] === 'add') return renderEditor(null, dir, p[1] || '');
  if (p[0] === 'book') {
    const book = findBook(p[1]);
    if (!book) { location.replace('#/'); return; }
    return p[2] === 'edit' ? renderEditor(book, dir) : renderDetail(book, sameBook ? 'none' : dir, p[2] || 'overview');
  }
  if (p[0] === 'settings' && p[1]) return renderSettingsPage(p[1], dir);
  if (p[0] === 'genres') return renderGenres(dir);
  if (p[0] === 'stats') return renderStats(dir);
  if (p[0] === 'settings') return renderSettings(dir);
  if (p.length) { location.replace('#/'); return; }
  renderLibrary(dir);
}

/* ---------- shared pieces ---------- */
function badge(b, idx, size = '', persist = true) {
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

function pageHeader(title, subtitle = '') {
  return `<header class="page-head"><h1 class="display">${esc(title)}</h1>${subtitle ? `<p class="title-m muted">${esc(subtitle)}</p>` : ''}</header>`;
}

function subPageBar(title, extra = '') {
  return `<header class="bar">
    <button class="icon-btn" data-back aria-label="Back">${icon('arrow_back')}</button>
    <h1 class="headline-s grow bar-title">${esc(title)}</h1>${extra}
  </header>`;
}

/** Bars for pages read per day, oldest first; today's bar in the secondary colour. */
function chart(range, values) {
  return `<div class="chart" data-chart data-values="${values.join(',')}">
      ${range.map((d, i) => `<i class="${values[i] === 0 ? 'zero' : ''} ${d === today() && values[i] > 0 ? 'today' : ''}"></i>`).join('')}
    </div>
    <div class="days">${range.map((d) => `<span class="${d === today() ? 'today' : ''}">${fromISO(d).toLocaleDateString(undefined, { weekday: 'narrow' })}</span>`).join('')}</div>`;
}

/** Grows chart bars in after render (so they animate). */
function animateCharts() {
  requestAnimationFrame(() => requestAnimationFrame(() => {
    app.querySelectorAll('[data-chart]').forEach((c) => {
      const values = c.dataset.values.split(',').map(Number);
      const max = Math.max(1, ...values);
      c.querySelectorAll('i').forEach((bar, i) => {
        bar.style.height = (values[i] === 0 ? 4 : Math.max(8, (values[i] / max) * 100)) + '%';
      });
    });
  }));
}

function bookCard(b, i, idx = colorIndex(b), clickable = true) {
  const pct = Math.floor(progress(b) * 100);
  return `<${clickable ? 'button' : 'div'} class="card k${idx}" ${clickable ? `data-book="${esc(b.id)}"` : ''} style="animation-delay:${Math.min(i, 8) * 0.04}s">
    <div class="row">
      ${badge(b, idx)}
      <div class="grow">
        <h3 class="title clamp2">${esc(b.title)}</h3>
        ${b.author ? `<div class="ellipsis body-l">by ${esc(b.author)}</div>` : ''}
        ${b.releaseDate ? `<div class="body-s">Released ${esc(prettyDate(b.releaseDate))}</div>` : ''}
      </div>
      ${isFinished(b) ? `<span class="check">${icon('check_circle')}</span>` : ''}
    </div>
    ${wavyLine(progress(b))}
    <div class="progress-row"><span class="title-m grow">Page ${currentPage(b)} of ${b.totalPages}</span><span class="pct">${pct}%</span></div>
  </${clickable ? 'button' : 'div'}>`;
}

function bindBookLinks(root = app) {
  root.querySelectorAll('[data-book]').forEach((el) => { el.onclick = () => go('/book/' + el.dataset.book); });
}

/* ---------- Books tab ---------- */
function renderLibrary(dir) {
  const reading = books.filter((b) => !isFinished(b)).length;
  // Unfinished first, newest first.
  const ordered = [...books].sort((a, b) => (isFinished(a) - isFinished(b)) || (b.createdAt - a.createdAt));
  app.innerHTML = `<section class="screen ${dir}">
    ${pageHeader('My Books', books.length ? `${reading} reading · ${books.length - reading} finished` : '')}
    ${books.length === 0 ? `
      <div class="empty">
        <div class="blob">${icon('menu_book')}</div>
        <h2 class="headline">No books yet</h2>
        <p class="muted">Tap “Add book” to start tracking what you read, day by day.</p>
      </div>` : `<div class="list">${ordered.map((b, i) => bookCard(b, i)).join('')}</div>`}
    <button class="fab" data-add>${icon('add')}Add book</button>
  </section>`;
  app.querySelector('[data-add]').onclick = () => go('/add');
  bindBookLinks();
}

/* ---------- book page: Overview / Log / History tabs ---------- */
const DETAIL_TABS = [['overview', 'Overview', 'menu_book'], ['log', 'Log', 'edit'], ['history', 'History', 'history']];

function renderDetail(b, dir, tab) {
  const idx = colorIndex(b);
  const pct = Math.floor(progress(b) * 100);
  const s = streak(b);
  if (!DETAIL_TABS.some(([t]) => t === tab)) tab = 'overview';

  const overview = `
    <div class="hero k${idx}">
      <div class="row" style="align-items:flex-start">
        ${b.coverUrl ? badge(b, idx, 'big') : ''}
        <div class="grow">
          <h2 class="${b.coverUrl ? 'headline' : 'display'}">${esc(b.title)}</h2>
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
          ${s > 0 ? `<div class="info small"><span style="color:var(--accent)">${icon('local_fire_department')}</span>${s} day streak</div>` : ''}
        </div>
      </div>
    </div>
    <div class="panel row">
      <div class="grow"><div class="title-m muted">Today</div>
        <div class="headline-s">${pagesToday(b) > 0 ? `+${pagesToday(b)} pages` : 'Not logged yet'}</div></div>
      ${isFinished(b) ? '' : `<button class="btn filled k${idx} accent-btn" data-go-log>${icon('edit')}Log reading</button>`}
    </div>
    <button class="btn outline big" data-copies>${icon('download')}Find online copies</button>`;

  const log = `
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
          <span class="lbl">Page I'm on</span>${icon('menu_book', 'lead')}<span class="suffix">/ ${b.totalPages}</span>
        </label>
        <div class="help" data-help></div>
      </div>
      <div class="chips">${[1, 5, 10, 25].map((n) => `<button class="btn" data-bump="${n}">+${n}</button>`).join('')}</div>
      <div class="row" style="margin-top:16px;gap:12px">
        <button class="btn big filled grow" data-save>Save page</button>
        <button class="btn square-btn" data-end>END</button>
      </div>
    </div>`;

  const days = dailyPages(b);
  const byDate = Object.fromEntries(days.map((d) => [d.date, d.read]));
  const range = Array.from({ length: 14 }, (_, i) => addDays(today(), i - 13));
  const values = range.map((d) => byDate[d] ?? 0);
  const history = `
    <div class="panel k${idx}" style="background:var(--surface-container-high);color:var(--on-bg)">
      <div class="row" style="align-items:baseline">
        <h2 class="headline-s grow">Last 14 days</h2><span class="title-m" style="color:var(--accent)">${values.reduce((a, v) => a + v, 0)} pages</span>
      </div>
      ${chart(range, values)}
    </div>
    <h2 class="headline section-title">Daily log</h2>
    ${days.length === 0 ? `<p class="muted" style="margin:0 4px">Nothing logged yet. Save the page you reached today on the Log tab to start your log.</p>` :
      days.map((d, i) => `<div class="entry" style="animation-delay:${Math.min(i, 10) * 0.03}s">
        <div class="amount k${idx}">+${d.read}</div>
        <div class="grow"><div class="title-m">${esc(relDate(d.date))}</div><div class="muted body-s">Reached page ${d.page}</div></div>
        <button class="icon-btn" data-remove="${d.date}" aria-label="Remove entry">${icon('close')}</button>
      </div>`).join('')}`;

  app.innerHTML = `<section class="screen ${dir}">
    <div class="sticky-top">
      ${subPageBar(b.title, `<button class="icon-btn" data-edit aria-label="Edit book">${icon('edit')}</button>
        <button class="icon-btn" data-delete aria-label="Delete book">${icon('delete')}</button>`)}
      <nav class="tabrow">${DETAIL_TABS.map(([t, label, ic]) =>
        `<button class="${t === tab ? 'active' : ''}" data-tab="${t}">${icon(ic)}<span>${label}</span></button>`).join('')}</nav>
    </div>
    <div class="tab-content ${dir === 'none' ? 'swap' : ''}">${tab === 'overview' ? overview : tab === 'log' ? log : history}</div>
  </section>`;

  const $ = (sel) => app.querySelector(sel);
  const switchTab = (t) => location.replace(`#/book/${b.id}${t === 'overview' ? '' : '/' + t}`);
  app.querySelectorAll('[data-tab]').forEach((el) => { el.onclick = () => switchTab(el.dataset.tab); });
  // Swipe left/right between tabs, like the Android app.
  let startX = null, startY = null;
  const content = $('.tab-content');
  content.addEventListener('touchstart', (e) => { startX = e.touches[0].clientX; startY = e.touches[0].clientY; }, { passive: true });
  content.addEventListener('touchend', (e) => {
    if (startX === null) return;
    const dx = e.changedTouches[0].clientX - startX, dy = e.changedTouches[0].clientY - startY;
    startX = null;
    if (Math.abs(dx) < 70 || Math.abs(dy) > Math.abs(dx) * 0.7) return;
    const i = DETAIL_TABS.findIndex(([t]) => t === tab) + (dx < 0 ? 1 : -1);
    if (i >= 0 && i < DETAIL_TABS.length) switchTab(DETAIL_TABS[i][0]);
  });

  $('[data-back]').onclick = () => back('/');
  $('[data-edit]').onclick = () => go(`/book/${b.id}/edit`);
  $('[data-delete]').onclick = async () => {
    const ok = await confirmDialog({ title: 'Delete this book?', text: `“${b.title}” and its whole reading log will be removed.`, confirm: 'Delete', danger: true });
    if (!ok) return;
    replaceBooks(books.filter((x) => x !== b));
    navDepth = 0;
    location.replace('#/');
  };

  if (tab === 'overview') {
    $('[data-go-log]')?.addEventListener('click', () => switchTab('log'));
    $('[data-copies]').onclick = () => showCopies(b.title, b.author);
  }
  if (tab === 'history') {
    animateCharts();
    app.querySelectorAll('[data-remove]').forEach((btn) => {
      btn.onclick = () => { b.entries = b.entries.filter((e) => e.date !== btn.dataset.remove); saveBooks(); renderDetail(b, 'none', tab); };
    });
  }
  if (tab === 'log') bindLog(b);
}

function bindLog(b) {
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
      pageInput.value = String(clamp(base + Number(btn.dataset.bump), 0, b.totalPages));
      update();
    };
  });
  $('[data-end]').onclick = () => { pageInput.value = String(b.totalPages); update(); };
  saveBtn.onclick = () => {
    const page = Number(pageInput.value), date = dateInput.value;
    logPage(b, date, page);
    toast(`Saved page ${page} · ${relDate(date)}`);
    renderDetail(b, 'none', 'log');
  };
  pageInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') pageInput.blur(); });
  resetPage();
}

/* ---------- Stats tab ---------- */
function renderStats(dir) {
  const perDay = {};
  books.forEach((b) => dailyPages(b).forEach((d) => { perDay[d.date] = (perDay[d.date] || 0) + d.read; }));
  const range = Array.from({ length: 14 }, (_, i) => addDays(today(), i - 13));
  const values = range.map((d) => perDay[d] || 0);
  const week = Array.from({ length: 7 }, (_, i) => perDay[addDays(today(), -i)] || 0).reduce((a, v) => a + v, 0);
  const allTime = books.reduce((n, b) => n + currentPage(b), 0);
  const finished = books.filter(isFinished).length;
  const dayStreak = streakOf(new Set(Object.keys(perDay).filter((d) => perDay[d] > 0)));
  const since = addDays(today(), -7);
  const weekly = books
    .map((b) => ({ b, pages: dailyPages(b).filter((d) => d.date > since).reduce((n, d) => n + d.read, 0) }))
    .filter((x) => x.pages > 0).sort((a, b) => b.pages - a.pages);

  app.innerHTML = `<section class="screen ${dir}">
    ${pageHeader('Stats')}
    <div class="stats">
      <div class="stat c0"><b>${perDay[today()] || 0}</b><span>Pages today</span></div>
      <div class="stat c1" style="animation-delay:.05s"><b>${week}</b><span>This week</span></div>
      <div class="stat c2" style="animation-delay:.1s"><b>${dayStreak}</b><span>Day streak</span></div>
    </div>
    <div class="stats two">
      <div class="stat k0"><b>${allTime}</b><span>Pages read in total</span></div>
      <div class="stat k2" style="animation-delay:.05s"><b>${finished} / ${books.length}</b><span>Books finished</span></div>
    </div>
    <div class="panel">
      <div class="row" style="align-items:baseline">
        <h2 class="headline-s grow">Last 14 days</h2><span class="title-m" style="color:var(--primary)">${values.reduce((a, v) => a + v, 0)} pages</span>
      </div>
      <div style="--accent:var(--primary)">${chart(range, values)}</div>
    </div>
    <div class="panel">
      <h2 class="headline-s">Most read this week</h2>
      ${weekly.length === 0 ? '<p class="muted">Log some pages and your top books will show up here.</p>' :
        `<div class="weekly">${weekly.slice(0, 5).map(({ b, pages }) => `
          <button class="weekly-row k${colorIndex(b)}" data-book="${esc(b.id)}">
            ${badge(b, colorIndex(b), 'tiny')}
            <div class="grow"><div class="title-m ellipsis">${esc(b.title)}</div>
              <div class="meter"><i style="width:${(pages / weekly[0].pages) * 100}%"></i></div></div>
            <b class="title">${pages}</b>${icon('chevron_right', 'muted')}
          </button>`).join('')}</div>`}
    </div>
  </section>`;
  bindBookLinks();
  animateCharts();
}

/* ---------- add / edit with autofill ---------- */
function renderEditor(book, dir, initialTitle = '') {
  const form = {
    title: book?.title ?? initialTitle, author: book?.author ?? '', releaseDate: book?.releaseDate ?? '',
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
          <span class="lbl">Total pages</span>${icon('menu_book', 'lead')}
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
        ${autoFilled ? `<div class="filled-title">${icon('auto_awesome')}Details filled in</div>
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

  $('[data-close]').onclick = () => back(book ? `/book/${book.id}` : initialTitle ? '/genres' : '/');
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
  // A prefilled title (from a recommendation) starts the autofill search straight away.
  if (initialTitle) onTitle();
}

/* ---------- Genres tab ---------- */
const GENRES_KEY = 'booktracker.genres';
const genreState = { analysis: store.get(GENRES_KEY), running: false, error: null };
const signature = () => librarySignature(books, isFinished);
const helpers = { currentPage, isFinished, dailyPages };

async function analyze() {
  if (genreState.running || !books.length) return;
  genreState.running = true;
  genreState.error = null;
  if (parts()[0] === 'genres') renderGenres('none');
  const config = getConfig();
  try {
    const result = config
      ? await analyzeWithAi(config, books, signature(), helpers)
      : await analyzeBasic(books, signature(), currentPage);
    genreState.analysis = result;
    store.set(GENRES_KEY, result);
  } catch (e) {
    genreState.error = e?.message || 'Something went wrong. Try again.';
  }
  genreState.running = false;
  // Don't redraw under someone typing a search; the results show when the search is cleared.
  if (parts()[0] === 'genres' && genreQuery.trim().length < 2) renderGenres('none');
}

function renderGenres(dir) {
  const { analysis, running, error } = genreState;
  const ai = getConfig();
  const hasAi = !!ai;
  const name = ai && ai.provider.id !== 'custom' ? ai.provider.label : 'The AI';
  const outdated = !!analysis && analysis.librarySignature !== signature();
  // Basic mode is free, so keep it up to date automatically. AI runs only when asked.
  if (books.length && !hasAi && !running && !error && (!analysis || outdated)) setTimeout(analyze);

  let status;
  if (running) status = hasAi ? `${name} is reading your library…` : 'Sorting your books…';
  else if (hasAi && analysis?.source === 'BASIC') status = `Showing basic results. Tap Analyze for ${name}'s take.`;
  else if (hasAi && !analysis) status = `Tap Analyze to let ${name} sort your books and pick what to read next.`;
  else if (hasAi && outdated) status = 'Your library changed since the last analysis.';
  else if (hasAi && analysis?.madeBy && analysis.madeBy !== ai.displayName) status = `Last analysis by ${analysis.madeBy}. Tap Re-analyze to use ${name}.`;
  else if (hasAi) status = `Genres and picks by ${name}.`;
  else status = 'Genres from Open Library. Add an AI API key (Claude, Gemini, Grok, Kimi and more) for AI genres and personal picks.';

  let body = '';
  if (!books.length) {
    body = `<div class="panel"><h2 class="headline-s">Add some books first</h2>
      <p class="muted">Once you have books in your library, they'll be sorted into genres here with recommendations.</p></div>`;
  } else {
    body = `<div class="panel mode ${hasAi ? 'ai' : ''}">
        <div class="row">
          <div class="mode-icon">${icon(hasAi ? 'psychology' : 'category')}</div>
          <div class="grow"><div class="title">${hasAi ? `AI: ${esc(ai.displayName)}` : 'Basic mode'}</div><div class="muted body-m">${status}</div></div>
        </div>
        ${running ? '<div class="center" style="margin-top:14px"><div class="loader big"></div></div>' : `
        <div class="row" style="margin-top:14px;gap:10px">
          <button class="btn filled grow" data-analyze>${icon(analysis ? 'refresh' : 'auto_awesome')}${!analysis ? 'Analyze' : outdated ? 'Update' : 'Re-analyze'}</button>
          ${hasAi ? '' : `<button class="btn" data-setup-ai>${icon('key')}Set up AI</button>`}
        </div>`}
      </div>
      ${error ? `<div class="panel error-panel row">${icon('error')}<span>${esc(error)}</span></div>` : ''}`;

    if (analysis) {
      if (analysis.summary) {
        body += `<div class="panel taste"><div class="row" style="gap:8px">${icon('auto_awesome')}<span class="title">Your reading taste</span></div>
          <p class="body-l" style="margin:8px 0 0">${esc(analysis.summary)}</p></div>`;
      }
      // Group by main genre in the fixed genre order; books added since go last.
      const groups = new Map();
      books.forEach((b) => {
        const g = analysis.bookGenres[b.id]?.[0] || 'Not sorted yet';
        if (!groups.has(g)) groups.set(g, []);
        groups.get(g).push(b);
      });
      const order = [...GENRES, 'Not sorted yet'];
      [...groups.entries()].sort((a, b) => (order.indexOf(a[0]) - order.indexOf(b[0])) || (b[1].length - a[1].length)).forEach(([genre, list]) => {
        body += `<div class="panel genre">
          <div class="row"><h2 class="headline-s grow">${esc(genre)}</h2><span class="count">${list.length}</span></div>
          <div class="shelf">${list.map((b) => `<button class="shelf-book" data-book="${esc(b.id)}">
            ${badge(b, colorIndex(b), 'shelf-badge')}
            <span class="label clamp2">${esc(b.title)}</span>
            ${analysis.bookGenres[b.id]?.[1] ? `<span class="body-s muted ellipsis">+ ${esc(analysis.bookGenres[b.id][1])}</span>` : ''}
          </button>`).join('')}</div>
        </div>`;
      });
      if (analysis.recommendations.length) {
        body += `<h2 class="headline section-title">Recommended for you</h2>` + analysis.recommendations.map((r, i) => {
          const idx = Math.max(0, GENRES.indexOf(r.genre)) % 3;
          return `<div class="panel rec" data-rec="${i}" style="animation-delay:${Math.min(i, 8) * 0.04}s">
            <div class="row" style="align-items:flex-start">
              <div data-rec-cover>${badge({ title: r.title }, idx, 'rec-badge')}</div>
              <div class="grow">
                <h3 class="title clamp2">${esc(r.title)}</h3>
                <div class="muted body-m">${esc([r.author, r.year].filter(Boolean).join(' · '))}</div>
                <div class="pills"><span class="pill-tag k${idx}" style="background:var(--accent);color:var(--on-accent)">${esc(r.genre)}</span><span data-rec-access></span></div>
              </div>
            </div>
            ${r.reason ? `<p class="body-l" style="margin:12px 0 0">${esc(r.reason)}</p>` : ''}
            <div class="row" style="gap:10px;margin-top:14px">
              <button class="btn grow" data-copy="${i}">${icon('download')}Get a copy</button>
              <button class="btn outline grow" data-add-rec="${i}">${icon('library_add')}Add to books</button>
            </div>
          </div>`;
        }).join('');
      }
    }
  }

  const searching = genreQuery.trim().length >= 2;
  app.innerHTML = `<section class="screen ${dir}">${pageHeader('Genres', 'Search any book, see your genres and what to read next')}
    <label class="field search-field">
      <input type="search" data-search value="${esc(genreQuery)}" placeholder="Search any book, author or topic" enterkeyhint="search" autocomplete="off" spellcheck="false">
      ${icon('search', 'lead')}<span class="trail">${genreQuery ? `<button class="icon-btn" data-clear-search aria-label="Clear search">${icon('close')}</button>` : ''}</span>
    </label>
    <div data-genres-body>${searching ? searchBodyHtml() : body}</div></section>`;
  bindSearch();
  if (searching) return;
  bindBookLinks();
  app.querySelector('[data-analyze]')?.addEventListener('click', analyze);
  app.querySelector('[data-setup-ai]')?.addEventListener('click', () => go('/settings/ai'));
  const recs = analysis?.recommendations || [];
  app.querySelectorAll('[data-copy]').forEach((el) => { el.onclick = () => { const r = recs[el.dataset.copy]; showCopies(r.title, r.author); }; });
  app.querySelectorAll('[data-add-rec]').forEach((el) => { el.onclick = () => go('/add/' + encodeURIComponent(recs[el.dataset.addRec].title)); });
  // Covers and free/borrow badges load in as Open Library answers.
  app.querySelectorAll('[data-rec]').forEach(async (card) => {
    const r = recs[card.dataset.rec];
    const c = await findCopies(r.title, r.author);
    if (!card.isConnected) return;
    if (c.coverUrl) card.querySelector('[data-rec-cover]').innerHTML = badge({ title: r.title, coverUrl: c.coverUrl }, Math.max(0, GENRES.indexOf(r.genre)) % 3, 'rec-badge');
    if (c.isFreeDownload || c.isBorrowable) {
      card.querySelector('[data-rec-access]').innerHTML = `<span class="pill-tag c2">${c.isFreeDownload ? 'Free download' : 'Borrow free'}</span>`;
    }
  });
}

/* ---------- search any book (Genres tab) ---------- */
let genreQuery = '';
const search = { timer: null, ctl: null, state: 'idle', query: '', results: [] };

function searchBodyHtml() {
  if (search.state !== 'done' || search.query !== genreQuery.trim()) return '<div class="center" style="padding:32px"><div class="loader big"></div></div>';
  if (!search.results.length) return `<p class="muted body-l" style="margin:8px 4px">No books found (or you're offline). Try another title, author or topic.</p>`;
  return `<div class="stack">${search.results.map((b, i) => {
    const owned = books.some((x) => x.title.toLowerCase() === b.title.toLowerCase());
    const meta = [b.year, b.pages ? `${b.pages} pages` : '', catalogGenres(b)[0]].filter(Boolean).join(' · ');
    return `<button class="result-row" data-result="${i}" style="animation-delay:${Math.min(i, 10) * 0.03}s">
      ${badge(b, i % 3, 'result-badge')}
      <div class="grow"><div class="title-m clamp2">${esc(b.title)}</div>
        ${b.author ? `<div class="body-m ellipsis">${esc(b.author)}</div>` : ''}
        ${meta ? `<div class="meta">${esc(meta)}</div>` : ''}
        <div class="pills">${owned ? '<span class="pill-tag c1">In your books</span>' : ''}${b.freePdfUrl ? '<span class="pill-tag c2">Free PDF</span>' : b.ebookAccess === 'borrowable' ? '<span class="pill-tag c2">Borrow free</span>' : ''}</div>
      </div>${icon('chevron_right', 'muted')}
    </button>`;
  }).join('')}</div>`;
}

function updateSearchBody() {
  const body = app.querySelector('[data-genres-body]');
  if (!body) return;
  body.innerHTML = searchBodyHtml();
  body.querySelectorAll('[data-result]').forEach((el) => { el.onclick = () => showBookInfo(search.results[el.dataset.result]); });
}

function bindSearch() {
  const input = app.querySelector('[data-search]');
  const setQuery = (value) => {
    const wasSearching = genreQuery.trim().length >= 2;
    genreQuery = value;
    clearTimeout(search.timer);
    search.ctl?.abort();
    const q = value.trim();
    if (q.length < 2) {
      search.state = 'idle';
      if (wasSearching || !value) {
        renderGenres('none');
        const again = app.querySelector('[data-search]');
        again.focus();
        again.setSelectionRange(again.value.length, again.value.length);
      }
      return;
    }
    const trail = app.querySelector('.search-field .trail');
    if (trail && !trail.innerHTML.trim()) {
      trail.innerHTML = `<button class="icon-btn" data-clear-search aria-label="Clear search">${icon('close')}</button>`;
      trail.querySelector('[data-clear-search]').onclick = () => setQuery('');
    }
    search.state = 'loading';
    updateSearchBody();
    search.timer = setTimeout(async () => {
      const ctl = new AbortController();
      search.ctl = ctl;
      try {
        const results = await catalogSearch(q, ctl.signal);
        if (ctl.signal.aborted) return;
        Object.assign(search, { state: 'done', query: q, results });
      } catch {
        if (ctl.signal.aborted) return;
        Object.assign(search, { state: 'done', query: q, results: [] });
      }
      updateSearchBody();
    }, 400);
  };
  input.addEventListener('input', () => setQuery(input.value));
  input.addEventListener('keydown', (e) => { if (e.key === 'Enter') input.blur(); });
  app.querySelector('[data-clear-search]')?.addEventListener('click', () => setQuery(''));
  if (genreQuery.trim().length >= 2) {
    if (search.query === genreQuery.trim() && search.state === 'done') updateSearchBody();
    else setQuery(genreQuery);
  }
}

/** Everything about a book from search, one-tap add, and a free PDF when it's legally available. */
function showBookInfo(initial) {
  let b = initial;
  let addedId = null;
  const body = openSheet('');
  const draw = () => {
    if (!body.isConnected) return;
    const genres = catalogGenres(b);
    const idx = Math.max(0, GENRES.indexOf(genres[0])) % 3;
    const existing = books.find((x) => x.title.toLowerCase() === b.title.toLowerCase());
    const openId = addedId || existing?.id;
    const date = b.releaseDate ? (b.releaseDate.endsWith('-01-01') ? `First published ${b.releaseDate.slice(0, 4)}` : `Released ${prettyDate(b.releaseDate)}`) : null;
    const link = (ic, text, url) => `<a class="btn outline big link-btn" href="${esc(url)}" target="_blank" rel="noopener">${icon(ic)}<span class="grow">${text}</span>${icon('open_in_new')}</a>`;
    body.innerHTML = `
      <div class="row" style="align-items:flex-start">
        ${badge(b, idx, 'info-badge')}
        <div class="grow">
          <h2 class="headline-s">${esc(b.title)}</h2>
          ${b.author ? `<div class="title-m">by ${esc(b.author)}</div>` : ''}
          <div class="facts">
            ${date ? `<div>${icon('event')}${esc(date)}</div>` : ''}
            ${b.pages ? `<div>${icon('menu_book')}${b.pages} pages</div>` : ''}
            ${b.publisher ? `<div>${icon('category')}${esc(b.publisher)}</div>` : ''}
            ${b.rating ? `<div>${icon('bar_chart')}${Number(b.rating).toFixed(1)} / 5 rating</div>` : ''}
          </div>
        </div>
      </div>
      ${genres.length ? `<div class="pills" style="margin-top:14px">${genres.map((g) => `<span class="pill-tag k${idx}" style="background:var(--accent);color:var(--on-accent)">${esc(g)}</span>`).join('')}</div>` : ''}
      <div class="stack" style="margin-top:18px">
        ${openId ? `<button class="btn big filled added" data-open-book="${esc(openId)}">${icon('check_circle')}${addedId ? 'Added · Open book' : 'In your books · Open'}</button>`
          : `<button class="btn big filled" data-add-book>${icon('library_add')}${b.pages ? 'Add to my books' : 'Add to my books…'}</button>`}
        ${b.freePdfUrl ? `<a class="btn big pdf-btn" href="${esc(b.freePdfUrl)}" target="_blank" rel="noopener">${icon('download')}Download free PDF</a>
          <p class="body-s muted" style="margin:-4px 8px 0">Public domain, free and legal to download.</p>`
          : `<div class="note-box">${icon('lock')}<span>No free PDF: this book is still under copyright. You can borrow it or buy it below.</span></div>`}
        ${b.ebookAccess === 'borrowable' ? link('menu_book', 'Borrow free on Open Library', b.workKey ? `https://openlibrary.org${b.workKey}` : `https://openlibrary.org/search?q=${encodeURIComponent(b.title)}`) : ''}
        ${b.freePdfUrl && b.iaId ? link('download', 'Other formats (EPUB, text)', `https://archive.org/details/${b.iaId}`) : ''}
        ${b.googlePreviewUrl ? link('open_in_new', 'Preview on Google Books', b.googlePreviewUrl) : ''}
        ${link('link', 'Open Library page', b.workKey ? `https://openlibrary.org${b.workKey}` : `https://openlibrary.org/search?q=${encodeURIComponent(`${b.title} ${b.author}`)}`)}
      </div>
      ${b.description ? `<h3 class="title" style="margin:20px 0 6px">About this book</h3><p class="body-l description">${esc(b.description)}</p>`
        : b.workKey ? '<div class="center" style="padding:16px"><div class="loader"></div></div>' : ''}`;
    body.querySelector('[data-open-book]')?.addEventListener('click', () => { closeSheet(); go('/book/' + body.querySelector('[data-open-book]').dataset.openBook); });
    body.querySelector('[data-add-book]')?.addEventListener('click', () => {
      if (!b.pages) { closeSheet(); go('/add/' + encodeURIComponent(b.title)); return; }
      const book = { id: uuid(), title: b.title, author: b.author, releaseDate: b.releaseDate, totalPages: b.pages, coverUrl: b.coverUrl, entries: [], createdAt: Date.now() };
      books.push(book);
      saveBooks();
      addedId = book.id;
      toast(`Added “${b.title}” to your books`);
      draw();
      updateSearchBody();
    });
  };
  draw();
  catalogDetails(b).then((full) => { b = full; draw(); });
}

/** Bottom sheet with free, legal places to read, borrow or download a book. */
async function showCopies(title, author) {
  const heading = `<h2 class="headline-s">Get a copy</h2><p class="title-m muted" style="margin:0 0 16px">${esc([title, author].filter(Boolean).join(' · '))}</p>`;
  const body = openSheet(`${heading}<div class="center" style="padding:24px"><div class="loader big"></div></div>`);
  const c = await findCopies(title, author).catch(() => copiesFrom(null, [title, author].filter(Boolean).join(' ')));
  if (!body.isConnected) return;
  const link = (ic, name, sub, url, cls = '') => `<a class="link-row ${cls}" href="${esc(url)}" target="_blank" rel="noopener">
    ${icon(ic)}<div class="grow"><div class="title-m">${name}</div><div class="body-s">${sub}</div></div>${icon('open_in_new')}</a>`;
  body.innerHTML = heading + `<div class="links">
    ${c.isFreeDownload && c.archiveUrl ? link('download', 'Download free', 'Public domain · PDF & EPUB on the Internet Archive', c.archiveUrl, 'c0') : ''}
    ${c.isBorrowable ? link('menu_book', 'Borrow free', 'Read online with a free Open Library account', c.openLibraryUrl, 'c1') : ''}
    ${link('link', 'Open Library', 'Editions, ebooks and libraries near you', c.openLibraryUrl)}
    ${link('download', 'Project Gutenberg', 'Free ebooks of classic, public-domain books', c.gutenbergUrl)}
    ${link('open_in_new', 'Google Books', 'Preview or buy the ebook', c.googleBooksUrl)}
  </div>
  <p class="body-s muted sheet-note">These are free, legal sources. Newer books are usually borrowed from a library or bought; free downloads are for books in the public domain.</p>`;
}

/* ---------- Settings tab ---------- */
const PREVIEW_BOOK = {
  id: 'preview', title: 'The Hobbit', author: 'J.R.R. Tolkien', releaseDate: '1937-09-21', totalPages: 310, coverUrl: null,
  entries: [{ date: addDays(today(), -1), page: 96 }, { date: today(), page: 142 }], createdAt: 0,
};
const preview = () => `<div class="preview"><div class="label muted" style="margin:0 4px 8px">Preview</div>${bookCard(PREVIEW_BOOK, 0, 0, false)}</div>`;
const labelOf = (list, v) => (list.find((x) => x[0] === v) || list[0])[1];

function navRow(ic, title, sub, cls, route) {
  return `<button class="nav-row" data-route="${route}"><span class="nav-icon ${cls}">${icon(ic)}</span>
    <span class="grow"><span class="title block">${title}</span><span class="body-m muted block">${sub}</span></span>${icon('chevron_right', 'muted')}</button>`;
}

function renderSettings(dir) {
  const a = appearance;
  aiPagePick = null;
  app.innerHTML = `<section class="screen ${dir}">
    ${pageHeader('Settings')}
    ${preview()}
    <div class="stack" style="margin-top:14px">
      ${navRow('palette', 'Theme & colors', `${labelOf(THEME_MODES, a.theme)} · ${labelOf(PALETTES, a.palette)}`, 'c0', 'theme')}
      ${navRow('text_fields', 'Text', `${labelOf(TEXT_SIZES, a.textSize)} · ${labelOf(FONTS, a.font)}${a.bold ? ' · Bold' : ''}`, 'c1', 'text')}
      ${navRow('style', 'Style', `${labelOf(CORNERS, a.corners)} corners · ${labelOf(ICON_STYLES, a.icons)} icons`, 'c2', 'style')}
      ${navRow('psychology', 'AI', getConfig() ? esc(getConfig().displayName) : 'Not set up · genres use basic mode', 'k1 nav-soft', 'ai')}
      ${navRow('save', 'Backups', 'Export and restore your books (CSV or JSON)', 'k0 nav-soft', 'backups')}
      <button class="btn outline big" data-reset>${icon('restart_alt')}Reset look to defaults</button>
      <p class="body-s muted">Book Tracker ${VERSION} (web)</p>
    </div>
  </section>`;
  app.querySelectorAll('[data-route]').forEach((el) => { el.onclick = () => go('/settings/' + el.dataset.route); });
  app.querySelector('[data-reset]').onclick = () => { setAppearance(null); renderSettings('none'); toast('Look reset to defaults'); };
}

function choice(value, label, selected, extra = '') {
  return `<button class="choice ${selected ? 'selected' : ''}" data-value="${value}" ${extra}>${label}</button>`;
}

// The provider being viewed on the AI page (it only becomes active once saved).
let aiPagePick = null;

function renderSettingsPage(page, dir) {
  const a = appearance;
  let title, body;
  if (page === 'theme') {
    title = 'Theme & colors';
    body = `${preview()}
      <div class="panel"><h2 class="headline-s">Theme</h2>
        <div class="big-choices" data-group="theme">${THEME_MODES.map(([v, l, ic]) =>
          `<button class="big-choice ${a.theme === v ? 'selected' : ''}" data-value="${v}">${icon(ic)}<span class="label">${l}</span></button>`).join('')}</div></div>
      <div class="panel"><h2 class="headline-s">Colors</h2>
        <div class="swatches" data-group="palette">${PALETTES.map(([v, l]) => {
          const [p, s, t] = paletteSwatch(v, isDark());
          return `<button class="swatch ${a.palette === v ? 'selected' : ''}" data-value="${v}" aria-label="${l}">
            <span class="swatch-circle"><i style="background:${p}"></i><i style="background:${s}"></i><i style="background:${t}"></i>
            ${a.palette === v ? `<span class="swatch-check">${icon('check')}</span>` : ''}</span><span class="label">${l}</span></button>`;
        }).join('')}</div></div>`;
  } else if (page === 'text') {
    title = 'Text';
    body = `<div class="panel k0" style="background:var(--primary-container);color:var(--on-primary-container)">
        <div class="headline">The Hobbit</div><div class="title-m">by J.R.R. Tolkien</div>
        <p class="body-l" style="margin:8px 0 0">In a hole in the ground there lived a hobbit.</p></div>
      <div class="panel"><h2 class="headline-s">Text size</h2>
        <div class="choices" data-group="textSize">${TEXT_SIZES.map(([v, l]) => choice(v, `${icon(a.textSize === v ? 'check' : 'format_size')}${l}`, a.textSize === v)).join('')}</div></div>
      <div class="panel"><h2 class="headline-s">Font</h2>
        <div class="choices three" data-group="font">${FONTS.map(([v, l, stack]) => choice(v, `<span style="font-family:${esc(stack)};font-weight:700">${l}</span>`, a.font === v)).join('')}</div></div>
      <div class="panel row"><div class="grow"><h2 class="headline-s">Bold text</h2><div class="muted body-m">Heavy headings and numbers</div></div>
        <label class="switch"><input type="checkbox" data-bold ${a.bold ? 'checked' : ''}><span></span></label></div>`;
  } else if (page === 'style') {
    title = 'Style';
    loadFonts(['outlined', 'rounded', 'sharp']); // to preview every icon style
    body = `${preview()}
      <div class="panel"><h2 class="headline-s">Corners</h2>
        <div class="big-choices" data-group="corners">${CORNERS.map(([v, l, scale]) =>
          `<button class="big-choice ${a.corners === v ? 'selected' : ''}" data-value="${v}"><span class="corner-demo" style="border-radius:${Math.round(18 * scale)}px"></span><span class="label">${l}</span></button>`).join('')}</div></div>
      <div class="panel"><h2 class="headline-s">Icons</h2>
        <div class="stack" data-group="icons">${ICON_STYLES.map(([v, l]) =>
          `<button class="icon-choice ${a.icons === v ? 'selected' : ''}" data-value="${v}"><span class="title-m grow">${l}</span>
            <span class="icon-sample" data-icons="${v}">${['menu_book', 'settings', 'delete', 'person'].map((n) => icon(n)).join('')}</span></button>`).join('')}</div></div>`;
  } else if (page === 'ai') {
    title = 'AI';
    const active = getConfig();
    const p = providerById(aiPagePick || aiSettings.provider().id);
    aiPagePick = p.id;
    const savedKey = aiSettings.key(p.id);
    const model = aiSettings.model(p.id);
    body = `<div class="panel k1" style="background:var(--secondary-container);color:var(--on-secondary-container)">
        <div class="row" style="gap:10px">${icon('psychology')}<span class="headline-s">${active ? `Using ${esc(active.displayName)}` : 'Not set up'}</span></div>
        <p class="body-l" style="margin:8px 0 0">The Genres tab can use an AI of your choice to sort your library into genres, describe your reading taste and recommend books. It sends your books' titles, authors and reading progress to that service when you tap Analyze.</p></div>
      <div class="panel"><h2 class="headline-s">AI service</h2>
        <div class="choices">${PROVIDERS.map((x) => `<button class="choice ${x.id === p.id ? 'selected' : ''}" data-provider="${x.id}">
          ${x.id === p.id ? icon('check') : aiSettings.key(x.id) ? icon('check_circle') : ''}${esc(x.label)}</button>`).join('')}</div>
        ${p.id === 'openrouter' ? '<p class="muted body-m" style="margin:10px 0 0">OpenRouter gives one key for hundreds of models from many companies.</p>' : ''}
      </div>
      <div class="panel"><h2 class="headline-s">${esc(p.label)} settings</h2>
        ${savedKey ? `<div class="row" style="margin:12px 0"><span style="color:var(--primary)">${icon('check_circle')}</span>
          <span class="title-m grow">Key saved · ${esc(savedKey.length > 12 ? savedKey.slice(0, 6) + '…' + savedKey.slice(-4) : 'saved')}</span>
          <button class="btn text" data-remove-key>Remove</button></div>` : ''}
        ${p.id === 'custom' ? `<div style="margin-top:12px"><label class="field">
          <input data-base-url value="${esc(aiSettings.baseUrl('custom'))}" inputmode="url" autocomplete="off" autocapitalize="off" spellcheck="false" placeholder="https://example.com/v1">
          <span class="lbl">API base URL</span>${icon('link', 'lead')}
        </label><div class="help">Any service with an OpenAI-compatible /chat/completions API</div></div>` : ''}
        <div style="margin-top:12px"><label class="field">
          <input type="password" data-key autocomplete="off" autocapitalize="off" spellcheck="false" placeholder="${esc(p.hint)}">
          <span class="lbl">${savedKey ? 'Replace API key' : `Paste your ${esc(p.label)} API key`}</span>${icon('key', 'lead')}
          <span class="trail"><button class="icon-btn" data-show aria-label="Show key">${icon('visibility')}</button></span>
        </label></div>
        <div style="margin-top:12px"><label class="field">
          <input data-model value="${esc(model)}" autocomplete="off" autocapitalize="off" spellcheck="false" placeholder="${esc(p.model)}">
          <span class="lbl">Model</span>${icon('psychology', 'lead')}
        </label><div class="help">${p.model ? `Default: ${esc(p.model)}. Any model name from ${esc(p.label)} works.` : 'The model name your service uses'}</div></div>
        <button class="btn big filled" data-save-ai disabled style="margin-top:8px">Save and use ${esc(p.label)}</button>
        ${p.keyUrl ? `<a class="btn outline big" style="margin-top:8px" href="${esc(p.keyUrl)}" target="_blank" rel="noopener">${icon('open_in_new')}Get a ${esc(p.label)} API key</a>` : ''}
      </div>
      <div class="panel"><h2 class="headline-s">Good to know</h2>
        <div class="note">${icon('lock')}<span>Keys are stored only in this browser on this device, and each is sent only to its own service.</span></div>
        <div class="note">${icon('bar_chart')}<span>Usage is billed by the service you choose (some, like Gemini and Groq, have free tiers). It only runs when you tap Analyze.</span></div>
        <div class="note">${icon('error')}<span>A few services don't accept requests from web apps. If one can't be reached here, try OpenRouter or the Android app.</span></div>
        <div class="note">${icon('category')}<span>Without a key, Genres still works in basic mode using Open Library's subject tags.</span></div>
      </div>`;
  } else if (page === 'backups') {
    title = 'Backups';
    body = `<div class="panel"><h2 class="headline-s">Save a copy</h2>
        <p class="muted body-m">Your books live only in this browser. Save a backup now and then, especially before clearing Safari's data.</p>
        <button class="btn big filled" data-csv>${icon('download')}Export CSV (spreadsheet)</button>
        <button class="btn big" data-json style="margin-top:8px">${icon('save')}Export full backup (JSON)</button></div>
      <div class="panel"><h2 class="headline-s">Restore</h2>
        <p class="muted body-m">Restore a CSV or JSON backup from this web app, or a CSV backup from the Android app.</p>
        <button class="btn big outline" data-restore>${icon('upload')}Restore from a backup</button></div>
      <p class="body-s muted" style="margin:0 4px">Automatic scheduled backups are Android-only: browsers can't run tasks in the background or save into folders on their own.</p>`;
  } else {
    location.replace('#/settings');
    return;
  }

  app.innerHTML = `<section class="screen ${dir}">${subPageBar(title)}<div class="stack-l">${body}</div></section>`;
  app.querySelector('[data-back]').onclick = () => back('/settings');

  const rerender = () => renderSettingsPage(page, 'none');
  app.querySelectorAll('[data-group]').forEach((group) => {
    group.querySelectorAll('[data-value]').forEach((btn) => {
      btn.onclick = () => { setAppearance({ [group.dataset.group]: btn.dataset.value }); rerender(); };
    });
  });
  const bold = app.querySelector('[data-bold]');
  if (bold) bold.onchange = () => { setAppearance({ bold: bold.checked }); };

  if (page === 'ai') {
    const p = providerById(aiPagePick);
    const keyIn = app.querySelector('[data-key]'), modelIn = app.querySelector('[data-model]'), urlIn = app.querySelector('[data-base-url]');
    const save = app.querySelector('[data-save-ai]');
    const check = () => {
      const hasKey = keyIn.value.trim().length >= 8 || !!aiSettings.key(p.id);
      save.disabled = !(hasKey && modelIn.value.trim() && (!urlIn || /^https?:\/\//.test(urlIn.value.trim())));
    };
    [keyIn, modelIn, urlIn].forEach((el) => el?.addEventListener('input', check));
    check();
    app.querySelectorAll('[data-provider]').forEach((btn) => {
      btn.onclick = () => {
        aiPagePick = btn.dataset.provider;
        // Switch straight away if this service is already set up.
        if (aiSettings.key(aiPagePick)) aiSettings.setProvider(aiPagePick);
        rerender();
      };
    });
    app.querySelector('[data-show]').onclick = (e) => {
      const show = keyIn.type === 'password';
      keyIn.type = show ? 'text' : 'password';
      e.currentTarget.innerHTML = icon(show ? 'visibility_off' : 'visibility');
    };
    save.onclick = () => {
      if (keyIn.value.trim()) aiSettings.setKey(p.id, keyIn.value);
      aiSettings.setModel(p.id, modelIn.value.trim() || p.model);
      if (urlIn) aiSettings.setCustomUrl(urlIn.value);
      aiSettings.setProvider(p.id);
      toast('Saved. Open Genres and tap Analyze.');
      rerender();
    };
    app.querySelector('[data-remove-key]')?.addEventListener('click', () => { aiSettings.setKey(p.id, null); toast(`${p.label} key removed`); rerender(); });
  }
  if (page === 'backups') {
    app.querySelector('[data-csv]').onclick = () => download(booksToCsv(books), `booktracker-backup-${today()}.csv`, 'text/csv');
    app.querySelector('[data-json]').onclick = () => download(JSON.stringify({ version: 1, books }, null, 2), `booktracker-backup-${today()}.json`, 'application/json');
    app.querySelector('[data-restore]').onclick = () => {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = '.csv,.json,text/csv,application/json,text/plain';
      input.onchange = async () => {
        try {
          const restored = booksFromBackup(await input.files[0].text());
          const ok = await confirmDialog({ title: 'Restore this backup?', text: `Your current books will be replaced with the ${restored.length} book${restored.length === 1 ? '' : 's'} in the backup.`, confirm: 'Restore' });
          if (!ok) return;
          replaceBooks(restored);
          toast(`Restored ${restored.length} books`);
        } catch { toast("That doesn't look like a Book Tracker backup"); }
      };
      input.click();
    };
  }
}

/* ---------- start ---------- */
applyAppearance();
if ('serviceWorker' in navigator && location.protocol === 'https:') {
  navigator.serviceWorker.register('sw.js').catch(() => {});
}
render();
