# andrdscren

[![Android CI — andrdscren](https://github.com/tech-master33/andrdscren/actions/workflows/android.yml/badge.svg)](https://github.com/tech-master33/andrdscren/actions/workflows/android.yml)

An Android screen reader and TTS management application, built as part of the [BAOSP project](https://github.com/tech-master33/baosp).

## Download

**Latest APK → [github.com/tech-master33/baosp/releases/tag/nightly](https://github.com/tech-master33/baosp/releases/tag/nightly)**

A fresh build is posted there automatically every night alongside the TTS engine.
You can also find standalone builds on the [releases page](https://github.com/tech-master33/andrdscren/releases) of this repo.

## Features

- Screen Reader Service (AccessibilityService)
- TTS (Text-to-Speech) Management
- Modern UI using Jetpack Compose

## Installing on your device

1. Download the APK from the nightly link above
2. Transfer it to your Android device
3. Install it — allow "unknown sources" if prompted
4. Go to **Settings → Accessibility → Downloaded services → andrdscren** and enable it

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
