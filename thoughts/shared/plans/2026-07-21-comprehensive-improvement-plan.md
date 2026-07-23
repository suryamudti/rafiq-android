# Comprehensive Improvement Plan — Rafiq Android

## Overview

Transform Rafiq Android from a functional single-module app into a **production-quality, multi-module, lead-engineer-grade Android codebase**. Every phase targets a specific portfolio signal: architectural rigor, testing culture, CI/CD automation, security awareness, accessibility, and product completeness.

---

## Current State Analysis

### What's Good (Preserve)

| Area | Strength |
|------|----------|
| **Stack** | Modern: Compose + Material3 + Hilt + Room + Nav3 + DataStore + Kotlin 2.0 |
| **Architecture Pattern** | Consistent Screen/ViewModel/UiState across all 15 features |
| **Navigation** | Nav3 type-safe routes with `@Serializable NavKey` tokens |
| **Dependency Injection** | Proper Hilt `@HiltViewModel` + `@Inject constructor` pattern |
| **Preferences** | DataStore (not SharedPreferences) |
| **Offline Quran** | Asset DBs with copy/verify/retry in `DatabaseCopier` |
| **Maps** | OsmDroid with proper cache config |
| **Theming** | Material3 dynamic color + custom Islamic palette |
| **Security** | No hardcoded secrets, no API keys committed |
| **Accessibility Icons** | All 5 `Icon()` composables have `contentDescription` |

### Critical Gaps

| Area | Issue | Severity |
|------|-------|----------|
| **Architecture** | Single `:app` module, no domain layer, no repository interfaces, no use cases | High |
| **Testing** | 7 test cases across 50+ files (2 test files) — 0.14 tests/file | High |
| **Error Handling** | `e.message` shown raw in 7 ViewModels, 8 screens | High |
| **CI/CD** | `.github/workflows/` is empty — only docs, no running pipelines | High |
| **Incomplete Features** | `AudioRecitationService` never wired; `PrayerNotificationWorker` never scheduled | High |
| **Build** | `isMinifyEnabled = false`, no build variants | Medium |
| **Security** | SQL injection in `QuranRepository.getTranslationForSura()` via `$suraNumber` interpolation | Medium |
| **Bug** | `Dhuha` mapped to `Imsak` time in `PrayerTimesViewModel` | Medium |
| **Bug** | `PrayerTimesScreen` uses `Imsak` time for Dhuha display | Medium |
| **Performance** | No `@Stable`/`@Immutable` annotations; 4 parallel `collect` blocks in `SettingsViewModel` | Medium |
| **i18n** | ~85+ hardcoded strings; only `app_name` in `strings.xml`; no locale resources | Medium |
| **Accessibility** | No `android:importantForAccessibility`, no TalkBack pass | Low |
| **OkHttp** | No connect/read/write timeouts configured | Low |

---

## Desired End State

A **multi-module, clean-architecture Android app** demonstrating:

1. **Multi-module build**: `:core`, `:domain`, `:data`, `:feature:*`, `:app`
2. **Domain layer**: Repository interfaces + Use Cases, zero Android dependencies
3. **Error handling**: `Result<T, AppError>` sealed class across all data operations
4. **80%+ line coverage** on critical paths (ViewModels, Repositories, Use Cases)
5. **GitHub Actions CI**: lint → test → build on every PR; tag-driven release
6. **ProGuard/R8** minification, staging build variant
7. **Security-hardened**: No SQL injection, no raw exception exposure
8. **Feature-complete**: Audio recitation + prayer notifications wired end-to-end
9. **i18n**: English + Indonesian strings, RTL support verified
10. **Accessibility pass**: TalkBack-friendly UI

---

## What We're NOT Doing

