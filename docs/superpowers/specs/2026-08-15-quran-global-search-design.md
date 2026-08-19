# Move Quran Search to the Surah Screen — Design

**Date:** 2026-08-15
**Status:** Approved (design review)

## 1. Overview

Move the search feature off the Ayah screen and onto the Surah (Quran) screen, and change its
scope from "filter the current surah's ayahs" to **global ayah search across the entire Quran**.

- The within-surah search on `AyahScreen` is removed entirely (icon, field, filter logic).
- The Quran screen (`QuranScreen`, NavKey `Quran`) gains a Search icon in its `TopAppBar` that
  toggles a search field; typing runs a debounced global query across all 114 surahs.
- Results are capped at 100 and each shows the surah name, `sura:aya` reference, Arabic text, and
  the current-language translation snippet with the matched term highlighted. Tapping a result
  navigates to the existing `Ayah` screen scrolled to that ayah (reuses the bookmark deep-link
  mechanism).

Search uses the existing raw-SQLite architecture (`QuranRepositoryImpl` + `rawQuery`) with
parameterized `LIKE` queries against the local quran DB and the current-language translation DB.
No schema changes, no new dependencies, no asset-DB regeneration.

## 2. Search semantics

Per product decisions:

- **Match scope:** Arabic text (`quran.text`) **and** the current-language translation
  (`verses.text`). "Current language" resolves via the existing preference logic:
  `translationLanguage` pref, with `system → currentLocaleCode()` (ID users search
  `id.indonesian.db`, everyone else searches `en.sahih.db`).
- **Locale:** passed as `localeCode` ("id" or "en"), same convention as `getChapters`.
- **Debounce:** 250 ms per keystroke with stale-response guard, mirroring `HadithSearchViewModel`.
- **Limit:** 100 results.
- **Matching:** SQLite `LIKE` is case-insensitive for ASCII by default (covers EN/ID); Arabic has
  no case. User input `%`, `_`, and `\` are escaped so they match literally.

## 3. Domain Layer (`:domain`)

Extend the `QuranRepository` interface (matching the synchronous `Result` convention):

```kotlin
interface QuranRepository {
    fun getChapters(localeCode: String = "en"): Result<List<Surah>, AppError>
    fun getAyahsWithTranslation(suraNumber: Int, localeCode: String = "en"): Result<List<Ayah>, AppError>

    /** Case-insensitive global substring search across Arabic text + the current-language translation. */
    fun searchAyahs(query: String, localeCode: String = "en", limit: Int = 100): Result<List<Ayah>, AppError>
}
```

No new domain models — results are existing `Ayah` rows. The result `Ayah` carries `sura`, `aya`,
`text`, the current-language `translation`, and page/juz metadata for display enrichment.

## 4. Data Layer (`:data`)

### `searchAyahs(query, localeCode, limit)` in `QuranRepositoryImpl`

1. Trim the query; a blank query returns `Result.Success(emptyList())` without touching the DB.
2. Escape `%`, `_`, `\` in user input and build `%term%` patterns with `ESCAPE '\'`.
3. Query the quran DB for Arabic matches (all surahs):
   `SELECT sura, aya, text, bismillah FROM quran WHERE text LIKE ? ESCAPE '\'`
4. Query the current-language translation DB (id or en, based on `localeCode`):
   `SELECT sura, ayah, text FROM verses WHERE text LIKE ? ESCAPE '\'`
5. Merge both result sets keyed by `sura:aya`, dedupe, sort by `sura` then `aya`, cap at `limit`.
6. Enrich each match with page/juz/sajda from the existing `getMetadataMap()` and the current
   language's translation text (reuse `getTranslationForSuraSafe`); map to `Ayah` via the existing
   `AyahData.toDomain()` path.
7. Failure → `Result.Error(AppError.Database(...))` (same catch style as existing methods).

DBs are opened exactly as today (`getQuranDatabase`, `getTranslationDatabase`). A single-pass
`LIKE` scan over ~6,236 ayahs on a local SQLite DB is fast enough for debounced as-you-type search
(the hadith search already does the same across its 14,734-row corpus).

## 5. UI (`:app`)

### `QuranViewModel` — extend (no new ViewModel)

Add to `QuranUiState`:

```kotlin
val searchQuery: String = "",
val searchResults: List<Ayah> = emptyList(),
val searchLoading: Boolean = false,
val searchError: AppError? = null,
val translationLanguage: String = "system"
```

- Constructor gains `preferencesManager: PreferencesManager` (Hilt-injected) — the existing
  `QuranViewModelTest` must add a mockk prefs arg to its `QuranViewModel(...)` construction.
