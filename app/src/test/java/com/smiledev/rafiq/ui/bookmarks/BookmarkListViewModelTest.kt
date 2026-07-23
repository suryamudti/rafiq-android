package com.smiledev.rafiq.ui.bookmarks

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.repository.BookmarkItem
import com.smiledev.rafiq.domain.repository.BookmarkRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val bookmarkRepository: BookmarkRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load bookmarks on init`() = runTest(testDispatcher) {
        val bookmarks = listOf(
            BookmarkItem(id = 1, sura = 1, suraName = "Al-Fatiha", aya = 1, insertTime = "2024-01-01")
        )
        val flow = MutableStateFlow(bookmarks)
        every { bookmarkRepository.observeAll() } returns flow

        val vm = BookmarkListViewModel(bookmarkRepository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.bookmarks.size)
    }

    @Test
    fun `empty state`() = runTest(testDispatcher) {
        val flow = MutableStateFlow(emptyList<BookmarkItem>())
        every { bookmarkRepository.observeAll() } returns flow

        val vm = BookmarkListViewModel(bookmarkRepository, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.bookmarks.size)
    }

    @Test
    fun `delete calls repository`() = runTest(testDispatcher) {
        val flow = MutableStateFlow(emptyList<BookmarkItem>())
        every { bookmarkRepository.observeAll() } returns flow
        coEvery { bookmarkRepository.delete(any(), any()) } returns Result.Success(Unit)

        val vm = BookmarkListViewModel(bookmarkRepository, testDispatcherProvider)
        vm.delete(1, 1)
        advanceUntilIdle()

        coVerify { bookmarkRepository.delete(1, 1) }
    }
}
