package com.smiledev.rafiq_quran.tv.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.smiledev.rafiq_quran.tv.R

object TvColors {
    val DeepNavy = Color(0xFF07111C)
    val SurfaceDark = Color(0xFF0F1E30)
    val SurfaceCard = Color(0xFF152940)
    val SurfaceHighlight = Color(0xFF1C3654)
    val TealPrimary = Color(0xFF26A69A)
    val TealBright = Color(0xFF4DB6AC)
    val EmeraldGlow = Color(0xFF00E5B0)
    val GoldAccent = Color(0xFFFFD54F)
    val GoldSoft = Color(0xFFFFE082)
    val TextPrimary = Color(0xFFF5F7FA)
    val TextSecondary = Color(0xFFB0BEC5)
    val TextMuted = Color(0xFF78909C)
    val ErrorRed = Color(0xFFEF5350)
}

private val TvDarkColorScheme = darkColorScheme(
    primary = TvColors.TealPrimary,
    onPrimary = Color.Black,
    secondary = TvColors.GoldAccent,
    onSecondary = Color.Black,
    background = TvColors.DeepNavy,
    onBackground = TvColors.TextPrimary,
    surface = TvColors.SurfaceDark,
    onSurface = TvColors.TextPrimary,
    surfaceVariant = TvColors.SurfaceCard,
    onSurfaceVariant = TvColors.TextSecondary,
    error = TvColors.ErrorRed,
    onError = Color.Black
)

val TvArabicFontFamily = FontFamily(Font(R.font.me_quran))

@Composable
fun RafiqTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        content = content
    )
}
