package com.smiledev.rafiq_quran.data.local

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HifzDaoTest {

    private lateinit var db: HifzDatabase
    private lateinit var dao: HifzDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), HifzDatabase::class.java).build()
        dao = db.hifzDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `upsert and getAllGoals flow`() = runTest {
        dao.upsert(HifzEntity(suraNumber = 67, suraName = "Al-Mulk", ayaStart = 1, ayaEnd = 12))
        dao.upsert(HifzEntity(suraNumber = 112, suraName = "Al-Ikhlas", ayaStart = 1, ayaEnd = 4))

        val goals = dao.getAllGoals().first()

        assertEquals(2, goals.size)
    }

    @Test
    fun `memorized set survives csv round trip`() = runTest {
        dao.upsert(
            HifzEntity(
                suraNumber = 112, suraName = "Al-Ikhlas", ayaStart = 1, ayaEnd = 4,
                memorizedAyahsCsv = "1,3"
            )
        )

        val goals = dao.getAllGoals().first()

        assertEquals("1,3", goals.first().memorizedAyahsCsv)
    }

    @Test
    fun `deleteById removes the goal`() = runTest {
        val id = dao.upsert(HifzEntity(suraNumber = 67, suraName = "Al-Mulk", ayaStart = 1, ayaEnd = 3))
        dao.deleteById(id)

        val goals = dao.getAllGoals().first()

        assertTrue(goals.isEmpty())
    }
}
