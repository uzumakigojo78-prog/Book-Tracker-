// Genres & recommendations with Claude (the reader's own Anthropic API key), mirroring the Android app.
import { addDays, store, today } from './util.js';

export const MODEL = 'claude-opus-5';
export const MODEL_NAME = 'Claude Opus 5';
const KEY_STORE = 'booktracker.ai.key';

export const getApiKey = () => store.get(KEY_STORE) || null;
export const setApiKey = (key) => (key ? store.set(KEY_STORE, key.trim()) : store.remove(KEY_STORE));

/** The fixed set of genres the app groups books into, so every mode sorts the same way. */
export const GENRES = [
  'Fantasy', 'Science Fiction', 'Mystery & Thriller', 'Horror', 'Romance', 'Historical Fiction',
  'Literary Fiction', 'Classics', 'Young Adult', "Children's", 'Adventure', 'Comics & Graphic Novels',
  'Poetry', 'Biography & Memoir', 'History', 'Science & Nature', 'Self-Help', 'Business & Money',
  'Philosophy & Religion', 'Travel', 'Other',
];

/** Keywords (in Open Library subjects or loose labels) for each genre, most specific first. */
export const SUBJECT_KEYWORDS = [
  [['graphic novel', 'comic', 'manga'], 'Comics & Graphic Novels'],
  [['young adult', 'teen', 'juvenile fiction'], 'Young Adult'],
  [['children', 'picture book', 'juvenile'], "Children's"],
  [['science fiction', 'sci-fi', 'dystopia', 'space opera', 'cyberpunk'], 'Science Fiction'],
  [['fantasy', 'magic', 'dragons', 'wizards'], 'Fantasy'],
  [['horror', 'ghost', 'vampire', 'zombie'], 'Horror'],
  [['mystery', 'thriller', 'detective', 'crime', 'suspense', 'espionage'], 'Mystery & Thriller'],
  [['romance', 'love stories'], 'Romance'],
  [['historical fiction'], 'Historical Fiction'],
  [['adventure', 'survival', 'sea stories'], 'Adventure'],
  [['poetry', 'poems'], 'Poetry'],
  [['biography', 'autobiography', 'memoir'], 'Biography & Memoir'],
  [['self-help', 'self help', 'personal development', 'success', 'habits', 'happiness'], 'Self-Help'],
  [['business', 'economics', 'finance', 'management', 'investing', 'money'], 'Business & Money'],
  [['philosophy', 'religion', 'spirituality', 'theology', 'christianity', 'buddhism'], 'Philosophy & Religion'],
  [['travel', 'voyages'], 'Travel'],
  [['science', 'nature', 'physics', 'biology', 'astronomy', 'mathematics', 'psychology'], 'Science & Nature'],
  [['history', 'war', 'civilization'], 'History'],
  [['classic', 'classical literature'], 'Classics'],
  [['fiction', 'novel', 'literature'], 'Literary Fiction'],
];

export function normalizeGenre(raw) {
  const s = String(raw || '').trim().toLowerCase();
  const exact = GENRES.find((g) => g.toLowerCase() === s);
  if (exact) return exact;
  return SUBJECT_KEYWORDS.find(([keywords]) => keywords.some((k) => s.includes(k)))?.[1] || 'Other';
}

/** Library fingerprint: changes when books are added, removed, renamed or finished. */
export const librarySignature = (list, isFinished) =>
  [...list].sort((a, b) => a.id.localeCompare(b.id)).map((b) => `${b.id}|${b.title}|${b.author}|${isFinished(b)}`).join('\n');

const SYSTEM_PROMPT = `You organize a reader's personal library into genres and suggest what they might enjoy reading next. You'll receive their books as JSON, including how far they've read and how much they read recently.

Give each book one or two genres from this list, main genre first: ${GENRES.join(', ')}. Use what you know about the actual book; if you don't recognize it, infer from the title and author, and use "Other" only as a last resort.

Write a two-sentence summary of their reading taste, addressed to them ("You…"). Books they finished or are actively reading say more about their taste than ones they barely started.

Recommend 8 real, published books that are not already in their library: mostly from the genres they read most, plus one or two stretch picks they'd plausibly enjoy. Give each a one-sentence reason that mentions a book from their library. Only recommend books you are confident exist, with the correct author.

Reply with only a JSON object and no other text, in this shape:
{"books":[{"id":"<book id>","genres":["<genre>"]}],"summary":"<two sentences>","recommendations":[{"title":"<title>","author":"<author>","year":<first published year or null>,"genre":"<genre from the list>","reason":"<one sentence>"}]}`;

