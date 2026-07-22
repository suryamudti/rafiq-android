package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Reciter

interface ReciterRepository {
    fun getReciters(): Result<List<Reciter>, AppError>
}
