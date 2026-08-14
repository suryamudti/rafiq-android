# Hadith Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add global full-text search across all hadiths (Sahih Bukhari + Muslim, 14,734 rows) with a dedicated search screen, live debounced as-you-type results, highlighted snippets, and tap-through to the existing hadith detail screen.

**Architecture:** Parameterized SQLite `LIKE` queries in the existing raw-SQLite `HadithRepositoryImpl` (`:data`), exposed through new `HadithRepository` interface methods (`:domain`). New `HadithSearchViewModel` + `HadithSearchScreen` (`:app`) follow the Prophets/Asmaul Husna search pattern. `HadithDetailScreen` gets a small `loadById` fix so it works when navigated to directly from search results.

**Tech Stack:** Kotlin 2.0.0, Compose (Material3), Hilt (KAPT), Navigation3 (`@Serializable` NavKeys), DataStore (`PreferencesManager`), Robolectric + mockk for JVM tests, Compose UI tests for instrumented tests.

## Global Constraints

- Hilt uses KAPT (`plugins.kapt`), **not** KSP. Never replace it.
- Material Icons: `material-icons-core` only (`Icons.Filled.Search` is available — used in `AyahScreen.kt:35`). Never add `material-icons-extended`.
- Arabic font loads via `FontFamily(Font(R.font.me_quran))` — never `fontResource()`.
- All user prefs via `PreferencesManager` (DataStore), never SharedPreferences.
- `AppError.displayMessage` is the single error-message source (`core/.../AppError.kt:10`).
- Cross-module smart casts from nullable don't work — use `!!` (if guarded) or `?:` / local `val`.
- Do NOT create new Room entities/DAOs or use cases — content is a read-only asset DB; VMs use repos directly.
- No changes to `DatabaseCopier` internals and no schema/asset-DB changes.
- String resources are added to BOTH `app/src/main/res/values/strings.xml` and `app/src/main/res/values-id/strings.xml`.
- DB asset `hadiths/hadith.db` flattens to `hadiths_hadith.db` in the repo's existing `getDatabase()` path.

## Current State (verified)

- `HadithRepository` interface: `getBooks()`, `getHadithsByBook(bookId)` only (`domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/HadithRepository.kt`).
- `HadithRepositoryImpl` opens `hadith.db` read-only via `DatabaseCopier.copyDatabaseIfNeeded("hadiths/hadith.db")` + `SQLiteDatabase.openDatabase`; reads with `rawQuery`; maps columns manually (`data/src/main/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImpl.kt`).
- Schema: `books(id TEXT PK, collection TEXT, number INTEGER, name_ar, name_en, name_id)`; `hadiths(id INTEGER PK, book_id TEXT, in_book_number INTEGER, narrator_ar, narrator_en, text_ar, text_en, text_id)`; index `idx_hadiths_book`.
- `HadithDetailScreen(hadithId)` reuses `HadithListViewModel` and does `state.hadiths.find { it.id == hadithId }` but NEVER calls `load()` — so it only works when the VM was pre-populated. Search → detail needs `loadById`.
- Existing search pattern: `ProphetsViewModel.search()` + `filterProphets()` (in-memory), `AsmaulHusnaScreen` TextField. `AyahViewModel.getTranslationText` language resolution: `if (lang == "system") currentLocaleCode() else lang`.
- Nav: keys in `NavigationKeys.kt`, entries in `Navigation.kt` via `entry<Key> { key -> ... }`.

## What We're NOT Doing

- No FTS5 / relevance ranking, no pagination beyond `LIMIT 100`, no Arabic diacritic normalization.
- No search within a single book (global only).
- No new dependencies, no DB asset changes.

---

## Implementation Approach

| Layer | Mirrors |
|---|---|
| `:domain` interface methods | existing `HadithRepository` methods |
| `:data` impl | existing `rawQuery` + manual cursor mapping in `HadithRepositoryImpl` |
| `HadithSearchViewModel` | `ProphetsViewModel` (search state) + `AyahViewModel` (lang resolution) |
| `HadithSearchScreen` | `AsmaulHusnaScreen` TextField + `HadithListScreen` card list |
| Nav | `NavigationKeys.kt` + `Navigation.kt` entries |
| Tests | `HadithRepositoryImplTest`, `HadithListViewModelTest`, `HadithSearchViewModelTest`, `HadithSearchScreenTest` |

