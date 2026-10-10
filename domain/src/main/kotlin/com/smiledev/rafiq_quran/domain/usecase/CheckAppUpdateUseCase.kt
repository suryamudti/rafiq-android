package com.smiledev.rafiq_quran.domain.usecase

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.AppUpdateInfo
import com.smiledev.rafiq_quran.domain.repository.AppUpdateRepository

class CheckAppUpdateUseCase(
    private val repository: AppUpdateRepository
) {
    suspend operator fun invoke(currentVersion: String): Result<AppUpdateInfo, AppError> {
        return repository.checkForUpdate(currentVersion)
    }
}
