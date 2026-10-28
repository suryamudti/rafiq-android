package com.smiledev.rafiq_quran.ui.designsystem.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Emil Kowalski design engineering motion system and animation curves.
 * Prioritizes tactile spring physics, instant touch-down responses,
 * and optical easing curves.
 */
object RafiqMotion {
    // Animation Durations (crisp & snappy, strictly <= 300ms)
    const val DurationFast = 150
    const val DurationNormal = 200
    const val DurationMedium = 250
    const val DurationSlow = 300

    // Press scale values tailored per component size
    const val DefaultPressScale = 0.97f
    const val IconButtonPressScale = 0.92f
    const val CardPressScale = 0.985f
    const val ChipPressScale = 0.95f

    // Spring specification for tactile feedback
    val TactileSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Emil Kowalski signature easing curves
    val EaseOutCubic = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val EaseInOutCubic = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
    val EaseDrawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
}

// Top-level aliases for direct usage
val EaseOutCubic = RafiqMotion.EaseOutCubic
val EaseInOutCubic = RafiqMotion.EaseInOutCubic
val EaseDrawer = RafiqMotion.EaseDrawer

/**
 * Emil Kowalski enter transition helper.
 * Principle: NEVER animate from scale(0f); always enter from scale(0.95f) + fadeIn()
 * to prevent unnatural visual pop-ins while maintaining momentum.
 */
fun rafiqEnterTransition(
    initialScale: Float = 0.95f,
    durationMillis: Int = RafiqMotion.DurationNormal,
    easing: Easing = EaseOutCubic
): EnterTransition = fadeIn(
    animationSpec = tween(durationMillis = durationMillis, easing = easing)
) + scaleIn(
    initialScale = initialScale,
    animationSpec = tween(durationMillis = durationMillis, easing = easing)
)

/**
 * Emil Kowalski exit transition helper.
 * Principle: Exit quickly to scale(0.95f) + fadeOut() so dismissals feel crisp and immediate.
 */
fun rafiqExitTransition(
    targetScale: Float = 0.95f,
    durationMillis: Int = RafiqMotion.DurationFast,
    easing: Easing = EaseInOutCubic
): ExitTransition = fadeOut(
    animationSpec = tween(durationMillis = durationMillis, easing = easing)
) + scaleOut(
    targetScale = targetScale,
    animationSpec = tween(durationMillis = durationMillis, easing = easing)
)

/**
 * Modifier for tactile press feedback with zero-delay touch-down response.
 * Uses pointer gestures so touch down responds immediately without scroll-timeout delay,
 * animating with spring physics to the target scale.
 */
fun Modifier.rafiqPressable(
    targetScale: Float = RafiqMotion.DefaultPressScale,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = RafiqMotion.TactileSpringSpec,
        label = "rafiq_pressable_pointer_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * Modifier for tactile press feedback connected to a component's [InteractionSource].
 */
fun Modifier.rafiqPressable(
    interactionSource: InteractionSource,
    targetScale: Float = RafiqMotion.DefaultPressScale,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = RafiqMotion.TactileSpringSpec,
        label = "rafiq_pressable_interaction_scale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Alias for [rafiqPressable] for bounce click feedback.
 */
fun Modifier.bounceClick(
    targetScale: Float = RafiqMotion.DefaultPressScale,
    enabled: Boolean = true
): Modifier = rafiqPressable(targetScale = targetScale, enabled = enabled)

/**
 * Alias for [rafiqPressable] with [InteractionSource].
 */
fun Modifier.bounceClick(
    interactionSource: InteractionSource,
    targetScale: Float = RafiqMotion.DefaultPressScale,
    enabled: Boolean = true
): Modifier = rafiqPressable(interactionSource = interactionSource, targetScale = targetScale, enabled = enabled)
