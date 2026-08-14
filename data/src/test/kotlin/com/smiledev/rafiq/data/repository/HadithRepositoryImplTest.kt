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
import org.junit.Assert.assertNull
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
        db.execSQL("INSERT INTO hadiths VALUES (1,'bukhari.1',1,'','','نص واحد','t1','satu')")
        db.execSQL("INSERT INTO hadiths VALUES (2,'muslim.1',1,'','','نص اثنان','prayer text','teks shalat')")
        db.execSQL("INSERT INTO hadiths VALUES (3,'bukhari.1',2,'','','نص ثلاثة','price 50% off','harga diskon 50%')")
        db.execSQL("INSERT INTO hadiths VALUES (4,'muslim.1',2,'','','نص أربعة','under_score text','teks garis_bawah')")
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

    @Test
    fun `getHadithsByBook returns hadiths in book number order`() {
        val result = repo.getHadithsByBook("bukhari.1")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val hadiths = (result as Result.Success).data
        assertEquals(2, hadiths.size)
        assertEquals("bukhari.1", hadiths[0].bookId)
        assertEquals("t1", hadiths[0].textEn)
    }

    @Test
    fun `getHadithsByBook filters to the requested book`() {
        val result = repo.getHadithsByBook("muslim.1")

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val hadiths = (result as Result.Success).data
        assertEquals(2, hadiths.size)
        assertTrue(hadiths.all { it.bookId == "muslim.1" })
    }

    @Test
    fun `getHadithsByBook returns Error when db file missing`() {
        dbFile.delete()

        val result = repo.getHadithsByBook("bukhari.1")

        assertTrue("Expected Error but got ${result}", result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }

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
}
