package com.smiledev.rafiq_quran.data.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.remote.GithubReleaseApiService
import com.smiledev.rafiq_quran.domain.model.AppUpdateInfo
import com.smiledev.rafiq_quran.domain.repository.AppUpdateRepository
import com.smiledev.rafiq_quran.domain.util.VersionComparator
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepositoryImpl @Inject constructor(
    private val apiService: GithubReleaseApiService,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : AppUpdateRepository {

    override suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo, AppError> {
        return withContext(dispatcherProvider.io) {
            try {
                val release = apiService.getLatestRelease()
                val latestTag = release.tagName.trim()
                val isNewer = VersionComparator.isNewer(currentVersion, latestTag)

                val assets = release.assets.orEmpty()
                val universalApk = assets.firstOrNull { it.name.endsWith("-universal.apk", ignoreCase = true) }
                val anyApk = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                val downloadUrl = (universalApk ?: anyApk)?.browserDownloadUrl ?: release.htmlUrl

                val cleanLatest = latestTag.removePrefix("v").removePrefix("V")
                val info = AppUpdateInfo(
                    currentVersion = currentVersion,
                    latestVersion = cleanLatest,
                    isUpdateAvailable = isNewer,
                    releaseName = release.name ?: latestTag,
                    releaseNotes = release.body,
                    releaseUrl = release.htmlUrl,
                    downloadUrl = downloadUrl,
                    publishedAt = release.publishedAt
                )
                Result.Success(info)
            } catch (e: Exception) {
                Result.Error(AppError.Network(e.message ?: "Failed to check for updates", e))
            }
        }
    }
}
