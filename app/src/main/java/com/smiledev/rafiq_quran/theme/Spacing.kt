package com.smiledev.rafiq_quran.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class RafiqSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val huge: Dp = 48.dp
)

@Immutable
data class RafiqIconSizes(
    val xs: Dp = 16.dp,
    val s: Dp = 20.dp,
    val m: Dp = 24.dp,
    val l: Dp = 32.dp,
    val xl: Dp = 48.dp
)

@Immutable
data class RafiqCornerRadius(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 22.dp,
    val pill: Dp = 999.dp
)

@Immutable
data class RafiqElevation(
    val none: Dp = 0.dp,
    val low: Dp = 1.dp,
    val default: Dp = 2.dp,
    val high: Dp = 4.dp,
    val modal: Dp = 8.dp
)

val LocalSpacing = staticCompositionLocalOf { RafiqSpacing() }
val LocalIconSizes = staticCompositionLocalOf { RafiqIconSizes() }
val LocalCornerRadius = staticCompositionLocalOf { RafiqCornerRadius() }
val LocalElevation = staticCompositionLocalOf { RafiqElevation() }
