# ECHO

<p align="center">
  <img src="icon.png" width="128" alt="ECHO icon" />
</p>

<p align="center">
  <b>A voice diary of time.</b><br/>
  A minute a day about how it went — and exactly a year later the app returns<br/>
  your voice to you, like a letter from the past.
</p>

---

## About

ECHO is an offline-first voice journaling app for Android. Record a short voice
note (30–120 seconds) about your day, tag it with a mood — and move on with your
life. One year later, on the very same day, the app gently hands your own voice
back to you: *"Exactly a year ago, you recorded this. Listen."*

No accounts, no cloud, no tracking. Everything lives on your phone.

## Features

- **Voice recording** — up to 120 seconds per entry, with a live waveform,
  silence prompts ("talk to me" suggestions) and optional auto-stop on silence.
- **The Letter** — every recording comes back exactly one year later as a
  personal voice letter from your past self.
- **Timeline** — browse your whole history, play entries, delete what you don't
  need, reply to entries with threaded voice comments.
- **Year map** — see your year at a glance: every day you recorded something
  lights up on the map.
- **Sealed recordings** — lock an entry until a chosen date. Sealed entries
  open only after the date arrives (and can be protected with biometrics).
- **Moods** — tag every entry with an emoji mood and watch your emotional
  landscape over the year.
- **Streaks** — a gentle 🔥 counter keeps your daily habit alive.
- **On-device transcription** — optional offline speech-to-text for every
  entry, powered by Android's `SpeechRecognizer`. Fully optional: if it fails,
  the recording is still saved.
- **Smart reminders** — a daily reminder at your chosen time, plus a
  "Recording of the week" that resurfaces a random entry from your past.
- **Missed anniversaries** — if the app wasn't around on the exact date, queued
  letters are delivered as soon as it opens again.
- **Audio quality settings** — Eco (64 kbps), Standard (128 kbps), High (192 kbps).
- **Themes & languages** — dark / light / system theme, English and Russian.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 17) |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM (Compose + ViewModels) |
| Navigation | Navigation-Compose |
| Database | Room (with KSP) |
| Preferences | DataStore |
| Background work | WorkManager (reminders, anniversary processing) |
| Audio | `MediaRecorder` (AAC/M4A), `AudioTrack` for UI sounds |
| Speech-to-text | `SpeechRecognizer` (offline, on-device) |
| Security | AndroidX Biometric (sealed entries) |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 15+ (API 37) |

## Privacy

- **100% offline** — no network permission is used for your data; recordings
  never leave the device.
- Recordings are stored in the app's private storage (`filesDir`).
- No analytics, no ads, no third-party SDKs that phone home.

## Getting started

### Prerequisites

- Android Studio (latest stable)
- JDK 17
- Android SDK 36 with Build-Tools

### Build

**Option 1 — Android Studio (the easy way)**

1. Open Android Studio → **Open** → pick the project folder.
2. Wait for Gradle Sync to finish.
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Click **"locate"** in the popup — your APK is in
   `app/build/outputs/apk/debug/app-debug.apk`.

**Option 2 — Command line**

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

### Release build

Debug builds are signed with the debug key and are fine for personal use. To
distribute, build a signed release:

1. **Generate a keystore** (once):
   ```bash
   keytool -genkey -v -keystore echo-release.jks -keyalg RSA \
       -keysize 2048 -validity 10000 -alias echo
   ```
2. Add the keystore path/credentials to `~/.gradle/gradle.properties`
   (never commit the `.jks` file itself).
3. Configure a `signingConfigs` block in `app/build.gradle.kts` and run:
   ```bash
   ./gradlew assembleRelease
   ```
   The signed APK will be in `app/build/outputs/apk/release/`.

### Project structure

```
app/src/main/java/ru/echoapp/
├── audio/          # AudioRecorder, Transcriber, SoundEffects
├── data/           # Room database + RecordingRepository
├── notifications/  # Reminder & anniversary notifications
├── settings/       # DataStore-backed settings
├── ui/             # Compose screens: record, feed, year, letter, playback, settings
├── work/           # WorkManager schedulers
└── util/           # Streaks, anniversaries, moods
```

## Roadmap

- [ ] Foreground service recording with a persistent notification
- [ ] Waveform scrubbing on the playback screen
- [ ] Export / backup of the archive
- [ ] Home-screen widget with the streak

## Third-party licenses

ECHO is built on open-source software. All dependencies are distributed under
the permissive **Apache License 2.0**, which allows use in closed-source,
commercial applications:

- [Android Jetpack](https://developer.android.com/jetpack) — Compose, Room,
  WorkManager, DataStore, Navigation, Biometric, AppCompat, Core KTX
- [Kotlin](https://kotlinlang.org/) by JetBrains
- [Material Icons](https://fonts.google.com/icons)

JUnit (Eclipse Public License 2.0) is used for unit tests only and is not
shipped in the app.

## License

This project is licensed under the [MIT License](LICENSE) — © 2026 Lunarium Studio.

---

<p align="center">
  Developed by <b>Lunarium Studio</b><br/>
  <a href="https://lunariumstudio.space/">https://lunariumstudio.space/</a>
</p>
