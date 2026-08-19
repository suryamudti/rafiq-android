package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.domain.model.Mosque

interface MosqueRepository {
    suspend fun getNearbyMosques(lat: Double, lon: Double, radiusMeters: Int = 5000): Result<List<Mosque>, AppError>
}
