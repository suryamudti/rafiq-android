package com.smiledev.rafiq_quran.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import com.smiledev.rafiq_quran.wear.R

object WearColors {
    val DeepNavy = Color(0xFF08121E)
    val SurfaceDark = Color(0xFF112236)
    val SurfaceCard = Color(0xFF172D44)
    val TealPrimary = Color(0xFF26A69A)
    val TealBright = Color(0xFF4DB6AC)
    val GoldAccent = Color(0xFFFFD54F)
    val GoldSoft = Color(0xFFFFE082)
    val TextPrimary = Color(0xFFF5F7FA)
    val TextSecondary = Color(0xFFB0BEC5)
    val TextMuted = Color(0xFF78909C)
    val ErrorRed = Color(0xFFEF5350)
}

private val WearColorPalette = Colors(
    primary = WearColors.TealPrimary,
    primaryVariant = WearColors.TealBright,
    secondary = WearColors.GoldAccent,
    secondaryVariant = WearColors.GoldSoft,
    background = WearColors.DeepNavy,
    surface = WearColors.SurfaceDark,
    error = WearColors.ErrorRed,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = WearColors.TextPrimary,
    onSurface = WearColors.TextPrimary,
    onSurfaceVariant = WearColors.TextSecondary,
    onError = Color.Black
)

val WearArabicFontFamily = FontFamily(Font(R.font.me_quran))

@Composable
fun RafiqWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WearColorPalette,
        content = content
    )
}