export function libraryJson(list, h) {
  const since = addDays(today(), -30);
  return JSON.stringify({
    books: list.map((b) => ({
      id: b.id,
      title: b.title,
      author: b.author || null,
      release_year: b.releaseDate ? Number(b.releaseDate.slice(0, 4)) : null,
      total_pages: b.totalPages,
      current_page: h.currentPage(b),
      status: h.isFinished(b) ? 'finished' : h.currentPage(b) > 0 ? 'reading' : 'not started',
      pages_read_last_30_days: h.dailyPages(b).filter((d) => d.date > since).reduce((n, d) => n + d.read, 0),
    })),
  });
}

const titleKey = (t) => String(t).toLowerCase().replace(/[^\p{L}\p{N}]/gu, '');

/** Parses Claude's JSON reply, tolerating stray text around it and unknown genres. */
export function parseAnalysis(text, list, signature) {
  const start = text.indexOf('{'), end = text.lastIndexOf('}');
  let o;
  try { o = JSON.parse(text.slice(start, end + 1)); } catch { throw new Error("Claude's answer couldn't be read. Try again."); }
  if (start < 0 || !o || typeof o !== 'object') throw new Error("Claude's answer couldn't be read. Try again.");

  const assigned = {};
  (o.books || []).forEach((b) => {
    const g = [...new Set((b?.genres || []).map(normalizeGenre))].slice(0, 2);
    if (b?.id && g.length) assigned[b.id] = g;
  });
  const bookGenres = Object.fromEntries(list.map((b) => [b.id, assigned[b.id] || ['Other']]));
  const owned = new Set(list.map((b) => titleKey(b.title)));
  const recommendations = (o.recommendations || [])
    .filter((r) => r?.title && !owned.has(titleKey(r.title)))
    .map((r) => ({
      title: String(r.title).trim(),
      author: String(r.author || '').trim(),
      year: Number(r.year) > 0 ? Number(r.year) : null,
      genre: normalizeGenre(r.genre),
      reason: String(r.reason || '').trim(),
    }));
  return { source: 'AI', bookGenres, summary: (o.summary || '').trim() || null, recommendations, createdAt: Date.now(), librarySignature: signature };
}

/** Asks Claude to sort the library into genres and recommend books. */
export async function analyzeWithClaude(apiKey, list, signature, helpers) {
  const { default: Anthropic } = await import('../vendor/anthropic-sdk-0.128.0.mjs');
  // The key is the reader's own and stays on their device; it's sent only to Anthropic.
  const client = new Anthropic({ apiKey, dangerouslyAllowBrowser: true, baseURL: analyzeWithClaude.baseURL });
  let response;
  try {
    response = await client.beta.messages.create({
      model: MODEL,
      max_tokens: 16000,
      system: SYSTEM_PROMPT,
      messages: [{ role: 'user', content: libraryJson(list, helpers) }],
      // If a request is declined, let the API retry it on a suitable fallback model.
      betas: ['server-side-fallback-2026-07-01'],
      fallbacks: 'default',
    });
  } catch (e) {
    if (e instanceof Anthropic.AuthenticationError) throw new Error('Your Anthropic API key was rejected. Check it in Settings → AI.');
    if (e instanceof Anthropic.PermissionDeniedError) throw new Error("This API key doesn't have access to Claude. Check it in Settings → AI.");
    if (e instanceof Anthropic.RateLimitError) throw new Error('Claude is busy right now (rate limit). Try again in a minute.');
    if (e instanceof Anthropic.APIConnectionError) throw new Error("Couldn't reach Claude. Check your internet connection.");
    if (e instanceof Anthropic.APIError) throw new Error(`Claude returned an error (${e.status}). Try again later.`);
    throw e;
  }
  if (response.stop_reason === 'refusal') throw new Error('Claude declined to answer this time. Try again later.');
  const text = response.content.filter((b) => b.type === 'text').map((b) => b.text).join('');
  return parseAnalysis(text, list, signature);
}
/** Overridable for tests. */
analyzeWithClaude.baseURL = undefined;
