# Architecture & Portfolio Improvements — Rafiq Android

## Overview

This document outlines strategic improvements to transform Rafiq Android from a functional hobby app into a **production-quality** codebase that demonstrates **lead Android engineering** skills: architectural rigor, testing culture, CI/CD automation, and maintainability at scale.

---

## Current State Analysis

The app has **strong foundations** but **gaps that prevent portfolio readiness**:

| Area | Current State | Gap |
|------|--------------|-----|
| **Architecture** | Flat single module, no domain layer, no repository interfaces | Cannot demonstrate modularization at scale |
| **Testing** | 2 test files, 7 test cases for 40+ files | Massive coverage gap |
| **Error Handling** | `error: String?` with raw `e.message` | No structure, no retry, no user-friendly messages |
| **CI/CD** | Docs-only workflows, empty `.github/workflows/` | No automated quality gates |
| **Services** | `AudioRecitationService` unused, `PrayerNotificationWorker` never scheduled | Dead code / incomplete features |
| **Build** | ProGuard disabled, no build variants | Not production-ready |
| **API Clients** | No offline cache, no timeout config, exceptions propagate raw | Brittle network layer |

**Strengths to preserve:** Modern Compose + Nav3 stack, consistent Screen/ViewModel/UiState pattern across all 13 features, DataStore adoption, Material3 dynamic theming, offline-first Quran DBs with robust copy/verify.

---

## Desired End State

A **multi-module, clean-architecture Android app** with:

- `:core`, `:domain`, `:data` modules separate from `:feature:*` modules
- Repository interfaces in `:domain` with implementations in `:data`
- `Result<T, AppError>` sealed class for all data operations
- 80%+ line coverage on critical paths (ViewModels, Repositories, Use Cases)
- GitHub Actions CI with lint, test, build, and automated release
- ProGuard/R8 minification, build variants, versioned releases
- Accessibility and i18n pass
- All 15 navigation routes work with proper lifecycle management

---

## What We're NOT Doing

