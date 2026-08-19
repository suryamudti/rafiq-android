# Quran Global Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the search off the Ayah screen onto the Quran (surah) screen as a global ayah search across all 114 surahs, and remove the within-surah search entirely.

**Architecture:** Add `searchAyahs()` to `QuranRepository` (raw SQLite `LIKE` across the quran DB + the current-language translation DB, merged/deduped by `sura:aya`). Extend `QuranViewModel` with debounced search state. Add a toggled search field + results list to `QuranScreen` (reusing the hadith-search highlight pattern). Tapping a result navigates to `Ayah(suraNumber, suraName, scrollToAya)`. Remove all search code from `AyahViewModel`/`AyahScreen`.

**Tech Stack:** Kotlin 2.0.0, Jetpack Compose (Material3, Navigation3), Hilt (KAPT), raw SQLite via `SQLiteDatabase`, Robolectric + MockK + kotlinx-coroutines-test.

## Global Constraints

- KAPT, not KSP. `correctErrorTypes=true` in `:app`. Kotlin 2.0.0.
- Only `material-icons-core` — `Icons.Filled.Search` is in core (it is already used by `AyahScreen`/`HadithBooksScreen`).
- Data loads on `init` via `viewModelScope.launch(dispatcherProvider.io)`.
- Locale: `if (Locale.getDefault().language == "id" || "in") "id" else "en"` via `currentLocaleCode()`.
- Cross-module smart casts from nullable don't work — use `!!`/`?:`/local `val` (e.g. `state.searchResults` items are `Ayah` with nullable `translation`).
- Room `room-runtime` is `api` in `:data`; `:app` accesses `RoomDatabase` transitively — do not change dependency scopes.
- Build on Windows needs `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` before any `gradlew` command.
- Never add code comments unless the existing pattern uses them.

---

### Task 1: Repository `searchAyahs` (domain + data)

**Files:**
- Modify: `domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/QuranRepository.kt`
- Modify: `data/src/main/kotlin/com/smiledev/rafiq/data/repository/QuranRepositoryImpl.kt`
- Test: `data/src/test/kotlin/com/smiledev/rafiq/data/repository/QuranRepositoryImplTest.kt`

**Interfaces:**
- Consumes: existing `QuranRepository` members `getChapters(localeCode)`, `getAyahsWithTranslation(suraNumber, localeCode)`; existing `getMetadataMap()`; existing `getQuranDatabase()`; existing `getTranslationDatabase(localeCode)`; `AyahData`/`toDomain()` from `data` module.
- Produces: `fun searchAyahs(query: String, localeCode: String = "en", limit: Int = 100): Result<List<Ayah>, AppError>` on `QuranRepository` (implemented in `QuranRepositoryImpl`). Later tasks depend on exactly this signature.

- [ ] **Step 1: Update the repository test file with the fixture DBs and failing search tests**

Replace the contents of `QuranRepositoryImplTest.kt` with the following (keeps the two `getChapters` tests, adds real SQLite fixture DBs and `searchAyahs` cases):

