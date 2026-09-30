// Stores to buy books from. Every link is a store search, so it works for any book.
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