- **Not replacing Nav3** — architecture choice, stable, no portfolio value in churn
- **Not replacing Room for Quran DBs** — Room for read-only asset DBs adds overhead without benefit
- **Not replacing OsmDroid with Google Maps** — valid choice, no replacement needed
- **Not adding server-side features** — Android portfolio, not full-stack
- **Not migrating to KSP** — Hilt 2.56.2 requires KAPT; KSP migration is a separate effort with no user-facing value
- **Not rewriting UI** — Compose patterns are sound; only annotations and accessibility need adding
- **Not adding Play Store publishing automation** — signing keys are personal; guide docs instead

---

## Implementation Approach

**Three parallel tracks:**

1. **Structural (Foundations)** — Multi-module refactor, domain layer, `Result<T,E>`, `DispatcherProvider` — enables everything else
2. **Quality (Gates)** — Testing, CI/CD, ProGuard — automated quality signals
3. **Product (Visible)** — Error handling, incomplete features, security fixes, i18n, accessibility, performance — what users and interviewers see

---

## Phases

---

### Phase 1: Modularization & Domain Layer (Foundations)

**Portfolio Signal:** "I can architect apps that scale to teams"

#### Changes Required

**1a. Module creation** — restructure from monolith:

```
rafiq-android/
  build.gradle.kts                   # Root: plugin declarations
  settings.gradle.kts                # Include all modules
  gradle/libs.versions.toml          # Already exists, shared
  :core/
    build.gradle.kts                 # Pure Kotlin (no Android plugin)
    src/main/kotlin/
      com/smiledev/rafiq/core/
        Result.kt                    # sealed class Result<T, E>
        AppError.kt                  # sealed interface AppError
        DispatcherProvider.kt        # Interface for testable dispatchers
        retryIO.kt                   # Retry utility
  :domain/
    build.gradle.kts                 # Pure Kotlin (no Android plugin)
    src/main/kotlin/
      com/smiledev/rafiq/domain/
        repository/                  # Interfaces only
          QuranRepository.kt
          PrayerTimesRepository.kt
          MetalPriceRepository.kt
          BookmarkRepository.kt
          PrayerLogRepository.kt
          IslamicCalendarRepository.kt
          ProphetRepository.kt
          ReciterRepository.kt
          AsmaulHusnaRepository.kt
        usecase/
          GetAyahsWithTranslationUseCase.kt
          CalculateZakatUseCase.kt
          GetPrayerTimesUseCase.kt
          CalculateQiblaUseCase.kt
        model/                       # Domain models (no Android annotations)
          Surah.kt
          Ayah.kt
          PrayerTimings.kt
  :data/
    build.gradle.kts                 # Android library
    src/main/kotlin/
      com/smiledev/rafiq/data/
        repository/                  # Implementations of domain interfaces
          QuranRepositoryImpl.kt
          PrayerTimesRepositoryImpl.kt
          MetalPriceRepositoryImpl.kt
          ...
        local/                       # Room DBs (BookmarkDatabase, PrayerLogDatabase)
        remote/                      # Retrofit APIs
        preferences/                 # PreferencesManager
  :feature:quran/
    build.gradle.kts                 # Android library, depends on :domain + :core
  :feature:prayertimes/
  :feature:qibla/
  :feature:settings/
  :feature:dashboard/
  :feature:mosques/
  :feature:bookmarks/
  :feature:prayerlog/
  :feature:zakat/
  :feature:tasbih/
  :feature:asmaulhusna/
  :feature:prophets/
  :feature:recitation/
  :feature:calendar/
  :app/                              # Thin shell: DI wiring, navigation host, Application
```

**1b. Repository interfaces in `:domain`**

Move current classes to interfaces:
- `QuranRepository` → interface in `:domain`; existing class → `QuranRepositoryImpl` in `:data`
- `PrayerTimesRepository` → interface in `:domain`; existing class → `PrayerTimesRepositoryImpl` in `:data`
- Same for: `MetalPriceRepository`, all other repositories

**1c. Use Cases in `:domain`**

