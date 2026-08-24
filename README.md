# PDF Reader (Android)

An Android app that lets you upload a PDF, then displays its text line by line
and reads it aloud, auto-scrolling and highlighting the current line as it
goes. Includes play/pause, stop, a speed slider, and tap-to-jump on any line.

## How it works

- **Upload**: Uses the system file picker (Storage Access Framework) filtered
  to `application/pdf` — no storage permission needed.
- **Text extraction**: Android has no built-in API for extracting *text* from
  a PDF (only rendering pages as images via `PdfRenderer`), so this app uses
  the open-source **PdfBox-Android** library.
- **Reading aloud**: Uses Android's built-in `TextToSpeech` engine, speaking
  one line at a time and advancing to the next line automatically when the
  previous one finishes (via `UtteranceProgressListener`).
- **UI**: Built with Jetpack Compose. The line list auto-scrolls to keep the
  currently-read line in view, and the current line is bolded/highlighted.

## Project structure

```
app/src/main/java/com/example/pdfreader/
  MainActivity.kt          - Compose UI: file picker, line list, controls
  PdfReaderViewModel.kt    - App state, TTS playback logic
  PdfTextExtractor.kt      - PDF -> list of text lines using PdfBox-Android
```

## Getting an installable APK — no local install needed

This project includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`)
that builds a debug APK in the cloud. You only need a free GitHub account and a
web browser:

1. Go to [github.com](https://github.com) and create a **new repository**
   (any name, e.g. `pdf-reader-app`). Keep it empty (no README/license).
2. On the new repo's page, use **"uploading an existing file"** (a link on the
   quick-setup page) and drag in everything from this unzipped `PdfReaderApp`
   folder — including the hidden `.github` folder. If the site hides it,
   instead use the **"Add file → Upload files"** button and select all files.
3. Commit the upload to the `main` branch.
4. Click the **Actions** tab at the top of the repo. A workflow run called
   "Build APK" should start automatically (takes ~3–5 minutes).
5. When it finishes (green checkmark), open that run and scroll to
   **Artifacts** at the bottom — download `pdf-reader-debug-apk`. Unzip it to
   get `app-debug.apk`.
6. Transfer `app-debug.apk` to your Android phone (email it to yourself,
   Google Drive, USB, etc.), open it, and allow "install from unknown
   sources" if prompted. That installs the app.

This is a **debug build**, which is fine for installing on your own device but
isn't signed for distribution on the Play Store.

## Opening the project in Android Studio instead

If you do get access to a computer later:

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended).
2. Choose **Open** and select the `PdfReaderApp` folder (the one containing `settings.gradle.kts`).
3. Let Android Studio sync Gradle — it will automatically generate the
   `gradlew` wrapper scripts and download Gradle 8.7 the first time.
4. Run on an emulator or a physical device (**Run ▶**). Minimum supported
   Android version is **7.0 (API 24)**.

## Notes / possible follow-ups

- Scanned PDFs (images of text, no embedded text layer) won't produce any
  lines, since there's no OCR step — the app will show a friendly message in
  that case. Adding OCR (e.g. via ML Kit) would be a natural next step.
- The speech language currently follows the device's default locale.
- Very long PDFs are extracted on a background thread so the UI stays
  responsive while loading.
