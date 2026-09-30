// Book lookups: title autofill (Open Library + Google Books), genres from subjects,
// and legal places to read a book online. None of these need an API key.
import { pad } from './util.js';
import { GENRES, SUBJECT_KEYWORDS } from './ai.js';

/* ---------- book lookup (Open Library + Google Books, no key) ---------- */
export async function getJSON(url, signal) {
  const res = await fetch(url, { signal });
  if (!res.ok) throw new Error('HTTP ' + res.status);
  return res.json();
}

const MONTHS = { jan: 1, feb: 2, mar: 3, apr: 4, may: 5, jun: 6, jul: 7, aug: 8, sep: 9, sept: 9, oct: 10, nov: 11, dec: 12 };
/** Parses "May 04, 2021", "4 May 2021", "2021-05-04" etc. into ISO, or null. */
export function parseLooseDate(s) {
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

export async function searchBooks(q, signal) {
  const [ol, g] = await Promise.allSettled([searchOpenLibrary(q, signal), searchGoogle(q, signal)]);
  return mergeResults(ol.value || [], g.value || []).slice(0, 8);
}


/* ---------- basic (no AI) genres and recommendations ---------- */

const GENRE_SUBJECTS = {
  'Fantasy': 'fantasy', 'Science Fiction': 'science_fiction', 'Mystery & Thriller': 'mystery_and_detective_stories',
  'Horror': 'horror', 'Romance': 'romance', 'Historical Fiction': 'historical_fiction', 'Literary Fiction': 'literary_fiction',
  'Classics': 'classic_literature', 'Young Adult': 'young_adult_fiction', "Children's": 'juvenile_fiction',
  'Adventure': 'adventure_stories', 'Comics & Graphic Novels': 'comics_&_graphic_novels', 'Poetry': 'poetry',
  'Biography & Memoir': 'biography', 'History': 'history', 'Science & Nature': 'science', 'Self-Help': 'self-help',
  'Business & Money': 'business', 'Philosophy & Religion': 'philosophy', 'Travel': 'travel',
};

/** Picks the one or two genres that the most subject tags point to. */
export function genresFromSubjects(subjects) {
  const counts = new Map();
  for (const raw of subjects.slice(0, 40)) {
    const s = String(raw).toLowerCase();
    const hit = SUBJECT_KEYWORDS.find(([keywords]) => keywords.some((k) => s.includes(k)));
    if (hit) counts.set(hit[1], (counts.get(hit[1]) || 0) + 1);
  }
  // "Literary Fiction" is a catch-all for any fiction tag; prefer a more specific genre.
  const ranked = [...counts.entries()]
    .sort((a, b) => ((b[0] === 'Literary Fiction' ? 0 : b[1]) - (a[0] === 'Literary Fiction' ? 0 : a[1])) || (b[1] - a[1]))
    .map(([g]) => g);
  return ranked.length ? ranked.slice(0, 2) : ['Other'];
}

async function genresFor(book) {
  const url = `https://openlibrary.org/search.json?title=${encodeURIComponent(book.title)}` +
    (book.author ? `&author=${encodeURIComponent(book.author)}` : '') + '&limit=1&fields=subject';
  const doc = (await getJSON(url)).docs?.[0];
  return doc?.subject ? genresFromSubjects(doc.subject) : ['Other'];
}

async function popularIn(genre) {
  const subject = GENRE_SUBJECTS[genre];
  if (!subject) return [];
  const data = await getJSON(`https://openlibrary.org/search.json?subject=${encodeURIComponent(subject)}&sort=readinglog&limit=20&fields=title,author_name,first_publish_year`);
  return (data.docs || []).filter((d) => d.title).map((d) => ({ title: d.title, author: d.author_name?.[0] || '', year: d.first_publish_year || null }));
}

const titleKey = (t) => String(t).toLowerCase().replace(/[^\p{L}\p{N}]/gu, '');

/** Genres from Open Library subject tags; recommendations are popular books in the top genres. */
export async function analyzeBasic(list, signature, currentPage) {
  const bookGenres = {};
  // A few at a time, to be polite to Open Library.
  for (let i = 0; i < list.length; i += 4) {
    await Promise.all(list.slice(i, i + 4).map(async (b) => {
      bookGenres[b.id] = await genresFor(b).catch(() => ['Other']);
    }));
  }
  const weight = new Map();
  list.forEach((b) => { const g = bookGenres[b.id][0]; weight.set(g, (weight.get(g) || 0) + Math.max(1, currentPage(b))); });
  let top = [...weight.entries()].filter(([g]) => g !== 'Other').sort((a, b) => b[1] - a[1]).map(([g]) => g).slice(0, 2);
  if (!top.length) top = ['Literary Fiction'];

  const owned = new Set(list.map((b) => titleKey(b.title)));
  const recommendations = [];
  for (const [genre, candidates] of await Promise.all(top.map(async (g) => [g, await popularIn(g).catch(() => [])]))) {
    const count = Object.values(bookGenres).filter((g) => g[0] === genre).length;
    candidates.filter((c) => !owned.has(titleKey(c.title)) && owned.add(titleKey(c.title))).slice(0, 4).forEach((c) => {
      recommendations.push({
        ...c, genre,
        reason: count ? `Popular with ${genre} readers, and you have ${count} ${genre} book${count === 1 ? '' : 's'} in your library.` : `A popular ${genre} pick.`,
      });
    });
  }
  return { source: 'BASIC', bookGenres, summary: null, recommendations, createdAt: Date.now(), librarySignature: signature };
}

/* ---------- where to read a book online (legal sources only) ---------- */

export function copiesFrom(doc, query) {
  const workKey = doc?.key?.startsWith('/works/') ? doc.key : null;
  const access = doc?.ebook_access || null;
  const ia = doc?.ia?.[0] || null;
  const q = encodeURIComponent(query).replace(/%20/g, '+');
  return {
    openLibraryUrl: workKey ? `https://openlibrary.org${workKey}` : `https://openlibrary.org/search?q=${q}`,
    access,
    archiveUrl: ia && (access === 'public' || access === 'borrowable') ? `https://archive.org/details/${ia}` : null,
    gutenbergUrl: `https://www.gutenberg.org/ebooks/search/?query=${q}`,
    googleBooksUrl: `https://www.google.com/books?q=${q}`,
    coverUrl: doc?.cover_i ? `https://covers.openlibrary.org/b/id/${doc.cover_i}-M.jpg?default=false` : null,
    pages: doc?.number_of_pages_median || null,
    isFreeDownload: access === 'public',
    isBorrowable: access === 'borrowable',
  };
}

const copiesCache = new Map();
export async function findCopies(title, author) {
  const query = [title, author].filter(Boolean).join(' ');
  if (copiesCache.has(query)) return copiesCache.get(query);
  let doc = null;
  try {
    doc = (await getJSON(`https://openlibrary.org/search.json?q=${encodeURIComponent(query)}&limit=1&fields=key,ebook_access,ia,cover_i,number_of_pages_median`)).docs?.[0] || null;
  } catch { /* fall back to search links */ }
  const copies = copiesFrom(doc, query);
  if (doc) copiesCache.set(query, copies);
  return copies;
}

/* ---------- book search for the Genres tab (all details + legal free PDFs) ---------- */

const stripHtml = (s) => String(s).replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]+>/g, '').trim();

