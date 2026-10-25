package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.Reciter

interface ReciterRepository {
    fun getReciters(): Result<List<Reciter>, AppError>
}
