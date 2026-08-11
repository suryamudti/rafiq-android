# Prophets Feature Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enrich the prophets feature with authored bilingual content (facts, events, lessons, verse references) plus UI polish (loading state, localized section headers, prev/next nav) and new features (story font-size controls, share, favorites, tappable verse refs).

**Architecture:** Vertical-slice delivery. Extend the domain model and JSON schema uniformly (with tolerant parsing so the 23 not-yet-authored prophets don't break), fully author Adam (id 1) and Nuh (id 3), build all UI/features end-to-end, then batch-author the remaining 23 prophets' content.

**Tech Stack:** Kotlin 2.0, Compose Material3, Hilt (KAPT), DataStore Preferences, Navigation3 type-safe nav, JUnit4 + MockK + Robolectric tests. `material-icons-core` only.

## Global Constraints

- `material-icons-core` ONLY. Never add `material-icons-extended`. Available icons: `DateRange`, `Face`, `Favorite`, `List` (AutoMirrored), `LocationOn`, `Notifications`, `Person`, `Place`, `PlayArrow`, `Refresh`, `ShoppingCart`, `Star`. No Share/Heart-outline icons — use text buttons or tinted `Favorite`.
- Arabic font via `FontFamily(Font(R.font.me_quran))`. Never `fontResource()`.
- All user prefs via DataStore `PreferencesManager`, NOT SharedPreferences.
- Locale: `if (Locale.getDefault().language == "id") "id" else "en"` (via `currentLocaleCode()` in `:core`).
- Cross-module smart casts don't work — use `!!`/local `val` for nullable props from other modules.
- No `Math.*`; use `kotlin.math.*`.
- Hilt via KAPT (NOT KSP). Room `room-runtime` must be `api` in `:data` (already is).
- Follow existing screen patterns: `@Composable fun XxxScreen(onBack, viewModel = hiltViewModel(), modifier)`, `MutableStateFlow<UiState>` + `StateFlow`, load on `init` via `viewModelScope.launch(dispatcherProvider.io)`.
- Robolectric is available in `:data` tests (`org.robolectric:robolectric:4.13`). Use `@RunWith(RobolectricTestRunner::class)` for Context-dependent tests.
- Existing prophets JSON ids: 1=Adam, 2=Idris, 3=Nuh, 4=Hud, 5=Salih, 6=Ibrahim, 7=Lut, 8=Ismail, 9=Ishaq, 10=Yaqub, 11=Yusuf, 12=Ayyub, 13=Shu'ayb, 14=Musa, 15=Harun, 16=Dhul-Kifl, 17=Dawud, 18=Sulaiman, 19=Ilyas, 20=Al-Yasa, 21=Yunus, 22=Zakariyya, 23=Yahya, 24=Isa, 25=Muhammad.

---

### Task 1: Domain model — add `VerseRef` and extend `ProphetStory`

**Files:**
- Create: `domain/src/main/kotlin/com/smiledev/rafiq/domain/model/VerseRef.kt`
- Modify: `domain/src/main/kotlin/com/smiledev/rafiq/domain/model/ProphetStory.kt`
- Test: `domain/src/test/kotlin/com/smiledev/rafiq/domain/model/VerseRefTest.kt` (new, simple)

**Interfaces:**
- Produces: `VerseRef(surah: Int, surahNameEn: String, surahNameId: String, ayahStart: Int, ayahEnd: Int)` and extended `ProphetStory(id, nameArabic, nameEn, nameId, summaryEn, summaryId, storyEn, storyId, miraclesEn, miraclesId, eraEn, eraId, peopleEn, peopleId, lifespanEn, lifespanId, eventsEn: List<String>, eventsId: List<String>, lessonsEn: List<String>, lessonsId: List<String>, verses: List<VerseRef>)`.

- [ ] **Step 1: Write the failing test** — `VerseRefTest.kt` verifying a `VerseRef` constructs with all fields and defaults (`ayahEnd` when equal to `ayahStart`).

```kotlin
package com.smiledev.rafiq.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VerseRefTest {

    @Test
    fun `verse ref holds surah and ayah range`() {
        val ref = VerseRef(
            surah = 2,
            surahNameEn = "Al-Baqarah",
            surahNameId = "Al-Baqarah",
            ayahStart = 30,
            ayahEnd = 39
        )
        assertEquals(2, ref.surah)
        assertEquals("Al-Baqarah", ref.surahNameEn)
        assertEquals("Al-Baqarah", ref.surahNameId)
        assertEquals(30, ref.ayahStart)
        assertEquals(39, ref.ayahEnd)
    }

    @Test
    fun `verse ref supports single ayah when end equals start`() {
        val ref = VerseRef(11, "Hud", "Hud", 25, 25)
        assertEquals(25, ref.ayahEnd)
        assertEquals(25, ref.ayahStart)
    }
}
```

- [ ] **Step 2: Run test to verify it fails** — `.\gradlew :domain:testDebug` — fails: unresolved `VerseRef`.

- [ ] **Step 3: Create `VerseRef.kt`**

```kotlin
package com.smiledev.rafiq.domain.model

data class VerseRef(
    val surah: Int,
    val surahNameEn: String,
    val surahNameId: String,
    val ayahStart: Int,
    val ayahEnd: Int
)
```

- [ ] **Step 4: Extend `ProphetStory.kt`**

```kotlin
package com.smiledev.rafiq.domain.model

data class ProphetStory(
    val id: Int,
    val nameArabic: String,
    val nameEn: String,
    val nameId: String,
    val summaryEn: String,
    val summaryId: String,
    val storyEn: String,
    val storyId: String,
    val miraclesEn: String,
    val miraclesId: String,
    val eraEn: String = "",
    val eraId: String = "",
    val peopleEn: String = "",
    val peopleId: String = "",
    val lifespanEn: String = "",
    val lifespanId: String = "",
    val eventsEn: List<String> = emptyList(),
    val eventsId: List<String> = emptyList(),
    val lessonsEn: List<String> = emptyList(),
    val lessonsId: List<String> = emptyList(),
    val verses: List<VerseRef> = emptyList()
)
```

- [ ] **Step 5: Run domain tests** — `.\gradlew :domain:testDebug` — all pass. (Existing `GetProphetsUseCaseTest` uses positional args that still compile because new fields have defaults.)

- [ ] **Step 6: Commit**

```bash
git add domain/src/main/kotlin/com/smiledev/rafiq/domain/model/VerseRef.kt domain/src/main/kotlin/com/smiledev/rafiq/domain/model/ProphetStory.kt domain/src/test/kotlin/com/smiledev/rafiq/domain/model/VerseRefTest.kt
git commit -m "feat(domain): extend ProphetStory with facts, events, lessons, verse refs"
```

---

### Task 2: Data — pure JSON parser + repository update + parser test

**Files:**
- Create: `data/src/test/kotlin/com/smiledev/rafiq/data/repository/ProphetParserTest.kt`
- Modify: `data/src/main/kotlin/com/smiledev/rafiq/data/repository/ProphetRepositoryImpl.kt`

**Interfaces:**
- Consumes: extended `ProphetStory` and `VerseRef` from Task 1.
- Produces: `internal fun parseProphets(json: String): List<ProphetStory>` (top-level, pure, no Android deps, tolerant of missing new fields).

- [ ] **Step 1: Write the failing parser test** — uses inline JSON fixtures. Missing new fields must parse to empty defaults (critical for the 23 not-yet-authored prophets).

```kotlin
package com.smiledev.rafiq.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProphetParserTest {

    @Test
    fun `parses all new fields`() {
        val json = """
            [
              {
                "id": 1,
                "name_arabic": "آدم",
                "name_en": "Adam",
                "name_id": "Adam",
                "summary_en": "S",
                "summary_id": "S",
                "story_en": "S",
                "story_id": "S",
                "miracles_en": "M",
                "miracles_id": "M",
                "era_en": "Primordial era",
                "era_id": "Zaman purba",
                "people_en": "All of humanity",
                "people_id": "Seluruh umat manusia",
                "lifespan_en": "~1000 years",
                "lifespan_id": "±1000 tahun",
                "events_en": ["Created from clay", "Sent to Earth"],
                "events_id": ["Diciptakan dari tanah liat", "Diturunkan ke Bumi"],
                "lessons_en": ["Humility defeats pride"],
                "lessons_id": ["Kerendahan hati menang"],
                "verses": [
                  {"surah": 2, "surah_name_en": "Al-Baqarah", "surah_name_id": "Al-Baqarah", "ayah_start": 30, "ayah_end": 39}
                ]
              }
            ]
        """.trimIndent()

        val result = parseProphets(json)

        assertEquals(1, result.size)
        val p = result[0]
        assertEquals("Primordial era", p.eraEn)
        assertEquals("Zaman purba", p.eraId)
        assertEquals("All of humanity", p.peopleEn)
        assertEquals("~1000 years", p.lifespanEn)
        assertEquals(listOf("Created from clay", "Sent to Earth"), p.eventsEn)
        assertEquals(listOf("Diciptakan dari tanah liat", "Diturunkan ke Bumi"), p.eventsId)
        assertEquals(listOf("Humility defeats pride"), p.lessonsEn)
        assertEquals(1, p.verses.size)
        assertEquals(2, p.verses[0].surah)
        assertEquals("Al-Baqarah", p.verses[0].surahNameEn)
        assertEquals(39, p.verses[0].ayahEnd)
    }

    @Test
    fun `missing new fields default to empty`() {
        val json = """
            [
              {
                "id": 2,
                "name_arabic": "نوح",
                "name_en": "Nuh",
                "name_id": "Nuh",
                "summary_en": "S",
                "summary_id": "S",
                "story_en": "S",
                "story_id": "S",
                "miracles_en": "M",
                "miracles_id": "M"
              }
            ]
        """.trimIndent()

        val result = parseProphets(json)

        assertEquals(1, result.size)
        val p = result[0]
        assertEquals("", p.eraEn)
        assertTrue(p.eventsEn.isEmpty())
        assertTrue(p.lessonsId.isEmpty())
        assertTrue(p.verses.isEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails** — `.\gradlew :data:testDebug --tests "com.smiledev.rafiq.data.repository.ProphetParserTest"` — fails: unresolved `parseProphets`.

- [ ] **Step 3: Add `parseProphets` and update the repository**

Add top-level `parseProphets` in `ProphetRepositoryImpl.kt` (same file, after class) and refactor `getProphets()` to use it:

```kotlin
internal fun parseProphets(json: String): List<ProphetStory> {
    val arr = JSONArray(json)
    val list = mutableListOf<ProphetStory>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(
            ProphetStory(
                id = obj.getInt("id"),
                nameArabic = obj.getString("name_arabic"),
                nameEn = obj.getString("name_en"),
                nameId = obj.getString("name_id"),
                summaryEn = obj.getString("summary_en"),
                summaryId = obj.getString("summary_id"),
                storyEn = obj.getString("story_en"),
                storyId = obj.getString("story_id"),
                miraclesEn = obj.getString("miracles_en"),
                miraclesId = obj.getString("miracles_id"),
                eraEn = obj.optString("era_en"),
                eraId = obj.optString("era_id"),
                peopleEn = obj.optString("people_en"),
                peopleId = obj.optString("people_id"),
                lifespanEn = obj.optString("lifespan_en"),
                lifespanId = obj.optString("lifespan_id"),
                eventsEn = obj.optJSONArray("events_en").toStringList(),
                eventsId = obj.optJSONArray("events_id").toStringList(),
                lessonsEn = obj.optJSONArray("lessons_en").toStringList(),
                lessonsId = obj.optJSONArray("lessons_id").toStringList(),
                verses = obj.optJSONArray("verses").toVerseRefs()
            )
        )
    }
    return list
}

