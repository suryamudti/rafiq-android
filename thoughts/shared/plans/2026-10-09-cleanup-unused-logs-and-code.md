# Cleanup Unused Logs and Code Implementation Plan

## Overview

Over the development of Rafiq Quran Android, various debug log statements, root directory diagnostic logs, unused ViewModel/UseCase methods, unused imports, and obsolete string resources accumulated in the repository. This plan details the phased elimination of unused logs, dead code, unused imports, and unreferenced string resources while preserving core error handling and reusable design system components.

---

## Current State Analysis

- **Verbose Logs in Production Code**:
  - [`DatabaseCopier.kt`](file:///c:/Flutter/rafiq-app-android/data/src/main/kotlin/com/smiledev/rafiq_quran/core/DatabaseCopier.kt#L54-L80) contains 4 `android.util.Log.i` info traces logging asset copy progress, file sizes, and verification successes.
  - [`QuranRepositoryImpl.kt`](file:///c:/Flutter/rafiq-app-android/data/src/main/kotlin/com/smiledev/rafiq_quran/data/repository/QuranRepositoryImpl.kt#L230) contains an informational `android.util.Log.i("QuranRepository", "Opened translation DB: ...")`.
  - [`PrayerGuidanceRepositoryImplTest.kt`](file:///c:/Flutter/rafiq-app-android/data/src/test/kotlin/com/smiledev/rafiq_quran/data/repository/PrayerGuidanceRepositoryImplTest.kt#L32-L37) contains leftover debug `println(...)` calls.
- **Root Directory Leftovers**:
  - Over 20 untracked `.log` files (`build*.log`, `d*.log`, `install.log`, `task1_build.log`, `devices.log`, `avds.log`, `dumpsys.log`) and transient debug text files (`db_diff.txt`, `merge_output.txt`, `pr45_rafiqapp.txt`) reside in the root directory.
  - [`.gitignore`](file:///c:/Flutter/rafiq-app-android/.gitignore) lacks `*.log` ignore pattern.
- **Dead Methods**:
  - [`AyahViewModel.clearAyahs()`](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq_quran/ui/quran/AyahViewModel.kt#L193-L195): Never referenced across the codebase.
  - [`AyahViewModel.hideTranslation()`](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq_quran/ui/quran/AyahViewModel.kt#L293-L295): Never referenced; translation toggle is managed via `revealTranslation`.
  - [`GetSunnahGuidanceUseCase.getByCategory()`](file:///c:/Flutter/rafiq-app-android/domain/src/main/kotlin/com/smiledev/rafiq_quran/domain/usecase/GetSunnahGuidanceUseCase.kt#L20-L22): Never invoked; filtering is done in-memory in `SunnahGuidanceViewModel`.
- **Unused Imports**:
  - 50 confirmed unused imports across 27 files in `app`, `data`, and `domain` modules (e.g. 12 unused imports in [`AsmaulHusnaScreen.kt`](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq_quran/ui/asmaulhusna/AsmaulHusnaScreen.kt)).
- **Unused String Resources**:
  - 54 string resources in [`strings.xml`](file:///c:/Flutter/rafiq-app-android/app/src/main/res/values/strings.xml) and [`values-id/strings.xml`](file:///c:/Flutter/rafiq-app-android/app/src/main/res/values-id/strings.xml) that are no longer referenced in code or XML layouts.

---

## Desired End State

1. **Repository & Git Hygiene**: Root directory is free of test/build `.log` files, and `.gitignore` prevents future command logs from polluting `git status`.
2. **Production Logging**: Informational log noise is removed from production code; essential warning and error diagnostics (`Log.w`, `Log.e`) are retained for critical failure cases (database corruptions or copy errors).
3. **Dead Code Removed**: Unreferenced methods in ViewModels and UseCases are eliminated without breaking compilation or tests.
4. **Clean Imports**: All 50 unused imports across the 27 source files are removed.
5. **Cleaned Resources**: Obsolete strings in `values/strings.xml` and `values-id/strings.xml` are pruned, reducing resource bloat.
6. **Design System Kept Intact**: Foundational UI components in `ui/designsystem/` remain preserved for future feature additions.

### Key Discoveries & Constraints:
- Production error logging (`Log.e`) in [`DatabaseCopier.kt`](file:///c:/Flutter/rafiq-app-android/data/src/main/kotlin/com/smiledev/rafiq_quran/core/DatabaseCopier.kt#L26) and [`QuranRepositoryImpl.kt`](file:///c:/Flutter/rafiq-app-android/data/src/main/kotlin/com/smiledev/rafiq_quran/data/repository/QuranRepositoryImpl.kt#L196) must be preserved because translation and Quran assets are unpacked dynamically from APK assets at runtime, and errors during unpacking need to be visible in Logcat.
- Do NOT remove `SunnahGuidanceRepository.getSunnahByCategory()` interface or implementation test, only remove the uncalled UseCase convenience wrapper `GetSunnahGuidanceUseCase.getByCategory()` if dead.
- All unit tests (`.\gradlew testDebug`) must continue to pass cleanly.

---

## What We're NOT Doing

- We are NOT removing design system components (`RafiqDateBadge`, `RafiqTextField`, `RafiqButton` variants, `RafiqListItem`) as they form the reusable design system toolkit.
- We are NOT removing any active drawables (all 33 drawables are verified in use).
- We are NOT removing essential error logging (`Log.e`, `Log.w`) in `DatabaseCopier` and `QuranRepositoryImpl`.
- We are NOT altering business logic or UI behavior.

---

## Implementation Approach

The cleanup is executed across four targeted, verifiable phases:
1. **Phase 1: Build Artifacts & Git Hygiene**
2. **Phase 2: Unused Logging Cleanup & Standardization**
3. **Phase 3: Dead Code & Unused Imports Cleanup**
4. **Phase 4: Unused String Resources Cleanup**

---

## Phase 1: Build Artifacts & Git Hygiene

### Overview
Remove lingering `.log` files and transient text dumps from the project root, and update `.gitignore` so future logs are automatically ignored.

### Changes Required:

#### 1. `.gitignore`
**File**: [`.gitignore`](file:///c:/Flutter/rafiq-app-android/.gitignore)
**Changes**: Add `*.log` rule under a general log section.

```gitignore
# Logs
*.log
```

#### 2. Root Directory Cleanup
Remove untracked `.log` files:
`avds.log`, `build2.log`, `build3.log`, `build4.log`, `build5.log`, `build6.log`, `build7.log`, `build_crash.log`, `build_out.log`, `build_pr.log`, `build_verify.log`, `d2.log`, `d3.log`, `d4.log`, `d5.log`, `devices.log`, `dumpsys.log`, `install.log`, `launch.log`, `stop.log`, `task1_build.log`, `merge_output.txt`, `pr45_rafiqapp.txt`, `db_diff.txt`.

### Success Criteria:

#### Automated Verification:
- [ ] `git status --porcelain` does not contain any `.log` files.
- [ ] `git check-ignore -v test.log` confirms `*.log` is ignored by `.gitignore`.

#### Manual Verification:
- [ ] Root directory clean and organized.

---

## Phase 2: Unused Logging Cleanup & Standardization

### Overview
Eliminate noisy informational `Log.i` logs and test stdout `println` calls, keeping only essential error logging.

### Changes Required:

#### 1. `DatabaseCopier.kt`
**File**: [`data/src/main/kotlin/com/smiledev/rafiq_quran/core/DatabaseCopier.kt`](file:///c:/Flutter/rafiq-app-android/data/src/main/kotlin/com/smiledev/rafiq_quran/core/DatabaseCopier.kt)
**Changes**: Remove verbose `Log.i` calls:
- Line 54: `android.util.Log.i("DatabaseCopier", "Copying: asset=$assetPath -> $flatName")`
- Line 66: `android.util.Log.i("DatabaseCopier", "File size: ${dbFile.length()} bytes for $flatName")`
- Line 70: `android.util.Log.i("DatabaseCopier", "Successfully opened DB: $flatName")`
- Line 80: `android.util.Log.i("DatabaseCopier", "Verification passed for $flatName")`
Retain `Log.e` statements for failures and corrupted databases.

#### 2. `QuranRepositoryImpl.kt`
**File**: [`data/src/main/kotlin/com/smiledev/rafiq_quran/data/repository/QuranRepositoryImpl.kt`](file:///c:/Flutter/rafiq-app-android/data/src/main/kotlin/com/smiledev/rafiq_quran/data/repository/QuranRepositoryImpl.kt)
**Changes**: Remove informational `Log.i` call:
- Line 230: `android.util.Log.i("QuranRepository", "Opened translation DB: $fileKey")`
Retain `Log.w` and `Log.e` for copy retries and opening failures.

#### 3. `PrayerGuidanceRepositoryImplTest.kt`
**File**: [`data/src/test/kotlin/com/smiledev/rafiq_quran/data/repository/PrayerGuidanceRepositoryImplTest.kt`](file:///c:/Flutter/rafiq-app-android/data/src/test/kotlin/com/smiledev/rafiq_quran/data/repository/PrayerGuidanceRepositoryImplTest.kt)
**Changes**: Remove debug `println` statements:
- Line 32: `println("Error message: ${result.error}")`
- Line 37: `println("Loaded ${items.size} items successfully!")`

### Success Criteria:

#### Automated Verification:
- [ ] `git grep "Log\.i"` returns 0 matches in production code.
- [ ] `git grep "println"` returns 0 matches across the repository.
- [ ] Unit tests pass: `.\gradlew testDebug`

#### Manual Verification:
- [ ] Logcat output during app start and Quran reading is free of diagnostic noise.

---

## Phase 3: Dead Code & Unused Imports Cleanup

### Overview
Remove uncalled functions in ViewModels and UseCases, and clean up 50 confirmed unused imports across 27 source files.

### Changes Required:

#### 1. Dead Functions Removal
- **`AyahViewModel.kt`** ([`app/src/main/java/com/smiledev/rafiq_quran/ui/quran/AyahViewModel.kt`](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq_quran/ui/quran/AyahViewModel.kt)):
  - Remove `fun clearAyahs()` (lines 193-195)
  - Remove `fun hideTranslation()` (lines 293-295)
- **`GetSunnahGuidanceUseCase.kt`** ([`domain/src/main/kotlin/com/smiledev/rafiq_quran/domain/usecase/GetSunnahGuidanceUseCase.kt`](file:///c:/Flutter/rafiq-app-android/domain/src/main/kotlin/com/smiledev/rafiq_quran/domain/usecase/GetSunnahGuidanceUseCase.kt)):
  - Remove `fun getByCategory(category: SunnahCategory)` (lines 20-22)

#### 2. Unused Imports Cleanup (27 files, 50 imports)
Remove confirmed unused imports in:
- `app/src/main/java/com/smiledev/rafiq_quran/ui/asmaulhusna/AsmaulHusnaScreen.kt` (12 imports: `border`, `CircularProgressIndicator`, `TextField`, `TextFieldDefaults`, `TopAppBar`, `TopAppBarDefaults`, `Color`, `semantics`, `Font`, `FontFamily`, `displayMessage`, `AsmaulHusna`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/prayerlog/PrayerLogScreen.kt` (6 imports: `AnimatedVisibility`, `fadeIn`, `fadeOut`, `Color`, `TextAlign`, `RafiqBadge`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardScreen.kt` (4 imports: `PaddingValues`, `GridCells`, `LazyVerticalGrid`, `PrayerTimeEntry`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/sunnahguidance/SunnahGuidanceDetailScreen.kt` (3 imports: `background`, `width`, `clip`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/quran/AyahScreen.kt` (2 imports: `border`, `semantics`)
- `app/src/main/java/com/smiledev/rafiq_quran/service/PrayerNotificationWorker.kt` (1 import: `SystemClock`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/bookmarks/BookmarkListScreen.kt` (1 import: `Box`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/designsystem/appbar/RafiqTopAppBar.kt` (1 import: `dp`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/designsystem/arabic/RafiqArabicText.kt` (1 import: `RafiqTheme`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/designsystem/badge/RafiqBadge.kt` (1 import: `dp`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/designsystem/badge/RafiqNumberBadge.kt` (1 import: `RafiqTheme`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/designsystem/input/RafiqSearchBar.kt` (1 import: `dp`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/designsystem/list/RafiqListItem.kt` (1 import: `Arrangement`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/hadith/HadithListScreen.kt` (1 import: `HadithBook`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/prayerguidance/PrayerGuidanceScreen.kt` (1 import: `width`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/prayerlog/PrayerLogViewModel.kt` (1 import: `Result`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/prayertimes/PrayerTimesScreen.kt` (1 import: `TextButton`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/recitation/RecitationScreen.kt` (1 import: `Surah`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreen.kt` (1 import: `LocalContext`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/tasbih/TasbihScreen.kt` (1 import: `Locale`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/tasbih/history/TasbihHistoryScreen.kt` (1 import: `Date`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/tasbih/history/TasbihHistoryViewModel.kt` (1 import: `TasbihHistoryItem`)
- `app/src/main/java/com/smiledev/rafiq_quran/ui/zakat/ZakatCalculatorScreen.kt` (1 import: `RoundedCornerShape`)
- `data/src/main/kotlin/com/smiledev/rafiq_quran/data/DomainMappers.kt` (1 import: `Surah`)
- `data/src/main/kotlin/com/smiledev/rafiq_quran/data/repository/QuranRepositoryImpl.kt` (1 import: `JSONArray`)
- `app/src/androidTest/java/com/smiledev/rafiq_quran/ui/hadith/HadithBooksScreenTest.kt` (1 import: `hasClickAction`)
- `app/src/test/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardVisibilityTest.kt` (1 import: `assertFalse`)
- `data/src/test/kotlin/com/smiledev/rafiq_quran/data/repository/HadithRepositoryImplTest.kt` (1 import: `every`)

### Success Criteria:

#### Automated Verification:
- [ ] No compilation or symbol resolution errors: `.\gradlew compileDebugKotlin`
- [ ] All unit tests pass: `.\gradlew testDebug`
- [ ] Automated unused imports check script reports 0 unused imports.

#### Manual Verification:
- [ ] Code cleanliness verified in modified files.

---

## Phase 4: Unused String Resources Cleanup

### Overview
Prune the 54 obsolete, unreferenced string resources from both `values/strings.xml` and `values-id/strings.xml` to prevent dead strings from accumulating.

### Changes Required:

#### 1. English Strings
**File**: [`app/src/main/res/values/strings.xml`](file:///c:/Flutter/rafiq-app-android/app/src/main/res/values/strings.xml)
**Changes**: Remove 54 unreferenced strings:
- `alhamdulillah`, `allahu_akbar`, `subhanallah`
- `asmaul_husna_grid_view`, `asmaul_husna_list_view`
- `bookmark_ayah`, `bookmark_ayah_bookmarked`, `bookmark_sura_aya`
- `calculate`, `cash`, `cash_amount`, `currency`
- `clear_search`
- `default_city_jakarta`
- `explore_sunnah_desc`, `explore_sunnah_guidance`
- `gold`, `gold_weight`, `silver`, `silver_weight`, `grams`
- `greeting_afternoon`, `greeting_evening`, `greeting_morning`, `greeting_night`
- `hadith_dalil_label`, `hadith_no_arabic`
- `hijri_date`, `jump_marker`
- `next_surah`, `previous_surah`
- `nisab`, `no_ayahs_loaded`, `no_chapters_found`, `no_events_this_month`
- `playing_ayah_title`
- `prayer_guidance_desc`
- `sajda_badge`, `search_chapters`, `share`
- `status_upcoming`, `step_number`, `stop`, `stop_audio`
- `surah_dalil_label`, `tap_for_details`, `tasbih_total`, `today`
- `total_zakat`, `you_are_not_obligated`, `you_are_obligated`, `your_location`, `zakat_due`, `zakat_not_due`

#### 2. Indonesian Strings
**File**: [`app/src/main/res/values-id/strings.xml`](file:///c:/Flutter/rafiq-app-android/app/src/main/res/values-id/strings.xml)
**Changes**: Mirror removals for the exact matching keys to maintain resource synchronization.

### Success Criteria:

#### Automated Verification:
- [ ] Compilation and resource packaging succeed: `.\gradlew assembleDebug`
- [ ] Unit tests pass: `.\gradlew testDebug`
- [ ] Resource scanner script reports 0 unused strings in `strings.xml`.

#### Manual Verification:
- [ ] All app screens (Dashboard, Quran, Prayer Times, Zakat Calculator, Asmaul Husna, Tasbih) render without missing resource string crashes.

---

## Testing Strategy

### Unit Tests:
- Run `.\gradlew testDebug` after each phase to guarantee no regression.
- Ensure all ViewModel, Repository, and UseCase tests execute and pass without failure.

### Build Verification:
- Run `.\gradlew assembleDebug` to guarantee resource packaging, Kotlin compilation, and KAPT stubs build cleanly.

---

## References
- [`DatabaseCopier.kt`](file:///c:/Flutter/rafiq-app-android/data/src/main/kotlin/com/smiledev/rafiq_quran/core/DatabaseCopier.kt)
- [`QuranRepositoryImpl.kt`](file:///c:/Flutter/rafiq-app-android/data/src/main/kotlin/com/smiledev/rafiq_quran/data/repository/QuranRepositoryImpl.kt)
- [`AyahViewModel.kt`](file:///c:/Flutter/rafiq-app-android/app/src/main/java/com/smiledev/rafiq_quran/ui/quran/AyahViewModel.kt)
- [`GetSunnahGuidanceUseCase.kt`](file:///c:/Flutter/rafiq-app-android/domain/src/main/kotlin/com/smiledev/rafiq_quran/domain/usecase/GetSunnahGuidanceUseCase.kt)
- [`strings.xml`](file:///c:/Flutter/rafiq-app-android/app/src/main/res/values/strings.xml)
- [`.gitignore`](file:///c:/Flutter/rafiq-app-android/.gitignore)
