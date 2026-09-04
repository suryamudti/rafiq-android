package com.smiledev.rafiq_quran.ui.mosques

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.DefaultDispatcherProvider
import com.smiledev.rafiq_quran.core.DispatcherProvider
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.model.Mosque
import com.smiledev.rafiq_quran.domain.repository.GeoLocation
import com.smiledev.rafiq_quran.domain.repository.LocationProvider
import com.smiledev.rafiq_quran.domain.repository.MosqueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class MosquesUiState(
    val userLocation: GeoLocation? = null,
    val searchCenter: GeoLocation? = null,
    val locationGranted: Boolean = false,
    val showPermissionDenied: Boolean = false,
    val isLoading: Boolean = false,
    val isSearchingArea: Boolean = false,
    val mosques: List<Mosque> = emptyList(),
    val error: AppError? = null
)

@HiltViewModel
class MosquesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val mosqueRepository: MosqueRepository,
    private val locationProvider: LocationProvider,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(MosquesUiState())
    val uiState: StateFlow<MosquesUiState> = _uiState

    private var lastLat: Double = -6.2088
    private var lastLon: Double = 106.8456

    fun checkLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val granted = coarse == PackageManager.PERMISSION_GRANTED
        if (granted) {
            _uiState.value = _uiState.value.copy(locationGranted = true)
            fetchLocation()
        }
        return granted
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.value = _uiState.value.copy(locationGranted = true, showPermissionDenied = false)
            fetchLocation()
        } else {
            _uiState.value = _uiState.value.copy(showPermissionDenied = true)
            loadMosques(-6.2088, 106.8456)
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = locationProvider.getLastLocation()) {
                is Result.Success -> {
                    val geoLocation = GeoLocation(result.data.latitude, result.data.longitude)
                    _uiState.value = _uiState.value.copy(
                        userLocation = geoLocation,
                        isLoading = false
                    )
                    loadMosques(result.data.latitude, result.data.longitude)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.error,
                        isLoading = false
                    )
                    // Fallback to default location
                    loadMosques(-6.2088, 106.8456)
                }
            }
        }
    }

    fun searchArea(lat: Double, lon: Double) {
        loadMosques(lat, lon, isAreaSearch = true)
    }

    fun recenterOnUser() {
        val loc = _uiState.value.userLocation
        if (loc != null) {
            loadMosques(loc.latitude, loc.longitude, isAreaSearch = false)
        } else {
            fetchLocation()
        }
    }

    private fun loadMosques(lat: Double, lon: Double, isAreaSearch: Boolean = false) {
        lastLat = lat
        lastLon = lon
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = if (isAreaSearch) {
                _uiState.value.copy(isSearchingArea = true, error = null)
            } else {
                _uiState.value.copy(isLoading = true, error = null)
            }
            when (val result = mosqueRepository.getNearbyMosques(lat, lon)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        mosques = result.data,
                        searchCenter = GeoLocation(lat, lon),
                        isLoading = false,
                        isSearchingArea = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.error,
                        isLoading = false,
                        isSearchingArea = false
                    )
                }
            }
        }
    }

    fun retry() {
        loadMosques(lastLat, lastLon)
    }
}