Extract business logic from ViewModels:
- `CalculateZakatUseCase` — from `ZakatCalculatorViewModel.calculate()` (app/Nisab math, currency conversion)
- `GetAyahsWithTranslationUseCase` — enriches ayahs with metadata
- `CalculateQiblaUseCase` — bearing/distance math from `QiblaViewModel`
- `GetPrayerTimesUseCase` — orchestrates API fetch + countdown math

**1d. `Result<T, AppError>` in `:core`**

```kotlin
sealed class Result<out T, out E : AppError> {
    data class Success<T>(val data: T) : Result<T, Nothing>()
    data class Error<E : AppError>(val error: E) : Result<Nothing, E>()
}
sealed interface AppError {
    data class Network(val message: String, val cause: Throwable?) : AppError
    data class Database(val message: String, val cause: Throwable?) : AppError
    data object NotFound : AppError
    data class Unknown(val message: String) : AppError
}
```

**1e. `DispatcherProvider` in `:core`**

```kotlin
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
// DefaultDispatcherProvider — @Inject @Singleton
// TestDispatcherProvider — StandardTestDispatcher-based
```

**1f. Move existing `DatabaseCopier` to `:core`**

Already in `core/` package; keep it but remove `copyAllQuranDatabases()` (unused dead code).

**1g. Move `PreferencesManager` to `:data:preferences`**

Keep as-is; it already has no domain layer dependency.

#### Success Criteria

- [ ] `gradle projects` shows 5+ modules (including feature modules)
- [ ] `:domain` has zero Android/Context dependencies
- [ ] `:core` has zero Android/Context dependencies
- [ ] All ViewModels inject `:domain` interfaces, not `:data` implementations
- [ ] Build completes successfully: `.\gradlew assembleDebug`
- [ ] All 15 navigation routes still work

---

### Phase 2: Testing Overhaul (Quality Signal)

**Portfolio Signal:** "I ship quality code with measurable coverage"

#### Changes Required

**2a. Test framework setup**
- Add JaCoCo plugin to `:core`, `:domain`, `:data`, and feature modules
- Add `TestDispatcherProvider` implementation to test source sets
- Add test reporting configuration

**2b. ViewModel tests (highest priority)**

| ViewModel | Tests to Write (min) | Key Scenarios |
|-----------|---------------------|---------------|
| `QuranViewModelTest` | 5 | Load surahs, load ayahs, toggle bookmark, missing data error, font size settings apply |
| `PrayerTimesViewModelTest` | 6 | Load times, countdown ticks, prev/next day, location changes, API error, non-200 response |
| `ZakatCalculatorViewModelTest` | 6 | Expand existing: gold above/below nisab, silver, cash, IDR conversion, empty input, network error |
| `SettingsViewModelTest` | 4 | Theme change, translation language, ayah font size, translation font size |
| `QiblaViewModelTest` | 3 | Bearing calculation, distance calculation, coordinates update |
| `TasbihViewModelTest` | 4 | Increment, decrement, reset, max cap |
| `MosquesViewModelTest` | 3 | Location permission granted, denied, preferences fallback |
| `BookmarkListViewModelTest` | 3 | Load bookmarks, delete bookmark, empty state |
| `PrayerLogViewModelTest` | 3 | Toggle prayer, load logs, date-based query |
| `AsmaulHusnaViewModelTest` | 3 | Load names, search/filter, error state |

**2c. Repository tests**

| Repository | Tests | Key Scenarios |
|------------|-------|---------------|
| `QuranRepositoryImplTest` | 5 | Load surahs from mock JSON, load ayahs with translation, missing translation fallback, metadata caching, SQL injection safe usage |
| `PrayerTimesRepositoryImplTest` | 4 | Success response parsing, network error, HTTP error, malformed JSON |
| `MetalPriceRepositoryImplTest` | 3 | Price parsing, empty response fallback, network error |
| `BookmarkRepositoryImplTest` | 4 | CRUD operations, flow emission, empty state |
| `PrayerLogRepositoryImplTest` | 3 | Upsert, query by date, flow ordering |