- **Not migrating Nav3 to the stable version** (Nav3 is the architecture choice; replacing it would be churn without portfolio value)
- **Not replacing raw SQLite Quran DBs with Room** (The risk/reward is poor; Room for read-only asset DBs adds overhead without benefit)
- **Not rewriting the UI** (Compose is already used; the patterns are sound)
- **Not adding server-side features** (This is an Android portfolio, not full-stack)
- **Not replacing OsmDroid with Google Maps** (OsmDroid is a valid choice; replacing it doesn't demonstrate engineering skill)

---

## Implementation Approach

**Two parallel tracks:**

1. **Structural** — Multi-module refactor, domain layer, testing infrastructure (foundational, done first)
2. **Feature** — Complete incomplete features, CI/CD, performance (visible impact, continuous)

---

## Phases

### Phase 1: Modularization & Domain Layer (Foundations)

**Why:** Demonstrates the single most important lead-engineer skill — organizing code for scale. Every other improvement (testing, CI, performance) benefits from clean module boundaries.

#### Changes Required

**1a. Module creation**

```
rafiq-android/
  build.gradle.kts           # Root: plugin declarations only
  settings.gradle.kts        # Include all modules
  gradle/libs.versions.toml  # Shared version catalog
  :core/
    build.gradle.kts          # No Android plugin; pure Kotlin
    src/main/kotlin/
      com/smiledev/rafiq/core/
        Result.kt             # sealed class Result<T, E>
        AppError.kt           # sealed interface AppError
        DispatcherProvider.kt # interface for testable coroutine dispatchers
  :domain/
    build.gradle.kts          # No Android plugin; pure Kotlin
    src/main/kotlin/
      com/smiledev/rafiq/domain/
        repository/           # Interfaces only
          QuranRepository.kt  # interface, not class
          PrayerTimesRepository.kt
          MetalPriceRepository.kt
          …
        usecase/              # Business logic extracted from ViewModels
          GetAyahsWithTranslationUseCase.kt
          CalculateZakatUseCase.kt
          GetPrayerTimesUseCase.kt
          …
        model/                # Domain models (no framework annotations)
          Surah.kt
          Ayah.kt
          PrayerTimings.kt
          …
  :data/
    build.gradle.kts          # Android library
    src/main/kotlin/
      com/smiledev/rafiq/data/
        repository/           # Implementations of domain interfaces
          QuranRepositoryImpl.kt
          PrayerTimesRepositoryImpl.kt
          …
        local/                # Room DBs, DataStore
        remote/               # Retrofit APIs
        preferences/          # PreferencesManager
  :feature:quran/
  :feature:prayertimes/
  :feature:settings/
  … (one module per screen group)
  :app/                       # Thin shell: DI wiring, navigation host, Application
```

**1b. Repository interfaces in `:domain`**
- `QuranRepository` → interface with `getAyahsWithTranslation()`, `getChapters()`
- `PrayerTimesRepository` → interface with `getPrayerTimes()`
- `MetalPriceRepository` → interface with `getGoldPricePerGram()`, `getSilverPricePerGram()`
- Existing classes become `*Impl` in `:data`

**1c. Use cases in `:domain`**
- Extract business logic from ViewModels:
  - `CalculateZakatUseCase` — from `ZakatCalculatorViewModel.calculate()`
  - `GetAyahsWithTranslationUseCase` — enriches ayahs with metadata
  - `CalculateQiblaUseCase` — bearing/distance math from `QiblaViewModel`
- Use cases are injectable, testable, and follow Single Responsibility

**1d. `Result<T, AppError>` sealed class**

```kotlin
// :core module
sealed class Result<out T, out E : AppError> {
    data class Success<T>(val data: T) : Result<T, Nothing>()
    data class Error<E : AppError>(val error: E) : Result<Nothing, E>()
}

sealed interface AppError {
    data class Network(val message: String, val cause: Throwable?) : AppError
    data class Database(val message: String, val cause: Throwable?) : AppError
    data object NotFound : AppError
    data object Unauthorized : AppError
    data class Unknown(val message: String) : AppError
}
```

**1e. `DispatcherProvider` interface**

```kotlin
// :core module
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
// Default impl via @Inject constructor
// Test impl via StandardTestDispatchers (already partially done in ZakatViewModelTest)
```

#### Success Criteria

- [ ] `gradle projects` shows 5+ modules
- [ ] `:domain` has zero Android dependencies
- [ ] `:core` has zero Android dependencies
- [ ] All ViewModels inject domain interfaces, not data implementations
- [ ] Build completes in under 2 minutes (incremental)

---

### Phase 2: Testing Infrastructure (Quality Signal)

**Why:** Tests are the single strongest signal of engineering maturity. A well-tested codebase with CI gates demonstrates professional standards.

#### Changes Required

**2a. Test framework setup**
- Add JaCoCo for coverage reports
- Add test reporting to CI artifacts
- Add `.github/workflows/test.yml`:

```yaml
name: PR Check
on: pull_request
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'oracle', java-version: '17' }
      - name: Unit tests
        run: ./gradlew testDebug --build-cache
      - name: Coverage report
        run: ./gradlew jacocoTestReport
      - uses: actions/upload-artifact@v4
        with: { name: coverage-report, path: '**/build/reports/jacoco/' }
```

**2b. ViewModel tests (highest priority)**

| ViewModel | Tests to Write |
|-----------|---------------|
| `QuranViewModelTest` | Load surahs, load ayahs, toggle bookmark, font size settings |
| `PrayerTimesViewModelTest` | Load times, countdown, location changes, API errors |
| `ZakatCalculatorViewModelTest` | Expand existing (gold nisab, silver nisab, currency conversion, loading states, errors) |
| `SettingsViewModelTest` | Theme change, translation language, font size changes |
| `QiblaViewModelTest` | Bearing calculation, distance calculation, location permission denied |
| `TasbihViewModelTest` | Counter increment/decrement/reset, haptic toggle |
| `MosquesViewModelTest` | Location search, empty results, API errors |

Each ViewModel test pattern:
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class XxxViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: XxxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Mock dependencies with MockK
    }

    @After
    fun cleanup() { Dispatchers.resetMain() }

    @Test
    fun `initial state is loading`() { ... }

    @Test
    fun `load data shows success state`() { ... }

    @Test
    fun `load data on error shows error state`() { ... }
}
```

**2c. Repository tests**

| Repository | Tests |
|-----------|-------|
| `QuranRepositoryTest` | Load surahs from mock JSON, load ayahs with translation, missing translation fallback |
| `PrayerTimesRepositoryTest` | Success response parsing, network error, HTTP error, malformed JSON |
| `MetalPriceRepositoryTest` | Price parsing, empty response fallback, network error |

**2d. Database DAO tests**

| DAO | Tests |
|-----|-------|
| `BookmarkDaoTest` | Insert, delete, getAyasBySura, isBookmarked, getAllFlow emits |
| `PrayerLogDaoTest` | Upsert, getLogForDate, getAllLogs flow |

Use Room's `MigrationTestHelper` or in-memory database:
```kotlin
@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {
    private lateinit var db: BookmarkDatabase
    private lateinit var dao: BookmarkDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BookmarkDatabase::class.java).build()
        dao = db.bookmarkDao()
    }

    @After
    fun cleanup() { db.close() }
}
```

**2e. UI Compose tests**

| Screen | Tests |
|--------|-------|
| `DashboardScreenTest` | All 12 feature cards visible, settings icon clickable |
| `AyahScreenTest` | Ayah list renders, translations shown, bookmark dialog appears |
| `SettingsScreenTest` | Radio button selection, slider interaction |
| `TasbihScreenTest` | Counter display, increment on tap |

```kotlin
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardShowsQuranCard() {
        composeTestRule.onNodeWithText("Quran").assertIsDisplayed()
    }
}
```

**2f. Coverage target**
- Add `build.gradle.kts` configuration for JaCoCo
- Set initial target: **60%** line coverage on `:domain` and `:data` modules
- Set stretch target: **70%** on ViewModels

#### Success Criteria

- [ ] `./gradlew testDebug` runs 50+ test cases (from 7 today)
- [ ] `./gradlew jacocoTestReport` produces a coverage report
- [ ] GitHub Actions PR check passes tests on every PR
- [ ] Coverage badge in README

---

### Phase 3: Error Handling Overhaul (Reliability)

**Why:** Raw `e.message` shown to users screams "hobby project." Proper error handling with retry and user-friendly messages is the mark of production software.

#### Changes Required

**3a. Adopt `Result<T, AppError>` across all repositories**

Pattern:
```kotlin
// Before
fun getPrayerTimes(date: String, lat: Double, lon: Double): PrayerTimesResponse?

