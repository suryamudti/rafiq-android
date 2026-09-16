package com.smiledev.rafiq_quran.ui.designsystem

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.theme.ArabicFontFamily
import com.smiledev.rafiq_quran.theme.DarkRafiqExtendedColors
import com.smiledev.rafiq_quran.theme.LightRafiqExtendedColors
import com.smiledev.rafiq_quran.theme.RafiqArabicTypography
import com.smiledev.rafiq_quran.theme.RafiqCornerRadius
import com.smiledev.rafiq_quran.theme.RafiqElevation
import com.smiledev.rafiq_quran.theme.RafiqIconSizes
import com.smiledev.rafiq_quran.theme.RafiqSpacing
import com.smiledev.rafiq_quran.theme.Typography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTest {

    @Test
    fun testSpacingTokensAscendingOrder() {
        val spacing = RafiqSpacing()
        assertEquals(0.dp, spacing.none)
        assertEquals(2.dp, spacing.xxs)
        assertEquals(4.dp, spacing.xs)
        assertEquals(8.dp, spacing.s)
        assertEquals(12.dp, spacing.m)
        assertEquals(16.dp, spacing.l)
        assertEquals(24.dp, spacing.xl)
        assertEquals(32.dp, spacing.xxl)
        assertEquals(48.dp, spacing.huge)

        assertTrue(spacing.none < spacing.xxs)
        assertTrue(spacing.xxs < spacing.xs)
        assertTrue(spacing.xs < spacing.s)
        assertTrue(spacing.s < spacing.m)
        assertTrue(spacing.m < spacing.l)
        assertTrue(spacing.l < spacing.xl)
        assertTrue(spacing.xl < spacing.xxl)
        assertTrue(spacing.xxl < spacing.huge)
    }

    @Test
    fun testIconSizesTokens() {
        val icons = RafiqIconSizes()
        assertEquals(16.dp, icons.xs)
        assertEquals(20.dp, icons.s)
        assertEquals(24.dp, icons.m)
        assertEquals(32.dp, icons.l)
        assertEquals(48.dp, icons.xl)

        assertTrue(icons.xs < icons.s)
        assertTrue(icons.s < icons.m)
        assertTrue(icons.m < icons.l)
        assertTrue(icons.l < icons.xl)
    }

    @Test
    fun testCornerRadiusTokens() {
        val corners = RafiqCornerRadius()
        assertEquals(4.dp, corners.xs)
        assertEquals(8.dp, corners.s)
        assertEquals(12.dp, corners.m)
        assertEquals(16.dp, corners.l)
        assertEquals(22.dp, corners.xl)
        assertEquals(999.dp, corners.pill)

        assertTrue(corners.xs < corners.s)
        assertTrue(corners.s < corners.m)
        assertTrue(corners.m < corners.l)
        assertTrue(corners.l < corners.xl)
        assertTrue(corners.xl < corners.pill)
    }

    @Test
    fun testElevationTokens() {
        val elevation = RafiqElevation()
        assertEquals(0.dp, elevation.none)
        assertEquals(1.dp, elevation.low)
        assertEquals(2.dp, elevation.default)
        assertEquals(4.dp, elevation.high)
        assertEquals(8.dp, elevation.modal)

        assertTrue(elevation.none < elevation.low)
        assertTrue(elevation.low < elevation.default)
        assertTrue(elevation.default < elevation.high)
        assertTrue(elevation.high < elevation.modal)
    }

    @Test
    fun testExtendedColorsPalettes() {
        val light = LightRafiqExtendedColors
        val dark = DarkRafiqExtendedColors

        assertNotNull(light.quranPrimary)
        assertNotNull(dark.quranPrimary)
        assertNotNull(light.goldAccent)
        assertNotNull(dark.goldAccent)
        assertNotNull(light.badgeFasting)
        assertNotNull(dark.badgeFasting)
        assertNotNull(light.badgeHoliday)
        assertNotNull(dark.badgeHoliday)
        assertNotNull(light.heroPrayerGradient)
        assertNotNull(dark.heroPrayerGradient)
    }

    @Test
    fun testArabicTypographyRtlAndScale() {
        val arabic = RafiqArabicTypography()

        assertEquals(TextDirection.Rtl, arabic.display.textDirection)
        assertEquals(TextDirection.Rtl, arabic.headline.textDirection)
        assertEquals(TextDirection.Rtl, arabic.title.textDirection)
        assertEquals(TextDirection.Rtl, arabic.body.textDirection)
        assertEquals(TextDirection.Rtl, arabic.bismillah.textDirection)
        assertEquals(TextAlign.Center, arabic.bismillah.textAlign)

        assertEquals(32.sp, arabic.display.fontSize)
        assertEquals(26.sp, arabic.headline.fontSize)
        assertEquals(22.sp, arabic.title.fontSize)
        assertEquals(18.sp, arabic.body.fontSize)

        assertTrue(arabic.display.fontSize.value > arabic.headline.fontSize.value)
        assertTrue(arabic.headline.fontSize.value > arabic.title.fontSize.value)
        assertTrue(arabic.title.fontSize.value > arabic.body.fontSize.value)

        // Ensure line height accommodates tashkeel diacritics
        assertTrue(arabic.display.lineHeight.value > arabic.display.fontSize.value)
        assertTrue(arabic.headline.lineHeight.value > arabic.headline.fontSize.value)
        assertTrue(arabic.title.lineHeight.value > arabic.title.fontSize.value)
        assertTrue(arabic.body.lineHeight.value > arabic.body.fontSize.value)

        assertEquals(ArabicFontFamily, arabic.display.fontFamily)
        assertEquals(ArabicFontFamily, arabic.body.fontFamily)
    }

    @Test
    fun testMaterialTypographyHierarchy() {
        assertEquals(16.sp, Typography.bodyLarge.fontSize)
        assertEquals(14.sp, Typography.bodyMedium.fontSize)
        assertEquals(12.sp, Typography.bodySmall.fontSize)
        assertEquals(22.sp, Typography.titleLarge.fontSize)
        assertEquals(16.sp, Typography.titleMedium.fontSize)
    }
}
