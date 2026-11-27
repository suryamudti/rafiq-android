package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.local.HifzDao
import com.smiledev.rafiq_quran.data.local.HifzDatabase
import com.smiledev.rafiq_quran.data.local.HifzEntity
import com.smiledev.rafiq_quran.domain.repository.HifzGoal
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HifzRepositoryImplTest {

    private val context: Context = mockk(relaxed = true)
    private val database: HifzDatabase = mockk()
    private val dao: HifzDao = mockk()
    private lateinit var repository: HifzRepositoryImpl

    @Before
    fun setUp() {
        mockkObject(HifzDatabase.Companion)
        every { HifzDatabase.getInstance(context) } returns database
        every { database.hifzDao() } returns dao
        repository = HifzRepositoryImpl(context)
    }

    @After
    fun tearDown() {
        unmockkObject(HifzDatabase.Companion)
    }

    @Test
    fun `observeAll maps HifzEntity list and parses memorizedAyahsCsv`() = runTest {
        every { dao.getAllGoals() } returns flowOf(
            listOf(
                HifzEntity(
                    id = 10L,
                    suraNumber = 67,
                    suraName = "Al-Mulk",
                    ayaStart = 1,
                    ayaEnd = 30,
                    memorizedAyahsCsv = "1,2,3,bad,5"
                ),
                HifzEntity(
                    id = 11L,
                    suraNumber = 112,
                    suraName = "Al-Ikhlas",
                    ayaStart = 1,
                    ayaEnd = 4,
                    memorizedAyahsCsv = ""
                )
            )
        )

        val goals = repository.observeAll().first()

        assertEquals(2, goals.size)
        assertEquals(10L, goals[0].id)
        assertEquals(67, goals[0].suraNumber)
        assertEquals("Al-Mulk", goals[0].suraName)
        assertEquals(setOf(1, 2, 3, 5), goals[0].memorizedAyahs)
        assertTrue(goals[1].memorizedAyahs.isEmpty())
    }

    @Test
    fun `upsert serializes memorizedAyahs sorted as csv and delegates to dao`() = runTest {
        val captured = slot<HifzEntity>()
        coEvery { dao.upsert(capture(captured)) } returns 7L

        val goal = HifzGoal(
            id = 7L,
            suraNumber = 78,
            suraName = "An-Naba",
            ayaStart = 1,
            ayaEnd = 40,
            memorizedAyahs = setOf(5, 1, 3)
        )

        val result = repository.upsert(goal)

        assertTrue(result is Result.Success)
        assertEquals(7L, captured.captured.id)
        assertEquals(78, captured.captured.suraNumber)
        assertEquals("An-Naba", captured.captured.suraName)
        assertEquals("1,3,5", captured.captured.memorizedAyahsCsv)
    }

    @Test
    fun `upsert returns Database error when dao throws`() = runTest {
        coEvery { dao.upsert(any()) } throws RuntimeException("insert error")

        val result = repository.upsert(
            HifzGoal(id = 1L, suraNumber = 1, suraName = "Al-Fatihah", ayaStart = 1, ayaEnd = 7)
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }

    @Test
    fun `delete removes goal by id and handles dao exceptions`() = runTest {
        coEvery { dao.deleteById(42L) } returns Unit

        val ok = repository.delete(42L)
        assertTrue(ok is Result.Success)
        coVerify(exactly = 1) { dao.deleteById(42L) }

        coEvery { dao.deleteById(99L) } throws RuntimeException("delete error")
        val err = repository.delete(99L)
        assertTrue(err is Result.Error)
        assertTrue((err as Result.Error).error is AppError.Database)
    }

    @Test
    fun `entity and domain mapping round-trips cleanly`() {
        val goal = HifzGoal(
            id = 3L,
            suraNumber = 114,
            suraName = "An-Nas",
            ayaStart = 1,
            ayaEnd = 6,
            memorizedAyahs = setOf(6, 2, 4)
        )

        val entity = goal.toEntity()
        assertEquals("2,4,6", entity.memorizedAyahsCsv)
        assertEquals(goal, entity.toDomain())
    }
}
