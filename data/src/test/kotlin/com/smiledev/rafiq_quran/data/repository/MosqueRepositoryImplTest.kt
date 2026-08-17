package com.smiledev.rafiq.data.repository

import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.getOrNull
import com.smiledev.rafiq.data.remote.OverpassApi
import com.smiledev.rafiq.data.remote.OverpassCenter
import com.smiledev.rafiq.data.remote.OverpassElement
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MosqueRepositoryImplTest {

    private val overpassApi: OverpassApi = mockk()
    private lateinit var repo: MosqueRepositoryImpl

    @Before
    fun setUp() {
        repo = MosqueRepositoryImpl(overpassApi)
    }

    @Test
    fun `getNearbyMosques returns mapped mosques`() = runTest {
        val elements = listOf(
            OverpassElement(type = "node", id = 1, lat = -6.2, lon = 106.8, tags = mapOf("name" to "Masjid Istiqlal")),
            OverpassElement(type = "node", id = 2, lat = -6.21, lon = 106.81, tags = mapOf("name" to "Masjid Raya")),
            OverpassElement(type = "way", id = 3, center = OverpassCenter(lat = -6.22, lon = 106.82), tags = mapOf("name" to "Masjid Jami"))
        )
        coEvery { overpassApi.fetchMosques(-6.2, 106.8, 5000) } returns elements

        val result = repo.getNearbyMosques(-6.2, 106.8)

        val mosques = result.getOrNull()
        assertEquals(3, mosques?.size)
        assertEquals("Masjid Istiqlal", mosques?.get(0)?.name)
        assertEquals("Masjid Raya", mosques?.get(1)?.name)
        assertEquals("Masjid Jami", mosques?.get(2)?.name)
    }

    @Test
    fun `getNearbyMosques skips elements without name`() = runTest {
        val elements = listOf(
            OverpassElement(type = "node", id = 1, lat = -6.2, lon = 106.8, tags = mapOf("name" to "Masjid Istiqlal")),
            OverpassElement(type = "node", id = 2, lat = -6.21, lon = 106.81, tags = null),
            OverpassElement(type = "node", id = 3, lat = -6.22, lon = 106.82, tags = mapOf("not_name" to "x"))
        )
        coEvery { overpassApi.fetchMosques(-6.2, 106.8, 5000) } returns elements

        val result = repo.getNearbyMosques(-6.2, 106.8)

        val mosques = result.getOrNull()
        assertEquals(1, mosques?.size)
        assertEquals("Masjid Istiqlal", mosques?.get(0)?.name)
    }

    @Test
    fun `getNearbyMosques network error returns AppError`() = runTest {
        coEvery { overpassApi.fetchMosques(-6.2, 106.8, 5000) } throws RuntimeException("Timeout")

        val result = repo.getNearbyMosques(-6.2, 106.8)

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
    }
}
