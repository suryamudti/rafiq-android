package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.AsmaulHusna

interface AsmaulHusnaRepository {
    fun getNames(): Result<List<AsmaulHusna>, AppError>
}
