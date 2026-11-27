package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.local.PrayerLogDao
import com.smiledev.rafiq_quran.data.local.PrayerLogDatabase
import com.smiledev.rafiq_quran.data.local.PrayerLogEntity
import com.smiledev.rafiq_quran.domain.repository.PrayerLogDay
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PrayerLogRepositoryImplTest {

    private val context: Context = mockk(relaxed = true)
    private val database: PrayerLogDatabase = mockk()
    private val dao: PrayerLogDao = mockk()
    private lateinit var repository: PrayerLogRepositoryImpl

    @Before
    fun setUp() {
        mockkObject(PrayerLogDatabase.Companion)
        every { PrayerLogDatabase.getInstance(context) } returns database
        every { database.prayerLogDao() } returns dao
        repository = PrayerLogRepositoryImpl(context)
    }

    @After
    fun tearDown() {
        unmockkObject(PrayerLogDatabase.Companion)
    }

    @Test
    fun `observeAll maps PrayerLogEntity list to domain PrayerLogDay list`() = runTest {
        every { dao.getAllLogs() } returns flowOf(
            listOf(
                PrayerLogEntity("2026-10-02", fajr = true, dhuhr = true, asr = false, maghrib = true, isha = true),
                PrayerLogEntity("2026-10-01", fajr = true, dhuhr = false, asr = false, maghrib = false, isha = false)
            )
        )

        val logs = repository.observeAll().first()

        assertEquals(2, logs.size)
        assertEquals(
            PrayerLogDay("2026-10-02", fajr = true, dhuhr = true, asr = false, maghrib = true, isha = true),
            logs[0]
        )
        assertEquals(
            PrayerLogDay("2026-10-01", fajr = true, dhuhr = false, asr = false, maghrib = false, isha = false),
            logs[1]
        )
    }

    @Test
    fun `getForDate returns mapped PrayerLogDay when present and null when absent`() = runTest {
        coEvery { dao.getLogForDate("2026-10-02") } returns PrayerLogEntity(
            date = "2026-10-02",
            fajr = true,
            dhuhr = true,
            asr = true,
            maghrib = true,
            isha = true
        )
        coEvery { dao.getLogForDate("2026-10-03") } returns null

        val found = repository.getForDate("2026-10-02")
        val missing = repository.getForDate("2026-10-03")

        assertTrue(found is Result.Success)
        assertEquals(
            PrayerLogDay("2026-10-02", fajr = true, dhuhr = true, asr = true, maghrib = true, isha = true),
            (found as Result.Success).data
        )
        assertTrue(missing is Result.Success)
        assertNull((missing as Result.Success).data)
    }

    @Test
    fun `getForDate returns Database error when dao throws`() = runTest {
        coEvery { dao.getLogForDate("2026-10-02") } throws RuntimeException("query error")

        val result = repository.getForDate("2026-10-02")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }

    @Test
    fun `upsert maps PrayerLogDay to entity and delegates to dao`() = runTest {
        val captured = slot<PrayerLogEntity>()
        coEvery { dao.upsert(capture(captured)) } returns Unit

        val day = PrayerLogDay(
            date = "2026-10-05",
            fajr = true,
            dhuhr = false,
            asr = true,
            maghrib = false,
            isha = true
        )

        val result = repository.upsert(day)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dao.upsert(any()) }
        assertEquals(
            PrayerLogEntity("2026-10-05", fajr = true, dhuhr = false, asr = true, maghrib = false, isha = true),
            captured.captured
        )
    }

    @Test
    fun `upsert returns Database error when dao throws`() = runTest {
        coEvery { dao.upsert(any()) } throws RuntimeException("write error")

        val result = repository.upsert(PrayerLogDay(date = "2026-10-05"))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Database)
    }
}
