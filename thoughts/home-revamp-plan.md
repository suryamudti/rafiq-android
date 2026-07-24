# Home Screen Revamp — Implementation Plan

## Overview

Redesign the home (Dashboard) screen from a uniform 12-tile grid to a modern layout with a **live Prayer Times hero widget** and a **condensed 7-tile feature grid**. Four low-use features move to a collapsible "More Features" section in Settings.

---

## Current State Analysis

| Aspect | Detail |
|--------|--------|
| **File** | `app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt` |
| **Layout** | `Column` with title row + subtitle + `LazyVerticalGrid(columns = GridCells.Fixed(2))` |
| **Tiles** | 12 `FeatureItem` entries in a static `features` list, rendered by private `FeatureCard` composable |
| **Data** | No ViewModel — purely static navigation |
| **Tile icons** | All from `material-icons-core`, hardcoded hex colors |
| **Header** | Title "Rafiq App" + Face icon (Settings) + subtitle "Your Islamic companion" |
| **Settings screen** | Theme, translation, font size — no navigation to other features |
| **Dashboard test** | `DashboardScreenTest.kt` — checks title, subtitle, 5 tile labels |

---

## Desired End State

```
┌─────────────────────────────┐
│ السلام عليكم          ⚙️    │  ← Time-based greeting
│ Your Islamic companion      │  ← Subtitle
├─────────────────────────────┤
│  ┌─────────────────────┐    │
│  │  Next Prayer: Asr   │    │  ← Hero card — live prayer data
│  │  15:30              │    │     Tapping navigates → PrayerTimesScreen
│  │  2h 15m left        │    │
│  └─────────────────────┘    │
├─────────────────────────────┤
│  ┌──────┐  ┌──────┐        │
│  │Quran │  │Qibla │        │  ← 7 feature tiles in 2-column grid
│  ├──────┤  ├──────┤        │
│  │Mosq. │  │Recit.│        │
│  ├──────┤  ├──────┤        │
│  │Cal.  │  │Zakat │        │
│  ├──────┤  ├──────┤        │
│  │Tasbih│  │      │        │
│  └──────┘  └──────┘        │
└─────────────────────────────┘
```

**Settings screen** (after revamp):
```
┌─────────────────────────────┐
│ ← Back    Settings          │
├─────────────────────────────┤
│  Theme                      │
│  ○ System default           │
│  ○ Light  ○ Dark           │
│  Quran Translation          │
│  ...                        │
│  ▼ More Features            │  ← Collapsible section
│    99 Names                 │  → AsmaulHusnaScreen
│    Prophets                 │  → ProphetsScreen
│    Bookmarks                │  → BookmarkListFullScreen
│    Prayer Log               │  → PrayerLogScreen
└─────────────────────────────┘
```

---

## What We're NOT Doing

- **NOT removing** any screen files, ViewModels, or navigation routes — every feature stays accessible
- **NOT deleting** `NavigationKeys.kt` entries — all 15 NavKeys remain
- **NOT touching** any feature screen implementation files (Quran, Qibla, Mosques, Recitations, Calendar, Zakat, Tasbih — untouched)
- **NOT adding** `material-icons-extended` dependency
- **NOT replacing** KAPT with KSP
- **NOT changing** any data layer repositories, Room DBs, or DI bindings
- **NOT removing** the Indonesian strings for removed tiles — they stay in `strings.xml` for the Settings sub-menu labels

---

## Implementation Approach

### Architecture Decision: New DashboardViewModel

The hero widget needs live data. We create a **lightweight `DashboardViewModel`** that:
- Injects `PrayerTimesRepository` + `PreferencesManager` + `DispatcherProvider`
- Loads today's prayer times on `init`
- Computes next prayer + countdown (subset of `PrayerTimesViewModel` logic)
- Exposes `StateFlow<DashboardUiState>` with loading/error/success states
- Does NOT duplicate full `PrayerTimesViewModel` — only the fields needed for the hero card

This keeps the dashboard decoupled from the full `PrayerTimesViewModel` and avoids pulling in dependencies of the full prayer times screen.

