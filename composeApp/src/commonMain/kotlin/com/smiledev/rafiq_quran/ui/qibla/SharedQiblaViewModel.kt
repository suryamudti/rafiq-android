package com.smiledev.rafiq_quran.ui.qibla

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.smiledev.rafiq_quran.domain.usecase.CalculateQiblaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

@Immutable
data class SharedQiblaUiState(
    val bearing: Int = 295,
    val distanceKm: Int = 7924,
    val userLat: Double = -6.2088,
    val userLon: Double = 106.8456,
    val cityName: String = "Jakarta, Indonesia",
    val compassDirection: String = "WNW",
    val kaabaCoordinates: String = "21.4225° N, 39.8262° E",
    val alignmentThresholdDegrees: Float = 5.0f
)

private val COMPASS_POINTS = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
)

fun calculateCompassDirection(bearing: Int): String {
    val normalized = ((bearing % 360) + 360) % 360
    val index = ((normalized + 11.25) / 22.5).toInt() % 16
    return COMPASS_POINTS[index]
}

fun normalizeAngleDifference(targetDeg: Float, currentDeg: Float): Float {
    var diff = (targetDeg - currentDeg) % 360f
    if (diff > 180f) diff -= 360f
    if (diff < -180f) diff += 360f
    return diff
}

class SharedQiblaViewModel(
    initialLat: Double = -6.2088,
    initialLon: Double = 106.8456,
    initialCity: String = "Jakarta, Indonesia",
    private val calculateQiblaUseCase: CalculateQiblaUseCase = CalculateQiblaUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        createState(initialLat, initialLon, initialCity)
    )
    val uiState: StateFlow<SharedQiblaUiState> = _uiState.asStateFlow()

    fun updateCoordinates(cityName: String, lat: Double, lon: Double) {
        _uiState.update {
            createState(lat, lon, cityName)
        }
    }

    fun isAligned(deviceAzimuth: Float): Boolean {
        val diff = abs(normalizeAngleDifference(_uiState.value.bearing.toFloat(), deviceAzimuth))
        return diff <= _uiState.value.alignmentThresholdDegrees
    }

    private fun createState(lat: Double, lon: Double, city: String): SharedQiblaUiState {
        val result = calculateQiblaUseCase(lat, lon)
        return SharedQiblaUiState(
            bearing = result.bearing,
            distanceKm = result.distanceKm,
            userLat = lat,
            userLon = lon,
            cityName = city,
            compassDirection = calculateCompassDirection(result.bearing)
        )
    }
}
