package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.AppUpdateInfo
import com.smiledev.rafiq_quran.domain.repository.AppUpdateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckAppUpdateUseCaseTest {

    private val repository: AppUpdateRepository = mockk()
    private val useCase = CheckAppUpdateUseCase(repository)

    @Test
    fun invoke_delegatesToRepository() = runTest {
        val expectedInfo = AppUpdateInfo(
            currentVersion = "1.0",
            latestVersion = "1.0.74",
            isUpdateAvailable = true,
            releaseName = "Release v1.0.74",
            releaseNotes = "Bug fixes",
            releaseUrl = "https://github.com/suryamudti/rafiq-android/releases/tag/v1.0.74",
            downloadUrl = "https://github.com/suryamudti/rafiq-android/releases/download/v1.0.74/rafiq-v1.0.74-universal.apk"
        )
        coEvery { repository.checkForUpdate("1.0") } returns Result.Success(expectedInfo)

        val result = useCase("1.0")

        assertTrue(result is Result.Success)
        assertEquals(expectedInfo, (result as Result.Success).data)
        coVerify(exactly = 1) { repository.checkForUpdate("1.0") }
    }

    @Test
    fun invoke_whenRepositoryFails_returnsError() = runTest {
        val error = AppError.Network("Network down")
        coEvery { repository.checkForUpdate("1.0") } returns Result.Error(error)

        val result = useCase("1.0")

        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
    }
}
