package com.smiledev.rafiq_quran.data.repository

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.smiledev.rafiq_quran.core.AppError
import com.smiledev.rafiq_quran.core.Result
import com.smiledev.rafiq_quran.core.retryIO
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.domain.repository.LocationProvider
import com.smiledev.rafiq_quran.domain.repository.GeoLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : LocationProvider {

    override suspend fun getLastLocation(): Result<GeoLocation, AppError> {
        return retryIO {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val location = Tasks.await(fusedClient.lastLocation)
                if (location != null) {
                    preferencesManager.setLatitude(location.latitude.toString())
                    preferencesManager.setLongitude(location.longitude.toString())
                    Result.Success(GeoLocation(location.latitude, location.longitude))
                } else {
                    fallbackToPreferences()
                }
            } catch (e: Exception) {
                fallbackToPreferences()
            }
        }
    }

    private suspend fun fallbackToPreferences(): Result<GeoLocation, AppError> {
        val latStr = preferencesManager.latitude.first()
        val lonStr = preferencesManager.longitude.first()
        val lat = latStr.toDoubleOrNull() ?: -6.2088
        val lon = lonStr.toDoubleOrNull() ?: 106.8456
        return Result.Success(GeoLocation(lat, lon))
    }
}