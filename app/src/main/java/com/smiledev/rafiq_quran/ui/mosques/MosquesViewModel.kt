package com.smiledev.rafiq.ui.mosques

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.data.preferences.PreferencesManager
import com.smiledev.rafiq.domain.model.Mosque
import com.smiledev.rafiq.domain.repository.GeoLocation
import com.smiledev.rafiq.domain.repository.LocationProvider
import com.smiledev.rafiq.domain.repository.MosqueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

@Immutable
data class MosquesUiState(
    val userLocation: GeoPoint? = null,
    val locationGranted: Boolean = false,
    val showPermissionDenied: Boolean = false,
    val isLoading: Boolean = false,
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

    fun checkLocationPermission() {
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (coarse == PackageManager.PERMISSION_GRANTED) {
            _uiState.value = _uiState.value.copy(locationGranted = true)
            fetchLocation()
        }
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
    private fun fetchLocation() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = locationProvider.getLastLocation()) {
                is Result.Success -> {
                    val geoPoint = GeoPoint(result.data.latitude, result.data.longitude)
                    _uiState.value = _uiState.value.copy(
                        userLocation = geoPoint,
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

    private fun loadMosques(lat: Double, lon: Double) {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = mosqueRepository.getNearbyMosques(lat, lon)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        mosques = result.data,
                        isLoading = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.error,
                        isLoading = false
                    )
                }
            }
        }
    }
}