---

## Phases

### ✅ Phase 1: DashboardViewModel (+ UiState) — DONE

**Files to create:**
- `app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardViewModel.kt`

**Content:**
```kotlin
@Immutable
data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val nextPrayerName: String = "",
    val nextPrayerTime: String = "",
    val countdown: String = "",
    val greeting: String = "",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val calculationMethod: Int = 20
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel()
```

- Load prayer times on `init` (same pattern as `PrayerTimesViewModel` but lighter)
- `startCountdown()` using `viewModelScope.launch` with 30s delay
- `refresh()` public method
- `greeting` computed from `Calendar.HOUR_OF_DAY`:
  - Always show "السلام عليكم" as the primary greeting
  - Secondary line based on time: "Good morning" / "Good afternoon" / "Good evening" / "Good night"
  - Use `Locale.getDefault().language` for Eng/Indo

**Success criteria:**
- `DashboardViewModel` compiles and loads prayer times
- Countdown updates correctly
- Greeting changes based on time of day

---

### ✅ Phase 2: Rewrite DashboardScreen.kt — DONE

**Files to modify:**
- `app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt`

**Changes:**

1. **Remove from `features` list**: Prophets, Asmaul Husna, Bookmarks, Prayer Log
2. **Remove unused imports**: `Face`, `Person`, `Star`, `Favorite` icons; `Prophets`, `AsmaulHusna`, `BookmarkList`, `PrayerLog` NavKeys
3. **Add signature**: accept `viewModel: DashboardViewModel = hiltViewModel()` parameter
4. **Replace header layout**:
   - Greeting text (from `state.greeting`) → `headlineMedium` style, weight Bold
   - Subtitle "Your Islamic companion" → `bodyLarge`, `onSurfaceVariant`
   - Settings gear icon (keep `Icons.Filled.Face`)
5. **Add hero card** between header and grid:
   - `Card` with `RoundedCornerShape(16.dp)`, 4dp elevation, teal background
   - If `isLoading`: show `CircularProgressIndicator`
   - If `error`: show error text with retry button
   - If success: show next prayer name (bold, white), time (large 35sp, bold), countdown
   - `Modifier.clickable` → `onNavigate(PrayerTimes)`
6. **Remove empty state**: the `LazyVerticalGrid` approach with 7 tiles
7. **Use `Column` with `verticalScroll`**: since we only have 7 tiles, manually lay them out in `Row`s to avoid nested scrolling conflicts
   - 4 rows: 3 full rows of 2 + final row with 1 tile + 1 spacer
8. **Keep `FeatureCard` composable** unchanged

**Success criteria:**
- Dashboard renders greeting, hero card with live prayer data, and 7-tile grid
- Hero card tap navigates to Prayer Times
- All 7 remaining tiles navigate correctly
- Header shows appropriate greeting for time of day
- Loading and error states display correctly in hero card

---

### ✅ Phase 3: SettingsScreen — Add "More Features" Collapsible Section — DONE

**Files to modify:**
- `app/src/main/java/com/smiledev/rafiq/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/smiledev/rafiq/Navigation.kt`

**SettingsScreen.kt changes:**
1. **Add `onNavigate: (NavKey) -> Unit` parameter** alongside existing `onBack`
2. **Add collapsible "More Features" section** after the existing font size sliders:
   - `AnimatedVisibility` with a header row that has text + expand/collapse indicator
   - Inside: links for "99 Names" → `AsmaulHusna`, "Prophets" → `Prophets`, "Bookmarks" → `BookmarkList`, "Prayer Log" → `PrayerLog`
   - Each link: `Row` with `Modifier.clickable { onNavigate(key) }`, shows the feature name
3. **Add string resource** `more_features` (English: "More Features", Indonesian: "Fitur Lainnya")

**Navigation.kt changes:**
1. In `entry<Settings> { ... }` block, pass `onNavigate = { navKey -> backStack.add(navKey) }` alongside `onBack`

**Success criteria:**
- Settings screen shows collapsible "More Features" section
- Expanding shows all 4 removed features
- Each feature navigates to its correct screen
- Back navigation from those features works correctly