**Task order:** data first (interface + impl + repo tests) → detail-screen fix → search VM → search screen + strings → navigation wiring. Each task compiles and passes its own tests before the next.

---

### Task 1: Domain interface + data implementation + repository tests

Adds `searchHadiths` and `getHadithById` to the interface and implements them with parameterized SQL. Delivers a testable searchable repository.

**Files:**
- Modify: `domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/HadithRepository.kt`
- Modify: `data/src/main/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImpl.kt`
- Test: `data/src/test/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `com.smiledev.rafiq.core.Result`, `com.smiledev.rafiq.core.AppError`, `com.smiledev.rafiq.data.asSuccess` (all exist).
- Produces (exact signatures later tasks rely on):
  - `fun searchHadiths(query: String, limit: Int = 100): Result<List<Hadith>, AppError>`
  - `fun getHadithById(id: Int): Result<Hadith?, AppError>`

- [ ] **Step 1: Write failing repository tests**

Add these tests to `HadithRepositoryImplTest`. Extend `createFixtureDb` (see Step 3) so the fixture has 4 hadiths across 2 books with distinct text in each language, a literal `%` and literal `_`, then add the test methods:

```kotlin
@Test
fun `searchHadiths matches text_id`() {
    val result = repo.searchHadiths("shalat")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    val hadiths = (result as Result.Success).data
    assertEquals(1, hadiths.size)
    assertEquals(2, hadiths[0].id)
}

@Test
fun `searchHadiths matches text_en`() {
    val result = repo.searchHadiths("prayer")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertEquals(listOf(2), (result as Result.Success).data.map { it.id })
}

@Test
fun `searchHadiths matches text_ar`() {
    val result = repo.searchHadiths("نص")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertEquals(listOf(1, 2, 3, 4), (result as Result.Success).data.map { it.id })
}

@Test
fun `searchHadiths matches book name_en`() {
    val result = repo.searchHadiths("Revelation")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertEquals(listOf(1, 3), (result as Result.Success).data.map { it.id })
}

@Test
fun `searchHadiths matches book name_id`() {
    val result = repo.searchHadiths("Iman")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertEquals(listOf(2, 4), (result as Result.Success).data.map { it.id })
}

@Test
fun `searchHadiths returns empty for no match`() {
    val result = repo.searchHadiths("zzz-not-there")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertTrue((result as Result.Success).data.isEmpty())
}

@Test
fun `searchHadiths blank query returns empty without error`() {
    val result = repo.searchHadiths("   ")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertTrue((result as Result.Success).data.isEmpty())
}

@Test
fun `searchHadiths treats percent as literal not wildcard`() {
    val result = repo.searchHadiths("50%")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertEquals(listOf(3), (result as Result.Success).data.map { it.id })
}

@Test
fun `searchHadiths treats underscore as literal not wildcard`() {
    val result = repo.searchHadiths("under_score")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertEquals(listOf(4), (result as Result.Success).data.map { it.id })
}

@Test
fun `searchHadiths applies limit`() {
    val result = repo.searchHadiths("نص", limit = 2)

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertEquals(2, (result as Result.Success).data.size)
}

@Test
fun `searchHadiths returns Error when db file missing`() {
    dbFile.delete()

    val result = repo.searchHadiths("prayer")

    assertTrue("Expected Error but got ${result}", result is Result.Error)
    assertTrue((result as Result.Error).error is AppError.Database)
}

@Test
fun `getHadithById returns hadith when found`() {
    val result = repo.getHadithById(2)

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertEquals(2, (result as Result.Success).data?.id)
}

