package com.smiledev.rafiq_quran.ui.mosques

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.smiledev.rafiq_quran.domain.model.Mosque
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
data class SharedMosqueItem(
    val mosque: Mosque,
    val distanceMeters: Int,
    val formattedDistance: String,
    val subtitle: String = "Mosque / Tempat Ibadah"
)

@Immutable
data class SharedMosquesUiState(
    val userLat: Double = -6.2088,
    val userLon: Double = 106.8456,
    val cityName: String = "Jakarta, Indonesia",
    val radiusMeters: Int = 2000,
    val isMapView: Boolean = true,
    val mosques: List<SharedMosqueItem> = emptyList(),
    val selectedMosque: Mosque? = null,
    val isLoading: Boolean = false
)

fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val r = 6371000.0 // Earth radius in meters
    val phi1 = lat1 * PI / 180.0
    val phi2 = lat2 * PI / 180.0
    val deltaPhi = (lat2 - lat1) * PI / 180.0
    val deltaLambda = (lon2 - lon1) * PI / 180.0
    val a = sin(deltaPhi / 2.0) * sin(deltaPhi / 2.0) +
            cos(phi1) * cos(phi2) *
            sin(deltaLambda / 2.0) * sin(deltaLambda / 2.0)
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    return (r * c).roundToInt()
}

fun formatDistanceMeters(meters: Int): String {
    return if (meters >= 1000) {
        val km = meters / 1000.0
        val rounded = (km * 10).roundToInt() / 10.0
        if (rounded % 1.0 == 0.0) "${rounded.toInt()} km" else "$rounded km"
    } else {
        "$meters m"
    }
}

val DEFAULT_JAKARTA_MOSQUES = listOf(
    Mosque(1L, "Masjid Istiqlal", -6.1702, 106.8315),
    Mosque(2L, "Masjid Cut Meutia", -6.1873, 106.8351),
    Mosque(3L, "Masjid Agung Sunda Kelapa", -6.1994, 106.8327),
    Mosque(4L, "Masjid Baitul Ihsan (Bank Indonesia)", -6.1805, 106.8223),
    Mosque(5L, "Masjid Agung Al-Azhar", -6.2351, 106.7997),
    Mosque(6L, "Masjid At-Tin TMII", -6.3006, 106.8837)
)

class SharedMosquesViewModel(
    initialLat: Double = -6.2088,
    initialLon: Double = 106.8456,
    initialCity: String = "Jakarta, Indonesia",
    initialMosques: List<Mosque> = DEFAULT_JAKARTA_MOSQUES
) : ViewModel() {

    private val allMosques: List<Mosque> = initialMosques

    private val _uiState = MutableStateFlow(
        SharedMosquesUiState(
            userLat = initialLat,
            userLon = initialLon,
            cityName = initialCity
        )
    )
    val uiState: StateFlow<SharedMosquesUiState> = _uiState.asStateFlow()

    init {
        refreshCalculatedMosques()
    }

    fun selectMosque(mosque: Mosque?) {
        _uiState.update { it.copy(selectedMosque = mosque) }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isMapView = !it.isMapView) }
    }

    fun setRadius(meters: Int) {
        _uiState.update { it.copy(radiusMeters = meters) }
        refreshCalculatedMosques()
    }

    fun updateUserLocation(lat: Double, lon: Double, city: String) {
        _uiState.update {
            it.copy(
                userLat = lat,
                userLon = lon,
                cityName = city
            )
        }
        refreshCalculatedMosques()
    }

    private fun refreshCalculatedMosques() {
        val state = _uiState.value
        val items = allMosques.map { mosque ->
            val dist = calculateDistanceMeters(state.userLat, state.userLon, mosque.latitude, mosque.longitude)
            SharedMosqueItem(
                mosque = mosque,
                distanceMeters = dist,
                formattedDistance = formatDistanceMeters(dist)
            )
        }.sortedBy { it.distanceMeters }

        _uiState.update { it.copy(mosques = items) }
    }
}
