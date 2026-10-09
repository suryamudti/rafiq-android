package com.smiledev.rafiq_quran.data.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.remote.GithubAssetDto
import com.smiledev.rafiq_quran.data.remote.GithubReleaseApiService
import com.smiledev.rafiq_quran.data.remote.GithubReleaseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateRepositoryImplTest {

    private val apiService: GithubReleaseApiService = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private val repository = AppUpdateRepositoryImpl(apiService, dispatcherProvider)

    @Test
    fun checkForUpdate_whenNewerReleaseFound_returnsUpdateAvailableTrue() = runTest(testDispatcher) {
        val releaseDto = GithubReleaseDto(
            tagName = "v1.0.74",
            name = "Release v1.0.74",
            body = "Bug fixes and improvements",
            htmlUrl = "https://github.com/suryamudti/rafiq-android/releases/tag/v1.0.74",
            publishedAt = "2026-10-09T12:37:23Z",
            assets = listOf(
                GithubAssetDto(
                    name = "rafiq-v1.0.74-universal.apk",
                    browserDownloadUrl = "https://github.com/suryamudti/rafiq-android/releases/download/v1.0.74/rafiq-v1.0.74-universal.apk"
                ),
                GithubAssetDto(
                    name = "rafiq-v1.0.74-arm64-v8a.apk",
                    browserDownloadUrl = "https://github.com/suryamudti/rafiq-android/releases/download/v1.0.74/rafiq-v1.0.74-arm64-v8a.apk"
                )
            )
        )
        coEvery { apiService.getLatestRelease() } returns releaseDto

        val result = repository.checkForUpdate("1.0.73")

        assertTrue(result is Result.Success)
        val info = (result as Result.Success).data
        assertTrue(info.isUpdateAvailable)
        assertEquals("1.0.74", info.latestVersion)
        assertEquals("Release v1.0.74", info.releaseName)
        assertEquals("Bug fixes and improvements", info.releaseNotes)
        assertEquals("https://github.com/suryamudti/rafiq-android/releases/download/v1.0.74/rafiq-v1.0.74-universal.apk", info.downloadUrl)
    }

    @Test
    fun checkForUpdate_whenSameVersion_returnsUpdateAvailableFalse() = runTest(testDispatcher) {
        val releaseDto = GithubReleaseDto(
            tagName = "v1.0.74",
            name = "Release v1.0.74",
            body = null,
            htmlUrl = "https://github.com/suryamudti/rafiq-android/releases/tag/v1.0.74",
            assets = emptyList()
        )
        coEvery { apiService.getLatestRelease() } returns releaseDto

        val result = repository.checkForUpdate("1.0.74")

        assertTrue(result is Result.Success)
        val info = (result as Result.Success).data
        assertFalse(info.isUpdateAvailable)
        assertEquals("1.0.74", info.latestVersion)
        assertEquals("https://github.com/suryamudti/rafiq-android/releases/tag/v1.0.74", info.downloadUrl)
    }

    @Test
    fun checkForUpdate_whenApiThrows_returnsError() = runTest(testDispatcher) {
        coEvery { apiService.getLatestRelease() } throws RuntimeException("Network timeout")

        val result = repository.checkForUpdate("1.0")

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
    }
}
