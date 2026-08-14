# Hadith Search Feature — Design

**Date:** 2026-08-14
**Status:** Approved (design review)

## 1. Overview

Add **global full-text search** across the entire hadith corpus (Sahih al-Bukhari + Sahih Muslim,
14,734 hadiths) to the existing hadith browser. A Search icon on the hadith books top bar opens a
dedicated search screen with a live (debounced) as-you-type field. Results are capped at 100 and
each result shows a localized snippet with the matched term highlighted; tapping a result opens the
existing hadith detail screen.

Search uses the current raw-SQLite architecture (`HadithRepositoryImpl` + `rawQuery`) with
parameterized `LIKE` queries. No schema changes, no new dependencies, no asset-DB regeneration.

## 2. Domain Layer (`:domain`)

Extend the `HadithRepository` interface (matching the synchronous `Result` convention):

```kotlin
interface HadithRepository {
    fun getBooks(): Result<List<HadithBook>, AppError>
    fun getHadithsByBook(bookId: String): Result<List<Hadith>, AppError>

    /** Case-insensitive substring search across translations, Arabic matn, and book names. */
    fun searchHadiths(query: String, limit: Int = 100): Result<List<Hadith>, AppError>

    /** Load a single hadith by its global id (used by detail screen reached from search). */
    fun getHadithById(id: Int): Result<Hadith?, AppError>
}
```

No new domain models are needed — results are existing `Hadith` rows. The repository returns the
`Hadith` list plus the book id; the screen resolves the book name locally (see §5).

## 3. Data Layer (`:data`)

### `searchHadiths(query, limit)`

Single parameterized `rawQuery` joining `hadiths` to `books`:

```sql
SELECT h.id, h.book_id, h.in_book_number, h.narrator_ar, h.narrator_en,
       h.text_ar, h.text_en, h.text_id
FROM hadiths h
JOIN books b ON b.id = h.book_id
WHERE h.text_id   LIKE ? ESCAPE '\'
   OR h.text_en   LIKE ? ESCAPE '\'
   OR h.text_ar   LIKE ? ESCAPE '\'
   OR b.name_id   LIKE ? ESCAPE '\'
   OR b.name_en   LIKE ? ESCAPE '\'
   OR b.name_ar   LIKE ? ESCAPE '\'
ORDER BY h.id
LIMIT ?
```

- Query term is trimmed; a blank query returns an empty `Result.Success` without touching the DB.
- `%`, `_`, and `\` in user input are escaped with `\` before building the `LIKE` patterns
  (`%term%`), using `ESCAPE '\'` in SQL.
- SQLite `LIKE` is case-insensitive for ASCII by default, which covers EN/ID. Arabic has no case.
- Column mapping mirrors `getHadithsByBook` exactly (narrators `.ifBlank { null }`).
- Failure → `Result.Error(AppError.Database(...))` (same catch style as existing methods).

### `getHadithById(id)`

`SELECT ... FROM hadiths WHERE id = ?` (same column list + mapping). Returns `Result.Success(null)`
when no row matches; `Result.Error(AppError.Database(...))` on exception.

## 4. UI (`:app`)

### New screen: `HadithSearchScreen` + `HadithSearchViewModel`

Follows the Prophets / Asmaul Husna pattern.

- `HadithSearchUiState(query: String = "", results: List<Hadith> = emptyList(), isLoading: Boolean = false, error: AppError? = null, translationLanguage: String = "system")`
- `search(query: String)`: updates `query` in state immediately, then launches a debounced search
  on `dispatcherProvider.io`. Debounce via a 250 ms `delay` before re-running the query (kept simple
  and deterministic for tests — same style as existing VMs which use `viewModelScope.launch` +
  `delay`). Stale responses are ignored by comparing the query at completion time to the latest
  requested query.
- `resolvedLanguage()` reuses the existing `translationLanguage`/`currentLocaleCode()` logic.