@Test
fun `getHadithById returns null when not found`() {
    val result = repo.getHadithById(999)

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertNull((result as Result.Success).data)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew :data:testDebugUnitTest --tests "com.smiledev.rafiq.data.repository.HadithRepositoryImplTest"`
Expected: compilation FAILS — `searchHadiths` / `getHadithById` not defined on `HadithRepository`.

- [ ] **Step 3: Update the fixture in `createFixtureDb`**

Replace the body of `createFixtureDb` in `HadithRepositoryImplTest` so it creates 4 hadiths (keep the same two books):

```kotlin
private fun createFixtureDb(file: File) {
    val db = SQLiteDatabase.openOrCreateDatabase(file, null)
    db.execSQL(
        "CREATE TABLE books (id TEXT PRIMARY KEY, collection TEXT NOT NULL, number INTEGER NOT NULL," +
            " name_ar TEXT NOT NULL, name_en TEXT NOT NULL, name_id TEXT NOT NULL)"
    )
    db.execSQL(
        "CREATE TABLE hadiths (id INTEGER PRIMARY KEY, book_id TEXT NOT NULL, in_book_number INTEGER NOT NULL," +
            " narrator_ar TEXT, narrator_en TEXT, text_ar TEXT NOT NULL, text_en TEXT NOT NULL, text_id TEXT NOT NULL)"
    )
    db.execSQL(
        "INSERT INTO books VALUES ('muslim.1','muslim',1,'كتاب الإيمان','Faith','Iman')"
    )
    db.execSQL(
        "INSERT INTO books VALUES ('bukhari.1','bukhari',1,'كتاب بدء الوحي','Revelation','Permulaan Wahyu')"
    )
    db.execSQL("INSERT INTO hadiths VALUES (1,'bukhari.1',1,'','','نص واحد','t1','satu')")
    db.execSQL("INSERT INTO hadiths VALUES (2,'muslim.1',1,'','','نص اثنان','prayer text','teks shalat')")
    db.execSQL("INSERT INTO hadiths VALUES (3,'bukhari.1',2,'','','نص ثلاثة','price 50% off','harga diskon 50%')")
    db.execSQL("INSERT INTO hadiths VALUES (4,'muslim.1',2,'','','نص أربعة','under_score text','teks garis_bawah')")
    db.close()
}
```

Note: the pre-existing `getHadithsByBook` test asserts `hadiths[0].textEn == "t1"`; the new fixture keeps hadith id 1's `text_en` as `t1`, so that assertion still passes unchanged. No other pre-existing test references the id-1 text values.

- [ ] **Step 4: Implement the interface + implementation**

Modify `HadithRepository.kt`:

```kotlin
package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook

interface HadithRepository {
    fun getBooks(): Result<List<HadithBook>, AppError>
    fun getHadithsByBook(bookId: String): Result<List<Hadith>, AppError>
    fun searchHadiths(query: String, limit: Int = 100): Result<List<Hadith>, AppError>
    fun getHadithById(id: Int): Result<Hadith?, AppError>
}
```

Add to `HadithRepositoryImpl.kt` (imports to add: `android.database.Cursor`):

```kotlin
override fun searchHadiths(query: String, limit: Int): Result<List<Hadith>, AppError> {
    val term = query.trim()
    if (term.isEmpty()) return emptyList<Hadith>().asSuccess()
    return try {
        val d = getDatabase()
        val pattern = "%${escapeLike(term)}%"
        val args = arrayOf(pattern, pattern, pattern, pattern, pattern, pattern, limit.toString())
        val cursor = d.rawQuery(
            """
            SELECT h.id, h.book_id, h.in_book_number, h.narrator_ar, h.narrator_en,
                   h.text_ar, h.text_en, h.text_id
            FROM hadiths h
            JOIN books b ON b.id = h.book_id
            WHERE h.text_id LIKE ? ESCAPE '\'
               OR h.text_en LIKE ? ESCAPE '\'
               OR h.text_ar LIKE ? ESCAPE '\'
               OR b.name_id LIKE ? ESCAPE '\'
               OR b.name_en LIKE ? ESCAPE '\'
               OR b.name_ar LIKE ? ESCAPE '\'
            ORDER BY h.id
            LIMIT ?
            """.trimIndent(),
            args
        )
        val list = mutableListOf<Hadith>()
        while (cursor.moveToNext()) {
            list.add(cursorToHadith(cursor))
        }
        cursor.close()
        list.asSuccess()
    } catch (e: Exception) {
        Result.Error(AppError.Database("Failed to search hadiths for \"$query\"", e))
    }
}

override fun getHadithById(id: Int): Result<Hadith?, AppError> {
    return try {
        val d = getDatabase()
        val cursor = d.rawQuery(
            "SELECT id, book_id, in_book_number, narrator_ar, narrator_en, text_ar, text_en, text_id" +
                " FROM hadiths WHERE id = ?",
            arrayOf(id.toString())
        )
        val hadith = if (cursor.moveToFirst()) cursorToHadith(cursor) else null
        cursor.close()
        hadith.asSuccess()
    } catch (e: Exception) {
        Result.Error(AppError.Database("Failed to load hadith $id", e))
    }
}

private fun escapeLike(term: String): String =
    term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

private fun cursorToHadith(c: Cursor): Hadith = Hadith(
    id = c.getInt(0),
    bookId = c.getString(1),
    inBookNumber = c.getInt(2),
    narratorAr = c.getString(3).ifBlank { null },
    narratorEn = c.getString(4).ifBlank { null },
    textAr = c.getString(5),
    textEn = c.getString(6),
    textId = c.getString(7)
)
```

Also refactor `getHadithsByBook` to reuse `cursorToHadith` (replace its inline `Hadith(...)` construction with `list.add(cursorToHadith(cursor))`) so the mapping stays in one place.

- [ ] **Step 5: Run repository tests to verify they pass**

Run: `.\gradlew :data:testDebugUnitTest --tests "com.smiledev.rafiq.data.repository.HadithRepositoryImplTest"`
Expected: ALL PASS (including the pre-existing `getBooks`/`getHadithsByBook` tests — fix any assertions your fixture change affected).

- [ ] **Step 6: Commit**

```bash
git add domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/HadithRepository.kt data/src/main/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImpl.kt data/src/test/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImplTest.kt
git commit -m "feat(data): hadith search and getById repository methods"
```

---

### Task 2: Detail screen `loadById` fix

Makes `HadithDetailScreen` load a single hadith by id so it works when opened directly from search results (today it only searches a pre-populated list).

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithListViewModel.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithDetailScreen.kt`
- Test: `app/src/test/java/com/smiledev/rafiq/ui/hadith/HadithListViewModelTest.kt`
- Test: `app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithDetailScreenTest.kt`

**Interfaces:**
- Consumes: `hadithRepository.getHadithById(id)` (Task 1).
- Produces: `HadithListViewModel.loadById(id: Int)`.

- [ ] **Step 1: Write failing VM tests**

Add to `HadithListViewModelTest.kt`:

```kotlin
@Test
fun `loadById populates single hadith and book`() = runTest(testDispatcher) {
    every { repository.getBooks() } returns Result.Success(listOf(book))
    every { repository.getHadithById(1) } returns Result.Success(hadith)

    val vm = createVm()
    vm.loadById(1)
    advanceUntilIdle()

    assertEquals(1, vm.uiState.value.hadiths.size)
    assertEquals(hadith.id, vm.uiState.value.hadiths[0].id)
    assertEquals("bukhari.1", vm.uiState.value.book?.id)
    assertEquals(false, vm.uiState.value.isLoading)
}

@Test
fun `loadById not found leaves empty hadiths`() = runTest(testDispatcher) {
    every { repository.getBooks() } returns Result.Success(listOf(book))
    every { repository.getHadithById(999) } returns Result.Success(null)

    val vm = createVm()
    vm.loadById(999)
    advanceUntilIdle()

    assertTrue(vm.uiState.value.hadiths.isEmpty())
}

@Test
fun `loadById error surfaces in state`() = runTest(testDispatcher) {
    every { repository.getHadithById(1) } returns Result.Error(AppError.Database("fail", null))

    val vm = createVm()
    vm.loadById(1)
    advanceUntilIdle()

    assertEquals(true, vm.uiState.value.error != null)
    assertEquals(false, vm.uiState.value.isLoading)
}
```

Add missing imports if not already present: `org.junit.Assert.assertTrue`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq.ui.hadith.HadithListViewModelTest"`
Expected: FAIL — `loadById` is unresolved.

- [ ] **Step 3: Implement `loadById` in `HadithListViewModel`**

Add after `load(bookId)`:

```kotlin
fun loadById(id: Int) {
    viewModelScope.launch(dispatcherProvider.io) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val booksResult = hadithRepository.getBooks()
        val hadithResult = hadithRepository.getHadithById(id)
        when (hadithResult) {
            is Result.Success -> {
                val hadith = hadithResult.data
                val book = hadith?.let { h ->
                    (booksResult as? Result.Success)?.data?.find { it.id == h.bookId }
                }
                _uiState.value = _uiState.value.copy(
                    hadiths = hadith?.let { listOf(it) } ?: emptyList(),
                    book = book,
                    isLoading = false
                )
            }
            is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = hadithResult.error)
        }
    }
}
```

Note the `hadith?.let { h -> ... }` binding is required — cross-module smart casts from nullable don't work.

- [ ] **Step 4: Wire `loadById` into `HadithDetailScreen`**

In `HadithDetailScreen.kt`, after `val state by viewModel.uiState.collectAsState()`, add:

```kotlin
LaunchedEffect(hadithId) { viewModel.loadById(hadithId) }
```

Add the import `androidx.compose.runtime.LaunchedEffect`.

- [ ] **Step 5: Update `HadithDetailScreenTest` fixture stubs**

In `HadithDetailScreenTest.kt`'s `viewModel()` helper, the repo is `mockk(relaxed = true)`; the new `LaunchedEffect` will call `repo.getHadithById(1)`, which a relaxed mockk would answer with an empty/nonsense `Result`. Stub it explicitly:

```kotlin
every { repo.getHadithById(1) } returns Result.Success(hadith)
```

`getBooks()` is already stubbed. All four existing tests (`showsArabicAndReferenceLine`, `enModeShowsEnglishTranslationOnly`, `idModeShowsIndonesianTranslationOnly`, `bothModeShowsBothTranslationsWithChips`) then pass unchanged because `loadById` repopulates `hadiths` with the same single hadith.

- [ ] **Step 6: Run tests to verify they pass**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq.ui.hadith.HadithListViewModelTest"`
Expected: ALL PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithListViewModel.kt app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithDetailScreen.kt app/src/test/java/com/smiledev/rafiq/ui/hadith/HadithListViewModelTest.kt app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithDetailScreenTest.kt
git commit -m "feat(hadith): load hadith detail by id so search can deep-link"
```

---

### Task 3: `HadithSearchViewModel`

State + debounced search logic. Debounce cancels the in-flight search Job on each keystroke, and a post-await query check drops stale responses.

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithSearchViewModel.kt`
- Test: `app/src/test/java/com/smiledev/rafiq/ui/hadith/HadithSearchViewModelTest.kt`

**Interfaces:**
- Consumes: `hadithRepository.searchHadiths(query, limit=100)`, `hadithRepository.getBooks()`, `PreferencesManager.translationLanguage`, `currentLocaleCode()`.
- Produces (later tasks rely on):
  - `data class HadithSearchUiState(query, results: List<Hadith>, books: List<HadithBook>, isLoading, error, translationLanguage)`
  - `val uiState: StateFlow<HadithSearchUiState>`
  - `fun search(query: String)`
  - `fun resolvedLanguage(): String`

- [ ] **Step 1: Write the failing VM test**

Create `HadithSearchViewModelTest.kt` (mirror `HadithListViewModelTest` setup — mockk repo, relaxed prefs with `MutableStateFlow("system")`, `TestDispatcherProvider` over `StandardTestDispatcher`):

```kotlin
package com.smiledev.rafiq.ui.hadith

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HadithSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: HadithRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private val book = HadithBook("bukhari.1", "bukhari", 1, "كتاب بدء الوحي", "Revelation", "Permulaan Wahyu")
    private val hadith = Hadith(1, "bukhari.1", 1, "نarrator", "Narrator", "arabic", "english", "indonesia")

    private fun createVm(): HadithSearchViewModel {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        every { repository.getBooks() } returns Result.Success(listOf(book))
        return HadithSearchViewModel(repository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `search populates results after debounce`() = runTest(testDispatcher) {
        every { repository.searchHadiths("indonesia", 100) } returns Result.Success(listOf(hadith))

        val vm = createVm()
        vm.search("indonesia")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals(listOf(hadith), vm.uiState.value.results)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `debounce cancels the earlier keystroke`() = runTest(testDispatcher) {
        every { repository.searchHadiths("indonesia", 100) } returns Result.Success(listOf(hadith))
        every { repository.searchHadiths("ind", 100) } returns Result.Success(listOf(hadith))

        val vm = createVm()
        vm.search("ind")
        advanceTimeBy(100)
        vm.search("indonesia")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals("indonesia", vm.uiState.value.query)
        assertEquals(listOf(hadith), vm.uiState.value.results)
    }

    @Test
    fun `blank query clears results without hitting repo`() = runTest(testDispatcher) {
        every { repository.searchHadiths("indonesia", 100) } returns Result.Success(listOf(hadith))

        val vm = createVm()
        vm.search("indonesia")
        advanceTimeBy(250)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.results.size)

        vm.search("   ")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.results.isEmpty())
    }

    @Test
    fun `search error surfaces in state`() = runTest(testDispatcher) {
        every { repository.searchHadiths("indonesia", 100) } returns Result.Error(AppError.Database("fail", null))

        val vm = createVm()
        vm.search("indonesia")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals(true, vm.uiState.value.error != null)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `resolvedLanguage maps system to locale code`() = runTest(testDispatcher) {
        val vm = createVm()
        assertEquals("en", vm.resolvedLanguage())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq.ui.hadith.HadithSearchViewModelTest"`
Expected: FAIL — `HadithSearchViewModel` unresolved.

- [ ] **Step 3: Implement `HadithSearchViewModel`**

Create `HadithSearchViewModel.kt`:

```kotlin
package com.smiledev.rafiq.ui.hadith

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 250L
private const val SEARCH_LIMIT = 100

@Immutable
data class HadithSearchUiState(
    val query: String = "",
    val results: List<Hadith> = emptyList(),
    val books: List<HadithBook> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val translationLanguage: String = "system"
)

@HiltViewModel
class HadithSearchViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HadithSearchUiState())
    val uiState: StateFlow<HadithSearchUiState> = _uiState

    private var searchJob: Job? = null

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.value = _uiState.value.copy(translationLanguage = lang)
            }
        }
        loadBooks()
    }

    private fun loadBooks() {
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = hadithRepository.getBooks()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(books = result.data)
                is Result.Error -> _uiState.value = _uiState.value.copy(error = result.error)
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatcherProvider.io) {
            delay(SEARCH_DEBOUNCE_MS)
            val term = _uiState.value.query.trim()
            if (term.isEmpty()) {
                _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false, error = null)
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = hadithRepository.searchHadiths(term, SEARCH_LIMIT)) {
                is Result.Success -> {
                    if (_uiState.value.query.trim() == term) {
                        _uiState.value = _uiState.value.copy(results = result.data, isLoading = false)
                    }
                }
                is Result.Error -> {
                    if (_uiState.value.query.trim() == term) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                    }
                }
            }
        }
    }

    fun resolvedLanguage(): String {
        val lang = _uiState.value.translationLanguage
        return if (lang == "system") currentLocaleCode() else lang
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq.ui.hadith.HadithSearchViewModelTest"`
Expected: ALL PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithSearchViewModel.kt app/src/test/java/com/smiledev/rafiq/ui/hadith/HadithSearchViewModelTest.kt
git commit -m "feat(hadith): debounced hadith search view model"
```

---

### Task 4: `HadithSearchScreen` + strings

The dedicated search screen: search field, hint, no-results, and result cards with highlighted snippets, plus the bilingual strings.

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithSearchScreen.kt`
- Test: `app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithSearchScreenTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-id/strings.xml`

**Interfaces:**
- Consumes: `HadithSearchViewModel` (Task 3) with `uiState`, `search()`, `resolvedLanguage()`.
- Produces: `@Composable fun HadithSearchScreen(onHadithClick: (Int) -> Unit, onBack: () -> Unit, viewModel: HadithSearchViewModel = hiltViewModel(), modifier: Modifier = Modifier)`.

- [ ] **Step 1: Add string resources**

In `app/src/main/res/values/strings.xml`, inside the `<!-- Hadith -->` block:

```xml
<string name="search_hadiths">Search hadiths…</string>
<string name="search_hadiths_hint">Type to search the whole hadith collection</string>
<string name="no_hadiths_match">No hadiths match \"%s\"</string>
```

In `app/src/main/res/values-id/strings.xml`, inside the `<!-- Hadith -->` block:

```xml
<string name="search_hadiths">Cari hadis…</string>
<string name="search_hadiths_hint">Ketik untuk mencari seluruh koleksi hadis</string>
<string name="no_hadiths_match">Tidak ada hadis yang cocok dengan \"%s\"</string>
```

- [ ] **Step 2: Write the failing Compose UI test**

Create `HadithSearchScreenTest.kt` (mirror `HadithDetailScreenTest` setup — `createComposeRule`, anonymous `DispatcherProvider` over `UnconfinedTestDispatcher(testScope.testScheduler)`). Because the VM debounces with `delay(250)`, call `search()` then advance the test scheduler BEFORE `setContent` so results are ready:

```kotlin
package com.smiledev.rafiq.ui.hadith

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class HadithSearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testScope = TestScope()
    private val testDispatcher = UnconfinedTestDispatcher(testScope.testScheduler)

    private fun dispatcher() = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
        override val unconfined = testDispatcher
    }

    private val book = HadithBook("bukhari.1", "bukhari", 1, "كتاب بدء الوحي", "Revelation", "Permulaan Wahyu")
    private val hadith = Hadith(1, "bukhari.1", 1, "Narrator", "Narrator", "نص عربي", "English text", "Teks Indonesia")

    private fun viewModel(query: String, result: List<Hadith>): HadithSearchViewModel {
        val repo = mockk<HadithRepository>(relaxed = true)
        every { repo.getBooks() } returns Result.Success(listOf(book))
        every { repo.searchHadiths(any(), 100) } returns Result.Success(result)
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.translationLanguage } returns MutableStateFlow("en")
        return HadithSearchViewModel(repo, prefs, dispatcher()).apply {
            search(query)
            testScope.testScheduler.advanceUntilIdle()
        }
    }

    @Test
    fun showsHintWhenQueryBlank() {
        composeTestRule.setContent {
            HadithSearchScreen(onHadithClick = {}, onBack = {}, viewModel = viewModel("", emptyList()))
        }

        composeTestRule.onNodeWithText("Type to search the whole hadith collection").assertIsDisplayed()
    }

    @Test
    fun showsResultAndTappingCallsOnHadithClick() {
        var clicked: Int? = null
        composeTestRule.setContent {
            HadithSearchScreen(onHadithClick = { clicked = it }, onBack = {}, viewModel = viewModel("English", listOf(hadith)))
        }

        composeTestRule.onNodeWithText("English text").assertIsDisplayed()
        composeTestRule.onNodeWithText("English text").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, clicked)
    }

    @Test
    fun showsNoMatchMessageWhenQueryHasNoResults() {
        composeTestRule.setContent {
            HadithSearchScreen(onHadithClick = {}, onBack = {}, viewModel = viewModel("zzz", emptyList()))
        }

        composeTestRule.onNodeWithText("No hadiths match \"zzz\"").assertIsDisplayed()
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `.\gradlew :app:connectedDebugAndroidTest --tests "com.smiledev.rafiq.ui.hadith.HadithSearchScreenTest"` (emulator `Medium_Phone_API_35` required)
Expected: FAIL — `HadithSearchScreen` unresolved.

- [ ] **Step 4: Implement `HadithSearchScreen`**

Create `HadithSearchScreen.kt`:

```kotlin
package com.smiledev.rafiq.ui.hadith

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.displayMessage
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook

private val arabicFont = FontFamily(Font(R.font.me_quran))

private fun collectionName(collection: String): String =
    if (collection == "bukhari") "al-Bukhari" else "Muslim"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithSearchScreen(
    onHadithClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: HadithSearchViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_hadiths)) },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = state.query,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text(stringResource(R.string.search_hadiths)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            when {
                state.error != null -> Text(
                    text = state.error?.displayMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
                state.query.isBlank() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_hadiths_hint),
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                state.isLoading && state.results.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                state.results.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_hadiths_match, state.query.trim()),
                        color = Color.Gray,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.results, key = { it.id }) { hadith ->
                        SearchResultCard(
                            hadith = hadith,
                            query = state.query.trim(),
                            book = state.books.find { it.id == hadith.bookId },
                            lang = viewModel.resolvedLanguage(),
                            onClick = { onHadithClick(hadith.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    hadith: Hadith,
    query: String,
    book: HadithBook?,
    lang: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = if (book != null) {
                    stringResource(
                        R.string.hadith_reference,
                        collectionName(book.collection),
                        book.number,
                        hadith.inBookNumber
                    )
                } else {
                    "Book ${hadith.bookId.substringAfterLast('.')} · Hadith ${hadith.inBookNumber}"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = highlightMatches(snippetFor(hadith, lang), query),
                fontFamily = if (snippetFor(hadith, lang) == hadith.textAr) arabicFont else FontFamily.Default,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun snippetFor(hadith: Hadith, lang: String): String = when {
    lang == "id" || lang == "both" -> hadith.textId.ifBlank { hadith.textEn }
    lang == "en" -> hadith.textEn.ifBlank { hadith.textId }
    else -> hadith.textAr
}

@Composable
private fun highlightMatches(text: String, query: String): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        val q = query.trim()
        if (q.isEmpty()) return@buildAnnotatedString
        val style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        var index = text.indexOf(q, ignoreCase = true)
        while (index >= 0) {
            addStyle(style, index, index + q.length)
            index = text.indexOf(q, index + q.length, ignoreCase = true)
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `.\gradlew :app:connectedDebugAndroidTest --tests "com.smiledev.rafiq.ui.hadith.HadithSearchScreenTest"` (emulator required)
Expected: ALL PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithSearchScreen.kt app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithSearchScreenTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-id/strings.xml
git commit -m "feat(hadith): hadith search screen with highlighted results"
```

---

### Task 5: Navigation wiring

Adds the `HadithSearch` route and a Search action on the hadith books top bar.

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/NavigationKeys.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq/Navigation.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithBooksScreen.kt`

**Interfaces:**
- Consumes: `HadithSearchScreen` (Task 4), `HadithList`/`HadithDetail` keys (exist).
- Produces: `@Serializable data object HadithSearch : NavKey`; `HadithBooksScreen(onHadithBookClick, onSearch, onBack, ...)`.

- [ ] **Step 1: Add the NavKey**

In `NavigationKeys.kt`, add next to the other hadith keys:

```kotlin
@Serializable data object HadithSearch : NavKey
```

- [ ] **Step 2: Wire the route**

In `Navigation.kt`, add an import `com.smiledev.rafiq.ui.hadith.HadithSearchScreen` and, after the `entry<HadithBooks>` block:

```kotlin
entry<HadithSearch> {
    HadithSearchScreen(
        onHadithClick = { id -> backStack.add(HadithDetail(id)) },
        onBack = { backStack.removeLastOrNull() },
        modifier = Modifier.safeDrawingPadding()
    )
}
```

- [ ] **Step 3: Add the Search action to `HadithBooksScreen`**

Add an `onSearch: () -> Unit = {}` parameter (default keeps the existing `HadithBooksScreenTest` call sites compiling) and a Search `IconButton` in the `TopAppBar` actions:

```kotlin
fun HadithBooksScreen(
    onHadithBookClick: (String) -> Unit,
    onSearch: () -> Unit = {},
    onBack: () -> Unit,
    viewModel: HadithBooksViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
```

TopAppBar gets:

```kotlin
actions = {
    IconButton(onClick = onSearch) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(R.string.search_hadiths)
        )
    }
},
```

Add imports: `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.filled.Search`, `androidx.compose.material3.Icon`, `androidx.compose.material3.IconButton`.

Update `Navigation.kt`'s `entry<HadithBooks>` to pass `onSearch = { backStack.add(HadithSearch) }`.

- [ ] **Step 4: Build to verify**

Run: `.\gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Hilt graph + nav wiring compile).

- [ ] **Step 5: Run the JVM test suite to confirm nothing regressed**

Run: `.\gradlew :app:testDebugUnitTest :data:testDebugUnitTest`
Expected: ALL PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/NavigationKeys.kt app/src/main/java/com/smiledev/rafiq/Navigation.kt app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithBooksScreen.kt
git commit -m "feat(hadith): wire hadith search screen into navigation"
```

---

## Verification

After all tasks:
- `.\gradlew :data:testDebugUnitTest :app:testDebugUnitTest` — all green.
- `.\gradlew :app:assembleDebug` — BUILD SUCCESSFUL.
- `adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk` then manually: Hadiths → Search icon → type a term → tap a result → detail opens.

## Self-Review Notes

- Spec coverage: search fields (Task 1 SQL covers `text_id`/`text_en`/`text_ar` + all three `books` name columns), 100-result cap (`LIMIT ?` + `SEARCH_LIMIT`), debounce (Task 3), highlight + snippet (Task 4), search → detail deep-link (Task 2 `loadById`), search entry point (Task 5), bilingual strings (Task 4), error handling (`displayMessage` + `AppError.Database`), blank-query short-circuit (Tasks 1 & 3). No FTS/ranking/pagination — deferred per spec.
- Type consistency: `searchHadiths(query, limit = 100)` and `getHadithById(id)` names match across Tasks 1–3; `HadithSearchUiState` field names used in Task 4 match Task 3; `onSearch` parameter flows Task 5 → `HadithBooksScreen` consistently.
