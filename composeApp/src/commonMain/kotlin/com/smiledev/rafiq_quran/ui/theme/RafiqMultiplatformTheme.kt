package com.smiledev.rafiq_quran.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val PrimaryTeal = Color(0xFF0F766E)
val PrimaryTealDark = Color(0xFF14B8A6)
val SecondaryGold = Color(0xFFD97706)
val SurfaceLight = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF0F172A)
val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF020617)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryGold,
    surface = SurfaceLight,
    background = BackgroundLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF0F172A),
    onBackground = Color(0xFF0F172A)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTealDark,
    secondary = SecondaryGold,
    surface = SurfaceDark,
    background = BackgroundDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onSurface = Color(0xFFF8FAFC),
    onBackground = Color(0xFFF8FAFC)
)

val RafiqShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun RafiqMultiplatformTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = RafiqShapes,
        content = content
    )
}
