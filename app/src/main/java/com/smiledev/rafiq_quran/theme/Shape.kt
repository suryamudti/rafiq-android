package com.smiledev.rafiq_quran.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Emil Kowalski design engineering shape system with refined, harmonic corner radiuses.
 */
@Immutable
data class RafiqCustomShapes(
    val extraSmall: CornerBasedShape = RoundedCornerShape(6.dp),
    val small: CornerBasedShape = RoundedCornerShape(10.dp),
    val medium: CornerBasedShape = RoundedCornerShape(16.dp),
    val large: CornerBasedShape = RoundedCornerShape(20.dp),
    val extraLarge: CornerBasedShape = RoundedCornerShape(26.dp),
    val pill: CornerBasedShape = CircleShape
)

val LocalRafiqShapes = staticCompositionLocalOf { RafiqCustomShapes() }
