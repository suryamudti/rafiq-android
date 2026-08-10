package com.smiledev.rafiq.domain.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result

data class GeoLocation(
    val latitude: Double,
    val longitude: Double
)

interface LocationProvider {
    suspend fun getLastLocation(): Result<GeoLocation, AppError>
}