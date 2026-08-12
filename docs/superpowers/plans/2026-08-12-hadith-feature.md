# Hadith App Feature Implementation Plan (Plan A)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Sahih Bukhari + Sahih Muslim hadith browser to Rafiq, showing Arabic matn plus English/Bahasa Indonesia translations, with language driven by the existing Quran translation setting.

**Architecture:** Read-only asset SQLite (`hadith.db`) copied via `DatabaseCopier` and queried with `rawQuery` (the `QuranRepositoryImpl` translation-DB pattern). Three screens (Books → Hadith list → Detail) following the Prophets list/detail pattern, wired through Navigation3 keys. No Room, no ViewModel scan: data repos are synchronous `Result`-returning methods, and detail reuses the list ViewModel to avoid double fetches.

**Tech Stack:** Kotlin 2.0.0, Compose (Material3), Hilt (KAPT, not KSP), Navigation3 (`@Serializable` NavKeys), DataStore (`PreferencesManager`), Robolectric + mockk for JVM tests, Compose UI tests for instrumented tests.

**Note on plan split:** This is Plan A (app feature, seeded with a small real DB). Plan B (`docs/superpowers/plans/2026-08-12-hadith-content-pipeline.md`) is the Python content pipeline that produces the full ~12k-hadith corpus; Plan A is fully testable before Plan B lands.

## Global Constraints

- Hilt uses KAPT (`plugins.kapt`), **not** KSP. Never replace it.
- Kotlin 2.0.0; Room 2.8.4. Do not upgrade/downgrade dependencies.
- Material Icons: `material-icons-core` only. Never add `material-icons-extended`. Available: `DateRange`, `Face`, `Favorite`, `List` (AutoMirrored), `LocationOn`, `Notifications`, `Person`, `Place`, `PlayArrow`, `Refresh`, `ShoppingCart`, `Star`. Dashboard cards use vector drawables (`R.drawable.*`), not icons.
- Arabic font loads via `FontFamily(Font(R.font.me_quran))` — never `fontResource()`.
- All user prefs via `PreferencesManager` (DataStore), never SharedPreferences.
- `AppError.displayMessage` is the single error-message source (`core/.../AppError.kt:10`).
- Cross-module smart casts from nullable don't work — use `!!` (if guarded) or `?:` / local `val` (see `AyahScreen.kt:663`).
- Do NOT create new Room entities/DAOs for this feature; content is a read-only asset DB.
- Do NOT create use cases (YAGNI) — the prophets/asmaul-husna VMs use repos directly.
- Do NOT touch `SettingsScreen` language picker — the Quran `translation_language` pref is reused as-is.
- DB asset name contains `/` → `DatabaseCopier` flattens to `_` (e.g. `hadiths/hadith.db` → `hadiths_hadith.db`).

## Current State Analysis

- Worktree `.worktree/worktrees_hadith` on branch `feat/hadith-translation`, off `main@8c49d49`, spec committed (`docs/superpowers/specs/2026-08-12-hadith-translation-design.md`).
- Bundled-content pattern: `ProphetRepositoryImpl` (JSON, synchronous `Result`), `QuranRepositoryImpl` (asset SQLite via `DatabaseCopier` + `rawQuery`).
- Translation flow: `PreferencesManager.translationLanguage` (`"system"|"id"|"en"|"both"`), resolved at render time exactly like `AyahViewModel.getTranslationText` (`AyahViewModel.kt:297-305`): `if (lang == "system") currentLocaleCode() else lang`.
- `DatabaseCopier.copyAndVerifyTranslationDb` hard-codes a `verses`-table check (`DatabaseCopier.kt:56`); hadith DB has `books`/`hadiths` tables → use `copyDatabaseIfNeeded` + repo-local `sqlite_master` sanity check instead.
- Nav: keys in `NavigationKeys.kt` (16 routes); entries in `Navigation.kt` via `entry<Key> { key -> ... }`; feature entries exist for Dashboard (`DashboardScreen.kt:59-67`) and Settings "More Features" (`SettingsScreen.kt:119-122`).
- Assets live at `app/src/main/assets/quran-data/` (e.g. `prophets.json`, `translations/en.sahih.db`, `quran-uthmani.db`).

## Desired End State

- User opens **Hadiths** from the Dashboard (or Settings → More Features), sees Sahih Bukhari and Sahih Muslim books, taps a book, sees its hadith list, taps a hadith, sees Arabic text + EN/ID translation per the `translation_language` setting (`id`/`en`/`both`, `system` → device locale).
- Data layer reads `hadith.db` read-only; missing/corrupt DB surfaces `AppError.Database` (rendered via `displayMessage`).
- All new codeunit-tested (JVM) with the repo/VM tested to the same standard as `QuranRepositoryImplTest` / `ProphetsViewModelTest`; screens have Compose UI tests like `SettingsScreenTest`.

## What We're NOT Doing

- No search across hadith.
- No favorites/bookmark integration, share/copy, or font-size controls.
- No per-hadith translation setting (reuses Quran setting).
- No extra collections (Tirmidhi/Abu Dawud/etc.).
- No Room entities, no DAOs, no `hadith` FTS.
- No full corpus build in this plan — that is Plan B. Plan A uses a committed seed DB.
- No new `:core`/`:data` infrastructure beyond what the repo already provides.
- No changes to `DatabaseCopier` internals (repo does its own sanity check).

---

## Implementation Approach

Mirror existing patterns file-for-file:

| Layer | Mirrors |
|---|---|
| `domain/model/HadithBook.kt`, `Hadith.kt` | `domain/model/ProphetStory.kt` |
| `domain/repository/HadithRepository.kt` | `domain/repository/ProphetRepository.kt` |
| `data/repository/HadithRepositoryImpl.kt` | `QuranRepositoryImpl.kt` (SQLite open) + `ProphetRepositoryImpl.kt` (sync `Result`) |
| `app` VMs | `ProphetsViewModel` shape + `AyahViewModel` pref-combination |
| Screens | `ProphetsScreen` / `ProphetDetailScreen` + `AyahScreen` "both" branch |
| Nav | `NavigationKeys.kt` + `Navigation.kt` entries |
| Tests | `ProphetsViewModelTest`, `QuranRepositoryImplTest`, `SettingsScreenTest` |

**Task order rationale:** pure declarations first (Task 1), then the source-of-truth DB + generator (Task 2), then the data layer up through DI (Tasks 3–5), then VMs (6–7), screens (8–10), navigation wiring (11). Each task compiles and passes its own tests before the next starts; Task 11 is the only one with no new unit test because wiring is verified by build + the already-tested screens.

## Phases

### Phase 1 — Domain declarations

---

### Task 1: Domain models + repository interface

**Files:**
- Create: `domain/src/main/kotlin/com/smiledev/rafiq/domain/model/HadithBook.kt`
- Create: `domain/src/main/kotlin/com/smiledev/rafiq/domain/model/Hadith.kt`
- Create: `domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/HadithRepository.kt`

