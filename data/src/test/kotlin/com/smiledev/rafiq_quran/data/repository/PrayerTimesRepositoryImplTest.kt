package com.smiledev.rafiq_quran.data.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.CachedPrayerTimes
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.data.remote.AladhanApi
import com.smiledev.rafiq_quran.data.remote.AladhanDateDto
import com.smiledev.rafiq_quran.data.remote.HijriDateDto
import com.smiledev.rafiq_quran.data.remote.HijriMonthDto
import com.smiledev.rafiq_quran.data.remote.PrayerTimesData
import com.smiledev.rafiq_quran.data.remote.PrayerTimesResponse
import com.smiledev.rafiq_quran.data.remote.PrayerTimings
import com.smiledev.rafiq_quran.domain.model.PrayerTimings as DomainTimings
import com.smiledev.rafiq_quran.domain.model.PrayerTimesData as DomainPrayerTimesData
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

    private lateinit var aladhanApi: AladhanApi
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repo: PrayerTimesRepositoryImpl

    @Before
    fun setup() {
        aladhanApi = mockk()
        preferencesManager = mockk(relaxed = true)
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
                date = AladhanDateDto(
                    hijri = HijriDateDto(
                        day = "1",
                        month = HijriMonthDto(en = "Muharram"),
                        year = "1446"
                    )
                )
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
            data = PrayerTimesData(
                timings = PrayerTimings("", "", "", "", "", "", ""),
                date = null
            )
        )
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } returns response
        coEvery { preferencesManager.getCachedPrayerTimes() } returns null

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "2024-01-01", 20)

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
    }

    @Test
    fun `api exception returns Network error`() = runTest {
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } throws RuntimeException("Network down")
        coEvery { preferencesManager.getCachedPrayerTimes() } returns null

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "2024-01-01", 20)

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
    }

    @Test
    fun `network error falls back to cache if date matches`() = runTest {
        val cachedDomainData = DomainPrayerTimesData(
            timings = DomainTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50"),
            hijriDate = "1 Muharram 1446"
        )
        val cached = CachedPrayerTimes(
            date = "2024-01-01",
            latitude = -6.2088,
            longitude = 106.8456,
            calculationMethod = 20,
            data = cachedDomainData
        )
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } throws RuntimeException("Network down")
        coEvery { preferencesManager.getCachedPrayerTimes() } returns cached

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "2024-01-01", 20)

        assertTrue(result is Result.Success)
        assertEquals("04:40", (result as Result.Success).data.timings.fajr)
    }

    @Test
    fun `network error does not fallback to cache if date differs`() = runTest {
        val cachedDomainData = DomainPrayerTimesData(
            timings = DomainTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50"),
            hijriDate = "1 Muharram 1446"
        )
        val cached = CachedPrayerTimes(
            date = "2024-01-02",
            latitude = -6.2088,
            longitude = 106.8456,
            calculationMethod = 20,
            data = cachedDomainData
        )
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } throws RuntimeException("Network down")
        coEvery { preferencesManager.getCachedPrayerTimes() } returns cached

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "2024-01-01", 20)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `successful fetch saves to cache`() = runTest {
        val timings = PrayerTimings(
            Imsak = "04:30", Fajr = "04:40", Sunrise = "05:50",
            Dhuhr = "11:45", Asr = "15:00", Maghrib = "17:40", Isha = "18:50"
        )
        val response = PrayerTimesResponse(
            code = 200,
            status = "OK",
            data = PrayerTimesData(timings = timings, date = null)
        )
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } returns response

        repo.fetchPrayerTimes(-6.2088, 106.8456, "2024-01-01", 20)

        coVerify { preferencesManager.saveCachedPrayerTimes("2024-01-01", -6.2088, 106.8456, 20, any()) }
    }

    @Test
    fun `cached data returned without network call when valid`() = runTest {
        val cachedDomainData = DomainPrayerTimesData(
            timings = DomainTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50"),
            hijriDate = "1 Muharram 1446"
        )
        val cached = CachedPrayerTimes(
            date = "2024-01-01",
            latitude = -6.2088,
            longitude = 106.8456,
            calculationMethod = 20,
            data = cachedDomainData
        )
        coEvery { preferencesManager.getCachedPrayerTimes() } returns cached

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "2024-01-01", 20, forceRefresh = false)

        assertTrue(result is Result.Success)
        assertEquals("04:40", (result as Result.Success).data.timings.fajr)
        coVerify(exactly = 0) { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) }
    }

    @Test
    fun `forceRefresh bypasses cache`() = runTest {
        val cachedDomainData = DomainPrayerTimesData(
            timings = DomainTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50"),
            hijriDate = "1 Muharram 1446"
        )
        val cached = CachedPrayerTimes(
            date = "2024-01-01",
            latitude = -6.2088,
            longitude = 106.8456,
            calculationMethod = 20,
            data = cachedDomainData
        )
        val timings = PrayerTimings(
            Imsak = "04:30", Fajr = "04:40", Sunrise = "05:50",
            Dhuhr = "11:45", Asr = "15:00", Maghrib = "17:40", Isha = "18:50"
        )
        val response = PrayerTimesResponse(
            code = 200,
            status = "OK",
            data = PrayerTimesData(timings = timings, date = null)
        )
        coEvery { preferencesManager.getCachedPrayerTimes() } returns cached
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } returns response

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "2024-01-01", 20, forceRefresh = true)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) }
    }

    @Test
    fun `getCachedPrayerTimes returns null when location differs significantly`() = runTest {
        val cachedDomainData = DomainPrayerTimesData(
            timings = DomainTimings("04:30", "04:40", "05:50", "11:45", "15:00", "17:40", "18:50"),
            hijriDate = "1 Muharram 1446"
        )
        val cached = CachedPrayerTimes(
            date = "2024-01-01",
            latitude = -6.2088,
            longitude = 106.8456,
            calculationMethod = 20,
            data = cachedDomainData
        )
        coEvery { preferencesManager.getCachedPrayerTimes() } returns cached

        val result = repo.getCachedPrayerTimes(0.0, 0.0, "2024-01-01", 20)

        assertNull(result)
    }

    @Test
    fun `extractHijriDate handles null date gracefully`() = runTest {
        val timings = PrayerTimings(
            Imsak = "04:30", Fajr = "04:40", Sunrise = "05:50",
            Dhuhr = "11:45", Asr = "15:00", Maghrib = "17:40", Isha = "18:50"
        )
        val response = PrayerTimesResponse(
            code = 200,
            status = "OK",
            data = PrayerTimesData(timings = timings, date = null)
        )
        coEvery { aladhanApi.fetchPrayerTimes(any(), any(), any(), any()) } returns response

        val result = repo.fetchPrayerTimes(-6.2088, 106.8456, "2024-01-01", 20)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("", data.hijriDate)
    }
}
