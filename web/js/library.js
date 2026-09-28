// Stores to buy books from, and the reader's public library (found with OpenStreetMap).
// Location is only used to look up libraries: rounded to ~1 km, used once, never saved.
import { store } from './util.js';

const enc = (s) => encodeURIComponent(s);
const joinQ = (title, author) => [title, author].map((s) => (s || '').trim()).filter(Boolean).join(' ');

/** Store searches for a book, so every link works for any title. */
export function buyLinks(title, author) {
  const e = enc(joinQ(title, author));
  return [
    { id: 'amazon', name: 'Amazon', note: 'Print, Kindle & Audible', url: `https://www.amazon.com/s?k=${e}&i=stripbooks` },
    { id: 'bn', name: 'Barnes & Noble', note: 'Print, Nook & audiobook', url: `https://www.barnesandnoble.com/s/${e}` },
    { id: 'bookshop', name: 'Bookshop.org', note: 'Supports local bookstores', url: `https://bookshop.org/search?keywords=${e}` },
    { id: 'bam', name: 'Books-A-Million', note: 'Print books', url: `https://www.booksamillion.com/search?query=${e}` },
    { id: 'google', name: 'Google Play Books', note: 'Ebooks & audiobooks', url: `https://play.google.com/store/search?q=${e}&c=books` },
    { id: 'kobo', name: 'Kobo', note: 'Ebooks & audiobooks', url: `https://www.kobo.com/us/en/search?query=${e}` },
    { id: 'thriftbooks', name: 'ThriftBooks', note: 'Cheap used copies', url: `https://www.thriftbooks.com/browse/?b.search=${e}` },
    { id: 'abebooks', name: 'AbeBooks', note: 'Used, rare & first editions', url: `https://www.abebooks.com/servlet/SearchResults?kn=${e}` },
  ];
}

export const worldCat = (title, author) => `https://search.worldcat.org/search?q=${enc(joinQ(title, author))}`;

/* ---------- the reader's library ---------- */
const KEY = 'booktracker.library';
export const PLACEHOLDER = 'booktracker';

export const myLibrary = () => store.get(KEY);
/** Saves the library (never the search distance, which came from the reader's location). */
export function saveLibrary(lib) {
  const { distanceKm, ...keep } = lib; // eslint-disable-line no-unused-vars
  store.set(KEY, keep);
}
export const unlinkLibrary = () => store.remove(KEY);

export const hasCatalog = (lib) => !!lib?.catalogUrl && (lib.catalogUrl.includes('{q}') || lib.catalogUrl.toLowerCase().includes(PLACEHOLDER));

/** Search the library's own catalog, or WorldCat until the reader links it. */
export function catalogSearch(lib, query) {
  const t = (lib?.catalogUrl || '').trim();
  const e = enc(query);
  if (t.includes('{q}')) return t.replaceAll('{q}', e);
  if (t.toLowerCase().includes(PLACEHOLDER)) return t.replace(new RegExp(PLACEHOLDER, 'gi'), e);
  return `https://search.worldcat.org/search?q=${e}`;
}

