package com.smiledev.rafiq.data.local

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PrayerLogDaoTest {

    private lateinit var db: PrayerLogDatabase
    private lateinit var dao: PrayerLogDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PrayerLogDatabase::class.java).build()
        dao = db.prayerLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `upsert and getAllLogs flow`() = runTest {
        dao.upsert(PrayerLogEntity(date = "2024-01-01", fajr = true))
        dao.upsert(PrayerLogEntity(date = "2024-01-02", dhuhr = true))

        val logs = dao.getAllLogs().first()

        assertEquals(2, logs.size)
    }

    @Test
    fun `getLogForDate returns correct entry`() = runTest {
        dao.upsert(PrayerLogEntity(date = "2024-01-01", fajr = true, maghrib = true))

        val log = dao.getLogForDate("2024-01-01")

        assertNotNull(log)
        assertEquals("2024-01-01", log?.date)
    }

    @Test
    fun `getLogForDate returns null for missing date`() = runTest {
        val log = dao.getLogForDate("2099-12-31")

        assertNull(log)
    }
}