function catalogFromOpenLibrary(data) {
  return (data.docs || []).filter((d) => d.title).map((d) => {
    const year = d.first_publish_year || null;
    const ia = d.ia?.[0] || null;
    const access = d.ebook_access || null;
    const exact = (d.publish_date || []).map(parseLooseDate).filter((x) => x && Number(x.slice(0, 4)) === year).sort()[0];
    return {
      title: d.title, author: (d.author_name || []).join(', '), year,
      releaseDate: exact || (year ? `${year}-01-01` : null),
      pages: d.number_of_pages_median || null,
      coverUrl: d.cover_i ? `https://covers.openlibrary.org/b/id/${d.cover_i}-L.jpg?default=false` : null,
      publisher: d.publisher?.[0] || null, rating: d.ratings_average || null,
      subjects: (d.subject || []).slice(0, 40), description: null,
      workKey: d.key?.startsWith('/works/') ? d.key : null, ebookAccess: access, iaId: ia,
      // Public-domain scans on the Internet Archive can be downloaded as PDF.
      freePdfUrl: access === 'public' && ia ? `https://archive.org/download/${ia}/${ia}.pdf` : null,
      googlePreviewUrl: null,
    };
  });
}

function catalogFromGoogle(data) {
  return (data.items || []).filter((it) => it.volumeInfo?.title).map((it) => {
    const v = it.volumeInfo, a = it.accessInfo || {};
    const date = v.publishedDate ? (/^\d{4}$/.test(v.publishedDate) ? `${v.publishedDate}-01-01` : /^\d{4}-\d{2}$/.test(v.publishedDate) ? `${v.publishedDate}-01` : v.publishedDate.slice(0, 10)) : null;
    return {
      title: v.title, author: (v.authors || []).join(', '), year: date ? Number(date.slice(0, 4)) : null, releaseDate: date,
      pages: v.pageCount || null,
      coverUrl: v.imageLinks?.thumbnail ? v.imageLinks.thumbnail.replace('http://', 'https://').replace('&edge=curl', '') : null,
      publisher: v.publisher || null, rating: v.averageRating || null, subjects: v.categories || [],
      description: v.description ? stripHtml(v.description) : null,
      workKey: null, ebookAccess: null, iaId: null,
      // Google only offers a PDF download link for free, public-domain books.
      freePdfUrl: a.publicDomain && a.pdf?.isAvailable && /^https?:/.test(a.pdf.downloadLink || '') ? a.pdf.downloadLink.replace('http://', 'https://') : null,
      googlePreviewUrl: v.previewLink ? v.previewLink.replace('http://', 'https://') : null,
    };
  });
}

