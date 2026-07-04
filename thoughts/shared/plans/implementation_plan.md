# Implementation Plan - Rebuilding Rafiq App as a Native Android Application

This plan outlines the process of initializing a new, native Android application in `C:\Flutter\rafiq-app-android` from scratch using Kotlin and Jetpack Compose, while migrating the business logic and assets from the original Flutter codebase.

## Decisions Made (User Review)

> [!IMPORTANT]
> - **Architecture Choice:** We will use **MVVM (Model-View-ViewModel)** with Kotlin Coroutines/Flow, Room DB for local data, Hilt for dependency injection, and Jetpack Compose for the UI.
> - **Pre-populated Quran DBs:** The app depends on SQLite files stored in `assets/quran-data/` (`quran-uthmani.db`, `translations.db`, etc.). We will implement a custom database asset copier utility in Kotlin to extract these to the app's `databases/` directory.
> - **Maps SDK:** We will use **OpenStreetMap** (via OsmDroid wrapped in an `AndroidView`) for the Qibla map and nearby mosques.
> - **Background Scheduler:** We will use **WorkManager** to handle recurring background updates and local notification scheduling.
> - **Asset Migration:** All assets (audio, fonts, JSONs, SQLite databases, and images) will be copied from `C:\Flutter\rafiq-app\assets` into `C:\Flutter\rafiq-app-android\app\src\main\assets`.

---

## Proposed Changes

We will build the native Android project in `C:\Flutter\rafiq-app-android` using the following components and logical steps:

### Phase 1: Project Initialization & Configuration

#### [NEW] [C:\Flutter\rafiq-app-android](file:///C:/Flutter/rafiq-app-android)
* Run the Android CLI project generation:
  ```bash
  android create empty-activity --name="Rafiq App" --minSdk=21 -o C:\Flutter\rafiq-app-android
  ```
* Configure Gradle dependencies (`libs.versions.toml` and `build.gradle.kts` files):
  - **Jetpack Compose:** Material 3, Navigation Compose.
  - **Hilt:** For Dependency Injection (`com.google.dagger:hilt-android`).
  - **Room:** For local data storage (bookmarks and prayer logs).
  - **OsmDroid:** For OpenStreetMap rendering (`org.osmdroid:osmdroid-android`).
  - **WorkManager:** For notification scheduling (`androidx.work:work-runtime-ktx`).
  - **Retrofit & OkHttp:** For HTTP network requests.
  - **Play Services Location:** For GPS coordination.
  - **Gson / Kotlinx Serialization:** For JSON decoding.

---

### Phase 2: Core Architecture & Data Layer Setup

#### [NEW] `com.smiledev.rafiq.core`
- **Database Asset Copier:** A helper class to check, copy, and open read-only databases (`quran-uthmani.db`, translations) from the `assets/` folder to the system databases directory.
- **Preferences Manager:** Key-value storage for settings (Dark Theme toggle, Language preference, prayer notification enables).

#### [NEW] `com.smiledev.rafiq.data`
- **Room Databases:**
  - `BookmarksDatabase`: Entity, Dao, and Database for surah/ayah bookmarks.
  - `PrayerLogDatabase`: Entity, Dao, and Database for prayer tracker history.
- **Repository Interface & Implementations:**
  - `QuranRepository`: Reads Arabic text and translations from copied SQLite DBs.
  - `PrayerTimeRepository`: Queries Aladhan API and caches times.
  - `MosqueRepository`: Performs Overpass API query calls to retrieve nearby mosque markers.

---

### Phase 3: Domain & UI Presentation Layer

#### [NEW] `com.smiledev.rafiq.ui`
- **UI Theme:** Custom Material 3 color schemes for light and dark modes matching the original Flutter app styling.
- **Navigation:** Define Compose routing `NavHost` containing:
  - `DashboardScreen`: Card list, prayer countdown, and status overview.
  - `QuranScreen`: Surah browser tab and Bookmarks list.
  - `PrayerTimesScreen`: Current schedules and manual offset configurations.
  - `QiblaScreen`: Digital compass and map pointer to Mecca.
  - `MosquesScreen`: Map visualization and listing of nearby places of worship.
  - `TasbihScreen`: Tally counter with haptics.

---

## Verification Plan

### Automated Tests
- Build and compile check: `./gradlew assembleDebug`
- Unit tests for the database copier and coordinatebearing calculations: `./gradlew test`

### Manual Verification
- Deploying the app on the local emulator `Medium_Phone_API_35`.
- Verifying UI theme switching (dark/light toggles).
- Verifying database read-write flows (adding bookmarks, reading Quran chapters).
