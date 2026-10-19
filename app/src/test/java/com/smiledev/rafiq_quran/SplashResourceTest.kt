package com.smiledev.rafiq_quran

import org.junit.Assert.assertNotEquals
import org.junit.Test

class SplashResourceTest {

    @Test
    fun splashScreenThemeResourceIsGenerated() {
        assertNotEquals(0, R.style.Theme_RafiqApp_Starting)
    }

    @Test
    fun splashScreenColorResourcesAreGenerated() {
        assertNotEquals(0, R.color.splash_background)
        assertNotEquals(0, R.color.splash_icon_tint)
    }

    @Test
    fun splashScreenLogoDrawableResourceIsGenerated() {
        assertNotEquals(0, R.drawable.ic_splash_logo)
    }

    @Test
    fun dashboardIconDrawableResourcesAreGeneratedAndDistinct() {
        val dashboardIcons = listOf(
            R.drawable.ic_quran,
            R.drawable.ic_prayer_times,
            R.drawable.ic_qibla,
            R.drawable.ic_tasbih,
            R.drawable.ic_hadith,
            R.drawable.ic_prophet,
            R.drawable.ic_asmaul_husna,
            R.drawable.ic_recitation,
            R.drawable.ic_mosque,
            R.drawable.ic_calendar,
            R.drawable.ic_zakat,
            R.drawable.ic_prayer_log,
            R.drawable.ic_prayer_guide,
            R.drawable.ic_sunnah,
            R.drawable.ic_hifz,
            R.drawable.ic_location
        )
        dashboardIcons.forEach { resId ->
            assertNotEquals(0, resId)
        }
        assertNotEquals(R.drawable.ic_prayer_times, R.drawable.ic_calendar)
        assertNotEquals(R.drawable.ic_recitation, R.drawable.ic_play)
    }
}
