# Design: Customizable Dashboard + Hifz Tracker (Option A)

Date: 2026-10-01
Status: approved (sections 1-3 reviewed by user)

## Goal

- Let users hide/show any of the 14 dashboard shortcuts (all ON by default).
- Add ONE learning feature: Hifz / memorization tracker.
- Defer Tajweed guide + Quiz to v2.

## Section 1 — Hide: Customizable Dashboard

### Changes

- `PreferencesManager.kt` — add 1 key, not 14:
  - `HIDDEN_DASHBOARD_KEYS = stringSetPreferencesKey("hidden_dashboard_keys")`
  - `hiddenDashboardKeys: Flow<Set<String>>` (default empty = all visible)
  - `setDashboardHidden(keys: Set<String>)` + `toggleDashboardKey(key: String)`
- `QuickServiceItem` in `DashboardScreen.kt:80` — add `key: String`:
  `quran`, `prayer_times`, `qibla`, `tasbih`, `hadith`, `prophets`,
  `asmaul_husna`, `recitation`, `mosques`, `calendar`, `zakat`,
  `prayer_log`, `prayer_guidance`, `sunnah_guidance`, plus `hifz` once
  Section 2 lands (total 15 toggleable keys).
- `DashboardViewModel` — combine existing state + `hiddenDashboardKeys`,
  expose `visibleServices`. Filter in ViewModel, not UI.
- `SettingsScreen.kt` — new "Dashboard customization" group above
  "More features" with 14 Switch rows (reuse `PrayerLogScreen` Switch
  pattern). Icon + label + toggle. Plus "Reset to defaults" button.
- Empty state: if all 14 hidden, show card ("No shortcuts — tap to
  restore") reusing `RafiqEmptyState`.

### Data flow

Settings toggle -> DataStore edit -> Flow emits -> Dashboard recomposes.
Survives restart. No Room needed.

### Error handling

- Corrupt set ignored -> fallback to empty (all visible).
- Unknown keys ignored for forward-compat.

## Section 2 — Add: Hifz / Memorization Tracker

### What it does

User picks Surah + ayah range as a goal (e.g. Al-Mulk 1-12), checks off
ayahs as memorized, sees progress % and streak. Deep-links into existing
`Ayah(suraNumber, suraName, scrollToAya)` reader for revision.

### Components (follows PrayerLog pattern)

- `:domain` — `HifzGoal(id, suraNumber, suraName, ayaStart, ayaEnd,
  memorizedAyahs: Set<Int>)` + `HifzRepository`
  (`observeAll(): Flow<List<HifzGoal>>`, `upsert()`, `delete()`).
- `:data` — `HifzDatabase` (singleton `getInstance()` +
  `fallbackToDestructiveMigration()`, table `hifz_goals`) + `HifzEntity`
  + `HifzDao` + `HifzRepositoryImpl`. `room-runtime` stays `api`.
- `:app` — new `Hifz` NavKey in `NavigationKeys.kt`, `entry<Hifz>` in
  `Navigation.kt`, `HifzScreen(onBack, viewModel = hiltViewModel(),
  modifier)` + `HifzViewModel` (`MutableStateFlow<HifzUiState>`,
  `viewModelScope.launch(Dispatchers.IO)` on init). UI uses
  `RafiqTopAppBar`, `RafiqCard`, progress indicator, Checkbox rows.
  Plus 15th dashboard card (teal container like Quran).
- `AppModule.kt` — `@Singleton` provides for DB + repo.

### Data flow

HifzScreen -> ViewModel toggleAyah(id, aya) -> Room upsert -> Flow
re-emits -> progress % recomputed. Reuses
`PreferencesManager.lastReadSura/Aya`. No new prefs.

### Edge cases

- Invalid range (start > end) blocked in picker.
- 0 goals -> onboarding card ("Create first goal"), not blank screen.
- Deleted Surah data -> empty-state, no crash.

### Out of scope (v2)

Tajweed static guide, Quiz, Kids mode, reorder/drag, onboarding chooser.

## Section 3 — Testing & Rollout

- `.\gradlew testDebug` — hidden-keys filter (empty = all visible,
  unknown keys ignored, hide-all shows empty-state); Hifz progress %
  math + streak calc.
- `.\gradlew connectedDebugAndroidTest` (emulator Medium_Phone_API_35) —
  smoke: toggle Zakat off -> card gone -> restart -> still gone; create
  Hifz goal -> toggle ayah -> progress updates.
- Manual: id/en locale, dark/light + dynamic color (Android 12+),
  small-screen 4-column grid with 15th card wraps correctly.
- Rollout: no migration (empty hidden-set = current behavior). Minor
  version bump. No analytics SDK; success = no crashes + 1 week dogfood.

## Constraints respected

- `material-icons-core` only, no extended set.
- KAPT (not KSP) for Hilt/Room.
- DataStore via `PreferencesManager`, never SharedPreferences.
- `room-runtime` is `api` in `:data`.
- `kotlin.math.*`, never `Java Math.*`.
- Screens follow `XxxScreen(onBack, viewModel = hiltViewModel(), modifier)`
  + `backStack.removeLastOrNull()` pattern.
