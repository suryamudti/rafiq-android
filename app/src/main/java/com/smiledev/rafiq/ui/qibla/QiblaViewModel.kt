package com.smiledev.rafiq.ui.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.*
import javax.inject.Inject

private const val KAABA_LAT = 21.4225
private const val KAABA_LON = 39.8262

data class QiblaUiState(
    val bearing: Int = 0,
    val distanceKm: Int = 0,
    val userLat: Double = -6.2088,
    val userLon: Double = 106.8456,
    val isLoading: Boolean = false
)

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val latStr = preferencesManager.latitude.first()
            val lonStr = preferencesManager.longitude.first()
            val lat = latStr.toDoubleOrNull() ?: -6.2088
            val lon = lonStr.toDoubleOrNull() ?: 106.8456
            val bearing = calculateBearing(lat, lon, KAABA_LAT, KAABA_LON)
            val distanceKm = calculateDistance(lat, lon, KAABA_LAT, KAABA_LON)
            _uiState.value = QiblaUiState(
                bearing = bearing,
                distanceKm = distanceKm,
                userLat = lat,
                userLon = lon
            )
        }
    }

    fun recalculate(lat: Double, lon: Double) {
        val bearing = calculateBearing(lat, lon, KAABA_LAT, KAABA_LON)
        val distanceKm = calculateDistance(lat, lon, KAABA_LAT, KAABA_LON)
        _uiState.value = QiblaUiState(
            bearing = bearing,
            distanceKm = distanceKm,
            userLat = lat,
            userLon = lon
        )
    }
}

fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val phi1 = lat1 * PI / 180
    val phi2 = lat2 * PI / 180
    val deltaLambda = (lon2 - lon1) * PI / 180

    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

    val bearing = atan2(y, x) * 180 / PI
    return ((bearing + 360) % 360).roundToInt()
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val r = 6371.0
    val dLat = (lat2 - lat1) * PI / 180
    val dLon = (lon2 - lon1) * PI / 180
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180) * cos(lat2 * PI / 180) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (r * c).roundToInt()
}
