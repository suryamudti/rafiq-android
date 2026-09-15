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
}