**Interfaces:**
- Consumes: `com.smiledev.rafiq.core.Result`, `com.smiledev.rafiq.core.AppError` (from `:core`).
- Produces (exact signatures later tasks rely on):
  - `data class HadithBook(val id: String, val collection: String, val number: Int, val nameAr: String, val nameEn: String, val nameId: String)`
  - `data class Hadith(val id: Int, val bookId: String, val inBookNumber: Int, val narratorAr: String?, val narratorEn: String?, val textAr: String, val textEn: String, val textId: String)`
  - `interface HadithRepository { fun getBooks(): Result<List<HadithBook>, AppError>; fun getHadithsByBook(bookId: String): Result<List<Hadith>, AppError> }`

- [ ] **Step 1: Write `HadithBook.kt`**

```kotlin
package com.smiledev.rafiq.domain.model

data class HadithBook(
    val id: String,
    val collection: String,
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val nameId: String
)
```

- [ ] **Step 2: Write `Hadith.kt`**

```kotlin
package com.smiledev.rafiq.domain.model

data class Hadith(
    val id: Int,
    val bookId: String,
    val inBookNumber: Int,
    val narratorAr: String?,
    val narratorEn: String?,
    val textAr: String,
    val textEn: String,
    val textId: String
)
```

- [ ] **Step 3: Write `HadithRepository.kt`**

```kotlin
package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook

interface HadithRepository {
    fun getBooks(): Result<List<HadithBook>, AppError>
    fun getHadithsByBook(bookId: String): Result<List<Hadith>, AppError>
}
```

- [ ] **Step 4: Verify compile**

Run: `.\gradlew :domain:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/com/smiledev/rafiq/domain/model/HadithBook.kt domain/src/main/kotlin/com/smiledev/rafiq/domain/model/Hadith.kt domain/src/main/kotlin/com/smiledev/rafiq/domain/repository/HadithRepository.kt
git commit -m "feat(domain): hadith book/hadith models + repository interface"
```

---

### Task 2: Seed hadith.db generator + committed seed DB

Builds the schema-defining seed generator and commits the resulting `hadith.db` so the app is end-to-end testable. This script is deliberately small; Plan B extends it.

**Files:**
- Create: `tools/hadith-pipeline/build_seed_db.py`
- Create (output, committed): `app/src/main/assets/quran-data/hadiths/hadith.db`

**Interfaces:**
- Produces: a SQLite DB at `app/src/main/assets/quran-data/hadiths/hadith.db` with exactly:
  - `books(id TEXT PRIMARY KEY, collection TEXT, number INTEGER, name_ar TEXT, name_en TEXT, name_id TEXT)`
  - `hadiths(id INTEGER PRIMARY KEY, book_id TEXT, in_book_number INTEGER, narrator_ar TEXT, narrator_en TEXT, text_ar TEXT, text_en TEXT, text_id TEXT)`
  - `CREATE INDEX idx_hadiths_book ON hadiths(book_id)`
  - ≥ 2 books (`bukhari.1`, `muslim.1`) and ≥ 4 hadith total, each with non-blank `text_ar`, `text_en`, and `text_id`.
- Consumes: hadith source text (see Step 1).

- [ ] **Step 1: Collect verified seed hadith content**

Copy these two **Sahih al-Bukhari, Book 1 (Revelation)** hadith verbatim from https://sunnah.com/bukhari:1 and https://sunnah.com/bukhari:2 (Arabic + English narrator/text). Source the Bahasa Indonesia translation for the same hadith from the dataset referenced in Plan B (`irsyadulibad/hadits-database`, MIT licensed) or from https://carihadis.com. Then copy the first two **Sahih Muslim, Book 1 (Faith)** hadith verbatim from https://sunnah.com/muslim:1 and https://sunnah.com/muslim:2 (Arabic + English), plus the ID translation of each. Keep the exact `text_ar`/`text_en`/`text_id` strings; do not paraphrase.

All four hadith must appear in the final generated DB. Store the strings in the Python file as module-level constants (one per hadith), exactly as in Step 2's placeholder positions.

- [ ] **Step 2: Write `build_seed_db.py`**