- Collect `translationLanguage` from `PreferencesManager` in `init` (same as `HadithSearchViewModel`).
- `setSearchQuery(query)`: updates `searchQuery` immediately.
- `search(query)`: debounced (250 ms `delay` on `dispatcherProvider.io`), calls
  `quranRepository.searchAyahs(term, resolvedLanguage(), SEARCH_LIMIT)`; ignores stale responses by
  comparing the query at completion to the latest. Blank query → clears results without a DB call.
- `resolvedLanguage()`: reuses the `translationLanguage`/`currentLocaleCode()` logic.

### `QuranScreen` — add search UI

- Add a `Search` `IconButton` (`Icons.Filled.Search` from `material-icons-core`) in the `TopAppBar`
  actions that toggles `showSearch` (same toggle pattern as today's Ayah screen).
- When `showSearch`, render an `OutlinedTextField` below the `TopAppBar` (bound to
  `viewModel.searchQuery`).
- Content area behavior:
  - Query non-blank → show search results `LazyColumn` in place of the tab content.
  - Query blank → show normal tab content (Surahs / Bookmarks).
- Result card shows:
  - Reference line: surah name (resolved from the VM's already-loaded `surahs` list via
    `chapterNumber`; fallback `"Surah $sura"` if the lookup misses) + `sura:aya`.
  - Arabic text (me_quran font) and the current-language translation snippet (~3 lines,
    `TextOverflow.Ellipsis`), with the matched term highlighted case-insensitively via
    `buildAnnotatedString` + `SpanStyle` (reuse the `highlightMatches` pattern from
    `HadithSearchScreen`).
- Empty state (non-blank query, no results) → "No results found".
- Loading state → `CircularProgressIndicator`.

### Navigation

- `QuranScreen` gains `onSearchResultClick: (Int, String, Int) -> Unit` callback.
- `Navigation.kt`: wire it to `backStack.add(Ayah(suraNumber = sura, suraName = name,
  scrollToAya = aya))` — identical to the existing `onBookmarkClick` handler.

### Remove search from the Ayah screen

- `AyahViewModel`: delete `searchQuery` from `AyahUiState`, `setSearchQuery()`, and
  `getFilteredAyahs()`.
- `AyahScreen`: remove `showSearch` state, the Search `IconButton`, the `OutlinedTextField`,
  the `Icons.Filled.Search` import, and the "No results found" branch. The list renders `state.ayahs`
  directly again (no filtering).

### Strings (`values/strings.xml` + `values-id/strings.xml`)

- `search_quran_hint` — "Search the whole Quran…" / "Cari di seluruh Al-Quran…"
- `no_ayahs_match` — "No results found" / "Tidak ada hasil"

## 6. Data flow

1. User taps Search icon on `QuranScreen` → `showSearch` toggles the field.
2. User types → `viewModel.search(query)` debounced 250 ms → `searchAyahs(term, resolvedLang, 100)`.
3. Repo queries Arabic + current-language translation DBs, merges/dedupes, enriches, caps at 100.
4. Results render as cards; tap → `onSearchResultClick(sura, name, aya)` →
   `Ayah(suraNumber, suraName, scrollToAya = aya)` → existing Ayah screen scrolled to the ayah.
5. The old within-surah search is gone from `AyahScreen`.

## 7. Error handling

- DB copy failure / query exception → `AppError.Database` → inline error text with `displayMessage`
  (same as other Quran screens).
- Blank query → no DB call, empty results, normal tab content.
- Result translation missing for resolved locale → card shows Arabic only (no fallback text added).

## 8. Testing

- `data/src/test/.../QuranRepositoryImplTest`: Robolectric fixture DBs (pattern from
  `HadithRepositoryImplTest` — `SQLiteDatabase.openOrCreateDatabase` + `CREATE TABLE`/`INSERT` for
  both the `quran` table and the current-language `verses` table):
  - Arabic text match across surahs.
  - Translation match for `id` and for `en`.
  - Merge/dedupe when a query matches both Arabic and translation for the same ayah.
  - `%`/`_` literal escaping; blank query returns empty without error; no-match returns empty;
    `limit` applied.
- `app/src/test/.../QuranViewModelTest`: mockk repo + `TestDispatcherProvider` — debounce
  coalesces rapid queries, blank query short-circuits, success populates results, error surfaces in
  state, stale-response race ignored.
- `AyahScreenTest` (existing instrumented): remains green; no search assertions to remove.

## 9. Out of scope (deferred)

- FTS5 tokenized search / relevance ranking (overkill for 6,236 rows).
- Pagination beyond the 100 cap; no result-count banner.
- Arabic diacritic (tashkeel) insensitive normalization.
- Searching the non-current translation (user decision: current-language only).
- Search history / recent searches.