private fun org.json.JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).map { optString(it) }
}

private fun org.json.JSONArray?.toVerseRefs(): List<VerseRef> {
    if (this == null) return emptyList()
    return (0 until length()).map { i ->
        val v = getJSONObject(i)
        VerseRef(
            surah = v.getInt("surah"),
            surahNameEn = v.getString("surah_name_en"),
            surahNameId = v.getString("surah_name_id"),
            ayahStart = v.getInt("ayah_start"),
            ayahEnd = v.getInt("ayah_end")
        )
    }
}
```

Refactor `getProphets()` body:

```kotlin
override fun getProphets(): Result<List<ProphetStory>, AppError> {
    return try {
        if (cache != null) return cache!!.asSuccess()
        val text = readAssetText("quran-data/prophets/prophets.json")
        val list = parseProphets(text)
        cache = list
        list.asSuccess()
    } catch (e: Exception) {
        Result.Error(AppError.Database("Failed to load prophets", e))
    }
}
```

Replace `readAssetJsonArray` with:

```kotlin
private fun readAssetText(path: String): String {
    val stream = context.assets.open(path)
    val reader = BufferedReader(InputStreamReader(stream))
    val text = reader.readText()
    reader.close()
    return text
}
```

Add imports: `com.smiledev.rafiq.domain.model.VerseRef`. Remove the unused `JSONArray` import if no longer referenced in the class body.

- [ ] **Step 4: Run data tests** — `.\gradlew :data:testDebug` — parser tests pass; existing data tests unaffected.

- [ ] **Step 5: Commit**

```bash
git add data/src/test/kotlin/com/smiledev/rafiq/data/repository/ProphetParserTest.kt data/src/main/kotlin/com/smiledev/rafiq/data/repository/ProphetRepositoryImpl.kt
git commit -m "feat(data): pure prophet JSON parser with tolerant new-field parsing"
```

---

### Task 3: Data — PreferencesManager favorites + story font size

**Files:**
- Modify: `data/src/main/kotlin/com/smiledev/rafiq/data/preferences/PreferencesManager.kt`

**Interfaces:**
- Produces: `val favoriteProphetIds: Flow<Set<Int>>`, `suspend fun toggleFavoriteProphet(id: Int)`, `val storyFontSize: Flow<Int>`, `suspend fun setStoryFontSize(size: Int)`.

- [ ] **Step 1: Add keys to companion object** (after `LAST_READ_AYA`):

```kotlin
val FAVORITE_PROPHET_IDS = stringSetPreferencesKey("favorite_prophet_ids")
val STORY_FONT_SIZE = intPreferencesKey("story_font_size")
```

Add imports: `androidx.datastore.preferences.core.stringSetPreferencesKey`.

- [ ] **Step 2: Add flows + setters** (after `lastReadAya` / `setLastReadPosition`):

```kotlin
val favoriteProphetIds: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
    prefs[FAVORITE_PROPHET_IDS].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
}

