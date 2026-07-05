package com.smiledev.rafiq.ui.mosques

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.smiledev.rafiq.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

data class MosquesUiState(
    val userLocation: GeoPoint? = null,
    val locationGranted: Boolean = false,
    val showPermissionDenied: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class MosquesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
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
        }
    }

    private fun fetchLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val location = Tasks.await(fusedClient.lastLocation)
                if (location != null) {
                    _uiState.value = _uiState.value.copy(
                        userLocation = GeoPoint(location.latitude, location.longitude),
                        isLoading = false
                    )
                } else {
                    fallbackToPreferences()
                }
            } catch (_: Exception) {
                fallbackToPreferences()
            }
        }
    }

    private suspend fun fallbackToPreferences() {
        val latStr = preferencesManager.latitude.first()
        val lonStr = preferencesManager.longitude.first()
        val lat = latStr.toDoubleOrNull() ?: -6.2088
        val lon = lonStr.toDoubleOrNull() ?: 106.8456
        _uiState.value = _uiState.value.copy(
            userLocation = GeoPoint(lat, lon),
            isLoading = false
        )
    }
}
