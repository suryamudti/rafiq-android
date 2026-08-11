# Prophets Feature Improvements — Design

## Overview

Improve the Prophets feature across three angles: **content depth** (author new bilingual data for all 25 prophets), **UI/UX polish** (loading/empty states, localize hardcoded titles, remove debug `(id)`, prev/next navigation), and **feature additions** (story font-size controls, share story, favorite prophets, tappable Quranic verse references).

## Current State

- **Data**: `app/src/main/assets/quran-data/prophets/prophets.json` — 25 prophets with `id`, `name_arabic`, `name_en`, `name_id`, `summary_en/id`, `story_en/id`, `miracles_en/id`.
- **`ProphetRepositoryImpl`** reads the asset, caches in memory, returns `Result<List<ProphetStory>, AppError>`.
- **`ProphetStory`** (domain model): 10 bare fields (id + names + bilingual summary/story/miracles).
- **`ProphetsScreen`**: search TextField + 2-column grid. Cards show Arabic name, localized name, and a debug `(${prophet.id})`. `isLoading` state exists but is unused; no loading UI.
- **`ProphetDetailScreen`**: header card + hardcoded "Summary"/"Story"/"Miracles" section titles (not localized). Navigates to detail from the already-loaded list; infinite spinner if opened with an empty list.
- **`ProphetsViewModel`**: loads on init, filters by `nameEn`/`nameId` only. `getProphetById()` repo method exists but unused by the ViewModel.
- **Conventions**: DataStore via `PreferencesManager` (no SharedPreferences), Hilt `@HiltViewModel`, `Result<T, AppError>`, `material-icons-core` only, share via `ACTION_SEND` (see `shareAyah` in AyahScreen.kt:808), AyahScreen font-size bottom sheet pattern.

## Approach

Selected: **Vertical slice (tracer bullet)**.

1. Define uniform JSON schema + extend domain model + repo parsing for all 25 prophets.
2. Fully author content for **Adam** (id 1) and **Nuh** (id 2) — all new fields, bilingual.
3. Build all UI and features end-to-end, verified on emulator against the two slices.
4. Then batch-author remaining 23 prophets' content.

## 1. Data Model & JSON Schema

New JSON shape (existing fields unchanged; new fields added):

```json
{
  "id": 1,
  "name_arabic": "آدم",
  "name_en": "Adam",
  "name_id": "Adam",
  "summary_en": "...",
  "summary_id": "...",
  "story_en": "...",
  "story_id": "...",
  "miracles_en": "...",
  "miracles_id": "...",
  "era_en": "Pre-history / early humanity",
  "era_id": "Zaman dahulu / awal kemanusiaan",
  "people_en": "His descendants; the angels",
  "people_id": "Keturunannya; para malaikat",
  "lifespan_en": "~1000 years",
  "lifespan_id": "±1000 tahun",
  "events_en": ["Created from clay", "Taught the names", "Sent to Earth"],
  "events_id": ["Diciptakan dari tanah liat", "Diajarkan nama-nama", "Diturunkan ke Bumi"],
  "lessons_en": ["Humility defeats pride", "Sincere repentance is accepted"],
  "lessons_id": ["Kerendahan hati mengalahkan kesombongan", "Taubat yang tulus diterima"],
  "verses": [
    { "surah": 2, "surah_name_en": "Al-Baqarah", "surah_name_id": "Al-Baqarah", "ayah_start": 30, "ayah_end": 39 }
  ]
}
```

**Domain model additions** (`ProphetStory`):
- `eraEn: String`, `eraId: String`, `peopleEn: String`, `peopleId: String`, `lifespanEn: String`, `lifespanId: String`
- `eventsEn: List<String>`, `eventsId: List<String>`
- `lessonsEn: List<String>`, `lessonsId: List<String>`
- `verses: List<VerseRef>`

**New domain model** `VerseRef`:
- `surah: Int`, `surahNameEn: String`, `surahNameId: String`, `ayahStart: Int`, `ayahEnd: Int`

`surah_name` is stored in the JSON so verse refs can navigate to Ayah without a cross-feature lookup.

## 2. Repository & ViewModel

