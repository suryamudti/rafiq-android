package com.smiledev.rafiq_quran.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    outline = DarkOutline,
    error = DarkError,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    outline = LightOutline,
    error = LightError,
)

@Composable
fun RafiqAppTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val extendedColors = if (darkTheme) DarkRafiqExtendedColors else LightRafiqExtendedColors
    val spacing = RafiqSpacing()
    val iconSizes = RafiqIconSizes()
    val cornerRadius = RafiqCornerRadius()
    val elevation = RafiqElevation()
    val arabicTypography = RafiqArabicTypography()
    val customShapes = RafiqCustomShapes()

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalIconSizes provides iconSizes,
        LocalCornerRadius provides cornerRadius,
        LocalElevation provides elevation,
        LocalRafiqExtendedColors provides extendedColors,
        LocalRafiqArabicTypography provides arabicTypography,
        LocalRafiqShapes provides customShapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
