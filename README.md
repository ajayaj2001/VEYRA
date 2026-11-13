# Veyra for Android

Veyra is a local-first personal health companion for Google Fitbit Air. It is designed to combine a readable daily health dashboard, fast strength training, low-friction nutrition logging, and a bring-your-own-key AI coach in one premium Android experience.

## Current prototype

- Premium OLED-first Compose design system
- Health Connect availability, permission and sync flow
- Live Fitbit-derived sleep, HRV, resting-HR, activity and heart-rate reads
- Transparent Recovery, Sleep and Strain calculations with calibration states
- Coach brief and daily signals
- Workout routines and exercise library
- Smooth vector exercise demonstrations
- Front/back muscle-impact visualization
- Active workout set logging with previous values
- Editable weights and reps stored in an on-device Room database
- Completion haptics and automatic rest timer
- Crash-safe active workout resume and completed-session summaries
- Durable product structure for Fuel, Coach and personal health records

The Today dashboard uses real Health Connect data after permission is granted. Active workouts and their latest completed summary are local-first and remain available after the app or phone restarts. The initial Push routine is currently fixed while routine editing is built.

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
- Room for local workout persistence
- Min SDK 28 / target and compile SDK 36
- Gradle 8.13 and Android Gradle Plugin 8.13.2

Planned platform components include WorkManager, CameraX, ML Kit and Android Keystore.

## Build

Install Android SDK 36 and Java 17, then run:

```bash
./gradlew assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Next milestones

1. Validate Fitbit Air record coverage on the target phone and tune score thresholds.
2. Add routine editing, workout history and Health Connect session export.
3. Add text, barcode and photo-assisted nutrition logging.
4. Add encrypted medical records and evidence-grounded coach tools.

Adaptive daily plans, universal quick logging, and the cross-domain “Why did this change?” timeline are deliberately reserved for a later phase.
