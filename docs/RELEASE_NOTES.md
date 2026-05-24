## v1.0.0

**RhythmWise** is a privacy-first menstrual cycle tracker for Android. All of your data stays on your device — encrypted, offline, and fully under your control.

### What is RhythmWise?

A comprehensive cycle tracking app built with one guiding principle: **your data belongs to you**. There are no accounts, no cloud sync, no analytics, and no internet connection — ever. Your database is encrypted with AES-256-GCM (SQLCipher) behind a passphrase that only you know.

### What's New in 1.0.0

RhythmWise graduates from beta to its first stable release — the full privacy-first tracker, ready for everyday use.

Highlights since the beta series:

- **Backup and restore** — Move your encrypted database between devices with a new export and import flow.
- **Readable insights charts** — Chart axes show phase names and cycle labels, multi-series charts have a color-coded legend, and tapping a bar or point reveals its exact value.
- **Contextual help** — Help buttons across the tracker and daily log open short usage tips so features are easier to discover.
- **Daily log polish** — A "Done" button and tab completion tinting make finishing an entry clearer; mood, energy, and libido stars can be deselected by tapping the selected star again.
- **Calendar refresh** — Distinct shaped day indicators (instead of dots) and phase legend chips that match the calendar fill.
- **Custom tag library** — Custom tags are now first-class library objects you can rename and delete alongside symptoms and medications.
- **Settings reorganized** — Restructured and normalized for clearer navigation.

### Features

- **Period tracking** — Log start/end dates, flow intensity, color, and consistency. View your history on an interactive calendar.
- **Daily wellness log** — Record mood, energy, libido, freeform notes, and custom tags.
- **Symptom tracking** — Curated symptom library organized by category with severity ratings and pattern tracking.
- **Medication log** — Track medications and supplements with dosage notes and a personal medication library.
- **Custom tag library** — Build a personal library of tags you can apply to any daily log entry.
- **Water intake** — Set a daily hydration goal and log with a single tap.
- **Cycle insights** — Cycle length trends, next period predictions, fertile window estimates, symptom recurrence patterns, mood analysis, and phase-based breakdowns — all calculated locally.
- **Educational content** — Articles on cycle basics, symptoms, wellness tips, and when to see a doctor, sourced from U.S. government public health agencies.
- **Reminders** — Customizable daily reminders for logging, period predictions, and hydration goals.
- **Backup and restore** — Export your encrypted database for safekeeping or to migrate to a new device.

### Privacy & Security

- Zero internet permissions — the app **cannot** connect to the internet
- No analytics, no telemetry, no third-party data SDKs
- AES-256-GCM encryption via SQLCipher with Argon2id key derivation
- Encryption key exists only in memory while unlocked, then destroyed
- No passphrase recovery by design — only you can access your data
- Screen capture protection enabled by default

### Install

Download the APK below and sideload it on your Android device (Android 8.0+).

> **Feedback:** If you encounter bugs or have feedback, please [open an issue](../../issues).

RhythmWise is free, open-source (Apache 2.0), and contains no ads or in-app purchases.
