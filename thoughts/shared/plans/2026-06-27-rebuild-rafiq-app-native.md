# Rebuilding Rafiq App as Native Android Application Implementation Plan

## Overview

We are rebuilding the existing Flutter application `rafiq-app` as a native Android mobile application in `C:\Flutter\rafiq-app-android`. This native rebuild will follow modern Android best practices, using Kotlin, Jetpack Compose, the MVVM architecture, Room DB, WorkManager, Dagger Hilt, and OpenStreetMap (OsmDroid).

## Current State Analysis

The original application is a cross-platform Flutter app (`C:\Flutter\rafiq-app`). Key features include:
1. **Quran Reader:** Renders Uthmani text via SQLite databases copied at runtime from assets, with bookmarking capabilities.
2. **Prayer Times:** Uses Geolocator for GPS, fetches times from Aladhan API, caches locally, and schedules alarms.
3. **Qibla Compass:** Device compass integration, calculates bearings to Kaaba, and displays Osm map pointers.
4. **Nearby Mosque:** Queries Overpass API for mosques and displays them on a map.
5. **25 Prophets Stories:** Renders detailed profiles and stories of the prophets, supporting bilingual localization.
6. **Audio Recitations:** Streams Surah recitations with media player controls.
7. **Islamic Calendar:** Gregorian-to-Hijri converter and list of Islamic holidays/events.
8. **Zakat Calculator:** Assets calculator (gold, silver, cash) referencing real-time prices.
9. **Asmaul Husna Browser:** Displays the 99 names of Allah in Arabic, English, and Indonesian.
10. **Tasbih Counter:** Tally counter with haptic feedback and custom limit alerts.

The target project directory (`C:\Flutter\rafiq-app-android`) is currently initialized only with plan/design system documentation files.

## Desired End State

A fully functional, compile-ready native Android application in `C:\Flutter\rafiq-app-android` that replicates all features of `rafiq-app` using Kotlin and Jetpack Compose.