```kotlin
package com.smiledev.rafiq.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DatabaseCopier
import com.smiledev.rafiq.core.Result
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class QuranRepositoryImplTest {

    private val context: Context = mockk(relaxed = true)
    private val assetManager: AssetManager = mockk()
    private val databaseCopier: DatabaseCopier = mockk(relaxed = true)
    private lateinit var repo: QuranRepositoryImpl
    private lateinit var tempRoot: File

    private val metadataJson = """
        {"verses": [
            {"sura":1,"aya":1,"page":1,"juz":1},
            {"sura":1,"aya":2,"page":1,"juz":1},
            {"sura":2,"aya":255,"page":42,"juz":2}
        ]}
    """.trimIndent()

    @Before
    fun setUp() {
        every { context.assets } returns assetManager
        tempRoot = createTempDir()
        every { context.filesDir } returns tempRoot
        every { databaseCopier.copyAndVerifyTranslationDb(any()) } returns true
        every { assetManager.open("quran-data/quran-metadata.json") } returns
            ByteArrayInputStream(metadataJson.toByteArray())
        repo = QuranRepositoryImpl(context, databaseCopier)
        createFixtureDatabases()
    }

    @After
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    private fun createFixtureDatabases() {
        val dbDir = File(tempRoot, "databases").apply { mkdirs() }

        SQLiteDatabase.openOrCreateDatabase(File(dbDir, "quran-uthmani.db"), null).use { db ->
            db.execSQL(
                "CREATE TABLE quran (sura TEXT NOT NULL, aya TEXT NOT NULL, text TEXT NOT NULL, bismillah TEXT)"
            )
            db.execSQL("INSERT INTO quran VALUES ('1','1','بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ','')")
            db.execSQL("INSERT INTO quran VALUES ('1','2','الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ',NULL)")
            db.execSQL("INSERT INTO quran VALUES ('2','255','اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ',NULL)")
        }

        SQLiteDatabase.openOrCreateDatabase(File(dbDir, "translations_id.indonesian.db"), null).use { db ->
            db.execSQL("CREATE TABLE verses (sura TEXT NOT NULL, ayah INTEGER NOT NULL, text TEXT NOT NULL)")
            db.execSQL("INSERT INTO verses VALUES ('1',1,'Dengan nama ٱللَّهِ yang Maha Pengasih')")
            db.execSQL("INSERT INTO verses VALUES ('1',2,'Segala puji bagi Allah, Tuhan semesta alam')")
            db.execSQL("INSERT INTO verses VALUES ('2',255,'Allah, tidak ada tuhan selain Dia')")
        }

        SQLiteDatabase.openOrCreateDatabase(File(dbDir, "translations_en.sahih.db"), null).use { db ->
            db.execSQL("CREATE TABLE verses (sura TEXT NOT NULL, ayah INTEGER NOT NULL, text TEXT NOT NULL)")
            db.execSQL("INSERT INTO verses VALUES ('1',1,'In the name of Allah, the Entirely Merciful')")
            db.execSQL("INSERT INTO verses VALUES ('1',2,'All praise is due to Allah, Lord of the worlds')")
            db.execSQL("INSERT INTO verses VALUES ('2',255,'price 50% off')")
        }
    }

    @Test
    fun `getChapters parses valid JSON`() {
        val json = """
            {
                "chapters": [
                    {
                        "id": 1, "chapter_number": 1, "name_arabic": "الفاتحة",
                        "name_simple": "Al-Fatiha", "translated_name": {"name": "The Opening"},
                        "verses_count": 7, "revelation_place": "makkah"
                    }
                ]
            }
        """.trimIndent()
        every { assetManager.open("quran-data/chapters/chapters.en.json") } returns
            ByteArrayInputStream(json.toByteArray())

        val result = repo.getChapters("en")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val chapters = (result as Result.Success).data
        assertEquals(1, chapters.size)
        assertEquals("Al-Fatiha", chapters[0].nameSimple)
        assertEquals("The Opening", chapters[0].translatedName)
    }

    @Test
    fun `getChapters handles missing asset`() {
        every { assetManager.open("quran-data/chapters/chapters.en.json") } throws
            RuntimeException("File not found")

        val result = repo.getChapters("en")

        assertTrue("Expected Error but got ${result}", result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }

    @Test
    fun `searchAyahs matches Arabic text across surahs`() {
        val result = repo.searchAyahs("الْعَالَمِينَ", "en")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val ayahs = (result as Result.Success).data
        assertEquals(1, ayahs.size)
        assertEquals(1, ayahs[0].sura)
        assertEquals(2, ayahs[0].aya)
    }

    @Test
    fun `searchAyahs includes translation for Arabic matches`() {
        val result = repo.searchAyahs("الْعَالَمِينَ", "en")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val ayahs = (result as Result.Success).data
        assertEquals("All praise is due to Allah, Lord of the worlds", ayahs[0].translation)
    }

    @Test
    fun `searchAyahs matches id translation`() {
        val result = repo.searchAyahs("puji", "id")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val ayahs = (result as Result.Success).data
        assertEquals(listOf(1 to 2), ayahs.map { it.sura to it.aya })
    }

    @Test
    fun `searchAyahs matches en translation`() {
        val result = repo.searchAyahs("Merciful", "en")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val ayahs = (result as Result.Success).data
        assertEquals(listOf(1 to 1), ayahs.map { it.sura to it.aya })
    }

    @Test
    fun `searchAyahs dedupes when term matches Arabic and translation of same ayah`() {
        val result = repo.searchAyahs("ٱللَّهِ", "id")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val ayahs = (result as Result.Success).data
        assertEquals(listOf(1 to 1), ayahs.map { it.sura to it.aya })
        assertEquals("Dengan nama ٱللَّهِ yang Maha Pengasih", ayahs[0].translation)
    }

    @Test
    fun `searchAyahs escapes percent`() {
        val result = repo.searchAyahs("50%", "en")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val ayahs = (result as Result.Success).data
        assertEquals(listOf(2 to 255), ayahs.map { it.sura to it.aya })
    }

    @Test
    fun `searchAyahs blank query returns empty without error`() {
        val result = repo.searchAyahs("   ")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `searchAyahs returns empty for no match`() {
        val result = repo.searchAyahs("zzz-not-there", "en")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `searchAyahs applies limit`() {
        val result = repo.searchAyahs("Allah", "en", limit = 1)

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        assertEquals(1, (result as Result.Success).data.size)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew :data:testDebugUnitTest --tests "com.smiledev.rafiq.data.repository.QuranRepositoryImplTest"
```

