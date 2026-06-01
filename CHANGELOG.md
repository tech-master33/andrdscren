# Changelog — andrdscren

All notable changes to andrdscren are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Nightly builds are automatically tagged; see the [releases page](https://github.com/tech-master33/andrdscren/releases) for APK downloads.

---

## [Unreleased]

Changes merged to `main` but not yet cut into a versioned release.

---

## [0.1.0] — 2025-06-01 (initial nightly)

### Added
- **AccessibilityService** — screen reader service that announces UI elements as the user interacts with them
- **TTS Management** — wraps the Android TextToSpeech API; integrates with aotts (SVOX Pico) when installed
- **Jetpack Compose UI** — modern declarative UI using Material3
- GitHub Actions workflow (`android.yml`) — builds APK on every push to `main` and publishes a release
- Integrated into BAOSP nightly bundle (`baosp-nightly.yml` job 1) alongside aotts and aoler

### Fixed / hardened (build infrastructure)
- Gradle wrapper added (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) — CI can now build without a pre-installed Gradle
- `app/build.gradle.kts` updated:
  - namespace + applicationId changed to `org.baosp.andrdscren`
  - minSdk bumped 24 → 26 (aligned with BAOSP baseline)
  - `jvmTarget` and `compileOptions` bumped 1.8 → 17
  - Release build: `isMinifyEnabled = true`, `isShrinkResources = true`, debug-keystore signing for installable nightlies
- `app/proguard-rules.pro` added — keeps AccessibilityService, Compose, and Kotlin metadata intact

### Technical details
- minSdk 26 (Android 8.0), targetSdk 34 (Android 14)
- Kotlin + Jetpack Compose + Material3, JDK 17, AGP 8.3.2
- Package: `org.baosp.andrdscren`

---

## How the nightly build works

Each night at midnight UTC the `baosp-nightly.yml` workflow in the [baosp](https://github.com/tech-master33/baosp) repo:

1. Checks out the latest `main` of this repo
2. Runs `./gradlew clean assembleRelease` (R8-optimised, debug-signed)
3. Renames the APK to `andrdscren-<git-sha>.apk`
4. Uploads it as part of the combined BAOSP nightly release at  
   **[github.com/tech-master33/baosp/releases/tag/nightly](https://github.com/tech-master33/baosp/releases/tag/nightly)**

The standalone `android.yml` in this repo builds on every push to `main`.

[Unreleased]: https://github.com/tech-master33/andrdscren/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/tech-master33/andrdscren/releases/tag/v0.1.0
