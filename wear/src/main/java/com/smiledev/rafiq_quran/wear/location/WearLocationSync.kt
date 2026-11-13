package com.smiledev.rafiq_quran.wear.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.repository.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.abs

@Singleton
class WearLocationSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationProvider: LocationProvider,
    private val preferencesManager: PreferencesManager
) {
    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    suspend fun syncIfPermitted(forceGeocode: Boolean = false): Boolean {
        if (!hasLocationPermission()) return false

        val prevLat = preferencesManager.latitude.first().toDoubleOrNull()
        val prevLon = preferencesManager.longitude.first().toDoubleOrNull()
        val prevCity = preferencesManager.cityName.first()

        val result = withContext(Dispatchers.IO) {
            locationProvider.getLastLocation()
        }
        if (result !is Result.Success) return false

        val geo = result.data
        val movedSignificantly = prevLat == null || prevLon == null ||
            abs(geo.latitude - prevLat) > 0.02 ||
            abs(geo.longitude - prevLon) > 0.02

        if (forceGeocode || movedSignificantly || prevCity.isBlank()) {
            val resolvedCity = resolveCityName(geo.latitude, geo.longitude)
                ?: fallbackCityName(geo.latitude, geo.longitude, prevCity, movedSignificantly)
            if (resolvedCity.isNotBlank()) {
                preferencesManager.setCityName(resolvedCity)
            }
        }
        return true
    }

    private suspend fun resolveCityName(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return withContext(Dispatchers.IO) {
            runCatching {
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    withTimeoutOrNull(4_000L) {
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(results: MutableList<Address>) {
                                    if (cont.isActive) cont.resume(results)
                                }

                                override fun onError(errorMessage: String?) {
                                    if (cont.isActive) cont.resume(null)
                                }
                            })
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)
                }
                val address = addresses?.firstOrNull()
                address?.locality
                    ?: address?.subAdminArea
                    ?: address?.adminArea
                    ?: address?.countryName
            }.getOrNull()
        }
    }

    private fun fallbackCityName(
        lat: Double,
        lon: Double,
        previousCity: String,
        movedSignificantly: Boolean
    ): String {
        if (!movedSignificantly && previousCity.isNotBlank()) return previousCity
        val nearJakarta = abs(lat - (-6.2088)) < 0.05 && abs(lon - 106.8456) < 0.05
        return if (nearJakarta) {
            "Jakarta"
        } else {
            String.format(Locale.US, "%.2f, %.2f", lat, lon)
        }
    }
}
