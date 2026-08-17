package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.Mosque

interface MosqueRepository {
    suspend fun getNearbyMosques(lat: Double, lon: Double, radiusMeters: Int = 5000): Result<List<Mosque>, AppError>
}
