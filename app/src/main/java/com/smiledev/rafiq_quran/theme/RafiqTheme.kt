package com.smiledev.rafiq_quran.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

object RafiqTheme {
    val spacing: RafiqSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val iconSizes: RafiqIconSizes
        @Composable
        @ReadOnlyComposable
        get() = LocalIconSizes.current

    val cornerRadius: RafiqCornerRadius
        @Composable
        @ReadOnlyComposable
        get() = LocalCornerRadius.current

    val elevation: RafiqElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalElevation.current

    val extendedColors: RafiqExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRafiqExtendedColors.current

    val arabicTypography: RafiqArabicTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalRafiqArabicTypography.current

    val customShapes: RafiqCustomShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalRafiqShapes.current

    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes
}
