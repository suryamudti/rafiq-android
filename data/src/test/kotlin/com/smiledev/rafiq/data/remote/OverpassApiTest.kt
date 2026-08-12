package com.smiledev.rafiq.data.remote

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OverpassApiTest {

    private val service: OverpassApiService = mockk()
    private lateinit var api: OverpassApi

    @Before
    fun setUp() {
        api = OverpassApi(listOf(service))
    }

    @Test
    fun `fetchMosques returns elements with lat and lon`() = runTest {
        val elements = listOf(
            OverpassElement(type = "node", id = 1, lat = -6.2, lon = 106.8, tags = mapOf("name" to "Masjid")),
            OverpassElement(type = "way", id = 2, center = OverpassCenter(-6.21, 106.81), tags = mapOf("name" to "Masjid 2"))
        )
        coEvery { service.query(any()) } returns OverpassResponse(elements)

        val result = api.fetchMosques(-6.2, 106.8)

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(-6.2, result[0].lat!!, 0.001)
        assertEquals(106.8, result[0].lon!!, 0.001)
        assertEquals(2L, result[1].id)
        assertEquals(-6.21, result[1].center!!.lat, 0.001)
        assertEquals(106.81, result[1].center!!.lon, 0.001)
    }

    @Test
    fun `fetchMosques builds correct query`() = runTest {
        coEvery { service.query(any()) } returns OverpassResponse(emptyList())

        api.fetchMosques(-6.2088, 106.8456, 3000)

        val expected = "[out:json];" +
            "(" +
            """node["amenity"="place_of_worship"]["religion"="muslim"](around:3000,-6.2088,106.8456);""" +
            """way["amenity"="place_of_worship"]["religion"="muslim"](around:3000,-6.2088,106.8456);""" +
            ");" +
            "out center 50;"
        coEvery { service.query(expected) } returns OverpassResponse(emptyList())
    }

    @Test
    fun `fetchMosques falls back to next mirror when first fails`() = runTest {
        val failingService: OverpassApiService = mockk()
        val workingService: OverpassApiService = mockk()
        val elements = listOf(
            OverpassElement(type = "node", id = 1, lat = -6.2, lon = 106.8, tags = mapOf("name" to "Masjid"))
        )
        coEvery { failingService.query(any()) } throws RuntimeException("mirror down")
        coEvery { workingService.query(any()) } returns OverpassResponse(elements)

        val api = OverpassApi(listOf(failingService, workingService))
        val result = api.fetchMosques(-6.2, 106.8)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `fetchMosques rethrows when all mirrors fail`() = runTest {
        val service1: OverpassApiService = mockk()
        val service2: OverpassApiService = mockk()
        coEvery { service1.query(any()) } throws RuntimeException("mirror 1 down")
        coEvery { service2.query(any()) } throws RuntimeException("mirror 2 down")

        val api = OverpassApi(listOf(service1, service2))

        try {
            api.fetchMosques(-6.2, 106.8)
            throw AssertionError("Expected exception was not thrown")
        } catch (e: RuntimeException) {
            assertEquals("mirror 2 down", e.message)
        }
    }
}
