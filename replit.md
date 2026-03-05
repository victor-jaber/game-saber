# Jogo da Memória Bobo

A simple offline Android memory game (MVP) intended for Google Play Store publication.

## Project Overview

- **Type:** Native Android application (Kotlin)
- **Language:** Kotlin 1.9.24
- **Platform:** Android (minSdk 24, targetSdk 35)
- **Build System:** Gradle with Kotlin DSL (`.gradle.kts`)
- **Architecture:** MVVM with ViewModel and LiveData
- **UI:** Android View system (XML Layouts) with Material Components

## Features

- 16-card grid memory game (8 pairs)
- 90-second countdown timer
- Score tracking by performance
- Local high score persistence via SharedPreferences
- Portuguese (pt-BR) interface

## Project Structure

```
app/src/main/java/com/victorjaber/gamejaber/
├── MainActivity.kt              # Main entry point, UI + card interaction
├── data/ScoreRepository.kt      # High score persistence (SharedPreferences)
└── game/
    ├── Card.kt                  # Card data model
    ├── FlipResult.kt            # Flip result enum
    ├── GameUiState.kt           # UI state model
    ├── GameViewModel.kt         # MVVM ViewModel (timer, UI state)
    └── MemoryGameEngine.kt      # Core game logic (deck, flipping, matching)
```

## Build Setup

- **Java:** GraalVM CE 22.3.1 (OpenJDK 19) — installed via Replit modules
- **Gradle:** 8.7 (downloaded via wrapper)
- **Gradle Wrapper:** `gradle/wrapper/` — configured for Gradle 8.7

### To build the debug APK:
```bash
./gradlew assembleDebug
```

> **Note:** The Android SDK (build-tools, platform-35) must be available.
> The Replit environment does not include the Android SDK, so the build
> will fail unless the SDK is manually installed. This app must be built
> and run in Android Studio or a CI environment with the Android SDK.

## Workflow

- **Build APK** — runs `bash build_info.sh` (console output)

## Dependencies

- `androidx.core:core-ktx:1.13.1`
- `androidx.appcompat:appcompat:1.7.0`
- `com.google.android.material:material:1.12.0`
- `androidx.constraintlayout:constraintlayout:2.1.4`
- `androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4`
- `androidx.lifecycle:lifecycle-livedata-ktx:2.8.4`
- `androidx.activity:activity-ktx:1.9.1`
