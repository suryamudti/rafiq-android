# Rafiq - Islamic Lifestyle App

A native Android rebuild of the Flutter Rafiq app, built with Kotlin, Jetpack Compose, and modern Android architecture.

## Tech Stack

- **Kotlin** 2.0.21 + **Jetpack Compose** (Material3)
- **MVVM** architecture with **Hilt** DI (2.56.2)
- **Room** (2.6.1) for local databases (bookmarks, prayer logs)
- **Navigation3** for type-safe navigation
- **Retrofit + Gson** for REST APIs
- **DataStore** Preferences for settings
- **WorkManager** for background tasks (prayer notifications)
- **Media3** (ExoPlayer) for audio recitations
- **OsmDroid** (6.1.18) for OpenStreetMap maps
- **Play Services Location** for GPS
- **AGP** 8.9.2, **Gradle** 8.12, minSdk 23, targetSdk 36

## Features

| Feature | Description |
|---------|-------------|
| Prayer Times | Real-time prayer schedules via Aladhan API, countdown timer, date navigation |
| Quran | 114 surahs with Arabic text, translations, juz/page markers, sajdah indicators |
| Qibla Compass | Bearing calculation with compass visualization, distance to Mecca |
| Audio Recitations | 15 curated reciters, Media3 player |
| 99 Names of Allah | Full list with search, Arabic text, transliterations, meanings |
| Islamic Calendar | Hijri events with month selector, detail bottom sheet |
| Stories of the Prophets | 25 prophets with search, Arabic names, biographies |
| Zakat Calculator | Real-time gold/silver prices, nisab thresholds, asset-based calculation |
| Tasbih Counter | Tap counter with haptic feedback |
| Nearby Mosques | OsmDroid map view |
| Bookmarked Verses | Local Room database |
| Prayer Logging | Track daily prayers |

## Data Sources

- **Quran text:** `quran-uthmani.db` (Madinah Mushaf) bundled as asset
- **Translations:** Bundled SQLite databases (English, Indonesian, etc.)
- **Prayer times:** [Aladhan API](https://aladhan.com/prayer-times-api)
- **Metal prices:** [Metals.live API](https://metals.live/)
- **Maps:** OpenStreetMap via OsmDroid (offline tiles)

## Building

```bash
./gradlew assembleDebug
```

The APK is output at `app/build/outputs/apk/debug/app-debug.apk`.

## Development Setup

- Android Studio Ladybug (2024.2+)
- JDK 17+
- Android SDK 36
- Emulator: Medium Phone API 35 (x86_64)

## Structure

```
app/src/main/java/com/smiledev/rafiq/
├── data/
│   ├── models/        # Data classes
│   ├── repository/    # Data access layer
│   ├── remote/        # Retrofit API definitions
│   └── local/         # Room DAOs and databases
├── di/                # Hilt modules
├── ui/
│   ├── dashboard/     # Main screen with feature grid
│   ├── prayertimes/   # Prayer times screen + VM
│   ├── quran/         # Surah list + Ayah reader + VM
│   ├── qibla/         # Qibla compass screen
│   ├── recitation/    # Audio recitations
│   ├── asmaulhusna/   # 99 Names of Allah
│   ├── calendar/      # Islamic calendar
│   ├── prophets/      # Prophet stories
│   ├── zakat/         # Zakat calculator
│   ├── tasbih/        # Tasbih counter
│   └── mosques/       # Nearby mosques map
├── service/           # Foreground service (recitations)
├── worker/            # WorkManager workers
└── Navigation.kt      # Navigation3 routes
```

## License

All rights reserved.
