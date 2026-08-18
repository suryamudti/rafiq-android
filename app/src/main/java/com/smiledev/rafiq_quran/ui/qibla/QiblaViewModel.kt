package com.smiledev.rafiq_quran.ui.qibla

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.usecase.CalculateQiblaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class QiblaUiState(
    val bearing: Int = 0,
    val distanceKm: Int = 0,
    val userLat: Double = -6.2088,
    val userLon: Double = 106.8456,
    val isLoading: Boolean = false
)

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val calculateQiblaUseCase: CalculateQiblaUseCase,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            val latStr = preferencesManager.latitude.first()
            val lonStr = preferencesManager.longitude.first()
            val lat = latStr.toDoubleOrNull() ?: DEFAULT_LAT
            val lon = lonStr.toDoubleOrNull() ?: DEFAULT_LON
            recalculate(lat, lon)
        }
    }

    fun recalculate(lat: Double, lon: Double) {
        val result = calculateQiblaUseCase(lat, lon)
        _uiState.value = QiblaUiState(
            bearing = result.bearing,
            distanceKm = result.distanceKm,
            userLat = lat,
            userLon = lon
        )
    }
}

private const val DEFAULT_LAT = -6.2088
private const val DEFAULT_LON = 106.8456

private val COMPASS_POINTS = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
)

fun compassDirection(bearing: Int): String {
    val normalized = ((bearing % 360) + 360) % 360
    val index = ((normalized + 11.25) / 22.5).toInt() % 16
    return COMPASS_POINTS[index]
}

fun normalizeAngle180(deg: Double): Double {
    return ((deg + 180) % 360 + 360) % 360 - 180
}