Expected: FAIL — compile error `Cannot resolve reference 'searchAyahs'` (method not defined yet).

- [ ] **Step 3: Add `searchAyahs` to the domain interface**

Replace the entire contents of `domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/QuranRepository.kt`:

```kotlin
package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah

interface QuranRepository {
    fun getChapters(localeCode: String = "en"): Result<List<Surah>, AppError>
    fun getAyahsWithTranslation(suraNumber: Int, localeCode: String = "en"): Result<List<Ayah>, AppError>
    fun searchAyahs(query: String, localeCode: String = "en", limit: Int = 100): Result<List<Ayah>, AppError>
}
```

- [ ] **Step 4: Implement `searchAyahs` in `QuranRepositoryImpl`**

Add the public method and the private helpers to `data/src/main/kotlin/com/smiledev/rafiq/data/repository/QuranRepositoryImpl.kt`. Insert the public method right after `getAyahsWithTranslation` (after line 117), and add the helpers at the end of the class before the closing brace (after `readAssetJson`). Note: `asSuccess`, `AyahData`, and `toDomain` are already imported.

```kotlin
    override fun searchAyahs(query: String, localeCode: String, limit: Int): Result<List<Ayah>, AppError> {
        return try {
            val term = query.trim()
            if (term.isEmpty()) return emptyList<Ayah>().asSuccess()
            val pattern = "%${escapeLike(term)}%"

            val arabicMatches = searchArabic(pattern, limit)
            val translationMatches = searchTranslation(pattern, localeCode, limit)

            val keys = (arabicMatches.keys + translationMatches.keys)
                .sortedWith(compareBy({ it.first }, { it.second }))
                .take(limit)
            val metadata = getMetadataMap()

            val results = keys.map { (sura, aya) ->
                val arabicData = arabicMatches[sura to aya] ?: fetchArabic(sura, aya)
                val meta = metadata["$sura:$aya"]
                AyahData(
                    sura = sura,
                    aya = aya,
                    text = arabicData?.text ?: "",
                    bismillah = arabicData?.bismillah,
                    translation = translationMatches[sura to aya]
                        ?: getTranslationForAya(sura, aya, localeCode),
                    page = meta?.page ?: 0,
                    juz = meta?.juz ?: 0,
                    sajda = meta?.sajda ?: false,
                    sajdaType = meta?.sajdaType
                ).toDomain()
            }
            results.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to search ayahs", e))
        }
    }
```