```python
"""Generate the seed hadith.db committed to app assets.

Schema is the source of truth for Plan A. Plan B extends this generator with the
full corpus. Run:  python build_seed_db.py
Output: app/src/main/assets/quran-data/hadiths/hadith.db
"""
import os
import sqlite3

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_PATH = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "quran-data", "hadiths", "hadith.db")

BOOKS = [
    {
        "id": "bukhari.1",
        "collection": "bukhari",
        "number": 1,
        "name_ar": "كتاب بدء الوحي",
        "name_en": "Revelation",
        "name_id": "Permulaan Wahyu",
    },
    {
        "id": "muslim.1",
        "collection": "muslim",
        "number": 1,
        "name_ar": "كتاب الإيمان",
        "name_en": "Faith",
        "name_id": "Iman",
    },
]

# The first row is prefilled with the real Sahih al-Bukhari 1 (Book 1, Hadith 1):
# Arabic matn + public-domain Muhsin Khan English translation are authoritative;
# the text_id below MUST be replaced with the full verified Bahasa Indonesia text
# from Step 1's source (MIT dataset / carihadis.com) before committing.
# Append the remaining three rows in the same tuple shape:
#   (book_id, in_book_number, narrator_ar, narrator_en, text_ar, text_en, text_id)
#   bukhari.1/2 <- sunnah.com/bukhari:2 ; muslim.1/1 <- sunnah.com/muslim:1 ; muslim.1/2 <- sunnah.com/muslim:2
HADITH = [
    ("bukhari.1", 1,
     "حدثنا الحميدي عبد الله بن الزبير قال حدثنا سفيان قال حدثنا يحيى بن سعيد الأنصاري",
     "Narrated 'Umar bin Al-Khattab",
     "سمعت رسول الله صلى الله عليه وسلم يقول إنما الأعمال بالنيات وإنما لكل امرئ ما نوى ",
     "I heard Allah's Messenger (ﷺ) saying: \"The reward of deeds depends upon the intentions and every person will get the reward according to what he has intended. So whoever emigrated for worldly benefits or for a woman to marry, his emigration was for what he emigrated for.\"",
     "Aku mendengar Rasulullah ﷺ bersabda: \"Sesungguhnya setiap amalan tergantung pada niatnya, dan setiap orang akan mendapatkan sesuai dengan apa yang ia niatkan...\""),
    # PASTE bukhari.1/2 (sunnah.com/bukhari:2) here,
    # PASTE muslim.1/1  (sunnah.com/muslim:1)  here,
    # PASTE muslim.1/2  (sunnah.com/muslim:2)  here,
]

DB_SCHEMA = """
CREATE TABLE books (
    id TEXT PRIMARY KEY,
    collection TEXT NOT NULL,
    number INTEGER NOT NULL,
    name_ar TEXT NOT NULL,
    name_en TEXT NOT NULL,
    name_id TEXT NOT NULL
);
CREATE TABLE hadiths (
    id INTEGER PRIMARY KEY,
    book_id TEXT NOT NULL,
    in_book_number INTEGER NOT NULL,
    narrator_ar TEXT,
    narrator_en TEXT,
    text_ar TEXT NOT NULL,
    text_en TEXT NOT NULL,
    text_id TEXT NOT NULL
);
CREATE INDEX idx_hadiths_book ON hadiths(book_id);
"""


def main() -> None:
    assert len(HADITH) >= 4, "Seed must contain at least 4 verified hadith"
    for i, h in enumerate(HADITH, start=1):
        assert h[4].strip() and h[5].strip() and h[6].strip(), f"hadith #{i} has blank text"
    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    if os.path.exists(OUT_PATH):
        os.remove(OUT_PATH)
    conn = sqlite3.connect(OUT_PATH)
    try:
        conn.executescript(DB_SCHEMA)
        conn.executemany(
            "INSERT INTO books (id, collection, number, name_ar, name_en, name_id)"
            " VALUES (:id, :collection, :number, :name_ar, :name_en, :name_id)",
            BOOKS,
        )
        conn.executemany(
            "INSERT INTO hadiths (id, book_id, in_book_number, narrator_ar, narrator_en,"
            " text_ar, text_en, text_id)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            [tuple([i + 1] + list(h)) for i, h in enumerate(HADITH)],
        )
        conn.commit()
    finally:
        conn.close()
    print(f"Wrote {OUT_PATH}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 3: Run the generator**

Run: `python tools/hadith-pipeline/build_seed_db.py`
Expected: prints `Wrote ...\app\src\main\assets\quran-data\hadiths\hadith.db`.

- [ ] **Step 4: Verify DB contents**

Run:
```powershell
python -c "import sqlite3;c=sqlite3.connect(r'app\src\main\assets\quran-data\hadiths\hadith.db');print(c.execute('select count(*) from books').fetchone());print(c.execute('select count(*) from hadiths').fetchone());print(c.execute('select book_id,in_book_number from hadiths').fetchall())"
```
Expected: `(2,)`, `(4,)` (or more), and rows all under `bukhari.1` / `muslim.1`.

- [ ] **Step 5: Commit**

```bash
git add tools/hadith-pipeline/build_seed_db.py app/src/main/assets/quran-data/hadiths/hadith.db
git commit -m "feat(data): seed hadith.db generator and committed seed asset"
```

---

### Phase 2 — Data layer + DI

---

### Task 3: HadithRepositoryImpl — DB open + getBooks

**Files:**
- Create: `data/src/main/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImpl.kt`
- Test: `data/src/test/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `HadithRepository` (Task 1), `DatabaseCopier` (`data/.../core/DatabaseCopier.kt:15`), `HadithBook` (Task 1).
- Produces: `class HadithRepositoryImpl @Inject constructor(@ApplicationContext context: Context, databaseCopier: DatabaseCopier) : HadithRepository` with `getBooks()` returning the `books` rows ordered by `collection, number`. Later tasks call `getHadithsByBook(bookId)`.

- [ ] **Step 1: Write the failing test**

`data/src/test/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImplTest.kt`:

```kotlin
package com.smiledev.rafiq.data.repository

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
class HadithRepositoryImplTest {

    private val databaseCopier: DatabaseCopier = mockk(relaxed = true)
    private lateinit var repo: HadithRepositoryImpl
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbDir = File(context.filesDir, "databases").apply { mkdirs() }
        dbFile = File(dbDir, "hadiths_hadith.db")
        dbFile.delete()
        createFixtureDb(dbFile)
        repo = HadithRepositoryImpl(context, databaseCopier)
    }

    @After
    fun tearDown() {
        dbFile.delete()
    }

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
        db.execSQL(
            "INSERT INTO hadiths VALUES (1,'bukhari.1',1,'','','n1','t1','t1id')"
        )
        db.close()
    }

    @Test
    fun `getBooks returns books ordered by collection then number`() {
        val result = repo.getBooks()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val books = (result as Result.Success).data
        assertEquals(2, books.size)
        assertEquals("bukhari.1", books[0].id)   // bukhari sorts before muslim
        assertEquals("bukhari", books[0].collection)
        assertEquals("Revelation", books[0].nameEn)
    }

    @Test
    fun `getBooks returns Error when db file missing`() {
        dbFile.delete()

        val result = repo.getBooks()

        assertTrue("Expected Error but got ${result}", result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }
}
```

**Note:** `databaseCopier` is `mockk(relaxed = true)`, so `copyDatabaseIfNeeded` is a no-op in tests; the fixture DB is placed directly at the flattened path `databases/hadths_hadith.db` (mirrors how the real asset copy lays it down). The second test deletes the file so `openDatabase` throws → `Error`.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :data:testDebugUnitTest --tests "com.smiledev.rafiq.data.repository.HadithRepositoryImplTest"`
Expected: FAIL — `HadithRepositoryImplTest` does not exist / `HadithRepositoryImpl` not defined.

- [ ] **Step 3: Write minimal implementation**

`data/src/main/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImpl.kt`:

```kotlin
package com.smiledev.rafiq.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DatabaseCopier
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.asSuccess
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HadithRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseCopier: DatabaseCopier
) : HadithRepository {

    private var db: SQLiteDatabase? = null

    override fun getBooks(): Result<List<HadithBook>, AppError> {
        return try {
            val d = getDatabase()
            val cursor = d.rawQuery(
                "SELECT id, collection, number, name_ar, name_en, name_id FROM books ORDER BY collection, number",
                null
            )
            val list = mutableListOf<HadithBook>()
            while (cursor.moveToNext()) {
                list.add(
                    HadithBook(
                        id = cursor.getString(0),
                        collection = cursor.getString(1),
                        number = cursor.getInt(2),
                        nameAr = cursor.getString(3),
                        nameEn = cursor.getString(4),
                        nameId = cursor.getString(5)
                    )
                )
            }
            cursor.close()
            list.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load hadith books", e))
        }
    }

    override fun getHadithsByBook(bookId: String): Result<List<Hadith>, AppError> {
        return try {
            val d = getDatabase()
            val cursor = d.rawQuery(
                "SELECT id, book_id, in_book_number, narrator_ar, narrator_en, text_ar, text_en, text_id" +
                    " FROM hadiths WHERE book_id = ? ORDER BY in_book_number",
                arrayOf(bookId)
            )
            val list = mutableListOf<Hadith>()
            while (cursor.moveToNext()) {
                list.add(
                    Hadith(
                        id = cursor.getInt(0),
                        bookId = cursor.getString(1),
                        inBookNumber = cursor.getInt(2),
                        narratorAr = cursor.getString(3).ifBlank { null },
                        narratorEn = cursor.getString(4).ifBlank { null },
                        textAr = cursor.getString(5),
                        textEn = cursor.getString(6),
                        textId = cursor.getString(7)
                    )
                )
            }
            cursor.close()
            list.asSuccess()
        } catch (e: Exception) {
            Result.Error(AppError.Database("Failed to load hadiths for book $bookId", e))
        }
    }

    private fun getDatabase(): SQLiteDatabase {
        if (db?.isOpen == true) return db!!
        databaseCopier.copyDatabaseIfNeeded("hadiths/hadith.db")
        val flatName = "hadiths/hadith.db".replace('/', '_')
        val dbFile = File(context.filesDir, "databases/$flatName")
        if (!dbFile.exists() || dbFile.length() == 0L) {
            throw IllegalStateException("hadith.db missing after copy: $flatName")
        }
        val opened = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = opened.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('books','hadiths')",
            null
        )
        val tables = mutableSetOf<String>()
        while (cursor.moveToNext()) tables.add(cursor.getString(0))
        cursor.close()
        if (tables.size < 2) {
            opened.close()
            dbFile.delete()
            throw IllegalStateException("hadith.db missing required tables: $tables")
        }
        db = opened
        return opened
    }
}
```

**Note:** `getHadithsByBook` is included now (both queries share `getDatabase()` and the test fixture uses both tables) — Task 4 only adds its dedicated tests.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :data:testDebugUnitTest --tests "com.smiledev.rafiq.data.repository.HadithRepositoryImplTest"`
Expected: PASS (both tests).

