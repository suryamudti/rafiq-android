package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DatabaseCopier
import com.smiledev.rafiq_quran.core.Result
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
    fun `searchAyahs matches id translation when locale is both`() {
        val result = repo.searchAyahs("puji", "both")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val ayahs = (result as Result.Success).data
        assertEquals(listOf(1 to 2), ayahs.map { it.sura to it.aya })
        assertEquals("Segala puji bagi Allah, Tuhan semesta alam", ayahs[0].translation)
    }

    @Test
    fun `searchAyahs matches en translation when locale is both`() {
        val result = repo.searchAyahs("Merciful", "both")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val ayahs = (result as Result.Success).data
        assertEquals(listOf(1 to 1), ayahs.map { it.sura to it.aya })
    }

    @Test
    fun `searchAyahs with both finds id-only term in id translation`() {
        val result = repo.searchAyahs("selain", "both")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val ayahs = (result as Result.Success).data
        assertEquals(listOf(2 to 255), ayahs.map { it.sura to it.aya })
        assertEquals("Allah, tidak ada tuhan selain Dia", ayahs[0].translation)
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

    @Test
    fun `searchAyahs batches missing Arabic and translation lookups across surahs`() {
        val dbDir = File(tempRoot, "databases")
        SQLiteDatabase.openDatabase(File(dbDir, "quran-uthmani.db").absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("INSERT INTO quran VALUES ('1','3','الرَّحْمَٰنِ الرَّحِيمِ',NULL)")
            db.execSQL("INSERT INTO quran VALUES ('2','256','لَا إِكْرَاهَ فِي الدِّينِ','بِسْمِ')")
        }
        SQLiteDatabase.openDatabase(File(dbDir, "translations_en.sahih.db").absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("INSERT INTO verses VALUES ('1',3,'The Entirely Merciful, the Especially Merciful')")
            db.execSQL("INSERT INTO verses VALUES ('2',256,'There shall be no compulsion in religion Merciful')")
        }
        SQLiteDatabase.openDatabase(File(dbDir, "translations_id.indonesian.db").absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("INSERT INTO verses VALUES ('1',3,'Yang Maha Pengasih, Maha Penyayang')")
            // Intentionally omit (2, 256) from id DB to test "both" fallback to en
        }

        // Translation-only matches across surahs 1 (ayas 1, 3) and 2 (aya 256) -> batched Arabic fetch
        val transResult = repo.searchAyahs("Merciful", "en")
        assertTrue("Expected Success but got $transResult", transResult is Result.Success)
        val transAyahs = (transResult as Result.Success).data
        assertEquals(listOf(1 to 1, 1 to 3, 2 to 256), transAyahs.map { it.sura to it.aya })
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", transAyahs[0].text)
        assertEquals("الرَّحْمَٰنِ الرَّحِيمِ", transAyahs[1].text)
        assertEquals("لَا إِكْرَاهَ فِي الدِّينِ", transAyahs[2].text)
        assertEquals("بِسْمِ", transAyahs[2].bismillah)

        // Arabic-only match on (2, 256) with locale "both" -> missing in id DB, falls back to en in batch
        val arabicFallbackResult = repo.searchAyahs("إِكْرَاهَ", "both")
        assertTrue("Expected Success but got $arabicFallbackResult", arabicFallbackResult is Result.Success)
        val fallbackAyahs = (arabicFallbackResult as Result.Success).data
        assertEquals(listOf(2 to 256), fallbackAyahs.map { it.sura to it.aya })
        assertEquals("There shall be no compulsion in religion Merciful", fallbackAyahs[0].translation)

        // Arabic-only match on (1, 3) with locale "both" -> prefers id translation over en
        val arabicPreferIdResult = repo.searchAyahs("الرَّحْمَٰنِ", "both")
        assertTrue("Expected Success but got $arabicPreferIdResult", arabicPreferIdResult is Result.Success)
        val preferIdAyahs = (arabicPreferIdResult as Result.Success).data
        assertEquals(listOf(1 to 3), preferIdAyahs.map { it.sura to it.aya })
        assertEquals("Yang Maha Pengasih, Maha Penyayang", preferIdAyahs[0].translation)
    }

    @Test
    fun `searchAyahs batched translation lookup is compatible with FTS3 virtual tables`() {
        val ftsRoot = kotlin.io.path.createTempDirectory("quran_fts3_test").toFile()
        try {
            val ftsContext: Context = mockk(relaxed = true)
            val ftsAssetManager: AssetManager = mockk()
            val ftsCopier: DatabaseCopier = mockk(relaxed = true)
            every { ftsContext.assets } returns ftsAssetManager
            every { ftsContext.filesDir } returns ftsRoot
            every { ftsCopier.copyAndVerifyTranslationDb(any()) } returns true
            every { ftsAssetManager.open("quran-data/quran-metadata.json") } returns
                ByteArrayInputStream(metadataJson.toByteArray())

            val dbDir = File(ftsRoot, "databases").apply { mkdirs() }
            SQLiteDatabase.openOrCreateDatabase(File(dbDir, "quran-uthmani.db"), null).use { db ->
                db.execSQL("CREATE TABLE quran (sura TEXT NOT NULL, aya TEXT NOT NULL, text TEXT NOT NULL, bismillah TEXT)")
                db.execSQL("INSERT INTO quran VALUES ('1','1','بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ','')")
                db.execSQL("INSERT INTO quran VALUES ('1','2','الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ',NULL)")
                db.execSQL("INSERT INTO quran VALUES ('2','255','اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ',NULL)")
            }
            SQLiteDatabase.openOrCreateDatabase(File(dbDir, "translations_en.sahih.db"), null).use { db ->
                db.execSQL("CREATE VIRTUAL TABLE verses USING fts3(sura integer, ayah integer, text text, primary key(sura, ayah))")
                db.execSQL("INSERT INTO verses (sura, ayah, text) VALUES (1, 1, 'In the name of Allah, the Entirely Merciful')")
                db.execSQL("INSERT INTO verses (sura, ayah, text) VALUES (1, 2, 'All praise is due to Allah, Lord of the worlds')")
                db.execSQL("INSERT INTO verses (sura, ayah, text) VALUES (2, 255, 'Allah - there is no deity except Him')")
            }
            SQLiteDatabase.openOrCreateDatabase(File(dbDir, "translations_id.indonesian.db"), null).use { db ->
                db.execSQL("CREATE VIRTUAL TABLE verses USING fts3(sura integer, ayah integer, text text, primary key(sura, ayah))")
                db.execSQL("INSERT INTO verses (sura, ayah, text) VALUES (1, 1, 'Dengan nama Allah Yang Maha Pengasih')")
                db.execSQL("INSERT INTO verses (sura, ayah, text) VALUES (1, 2, 'Segala puji bagi Allah, Tuhan seluruh alam')")
                db.execSQL("INSERT INTO verses (sura, ayah, text) VALUES (2, 255, 'Allah, tidak ada tuhan selain Dia')")
            }

            val ftsRepo = QuranRepositoryImpl(ftsContext, ftsCopier)

            // Arabic matches in sura 1 (aya 2) and sura 2 (aya 255) -> batched FTS3 lookup
            val arabicSearch = ftsRepo.searchAyahs("لِلَّهِ", "en")
            assertTrue("Expected Success but got $arabicSearch", arabicSearch is Result.Success)
            val arabicAyahs = (arabicSearch as Result.Success).data
            assertEquals(listOf(1 to 2), arabicAyahs.map { it.sura to it.aya })
            assertEquals("All praise is due to Allah, Lord of the worlds", arabicAyahs[0].translation)

            // Translation match across suras 1 and 2 on FTS3 -> batched Arabic lookup
            val transSearch = ftsRepo.searchAyahs("Allah", "en")
            assertTrue("Expected Success but got $transSearch", transSearch is Result.Success)
            val transAyahs = (transSearch as Result.Success).data
            assertEquals(listOf(1 to 1, 1 to 2, 2 to 255), transAyahs.map { it.sura to it.aya })
            assertEquals("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ", transAyahs[2].text)
        } finally {
            ftsRoot.deleteRecursively()
        }
    }
}
