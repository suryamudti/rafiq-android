package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.remote.AladhanApi
import com.smiledev.rafiq.data.remote.PrayerTimesData
import com.smiledev.rafiq.data.remote.PrayerTimesResponse
import com.smiledev.rafiq.data.remote.PrayerTimings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import com.smiledev.rafiq.core.getOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PrayerTimesRepositoryImplTest {

    private val aladhanApi: AladhanApi = mockk()
    private lateinit var repo: PrayerTimesRepositoryImpl

    @Before
    fun setUp() {
        repo = PrayerTimesRepositoryImpl(aladhanApi)
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
}
