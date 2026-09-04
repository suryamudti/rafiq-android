package com.smiledev.rafiq_quran.core

import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DatabaseCopierTest {

    private val context: Context = mockk(relaxed = true)
    private val assetManager: AssetManager = mockk()
    private lateinit var tempDir: File
    private lateinit var copier: DatabaseCopier

    @Before
    fun setUp() {
        tempDir = createTempDir()
        every { context.filesDir } returns tempDir
        every { context.assets } returns assetManager
        copier = DatabaseCopier(context)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `copyDatabaseIfNeeded copies nested database paths`() {
        val dummyData = "sqlite dummy content".toByteArray()
        every { assetManager.open("quran-data/translations/id.indonesian.db") } returns ByteArrayInputStream(dummyData)

        copier.copyDatabaseIfNeeded("translations/id.indonesian.db")

        val targetFile = File(tempDir, "databases/translations_id.indonesian.db")
        assertTrue(targetFile.exists())
        assertTrue(targetFile.length() > 0)
    }

    @Test
    fun `copyDatabaseIfNeeded rejects path traversal and invalid paths`() {
        copier.copyDatabaseIfNeeded("../evil.db")
        copier.copyDatabaseIfNeeded("/absolute/evil.db")
        copier.copyDatabaseIfNeeded("nested\\evil.db")
        copier.copyDatabaseIfNeeded("")

        val databasesDir = File(tempDir, "databases")
        assertFalse(databasesDir?.exists() == true && databasesDir.listFiles()?.isNotEmpty() == true)
    }

    @Test
    fun `copyAndVerifyTranslationDb succeeds for valid database with verses table`() {
        val dbFile = File(tempDir, "source.db")
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL("CREATE TABLE verses (sura TEXT, ayah INTEGER, text TEXT)")
            db.execSQL("INSERT INTO verses VALUES ('1', 1, 'Sample text')")
        }
        val dbBytes = dbFile.readBytes()
        every { assetManager.open("quran-data/translations/en.sahih.db") } returns ByteArrayInputStream(dbBytes)

        val result = copier.copyAndVerifyTranslationDb("translations/en.sahih.db")

        assertTrue(result)
        val copiedFile = File(tempDir, "databases/translations_en.sahih.db")
        assertTrue(copiedFile.exists())
    }

    @Test
    fun `copyAndVerifyTranslationDb rejects invalid path names`() {
        assertFalse(copier.copyAndVerifyTranslationDb("../evil.db"))
        assertFalse(copier.copyAndVerifyTranslationDb("/root/evil.db"))
        assertFalse(copier.copyAndVerifyTranslationDb("path\\evil.db"))
        assertFalse(copier.copyAndVerifyTranslationDb(""))
    }
}
