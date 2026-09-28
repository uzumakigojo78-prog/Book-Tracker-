# Book Tracker

Has all details of a book and keeps track of how many pages you read per day.

Available as an **Android app** (Jetpack Compose) and a **web app for iPhone**, both
styled with **Material 3 Expressive**: bold type, extra-round shapes, wavy progress
indicators and springy motion.

## Features

- **Your library**: every book with its cover, name, author, release date and total pages
- **Auto-fill**: start typing a title and pick your book from the suggestions; the
  author, release date, page count and cover are filled in for you (from
  [Open Library](https://openlibrary.org) and Google Books, no account needed)
- **Daily page tracking**: log the page you reached each day (today or any past day);
  the app works out how many pages you read that day
- **Progress at a glance**: wavy progress bars, a big percentage ring, pages left
  and your current reading streak
- **Last 14 days chart** and a full day-by-day reading log
- Quick `+1 / +5 / +10 / +25` buttons and an `END` button for finishing a book
- **Genres tab with AI**: sorts your books into genres, describes your taste and recommends
  books to read next. Bring your own API key from the AI service you like (Settings → AI):
  **Claude, Gemini, Grok, Kimi, ChatGPT, DeepSeek, Mistral, OpenRouter, Groq**, or any other
  service with an OpenAI-compatible API (enter its base URL and model). Each service keeps
  its own key and model, and you can change the model name. Without a key it still works in
  basic mode using Open Library's subject tags. Usage is billed by the service you choose and
  only runs when you tap Analyze.
- **Search any book** (Genres tab): search by title, author or topic and open a full book page
  with cover, release date, pages, publisher, rating, genres and description. Add it to your
  books in one tap, and download a **free PDF** when the book is in the public domain (via
  Google Books or the Internet Archive). Copyrighted books get borrow, preview and buy links
  instead.
- **Get a copy**: links for any book or recommendation to free, legal sources:
  public-domain downloads and free borrowing via Open Library / the Internet Archive,
  Project Gutenberg, and Google Books.
- **Books tab sections**: Currently reading, Want to read and Read, collapsible and in any
  order (the reorder button next to "My Books").
- **Make it yours**: any Google Font as the main font (search 1,500+ fonts in Settings → Text),
  a colour wheel to build the whole colour scheme from any colour, and a reorderable bottom
  tab bar (Settings → Style & layout).
- **Home-screen widget** (Android): your current book with cover, progress and today's pages,
  plus quick buttons for +10 pages, Finish, Log, Add book and Settings. Long-press the app icon
  for shortcuts too.
- **Developer page** (Settings → Developer): version info, GitHub links, what's new, and an
  update check that links straight to the newest APK.
- **Automatic CSV backups** (Android): every 15 min, 30 min, 1, 3, 6 or 12 hours, daily
  or weekly, saved as spreadsheet-friendly CSV files in a folder you choose on your
  phone (newest 50 kept). Includes "Back up now" and "Restore from a CSV backup".
  The app only asks for access to that folder and for unrestricted battery use so
  backups run on time.
- Light and dark themes; your books and reading log are stored on your phone

## iPhone (and any browser)

1. Open **https://uzumakigojo78-prog.github.io/Book-Tracker-/** in **Safari**.
2. Tap the **Share** button, then **Add to Home Screen**.
3. Open Book Tracker from your home screen: it runs full-screen, works offline and
   keeps your books on your phone.

The web app has the same tabs and features as the Android app (Books, Genres with
AI, Stats, Settings with themes, text and icon styles). Use **Settings → Backups** now
and then to save a CSV or JSON copy of your data; it can also restore CSV backups made
by the Android app. Browsers can't run scheduled backups in the background, so
automatic backups are Android-only.

The web app lives in [`web/`](web) (plain HTML/CSS/JS modules, no build step; the
Anthropic SDK is bundled in `web/vendor/`) and is
published to GitHub Pages by `.github/workflows/pages.yml` on every push to `main`.
If the site isn't live after the first deploy, open **Settings → Pages** and set the
source to **Deploy from a branch → `gh-pages` / root**.

## Android

1. Open the [**Releases**](../../releases/latest) page of this repository.
2. Download `BookTracker-x.y.z.apk` on your Android phone (Android 8.0 or newer).
3. Open the file and allow installing from this source when asked.

A new release is published automatically every time `main` is updated. Every
other branch build also produces the APK as a download on the run's page in the
**Actions** tab.

### Signing (optional)

Without signing secrets, CI signs the APK with a throwaway key, so a new version
may need the old one uninstalled first. To install updates in place, create a
keystore once and add these repository secrets:

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | `base64 -w0 release.jks` |
| `SIGNING_STORE_PASSWORD` | keystore password |
| `SIGNING_KEY_ALIAS` | key alias |
| `SIGNING_KEY_PASSWORD` | key password |

```sh
keytool -genkeypair -v -keystore release.jks -alias booktracker \
  -keyalg RSA -keysize 2048 -validity 10000
```

## Building locally

Requires JDK 17+ and the Android SDK.

```sh
./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
```
