// Offline support: app files are network-first (so updates show up right
// away) with a cached fallback; covers and fonts are cache-first.
const SHELL_CACHE = 'booktracker-shell-v1';
const ASSET_CACHE = 'booktracker-assets-v1';
const SHELL = ['./', 'index.html', 'styles.css', 'app.js', 'manifest.webmanifest', 'icons/icon-192.png', 'icons/apple-touch-icon.png'];
const ASSET_HOSTS = ['covers.openlibrary.org', 'archive.org', 'books.google.com', 'books.googleusercontent.com', 'fonts.googleapis.com', 'fonts.gstatic.com'];
const MAX_ASSETS = 300;

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(SHELL_CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== SHELL_CACHE && k !== ASSET_CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);

  if (url.origin === self.location.origin) {
    event.respondWith(
      fetch(req)
        .then((res) => {
          if (res.ok) { const copy = res.clone(); caches.open(SHELL_CACHE).then((c) => c.put(req, copy)); }
          return res;
        })
        .catch(() => caches.match(req, { ignoreSearch: true }).then((hit) => hit || caches.match('./'))),
    );
    return;
  }

  if (ASSET_HOSTS.some((h) => url.hostname === h || url.hostname.endsWith('.' + h))) {
    event.respondWith(
      caches.open(ASSET_CACHE).then(async (cache) => {
        const hit = await cache.match(req);
        if (hit) return hit;
        const res = await fetch(req);
        if (res.ok || res.type === 'opaque') {
          await cache.put(req, res.clone());
          const keys = await cache.keys();
          if (keys.length > MAX_ASSETS) await cache.delete(keys[0]);
        }
        return res;
      }),
    );
  }
  // Everything else (book search APIs) goes straight to the network.
});
