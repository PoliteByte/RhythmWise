## v1.1.0

**RhythmWise** is a privacy-first menstrual cycle tracker for Android. All of your data stays on your device — encrypted, offline, and fully under your control.

### What is RhythmWise?

A comprehensive cycle tracking app built with one guiding principle: **your data belongs to you**. There are no accounts, no cloud sync, no analytics, and no internet connection — ever. Your database is encrypted with AES-256-GCM (SQLCipher) behind a passphrase that only you know.

### What's New in 1.1.0

A new app icon, plus a big round of beta feedback improvements: clearer mood, energy, and libido selectors, a cycle status banner on the home screen, one-tap period start that fills in your usual duration, predictions from day one, an easier to read calendar, a visual color picker, new auto-lock options, and optional sound effects for accessibility (off by default). Also fixes the follicular phase display and startup errors.

This update is built almost entirely from your beta feedback. Thanks for all of it, keep it coming.

- **A new look**: RhythmWise has a proper icon now, based on the moon cycle artwork. On Android 13+ it also adapts to your wallpaper colors if you use themed icons.
- **Faster logging**: Mood, energy, and libido use descriptive icons (faces, bolts, hearts), only your current pick stays highlighted, and a clear "3/5" style readout shows what you logged. The note editor capitalizes sentences and continues bullet lists for you, and a banner on the home screen shows where you are in your cycle at a glance.
- **Smarter tracking**: Setup asks for your typical cycle and period length, so predictions work from your very first day. Starting a period with one tap fills in your usual duration, with undo. The calendar is easier to scan, with visible daily scores and a stronger ring around today, and future dates can no longer be logged by accident.
- **Optional sound effects**: A new Sound section in Settings under Notifications adds quiet audio feedback for taps, swipes, and actions. It is off by default and meant mainly as an accessibility aid for anyone who finds the screen hard to see, with its own volume slider that previews the level as you set it.
- **Settings and polish**: Phase and heatmap colors have a real visual color picker. Auto-lock gains Immediately and Never options. Insight cards are calmer, learn articles collapse out of the way, and the tutorial has a visible skip button, a step indicator, and can be replayed.
- **Fixes**: An ongoing period no longer hides the follicular phase, and startup problems now show an error instead of a blank screen.

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
- **Sound feedback (optional)**: Quiet audio cues for taps, swipes, and actions, designed as an accessibility aid. Off by default.
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
