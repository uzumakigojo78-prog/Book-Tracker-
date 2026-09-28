// Genres & recommendations with the reader's own AI key (Claude, Gemini, Grok, Kimi, …), mirroring the Android app.
import { addDays, store, today } from './util.js';

export const MODEL = 'claude-opus-5';
export const MODEL_NAME = 'Claude Opus 5';

/**
 * AI services the Genres tab can use. Claude goes through Anthropic's SDK; the rest
 * speak the OpenAI-compatible chat completions API, so any provider offering that
 * API works via "custom".
 */
export const PROVIDERS = [
  { id: 'anthropic', label: 'Claude', baseUrl: 'https://api.anthropic.com', model: MODEL, keyUrl: 'https://console.anthropic.com/settings/keys', hint: 'sk-ant-…' },
  { id: 'gemini', label: 'Gemini', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', model: 'gemini-2.5-flash', keyUrl: 'https://aistudio.google.com/apikey', hint: 'AIza…' },
  { id: 'grok', label: 'Grok', baseUrl: 'https://api.x.ai/v1', model: 'grok-4', keyUrl: 'https://console.x.ai', hint: 'xai-…' },
  { id: 'kimi', label: 'Kimi', baseUrl: 'https://api.moonshot.ai/v1', model: 'kimi-k2-0905-preview', keyUrl: 'https://platform.moonshot.ai/console/api-keys', hint: 'sk-…' },
  { id: 'openai', label: 'ChatGPT', baseUrl: 'https://api.openai.com/v1', model: 'gpt-5-mini', keyUrl: 'https://platform.openai.com/api-keys', hint: 'sk-…' },
  { id: 'deepseek', label: 'DeepSeek', baseUrl: 'https://api.deepseek.com/v1', model: 'deepseek-chat', keyUrl: 'https://platform.deepseek.com/api_keys', hint: 'sk-…' },
  { id: 'mistral', label: 'Mistral', baseUrl: 'https://api.mistral.ai/v1', model: 'mistral-large-latest', keyUrl: 'https://console.mistral.ai/api-keys', hint: '' },
  { id: 'openrouter', label: 'OpenRouter', baseUrl: 'https://openrouter.ai/api/v1', model: 'openrouter/auto', keyUrl: 'https://openrouter.ai/keys', hint: 'sk-or-…' },
  { id: 'groq', label: 'Groq', baseUrl: 'https://api.groq.com/openai/v1', model: 'llama-3.3-70b-versatile', keyUrl: 'https://console.groq.com/keys', hint: 'gsk_…' },
  { id: 'custom', label: 'Other', baseUrl: '', model: '', keyUrl: null, hint: '' },
];
export const providerById = (id) => PROVIDERS.find((p) => p.id === id) || PROVIDERS[0];

/* ---------- settings (kept only in this browser; each provider keeps its own key and model) ---------- */

const AI_STORE = 'booktracker.ai';
function readAi() {
  const s = store.get(AI_STORE) || { provider: 'anthropic', keys: {}, models: {}, customUrl: '' };
  // Keys saved before other providers were added.
  const legacy = store.get('booktracker.ai.key');
  if (legacy && !s.keys.anthropic) { s.keys.anthropic = legacy; store.set(AI_STORE, s); store.remove('booktracker.ai.key'); }
  return s;
}
const writeAi = (s) => store.set(AI_STORE, s);

export const aiSettings = {
  provider: () => providerById(readAi().provider),
  key: (id) => readAi().keys[id] || null,
  model: (id) => readAi().models[id] || providerById(id).model,
  baseUrl: (id) => (id === 'custom' ? readAi().customUrl || '' : providerById(id).baseUrl),
  setProvider(id) { const s = readAi(); s.provider = id; writeAi(s); },
  setKey(id, key) { const s = readAi(); if (key) s.keys[id] = key.trim(); else delete s.keys[id]; writeAi(s); },
  setModel(id, model) { const s = readAi(); s.models[id] = model.trim(); writeAi(s); },
  setCustomUrl(url) { const s = readAi(); s.customUrl = url.trim().replace(/\/+$/, ''); writeAi(s); },
};

export const displayName = (p, model) => (p.id === 'anthropic' && model === MODEL ? MODEL_NAME : `${p.label} · ${model}`);

/** The ready-to-use AI configuration, or null when none is set up (basic mode). */
export function getConfig() {
  const p = aiSettings.provider();
  const apiKey = aiSettings.key(p.id), model = aiSettings.model(p.id), baseUrl = aiSettings.baseUrl(p.id);
  if (!apiKey || !model || !baseUrl) return null;
  return { provider: p, apiKey, model, baseUrl, displayName: displayName(p, model) };
}

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

/** Parses the AI's JSON reply, tolerating stray text around it and unknown genres. */
export function parseAnalysis(text, list, signature, madeBy = null) {
  const start = text.indexOf('{'), end = text.lastIndexOf('}');
  let o;
  try { o = JSON.parse(text.slice(start, end + 1)); } catch { throw new Error("The AI's answer couldn't be read. Try again."); }
  if (start < 0 || !o || typeof o !== 'object') throw new Error("The AI's answer couldn't be read. Try again.");

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
  return { source: 'AI', bookGenres, summary: (o.summary || '').trim() || null, recommendations, createdAt: Date.now(), librarySignature: signature, madeBy };
}

/** Asks Claude to sort the library into genres and recommend books. */
async function analyzeWithClaude({ apiKey, model, displayName: madeBy }, list, signature, helpers) {
  const { default: Anthropic } = await import('../vendor/anthropic-sdk-0.128.0.mjs');
  // The key is the reader's own and stays on their device; it's sent only to Anthropic.
  const client = new Anthropic({ apiKey, dangerouslyAllowBrowser: true, baseURL: analyzeWithClaude.baseURL });
  let response;
  try {
    response = await client.beta.messages.create({
      model,
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
  return parseAnalysis(text, list, signature, madeBy);
}
/** Overridable for tests. */
analyzeWithClaude.baseURL = undefined;

/** Any OpenAI-compatible chat completions API (Gemini, Grok, Kimi, ChatGPT, DeepSeek, …). */
async function analyzeWithCompatible(config, list, signature, helpers) {
  const name = config.provider.id === 'custom' ? 'The AI service' : config.provider.label;
  let res;
  try {
    res = await fetch(`${config.baseUrl.replace(/\/+$/, '')}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${config.apiKey}`,
        ...(config.provider.id === 'openrouter' ? { 'X-Title': 'Book Tracker' } : {}),
      },
      body: JSON.stringify({
        model: config.model,
        messages: [{ role: 'system', content: SYSTEM_PROMPT }, { role: 'user', content: libraryJson(list, helpers) }],
      }),
    });
  } catch {
    // Browsers also report blocked cross-site requests this way.
    throw new Error(`Couldn't reach ${name}. Check your connection. If it keeps happening, ${name} may not allow requests from web apps; try OpenRouter or the Android app.`);
  }
  const text = await res.text();
  if (!res.ok) {
    let detail = '';
    try { const o = JSON.parse(text); detail = (Array.isArray(o) ? o[0]?.error?.message : o.error?.message || o.message) || ''; } catch { /* not JSON */ }
    if (res.status === 401 || res.status === 403) throw new Error(`${name} rejected the API key. Check it in Settings → AI.`);
    if (res.status === 404) throw new Error(`${name} couldn't find that model. Check the model name in Settings → AI.`);
    if (res.status === 429) throw new Error(`${name} is busy or your quota ran out (rate limit). Try again later.`);
    throw new Error(`${name} returned an error (${res.status})${detail ? ': ' + String(detail).slice(0, 160) : '. Try again later.'}`);
  }
  let message;
  try { message = JSON.parse(text).choices[0].message; } catch { throw new Error("The AI's answer couldn't be read. Try again."); }
  const content = Array.isArray(message.content) ? message.content.map((p) => p.text || '').join('') : message.content;
  if (!content) throw new Error(message.refusal ? 'The AI declined to answer this time. Try again later.' : 'The AI sent an empty answer. Try again.');
  return parseAnalysis(content, list, signature, config.displayName);
}

/** Sorts the library into genres and recommends books with the configured AI. */
export function analyzeWithAi(config, list, signature, helpers) {
  return config.provider.id === 'anthropic'
    ? analyzeWithClaude(config, list, signature, helpers)
    : analyzeWithCompatible(config, list, signature, helpers);
}