### Key Discoveries:
- [pubspec.yaml](file:///C:/Flutter/rafiq-app/pubspec.yaml) - Lists all current library dependencies.
- [quran_data_services.dart:531-552](file:///C:/Flutter/rafiq-app/lib/src/services/services/quran_data_services.dart#L531-L552) - Explains how assets SQLite DBs are copied to the local device storage.
- [security.dart:7-24](file:///C:/Flutter/rafiq-app/lib/src/resources/security.dart#L7-L24) - Shows custom request signatures generated for backend API requests.

## What We're NOT Doing

1. iOS support.
2. Google Maps integration (OpenStreetMap is used instead).
3. Modifying the original Flutter repository.

## Implementation Approach

Rebuilding will proceed in 4 phases:
- **Phase 1:** Project initialization and asset copying.
- **Phase 2:** Core infrastructure, database copying, and Dependency Injection setup.
- **Phase 3:** Repositories, APIs, and business services.
- **Phase 4:** Jetpack Compose UI, Themes, and Navigation.

---

## Phase 1: Project Initialization & Configuration

### Overview
Initialize the empty activity Compose template using Android CLI and copy all assets and locale files.

### Changes Required:

#### 1. Project Creation
**Directory**: `C:\Flutter\rafiq-app-android`
**Action**: Run Android CLI initialization:
```bash
C:\Users\groun\AppData\AndroidCLI\android.exe create empty-activity --name="Rafiq App" --minSdk=21 -o C:\Flutter\rafiq-app-android
```

#### 2. Asset Copier Setup
**Action**: Copy files from `C:\Flutter\rafiq-app\assets` to `C:\Flutter\rafiq-app-android\app\src\main\assets`. Copy JSON files from `C:\Flutter\rafiq-app\locale` to `C:\Flutter\rafiq-app-android\app\src\main\assets\locale`.

#### 3. Gradle Dependencies
**File**: `app/build.gradle.kts`
**Changes**: Add dependencies for Dagger Hilt, Room, WorkManager, OsmDroid, Retrofit, Play Services Location, OkHttp, and **Media3 ExoPlayer** (for recitation streaming).

### Success Criteria:

#### Automated Verification:
- [x] Project compiles cleanly: `./gradlew assembleDebug`
- [x] Assets are present in the package build output

#### Manual Verification:
- [ ] Verify template app launches on `Medium_Phone_API_35` emulator

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase.

---

## Phase 2: Core Architecture Setup

### Overview
Setup the Hilt dependency injection graph, preferences storage, and database copier helper.

### Changes Required:

#### 1. Database Copier
**File**: `app/src/main/java/com/smiledev/rafiq/core/DatabaseCopier.kt`
**Changes**: Implement database copying from assets to app local databases directory:
```kotlin
class DatabaseCopier(private val context: Context) {
    fun copyDatabase(dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            context.assets.open("quran-data/$dbName").use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}
```

#### 2. Bookmarks & Prayer Logs Room Database
**File**: `app/src/main/java/com/smiledev/rafiq/data/local/BookmarkDatabase.kt`
**Changes**: Define Bookmark Room entity and DAO with SQLite compatibility. Also include a database for the prayer logs.

---

## Phase 3: Data Layer & Services

### Overview
Implement API endpoints, repositories, location trackers, and notification managers.

### Changes Required:

#### 1. Remote API Services
**File**: `app/src/main/java/com/smiledev/rafiq/data/remote/AladhanApi.kt`
**Changes**: Define Retrofit interface matching the prayer time endpoints used in the Flutter app.
**File**: `app/src/main/java/com/smiledev/rafiq/data/remote/MetalPriceApi.kt`
**Changes**: Define gold/silver live prices fetch endpoints for Zakat calculations.

#### 2. WorkManager Notification Scheduler
**File**: `app/src/main/java/com/smiledev/rafiq/service/PrayerNotificationWorker.kt`
**Changes**: Custom Worker that schedules notifications with custom sound files (`adzan_default.mp3` & `adzan_default_subuh.mp3`).

#### 3. Audio Recitations Service
**File**: `app/src/main/java/com/smiledev/rafiq/service/AudioRecitationService.kt`
**Changes**: Setup native Media3 session handling for foreground playback controls and notification player integrations.

---

## Phase 4: Jetpack Compose UI

### Overview
Build user interface screens, theme switching, and navigation routing.

### Changes Required:

#### 1. UI Navigation
**File**: `app/src/main/java/com/smiledev/rafiq/ui/navigation/NavGraph.kt`
**Changes**: NavHost specifying screens:
- **DashboardScreen**: Header countdown, Hijri dates, and quick features.
- **QuranScreen & AyahScreen**: Tab views (Surah list & Bookmarks), fast scroll, and ayah reading blocks.
- **PrayerTimesScreen**: Full schedule, settings offset correction, and alarms.
- **QiblaScreen**: Rotating dial and needle canvas pointing to Mecca.
- **MosquesScreen**: OpenStreetMap pinpoints and native map redirects.
- **ProphetsScreen & ProphetDetailScreen**: Searchable story browser.
- **AudioRecitationScreen**: Player controls, reciters browser, and background status.
- **IslamicCalendarScreen**: Conversion matrix and holidays list.
- **ZakatCalculatorScreen**: Metal calculation forms.
- **AsmaulHusnaScreen**: Allah's names browser.
- **TasbihScreen**: Digital tally widget with vibration.

### Success Criteria:

#### Automated Verification:
- [x] Layout checking and UI unit tests pass: `./gradlew test`

#### Manual Verification:
- [ ] Verify Qibla compass needle rotates correctly as device heading updates
- [ ] Verify OpenStreetMap rendering user location and Mecca pin points
- [ ] Verify light/dark theme switching persistence

---

## Testing Strategy

### Unit Tests:
- GPS bearing trigonometry calculations.
- Local time correction offsets algorithms.

### Manual Testing Steps:
1. Launch app, verify initial Quran database copying finishes.
2. Enable/disable dark theme, verify UI theme responds instantly.
3. Open Qibla screen, rotate emulator sensor, verify needle stays aligned to Mecca.

## Performance Considerations
- Use SQLite read-only connections for Quran searches to save memory.
- Recycler-like lazy columns for Quran rendering to avoid rendering lags.

## Migration Notes
- Initial setup copies databases. Prior settings are set to defaults.

## References
- Original code: `C:\Flutter\rafiq-app`
- Current implementation plan: `thoughts/shared/plans/implementation_plan.md`