// After
suspend fun getPrayerTimes(date: String, lat: Double, lon: Double): Result<PrayerTimesResponse, AppError>
```

**3b. Add retry utility to `:core`**

```kotlin
suspend fun <T> retryIO(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> Result<T, AppError>
): Result<T, AppError>
```

**3c. User-friendly error messages in UI**

Before:
```kotlin
Text(text = "Error: ${state.error}")
```

After:
```kotlin
when (val error = state.error) {
    is AppError.Network -> Text("Unable to connect. Check your internet and try again.")
    is AppError.Database -> Text("Something went wrong loading data. Please restart the app.")
    else -> Text("An unexpected error occurred. Please try again.")
}
```

**3d. Add pull-to-refresh to data screens**

- Quran list, Prayer Times, Bookmarks, Prayer Log
- Uses `PullToRefreshBox` (Material3) with ViewModel's `refresh()` method

#### Success Criteria

- [ ] No raw `e.message` shown to users
- [ ] Every network call has retry logic
- [ ] Pull-to-refresh on all data screens
- [ ] Error states show actionable messages, not stack traces

---

### Phase 4: CI/CD & Release Pipeline (DevOps)

**Why:** Automated release to Play Store demonstrates full lifecycle ownership, from commit to production.

#### Changes Required

**4a. CI workflow (PR Check)**

`.github/workflows/pr-build.yml` — should already exist based on `GITHUB_WORKFLOWS.md`:
```yaml
name: PR Check
on: pull_request
jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
      - run: ./gradlew lintDebug
  test:
    needs: lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./gradlew testDebug
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with: { name: apk-debug, path: app/build/outputs/apk/debug/*.apk }
```

**4b. Release workflow (tag-driven)**

`.github/workflows/release.yml`:
```yaml
name: Release
on:
  push:
    tags: ['v*']
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
      - run: ./gradlew assembleRelease
        env:
          SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
          ALIAS: ${{ secrets.ALIAS }}
          KEY_STORE_PASSWORD: ${{ secrets.KEY_STORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      - uses: actions/create-release@v1
        with: { tag_name: ${{ github.ref }}, release_name: "Release ${{ github.ref }}" }
      - uses: actions/upload-release-asset@v1
        with: { asset_path: app/build/outputs/apk/release/*.apk }
```

**4c. ProGuard/R8 rules**

Enable in `build.gradle.kts`:
```kotlin
release {
    isMinifyEnabled = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

Create `app/proguard-rules.pro` with rules for:
- Retrofit, OkHttp, Gson (keep serialized classes)
- Room entities
- Hilt/Dagger keep rules
- Kotlin serialization keep rules

**4d. Build variants**

Add `staging` build type for internal testing:
```kotlin
getByName("staging") {
    initWith(getByName("debug"))
    applicationIdSuffix = ".staging"
    versionNameSuffix = "-staging"
}
```

#### Success Criteria

- [ ] PRs automatically lint, test, and build
- [ ] `git tag v1.0` triggers Play Store–ready signed release
- [ ] Staging APK available for internal testers
- [ ] ProGuard shrinks APK by 30%+

---

### Phase 5: Complete Incomplete Features (Product Completeness)

**Why:** Dead code and incomplete features undercut the perception of production readiness.

#### Changes Required

**5a. Wire up `AudioRecitationService`**

- Add play/pause/seek controls to `RecitationScreen`
- Use Media3 `MediaController` from the composable to control the service
- Show currently playing surah with progress

**5b. Wire up `PrayerNotificationWorker`**

- Call `PrayerNotificationWorker.schedule()` from `RafiqApp.onCreate()`
- Use actual prayer times from the day's schedule per-prayer (not static notification)
- Add notification channel for each prayer time
- Handle permission flow on API 33+

**5c. Add launcher icon**

- Generate adaptive icon (foreground + background layers)
- Add `ic_launcher_foreground.xml` (vector drawable)
- Add `ic_launcher_background.xml` (color resource)
- Confirm `@mipmap/ic_launcher` resolves correctly

#### Success Criteria

- [ ] Audio recitation works end-to-end: tap reciter → select surah → play
- [ ] Prayer notifications fire at actual prayer times
- [ ] Launcher icon appears correctly on home screen

---

### Phase 6: Performance & Production Readiness (Polish)

**Why:** Performance optimization and production hardening shows you care about the user experience at scale.

#### Changes Required

**6a. Compose stability audit**

- Audit all `@Composable` params for stability:
  - `data class` → stable (good)
  - `List<AyahData>` → unstable unless `@Immutable` annotation is added
- Add `@Stable` / `@Immutable` where appropriate
- Add `remember` for expensive computations

**6b. Lazy loading for large lists**

- Surah list: already uses `LazyColumn` (good)
- Ayah list: already uses `itemsIndexed` (good)
- Bookmarks: verify `BookmarkListFullScreen` uses lazy loading
- Prophet detail: no list, single item (OK)

**6c. Image caching**

- No images currently loaded from network (good)
- If OsmDroid tiles are used: ensure tile caching is configured (already uses `osmdroidTileCache`)

**6d. Database indexing**

- Add `@Index` on `BookmarkEntity.sura` (queried in `getAyasBySura`)
- Add `@Index` on `PrayerLogEntity.date` (queried in `getLogForDate`)

**6e. Accessibility**

- Add `contentDescription` to all icons (currently only `Favorite` has it)
- Ensure `Text` composables have sufficient contrast ratios
- Test with TalkBack on emulator

**6f. i18n extraction**

- Extract all hardcoded strings to `strings.xml` (English)
- Create `values-id/strings.xml` for Bahasa Indonesia translations
- Create `values-ar/strings.xml` for Arabic translations (Arabic names, Islamic terms)

#### Success Criteria

- [ ] Compose layout inspector shows no unnecessary recompositions on AyahScreen
- [ ] Database queries return in < 10ms with new indices
- [ ] TalkBack reads all interactive elements correctly
- [ ] All user-facing strings are in `strings.xml`
- [ ] Bahasa Indonesia locale switches all app text

---

## Testing Strategy

| Layer | Tool | Target Coverage | CI Gate |
|-------|------|----------------|---------|
| Unit: ViewModel | JUnit 5 + MockK + kotlinx-coroutines-test | 70%+ | `testDebug` |
| Unit: Repository | JUnit 5 + MockK + file-based fixtures | 60%+ | `testDebug` |
| Unit: Use Case | JUnit 5 + MockK | 90%+ | `testDebug` |
| Integration: DAO | Room in-memory + AndroidX Test | 80%+ | `connectedDebugAndroidTest` |
| Integration: UI | Compose Test + Espresso | 40%+ critical paths | `connectedDebugAndroidTest` |
| E2E: Navigation | Compose Test (full flow) | Smoke tests | Manual / nightly |

**Test doubles strategy:**
- ViewModels: 100% mocked dependencies
- Repositories: mocked local/remote data sources, real JSON fixtures for parsing tests
- DAOs: real in-memory Room database
- UI: real Compose tree with mocked ViewModel (via `hiltViewModel()` override)

---

## Performance Considerations

| Concern | Mitigation |
|---------|-----------|
| Multi-module build speed | Gradle build cache + configuration cache (already enabled); parallel module builds |
| Compose recomposition | `@Stable`/`@Immutable` annotations on ViewModel state; `key()` on `itemsIndexed` |
| DB query speed | Room indices on queried columns; `@Transaction` for compound operations |
| APK size | ProGuard shrinking (Phase 4); resource optimization (WebP images) |
| Memory | Verify no bitmap leaks in OsmDroid; use `coil` or `glide` if images added later |
| Startup time | Baseline profiles (Android 12+); lazy-initialize Hilt components |
| Network | Add `HttpLogger` debuggable-only; add timeout constants to `OkHttpClient` |

---

## Summary: Portfolio Value Per Phase

| Phase | Portfolio Signal |
|-------|-----------------|
| **1: Modularization** | "I can architect apps that scale to teams" |
| **2: Testing** | "I ship quality code with measurable coverage" |
| **3: Error Handling** | "I build reliable, user-friendly software" |
| **4: CI/CD** | "I own the full lifecycle from commit to Play Store" |
| **5: Feature Completion** | "I finish what I start" |
| **6: Performance** | "I optimize for real users on real devices" |

**Recommendation:** Start with Phase 1 + Phase 2 in parallel (they build on each other) — modularization enables testability, and tests prove the architecture works. Then Phase 4 (CI) to automate quality gates. Phases 3, 5, and 6 can proceed in any order depending on portfolio timing needs.