const withScheme = (u) => (/^https?:\/\//i.test(u) ? u : `https://${u}`);
export const websiteLink = (lib) => (lib?.website ? withScheme(lib.website) : null);
export const accountLink = (lib) => ((lib?.accountUrl || lib?.website) ? withScheme(lib.accountUrl || lib.website) : null);
export const mapLink = (lib) => `https://www.google.com/maps/search/?api=1&query=${enc([lib.name, lib.address].filter(Boolean).join(', '))}`;

/* ---------- finding libraries ---------- */
/** Two decimal places is roughly 1 km: enough to find libraries, not a home. */
export const roundForPrivacy = (lat, lon) => [Math.round(lat * 100) / 100, Math.round(lon * 100) / 100];

export function overpassQuery(lat, lon, radiusKm = 15) {
  const around = `around:${radiusKm * 1000},${lat},${lon}`;
  return `[out:json][timeout:25];(node["amenity"="library"](${around});way["amenity"="library"](${around});`
    + `relation["amenity"="library"](${around}););out center tags 60;`;
}

export function distanceKm(lat1, lon1, lat2, lon2) {
  const rad = (d) => (d * Math.PI) / 180;
  const h = Math.sin(rad(lat2 - lat1) / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(rad(lon2 - lon1) / 2) ** 2;
  return 2 * 6371 * Math.asin(Math.sqrt(h));
}

const clean = (v) => (typeof v === 'string' && v.trim() ? v.trim() : null);
const dedupe = (list) => { const seen = new Set(); return list.filter((l) => { const k = `${l.name.toLowerCase()}|${l.address}`; if (seen.has(k)) return false; seen.add(k); return true; }); };

export function parseOverpass(root, fromLat, fromLon) {
  const out = (root?.elements || []).map((el) => {
    const t = el.tags || {};
    const name = clean(t.name);
    if (!name || ['private', 'no'].includes(t.access)) return null;
    const lat = el.lat ?? el.center?.lat ?? null;
    const lon = el.lon ?? el.center?.lon ?? null;
    const street = [clean(t['addr:housenumber']), clean(t['addr:street'])].filter(Boolean).join(' ');
    const address = [street, clean(t['addr:city']), clean(t['addr:state']) || clean(t['addr:postcode'])].filter(Boolean).join(', ') || null;
    return {
      name, address, lat, lon,
      website: clean(t.website) || clean(t['contact:website']) || clean(t.url),
      phone: clean(t.phone) || clean(t['contact:phone']),
      hours: clean(t.opening_hours),
      distanceKm: fromLat != null && lat != null ? distanceKm(fromLat, fromLon, lat, lon) : null,
    };
  }).filter(Boolean);
  return dedupe(out).sort((a, b) => (a.distanceKm ?? Infinity) - (b.distanceKm ?? Infinity)).slice(0, 30);
}

export function parseNominatim(results) {
  return dedupe((results || []).filter((r) => r.type === 'library').map((r) => {
    const a = r.address || {};
    const x = r.extratags || {};
    const street = [clean(a.house_number), clean(a.road)].filter(Boolean).join(' ');
    const city = clean(a.city) || clean(a.town) || clean(a.village) || clean(a.suburb);
    return {
      name: clean(r.name) || clean(a.amenity) || 'Library',
      address: [street, city, clean(a.state)].filter(Boolean).join(', ') || null,
      lat: Number(r.lat) || null, lon: Number(r.lon) || null,
      website: clean(x.website) || clean(x['contact:website']),
      phone: clean(x.phone) || clean(x['contact:phone']),
      hours: clean(x.opening_hours),
      distanceKm: null,
    };
  }));
}

export async function librariesNear(lat, lon) {
  const [rLat, rLon] = roundForPrivacy(lat, lon);
  const res = await fetch('https://overpass-api.de/api/interpreter', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: 'data=' + enc(overpassQuery(rLat, rLon)),
  });
  if (!res.ok) throw new Error(`Library search failed (HTTP ${res.status})`);
  return parseOverpass(await res.json(), rLat, rLon);
}

/** By library name, city or ZIP: no location needed. */
export async function searchLibraries(text) {
  const q = text.trim();
  if (!q) return [];
  const phrase = /librar/i.test(q) ? q : `library in ${q}`;
  const res = await fetch(`https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&extratags=1&limit=20&q=${enc(phrase)}`);
  if (!res.ok) throw new Error(`Library search failed (HTTP ${res.status})`);
  return parseNominatim(await res.json());
}

/** One rough location fix, only for the library search. */
export function roughLocation() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) { reject(new Error('unsupported')); return; }
    navigator.geolocation.getCurrentPosition(
      (p) => resolve([p.coords.latitude, p.coords.longitude]),
      (e) => reject(e),
      { enableHighAccuracy: false, timeout: 20000, maximumAge: 30 * 60 * 1000 },
    );
  });
}

export function formatDistance(km) {
  const miles = /-(US|GB|LR|MM)$/i.test(navigator.language || '');
  return miles ? `${(km * 0.621371).toFixed(1)} mi away` : `${km.toFixed(1)} km away`;
}