val storyFontSize: Flow<Int> = context.dataStore.data.map { prefs ->
    prefs[STORY_FONT_SIZE] ?: 16
}

suspend fun toggleFavoriteProphet(id: Int) {
    context.dataStore.edit { prefs ->
        val current = prefs[FAVORITE_PROPHET_IDS].orEmpty()
        val updated = if (id.toString() in current) current - id.toString() else current + id.toString()
        prefs[FAVORITE_PROPHET_IDS] = updated
    }
}

suspend fun setStoryFontSize(size: Int) {
    context.dataStore.edit { prefs -> prefs[STORY_FONT_SIZE] = size }
}
```

Note: `Set<String>` from `stringSetPreferencesKey` is stored as strings; map `toIntOrNull` for `Set<Int>`.

- [ ] **Step 3: Verify compile** — `.\gradlew :data:compileDebugKotlin`. No new unit test (DataStore requires instrumentation; existing `PreferencesManager` has no test and this follows that precedent).

- [ ] **Step 4: Commit**

```bash
git add data/src/main/kotlin/com/smiledev/rafiq/data/preferences/PreferencesManager.kt
git commit -m "feat(data): prophet favorites + story font size prefs in DataStore"
```

---

### Task 4: App — ProphetsViewModel favorites, filter, Arabic search, font size, not-found

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/prophets/ProphetsViewModel.kt`
- Modify: `app/src/test/java/com/smiledev/rafiq/ui/prophets/ProphetsViewModelTest.kt`

**Interfaces:**
- Consumes: `PreferencesManager.favoriteProphetIds`/`storyFontSize`/`toggleFavoriteProphet`/`setStoryFontSize` (Task 3).
- Produces: UiState gains `favoriteIds: Set<Int>`, `showFavoritesOnly: Boolean`, `storyFontSize: Int`. New methods `setShowFavoritesOnly(Boolean)`, `toggleFavorite(Int)`, `setStoryFontSize(Int)`. `filteredProphets()` now matches Arabic names and favorites-only. Constructor gains `preferencesManager: PreferencesManager`.

- [ ] **Step 1: Update the failing tests first** — rewrite `ProphetsViewModelTest` with a mocked `PreferencesManager`:

