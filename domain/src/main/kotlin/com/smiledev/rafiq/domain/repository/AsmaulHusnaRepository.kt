package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.AsmaulHusna

interface AsmaulHusnaRepository {
    fun getNames(): Result<List<AsmaulHusna>, AppError>
}
