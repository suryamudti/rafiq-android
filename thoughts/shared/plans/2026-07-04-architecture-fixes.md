# Architecture Fix Plan

## 1. Repository Layer for API Features

**Problem:** `PrayerTimesViewModel` calls `AladhanApi` directly. `ZakatCalculatorViewModel` calls `MetalPriceApi` directly. Inconsistent with Quran/Calendar/Prophets which use repositories.

**Fix:**
- Create `data/repository/PrayerTimesRepository.kt` wrapping `AladhanApi`
- Create `data/repository/MetalPriceRepository.kt` wrapping `MetalPriceApi`
- Inject repositories into ViewModels instead of API classes
- Remove direct API injection from `AppModule.kt`

**Files to touch:**
- `app/src/main/java/com/smiledev/rafiq/data/repository/PrayerTimesRepository.kt` (new)
- `app/src/main/java/com/smiledev/rafiq/data/repository/MetalPriceRepository.kt` (new)
- `app/src/main/java/com/smiledev/rafiq/ui/prayertimes/PrayerTimesViewModel.kt`
- `app/src/main/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorViewModel.kt`
- `app/src/main/java/com/smiledev/rafiq/di/AppModule.kt`

---

## 2. Remove Dead Code

**Problem:** `DataRepository` interface and `DefaultDataRepository` are unused stubs from the template.

**Fix:** Delete `data/DataRepository.kt`.

---

## 3. Islamic Color Palette

**Problem:** Theme uses generic Compose template colors (Purple80, Pink80).

**Fix:** Replace with app-specific colors inspired by Islamic art / mosque aesthetics. Keep dynamic color on Android 12+ as default, but make the fallback palette meaningful.

**Files to touch:**
- `app/src/main/java/com/smiledev/rafiq/theme/Color.kt`

---

## 4. Error/Loading State Audit

**Problem:** Missing or inconsistent error/loading handling across screens.

**Fix:** Audit every screen for:
- Loading indicator when data is being fetched
- Error display with retry option
- Empty state messaging

**Screens that need work:**
- `MosquesScreen` — handle OsmDroid init failure
- `TasbihScreen` — trivial (no network), already fine
- `QiblaScreen` — trivial (no network), already fine
- `ZakatCalculatorScreen` — already has error state, verify
- All others — verify existing states are adequate

---

## 5. ZakatCalculator Rotation Handling

**Problem:** Inputs lost on config change, API re-triggers on every recomposition when rotating.

**Fix:** Use `SavedStateHandle` in ViewModel to persist gold/silver/cash inputs across config changes. Only auto-calculate if not already calculated.

**Files to touch:**
- `app/src/main/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorViewModel.kt`
- `app/src/main/java/com/smiledev/rafiq/ui/zakat/ZakatCalculatorScreen.kt`

---

## 6. OsmDroid Location & Permissions

**Problem:** MosquesScreen shows a hardcoded Jakarta marker. No GPS request, no runtime permission handling.

**Fix:**
- Request `ACCESS_FINE_LOCATION` at runtime via `rememberLauncherForActivityResult`
- Use FusedLocationProviderClient to get user location
- Center map on user's location instead of hardcoded Jakarta
- Show "Location unavailable" fallback if permission denied

**Files to touch:**
- `app/src/main/java/com/smiledev/rafiq/ui/mosques/MosquesScreen.kt`

---

## 7. Missing Screens: BookmarkList + PrayerLog

**Problem:** Room databases and DAOs exist for `bookmarks.db` and `prayer_logs.db`, but no UI screens navigate to them.

**Fix:**
- Create `NavigationKeys` entries: `@Serializable data object BookmarkList : NavKey` and `@Serializable data object PrayerLog : NavKey`
- Create `ui/bookmarks/BookmarkListScreen.kt` + `ui/bookmarks/BookmarkListViewModel.kt`
- Create `ui/prayerlog/PrayerLogScreen.kt` + `ui/prayerlog/PrayerLogViewModel.kt`
- Wire them in `Navigation.kt` and `DashboardScreen.kt`

**Files to touch:**
- `app/src/main/java/com/smiledev/rafiq/NavigationKeys.kt`
- `app/src/main/java/com/smiledev/rafiq/Navigation.kt`
- `app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/smiledev/rafiq/ui/bookmarks/BookmarkListScreen.kt` (new)
- `app/src/main/java/com/smiledev/rafiq/ui/bookmarks/BookmarkListViewModel.kt` (new)
- `app/src/main/java/com/smiledev/rafiq/ui/prayerlog/PrayerLogScreen.kt` (new)
- `app/src/main/java/com/smiledev/rafiq/ui/prayerlog/PrayerLogViewModel.kt` (new)

---

## 8. Test Infrastructure

**Problem:** Zero tests.

**Fix:**
- Add JUnit 5 for unit tests (already have JUnit 4 dependency)
- Add a test for `QuranRepository` loading chapters JSON
- Add a test for `DatabaseCopier` flat name logic
- Add a basic UI test for `DashboardScreen` rendering

**Files to touch:**
- `app/src/test/java/com/smiledev/rafiq/` — unit tests (new)
- `app/src/androidTest/java/com/smiledev/rafiq/` — instrumented tests (new)

---

## Priority Order

1. **P0** — Dead code removal + Islamic palette (safe, no behavior change)
2. **P1** — Repository layer (consistency, testability)
3. **P1** — OsmDroid location (functional bug)
4. **P2** — BookmarkList + PrayerLog screens (missing feature)
5. **P2** — Error/loading audit (polish)
6. **P2** — ZakatCalculator rotation (UX bug)
7. **P3** — Test infrastructure (long-term quality)