```kotlin
    private fun searchArabic(pattern: String, limit: Int): Map<Pair<Int, Int>, AyahData> {
        val db = getQuranDatabase()
        val result = mutableMapOf<Pair<Int, Int>, AyahData>()
        db.rawQuery(
            "SELECT sura, aya, text, bismillah FROM quran WHERE text LIKE ? ESCAPE '\\' " +
                "ORDER BY CAST(sura AS INTEGER), CAST(aya AS INTEGER) LIMIT ?",
            arrayOf(pattern, limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val sura = c.getString(0).toIntOrNull() ?: 0
                val aya = c.getString(1).toIntOrNull() ?: 0
                val bismillahStr = if (c.isNull(3)) null else c.getString(3)
                result[sura to aya] = AyahData(
                    sura = sura,
                    aya = aya,
                    text = c.getString(2),
                    bismillah = if (bismillahStr.isNullOrEmpty()) null else bismillahStr
                )
            }
        }
        return result
    }

    private fun searchTranslation(pattern: String, localeCode: String, limit: Int): Map<Pair<Int, Int>, String> {
        val db = getTranslationDatabase(localeCode) ?: return emptyMap()
        val result = mutableMapOf<Pair<Int, Int>, String>()
        db.rawQuery(
            "SELECT sura, ayah, text FROM verses WHERE text LIKE ? ESCAPE '\\' " +
                "ORDER BY CAST(sura AS INTEGER), ayah LIMIT ?",
            arrayOf(pattern, limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val sura = c.getString(0).toIntOrNull() ?: 0
                val aya = c.getInt(1)
                result[sura to aya] = c.getString(2)
            }
        }
        return result
    }

    private fun fetchArabic(sura: Int, aya: Int): AyahData? {
        val db = getQuranDatabase()
        db.rawQuery(
            "SELECT text, bismillah FROM quran WHERE sura = ? AND aya = ?",
            arrayOf(sura.toString(), aya.toString())
        ).use { c ->
            if (!c.moveToFirst()) return null
            val bismillahStr = if (c.isNull(1)) null else c.getString(1)
            return AyahData(
                sura = sura,
                aya = aya,
                text = c.getString(0),
                bismillah = if (bismillahStr.isNullOrEmpty()) null else bismillahStr
            )
        }
    }

    private fun getTranslationForAya(sura: Int, aya: Int, localeCode: String): String? {
        val db = getTranslationDatabase(localeCode) ?: return null
        db.rawQuery(
            "SELECT text FROM verses WHERE CAST(sura AS INTEGER) = ? AND ayah = ?",
            arrayOf(sura.toString(), aya.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }

    private fun escapeLike(input: String): String =
        input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
```

- [ ] **Step 5: Run the tests to verify they pass**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew :data:testDebugUnitTest --tests "com.smiledev.rafiq.data.repository.QuranRepositoryImplTest"
```

Expected: PASS — all `searchAyahs` and `getChapters` tests green.

- [ ] **Step 6: Commit**

```bash
git add domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/QuranRepository.kt
git add data/src/main/kotlin/com/smiledev/rafiq/data/repository/QuranRepositoryImpl.kt
git add data/src/test/kotlin/com/smiledev/rafiq/data/repository/QuranRepositoryImplTest.kt
git commit -m "feat(data): global ayah search in Quran repository"
```

---

### Task 2: Extend `QuranViewModel` with debounced global search

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/quran/QuranViewModel.kt`
- Test: `app/src/test/java/com/smiledev/rafiq/ui/quran/QuranViewModelTest.kt`

**Interfaces:**
- Consumes: `QuranRepository.searchAyahs(query: String, localeCode: String = "en", limit: Int = 100): Result<List<Ayah>, AppError>` (Task 1); `PreferencesManager.translationLanguage: Flow<String>`; `currentLocaleCode()` from `:core`.
- Produces: `QuranUiState` gains `searchQuery: String`, `searchResults: List<Ayah>`, `searchLoading: Boolean`, `searchError: AppError?`, `translationLanguage: String`. New methods `search(query: String)` and `resolvedLanguage(): String`. Constructor signature becomes `QuranViewModel(quranRepository, preferencesManager, dispatcherProvider = DefaultDispatcherProvider)`. Later tasks rely on these exact names.

- [ ] **Step 1: Update the failing ViewModel test**

Replace the entire contents of `app/src/test/java/com/smiledev/rafiq/ui/quran/QuranViewModelTest.kt`:

