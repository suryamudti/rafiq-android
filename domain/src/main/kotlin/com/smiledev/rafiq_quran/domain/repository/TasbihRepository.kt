package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.TasbihItem

interface TasbihRepository {
    fun getTasbihItems(): Result<List<TasbihItem>, AppError>
}