```kotlin
package com.smiledev.rafiq.ui.prophets

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.ProphetStory
import com.smiledev.rafiq.domain.repository.ProphetRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProphetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val prophetRepository: ProphetRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()

    private fun prophet(id: Int, arabic: String, en: String, idName: String) = ProphetStory(
        id = id, nameArabic = arabic, nameEn = en, nameId = idName,
        summaryEn = "S", summaryId = "S", storyEn = "S", storyId = "S",
        miraclesEn = "M", miraclesId = "M"
    )

    private fun newVm(initialFavorites: Set<Int> = emptySet()): ProphetsViewModel {
        every { preferencesManager.favoriteProphetIds } returns flowOf(initialFavorites)
        every { preferencesManager.storyFontSize } returns flowOf(16)
        coEvery { preferencesManager.toggleFavoriteProphet(any()) } returns Unit
        coEvery { preferencesManager.setStoryFontSize(any()) } returns Unit
        return ProphetsViewModel(prophetRepository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `load prophets success`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(listOf(prophet(1, "آدم", "Adam", "Adam")))

        val vm = newVm()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.prophets.size)
    }

    @Test
    fun `load prophets error`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Error(AppError.Database("fail", null))

        val vm = newVm()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.prophets.size)
    }

    @Test
    fun `filteredProphets returns all when query empty`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(
            listOf(prophet(1, "آدم", "Adam", "Adam"), prophet(2, "نوح", "Noah", "Nuh"))
        )

        val vm = newVm()
        advanceUntilIdle()

        assertEquals(2, vm.filteredProphets().size)
    }

    @Test
    fun `filteredProphets filters by query`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(
            listOf(prophet(1, "آدم", "Adam", "Adam"), prophet(2, "نوح", "Noah", "Nuh"))
        )

        val vm = newVm()
        advanceUntilIdle()

        vm.search("Noah")
        assertEquals(1, vm.filteredProphets().size)
    }

    @Test
    fun `filteredProphets matches arabic name`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(
            listOf(prophet(1, "آدم", "Adam", "Adam"), prophet(2, "نوح", "Noah", "Nuh"))
        )

        val vm = newVm()
        advanceUntilIdle()

        vm.search("نوح")
        assertEquals(1, vm.filteredProphets().size)
        assertEquals(2, vm.filteredProphets()[0].id)
    }

    @Test
    fun `favorites only filters by favorite ids`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(
            listOf(prophet(1, "آدم", "Adam", "Adam"), prophet(2, "نوح", "Noah", "Nuh"))
        )

        val vm = newVm(initialFavorites = setOf(2))
        advanceUntilIdle()

        vm.setShowFavoritesOnly(true)
        assertEquals(1, vm.filteredProphets().size)
        assertEquals(2, vm.filteredProphets()[0].id)
    }

    @Test
    fun `toggleFavorite persists and updates state`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(emptyList())
        val vm = newVm(initialFavorites = setOf(1))
        advanceUntilIdle()

        vm.toggleFavorite(1)
        advanceUntilIdle()

        coVerify(exactly = 1) { preferencesManager.toggleFavoriteProphet(1) }
        assertTrue(1 !in vm.uiState.value.favoriteIds)
    }

    @Test
    fun `setStoryFontSize persists`() = runTest(testDispatcher) {
        every { prophetRepository.getProphets() } returns Result.Success(emptyList())
        val vm = newVm()
        advanceUntilIdle()

        vm.setStoryFontSize(24)
        advanceUntilIdle()

        coVerify(exactly = 1) { preferencesManager.setStoryFontSize(24) }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail** — `.\gradlew :app:testDebug --tests "com.smiledev.rafiq.ui.prophets.ProphetsViewModelTest"` — fails: constructor signature mismatch.

- [ ] **Step 3: Update `ProphetsViewModel`**

```kotlin
@Immutable
data class ProphetsUiState(
    val prophets: List<ProphetStory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val favoriteIds: Set<Int> = emptySet(),
    val showFavoritesOnly: Boolean = false,
    val storyFontSize: Int = 16
)