```kotlin
package com.smiledev.rafiq.ui.quran

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.QuranRepository
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
class QuranViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val quranRepository: QuranRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private val surah = Surah(1, 1, "الفاتحة", "Al-Fatiha", "Al-Fatiha", 7, "meccan")
    private val ayah = Ayah(sura = 1, aya = 1, text = "بِسْمِ ٱللَّهِ", bismillah = null,
        translation = "In the name of Allah")

    private fun createVm(): QuranViewModel {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        return QuranViewModel(quranRepository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `load surahs success`() = runTest(testDispatcher) {
        every { quranRepository.getChapters(any()) } returns Result.Success(listOf(surah))

        val vm = createVm()
        vm.loadSurahs()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.surahs.size)
        assertEquals("Al-Fatiha", vm.uiState.value.surahs[0].translatedName)
    }

    @Test
    fun `load surahs error sets error state`() = runTest(testDispatcher) {
        every { quranRepository.getChapters(any()) } returns Result.Error(AppError.Database("Failed", null))

        val vm = createVm()
        vm.loadSurahs()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.surahs.size)
    }

    @Test
    fun `search populates results after debounce`() = runTest(testDispatcher) {
        every { quranRepository.searchAyahs("In the name", "en", 100) } returns Result.Success(listOf(ayah))

        val vm = createVm()
        vm.search("In the name")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals(listOf(ayah), vm.uiState.value.searchResults)
        assertEquals(false, vm.uiState.value.searchLoading)
    }

    @Test
    fun `debounce cancels the earlier keystroke`() = runTest(testDispatcher) {
        every { quranRepository.searchAyahs("In", "en", 100) } returns Result.Success(listOf(ayah))
        every { quranRepository.searchAyahs("In the name", "en", 100) } returns Result.Success(listOf(ayah))

        val vm = createVm()
        vm.search("In")
        advanceTimeBy(100)
        vm.search("In the name")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals("In the name", vm.uiState.value.searchQuery)
        assertEquals(listOf(ayah), vm.uiState.value.searchResults)
    }

    @Test
    fun `blank query clears results without hitting repo`() = runTest(testDispatcher) {
        every { quranRepository.searchAyahs("In the name", "en", 100) } returns Result.Success(listOf(ayah))

        val vm = createVm()
        vm.search("In the name")
        advanceTimeBy(250)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.searchResults.size)

        vm.search("   ")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun `search error surfaces in state`() = runTest(testDispatcher) {
        every { quranRepository.searchAyahs("boom", "en", 100) } returns Result.Error(AppError.Database("fail", null))

        val vm = createVm()
        vm.search("boom")
        advanceTimeBy(250)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.searchError != null)
        assertEquals(false, vm.uiState.value.searchLoading)
    }

    @Test
    fun `resolvedLanguage maps system to locale code`() = runTest(testDispatcher) {
        val vm = createVm()
        assertEquals("en", vm.resolvedLanguage())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq.ui.quran.QuranViewModelTest"
```

