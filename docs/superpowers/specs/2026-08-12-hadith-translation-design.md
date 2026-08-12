# Hadith Translation Feature — Design

**Date:** 2026-08-12
**Branch:** `feat/hadith-translation` (worktree `.worktree/worktrees_hadith`)
**Status:** Approved

## 1. Overview

Add a **Sahih Bukhari + Sahih Muslim** hadith browser to Rafiq that shows each hadith in
Arabic plus English and Bahasa Indonesia translations. Translation language follows the
user's existing Quran translation setting (`system` / `id` / `en` / `both`). Scope is
**browse + read only** — no search, favorites, share, or font-size controls in this version.

Feature follows the established codebase patterns: content is a read-only asset SQLite DB
copied via `DatabaseCopier` and queried with `rawQuery` (the `QuranRepositoryImpl`
translation-DB pattern), the browse hierarchy mirrors Quran chapters → ayahs, the detail
screen reuses the list ViewModel the way `ProphetDetailScreen` does, and locale handling
reuses `PreferencesManager.translationLanguage` with `currentLocaleCode()` resolution.

## 2. Content & Data Model

**Scope:** Full Sahih al-Bukhari (≈ 7,008 hadith) and Sahih Muslim (≈ 5,362 hadith), each
with Arabic matn + EN translation + ID translation.

### Domain models (`:domain`)

```kotlin
data class HadithBook(
    val id: String,            // stable key, e.g. "bukhari.1"
    val collection: String,    // "bukhari" | "muslim"
    val number: Int,           // book number within the collection
    val nameAr: String,
    val nameEn: String,
    val nameId: String,
)

data class Hadith(
    val id: Int,               // global PK within hadith.db
    val bookId: String,
    val inBookNumber: Int,     // hadith number within the book
    val narratorAr: String?,
    val narratorEn: String?,
    val textAr: String,
    val textEn: String,
    val textId: String,        // may be empty -> fall back to textEn
)
```

### `:domain` repository interface

```kotlin
interface HadithRepository {
    fun getBooks(): Result<List<HadithBook>, AppError>
    fun getHadithsByBook(bookId: String): Result<List<Hadith>, AppError>
}
```

Synchronous `Result`-returning methods, matching `ProphetRepository`.

### Asset DB schema (`hadith.db`)

```
books:   id TEXT PK, collection TEXT, number INTEGER, name_ar TEXT, name_en TEXT, name_id TEXT
hadiths: id INTEGER PK, book_id TEXT, in_book_number INTEGER,
         narrator_ar TEXT, narrator_en TEXT, text_ar TEXT, text_en TEXT, text_id TEXT
index:   hadiths(book_id)
```

All TEXT/INTEGER, no FK constraints (read-only asset). Mirrors the translation-DB convention
(no Room, no DAO).

## 3. Data Layer (`:data`)

`HadithRepositoryImpl`:
- `@Singleton @Inject constructor(@ApplicationContext context, databaseCopier: DatabaseCopier)`.
- Opens `hadith.db` read-only via `databaseCopier.copyDatabaseIfNeeded("hadiths/hadith.db")`
  → `SQLiteDatabase.openDatabase(File(filesDir, "databases/hadiths_hadith.db"), null, OPEN_READONLY)`.
  Note: `copyAndVerifyTranslationDb` is **NOT** used — it hard-codes a `verses` table check that
  `hadith.db` does not satisfy. Use `copyDatabaseIfNeeded` (which copies on missing/empty file
  only) and add a lightweight self-check: a sanity `sqlite_master` query for the `books` and
  `hadiths` tables; failure → delete file, retry copy once, else `Result.Error`. The DB name
  contains `/`, so `DatabaseCopier` flattens to `_` for `getDatabasePath` (existing behavior —
  translation asset names are already `translations/id.indonesian.db`).
- `getBooks()` → `SELECT * FROM books ORDER BY collection, number`.
- `getHadithsByBook(bookId)` → `SELECT * FROM hadiths WHERE book_id = ? ORDER BY in_book_number`.
- Manual `Cursor` → domain mapping (no Gson needed for a DB; the prophets/asmaul-husna Gson pattern
  only applies to JSON assets).
- Failures → `Result.Error(AppError.Database(...))`.

DI: `@Binds` in `RepositoryModule` (`AppModule.kt`), one line like line 71.

## 4. Content Pipeline (external to the app build)

A Python generator committed under `tools/hadith-pipeline/` produces `hadith.db`, which is
committed to `app/src/main/assets/quran-data/hadiths/hadith.db`. The pipeline does **not** run
during the Gradle build.

