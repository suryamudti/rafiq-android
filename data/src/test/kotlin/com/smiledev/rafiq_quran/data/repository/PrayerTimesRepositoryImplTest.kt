package com.smiledev.rafiq_quran.data.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.CachedPrayerTimes
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.data.remote.AladhanApi
import com.smiledev.rafiq_quran.data.remote.PrayerTimesData
import com.smiledev.rafiq_quran.data.remote.PrayerTimesResponse
import com.smiledev.rafiq_quran.data.remote.PrayerTimings
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData as DomainPrayerTimesData
import com.smiledev.rafiq_quran.domain.model.PrayerTimings as DomainPrayerTimings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PrayerTimesRepositoryImplTest {

    private val aladhanApi: AladhanApi = mockk()
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private lateinit var repo: PrayerTimesRepositoryImpl

    @Before
    fun setUp() {
        repo = PrayerTimesRepositoryImpl(aladhanApi, preferencesManager)
    }

    @Test
    fun `successful response returns PrayerTimesData`() = runTest {
        val timings = PrayerTimings(
            Imsak = "04:30", Fajr = "04:40", Sunrise = "05:50",
            Dhuhr = "11:45", Asr = "15:00", Maghrib = "17:40", Isha = "18:50"
        )
        val response = PrayerTimesResponse(
            code = 200,
            status = "OK",
            data = PrayerTimesData(
                timings = timings,
                date = mapOf("hijri" to mapOf("day" to "1", "month" to mapOf("en" to "Muharram"), "year" to "1446"))
            )
        )
        coEvery { aladhanApi.fetchPrayerTimes(-6.2088, 106.8456, "2024-01-01", 20) } returns response

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "2024-01-01", 20)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("04:40", data.timings.fajr)
        assertEquals("1 Muharram 1446", data.hijriDate)
    }

    @Test
    fun `non-200 response returns Network error`() = runTest {
        val response = PrayerTimesResponse(
            code = 500,
            status = "Internal Server Error",
            data = PrayerTimesData(timings = PrayerTimings("","","","","","",""), date = null)
        )
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } returns response

        val result = repo.fetchPrayerTimes(0.0, 0.0, "2024-01-01", 20)

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
        if (error is AppError.Network) assertTrue(error.message.contains("API error"))
    }

    @Test
    fun `network exception returns Network error`() = runTest {
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } throws RuntimeException("Connection refused")

        val result = repo.fetchPrayerTimes(0.0, 0.0, "2024-01-01", 20)

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
    }

    @Test
    fun `hijri date extraction handles null date gracefully`() = runTest {
        val timings = PrayerTimings("04:30","04:40","05:50","11:45","15:00","17:40","18:50")
        val response = PrayerTimesResponse(
            code = 200, status = "OK",
            data = PrayerTimesData(timings = timings, date = null)
        )
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } returns response

        val result = repo.fetchPrayerTimes(0.0, 0.0, "2024-01-01", 20)

        val data = (result as Result.Success).data
        assertEquals("", data.hijriDate)
    }

    @Test
    fun `successful network fetch saves to cache`() = runTest {
        val timings = PrayerTimings("04:30","04:40","05:50","11:45","15:00","17:40","18:50")
        val response = PrayerTimesResponse(
            code = 200, status = "OK",
            data = PrayerTimesData(timings = timings, date = null)
        )
        coEvery { aladhanApi.fetchPrayerTimes(-6.2, 106.8, "12-09-2026", 20) } returns response
        coEvery { preferencesManager.getCachedPrayerTimes() } returns null

        val result = repo.fetchPrayerTimes(-6.2, 106.8, "12-09-2026", 20)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) {
            preferencesManager.saveCachedPrayerTimes(
                date = "12-09-2026",
                lat = -6.2,
                lon = 106.8,
                method = 20,
                data = any()
            )
        }
    }

    @Test
    fun `valid cache returns cached data without calling api`() = runTest {
        val domainData = DomainPrayerTimesData(
            timings = DomainPrayerTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50"),
            hijriDate = "14 Ramadan 1447"
        )
        val cached = CachedPrayerTimes(
            date = "12-09-2026",
            latitude = -6.2088,
            longitude = 106.8456,
            calculationMethod = 20,
            data = domainData
        )
        coEvery { preferencesManager.getCachedPrayerTimes() } returns cached

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "12-09-2026", 20, forceRefresh = false)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("04:40", data.timings.fajr)
        assertEquals("14 Ramadan 1447", data.hijriDate)
        coVerify(exactly = 0) { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) }
    }

    @Test
    fun `forceRefresh true bypasses cache and calls api`() = runTest {
        val domainData = DomainPrayerTimesData(
            timings = DomainPrayerTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50"),
            hijriDate = "14 Ramadan 1447"
        )
        val cached = CachedPrayerTimes(
            date = "12-09-2026",
            latitude = -6.2088,
            longitude = 106.8456,
            calculationMethod = 20,
            data = domainData
        )
        coEvery { preferencesManager.getCachedPrayerTimes() } returns cached

        val freshTimings = PrayerTimings("04:29", "04:39", "05:49", "11:44", "14:59", "17:39", "18:49")
        val response = PrayerTimesResponse(
            code = 200, status = "OK",
            data = PrayerTimesData(timings = freshTimings, date = null)
        )
        coEvery { aladhanApi.fetchPrayerTimes(-6.2088, 106.8456, "12-09-2026", 20) } returns response

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "12-09-2026", 20, forceRefresh = true)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("04:39", data.timings.fajr)
        coVerify(exactly = 1) { aladhanApi.fetchPrayerTimes(-6.2088, 106.8456, "12-09-2026", 20) }
    }

    @Test
    fun `cache for yesterday triggers network call for today`() = runTest {
        val domainData = DomainPrayerTimesData(
            timings = DomainPrayerTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50"),
            hijriDate = "13 Ramadan 1447"
        )
        val yesterdayCache = CachedPrayerTimes(
            date = "11-09-2026",
            latitude = -6.2088,
            longitude = 106.8456,
            calculationMethod = 20,
            data = domainData
        )
        coEvery { preferencesManager.getCachedPrayerTimes() } returns yesterdayCache

        val todayTimings = PrayerTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50")
        val response = PrayerTimesResponse(
            code = 200, status = "OK",
            data = PrayerTimesData(timings = todayTimings, date = null)
        )
        coEvery { aladhanApi.fetchPrayerTimes(-6.2088, 106.8456, "12-09-2026", 20) } returns response

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "12-09-2026", 20, forceRefresh = false)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { aladhanApi.fetchPrayerTimes(-6.2088, 106.8456, "12-09-2026", 20) }
    }

    @Test
    fun `network failure falls back to cached data if date matches`() = runTest {
        val domainData = DomainPrayerTimesData(
            timings = DomainPrayerTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50"),
            hijriDate = "14 Ramadan 1447"
        )
        val todayCache = CachedPrayerTimes(
            date = "12-09-2026",
            latitude = -6.2088,
            longitude = 106.8456,
            calculationMethod = 20,
            data = domainData
        )
        // Simulate forceRefresh or location drift where cache was bypassed initially
        coEvery { preferencesManager.getCachedPrayerTimes() } returns todayCache
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } throws RuntimeException("Offline")

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "12-09-2026", 20, forceRefresh = true)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("04:40", data.timings.fajr)
    }

    @Test
    fun `getCachedPrayerTimes returns data when valid and null when coordinates or method change`() = runTest {
        val domainData = DomainPrayerTimesData(
            timings = DomainPrayerTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50")
        )
        val cached = CachedPrayerTimes(
            date = "12-09-2026",
            latitude = -6.2088,
            longitude = 106.8456,
            calculationMethod = 20,
            data = domainData
        )
        coEvery { preferencesManager.getCachedPrayerTimes() } returns cached

        // Valid
        val valid = repo.getCachedPrayerTimes(-6.2088, 106.8456, "12-09-2026", 20)
        assertEquals(domainData, valid)

        // Wrong date
        val wrongDate = repo.getCachedPrayerTimes(-6.2088, 106.8456, "13-09-2026", 20)
        assertNull(wrongDate)

        // Changed location
        val changedLocation = repo.getCachedPrayerTimes(-6.9175, 107.6191, "12-09-2026", 20)
        assertNull(changedLocation)

        // Changed calculation method
        val changedMethod = repo.getCachedPrayerTimes(-6.2088, 106.8456, "12-09-2026", 3)
        assertNull(changedMethod)
    }
}
