package com.smiledev.rafiq.data.local

import androidx.room.Room
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
}