- **Sources (pinned releases, license-verified):**
  - Arabic + EN: Sunnah.com-derived corpus (`hadith-json` pinned tag). Muhsin Khan (Bukhari)
    and Abdul Hamid Siddiqui (Muslim) translations are public domain.
  - ID: MIT-licensed Indonesian dataset (`irsyadulibad/hadits-database`), keyed by
    collection + book + in-book number.
- **Merge key:** `(collection, book, in_book_number)`.
- **Validation gates** (fail loudly, never ship indeterminate data):
  - Every hadith has non-blank `text_ar` and `text_en`.
  - Hadith rows lacking `text_id` are reported for backfill (not silently dropped).
  - Book counts match canonical totals (Bukhari ≈ 7,008, Muslim ≈ 5,362).
  - Deterministic output with a pinned manifest + checksums.
- **Steps:** fetch pinned datasets → normalize → verify licenses/sources in README →
  merge → validate → emit `hadith.db` (Python `sqlite3`) → commit DB.

## 5. UI (`:app`)

Three screens under `ui/hadith/`:

1. **`HadithBooksScreen`** — `LazyVerticalGrid` of books (grouped by collection which is shown as
   section header or card subtitle). Card shows Arabic name (`FontFamily(Font(R.font.me_quran))`)
   + localized name (`if (resolvedLang == "id") book.nameId else book.nameEn`). Click →
   `HadithList(bookId)`.
2. **`HadithListScreen(bookId)`** — `LazyColumn` of hadith cards (`Book X · Hadith Y` header,
   one-line preview). Loading / error / empty states like `ProphetsScreen`. Click →
   `HadithDetail(hadithId)`.
3. **`HadithDetailScreen(hadithId)`** — reuses `HadithListViewModel` + `find { it.id == hadithId }`
   like `ProphetDetailScreen`. Shows reference line, Arabic text (me_quran font), then the
   translation section per the resolved language:
   - `"id"` → ID section
   - `"en"` → EN section
   - `"both"` → ID + EN sections with `ID`/`EN` chips (mirror `AyahScreen` "both" branch, L637-705)

### ViewModels

- `HadithBooksViewModel` — `HadithBooksUiState(books, isLoading, error, translationLanguage)`,
  `init { load() }` on `dispatcherProvider.io`.
- `HadithListViewModel` — `HadithListUiState(hadiths, isLoading, error, translationLanguage, book)`,
  `load(bookId)`.
- Shared `resolvedLanguage()` helper: `if (translationLanguage == "system") currentLocaleCode()
  else translationLanguage` (same logic as `AyahViewModel.getTranslationText`).
  Combination of `preferencesManager.translationLanguage` Flow (like `AyahViewModel.kt:73-90`).

### Navigation (`NavigationKeys.kt` / `Navigation.kt`)

- `@Serializable data class HadithList(val bookId: String) : NavKey`
- `@Serializable data class HadithDetail(val hadithId: Int) : NavKey`
- `HadithBooks` as the feature root (object NavKey) + three `entry<Key> { key -> ... }` blocks.
- Dashboard `FeatureItem` + Settings "More features" item pointing to `HadithBooks`.

### Strings

Bilingual (EN/ID) `strings.xml` / `values-id/strings.xml` entries: feature title,
`hadith_load_error`, list/detail labels, `book`/`hadith` reference labels.

## 6. Error Handling

- Asset DB copy uses `copyDatabaseIfNeeded` (ships full DB; no `verses` table check needed) plus a
  sanity `sqlite_master` check for `books`/`hadiths`, with one delete-and-retry like the
  translation-DB path.
- Missing/corrupt DB → `AppError.Database` → user-facing message `hadith_load_error`.
- Empty `textId` → fall back to `textEn` (exact `AyahViewModel` fallback).
- Missing Arabic → placeholder text (style of `[Translation unavailable]`).

## 7. Testing

- `data/src/test`: `HadithRepositoryImplTest` — Robolectric, mocked `AssetManager`, in-memory
  SQLite fixture; covers `getBooks`, `getHadithsByBook`, missing-DB error, ID→EN fallback at
  repository boundary.
- `app/src/test`: `HadithBooksViewModelTest`, `HadithListViewModelTest` — mockk repo,
  `TestDispatcherProvider`, `StandardTestDispatcher`, `advanceUntilIdle`; covers load success/
  error, `"system"`/`"id"`/`"en"`/`"both"` language resolution.
- `app/src/androidTest`: `HadithBooksScreenTest`, `HadithDetailScreenTest` — Compose UI tests
  (existing compose BOM + mockk-android setup).

## 8. Out of Scope (deferred)

- Search within/ across hadith.
- Favorites / bookmark integration.
- Share / copy.
- Font-size controls.
- Additional collections (Tirmidhi, Abu Dawud, etc.).
- A Settings toggle specific to hadith (language reuses the Quran setting).