---

### ✅ Phase 4: Add String Resources — DONE

**Files to modify:**
- `app/src/main/res/values/strings.xml` — add:
  - `greeting_morning` = "Good morning"
  - `greeting_afternoon` = "Good afternoon"
  - `greeting_evening` = "Good evening"
  - `greeting_night` = "Good night"
  - `greeting_assalamualaikum` = "السلام عليكم"
  - `more_features` = "More Features"
- `app/src/main/res/values-id/strings.xml` — add:
  - `greeting_morning` = "Selamat pagi"
  - `greeting_afternoon` = "Selamat siang"
  - `greeting_evening` = "Selamat sore"
  - `greeting_night` = "Selamat malam"
  - `greeting_assalamualaikum` = "السلام عليكم"
  - `more_features` = "Fitur Lainnya"

---

### ✅ Phase 5: Update Dashboard Test — DONE

**Files to modify:**
- `app/src/androidTest/java/com/smiledev/rafiq/ui/dashboard/DashboardScreenTest.kt`

**Changes:**
1. Remove checks for `Prophets`, `Bookmarks` labels (they're no longer in the grid)
2. Add check for greeting text (verify the greeting composable exists)
3. Verify remaining tile labels still display: Quran, Qibla, Tasbih

**Success criteria:**
- Tests pass with new layout
- No test references removed tiles

---

## Complete File Change Summary

| Action | File |
|--------|------|
| **CREATE** | `app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardViewModel.kt` |
| **MODIFY** | `app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt` |
| **MODIFY** | `app/src/main/java/com/smiledev/rafiq/ui/settings/SettingsScreen.kt` |
| **MODIFY** | `app/src/main/java/com/smiledev/rafiq/Navigation.kt` |
| **MODIFY** | `app/src/main/res/values/strings.xml` |
| **MODIFY** | `app/src/main/res/values-id/strings.xml` |
| **MODIFY** | `app/src/androidTest/java/com/smiledev/rafiq/ui/dashboard/DashboardScreenTest.kt` |

**Intentionally UNTOUCHED:**
- `NavigationKeys.kt` (all keys remain)
- All 4 removed feature screens + ViewModels (Prophets, AsmaulHusna, Bookmarks, PrayerLog)
- All 7 remaining feature screens + ViewModels (Quran, Qibla, Mosques, Recitations, Calendar, Zakat, Tasbih)
- `data/`, `domain/`, `core/` modules (no changes)
- `di/AppModule.kt` (no new DI needed — `PrayerTimesRepository` already bound)
- Theme files (`Color.kt`, `Theme.kt`, `Type.kt`)
- Build configuration

---

## Testing Strategy

| Type | What | How |
|------|------|-----|
| **Unit** | `DashboardViewModel` | JVM test verifying prayer times load, countdown computation, greeting logic |
| **Compose UI** | Dashboard layout | Update existing `DashboardScreenTest` — verify greeting, hero card, remaining 7 tiles |
| **Compose UI** | Settings sub-menu | Verify collapsible section renders and navigation triggers |
| **Manual** | Full flow | Verify all navigation paths, loading/error states, countdown updates |

**Running tests:**
```
.\gradlew testDebug                              # unit tests
.\gradlew connectedDebugAndroidTest              # instrumented tests
```

---

## Performance Considerations

| Concern | Mitigation |
|---------|-----------|
| **Countdown coroutine** | 30s interval, lightweight — same pattern as `PrayerTimesViewModel`. Cancels on ViewModel clear. |
| **Grid overhead** | Only 7 tiles — use manual `Column` + `Row` (no `LazyVerticalGrid`) to avoid nested scrolling issues |
| **Hero card data** | Single API call on init, cached in ViewModel. Countdown uses local clock only — no network polling. |
| **Memory** | `DashboardViewModel` is scoped to the dashboard `entry` in NavDisplay — destroyed when navigating away, freeing prayer data |
| **Recomposition** | `collectAsState()` in composable, minimal recomposition — greeting only computed once per period change |