**`ProphetRepositoryImpl`**:
- Parse new fields: `events_*`/`lessons_*` via `JSONArray` → `List<String>`; `verses` → `List<VerseRef>`.
- Keep in-memory cache + `getProphets()` / `getProphetById()`.
- `getProphetById()` remains unused by the ViewModel (detail screen reads from loaded list).

**Favorites persistence** (in `PreferencesManager`, DataStore):
- `favoriteProphetIds: Flow<Set<Int>>`
- `toggleFavoriteProphet(id: Int)`
- `isFavorite(id: Int)` (or derived from the Flow)

**`ProphetsViewModel`**:
- Collect `favoriteProphetIds` into a `StateFlow<Set<Int>>` via existing `combine` pattern.
- `filteredProphets()` extended: also matches `nameArabic`; honors `showFavoritesOnly` flag.
- New: `setShowFavoritesOnly(Boolean)`, `toggleFavorite(id)`, `setStoryFontSize(Int)` (DataStore, AyahScreen pattern).
- Detail not-found fix: if `prophets` empty and `isLoading` false → explicit "not found" state (no eternal spinner).

## 3. ProphetsScreen (list) polish

- Remove debug `(${prophet.id})` from cards.
- Centered `CircularProgressIndicator` while `isLoading`.
- Favorites toggle chip ("Favorites"); when active, only favorited prophets show, with an empty-state message when none.
- Search also matches Arabic names.
- Keep 2-column grid; cards show Arabic calligraphy + localized name only.

## 4. ProphetDetailScreen

**Localization**: hardcoded "Summary"/"Story"/"Miracles" + new headers ("Facts", "Key Events", "Lessons", "Verse References") via strings.xml (EN + ID).

**New sections (after Miracles)**:
- **Facts** — compact rows: Era, People sent to, Lifespan.
- **Key Events** — numbered timeline list.
- **Lessons** — bulleted list.
- **Verse References** — card/chips per verse; **tappable** → navigates to that Ayah.

**Features**:
- **Font-size controls**: "Aa" TopAppBar action → bottom sheet with slider (AyahScreen pattern); story reading size stored via DataStore.
- **Share**: ACTION_SEND share of the story (pattern from `shareAyah`). Icons constrained to `material-icons-core`; use text-button/share-action consistent with app patterns.
- **Favorite**: heart toggle using `Icons.Filled.Favorite` (in core set) — filled when favorited.
- **Prev/Next navigation** at bottom of scroll: Previous hidden on first prophet, Next hidden on last (**no wrap**).

## 5. Navigation & Integration

- `ProphetDetailScreen` gains `onVerseRefClick: (surah: Int, surahName: String, ayaStart: Int) -> Unit`. The screen computes `surahName` from `surahNameEn`/`surahNameId` using its `localeCode` and passes the localized name.
- `Navigation.kt`: `ProphetDetail` entry wires the callback to `backStack.add(Ayah(surah, surahName, ayaStart))` — reuses existing `Ayah` NavKey with `scrollToAya` (same as bookmarks).
- `NavigationKeys.kt`: no new keys.
- Add EN + ID strings for all new labels.

## 6. Testing & Verification

- Update `ProphetsViewModelTest`: filters, favorites-only, Arabic search, not-found state.
- Repository test for new-field parsing (extract a pure parser from `ProphetRepositoryImpl` to avoid Android context dependency, or test JSON→model mapping).
- `GetProphetsUseCaseTest` untouched.
- Verify: `.\gradlew testDebug`, then `.\gradlew assembleDebug` + install on emulator (`Medium_Phone_API_35`) per AGENTS.md.

## Out of Scope

- No new nav keys; no Room table for favorites (DataStore only).
- No new material-icons-extended dependency.
- No migration to KSP; no unrelated refactors.
- Content for all 25 prophets: schema applied uniformly; full authoring completed for the two slices first, remainder batch-authored after UI verification.

## Success Criteria

- [ ] All new fields render correctly for Adam and Nuh (facts, events, lessons, verses).
- [ ] Verse refs navigate to the correct Ayah (scrollToAya) and back to ProphetDetail.
- [ ] Favorites toggle persists (DataStore) and list filter works.
- [ ] Font-size control changes story text size and persists.
- [ ] Share sends the story text via the system share sheet.
- [ ] No hardcoded section headers; EN + ID strings complete.
- [ ] `testDebug` passes with updated tests.
- [ ] App installs and the prophets screens work on the emulator.