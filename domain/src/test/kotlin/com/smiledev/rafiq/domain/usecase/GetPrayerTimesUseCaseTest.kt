package com.smiledev.rafiq.domain.usecase

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.PrayerTimings
import com.smiledev.rafiq.domain.model.PrayerTimesData
import com.smiledev.rafiq.domain.repository.PrayerTimesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPrayerTimesUseCaseTest {

    private val repository: PrayerTimesRepository = mockk()
    private val useCase = GetPrayerTimesUseCase(repository)

    @Test
    fun `invoke delegates to repository with correct params`() = runTest {
        val timings = PrayerTimings("04:30", "05:00", "06:00", "12:00", "15:30", "18:00", "19:00")
        val data = PrayerTimesData(timings, "1 Ramadan 1445")
        coEvery { repository.fetchPrayerTimes(-6.2, 106.8, "2024-01-01", 20) } returns Result.Success(data)

        val result = useCase(-6.2, 106.8, "2024-01-01")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.fetchPrayerTimes(-6.2, 106.8, "2024-01-01", 20) }
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest {
        coEvery { repository.fetchPrayerTimes(any(), any(), any(), any()) } returns Result.Error(AppError.Network("error"))

        val result = useCase(0.0, 0.0, "2024-01-01")

        assertTrue(result is Result.Error)
    }
}
