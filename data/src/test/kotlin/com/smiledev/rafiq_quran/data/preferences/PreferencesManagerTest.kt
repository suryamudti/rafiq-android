package com.smiledev.rafiq_quran.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferencesManagerTest {

    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferencesManager = PreferencesManager(context)
    }

    @Test
    fun `setCityName preserves Unicode characters and strips control characters`() = runBlocking {
        preferencesManager.setCityName("  مكة المكرمة\n\r\t\u0000  ")
        assertEquals("مكة المكرمة", preferencesManager.cityName.first())

        preferencesManager.setCityName("São Paulo \u007F")
        assertEquals("São Paulo", preferencesManager.cityName.first())

        preferencesManager.setCityName("  東京  ")
        assertEquals("東京", preferencesManager.cityName.first())
    }

    @Test
    fun `setLatitude validates numeric range and finiteness`() = runBlocking {
        preferencesManager.setLatitude("-6.2088")
        assertEquals("-6.2088", preferencesManager.latitude.first())

        // Integer coordinates and boundary values should be accepted
        preferencesManager.setLatitude("0")
        assertEquals("0", preferencesManager.latitude.first())

        preferencesManager.setLatitude("90")
        assertEquals("90", preferencesManager.latitude.first())

        preferencesManager.setLatitude("-90.0")
        assertEquals("-90.0", preferencesManager.latitude.first())

        // Out-of-range and non-finite/non-numeric values should be ignored
        preferencesManager.setLatitude("90.1")
        preferencesManager.setLatitude("-90.1")
        preferencesManager.setLatitude("999.9")
        preferencesManager.setLatitude("NaN")
        preferencesManager.setLatitude("Infinity")
        preferencesManager.setLatitude("-Infinity")
        preferencesManager.setLatitude("abc")
        preferencesManager.setLatitude("")
        assertEquals("-90.0", preferencesManager.latitude.first())
    }

    @Test
    fun `setLongitude validates numeric range and finiteness`() = runBlocking {
        preferencesManager.setLongitude("106.8456")
        assertEquals("106.8456", preferencesManager.longitude.first())

        // Integer coordinates and boundary values should be accepted
        preferencesManager.setLongitude("0")
        assertEquals("0", preferencesManager.longitude.first())

        preferencesManager.setLongitude("180")
        assertEquals("180", preferencesManager.longitude.first())

        preferencesManager.setLongitude("-180.0")
        assertEquals("-180.0", preferencesManager.longitude.first())

        // Out-of-range and non-finite/non-numeric values should be ignored
        preferencesManager.setLongitude("180.1")
        preferencesManager.setLongitude("-180.1")
        preferencesManager.setLongitude("999.9")
        preferencesManager.setLongitude("NaN")
        preferencesManager.setLongitude("Infinity")
        preferencesManager.setLongitude("not-a-number")
        assertEquals("-180.0", preferencesManager.longitude.first())
    }

    @Test
    fun `flows do not re-emit on unrelated preference writes`() = runBlocking {
        preferencesManager.setThemeMode("dark")

        val themeEmissions = mutableListOf<String>()
        val collectJob = launch(Dispatchers.Unconfined) {
            preferencesManager.themeMode.collect { themeEmissions.add(it) }
        }

        // Wait for initial emission
        delay(50)
        assertEquals(listOf("dark"), themeEmissions)

        // Perform unrelated writes (e.g., Tasbih taps & city updates)
        preferencesManager.setTasbihCount(1)
        preferencesManager.setTasbihCount(2)
        preferencesManager.setCityName("Jakarta")
        delay(50)

        // Theme flow must still have emitted only once
        assertEquals(listOf("dark"), themeEmissions)

        // Changing theme mode emits the new value
        preferencesManager.setThemeMode("light")
        delay(50)
        assertEquals(listOf("dark", "light"), themeEmissions)

        collectJob.cancel()
    }
}
