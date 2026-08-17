package com.smiledev.rafiq_quran.domain.repository

import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result

data class GeoLocation(
    val latitude: Double,
    val longitude: Double
)

interface LocationProvider {
    suspend fun getLastLocation(): Result<GeoLocation, AppError>
}