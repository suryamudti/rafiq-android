package com.smiledev.rafiq_quran.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

data class OverpassElement(
    val type: String?,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
)

data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

data class OverpassResponse(
    val elements: List<OverpassElement>
)

interface OverpassApiService {
    @FormUrlEncoded
    @POST("interpreter")
    suspend fun query(@Field("data") query: String): OverpassResponse
}

@Singleton
class OverpassApi @Inject constructor(
    private val services: List<OverpassApiService>
) {
    suspend fun fetchMosques(lat: Double, lon: Double, radius: Int = 5000): List<OverpassElement> {
        val query = buildString {
            append("[out:json];")
            append("(")
            append("""node["amenity"="place_of_worship"]["religion"="muslim"](around:$radius,$lat,$lon);""")
            append("""way["amenity"="place_of_worship"]["religion"="muslim"](around:$radius,$lat,$lon);""")
            append(");")
            append("out center 50;")
        }
        var lastError: Exception? = null
        for (service in services) {
            try {
                return service.query(query).elements
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("No Overpass mirrors configured")
    }
}