- [ ] **Step 5: Commit**

```bash
git add data/src/main/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImpl.kt data/src/test/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImplTest.kt
git commit -m "feat(data): hadith repository over read-only asset db"
```

---

### Task 4: getHadithsByBook tests

**Files:**
- Modify: `data/src/test/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `HadithRepositoryImpl.getHadithsByBook(bookId)` (Task 3).
- Produces: no new API.

- [ ] **Step 1: Write the failing tests (append to the test class)**

```kotlin
@Test
fun `getHadithsByBook returns hadiths in book number order`() {
    val result = repo.getHadithsByBook("bukhari.1")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    val hadiths = (result as Result.Success).data
    assertEquals(1, hadiths.size)
    assertEquals("bukhari.1", hadiths[0].bookId)
    assertEquals("t1", hadiths[0].textEn)
}

@Test
fun `getHadithsByBook filters to the requested book`() {
    val result = repo.getHadithsByBook("muslim.1")

    assertTrue("Expected Success but got ${result}", result is Result.Success)
    assertEquals(0, (result as Result.Success).data.size)
}

@Test
fun `getHadithsByBook returns Error when db file missing`() {
    dbFile.delete()

    val result = repo.getHadithsByBook("bukhari.1")

    assertTrue("Expected Error but got ${result}", result is Result.Error)
    assertTrue((result as Result.Error).error is AppError.Database)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :data:testDebugUnitTest --tests "com.smiledev.rafiq.data.repository.HadithRepositoryImplTest"`
Expected: FAIL on the new tests (the connection returns 0 rows because `getDatabase()` was never called in those paths when classes exist but... task 3 code made queries; if tests fail, verify actual reason). If Task 3's code is complete these may pass immediately — that is expected and fine; the real gate is the missing-DB path which exercises the `Error` branch.

- [ ] **Step 3: Verify implementation handles both branches**

If any test fails (e.g. no rows returned), the cause is the fixture: add more `hadiths` rows to `createFixtureDb` for both books, then re-run. Confirm `getHadithsByBook` returns `Error` when the file is gone (already implemented in Task 3).

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :data:testDebugUnitTest --tests "com.smiledev.rafiq.data.repository.HadithRepositoryImplTest"`
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add data/src/test/kotlin/com/smiledev/rafiq/data/repository/HadithRepositoryImplTest.kt
git commit -m "test(data): hadith repository per-book query tests"
```

---

### Task 5: DI binding

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/di/AppModule.kt:63-76` (inside `RepositoryModule`)

**Interfaces:**
- Consumes: `HadithRepositoryImpl` (Task 3).
- Produces: Hilt binding so `@Inject` for `HadithRepository` resolves to `HadithRepositoryImpl`.

- [ ] **Step 1: Add the binding**

In `RepositoryModule`, after the `bindProphetRepository` line (line 71), add:

```kotlin
@Binds @Singleton abstract fun bindHadithRepository(impl: HadithRepositoryImpl): HadithRepository
```

Add imports if not present:

```kotlin
import com.smiledev.rafiq.data.repository.HadithRepositoryImpl
import com.smiledev.rafiq.domain.repository.HadithRepository
```

- [ ] **Step 2: Verify compile**

Run: `.\gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (KAPT generates the binding; no runtime DI test needed here).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/di/AppModule.kt
git commit -m "feat(di): bind HadithRepositoryImpl"
```

---

### Phase 3 — ViewModels

---

### Task 6: HadithBooksViewModel

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithBooksViewModel.kt`
- Test: `app/src/test/java/com/smiledev/rafiq/ui/hadith/HadithBooksViewModelTest.kt`

**Interfaces:**
- Consumes: `HadithRepository.getBooks()` (Task 1/3), `PreferencesManager.translationLanguage` (`data/.../preferences/PreferencesManager.kt:64`), `DispatcherProvider` (`:core`), `currentLocaleCode()` (`:core`).
- Produces:
  - `@Immutable data class HadithBooksUiState(val books: List<HadithBook> = emptyList(), val isLoading: Boolean = false, val error: AppError? = null, val translationLanguage: String = "system")`
  - `@HiltViewModel class HadithBooksViewModel` with `val uiState: StateFlow<HadithBooksUiState>` and `fun loadBooks()`; `val localeCode = currentLocaleCode()`.
  - `fun resolvedLanguage(): String` — `if (translationLanguage == "system") currentLocaleCode() else translationLanguage`.

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/smiledev/rafiq/ui/hadith/HadithBooksViewModelTest.kt` (mirror `ProphetsViewModelTest` + `SettingsScreenTest` pref-mocking):

```kotlin
package com.smiledev.rafiq.ui.hadith

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HadithBooksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: HadithRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private fun books() = listOf(
        HadithBook("bukhari.1", "bukhari", 1, "كتاب بدء الوحي", "Revelation", "Permulaan Wahyu")
    )

    private fun createVm(): HadithBooksViewModel {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        return HadithBooksViewModel(repository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `load books success`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Success(books())

        val vm = createVm()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.books.size)
        assertEquals(false, vm.uiState.value.isLoading)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun `load books error`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Error(AppError.Database("fail", null))

        val vm = createVm()
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.books.size)
        assertEquals(false, vm.uiState.value.isLoading)
        assertEquals(true, vm.uiState.value.error != null)
    }

    @Test
    fun `resolvedLanguage uses pref when set`() = runTest(testDispatcher) {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("id")

        val vm = createVm()
        advanceUntilIdle()

        assertEquals("id", vm.resolvedLanguage())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq.ui.hadith.HadithBooksViewModelTest"`
Expected: FAIL — `HadithBooksViewModel` not defined.

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithBooksViewModel.kt`:

```kotlin
package com.smiledev.rafiq.ui.hadith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HadithBooksUiState(
    val books: List<HadithBook> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val translationLanguage: String = "system"
)

@HiltViewModel
class HadithBooksViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HadithBooksUiState())
    val uiState: StateFlow<HadithBooksUiState> = _uiState

    val localeCode = currentLocaleCode()

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.value = _uiState.value.copy(translationLanguage = lang)
            }
        }
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = hadithRepository.getBooks()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(books = result.data, isLoading = false)
                is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
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

