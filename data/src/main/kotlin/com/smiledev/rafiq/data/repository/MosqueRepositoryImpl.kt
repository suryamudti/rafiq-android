package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.retryIO
import com.smiledev.rafiq.data.remote.OverpassApi
import com.smiledev.rafiq.domain.model.Mosque
import com.smiledev.rafiq.domain.repository.MosqueRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MosqueRepositoryImpl @Inject constructor(
    private val overpassApi: OverpassApi
) : MosqueRepository {

    override suspend fun getNearbyMosques(lat: Double, lon: Double, radiusMeters: Int): Result<List<Mosque>, AppError> {
        return retryIO {
            try {
                val elements = overpassApi.fetchMosques(lat, lon, radiusMeters)
                val mosques = elements.mapNotNull { element ->
                    val name = element.tags?.get("name")?.trim()
                    if (name.isNullOrBlank()) return@mapNotNull null
                    val elemLat = element.lat ?: element.center?.lat ?: return@mapNotNull null
                    val elemLon = element.lon ?: element.center?.lon ?: return@mapNotNull null
                    Mosque(
                        id = element.id,
                        name = name,
                        latitude = elemLat,
                        longitude = elemLon
                    )
                }
                Result.Success(mosques)
            } catch (e: Exception) {
                Result.Error(AppError.Network("Failed to fetch nearby mosques", e))
            }
        }
    }
}