**2d. Use case tests**

| Use Case | Tests | Key Scenarios |
|----------|-------|---------------|
| `CalculateZakatUseCaseTest` | 5 | Gold nisab boundary, silver nisab boundary, cash nisab, IDR conversion, zero input |
| `CalculateQiblaUseCaseTest` | 3 | Known coordinates (Makkah→Makkah = 0), Jakarta bearing, antipodal point |
| `GetAyahsWithTranslationUseCaseTest` | 3 | Cache hit, cache miss, missing translation |

**2e. DAO instrumented tests**

| DAO | Tests | Key Scenarios |
|-----|-------|---------------|
| `BookmarkDaoTest` | 4 | Insert, delete, getAyasBySura, isBookmarked, getAllFlow emits |
| `PrayerLogDaoTest` | 3 | Upsert, getLogForDate, getAllLogs flow |

Use Room in-memory database with `AndroidJUnit4` runner.

**2f. UI Compose tests (smoke)**

| Screen | Tests |
|--------|-------|
| `DashboardScreenTest` | All 12 feature cards visible, settings icon clickable |
| `AyahScreenTest` | Ayah list renders, translation shown, bookmark dialog appears |
| `SettingsScreenTest` | Radio button selection, slider interaction |
| `TasbihScreenTest` | Counter display, increment on tap |

**2g. Coverage targets**
- `:core`: 90%+
- `:domain`: 90%+ (use cases)
- `:data/repository`: 70%+
- Feature ViewModels: 70%+
- Overall project: 60%+

#### Success Criteria

- [ ] `./gradlew testDebug` runs 50+ test cases (from 7 today)
- [ ] `./gradlew jacocoTestReport` produces coverage report
- [ ] All ViewModels have at minimum: success, loading, and error state tests
- [ ] Coverage badge(s) added to README

---

### Phase 3: CI/CD Pipeline (DevOps)

**Portfolio Signal:** "I own the full lifecycle from commit to Play Store"

#### Changes Required

**3a. PR check workflow**

`.github/workflows/pr-check.yml`:
```yaml
name: PR Check
on: pull_request
jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'zulu', java-version: '17', cache: 'gradle' }
      - run: ./gradlew lintDebug
  test:
    needs: lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'zulu', java-version: '17', cache: 'gradle' }
      - run: chmod +x gradlew
      - run: ./gradlew testDebug
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'zulu', java-version: '17', cache: 'gradle' }
      - run: chmod +x gradlew
      - run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with: { name: apk-debug, path: app/build/outputs/apk/debug/*.apk }
```

**3b. Release workflow (tag-driven)**