Run: `.\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq.ui.hadith.HadithBooksViewModelTest"`
Expected: PASS (all three tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithBooksViewModel.kt app/src/test/java/com/smiledev/rafiq/ui/hadith/HadithBooksViewModelTest.kt
git commit -m "feat(app): hadith books viewmodel"
```

---

### Task 7: HadithListViewModel

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithListViewModel.kt`
- Test: `app/src/test/java/com/smiledev/rafiq/ui/hadith/HadithListViewModelTest.kt`

**Interfaces:**
- Consumes: `HadithRepository.getBooks()` + `getHadithsByBook(bookId)`, `PreferencesManager.translationLanguage`, `DispatcherProvider`, `currentLocaleCode()`; `HadithBook`/`Hadith` (Task 1).
- Produces:
  - `@Immutable data class HadithListUiState(val book: HadithBook? = null, val hadiths: List<Hadith> = emptyList(), val isLoading: Boolean = false, val error: AppError? = null, val translationLanguage: String = "system")`
  - `@HiltViewModel class HadithListViewModel` with `fun load(bookId: String)`, `fun resolvedLanguage(): String`, `val uiState`.

- [ ] **Step 1: Write the failing test**

`app/src/test/java/com/smiledev/rafiq/ui/hadith/HadithListViewModelTest.kt`:

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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HadithListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: HadithRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private val book = HadithBook("bukhari.1", "bukhari", 1, "كتاب بدء الوحي", "Revelation", "Permulaan Wahyu")
    private val hadith = Hadith(1, "bukhari.1", 1, "نarrator", "Narrator", "arabic", "english", "indonesia")

    private fun createVm(): HadithListViewModel {
        every { preferencesManager.translationLanguage } returns MutableStateFlow("system")
        return HadithListViewModel(repository, preferencesManager, testDispatcherProvider)
    }

    @Test
    fun `load populates book and hadiths`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Success(listOf(book))
        every { repository.getHadithsByBook("bukhari.1") } returns Result.Success(listOf(hadith))

        val vm = createVm()
        vm.load("bukhari.1")
        advanceUntilIdle()

        assertEquals("bukhari.1", vm.uiState.value.book?.id)
        assertEquals(1, vm.uiState.value.hadiths.size)
        assertEquals(false, vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `load error surfaces in state`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Success(listOf(book))
        every { repository.getHadithsByBook("bukhari.1") } returns Result.Error(AppError.Database("fail", null))

        val vm = createVm()
        vm.load("bukhari.1")
        advanceUntilIdle()

        assertEquals(true, vm.uiState.value.error != null)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `resolvedLanguage maps system to locale code`() = runTest(testDispatcher) {
        every { repository.getBooks() } returns Result.Success(emptyList())
        every { repository.getHadithsByBook(any()) } returns Result.Success(emptyList())

        val vm = createVm()
        vm.load("bukhari.1")
        advanceUntilIdle()

        assertEquals("en", vm.resolvedLanguage()) // JVM default locale; adjust if locale is id
    }
}
```

**Note:** `resolvedLanguage` "system" → `currentLocaleCode()`. On the CI/JVM default locale this returns `"en"` (see `LocaleUtil.kt:5-8`). If the machine locale is `id`, assert `"id"` instead — check with `Locale.getDefault().language`.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq.ui.hadith.HadithListViewModelTest"`
Expected: FAIL — `HadithListViewModel` not defined.

- [ ] **Step 3: Write minimal implementation**

`app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithListViewModel.kt`:

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HadithListUiState(
    val book: HadithBook? = null,
    val hadiths: List<Hadith> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val translationLanguage: String = "system"
)

@HiltViewModel
class HadithListViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val preferencesManager: PreferencesManager,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HadithListUiState())
    val uiState: StateFlow<HadithListUiState> = _uiState

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            preferencesManager.translationLanguage.collect { lang ->
                _uiState.value = _uiState.value.copy(translationLanguage = lang)
            }
        }
    }

    fun load(bookId: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val booksResult = hadithRepository.getBooks()
            val hadithsResult = hadithRepository.getHadithsByBook(bookId)
            val book = (booksResult as? Result.Success)?.data?.find { it.id == bookId }
            when (hadithsResult) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    book = book, hadiths = hadithsResult.data, isLoading = false
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = hadithsResult.error)
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

