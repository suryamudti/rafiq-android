package com.smiledev.rafiq_quran.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable

fun Modifier.tactilePress(
    targetScale: Float = RafiqMotion.CardPressScale,
    enabled: Boolean = true
): Modifier = this.rafiqPressable(targetScale = targetScale, enabled = enabled)

fun Modifier.tactilePress(
    interactionSource: InteractionSource,
    targetScale: Float = RafiqMotion.CardPressScale,
    enabled: Boolean = true
): Modifier = this.rafiqPressable(
    interactionSource = interactionSource,
    targetScale = targetScale,
    enabled = enabled
)

@Composable
fun rememberTactileScale(
    interactionSource: InteractionSource,
    targetScale: Float = RafiqMotion.CardPressScale
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = RafiqMotion.TactileSpringSpec,
        label = "tactile_scale"
    )
}
