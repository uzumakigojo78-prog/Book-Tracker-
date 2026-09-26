# Book Tracker

Has all details of a book and keeps track of how many pages you read per day.

An Android app built with Jetpack Compose and **Material 3 Expressive**: bold type,
extra-round shapes, wavy progress indicators and springy motion.

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
- Light and dark themes; your books and reading log are stored on your phone

## Download

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
