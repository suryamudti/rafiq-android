package com.smiledev.rafiq_quran.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class RafiqExtendedColors(
    val quranPrimary: Color,
    val quranContainer: Color,
    val quranOnContainer: Color,
    val goldAccent: Color,
    val goldContainer: Color,
    val goldOnContainer: Color,
    val hadithBrown: Color,
    val hadithContainer: Color,
    val prophetOrange: Color,
    val prophetContainer: Color,
    val badgeFasting: Color,
    val badgeHoliday: Color,
    val badgeObligatory: Color,
    val badgeSunnah: Color,
    val badgeNeutral: Color,
    val cardBorder: Color,
    val heroPrayerGradient: Brush,
    val quranGradient: Brush,
    val amberGradient: Brush
)

val LightRafiqExtendedColors = RafiqExtendedColors(
    quranPrimary = Color(0xFF00796B),
    quranContainer = Color(0xFFE0F2F1),
    quranOnContainer = Color(0xFF004D40),
    goldAccent = Color(0xFFFFA000),
    goldContainer = Color(0xFFFEF3C7),
    goldOnContainer = Color(0xFF78350F),
    hadithBrown = Color(0xFF78350F),
    hadithContainer = Color(0xFFFFFBEB),
    prophetOrange = Color(0xFFB45309),
    prophetContainer = Color(0xFFFEF3C7),
    badgeFasting = Color(0xFF7B1FA2),
    badgeHoliday = Color(0xFFB8860B),
    badgeObligatory = Color(0xFFD32F2F),
    badgeSunnah = Color(0xFF00796B),
    badgeNeutral = Color(0xFF607D8B),
    cardBorder = Color(0xFFE0E0E0),
    heroPrayerGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF00695C),
            Color(0xFF00897B),
            Color(0xFF004D40)
        )
    ),
    quranGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00796B),
            Color(0xFF004D40)
        )
    ),
    amberGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFF59E0B),
            Color(0xFFD97706)
        )
    )
)

val DarkRafiqExtendedColors = RafiqExtendedColors(
    quranPrimary = Color(0xFF80CBC4),
    quranContainer = Color(0xFF004D40),
    quranOnContainer = Color(0xFFE0F2F1),
    goldAccent = Color(0xFFFFE082),
    goldContainer = Color(0xFF452B00),
    goldOnContainer = Color(0xFFFEF3C7),
    hadithBrown = Color(0xFFD7CCC8),
    hadithContainer = Color(0xFF3E2723),
    prophetOrange = Color(0xFFFFB74D),
    prophetContainer = Color(0xFF4E2C00),
    badgeFasting = Color(0xFFCE93D8),
    badgeHoliday = Color(0xFFFFD54F),
    badgeObligatory = Color(0xFFEF9A9A),
    badgeSunnah = Color(0xFF80CBC4),
    badgeNeutral = Color(0xFFB0BEC5),
    cardBorder = Color(0xFF373737),
    heroPrayerGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF004D40),
            Color(0xFF00695C),
            Color(0xFF00332C)
        )
    ),
    quranGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF004D40),
            Color(0xFF002822)
        )
    ),
    amberGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFB45309),
            Color(0xFF78350F)
        )
    )
)

val LocalRafiqExtendedColors = staticCompositionLocalOf { LightRafiqExtendedColors }
