# Pulse for Android

Pulse is a local-first personal health companion for Google Fitbit Air. It is designed to combine a readable daily health dashboard, fast strength training, low-friction nutrition logging, and a bring-your-own-key AI coach in one premium Android experience.

## Current prototype

- Premium OLED-first Compose design system
- Today dashboard with Recovery, Sleep and Strain presentation
- Coach brief and daily signals
- Workout routines and exercise library
- Smooth vector exercise demonstrations
- Front/back muscle-impact visualization
- Active workout set logging with previous values
- Completion haptics and automatic rest timer
- Durable product structure for Fuel, Coach and personal health records

All health values in the current prototype are sample UI data. Health Connect persistence and real Fitbit ingestion are the next implementation layer.

## Product principles

- One glance should explain what to do today.
- Logging a set should take one tap.
- Exercise guidance should remain crisp and smooth on every Android density.
- Core workouts must work without network or AI.
- Health and medical information stays on-device except for data the user explicitly sends to their selected AI provider.

## Stack

- Kotlin
- Jetpack Compose
- Material 3, fully re-themed
- Min SDK 28 / target SDK 35
- Gradle 8.13 and Android Gradle Plugin 8.9.1

Planned platform components include Health Connect, Room, WorkManager, CameraX, ML Kit and Android Keystore.

## Build

Install Android SDK 35 and Java 17, then run:

```bash
./gradlew assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Next milestones

1. Validate Fitbit Air record coverage through Health Connect.
2. Replace sample metrics with local Health Connect ingestion and transparent scoring.
3. Persist workouts and export summarized sessions to Health Connect.
4. Add text, barcode and photo-assisted nutrition logging.
5. Add encrypted medical records and evidence-grounded coach tools.

Adaptive daily plans, universal quick logging, and the cross-domain “Why did this change?” timeline are deliberately reserved for a later phase.
