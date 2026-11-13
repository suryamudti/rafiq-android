package com.smiledev.rafiq_quran.wear.ui.qibla

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.usecase.CalculateQiblaUseCase
import com.smiledev.rafiq_quran.wear.location.WearLocationSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

private const val DEFAULT_LAT = -6.2088
private const val DEFAULT_LON = 106.8456
private const val DEFAULT_CITY = "Jakarta"

@Immutable
data class WearQiblaUiState(
    val bearing: Int = 0,
    val distanceKm: Int = 0,
    val userLat: Double = -6.2088,
    val userLon: Double = 106.8456,
    val cityName: String = "Jakarta",
    val directionCardinal: String = "WNW",
    val isSyncingLocation: Boolean = false
)

@HiltViewModel
class WearQiblaViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val calculateQiblaUseCase: CalculateQiblaUseCase,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider,
    private val wearLocationSync: WearLocationSync? = null
) : ViewModel() {

    constructor(
        preferencesManager: PreferencesManager,
        calculateQiblaUseCase: CalculateQiblaUseCase,
        dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
    ) : this(preferencesManager, calculateQiblaUseCase, dispatcherProvider, null)

    private val _uiState = MutableStateFlow(WearQiblaUiState())
    val uiState: StateFlow<WearQiblaUiState> = _uiState.asStateFlow()

    init {
        recalculate(DEFAULT_LAT, DEFAULT_LON, DEFAULT_CITY)
        viewModelScope.launch(dispatcherProvider.io) {
            combine(
                preferencesManager.latitude,
                preferencesManager.longitude,
                preferencesManager.cityName
            ) { latStr, lonStr, cityStr ->
                val lat = latStr.toDoubleOrNull() ?: DEFAULT_LAT
                val lon = lonStr.toDoubleOrNull() ?: DEFAULT_LON
                val city = if (cityStr.isNotBlank()) cityStr else DEFAULT_CITY
                Triple(lat, lon, city)
            }.collect { (lat, lon, city) ->
                recalculate(lat, lon, city)
            }
        }
    }

    fun refreshLocation(forceGeocode: Boolean = true) {
        val sync = wearLocationSync ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isSyncingLocation = true)
            sync.syncIfPermitted(forceGeocode = forceGeocode)
            _uiState.value = _uiState.value.copy(isSyncingLocation = false)
        }
    }

    fun recalculate(lat: Double, lon: Double, city: String = _uiState.value.cityName) {
        val result = calculateQiblaUseCase(lat, lon)
        _uiState.value = _uiState.value.copy(
            bearing = result.bearing,
            distanceKm = result.distanceKm,
            userLat = lat,
            userLon = lon,
            cityName = city,
            directionCardinal = wearCompassDirection(result.bearing, currentLocaleCode())
        )
    }
}

private val COMPASS_POINTS_EN = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
)

private val COMPASS_POINTS_ID = listOf(
    "U", "UTL", "TL", "TTL", "T", "TGr", "TG", "STG",
    "S", "SBD", "BD", "BBD", "B", "BBL", "BL", "UBL"
)

fun wearCompassDirection(bearing: Int, localeCode: String = "en"): String {
    val normalized = ((bearing % 360) + 360) % 360
    val index = ((normalized + 11.25) / 22.5).toInt() % 16
    val points = if (localeCode.equals("id", ignoreCase = true)) COMPASS_POINTS_ID else COMPASS_POINTS_EN
    return points[index]
}

fun wearNormalizeAngle180(deg: Double): Double = ((deg + 180) % 360 + 360) % 360 - 180

fun isQiblaAligned(bearing: Int, azimuth: Float, thresholdDegrees: Float = 5f): Boolean {
    val diff = wearNormalizeAngle180(bearing.toDouble() - azimuth.toDouble())
    return abs(diff) <= thresholdDegrees
}
