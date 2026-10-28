package com.smiledev.rafiq_quran.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// Apple Design Cubic Bezier Easings (Emil Kowalski Animation Principles)
val AppleEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val AppleEaseInOut = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
val AppleEaseDrawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

// Fluid Spring Physics Specifications
fun <T> AppleSpringSpec(
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMediumLow
) = spring<T>(
    dampingRatio = dampingRatio,
    stiffness = stiffness
)

val PrimaryTeal = Color(0xFF0F766E)
val PrimaryTealDark = Color(0xFF14B8A6)
val SecondaryGold = Color(0xFFD97706)
val SecondaryGoldLight = Color(0xFFF59E0B)
val EmeraldGreen = Color(0xFF059669)
val SurfaceLight = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF0F172A)
val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF020617)

// Extended Color Palette & Specular Highlights
data class RafiqMultiplatformExtendedColors(
    val cardBorder: Color,
    val cardHighlight: Color,
    val heroQuranGradient: Brush,
    val heroPrayerGradient: Brush,
    val heroGoldGradient: Brush,
    val heroEmeraldGradient: Brush,
    val heroSurfaceGradient: Brush
)

val LocalRafiqExtendedColors = staticCompositionLocalOf {
    RafiqMultiplatformExtendedColors(
        cardBorder = Color.Black.copy(alpha = 0.08f),
        cardHighlight = Color.White.copy(alpha = 0.50f),
        heroQuranGradient = Brush.verticalGradient(listOf(Color(0xFF0F766E), Color(0xFF115E59))),
        heroPrayerGradient = Brush.verticalGradient(listOf(Color(0xFF14B8A6), Color(0xFF0F766E))),
        heroGoldGradient = Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
        heroEmeraldGradient = Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
        heroSurfaceGradient = Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))
    )
}

val LightExtendedColors = RafiqMultiplatformExtendedColors(
    cardBorder = Color.Black.copy(alpha = 0.07f),
    cardHighlight = Color.White.copy(alpha = 0.60f),
    heroQuranGradient = Brush.verticalGradient(listOf(Color(0xFF0F766E), Color(0xFF0D5C56))),
    heroPrayerGradient = Brush.verticalGradient(listOf(Color(0xFF14B8A6), Color(0xFF0F766E))),
    heroGoldGradient = Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
    heroEmeraldGradient = Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
    heroSurfaceGradient = Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFEDF2F7)))
)

val DarkExtendedColors = RafiqMultiplatformExtendedColors(
    cardBorder = Color.White.copy(alpha = 0.12f),
    cardHighlight = Color.White.copy(alpha = 0.08f),
    heroQuranGradient = Brush.verticalGradient(listOf(Color(0xFF134E4A), Color(0xFF042F2E))),
    heroPrayerGradient = Brush.verticalGradient(listOf(Color(0xFF115E59), Color(0xFF0F766E))),
    heroGoldGradient = Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF78350F))),
    heroEmeraldGradient = Brush.verticalGradient(listOf(Color(0xFF065F46), Color(0xFF022C22))),
    heroSurfaceGradient = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryGold,
    surface = SurfaceLight,
    background = BackgroundLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF0F172A),
    onBackground = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF115E59)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTealDark,
    secondary = SecondaryGoldLight,
    surface = SurfaceDark,
    background = BackgroundDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onSurface = Color(0xFFF8FAFC),
    onBackground = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFF5EEAD4)
)

val RafiqShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

/**
 * Tactile Spring Physics Pressable Modifier
 * Scales smoothly to [targetScale] upon press using Apple-style spring physics.
 */
fun Modifier.rafiqPressable(
    targetScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = AppleSpringSpec(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "rafiq_tactile_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}

fun Modifier.bounceClick(
    targetScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier = rafiqPressable(targetScale = targetScale, onClick = onClick)

object RafiqMultiplatformTheme {
    val colors: RafiqMultiplatformExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRafiqExtendedColors.current
}

@Composable
fun RafiqMultiplatformTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalRafiqExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = RafiqShapes,
            content = content
        )
    }
}