Screen layout (`Scaffold` + `TopAppBar` with Back, same chrome as `HadithListScreen`):
- `TextField` pinned at top (placeholder `search_hadiths`), styled like `AsmaulHusnaScreen`.
- Empty query → centered hint `search_hadiths_hint`.
- No results & non-blank query → `no_hadiths_match` (with `%s` = query).
- Results → `LazyColumn` of result cards. Each card shows:
  - Reference line: `Sahih <collection> · Book N, Hadith M` (reuses `hadith_reference` pattern) —
    resolved from the hadith's `bookId` + a book-name lookup the VM loads once via `getBooks()`.
  - Snippet: chosen per `resolvedLanguage()` — ID `textId`, EN `textEn`, Arabic `textAr`; fallback
    to the first non-blank translation (same `translationText` fallback used in detail).
    Snippet capped to ~3 lines with `TextOverflow.Ellipsis`. The matched term is highlighted
    (case-insensitive) with `buildAnnotatedString` + `SpanStyle(background/color)`. Arabic
    highlights use the `me_quran` font family on the same annotated string.
  - Tap → `onHadithClick(hadith.id)` → `HadithDetail(hadithId)`.

### `HadithDetailScreen` fix

Today it never calls `load()` and only searches an already-populated `state.hadiths` list
(`find { it.id == hadithId }`), which breaks when navigated directly from search. Add a
`LaunchedEffect(hadithId) { viewModel.loadById(hadithId) }` and a `loadById(id)` method on
`HadithListViewModel` that calls the new `getHadithById` and populates `hadiths` with a single
element (keeping the existing `find` lookup working). When reached from the book list this is a
no-op-ish extra query; when reached from search it loads the hadith.

### Navigation

- `NavigationKeys.kt`: add `@Serializable data object HadithSearch : NavKey`.
- `Navigation.kt`: add `entry<HadithSearch>` wiring `HadithSearchScreen(onHadithClick = { id ->
  backStack.add(HadithDetail(id)) }, onBack = ...)`.
- `HadithBooksScreen`: add a `Search` `IconButton` (`Icons.Filled.Search` from `material-icons-core`)
  in the `TopAppBar` actions → `onSearch()` callback → `backStack.add(HadithSearch)`.

### Strings (`values/strings.xml` + `values-id/strings.xml`)

- `search_hadiths` — "Search hadiths…" / "Cari hadis…"
- `search_hadiths_hint` — "Type to search the whole hadith collection" / "Ketik untuk mencari seluruh koleksi hadis"
- `no_hadiths_match` — "No hadiths match \"%s\"" / "Tidak ada hadis yang cocok dengan \"%s\""

## 5. Data flow

1. User taps Search icon on `HadithBooksScreen` → `HadithSearch` pushed.
2. VM loads books once (`getBooks()`) for name resolution; user types → `search()` debounced.
3. Repo runs the `LIKE` query → capped 100 `Hadith` results → state updated.
4. Card renders reference + snippet with highlight; tap → `HadithDetail(hadithId)`.
5. Detail VM `loadById` → `getHadithById` → existing detail layout.

## 6. Error handling

- DB copy failure / query exception → `AppError.Database` → inline error text with `displayMessage`
  (same as other hadith screens).
- Blank query → no DB call, empty results.
- Snippet empty for resolved locale → fallback to first non-blank of `textId`/`textEn` (Arabic
  always present).

## 7. Testing

- `data/src/test/.../HadithRepositoryImplTest`: new cases with the existing Robolectric fixture —
  matches on `text_id`, `text_en`, `text_ar`, and book `name_id`; no-match returns empty; `%`/`_`
  literal escaping; blank query returns empty without error; `getHadithById` found + not-found.
- `app/src/test/.../HadithSearchViewModelTest`: mockk repo + `TestDispatcherProvider` —
  debounce coalesces rapid queries, blank query short-circuits, success populates results, error
  surfaces in state, stale-response race (out-of-order query results ignored).
- `app/src/test/.../HadithListViewModelTest`: add `loadById` success + not-found cases.

## 8. Out of scope (deferred)

- FTS5 tokenized search / relevance ranking (overkill for 14k rows).
- Pagination beyond the 100 cap; result-count "and more" UX is limited to a `LIMIT` and no banner.
- Arabic diacritic (tashkeel) insensitive normalization.
- Search within a single book (only global).
- Indexing/preloading — query runs directly against the copied asset DB.
