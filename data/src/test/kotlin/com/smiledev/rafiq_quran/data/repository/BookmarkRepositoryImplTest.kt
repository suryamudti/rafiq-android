package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.local.BookmarkDao
import com.smiledev.rafiq_quran.data.local.BookmarkDatabase
import com.smiledev.rafiq_quran.data.local.BookmarkEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookmarkRepositoryImplTest {

    private val context: Context = mockk(relaxed = true)
    private val database: BookmarkDatabase = mockk()
    private val dao: BookmarkDao = mockk()
    private lateinit var repository: BookmarkRepositoryImpl

    @Before
    fun setUp() {
        mockkObject(BookmarkDatabase.Companion)
        every { BookmarkDatabase.getInstance(context) } returns database
        every { database.bookmarkDao() } returns dao
        repository = BookmarkRepositoryImpl(context)
    }

    @After
    fun tearDown() {
        unmockkObject(BookmarkDatabase.Companion)
    }

    @Test
    fun `observeAll maps BookmarkEntity list to domain BookmarkItem list`() = runTest {
        every { dao.getAllBookmarks() } returns flowOf(
            listOf(
                BookmarkEntity(id = 2, sura = 2, suraName = "Al-Baqarah", aya = 255, insertTime = "2026-10-01"),
                BookmarkEntity(id = 1, sura = 1, suraName = "Al-Fatihah", aya = 1, insertTime = "2026-09-30")
            )
        )

        val items = repository.observeAll().first()

        assertEquals(2, items.size)
        assertEquals(2, items[0].id)
        assertEquals(2, items[0].sura)
        assertEquals("Al-Baqarah", items[0].suraName)
        assertEquals(255, items[0].aya)
        assertEquals("2026-10-01", items[0].insertTime)
        assertEquals("Al-Fatihah", items[1].suraName)
    }

    @Test
    fun `isBookmarked returns Success with dao result`() = runTest {
        coEvery { dao.isBookmarked(18, 10) } returns true
        coEvery { dao.isBookmarked(18, 11) } returns false

        val bookmarked = repository.isBookmarked(18, 10)
        val notBookmarked = repository.isBookmarked(18, 11)

        assertTrue(bookmarked is Result.Success && bookmarked.data)
        assertTrue(notBookmarked is Result.Success && !notBookmarked.data)
    }

    @Test
    fun `isBookmarked returns Database error on dao exception`() = runTest {
        coEvery { dao.isBookmarked(1, 1) } throws RuntimeException("DB error")

        val result = repository.isBookmarked(1, 1)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }

    @Test
    fun `toggle inserts bookmark when not yet bookmarked`() = runTest {
        coEvery { dao.isBookmarked(36, 1) } returns false
        val captured = slot<BookmarkEntity>()
        coEvery { dao.insert(capture(captured)) } returns 1L

        val result = repository.toggle(36, 1, "Ya-Sin")

        assertTrue(result is Result.Success)
        assertEquals(36, captured.captured.sura)
        assertEquals(1, captured.captured.aya)
        assertEquals("Ya-Sin", captured.captured.suraName)
        coVerify(exactly = 0) { dao.delete(any(), any()) }
    }

    @Test
    fun `toggle deletes bookmark when already bookmarked`() = runTest {
        coEvery { dao.isBookmarked(36, 1) } returns true
        coEvery { dao.delete(36, 1) } returns 1

        val result = repository.toggle(36, 1, "Ya-Sin")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dao.delete(36, 1) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `toggle returns Database error when dao throws`() = runTest {
        coEvery { dao.isBookmarked(36, 1) } throws IllegalStateException("closed")

        val result = repository.toggle(36, 1, "Ya-Sin")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }

    @Test
    fun `delete removes bookmark and handles dao exception`() = runTest {
        coEvery { dao.delete(2, 255) } returns 1
        val ok = repository.delete(2, 255)
        assertTrue(ok is Result.Success)
        coVerify(exactly = 1) { dao.delete(2, 255) }

        coEvery { dao.delete(2, 256) } throws RuntimeException("delete failed")
        val err = repository.delete(2, 256)
        assertFalse(err is Result.Success)
        assertTrue((err as Result.Error).error is AppError.Database)
    }
}