Run: `.\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq.ui.hadith.HadithListViewModelTest"`
Expected: PASS (all three tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithListViewModel.kt app/src/test/java/com/smiledev/rafiq/ui/hadith/HadithListViewModelTest.kt
git commit -m "feat(app): hadith list viewmodel"
```

---

### Phase 4 — Screens + strings

**Strings helper (added once, used by Tasks 8–11).** In `app/src/main/res/values/strings.xml`, add:

```xml
<!-- Hadith -->
<string name="hadiths">Hadiths</string>
<string name="hadith_reference">Sahih %1$s · Book %2$d, Hadith %3$d</string>
<string name="hadith_no_arabic">[No Arabic text]</string>
```

In `app/src/main/res/values-id/strings.xml`, add:

```xml
<!-- Hadith -->
<string name="hadiths">Hadis</string>
<string name="hadith_reference">Shahih %1$s · Kitab %2$d, Hadis %3$d</string>
<string name="hadith_no_arabic">[Tidak ada teks Arab]</string>
```

There is no per-collection string needed — collection label is produced from data (see Task 10).

---

### Task 8: HadithBooksScreen

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithBooksScreen.kt`
- Create: `app/src/main/res/drawable/ic_hadith.xml`
- Test: `app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithBooksScreenTest.kt`

**Interfaces:**
- Consumes: `HadithBooksViewModel` (Task 6), `HadithBook` (Task 1), strings above.
- Produces: `@Composable fun HadithBooksScreen(onHadithBookClick: (String) -> Unit, onBack: () -> Unit, viewModel: HadithBooksViewModel = hiltViewModel(), modifier: Modifier = Modifier)`.

- [ ] **Step 1: Create the icon `ic_hadith.xml`** (open-book vector, white fill — matches other `ic_*`):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M21,5c-1.11,-0.35 -2.33,-0.5 -3.5,-0.5c-1.95,0 -4.05,0.4 -5.5,1.5c-1.45,-1.1 -3.55,-1.5 -5.5,-1.5C5.33,4.5 4.11,4.65 3,5c-0.55,0.2 -1,0.75 -1,1.35V20c0,0.55 0.45,1 1,1c1.05,0 2.83,-0.15 4,-0.5c1.97,-0.6 2.64,-1.1 4,-1.5c1.36,0.4 2.03,0.9 4,1.5c1.17,0.35 2.95,0.5 4,0.5c0.55,0 1,-0.45 1,-1V6.35C22,5.75 21.55,5.2 21,5zM20,18.5c-1.05,-0.2 -2.27,-0.31 -3.5,-0.31c-1.45,0 -2.67,0.13 -3.5,0.34V8.68c0.83,-0.21 2.05,-0.34 3.5,-0.34c1.23,0 2.45,0.11 3.5,0.31V18.5z" />
</vector>
```

- [ ] **Step 2: Write the failing Compose UI test**

`app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithBooksScreenTest.kt` (mirror `SettingsScreenTest` structure — construct the VM with mocks and pass it in):

```kotlin
package com.smiledev.rafiq.ui.hadith

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasClickAction
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.HadithBook
import com.smiledev.rafiq.domain.repository.HadithRepository
import com.smiledev.rafiq.core.Result
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class HadithBooksScreenTest {

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

    @Test
    fun booksAreDisplayedAndClickNavigates() {
        val repo = mockk<HadithRepository>(relaxed = true)
        every { repo.getBooks() } returns Result.Success(listOf(book))
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.translationLanguage } returns MutableStateFlow("en")
        val viewModel = HadithBooksViewModel(repo, prefs, dispatcher())

        var clickedId: String? = null
        composeTestRule.setContent {
            HadithBooksScreen(
                onHadithBookClick = { clickedId = it },
                onBack = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Revelation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Revelation").performClick()
        assertEquals("bukhari.1", clickedId)
    }

    @Test
    fun collectionSubtitleShownPerBook() {
        val repo = mockk<HadithRepository>(relaxed = true)
        every { repo.getBooks() } returns Result.Success(listOf(book))
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.translationLanguage } returns MutableStateFlow("en")
        val viewModel = HadithBooksViewModel(repo, prefs, dispatcher())

        composeTestRule.setContent {
            HadithBooksScreen(onHadithBookClick = {}, onBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Sahih al-Bukhari · Book 1").assertIsDisplayed()
    }
}
```

**Note:** tests rely on `translationLanguage = "en"` so the localized name `nameEn` and the collection label `"Sahih al-Bukhari · Book 1"` render deterministically.

- [ ] **Step 3: Write the screen**

`app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithBooksScreen.kt` (mirrors `ProphetsScreen` grid + `DashboardScreen` collection label):

```kotlin
package com.smiledev.rafiq.ui.hadith

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.displayMessage
import com.smiledev.rafiq.domain.model.HadithBook

private val arabicFont = FontFamily(Font(R.font.me_quran))

private fun collectionLabel(collection: String): String =
    if (collection == "bukhari") "Sahih al-Bukhari" else "Sahih Muslim"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithBooksScreen(
    onHadithBookClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HadithBooksViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hadiths)) },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.error != null -> Text(
                    text = state.error?.displayMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
                state.isLoading && state.books.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                ) {
                    items(state.books) { book ->
                        val localizedName = if (viewModel.resolvedLanguage() == "id") book.nameId else book.nameEn
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .clickable { onHadithBookClick(book.id) },
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
                                    text = book.nameAr,
                                    fontFamily = arabicFont,
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = localizedName,
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${collectionLabel(book.collection)} · Book ${book.number}",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
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
```

- [ ] **Step 4: Verify build compiles**

Run: `.\gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run UI tests on emulator**

Run: `.\gradlew connectedDebugAndroidTest --tests "com.smiledev.rafiq.ui.hadith.HadithBooksScreenTest"`
Expected: PASS (emulator `Medium_Phone_API_35` must be running).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithBooksScreen.kt app/src/main/res/drawable/ic_hadith.xml app/src/main/res/values/strings.xml app/src/main/res/values-id/strings.xml app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithBooksScreenTest.kt
git commit -m "feat(app): hadith books screen"
```

---

### Task 9: HadithListScreen

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithListScreen.kt`
- Test: `app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithListScreenTest.kt`

**Interfaces:**
- Consumes: `HadithListViewModel.load(bookId)` (Task 7), `Hadith`/`HadithBook` (Task 1), strings.
- Produces: `@Composable fun HadithListScreen(bookId: String, onHadithClick: (Int) -> Unit, onBack: () -> Unit, viewModel: HadithListViewModel = hiltViewModel(), modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write the failing Compose UI test**

`app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithListScreenTest.kt`:

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
class HadithListScreenTest {

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

    private fun viewModel(): HadithListViewModel {
        val repo = mockk<HadithRepository>(relaxed = true)
        every { repo.getBooks() } returns Result.Success(listOf(book))
        every { repo.getHadithsByBook("bukhari.1") } returns Result.Success(listOf(hadith))
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.translationLanguage } returns MutableStateFlow("en")
        return HadithListViewModel(repo, prefs, dispatcher()).apply { load("bukhari.1") }
    }

    @Test
    fun hadithCardShownAndClickNavigates() {
        composeTestRule.setContent {
            HadithListScreen(bookId = "bukhari.1", onHadithClick = {}, onBack = {}, viewModel = viewModel())
        }

        composeTestRule.onNodeWithText("Book 1 · Hadith 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Book 1 · Hadith 1").performClick()
    }

    @Test
    fun titleShowsBookName() {
        composeTestRule.setContent {
            HadithListScreen(bookId = "bukhari.1", onHadithClick = {}, onBack = {}, viewModel = viewModel())
        }

        composeTestRule.onNodeWithText("Revelation").assertIsDisplayed()
        assertEquals(1, composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Book 1 · Hadith 1")).fetchSemanticsNodes().size)
    }
}
```

- [ ] **Step 2: Write the screen**

`app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithListScreen.kt` (mirrors `AsmaulHusnaScreen` single-column list + `ProphetsScreen` loading/error states):

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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.displayMessage
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithListScreen(
    bookId: String,
    onHadithClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: HadithListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(bookId) { viewModel.load(bookId) }

    val title = state.book?.let {
        if (viewModel.resolvedLanguage() == "id") it.nameId else it.nameEn
    } ?: stringResource(R.string.hadiths)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.error != null -> Text(
                    text = state.error?.displayMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
                state.isLoading && state.hadiths.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.hadiths, key = { it.id }) { hadith ->
                        HadithCard(
                            hadith = hadith,
                            onClick = { onHadithClick(hadith.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithCard(hadith: Hadith, onClick: () -> Unit) {
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
                text = "Book ${hadith.bookId.substringAfterLast('.')} · Hadith ${hadith.inBookNumber}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = hadith.narratorEn ?: hadith.textEn,
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
```

- [ ] **Step 3: Verify build compiles**

Run: `.\gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run UI tests on emulator**

Run: `.\gradlew connectedDebugAndroidTest --tests "com.smiledev.rafiq.ui.hadith.HadithListScreenTest"`
Expected: PASS (emulator running).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithListScreen.kt app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithListScreenTest.kt
git commit -m "feat(app): hadith list screen"
```

---

### Task 10: HadithDetailScreen

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithDetailScreen.kt`
- Test: `app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithDetailScreenTest.kt`

**Interfaces:**
- Consumes: `HadithListViewModel` (Task 7) reused (same pattern as `ProphetDetailScreen`), `Hadith`/`HadithBook` (Task 1), strings.
- Produces: `@Composable fun HadithDetailScreen(hadithId: Int, onBack: () -> Unit, viewModel: HadithListViewModel = hiltViewModel(), modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write the failing Compose UI test**

`app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithDetailScreenTest.kt` — tests the three language modes (`id`, `en`, `both`) plus Arabic rendering:

```kotlin
package com.smiledev.rafiq.ui.hadith

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class HadithDetailScreenTest {

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

    private fun viewModel(lang: String): HadithListViewModel {
        val repo = mockk<HadithRepository>(relaxed = true)
        every { repo.getBooks() } returns Result.Success(listOf(book))
        every { repo.getHadithsByBook("bukhari.1") } returns Result.Success(listOf(hadith))
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.translationLanguage } returns MutableStateFlow(lang)
        return HadithListViewModel(repo, prefs, dispatcher()).apply { load("bukhari.1") }
    }

    @Test
    fun showsArabicAndReferenceLine() {
        composeTestRule.setContent {
            HadithDetailScreen(hadithId = 1, onBack = {}, viewModel = viewModel("en"))
        }

        composeTestRule.onNodeWithText("نص عربي").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sahih al-Bukhari · Book 1, Hadith 1").assertIsDisplayed()
    }

    @Test
    fun enModeShowsEnglishTranslationOnly() {
        composeTestRule.setContent {
            HadithDetailScreen(hadithId = 1, onBack = {}, viewModel = viewModel("en"))
        }

        composeTestRule.onNodeWithText("English text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Teks Indonesia").assertDoesNotExist()
    }

    @Test
    fun idModeShowsIndonesianTranslationOnly() {
        composeTestRule.setContent {
            HadithDetailScreen(hadithId = 1, onBack = {}, viewModel = viewModel("id"))
        }

        composeTestRule.onNodeWithText("Teks Indonesia").assertIsDisplayed()
        composeTestRule.onNodeWithText("English text").assertDoesNotExist()
    }

    @Test
    fun bothModeShowsBothTranslationsWithChips() {
        composeTestRule.setContent {
            HadithDetailScreen(hadithId = 1, onBack = {}, viewModel = viewModel("both"))
        }

        composeTestRule.onNodeWithText("English text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Teks Indonesia").assertIsDisplayed()
        composeTestRule.onNodeWithText("ID").assertIsDisplayed()
        composeTestRule.onNodeWithText("EN").assertIsDisplayed()
    }
}
```

**Imports note:** `assertDoesNotExist` is a member of `SemanticsNodeInteraction` (`androidx.compose.ui.test.assertDoesNotExist`). Add the import.

- [ ] **Step 2: Write the screen**

`app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithDetailScreen.kt` (reuses `HadithListViewModel` + `find` like `ProphetDetailScreen.kt:56`; "both" branch mirrors `AyahScreen.kt:637-705`):

```kotlin
package com.smiledev.rafiq.ui.hadith

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.domain.model.Hadith

private val arabicFont = FontFamily(Font(R.font.me_quran))

private fun collectionName(collection: String): String =
    if (collection == "bukhari") "al-Bukhari" else "Muslim"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithDetailScreen(
    hadithId: Int,
    onBack: () -> Unit,
    viewModel: HadithListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val hadith = state.hadiths.find { it.id == hadithId }
    val resolvedLang = viewModel.resolvedLanguage()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hadiths)) },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (hadith == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                val collection = state.book?.collection ?: "bukhari"
                Text(
                    text = "Sahih ${collectionName(collection)} · Book ${hadith.bookId.substringAfterLast('.')}, Hadith ${hadith.inBookNumber}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                // Arabic matn
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(
                            text = hadith.textAr,
                            fontFamily = arabicFont,
                            fontSize = 28.sp,
                            lineHeight = 44.sp,
                            textAlign = TextAlign.Center
                        )
                        hadith.narratorAr?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = it,
                                fontFamily = arabicFont,
                                fontSize = 15.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                hadith.narratorEn?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                }

                when (resolvedLang) {
                    "id" -> TranslationSection("ID", hadith.translationText(preferId = true).orEmpty())
                    "en" -> TranslationSection("EN", hadith.translationText(preferId = false).orEmpty())
                    else -> {
                        TranslationSection("ID", hadith.translationText(preferId = true).orEmpty())
                        Spacer(Modifier.height(12.dp))
                        TranslationSection("EN", hadith.translationText(preferId = false).orEmpty())
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationSection(chip: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row {
                Text(
                    text = chip,
                    modifier = Modifier.padding(end = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (text.isBlank()) {
                Text(
                    text = "Translation unavailable",
                    fontSize = 15.sp,
                    color = Color.Gray
                )
            } else {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }
        }
    }
}
```

**Fallback helper** (at bottom of the same file) for ID→EN fallback, matching `AyahViewModel.getTranslationText` (`AyahViewModel.kt:297-305`):

```kotlin
private fun Hadith.translationText(preferId: Boolean): String? {
    val text = if (preferId) textId else textEn
    return text.takeIf { it.isNotBlank() }
        ?: (if (preferId) textEn else textId).takeIf { it.isNotBlank() }
}
```

**Note on the reference line:** `collectionName(collection)` returns `"al-Bukhari"`/`"Muslim"`, so the line renders `Sahih al-Bukhari · Book 1, Hadith 1` and `Sahih Muslim · Book 1, Hadith 1`, matching the test's `onNodeWithText("Sahih al-Bukhari · Book 1, Hadith 1")`.

- [ ] **Step 3: Verify build compiles**

Run: `.\gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run UI tests on emulator**

Run: `.\gradlew connectedDebugAndroidTest --tests "com.smiledev.rafiq.ui.hadith.HadithDetailScreenTest"`
Expected: PASS (emulator running). The Arabic text node matching works because the test value (`نص عربي`) equals the fixture's `textAr` exactly.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/hadith/HadithDetailScreen.kt app/src/androidTest/java/com/smiledev/rafiq/ui/hadith/HadithDetailScreenTest.kt
git commit -m "feat(app): hadith detail screen"
```

---

### Phase 5 — Navigation & entry points

---

### Task 11: Navigation keys, entries, dashboard + settings entry points

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/NavigationKeys.kt` (add 3 keys)
- Modify: `app/src/main/java/com/smiledev/rafiq/Navigation.kt` (add 3 entries + imports)
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt` (add FeatureItem + import)
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/settings/SettingsScreen.kt` (add MoreFeatureItem + import)

**Interfaces:**
- Consumes: `HadithBooksScreen` (Task 8), `HadithListScreen` (Task 9), `HadithDetailScreen` (Task 10), `ic_hadith` (Task 8), strings (Phase 4).
- Produces: nav routes `HadithBooks` (object), `HadithList(val bookId: String)`, `HadithDetail(val hadithId: Int)` and the three entry blocks.

- [ ] **Step 1: Add NavKeys**

In `NavigationKeys.kt`, append after `ProphetDetail` (line 13):

```kotlin
@Serializable data object HadithBooks : NavKey
@Serializable data class HadithList(val bookId: String) : NavKey
@Serializable data class HadithDetail(val hadithId: Int) : NavKey
```

- [ ] **Step 2: Register entries in `Navigation.kt`**

Imports:

```kotlin
import com.smiledev.rafiq.ui.hadith.HadithBooksScreen
import com.smiledev.rafiq.ui.hadith.HadithListScreen
import com.smiledev.rafiq.ui.hadith.HadithDetailScreen
```

Entries (place after the `ProphetDetail` block, line 95):

```kotlin
entry<HadithBooks> {
  HadithBooksScreen(
    onHadithBookClick = { bookId -> backStack.add(HadithList(bookId)) },
    onBack = { backStack.removeLastOrNull() },
    modifier = Modifier.safeDrawingPadding()
  )
}
entry<HadithList> { key ->
  HadithListScreen(
    bookId = key.bookId,
    onHadithClick = { id -> backStack.add(HadithDetail(id)) },
    onBack = { backStack.removeLastOrNull() },
    modifier = Modifier.safeDrawingPadding()
  )
}
entry<HadithDetail> { key ->
  HadithDetailScreen(
    hadithId = key.hadithId,
    onBack = { backStack.removeLastOrNull() },
    modifier = Modifier.safeDrawingPadding()
  )
}
```

- [ ] **Step 3: Add Dashboard feature card**

In `DashboardScreen.kt`, add import `import com.smiledev.rafiq.HadithBooks` and extend `features` (after line 66):

```kotlin
FeatureItem(R.string.hadiths, HadithBooks, R.drawable.ic_hadith, Color(0xFF8D6E63)),
```

- [ ] **Step 4: Add Settings "More Features" item**

In `SettingsScreen.kt`, add imports `com.smiledev.rafiq.HadithBooks` and add inside the `AnimatedVisibility` (after line 120):

```kotlin
MoreFeatureItem(R.string.hadiths) { onNavigate(HadithBooks) }
```

- [ ] **Step 5: Verify full build + all unit tests**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew testDebug`
Expected: BUILD SUCCESSFUL, all unit tests pass (including Tasks 3–7).

- [ ] **Step 6: Verify navigation smoke (instrumented)**

Run: `.\gradlew connectedDebugAndroidTest --tests "com.smiledev.rafiq.ui.hadith"`
Expected: PASS — all hadith Compose tests + the new nav wiring compile and run. (No dedicated nav-graph test is added; the dashboard card, settings item, and entries compile and the screen tests cover rendering.)

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/NavigationKeys.kt app/src/main/java/com/smiledev/rafiq/Navigation.kt app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt app/src/main/java/com/smiledev/rafiq/ui/settings/SettingsScreen.kt
git commit -m "feat(app): wire hadith screens into navigation"
```

---

### Phase 6 — Final verification

### Task 12: End-to-end verification

**Files:** (none new)

- [ ] **Step 1: Clean full unit test run**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew clean testDebug`
Expected: BUILD SUCCESSFUL; all modules' unit tests pass (domain, data, app).

- [ ] **Step 2: Build installable APK**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew assembleDebug`
Expected: `app/build/outputs/apk/debug/app-debug.apk` produced.

- [ ] **Step 3: Manual smoke on emulator**

Install and launch, then: Dashboard → Hadiths → tap book → tap hadith → verify Arabic + translation per the Quran translation setting; change setting to `both` in Settings and re-verify.

```powershell
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 shell am start -n com.smiledev.rafiq/.MainActivity
```

Expected: no crash; hadith books grid, list, and detail render; language follows `translation_language`.

- [ ] **Step 4: Commit any residual changes**

```bash
git status
git add -A
git commit -m "chore: finalize hadith feature"
# only if there are changes; otherwise skip
```

## Testing Strategy

- **JVM (unit)** — always runnable: `.\gradlew testDebug`. Covers domain declarations (compile), repo queries + error branches (`HadithRepositoryImplTest`), VM state transitions and language resolution (`HadithBooksViewModelTest`, `HadithListViewModelTest`). Run on JDK 17 (Adoptium path is set in the commands above; AGENTS.md's default JBR path is broken on this machine — its `conf/` dir is missing).
- **Instrumented (Compose)** — `.\gradlew connectedDebugAndroidTest --tests "com.smiledev.rafiq.ui.hadith"`; requires emulator `Medium_Phone_API_35`. Covers books grid + click nav, list rendering, detail rendering in `id`/`en`/`both` modes, Arabic + reference line.
- **Data fixture strategy** — repo tests place a fixture `hadiths_hadith.db` directly in `filesDir/databases/` (mirrors the real copy target) with `DatabaseCopier` mocked to a no-op, exactly like `QuranRepositoryImplTest` mocks `AssetManager`.
- **Pref mocking** — VMs take `PreferencesManager` (mockk relaxed) with `translationLanguage` stubbed to a `MutableStateFlow`; same as `SettingsScreenTest`.

## Performance Considerations

- **DB opened once, cached** — `HadithRepositoryImpl.db` field caches the open handle; `getDatabase()` reuses it while `isOpen` (`QuranRepositoryImpl.kt:35-37` pattern). `sqlite_master` sanity runs once because `db` is set after it.
- **Read-only** — `OPEN_READONLY`, no writes, no Room observability overhead; content is static.
- **Indexed lookup** — `idx_hadiths_book` (Task 2) makes the per-book `WHERE book_id = ?` an index scan; with ~12k rows post-Plan-B this stays sub-millisecond.
- **No double fetch on detail** — detail reuses `HadithListViewModel` (shared NavDisplay back-stack scope, `ProphetDetailScreen` pattern), so the list data already in state is not re-queried.
- **`LaunchedEffect(bookId)` in list screen** guards against redundant reloads on recomposition.
- **Grid/list sizes** — `LazyVerticalGrid`/`LazyColumn` render only visible items; a full Bukhari book (~90 hadith) renders fine. Full-corpus browsing is a Plan B concern (possible book-level pagination if needed — deferred).
- **Asset copy** — `copyDatabaseIfNeeded` copies once; subsequent opens skip the copy (exists + length > 0), same as the 3MB translation DBs.