# andrdscren

[![Android CI — andrdscren](https://github.com/tech-master33/andrdscren/actions/workflows/android.yml/badge.svg)](https://github.com/tech-master33/andrdscren/actions/workflows/android.yml)

An Android screen reader and TTS management application, built as part of the [BAOSP project](https://github.com/tech-master33/baosp).

## Download

**Latest APK → [github.com/tech-master33/baosp/releases/tag/nightly](https://github.com/tech-master33/baosp/releases/tag/nightly)**

A fresh build is posted there automatically every night alongside the TTS engine and launcher.
You can also find standalone builds on the [releases page](https://github.com/tech-master33/andrdscren/releases) of this repo.

## Features

- Screen Reader Service (AccessibilityService)
- TTS (Text-to-Speech) Management
- Modern UI using Jetpack Compose
- Works out of the box with **aoler** — the BAOSP accessible home screen launcher

## Installing on your device

1. Download the APK from the nightly link above
2. Transfer it to your Android device
3. Install it — allow "unknown sources" if prompted
4. Go to **Settings → Accessibility → Downloaded services → andrdscren** and enable it

For the best experience, also install [aoler](https://github.com/tech-master33/aoler) (the BAOSP launcher) and [aotts](https://github.com/tech-master33/aotts) (the TTS engine) from the same nightly bundle.

## Building locally

```bash
git clone https://github.com/tech-master33/andrdscren.git
cd andrdscren
chmod +x gradlew
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

## CI/CD

Every push to `main` automatically builds a new APK and posts it as a GitHub Release.
The badge above shows whether the latest build passed or failed.

## BAOSP Ecosystem

andrdscren is one of four repos that make up BAOSP — an accessible Android platform for blind and visually impaired users:

| Repo | What it does |
|------|-------------|
| [baosp](https://github.com/tech-master33/baosp) | Main project — nightly bundle, AOSP patches, release coordination |
| **[andrdscren](https://github.com/tech-master33/andrdscren)** | **Screen reader (this repo)** |
| [aotts](https://github.com/tech-master33/aotts) | SVOX Pico TTS engine — speech output |
| [aoler](https://github.com/tech-master33/aoler) | Accessible home screen launcher |

All four APKs are bundled together and published every night at  
**[github.com/tech-master33/baosp/releases/tag/nightly](https://github.com/tech-master33/baosp/releases/tag/nightly)**