Expected: FAIL — compile error (constructor mismatch: `QuranViewModel` currently takes 2 args; `search`, `resolvedLanguage`, and the new state fields don't exist).

- [ ] **Step 3: Implement the ViewModel changes**

Replace the entire contents of `app/src/main/java/com/smiledev/rafiq/ui/quran/QuranViewModel.kt`:

```kotlin
package com.smiledev.rafiq.ui.quran

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.QuranRepository
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
data class QuranUiState(
    val surahs: List<Surah> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val searchQuery: String = "",
    val searchResults: List<Ayah> = emptyList(),
    val searchLoading: Boolean = false,
    val searchError: AppError? = null,
    val translationLanguage: String = "system"
)

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState

    private val localeCode = currentLocaleCode()
    private var searchJob: Job? = null

    init {
        loadSurahs()
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.value = _uiState.value.copy(translationLanguage = lang)
            }
        }
    }

    fun loadSurahs() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = quranRepository.getChapters(localeCode)
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(surahs = result.data, isLoading = false)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    fun refresh() { loadSurahs() }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatcherProvider.io) {
            delay(SEARCH_DEBOUNCE_MS)
            val term = _uiState.value.searchQuery.trim()
            if (term.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    searchResults = emptyList(), searchLoading = false, searchError = null
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(searchLoading = true, searchError = null)
            when (val result = quranRepository.searchAyahs(term, resolvedLanguage(), SEARCH_LIMIT)) {
                is Result.Success -> {
                    if (_uiState.value.searchQuery.trim() == term) {
                        _uiState.value = _uiState.value.copy(searchResults = result.data, searchLoading = false)
                    }
                }
                is Result.Error -> {
                    if (_uiState.value.searchQuery.trim() == term) {
                        _uiState.value = _uiState.value.copy(searchLoading = false, searchError = result.error)
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

- [ ] **Step 4: Run the test to verify it passes**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq.ui.quran.QuranViewModelTest"
```

Expected: PASS — all 8 tests green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/quran/QuranViewModel.kt
git add app/src/test/java/com/smiledev/rafiq/ui/quran/QuranViewModelTest.kt
git commit -m "feat(quran): debounced global ayah search in Quran view model"
```

---

### Task 3: Add search UI to `QuranScreen` + strings + navigation

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/quran/QuranScreen.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq/Navigation.kt` (Quran entry block)
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-id/strings.xml`

**Interfaces:**
- Consumes: `QuranUiState` fields from Task 2 (`searchQuery`, `searchResults`, `searchLoading`, `searchError`, `surahs`); `QuranViewModel.search(query)`. `onSearchResultClick: (Int, String, Int) -> Unit` is a new composable param.
- Produces: `QuranScreen(initialTab, onSurahClick, onBookmarkClick, onSearchResultClick, onBack, viewModel, modifier)` with the new callback. `Navigation.kt` passes it. `quran` entry's `QuranScreen(...)` call now includes `onSearchResultClick = { sura, name, aya -> backStack.add(Ayah(suraNumber = sura, suraName = name, scrollToAya = aya)) }`.

- [ ] **Step 1: Add the string resources**

In `app/src/main/res/values/strings.xml`, after the existing `search_hadiths` block (near line 177), add:

```xml
    <string name="search_quran_hint">Search the whole Quran…</string>
    <string name="no_ayahs_match">No results found</string>
```

In `app/src/main/res/values-id/strings.xml`, after the existing `search_hadiths` block (near line 177), add:

```xml
    <string name="search_quran_hint">Cari di seluruh Al-Quran…</string>
    <string name="no_ayahs_match">Tidak ada hasil</string>
```

- [ ] **Step 2: Write the failing instrumented test**

Create `app/src/androidTest/java/com/smiledev/rafiq/ui/quran/QuranScreenSearchTest.kt`:

```kotlin
package com.smiledev.rafiq.ui.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.domain.model.Surah
import com.smiledev.rafiq.domain.repository.QuranRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class QuranScreenSearchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun searchIconTogglesFieldAndRendersResults() {
        val repo = mockk<QuranRepository>(relaxed = true)
        val prefs = mockk<PreferencesManager>(relaxed = true)

        every { repo.getChapters("en") } returns Result.Success(
            listOf(Surah(1, 1, "الفاتحة", "Al-Fatiha", "Al-Fatiha", 7, "Mecca"))
        )
        every { repo.searchAyahs(any(), any(), any()) } returns Result.Success(
            listOf(Ayah(1, 1, "بِسْمِ ٱللَّهِ", null, translation = "In the name of Allah"))
        )
        every { prefs.translationLanguage } returns MutableStateFlow("en")

        composeTestRule.setContent {
            QuranScreen(
                initialTab = 0,
                onSurahClick = { _, _ -> },
                onBookmarkClick = { _, _, _ -> },
                onSearchResultClick = { _, _, _ -> },
                onBack = {},
                viewModel = QuranViewModel(repo, prefs, DefaultDispatcherProvider)
            )
        }

        composeTestRule.onNodeWithContentDescription("Search").performClick()
        composeTestRule.onNodeWithContentDescription("Search field").assertIsDisplayed()
    }
}
```

Note: this instrumented test requires an emulator and is run with `connectedDebugAndroidTest`; it is a compile-check + smoke test. The `"Search field"` contentDescription must be added in Step 3 (see below) or the runtime assertion fails.

- [ ] **Step 3: Replace `QuranScreen.kt` with the search-enabled version**

Replace the entire contents of `app/src/main/java/com/smiledev/rafiq/ui/quran/QuranScreen.kt`:

```kotlin
package com.smiledev.rafiq.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.displayMessage
import com.smiledev.rafiq.domain.model.Ayah
import com.smiledev.rafiq.ui.bookmarks.BookmarkListTabContent

