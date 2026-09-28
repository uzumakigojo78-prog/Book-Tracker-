// Books, the daily reading log, and CSV/JSON backups (same CSV layout as the Android app).
import { addDays, clamp, store, today, toast } from './util.js';

const BOOKS_KEY = 'booktracker.v1';

export let books = loadBooks();

function loadBooks() {
  const data = store.get(BOOKS_KEY);
  return Array.isArray(data?.books) ? data.books : [];
}

export function saveBooks() {
  if (!store.set(BOOKS_KEY, { version: 1, books })) toast("Couldn't save. Is private browsing on?");
}

export function replaceBooks(list) {
  books = list;
  saveBooks();
}

export const sortedEntries = (b) => [...b.entries].sort((x, y) => x.date.localeCompare(y.date));
export const currentPage = (b) => sortedEntries(b).at(-1)?.page ?? 0;
export const progress = (b) => (b.totalPages > 0 ? clamp(currentPage(b) / b.totalPages, 0, 1) : 0);
export const isFinished = (b) => b.totalPages > 0 && currentPage(b) >= b.totalPages;

/** Pages read on each logged day, newest first. */
export function dailyPages(b) {
  let prev = 0;
  return sortedEntries(b).map((e) => {
    const read = Math.max(0, e.page - prev);
    prev = e.page;
    return { date: e.date, page: e.page, read };
  }).reverse();
}

export const pagesToday = (b) => dailyPages(b).find((d) => d.date === today())?.read ?? 0;

/** Consecutive days (ending today or yesterday) with pages read, for a set of dates. */
export function streakOf(readDates) {
  let day = today();
  if (!readDates.has(day)) day = addDays(day, -1);
  let n = 0;
  while (readDates.has(day)) { n++; day = addDays(day, -1); }
  return n;
}
export const streak = (b) => streakOf(new Set(dailyPages(b).filter((d) => d.read > 0).map((d) => d.date)));

export const colorIndex = (b) => Math.max(0, books.indexOf(b)) % 3;
export const findBook = (id) => books.find((b) => b.id === id);

export function logPage(b, date, page) {
  b.entries = b.entries.filter((e) => e.date !== date);
  b.entries.push({ date, page: clamp(page, 0, b.totalPages) });
  saveBooks();
}

/* ---------- CSV ---------- */

const CSV_HEADER = ['book_id', 'title', 'author', 'release_date', 'total_pages', 'cover_url', 'added_at', 'log_date', 'page_reached', 'pages_read'];

export function booksToCsv(list) {
  const cell = (v) => { const s = String(v ?? ''); return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s; };
  const rows = [CSV_HEADER];
  for (const b of list) {
    const base = [b.id, b.title, b.author, b.releaseDate, b.totalPages, b.coverUrl, b.createdAt];
    const days = dailyPages(b).reverse();
    if (days.length === 0) rows.push([...base, '', '', '']);
    days.forEach((d) => rows.push([...base, d.date, d.page, d.read]));
  }
  return '﻿' + rows.map((r) => r.map(cell).join(',')).join('\r\n') + '\r\n';
}

/** RFC 4180 parsing. */
function parseCsv(text) {
  const rows = [];
  let row = [], cell = '', quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"') { if (text[i + 1] === '"') { cell += '"'; i++; } else quoted = false; } else cell += c;
    } else if (c === '"') quoted = true;
    else if (c === ',') { row.push(cell); cell = ''; }
    else if (c === '\n') { row.push(cell); rows.push(row); row = []; cell = ''; }
    else if (c !== '\r') cell += c;
  }
  if (cell || row.length) { row.push(cell); rows.push(row); }
  return rows.filter((r) => r.some((v) => v.trim()));
}

/** Reads a Book Tracker CSV backup (from this web app or the Android app). */
export function booksFromCsv(text) {
  const rows = parseCsv(text.replace(/^﻿/, ''));
  if (!rows.length || CSV_HEADER.some((h, i) => (rows[0][i] || '').trim() !== h)) throw new Error('Not a Book Tracker backup');
  const out = new Map();
  for (const r of rows.slice(1)) {
    const get = (name) => (r[CSV_HEADER.indexOf(name)] ?? '').trim();
    const id = get('book_id');
    if (!id) continue;
    if (!out.has(id)) {
      out.set(id, {
        id, title: get('title'), author: get('author'), releaseDate: get('release_date') || null,
        totalPages: Number(get('total_pages')) || 0, coverUrl: get('cover_url') || null,
        createdAt: Number(get('added_at')) || 0, entries: [],
      });
    }
    if (get('log_date')) out.get(id).entries.push({ date: get('log_date'), page: Number(get('page_reached')) || 0 });
  }
  return [...out.values()];
}

/** Reads a JSON backup or a CSV backup. */
export function booksFromBackup(text) {
  const trimmed = text.replace(/^﻿/, '').trimStart();
  if (trimmed.startsWith('{')) {
    const data = JSON.parse(trimmed);
    if (!Array.isArray(data?.books)) throw new Error('bad file');
    return data.books.map((b) => ({ entries: [], createdAt: 0, coverUrl: null, releaseDate: null, author: '', ...b }));
  }
  return booksFromCsv(text);
}
