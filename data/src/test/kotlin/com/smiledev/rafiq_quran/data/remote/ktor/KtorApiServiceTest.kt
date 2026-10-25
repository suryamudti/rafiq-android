package com.smiledev.rafiq_quran.data.remote.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KtorApiServiceTest {

    @Test
    fun equranApiServiceReturnsParsedTafsir() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """
                    {
                      "code": 200,
                      "message": "OK",
                      "data": {
                        "nomor": 1,
                        "nama": "Al-Fatihah",
                        "namaLatin": "Al-Fatihah",
                        "jumlahAyat": 7,
                        "tafsir": [
                          { "ayat": 1, "teks": "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang." }
                        ]
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(KtorClientFactory.defaultJson)
            }
        }
        val api = KtorEQuranApiService(client, baseUrl = "https://equran.id/")
        val response = api.getTafsir(1)

        assertEquals(200, response.code)
        assertEquals("Al-Fatihah", response.data.nama)
        assertEquals(1, response.data.tafsir.size)
        assertEquals("Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.", response.data.tafsir[0].teks)
    }

    @Test
    fun aladhanApiServiceReturnsPrayerTimes() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """
                    {
                      "code": 200,
                      "status": "OK",
                      "data": {
                        "timings": {
                          "Imsak": "04:20",
                          "Fajr": "04:30",
                          "Sunrise": "05:45",
                          "Dhuhr": "11:55",
                          "Asr": "15:10",
                          "Maghrib": "18:00",
                          "Isha": "19:10"
                        }
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(KtorClientFactory.defaultJson)
            }
        }
        val api = KtorAladhanApiService(client, baseUrl = "https://api.aladhan.com/")
        val response = api.getTimings("25-10-2026", -6.2, 106.8)

        assertEquals(200, response.code)
        assertEquals("04:30", response.data.timings.Fajr)
        assertEquals("18:00", response.data.timings.Maghrib)
    }

    @Test
    fun metalPriceApiServiceReturnsPrices() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """
                    [
                      { "price": 2000.0, "unit": "oz", "date": "2026-10-25", "timestamp": 1234567890 }
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(KtorClientFactory.defaultJson)
            }
        }
        val api = KtorMetalPriceApiService(client, baseUrl = "https://api.gold-api.com/")
        val gold = api.getGoldPrice()
        assertEquals(1, gold.size)
        assertEquals(2000.0, gold[0].price, 0.001)
    }

    @Test
    fun githubReleaseApiServiceReturnsRelease() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """
                    {
                      "tag_name": "v1.2.0",
                      "name": "Release 1.2.0",
                      "html_url": "https://github.com/suryamudti/rafiq-android/releases/tag/v1.2.0"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(KtorClientFactory.defaultJson)
            }
        }
        val api = KtorGithubReleaseApiService(client, baseUrl = "https://api.github.com/")
        val release = api.getLatestRelease()
        assertEquals("v1.2.0", release.tagName)
        assertEquals("Release 1.2.0", release.name)
    }
}
