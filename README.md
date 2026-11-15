# Veyra for Android

Veyra is a local-first personal health companion for Google Fitbit Air. It is designed to combine a readable daily health dashboard, fast strength training, low-friction nutrition logging, and a bring-your-own-key AI coach in one premium Android experience.

## Current prototype

- Premium aurora-glass Compose design system with layered lighting, glossy bento cards, rich gradients and floating navigation
- Health Connect availability, permission and sync flow
- Live Fitbit-derived sleep, HRV, resting-HR, activity and heart-rate reads
- Transparent Recovery, Sleep and Strain calculations with calibration states
- Coach brief and daily signals
- Persistent Push, Pull and Legs routines plus custom routine creation/editing
- Looping start/end exercise demonstrations with playback controls and offline vector fallback
- Front/back muscle-impact visualization
- Empty workouts, add-exercise and dynamic add-set flows
- Active workout set logging with previous values, notes and per-exercise rest settings
- Editable weights and reps stored in an on-device Room database
- Completion haptics and automatic rest timer
- Crash-safe active workout resume and completed-session summaries
- Full local workout history with sync status
- Routine volume and exercise weight progression charts
- Searchable exercise picker with equipment and muscle filters
- Session-by-session exercise history with every completed set
- Heaviest-weight, estimated-1RM and session-volume progression modes
- Numbered exercise setup and execution guidance
- One-tap export and retry of completed workouts to Health Connect
- Daily calorie budget and protein, carbohydrate and fat targets
- Persistent food log with ingredient-level nutrition breakdowns
- Favorites, description and fully manual food entry flows
- Camera capture entry point ready for bring-your-own-key photo analysis
- Durable product structure for Fuel, Coach and personal health records

The Today dashboard uses real Health Connect data after permission is granted. Routines, active workouts and completed history are local-first and remain available after the app or phone restarts.

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

Exercise movement frames are loaded from the public-domain [free-exercise-db](https://github.com/yuhonas/free-exercise-db) dataset and cached on-device. VEYRA's generated vector demonstration remains available when media cannot be reached.

## Build

Install Android SDK 36 and Java 17, then run:

```bash
./gradlew assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

Every completed push to `main` publishes:

- An installable debug APK in the workflow artifacts and the latest GitHub Release
- An unsigned optimized release APK for build verification

A permanent Android signing keystore must be configured before release APKs can be installed as update-safe production builds or submitted to Google Play.

## Next milestones

1. Validate Fitbit Air record coverage on the target phone and tune score thresholds.
2. Connect bring-your-own-key food photo analysis and barcode lookup.
3. Add encrypted medical records and evidence-grounded coach tools.
4. Add permanent Android release signing and Play distribution.

Adaptive daily plans, universal quick logging, and the cross-domain “Why did this change?” timeline are deliberately reserved for a later phase.
