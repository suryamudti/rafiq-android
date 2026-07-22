package com.smiledev.rafiq.domain.usecase

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val KAABA_LAT = 21.4225
private const val KAABA_LON = 39.8262

data class QiblaResult(
    val bearing: Int,
    val distanceKm: Int
)

class CalculateQiblaUseCase {
    operator fun invoke(userLat: Double, userLon: Double): QiblaResult {
        val bearing = calculateBearing(userLat, userLon, KAABA_LAT, KAABA_LON)
        val distanceKm = calculateDistance(userLat, userLon, KAABA_LAT, KAABA_LON)
        return QiblaResult(bearing = bearing, distanceKm = distanceKm)
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val phi1 = lat1 * PI / 180
        val phi2 = lat2 * PI / 180
        val deltaLambda = (lon2 - lon1) * PI / 180
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearing = atan2(y, x) * 180 / PI
        return ((bearing + 360) % 360).roundToInt()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371.0
        val dLat = (lat2 - lat1) * PI / 180
        val dLon = (lon2 - lon1) * PI / 180
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * PI / 180) * cos(lat2 * PI / 180) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).roundToInt()
    }
}