`.github/workflows/release.yml` — based on existing `GITHUB_WORKFLOWS.md` but trigger on tags:
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
        with: { distribution: 'zulu', java-version: '17', cache: 'gradle' }
      - run: chmod +x gradlew
      - run: ./gradlew assembleRelease bundleRelease -PversionCode=${{ github.run_number }}
      # Sign and release
      - uses: softprops/action-gh-release@v2
        with:
          files: |
            app/build/outputs/apk/release/*.apk
            app/build/outputs/bundle/release/*.aab
```

#### Success Criteria

- [ ] PRs automatically lint, test, and build (green check on every PR)
- [ ] `git tag v1.0` triggers signed release build
- [ ] APK artifact available on every PR
- [ ] Build badge in README

---

### Phase 4: Error Handling Overhaul (Reliability)

**Portfolio Signal:** "I build reliable, user-friendly software"

#### Changes Required

**4a. Adopt `Result<T, AppError>` across all repositories**

Migrate every public repository method from nullable/exceptions to `Result<T, AppError>`:

| File | Method | Current Return | New Return |
|------|--------|---------------|------------|
| `QuranRepository.getChapters()` | `List<Surah>` | `Result<List<Surah>, AppError>` |
| `QuranRepository.getAyahsWithTranslation()` | `List<AyahData>` | `Result<List<AyahData>, AppError>` |
| `PrayerTimesRepository.fetchPrayerTimes()` | `PrayerTimesResponse` | `Result<PrayerTimesResponse, AppError>` |
| `MetalPriceRepository.getGoldPricePerGram()` | `Double` | `Result<Double, AppError>` |
| ... and all others |

**4b. Add retry utility to `:core`**

```kotlin
// core/retryIO.kt
suspend fun <T> retryIO(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> Result<T, AppError>
): Result<T, AppError>
```

Apply to all network-bound repository calls (Aladhan, MetalPrice).

**4c. Migrate ViewModels to `Result<T, AppError>`**

Before (current pattern everywhere):
```kotlin
_uiState.value = _uiState.value.copy(isLoading = true, error = null)
try {
    val data = repository.getData()
    _uiState.value = _uiState.value.copy(data = data, isLoading = false)
} catch (e: Exception) {
    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
}
```

After:
```kotlin
_uiState.value = _uiState.value.copy(isLoading = true, error = null)
when (val result = repository.getData()) {
    is Result.Success -> _uiState.value = _uiState.value.copy(data = result.data, isLoading = false)
    is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
}
```

**4d. User-friendly error messages in screens**

Replace `Text("Error: ${state.error}")` with structured error rendering:

```kotlin
when (val error = state.error) {
    is AppError.Network -> Text("Unable to connect. Please check your internet.")
    is AppError.Database -> Text("Something went wrong. Please restart the app.")
    is AppError.NotFound -> Text("The requested data was not found.")
    else -> Text("An unexpected error occurred. Please try again.")
}
```

Affected screens (8 total):
- `PrayerTimesScreen.kt:69`
- `QuranScreen.kt:84`
- `AyahScreen.kt:143`
- `AsmaulHusnaScreen.kt:69`
- `IslamicCalendarScreen.kt:73`
- `RecitationScreen.kt:68`
- `ZakatCalculatorScreen.kt:137`
- `ProphetsScreen.kt:134`

**4e. Add pull-to-refresh to data screens**

Add `PullToRefreshBox` (Material3) to:
- Quran surah list
- Prayer Times screen
- Bookmark list
- Prayer Log

Each triggers ViewModel's `refresh()` method.

**4f. Remove `error: String?` from UiState, replace with `error: AppError?`**

All 7 UiState data classes change `error: String?` to `error: AppError?`.

#### Success Criteria

- [ ] Zero instances of `e.message` shown to users
- [ ] Every network call has retry logic (3 attempts with exponential backoff)
- [ ] Pull-to-refresh on all data screens
- [ ] Error states show actionable messages, not stack traces or internals
- [ ] `MosquesViewModel` now shows errors instead of silently falling back

---

### Phase 5: Security Hardening & Bug Fixes

**Portfolio Signal:** "I write secure, correct code"

#### Changes Required

**5a. Fix SQL injection in `QuranRepository.getTranslationForSura()`**

File: `app/src/main/java/.../data/repository/QuranRepository.kt:127-132`

Before:
```kotlin
"SELECT ayah, text FROM verses WHERE sura = $suraNumber"
```

After:
```kotlin
"SELECT ayah, text FROM verses WHERE sura = ?"
// Use selectionArgs: arrayOf(suraNumber.toString())
```

This is the **only** unparameterized SQL query in the codebase. The query on line 58 is already parameterized — use the same pattern.

**5b. Fix Dhuha time mapping bug**

File: `app/src/main/java/.../ui/prayertimes/PrayerTimesViewModel.kt:90`

Before:
```kotlin
PrayerTimeEntry("Dhuha", t.Imsak),
```

`Dhuha` (mid-morning, ~20 min after sunrise) is incorrectly mapped to `Imsak` (pre-dawn, ~10 min before Fajr). 

After:
```kotlin
// Calculate Dhuha as Sunrise + 20 minutes (approximate)
val dhuhaTime = calculateDhuhaTime(t.Sunrise)
PrayerTimeEntry("Dhuha", dhuhaTime),
```

Or simply show the sunrise time with Dhuha label:
```kotlin
PrayerTimeEntry("Dhuha", t.Sunrise),
```

**5c. Fix inconsistent locale check**

File: `app/src/main/java/.../ui/quran/QuranViewModel.kt:43`
```kotlin
private val localeCode = if (Locale.getDefault().language == "in" || Locale.getDefault().language == "id") "id" else "en"
```

This checks `"in"` (deprecated Indonesian code) which is correct, but `AyahScreen.kt:269` uses `"in"`:
```kotlin
if (java.util.Locale.getDefault().language == "in" || java.util.Locale.getDefault().language == "id") "id" else "en"
```

Standardize to a shared utility function in `:core`:
```kotlin
fun currentLocaleCode(): String = if (Locale.getDefault().language.let { it == "id" || it == "in" }) "id" else "en"
```

**5d. Sanitize preferences writes (defense in depth)**

Add validation in `PreferencesManager`:
- `setLatitude`: validate with regex `^-?\d{1,3}\.\d+$` before storing
- `setLongitude`: same pattern
- `setCityName`: trim and strip non-printable characters

**5e. Add OkHttp timeout configuration**

File: `app/src/main/java/.../di/AppModule.kt:64-70`

Before:
```kotlin
fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = BASIC })
        .build()
}
```

After:
```kotlin
fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) BASIC else NONE
        })
        .build()
}
```

**5f. Remove unused `copyAllQuranDatabases()` method**

`DatabaseCopier.kt:75-83` — this method is never called. Remove it.

#### Success Criteria

- [ ] No SQL string interpolation in raw queries
- [ ] Dhuha shows correct post-sunrise time, not pre-dawn Imsak
- [ ] Locale code check is consistent across the app
- [ ] OkHttp has explicit timeouts on all connections
- [ ] No dead code in `DatabaseCopier`

---

### Phase 6: Feature Completion (Product Completeness)

**Portfolio Signal:** "I finish what I start"

#### Changes Required

**6a. Wire up `AudioRecitationService`**

Current state: `AudioRecitationService` is a Media3 `MediaSessionService` declared in `AndroidManifest.xml` but never started by any composable.

Implementation:
1. Create a `MediaController` wrapper/helper that binds to the service from `RecitationScreen`
2. Update `RecitationViewModel` to expose play/pause/seek state
3. Add play/pause UI controls to `RecitationScreen`
4. Show currently playing surah and progress indicator

**6b. Wire up `PrayerNotificationWorker`**

Current state: `PrayerNotificationWorker` has `schedule()` and `createNotificationChannel()` companion functions but neither is called.

Implementation:
1. Call `PrayerNotificationWorker.createNotificationChannel(this)` in `RafiqApp.onCreate()`
2. Call `PrayerNotificationWorker.schedule(this)` after channel creation
3. Enhance `doWork()` to check actual prayer times for the day (not just static notification) — query from `PrayerTimesRepository`
4. Schedule per-prayer notifications at actual prayer times using `setExactAndAllowWhileIdle` (not just a daily periodic worker)
5. Handle notification permission flow on API 33+ (POST_NOTIFICATIONS)

**6c. Add proper launcher icon**

Current state: `@mipmap/ic_launcher` referenced in manifest but may use default Android icon.

Implementation:
1. Create adaptive icon: foreground vector (`ic_launcher_foreground.xml`) + background color (`ic_launcher_background.xml`)
2. Place in `res/mipmap-anydpi-v26/`
3. Place fallback PNGs in `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/`

#### Success Criteria

- [ ] Audio recitation works end-to-end: browse reciters → select surah → play/pause/seek
- [ ] Prayer notifications fire at actual prayer times (not just once daily)
- [ ] Notification channel created on first launch
- [ ] Launcher icon is custom (not default Android icon)

---

### Phase 7: Performance & Polish

**Portfolio Signal:** "I optimize for real users on real devices"

#### Changes Required

**7a. Compose stability audit**

Files to annotate with `@Immutable` or `@Stable`:

| File | Class | Annotation |
|------|-------|------------|
| `data/models/AyahData.kt` | `AyahData` | `@Immutable` |
| `data/models/Surah.kt` | `Surah` | `@Immutable` |
| `ui/quran/QuranViewModel.kt` | `QuranUiState` | `@Immutable` |
| `ui/prayertimes/PrayerTimesViewModel.kt` | `PrayerTimesUiState` | `@Immutable` |
| All other UiState data classes | `*UiState` | `@Immutable` |

Add `@Stable` to any interface/class used as a composable parameter type.

**7b. Optimize ViewModel collect patterns**

`SettingsViewModel.kt:30-49`: 4 separate `viewModelScope.launch(Dispatchers.IO)` blocks collecting individual preferences.

After: merge into a single `combine()`:

```kotlin
viewModelScope.launch {
    combine(
        preferencesManager.themeMode,
        preferencesManager.translationLanguage,
        preferencesManager.ayahFontSize,
        preferencesManager.translationFontSize
    ) { theme, lang, ayahSize, transSize ->
        SettingsUiState(
            themeMode = theme,
            translationLanguage = lang,
            ayahFontSize = ayahSize,
            translationFontSize = transSize
        )
    }.collect { state ->
        _uiState.value = state
    }
}
```

Apply same pattern to `QuranViewModel.kt:47-61` (3 separate blocks for preferences).

**7c. Database indexing**

Add `@Index` to Room entities:
- `BookmarkEntity.sura`: `@Entity(tableName = "bookmarks", indices = [Index("sura")])`
- `PrayerLogEntity.date`: already `@PrimaryKey`, which has implicit index

**7d. i18n extraction**

1. Move all ~85 hardcoded strings to `values/strings.xml` (English)
2. Create `values-id/strings.xml` with Indonesian translations
3. Create `values-ar/strings.xml` for Arabic terms (Dhuhr, Maghrib, etc.)
4. Replace all `Text("string literal")` with `Text(stringResource(R.string.key))`

**7e. Remove `Dispatchers.IO` hardcoding — use `DispatcherProvider`**

Every `viewModelScope.launch(Dispatchers.IO)` should become `viewModelScope.launch(dispatcherProvider.io)`.

Affected files (12+ occurrences):
- `PrayerTimesViewModel.kt:54,76,113`
- `QuranViewModel.kt:47,52,57,65,77,96`
- `SettingsViewModel.kt:30,36,41,46`
- `QiblaViewModel.kt:35`
- `ProphetsViewModel.kt:36`
- `BookmarkListViewModel.kt:28`
- `PrayerLogViewModel.kt:24`
- `TasbihViewModel.kt:27`
- `RecitationViewModel.kt:30`

**7f. Content descriptions for all composables**

Even though all `Icon()` calls already have `contentDescription`, add:
- `contentDescription` to clickable Text fields used as buttons (e.g., "Back" texts)
- `contentDescription` to all `IconButton` icons
- State description for `CircularProgressIndicator`: `contentDescription = "Loading"`

#### Success Criteria

- [ ] Compose layout inspector shows no unnecessary recompositions on AyahScreen
- [ ] Database queries return in < 10ms with new indices
- [ ] `SettingsViewModel` uses single `combine()` instead of 4 `launch` blocks
- [ ] All `Dispatchers.IO` references replaced with `dispatcherProvider.io`
- [ ] English `strings.xml` contains all user-facing strings
- [ ] Indonesian `values-id/strings.xml` exists

---

## Testing Strategy

| Layer | Tool | Target Coverage | CI Gate |
|-------|------|----------------|---------|
| Unit: ViewModel | JUnit 4 + MockK + kotlinx-coroutines-test | 70%+ | `testDebug` |
| Unit: Repository | JUnit 4 + MockK + JSON fixtures | 60%+ | `testDebug` |
| Unit: Use Case | JUnit 4 + MockK | 90%+ | `testDebug` |
| Unit: Core | JUnit 4 | 90%+ | `testDebug` |
| Integration: DAO | Room in-memory + AndroidX Test | 80%+ | `connectedDebugAndroidTest` |
| Integration: UI | Compose Test + Espresso | 40% critical paths | `connectedDebugAndroidTest` |
| Static Analysis | `lintDebug` | — | pre-test CI gate |

**Test doubles strategy:**
- ViewModels: 100% mocked dependencies (MockK)
- Repositories: mocked local/remote data sources, real JSON fixtures for parsing tests
- DAOs: real in-memory Room database
- UI: real Compose tree with `createComposeRule()`, mock ViewModel via parameter injection

---

## Performance Considerations

| Concern | Mitigation |
|---------|-----------|
| Multi-module build speed | Gradle build cache + configuration cache (already enabled); parallel module builds |
| Compose recomposition | `@Immutable` on all UiState/Model data classes; `key()` on `itemsIndexed` |
| DB query speed | Room indices on `BookmarkEntity.sura`; parameterized queries |
| APK size | ProGuard shrinking (Phase 3); remove dead code `copyAllQuranDatabases()` |
| Startup time | Move notification channel creation to `Application.onCreate()` instead of first activity |
| Network | OkHttp timeouts prevent hanging connections; retryIO with exponential backoff |
| Memory | Verify OsmDroid tile cache configuration; no bitmap leaks |
| UI thread | All `Dispatchers.IO` replaced with injected `DispatcherProvider` for testability |

---

## Timeline Estimate

| Phase | Estimated Effort | Dependencies |
|-------|-----------------|--------------|
| **1: Modularization** | 3-5 days | None |
| **2: Testing** | 4-6 days | Phase 1 (enables clean tests) |
| **3: CI/CD** | 1 day | None (parallelizable) |
| **4: Error Handling** | 2-3 days | Phase 1 (Result type in :core) |
| **5: Security/Bugs** | 1 day | None (parallelizable) |
| **6: Feature Completion** | 2-3 days | None (parallelizable with 1, 2) |
| **7: Performance/Polish** | 2-3 days | Phase 1 (dependency injection changes) |

**Total: ~15-22 days** — but Phases 3, 5, 6 can run in parallel with 1-2.

---

## Portfolio Value Summary

| Phase | Interview Signal |
|-------|-----------------|
| **1: Modularization** | "I can architect apps that scale to teams" |
| **2: Testing** | "I ship quality code with measurable coverage" |
| **3: CI/CD** | "I own the full lifecycle from commit to production" |
| **4: Error Handling** | "I build reliable, user-friendly software" |
| **5: Security/Bugs** | "I write secure, correct code" |
| **6: Feature Completion** | "I finish what I start" |
| **7: Performance/Polish** | "I optimize for real users on real devices" |

---

## Open Questions (Resolved During Research)

All questions raised during the research phase have been answered:

1. **SQL injection risk?** Yes, one location: `QuranRepository.kt:130`
2. **AudioRecitationService wired?** No, declared in manifest but never started
3. **PrayerNotificationWorker scheduled?** No, `schedule()` never called
4. **Dhuha time correct?** No, mapped to `Imsak` instead of post-sunrise
5. **OkHttp timeouts?** Not configured — uses OkHttp defaults (infinite)
6. **Compose stability annotations?** None — no `@Stable`/`@Immutable` anywhere
7. **strings.xml?** Yes, but only contains app_name
8. **contentDescription on icons?** All 5 Icon calls have it (good)
9. **Build variants?** Debug only — no release/staging
10. **ProGuard?** Disabled

---

*Plan generated: 2026-07-21*
