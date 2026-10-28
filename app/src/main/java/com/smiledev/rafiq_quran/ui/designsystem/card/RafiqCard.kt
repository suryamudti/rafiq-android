package com.smiledev.rafiq_quran.ui.designsystem.card

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable

@Composable
fun RafiqCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RafiqTheme.customShapes.medium,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = RafiqTheme.elevation.low),
    border: BorderStroke? = null,
    contentPadding: Dp = RafiqTheme.spacing.l,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val animatedBorderColor by animateColorAsState(
            targetValue = if (isPressed) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                RafiqTheme.extendedColors.cardBorder
            },
            animationSpec = tween(durationMillis = RafiqMotion.DurationFast, easing = EaseOutCubic),
            label = "rafiq_card_border_highlight"
        )
        val activeBorder = border ?: BorderStroke(1.dp, animatedBorderColor)

        Card(
            onClick = onClick,
            modifier = modifier.rafiqPressable(
                interactionSource = interactionSource,
                targetScale = RafiqMotion.CardPressScale,
                enabled = true
            ),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = activeBorder,
            interactionSource = interactionSource
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun RafiqOutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RafiqTheme.customShapes.medium,
    colors: CardColors = CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    border: BorderStroke = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
    contentPadding: Dp = RafiqTheme.spacing.l,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val animatedBorderColor by animateColorAsState(
            targetValue = if (isPressed) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                RafiqTheme.extendedColors.cardBorder
            },
            animationSpec = tween(durationMillis = RafiqMotion.DurationFast, easing = EaseOutCubic),
            label = "rafiq_outlined_card_border_highlight"
        )

        OutlinedCard(
            onClick = onClick,
            modifier = modifier.rafiqPressable(
                interactionSource = interactionSource,
                targetScale = RafiqMotion.CardPressScale,
                enabled = true
            ),
            shape = shape,
            colors = colors,
            border = BorderStroke(1.dp, animatedBorderColor),
            interactionSource = interactionSource
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    } else {
        OutlinedCard(
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}
