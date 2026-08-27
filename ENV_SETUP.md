# Environment Setup Guide

This document describes the exact environment required to build this Kotlin Multiplatform Mobile (KMM) project reproducibly on any machine (macOS, Windows, or Linux).

## Required JDK Version

- **JDK 17** (recommended)
  - The project is configured to compile with Java 17 (`sourceCompatibility` / `targetCompatibility`).
  - Ensure `JAVA_HOME` points to a JDK 17 installation before running Gradle.

## Android Studio Version Recommendation

- **Android Studio Ladybug (2024.2.1) or newer**
  - Required for AGP 8.7.3 and Compose Multiplatform tooling.
  - Make sure the Kotlin Multiplatform plugin is enabled.

## Gradle Version

- **Gradle 8.10.2**
  - Pinned in `gradle/wrapper/gradle-wrapper.properties`.
  - The wrapper JAR and properties are committed to version control.
  - Always build with `./gradlew` — never use a locally installed Gradle.

## Kotlin Version

- **Kotlin 2.0.21**
  - Pinned in `gradle/libs.versions.toml`.
  - The Compose Compiler plugin is aligned with this Kotlin version.

## NDK Version

- **NDK 27.2.12479018**
  - Pinned in the Android application `build.gradle.kts` via `ndkVersion`.
  - Install via Android Studio SDK Manager or `sdkmanager`.

## Reproducible Build Command

```bash
./gradlew clean build
```

No additional flags or local configuration should be required (assuming `ANDROID_HOME` is set).

## Android SDK Requirements

See `sdk-versions.txt` for the exact SDK components that must be installed.

## Backend URL Configuration

The app uses a production URL reference as the default/fallback for all platforms:

- **Production base URL:** `https://nori-tura.onrender.com`
- **Android debug:** `http://10.0.2.2:8000` (set in `shared/build.gradle.kts`)
- **Android release:** `https://nori-tura.onrender.com` (set in `shared/build.gradle.kts`)
- **iOS:** `https://nori-tura.onrender.com` (set in `iosApp/iosApp/Info.plist` as `BASE_URL`)

To point a local build at a different backend, override the value in the
platform-specific location above. The production URL should remain the
default/fallback so release builds work without local changes.

For local iOS simulator testing against a backend running on your Mac,
temporarily change `BASE_URL` in `iosApp/iosApp/Info.plist` to your Mac's
local network IP (e.g. `http://192.168.1.x:8000`) — the simulator cannot
reach the Mac via `127.0.0.1` because that points to the simulator itself.

## Notes

- Do **not** commit `local.properties`. It is already excluded via `.gitignore`.
- Do **not** commit `.idea/` IDE metadata or `.gradle/` caches.
- The project uses the **Gradle Version Catalog** (`gradle/libs.versions.toml`) as the single source of truth for all dependency and plugin versions.