private val arabicFont = FontFamily(Font(R.font.me_quran))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    initialTab: Int = 0,
    onSurahClick: (Int, String) -> Unit,
    onBookmarkClick: (Int, String, Int) -> Unit,
    onSearchResultClick: (Int, String, Int) -> Unit,
    onBack: () -> Unit,
    viewModel: QuranViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val tabs = listOf("Surahs", stringResource(R.string.bookmarks))
    var selectedTabIndex by remember(initialTab) { mutableStateOf(initialTab) }
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.quran)) },
                    navigationIcon = {
                        Text(stringResource(R.string.back), modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                    },
                    actions = {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                if (showSearch) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.search(it) },
                        placeholder = { Text(stringResource(R.string.search_quran_hint)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { contentDescription = "Search field" },
                        singleLine = true
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val searching = showSearch && state.searchQuery.isNotBlank()
            if (!searching) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when {
                    searching -> SearchResultsContent(
                        state = state,
                        query = state.searchQuery,
                        onResultClick = onSearchResultClick,
                        modifier = Modifier.fillMaxSize()
                    )
                    selectedTabIndex == 0 -> {
                        var isRefreshing by remember { mutableStateOf(false) }
                        LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { isRefreshing = true; viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when {
                                state.isLoading && !isRefreshing -> {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).semantics { contentDescription = "Loading" })
                                }
                                state.error != null -> {
                                    Text(
                                        text = state.error?.displayMessage ?: "",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = modifier.fillMaxSize()
                                    ) {
                                        itemsIndexed(state.surahs) { index, surah ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                                    .clickable { onSurahClick(surah.chapterNumber, surah.nameSimple) },
                                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${surah.chapterNumber}.",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(end = 12.dp)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = surah.nameSimple,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = surah.translatedName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = surah.nameArabic,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            textAlign = TextAlign.End
                                                        )
                                                        Text(
                                                            text = "${surah.versesCount} verses",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    else -> {
                        BookmarkListTabContent(
                            onBookmarkClick = onBookmarkClick,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    state: QuranUiState,
    query: String,
    onResultClick: (Int, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.searchError != null -> {
            Text(
                text = state.searchError?.displayMessage ?: "",
                modifier = modifier.fillMaxSize().padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
        state.searchLoading && state.searchResults.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading" })
            }
        }
        state.searchResults.isEmpty() -> {
            Text(
                text = stringResource(R.string.no_ayahs_match),
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            LazyColumn(modifier = modifier) {
                items(state.searchResults, key = { "${it.sura}:${it.aya}" }) { ayah ->
                    val surahName = state.surahs.find { it.chapterNumber == ayah.sura }?.nameSimple
                        ?: "Surah ${ayah.sura}"
                    QuranSearchResultCard(
                        ayah = ayah,
                        surahName = surahName,
                        query = query.trim(),
                        onClick = { onResultClick(ayah.sura, surahName, ayah.aya) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuranSearchResultCard(
    ayah: Ayah,
    surahName: String,
    query: String,
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
                text = "$surahName · ${ayah.sura}:${ayah.aya}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = highlightMatches(ayah.text, query),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = arabicFont,
                    fontSize = 18.sp,
                    textDirection = TextDirection.Rtl
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            val translation = ayah.translation
            if (!translation.isNullOrBlank()) {
                Text(
                    text = highlightMatches(translation, query),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
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

- [ ] **Step 4: Wire navigation**

In `app/src/main/java/com/smiledev/rafiq/Navigation.kt`, replace the `entry<Quran>` block (currently lines 48-58) with:

```kotlin
        entry<Quran> { key ->
          QuranScreen(
            initialTab = key.initialTab,
            onSurahClick = { num, name -> backStack.add(Ayah(num, name)) },
            onBookmarkClick = { sura, name, aya ->
              backStack.add(Ayah(suraNumber = sura, suraName = name, scrollToAya = aya))
            },
            onSearchResultClick = { sura, name, aya ->
              backStack.add(Ayah(suraNumber = sura, suraName = name, scrollToAya = aya))
            },
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
```

- [ ] **Step 5: Compile-check the app**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew :app:compileDebugKotlin
.\gradlew :app:compileDebugAndroidTestKotlin
```

Expected: BUILD SUCCESSFUL (both). No unit tests for this UI-only task; the instrumented test compiles and is verified manually on the emulator.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/quran/QuranScreen.kt
git add app/src/main/java/com/smiledev/rafiq/Navigation.kt
git add app/src/main/res/values/strings.xml
git add app/src/main/res/values-id/strings.xml
git add app/src/androidTest/java/com/smiledev/rafiq/ui/quran/QuranScreenSearchTest.kt
git commit -m "feat(quran): global ayah search UI on the Quran screen"
```

---

### Task 4: Remove the within-surah search from the Ayah screen

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/quran/AyahViewModel.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/quran/AyahScreen.kt`
- Test: `app/src/androidTest/java/com/smiledev/rafiq/ui/quran/AyahScreenTest.kt` (existing — verify still green)

**Interfaces:**
- Consumes: nothing new. Removes `searchQuery` from `AyahUiState`, `setSearchQuery()`, and `getFilteredAyahs()` from `AyahViewModel`; removes `showSearch` + search UI from `AyahScreen`.
- Produces: `AyahScreen` renders `state.ayahs` directly (no filter). Later/other code (Navigation.kt) is unaffected because `AyahViewModel` construction is unchanged.

- [ ] **Step 1: Remove search state/methods from `AyahViewModel`**

In `app/src/main/java/com/smiledev/rafiq/ui/quran/AyahViewModel.kt`:

1. In `AyahUiState`, delete the line `val searchQuery: String = "",` (currently line 41).
2. Delete the two methods (currently lines 138-150):

```kotlin
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredAyahs(): List<Ayah> {
        val q = _uiState.value.searchQuery.trim().lowercase()
        if (q.isEmpty()) return _uiState.value.ayahs
        return _uiState.value.ayahs.filter { ayah ->
            ayah.text.lowercase().contains(q) ||
            (ayah.translationId?.lowercase()?.contains(q) == true) ||
            (ayah.translationEn?.lowercase()?.contains(q) == true)
        }
    }
```

- [ ] **Step 2: Remove search UI from `AyahScreen`**

In `app/src/main/java/com/smiledev/rafiq/ui/quran/AyahScreen.kt`:

1. Delete the import `import androidx.compose.material.icons.filled.Search` (currently line 35).
2. Delete the state line `var showSearch by remember { mutableStateOf(false) }` (currently line 101).
3. In the `TopAppBar` actions, delete the search `IconButton` (currently lines 323-325):

```kotlin
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
```

4. Delete the `if (showSearch) { OutlinedTextField(...) }` block (currently lines 364-372) — the whole
   `if (showSearch) {` … `}` inside the `topBar` `Column`.

5. Remove the filter wrapper and render `state.ayahs` directly. Make three precise replacements:

   **Replacement 5a** — drop the `displayAyahs`/`if-else` wrapper opening. Replace exactly:

   ```kotlin
                   else -> {
                       val displayAyahs = viewModel.getFilteredAyahs()
                       if (displayAyahs.isEmpty() && state.searchQuery.isNotBlank()) {
                           Text(
                               text = "No results found",
                               modifier = Modifier.fillMaxWidth().padding(32.dp),
                               textAlign = TextAlign.Center,
                               color = MaterialTheme.colorScheme.onSurfaceVariant
                           )
                       } else {
                           LazyColumn(
   ```

   with:

   ```kotlin
                   else -> {
                       LazyColumn(
   ```

   **Replacement 5b** — rename the list iterator. Replace exactly:

   ```kotlin
                               itemsIndexed(displayAyahs) { index, ayah ->
   ```

   with:

   ```kotlin
                               itemsIndexed(state.ayahs) { index, ayah ->
   ```

   **Replacement 5c** — remove the now-orphaned closing brace of the removed `else` wrapper. The
   `LazyColumn`'s closing brace is currently immediately followed by an extra `}` (the wrapper's
   close). Replace exactly:

   ```kotlin
                               )
                           }
                       }
                   }
               }
           }
       }
   ```

   with:

   ```kotlin
                               )
                           }
                   }
               }
           }
       }
   ```

   (i.e. delete the `}` that sits directly between `LazyColumn`'s close and the `else ->` branch
   close — indented one level less than `LazyColumn`'s close.) Verify the file compiles in Step 3;
   if the diff looks off, the block should end: `}` closes `itemsIndexed`, `}` closes `LazyColumn`,
   `}` closes `else ->`, `}` closes `when`, `}` closes the `Box`.

- [ ] **Step 3: Compile-check the app and run the unit suite**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew :app:compileDebugKotlin
.\gradlew :app:compileDebugAndroidTestKotlin
.\gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL. `AyahScreenTest` (androidTest) is unchanged and remains green — it asserts the TopAppBar title and translation text, which still exist.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/quran/AyahViewModel.kt
git add app/src/main/java/com/smiledev/rafiq/ui/quran/AyahScreen.kt
git commit -m "refactor(quran): remove within-surah search from the ayah screen"
```

---

## Final Verification

After all tasks are committed, run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew :data:testDebugUnitTest
.\gradlew :app:testDebugUnitTest
.\gradlew :app:compileDebugKotlin
.\gradlew :app:compileDebugAndroidTestKotlin
```

Then on an emulator (`Medium_Phone_API_35`), install and manually verify:

```powershell
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
```

Manual checks:
1. Quran screen → Search icon toggles the field.
2. Typing "mercy" (EN) filters the whole Quran; results show surah name, `sura:aya`, Arabic + translation with the term highlighted.
3. Tapping a result opens the Ayah screen scrolled to that ayah.
4. Blank query shows the normal tabs again.
5. Ayah screen no longer has a Search icon.