@HiltViewModel
class ProphetsViewModel @Inject constructor(
    private val prophetRepository: ProphetRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProphetsUiState())
    val uiState: StateFlow<ProphetsUiState> = _uiState

    val localeCode = currentLocaleCode()

    init {
        loadProphets()
        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                preferencesManager.favoriteProphetIds,
                preferencesManager.storyFontSize
            ) { favIds, size ->
                _uiState.value = _uiState.value.copy(
                    favoriteIds = favIds,
                    storyFontSize = size
                )
            }.collect()
        }
    }

    fun loadProphets() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = prophetRepository.getProphets()
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(prophets = result.data, isLoading = false)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setShowFavoritesOnly(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFavoritesOnly = show)
    }

    fun toggleFavorite(id: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.toggleFavoriteProphet(id)
        }
    }

    fun setStoryFontSize(size: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.setStoryFontSize(size)
        }
    }

    fun filteredProphets(): List<ProphetStory> {
        val state = _uiState.value
        val q = state.searchQuery.lowercase()
        return state.prophets.filter { p ->
            val favoriteOk = !state.showFavoritesOnly || p.id in state.favoriteIds
            val searchOk = q.isEmpty() ||
                p.nameEn.lowercase().contains(q) ||
                p.nameId.lowercase().contains(q) ||
                p.nameArabic.lowercase().contains(q)
            favoriteOk && searchOk
        }
    }
}
```

Add imports: `com.smiledev.rafiq.data.preferences.PreferencesManager`, `kotlinx.coroutines.flow.combine`.

Note: `toggleFavorite` updates DataStore; `favoriteIds` in state is updated by the DataStore flow emission. The test verifies persistence call; the in-memory `favoriteIds` reflects the flow source `flowOf(initialFavorites)` minus nothing (mockk flow doesn't re-emit), so the assertion `1 !in favoriteIds` is true only if we also update state — see next step.

- [ ] **Step 3b: Make `toggleFavorite` update local state immediately** (so UI is responsive without waiting for DataStore round-trip):

```kotlin
fun toggleFavorite(id: Int) {
    val current = _uiState.value.favoriteIds
    _uiState.value = _uiState.value.copy(
        favoriteIds = if (id in current) current - id else current + id
    )
    viewModelScope.launch(dispatcherProvider.io) {
        preferencesManager.toggleFavoriteProphet(id)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass** — `.\gradlew :app:testDebug --tests "com.smiledev.rafiq.ui.prophets.ProphetsViewModelTest"` — all pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/prophets/ProphetsViewModel.kt app/src/test/java/com/smiledev/rafiq/ui/prophets/ProphetsViewModelTest.kt
git commit -m "feat(prophets): favorites filter, arabic search, story font size in ViewModel"
```

---

### Task 5: Content — author Adam (1) and Nuh (3) new JSON fields

**Files:**
- Modify: `app/src/main/assets/quran-data/prophets/prophets.json`

**Interfaces:**
- Consumes: JSON keys from Task 2 parser (`era_en`, `era_id`, `people_en`, `people_id`, `lifespan_en`, `lifespan_id`, `events_en`, `events_id`, `lessons_en`, `lessons_id`, `verses`).

- [ ] **Step 1: Add fields to Adam (id 1)** — add these keys to the Adam object:

```json
"era_en": "Primordial era — the beginning of humanity",
"era_id": "Zaman purba — awal mula umat manusia",
"people_en": "All of humanity; his descendants",
"people_id": "Seluruh umat manusia; keturunannya",
"lifespan_en": "Reported to have lived about 1,000 years",
"lifespan_id": "Diriwayatkan hidup sekitar 1.000 tahun",
"events_en": [
  "Created by Allah from clay and given the breath of life",
  "Taught the names of all things",
  "Angels prostrated to him; Iblis refused out of pride",
  "Adam and Hawwa (Eve) sent to Earth after the forbidden tree",
  "Repented sincerely and was forgiven by Allah"
],
"events_id": [
  "Diciptakan oleh Allah dari tanah liat dan ditiupkan ruh kehidupan",
  "Diajarkan nama-nama segala sesuatu",
  "Para malaikat bersujud kepadanya; Iblis menolak karena sombong",
  "Adam dan Hawa diturunkan ke Bumi setelah memakan buah terlarang",
  "Bertaubat dengan tulus dan diampuni oleh Allah"
],
"lessons_en": [
  "Pride was Iblis's downfall — humility is the path to Allah",
  "Sincere repentance erases sin",
  "Humans were created with dignity and knowledge"
],
"lessons_id": [
  "Kesombongan menjadi kejatuhan Iblis — kerendahan hati adalah jalan menuju Allah",
  "Taubat yang tulus menghapus dosa",
  "Manusia diciptakan dengan kemuliaan dan ilmu"
],
"verses": [
  {"surah": 2, "surah_name_en": "Al-Baqarah", "surah_name_id": "Al-Baqarah", "ayah_start": 30, "ayah_end": 39},
  {"surah": 7, "surah_name_en": "Al-A'raf", "surah_name_id": "Al-A'raf", "ayah_start": 11, "ayah_end": 25},
  {"surah": 20, "surah_name_en": "Ta-Ha", "surah_name_id": "Thaha", "ayah_start": 115, "ayah_end": 123}
]
```

- [ ] **Step 2: Add fields to Nuh (id 3)**:

```json
"era_en": "Ancient era — generations after Adam",
"era_id": "Zaman dahulu kala — beberapa generasi setelah Adam",
"people_en": "His people, who worshipped idols",
"people_id": "Kaumnya yang menyembah berhala",
"lifespan_en": "Called his people to Allah for 950 years",
"lifespan_id": "Menyeru kaumnya kepada Allah selama 950 tahun",
"events_en": [
  "Called his people to monotheism for 950 years",
  "His people rejected and mocked him",
  "Built the ark by Allah's command",
  "The flood drowned the disbelievers",
  "The believers were saved aboard the ark"
],
"events_id": [
  "Menyeru kaumnya kepada tauhid selama 950 tahun",
  "Kaumnya menolak dan mencemoohnya",
  "Membangun bahtera atas perintah Allah",
  "Banjir besar menenggelamkan orang-orang kafir",
  "Orang-orang beriman selamat di atas bahtera"
],
"lessons_en": [
  "Persevere in calling to the truth even when mocked",
  "Patience over long-term struggle is rewarded",
  "Trust in Allah's command and follow it without hesitation"
],
"lessons_id": [
  "Istiqamah menyeru kebenaran meski diejek",
  "Kesabaran dalam perjuangan panjang akan berbuah",
  "Percaya pada perintah Allah dan melaksanakannya tanpa ragu"
],
"verses": [
  {"surah": 11, "surah_name_en": "Hud", "surah_name_id": "Hud", "ayah_start": 25, "ayah_end": 48},
  {"surah": 71, "surah_name_en": "Nuh", "surah_name_id": "Nuh", "ayah_start": 1, "ayah_end": 28}
]
```

- [ ] **Step 3: Validate JSON parses** — `.\gradlew :data:testDebug` still passes, and add a quick Robolectric check by running the app test:

`.\gradlew :app:testDebug --tests "com.smiledev.rafiq.ui.prophets.ProphetsViewModelTest"`

(Verifies nothing structural broke; real file parse is covered in Task 11 emulator/verification.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/quran-data/prophets/prophets.json
git commit -m "content(prophets): author Adam and Nuh facts, events, lessons, verses"
```

---

### Task 6: App — strings EN + ID

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-id/strings.xml`

- [ ] **Step 1: Add EN strings** after the Prophets block (line ~50):

```xml
    <string name="prophets_summary">Summary</string>
    <string name="prophets_story">Story</string>
    <string name="prophets_miracles">Miracles</string>
    <string name="prophets_facts">Facts</string>
    <string name="prophets_key_events">Key Events</string>
    <string name="prophets_lessons">Lessons</string>
    <string name="prophets_verse_references">Verse References</string>
    <string name="prophets_era">Era</string>
    <string name="prophets_people">People</string>
    <string name="prophets_lifespan">Lifespan</string>
    <string name="prophets_favorites">Favorites</string>
    <string name="no_favorite_prophets">No favorite prophets yet</string>
    <string name="prophet_not_found">Prophet not found</string>
    <string name="prophets_share">Share</string>
    <string name="prophets_previous">Previous</string>
    <string name="prophets_next">Next</string>
    <string name="prophets_story_font_size">Story Font Size: %dsp</string>
    <string name="verse_ref">%1$s %2$d:%3$d</string>
```

- [ ] **Step 2: Add ID strings** after the Prophets block in `values-id` (line ~50):

```xml
    <string name="prophets_summary">Ringkasan</string>
    <string name="prophets_story">Kisah</string>
    <string name="prophets_miracles">Mukjizat</string>
    <string name="prophets_facts">Fakta</string>
    <string name="prophets_key_events">Peristiwa Penting</string>
    <string name="prophets_lessons">Pelajaran</string>
    <string name="prophets_verse_references">Referensi Ayat</string>
    <string name="prophets_era">Era</string>
    <string name="prophets_people">Kaum</string>
    <string name="prophets_lifespan">Usia</string>
    <string name="prophets_favorites">Favorit</string>
    <string name="no_favorite_prophets">Belum ada nabi favorit</string>
    <string name="prophet_not_found">Nabi tidak ditemukan</string>
    <string name="prophets_share">Bagikan</string>
    <string name="prophets_previous">Sebelumnya</string>
    <string name="prophets_next">Berikutnya</string>
    <string name="prophets_story_font_size">Ukuran Huruf Kisah: %dsp</string>
    <string name="verse_ref">%1$s %2$d:%3$d</string>
```

- [ ] **Step 3: Verify** — `.\gradlew :app:processDebugResources` (or full `assembleDebug` in Task 11). No duplicates.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-id/strings.xml
git commit -m "feat(prophets): localized section headers and action strings (EN + ID)"
```

---

### Task 7: App — navigation wiring for verse refs + prophet change

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/Navigation.kt`

**Interfaces:**
- Consumes: `ProphetDetailScreen` gains `onVerseRefClick: (surah: Int, surahName: String, ayaStart: Int) -> Unit` and `onProphetNavigate: (Int) -> Unit` (Task 9).
- Produces: Nav wiring reuses existing `Ayah` NavKey: `backStack.add(Ayah(surah, surahName, ayaStart))`; `onProphetNavigate` replaces top entry via `backStack.removeLastOrNull(); backStack.add(ProphetDetail(id))`.

- [ ] **Step 1: Update the `entry<ProphetDetail>` block** (lines 89-95):

```kotlin
entry<ProphetDetail> { key ->
    ProphetDetailScreen(
        prophetId = key.prophetId,
        onBack = { backStack.removeLastOrNull() },
        onVerseRefClick = { surah, surahName, ayaStart ->
            backStack.add(Ayah(suraNumber = surah, suraName = surahName, scrollToAya = ayaStart))
        },
        onProphetNavigate = { id ->
            backStack.removeLastOrNull()
            backStack.add(ProphetDetail(id))
        },
        modifier = Modifier.safeDrawingPadding()
    )
}
```

- [ ] **Step 2: Verify compile** — `.\gradlew :app:compileDebugKotlin`. Will fail until Task 9 adds the params; acceptable mid-plan (Task 9 completes the signature). Alternatively implement Task 9 before this. If you see failure, complete Task 9 first, then return to verify.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/Navigation.kt
git commit -m "feat(prophets): wire verse-ref and prev/next navigation"
```

---

### Task 8: App — ProphetsScreen polish

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/prophets/ProphetsScreen.kt`

**Interfaces:**
- Consumes: `ProphetsViewModel` new state (`isLoading`, `showFavoritesOnly`, `favoriteIds`) and methods (`setShowFavoritesOnly`, `toggleFavorite`).
- Produces: loading spinner, favorites filter chip, empty-favorites message, no debug `(id)`.

- [ ] **Step 1: Update `ProphetsScreen`**

Replace the body of the `Box` (error-handling branch) so that:
1. Loading state shows a centered `CircularProgressIndicator` when `state.isLoading && state.prophets.isEmpty()`.
2. A `FilterChip` (label `stringResource(R.string.prophets_favorites)`, `selected = state.showFavoritesOnly`, `onClick = { viewModel.setShowFavoritesOnly(!state.showFavoritesOnly) }`) sits below the search TextField in a `Row`.
3. Remove the `"(${prophet.id})"` text from each card.
4. When `state.showFavoritesOnly && filtered.isEmpty()`, show `stringResource(R.string.no_favorite_prophets)` instead of the no-match message.

Imports to add: `androidx.compose.material3.FilterChip`, `androidx.compose.foundation.layout.Row`, `androidx.compose.foundation.layout.Arrangement`.

The updated Box:

```kotlin
Box(modifier = Modifier.fillMaxSize().padding(padding)) {
    when {
        state.isLoading && state.prophets.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.error != null -> Text(
            text = state.error?.displayMessage ?: "",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        )
        else -> {
            Column {
                TextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.search(it) },
                    placeholder = { Text(stringResource(R.string.search_prophets)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.showFavoritesOnly,
                        onClick = { viewModel.setShowFavoritesOnly(!state.showFavoritesOnly) },
                        label = { Text(stringResource(R.string.prophets_favorites)) }
                    )
                }
                if (filtered.isEmpty()) {
                    Text(
                        text = if (state.showFavoritesOnly) {
                            stringResource(R.string.no_favorite_prophets)
                        } else {
                            stringResource(R.string.no_prophets_match, state.searchQuery)
                        },
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        color = Color.Gray
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                    ) {
                        items(filtered) { prophet ->
                            val localizedName = if (viewModel.localeCode == "id") prophet.nameId else prophet.nameEn
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .clickable { onProphetClick(prophet.id) },
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = prophet.nameArabic,
                                        fontFamily = arabicFont,
                                        fontSize = 22.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = localizedName,
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile** — `.\gradlew :app:compileDebugKotlin`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/prophets/ProphetsScreen.kt
git commit -m "feat(prophets): list loading state, favorites filter chip, drop debug id"
```

---

### Task 9: App — ProphetDetailScreen features

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/prophets/ProphetDetailScreen.kt`

**Interfaces:**
- Consumes: extended `ProphetStory` fields, `VerseRef`, ViewModel (`favoriteIds`, `toggleFavorite`, `storyFontSize`, `setStoryFontSize`, `localeCode`), strings from Task 6.
- Produces: new params `onVerseRefClick: (Int, String, Int) -> Unit`, `onProphetNavigate: (Int) -> Unit`. Localized section headers, Facts/Key Events/Lessons/Verse References sections, favorite heart (`Icons.Filled.Favorite` tinted), share via ACTION_SEND, story font-size bottom sheet, prev/next buttons (no wrap).

- [ ] **Step 1: Update signature and loading/not-found logic**

```kotlin
fun ProphetDetailScreen(
    prophetId: Int,
    onBack: () -> Unit,
    onVerseRefClick: (surah: Int, surahName: String, ayaStart: Int) -> Unit,
    onProphetNavigate: (Int) -> Unit,
    viewModel: ProphetsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val prophet = state.prophets.find { it.id == prophetId }
    val localeCode = viewModel.localeCode
    val context = LocalContext.current
    val index = state.prophets.indexOfFirst { it.id == prophetId }
```

- [ ] **Step 2: Update the body** — replace the `if (prophet == null)` spinner block with a not-found fallback:

```kotlin
if (prophet == null) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading" })
        } else {
            Text(stringResource(R.string.prophet_not_found))
        }
    }
} else {
```

- [ ] **Step 3: Replace hardcoded section titles with localized strings** — in the detail content:
- "Summary" → `stringResource(R.string.prophets_summary)`
- "Story" → `stringResource(R.string.prophets_story)`
- "Miracles" → `stringResource(R.string.prophets_miracles)`

- [ ] **Step 4: Add Fact sheet section** after Miracles (only render if any of era/people/lifespan non-blank):

```kotlin
val facts = listOf(
    stringResource(R.string.prophets_era) to (if (localeCode == "id") prophet.eraId else prophet.eraEn),
    stringResource(R.string.prophets_people) to (if (localeCode == "id") prophet.peopleId else prophet.peopleEn),
    stringResource(R.string.prophets_lifespan) to (if (localeCode == "id") prophet.lifespanId else prophet.lifespanEn)
).filter { it.second.isNotBlank() }

if (facts.isNotEmpty()) {
    SectionCard(
        title = stringResource(R.string.prophets_facts),
        content = facts.joinToString("\n") { (label, value) -> "$label: $value" }
    )
    Spacer(Modifier.height(16.dp))
}
```

- [ ] **Step 5: Add Key Events and Lessons** sections:

```kotlin
val events = if (localeCode == "id") prophet.eventsId else prophet.eventsEn
if (events.isNotEmpty()) {
    SectionCard(
        title = stringResource(R.string.prophets_key_events),
        content = events.mapIndexed { i, e -> "${i + 1}. $e" }.joinToString("\n")
    )
    Spacer(Modifier.height(16.dp))
}

val lessons = if (localeCode == "id") prophet.lessonsId else prophet.lessonsEn
if (lessons.isNotEmpty()) {
    SectionCard(
        title = stringResource(R.string.prophets_lessons),
        content = lessons.joinToString("\n") { "• $it" }
    )
    Spacer(Modifier.height(16.dp))
}
```

- [ ] **Step 6: Add Verse References section** — tappable rows navigating to Ayah:

```kotlin
if (prophet.verses.isNotEmpty()) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.prophets_verse_references),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            prophet.verses.forEach { ref ->
                val refName = if (localeCode == "id") ref.surahNameId else ref.surahNameEn
                val label = stringResource(R.string.verse_ref, refName, ref.surah, ref.ayahStart)
                Text(
                    text = label,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVerseRefClick(ref.surah, refName, ref.ayahStart) }
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}
```

- [ ] **Step 7: Add favorite heart + share + font-size actions in the TopAppBar**, and a font-size ModalBottomSheet. TopAppBar title block + actions:

```kotlin
actions = {
    Text(
        text = "Aa",
        modifier = Modifier
            .clickable { showFontSizeSheet = true }
            .padding(horizontal = 8.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
    Icon(
        imageVector = Icons.Filled.Favorite,
        contentDescription = null,
        tint = if (prophetId in state.favoriteIds) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .clickable { viewModel.toggleFavorite(prophetId) }
            .padding(horizontal = 8.dp)
    )
    Text(
        text = stringResource(R.string.prophets_share),
        modifier = Modifier
            .clickable { shareProphetStory(context, prophet) }
            .padding(horizontal = 8.dp),
        fontSize = 14.sp
    )
}
```

Add imports: `androidx.compose.material3.Icon`, `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.filled.Favorite`, `androidx.compose.material3.ModalBottomSheet`, `androidx.compose.material3.rememberModalBottomSheetState`, `androidx.compose.material3.Slider`, `androidx.compose.ui.platform.LocalContext`.

- [ ] **Step 8: Add the font-size bottom sheet** (mirror AyahScreen pattern, single slider bound to `state.storyFontSize`, range 12f..30f):

```kotlin
if (showFontSizeSheet) {
    ModalBottomSheet(
        onDismissRequest = { showFontSizeSheet = false },
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.font_size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(R.string.prophets_story_font_size, state.storyFontSize),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("12", fontSize = 12.sp, fontWeight = FontWeight.Light)
                Slider(
                    value = state.storyFontSize.toFloat(),
                    onValueChange = { viewModel.setStoryFontSize(it.toInt()) },
                    valueRange = 12f..30f,
                    steps = 17,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("30", fontSize = 12.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = { showFontSizeSheet = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

Add `var showFontSizeSheet by remember { mutableStateOf(false) }` near the top of the composable (after `val index = ...`).

- [ ] **Step 9: Add prev/next buttons at the bottom** (no wrap — hidden at ends). Append after the verse-references section:

```kotlin
if (state.prophets.size > 1) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (index > 0) {
            Text(
                text = stringResource(R.string.prophets_previous),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onProphetNavigate(state.prophets[index - 1].id) }
                    .padding(8.dp)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (index in 0 until state.prophets.lastIndex) {
            Text(
                text = stringResource(R.string.prophets_next),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onProphetNavigate(state.prophets[index + 1].id) }
                    .padding(8.dp)
            )
        }
    }
}
```

Add import: `androidx.compose.foundation.layout.Arrangement`.

- [ ] **Step 10: Add the share helper** (mirror `shareAyah` from AyahScreen.kt:808):

```kotlin
private fun shareProphetStory(context: Context, prophet: ProphetStory, localeCode: String) {
    val name = if (localeCode == "id") prophet.nameId else prophet.nameEn
    val story = if (localeCode == "id") prophet.storyId else prophet.storyEn
    val text = buildString {
        appendLine(name)
        appendLine(prophet.nameArabic)
        appendLine()
        append(story)
        appendLine()
        append("— Rafiq App")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Story"))
}
```

Call as `shareProphetStory(context, prophet, localeCode)`. Add imports: `android.content.Context`, `android.content.Intent`. `SectionCard` title param already accepts String — pass localized strings.

- [ ] **Step 11: Apply story font size to story text** — the "Story" section content `Text(text = story, fontSize = 16.sp)` → `fontSize = state.storyFontSize.sp`. (Miracles/other sections keep 16.sp.)

- [ ] **Step 12: Verify compile** — `.\gradlew :app:compileDebugKotlin`.

- [ ] **Step 13: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/prophets/ProphetDetailScreen.kt
git commit -m "feat(prophets): detail facts/events/lessons/verses, favorites, share, font size, prev-next"
```

---

### Task 10: Content — batch author remaining 23 prophets

**Files:**
- Modify: `app/src/main/assets/quran-data/prophets/prophets.json`

**Interfaces:**
- Consumes: same JSON keys as Task 5. All new fields bilingual EN+ID.

- [ ] **Step 1: Author content** for ids 2, 4-25 following the Adam/Nuh template. Each prophet gets: `era_en/id`, `people_en/id`, `lifespan_en/id`, `events_en/id` (3-5 items), `lessons_en/id` (2-4 items), `verses` (1-3 refs with correct `surah_name_en/id`). Source content should be accurate to Islamic tradition.

- [ ] **Step 2: Validate all 25 parse** — `.\gradlew :data:testDebug` plus an emulator run (Task 11) confirms all entries render.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/quran-data/prophets/prophets.json
git commit -m "content(prophets): author remaining 23 prophets' facts, events, lessons, verses"
```

---

### Task 11: Final verification

**Files:** none (verification only).

- [ ] **Step 1: Run all unit tests**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew testDebug
```

Expected: all pass (domain `VerseRefTest`, data `ProphetParserTest`, app `ProphetsViewModelTest`, all existing).

- [ ] **Step 2: Build and install on emulator**

```powershell
.\gradlew assembleDebug
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
```

- [ ] **Step 3: Manual verification checklist** on `Medium_Phone_API_35`:
- [ ] Prophets list shows loading spinner then grid; no `(id)` on cards.
- [ ] Favorites chip filters; empty-favorites message shows; heart toggle on detail persists across screens (DataStore).
- [ ] Search matches English, Indonesian, and Arabic names.
- [ ] Adam & Nuh detail shows Facts, Key Events (numbered), Lessons (bulleted), Verse References.
- [ ] Tapping a verse reference opens the Ayah screen scrolled to that ayah; back returns to prophet detail.
- [ ] Aa bottom sheet changes story font size and persists.
- [ ] Share opens the system share sheet with the story text.
- [ ] Previous/Next buttons navigate between prophets; hidden at first/last.
- [ ] Remaining 23 prophets (if authored in Task 10) render all new sections.

- [ ] **Step 4: Final commit if any straggler files** — `git status` clean review.

## Self-Review Notes

- **Spec coverage:** All spec sections map to tasks — §1 (Task 1), §2 (Tasks 2,3,4), §3 (Task 8), §4 (Task 9), §5 (Tasks 6,7), §6 (Tasks 5,10,11).
- **Type consistency:** `VerseRef(surah, surahNameEn, surahNameId, ayahStart, ayahEnd)` used consistently in Task 1/2/9. `onVerseRefClick: (Int, String, Int) -> Unit` signature consistent in Tasks 7/9. `favoriteProphetIds`/`storyFontSize`/`toggleFavoriteProphet`/`setStoryFontSize` consistent in Tasks 3/4. Default story font size 16 consistent (PreferencesManager default + UiState default).
- **Placeholders:** None; all steps include concrete code.