export function mergeCatalog(primary, secondary) {
  const out = new Map();
  for (const b of [...primary, ...secondary]) {
    const key = norm(b.title) + '|' + norm(b.author.split(',')[0]);
    const e = out.get(key);
    if (!e) { out.set(key, { ...b }); continue; }
    const yearOnly = e.releaseDate?.endsWith('-01-01');
    if (!e.releaseDate || (yearOnly && b.releaseDate && b.releaseDate.slice(0, 4) === e.releaseDate.slice(0, 4) && !b.releaseDate.endsWith('-01-01'))) e.releaseDate = b.releaseDate;
    for (const f of ['pages', 'coverUrl', 'publisher', 'rating', 'description', 'freePdfUrl', 'googlePreviewUrl']) e[f] ??= b[f];
    e.subjects = [...new Set([...e.subjects, ...b.subjects])];
  }
  return [...out.values()];
}

export async function catalogSearch(query, signal) {
  const q = encodeURIComponent(query.trim());
  const [ol, g] = await Promise.allSettled([
    getJSON(`https://openlibrary.org/search.json?limit=15&fields=key,title,author_name,first_publish_year,number_of_pages_median,cover_i,publisher,ratings_average,subject,ebook_access,ia,publish_date&q=${q}`, signal),
    getJSON(`https://www.googleapis.com/books/v1/volumes?maxResults=15&printType=books&fields=items(id,volumeInfo(title,authors,publishedDate,pageCount,publisher,averageRating,categories,description,imageLinks/thumbnail,previewLink),accessInfo(publicDomain,pdf(isAvailable,downloadLink)))&q=${q}`, signal),
  ]);
  if (ol.status === 'rejected' && g.status === 'rejected' && signal?.aborted) throw new DOMException('aborted', 'AbortError');
  return mergeCatalog(ol.status === 'fulfilled' ? catalogFromOpenLibrary(ol.value) : [], g.status === 'fulfilled' ? catalogFromGoogle(g.value) : []).slice(0, 20);
}

/** Fills in the description from Open Library when the search didn't include one. */
export async function catalogDetails(book) {
  if (book.description || !book.workKey) return book;
  try {
    const d = (await getJSON(`https://openlibrary.org${book.workKey}.json`)).description;
    const text = typeof d === 'string' ? d : d?.value;
    if (text) return { ...book, description: text.split('\n----------')[0].replace(/\[([^\]]+)]\([^)]*\)/g, '$1').trim() };
  } catch { /* keep what we have */ }
  return book;
}

export const catalogGenres = (book) => genresFromSubjects(book.subjects).filter((g) => g !== 'Other');
