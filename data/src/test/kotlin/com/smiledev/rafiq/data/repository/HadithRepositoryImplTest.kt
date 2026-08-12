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
