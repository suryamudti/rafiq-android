package com.smiledev.rafiq_quran.data.repository

import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.local.TasbihDao
import com.smiledev.rafiq_quran.data.local.TasbihRecordEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TasbihHistoryRepositoryImplTest {

    private val tasbihDao: TasbihDao = mockk()
    private lateinit var repository: TasbihHistoryRepositoryImpl

    @Before
    fun setup() {
        repository = TasbihHistoryRepositoryImpl(tasbihDao)
    }

    @Test
    fun `addOrUpdateCount inserts new record when none exists`() = runTest {
        coEvery { tasbihDao.getRecord("2026-09-22", 1, "Subhanallah") } returns null
        val slot = slot<TasbihRecordEntity>()
        coEvery { tasbihDao.upsert(capture(slot)) } returns 1L

        val result = repository.addOrUpdateCount(
            date = "2026-09-22",
            dhikrId = 1,
            dhikrName = "Subhanallah",
            arabic = "سُبْحَانَ اللّهُ",
            delta = 1
        )

        assertTrue(result is Result.Success)
        assertEquals("2026-09-22", slot.captured.date)
        assertEquals(1, slot.captured.dhikrId)
        assertEquals("Subhanallah", slot.captured.dhikrName)
        assertEquals(1, slot.captured.count)
    }

    @Test
    fun `addOrUpdateCount updates count when record exists`() = runTest {
        val existing = TasbihRecordEntity(
            id = 5,
            date = "2026-09-22",
            dhikrId = 1,
            dhikrName = "Subhanallah",
            arabic = "سُبْحَانَ اللّهُ",
            count = 33
        )
        coEvery { tasbihDao.getRecord("2026-09-22", 1, "Subhanallah") } returns existing
        val slot = slot<TasbihRecordEntity>()
        coEvery { tasbihDao.upsert(capture(slot)) } returns 5L

        val result = repository.addOrUpdateCount(
            date = "2026-09-22",
            dhikrId = 1,
            dhikrName = "Subhanallah",
            arabic = "سُبْحَانَ اللّهُ",
            delta = 1
        )

        assertTrue(result is Result.Success)
        assertEquals(34, slot.captured.count)
    }

    @Test
    fun `addOrUpdateCount with negative delta decreases count but not below zero`() = runTest {
        val existing = TasbihRecordEntity(
            id = 5,
            date = "2026-09-22",
            dhikrId = 1,
            dhikrName = "Subhanallah",
            arabic = "سُبْحَانَ اللّهُ",
            count = 0
        )
        coEvery { tasbihDao.getRecord("2026-09-22", 1, "Subhanallah") } returns existing
        val slot = slot<TasbihRecordEntity>()
        coEvery { tasbihDao.upsert(capture(slot)) } returns 5L

        val result = repository.addOrUpdateCount(
            date = "2026-09-22",
            dhikrId = 1,
            dhikrName = "Subhanallah",
            arabic = "سُبْحَانَ اللّهُ",
            delta = -1
        )

        assertTrue(result is Result.Success)
        assertEquals(0, slot.captured.count)
    }

    @Test
    fun `observeAllRecords maps entity list to domain items`() = runTest {
        val entities = listOf(
            TasbihRecordEntity(1, "2026-09-22", 1, "Subhanallah", "سُبْحَانَ اللّهُ", 33),
            TasbihRecordEntity(2, "2026-09-22", 2, "Alhamdulillah", "ٱلْحَمْدُ لِلَّهِ", 33)
        )
        every { tasbihDao.observeAll() } returns flowOf(entities)

        val items = repository.observeAllRecords().first()
        assertEquals(2, items.size)
        assertEquals("Subhanallah", items[0].dhikrName)
        assertEquals(33, items[0].count)
        assertEquals("Alhamdulillah", items[1].dhikrName)
    }

    @Test
    fun `deleteDay calls dao deleteByDate`() = runTest {
        coEvery { tasbihDao.deleteByDate("2026-09-22") } returns 2
        val result = repository.deleteDay("2026-09-22")
        assertTrue(result is Result.Success)
        coVerify { tasbihDao.deleteByDate("2026-09-22") }
    }

    @Test
    fun `clearAll calls dao clearAll`() = runTest {
        coEvery { tasbihDao.clearAll() } returns 10
        val result = repository.clearAll()
        assertTrue(result is Result.Success)
        coVerify { tasbihDao.clearAll() }
    }
}
