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
