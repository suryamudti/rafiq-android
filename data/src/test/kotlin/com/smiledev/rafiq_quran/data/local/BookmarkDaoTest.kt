package com.smiledev.rafiq_quran.data.local

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BookmarkDaoTest {

    private lateinit var db: BookmarkDatabase
    private lateinit var dao: BookmarkDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), BookmarkDatabase::class.java).build()
        dao = db.bookmarkDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and getAllBookmarks flow`() = runTest {
        dao.insert(BookmarkEntity(sura = 1, aya = 1, suraName = "Al-Fatiha", insertTime = "2024-01-01"))
        dao.insert(BookmarkEntity(sura = 1, aya = 2, suraName = "Al-Fatiha", insertTime = "2024-01-02"))

        val items = dao.getAllBookmarks().first()

        assertEquals(2, items.size)
    }

    @Test
    fun `isBookmarked returns true after insert`() = runTest {
        dao.insert(BookmarkEntity(sura = 2, aya = 255, suraName = "Al-Baqarah", insertTime = "2024-01-01"))

        assertTrue(dao.isBookmarked(2, 255))
    }

    @Test
    fun `isBookmarked returns false for non-existent`() = runTest {
        assertFalse(dao.isBookmarked(99, 99))
    }

    @Test
    fun `delete removes bookmark`() = runTest {
        dao.insert(BookmarkEntity(sura = 1, aya = 1, suraName = "Al-Fatiha", insertTime = "2024-01-01"))
        dao.delete(1, 1)

        assertFalse(dao.isBookmarked(1, 1))
    }

    @Test
    fun `duplicate sura and aya insert is ignored by unique index`() = runTest {
        val firstId = dao.insert(
            BookmarkEntity(sura = 2, aya = 255, suraName = "Al-Baqarah", insertTime = "2024-01-01")
        )
        val duplicateId = dao.insert(
            BookmarkEntity(sura = 2, aya = 255, suraName = "Al-Baqarah", insertTime = "2024-01-02")
        )

        assertTrue(firstId > 0L)
        assertEquals(-1L, duplicateId)

        val items = dao.getAllBookmarks().first()
        assertEquals(1, items.size)
        assertEquals("2024-01-01", items.single().insertTime)
    }

    @Test
    fun `MIGRATION_2_3 deduplicates keeping latest id and enforces uniqueness`() = runTest {
        val tempDir = createTempDirectory("bookmark_migration_test").toFile()
        val dbName = "bookmarks.db"
        val dbFile = File(tempDir, dbName)
        val wrappedContext = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String): File = File(tempDir, name)
            override fun openOrCreateDatabase(
                name: String,
                mode: Int,
                factory: SQLiteDatabase.CursorFactory?
            ): SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(File(tempDir, name), factory)

            override fun openOrCreateDatabase(
                name: String,
                mode: Int,
                factory: SQLiteDatabase.CursorFactory?,
                errorHandler: android.database.DatabaseErrorHandler?
            ): SQLiteDatabase = SQLiteDatabase.openDatabase(
                File(tempDir, name).absolutePath,
                factory,
                SQLiteDatabase.CREATE_IF_NECESSARY,
                errorHandler
            )
        }

        try {
            SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { legacyDb ->
                legacyDb.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sura` INTEGER NOT NULL,
                        `suraName` TEXT NOT NULL,
                        `aya` INTEGER NOT NULL,
                        `insertTime` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                legacyDb.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_sura` ON `bookmarks` (`sura`)")
                legacyDb.execSQL(
                    "INSERT INTO bookmarks (id, sura, suraName, aya, insertTime) VALUES (1, 1, 'Al-Fatiha', 1, '2024-01-01')"
                )
                legacyDb.execSQL(
                    "INSERT INTO bookmarks (id, sura, suraName, aya, insertTime) VALUES (2, 2, 'Al-Baqarah', 255, '2024-01-02')"
                )
                legacyDb.execSQL(
                    "INSERT INTO bookmarks (id, sura, suraName, aya, insertTime) VALUES (3, 1, 'Al-Fatiha', 1, '2024-01-03')"
                )
                legacyDb.version = 2
            }

            val migratedDb = Room.databaseBuilder(wrappedContext, BookmarkDatabase::class.java, dbName)
                .addMigrations(BookmarkDatabase.MIGRATION_1_2, BookmarkDatabase.MIGRATION_2_3)
                .allowMainThreadQueries()
                .build()

            try {
                val migratedDao = migratedDb.bookmarkDao()
                val items = migratedDao.getAllBookmarks().first()
                assertEquals(2, items.size)

                val fatiha = items.first { it.sura == 1 && it.aya == 1 }
                assertEquals(3, fatiha.id)
                assertEquals("2024-01-03", fatiha.insertTime)

                val baqarah = items.first { it.sura == 2 && it.aya == 255 }
                assertEquals(2, baqarah.id)

                val duplicateInsert = migratedDao.insert(
                    BookmarkEntity(sura = 1, aya = 1, suraName = "Al-Fatiha", insertTime = "2024-01-04")
                )
                assertEquals(-1L, duplicateInsert)
                assertEquals(2, migratedDao.getAllBookmarks().first().size)
            } finally {
                migratedDb.close()
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
