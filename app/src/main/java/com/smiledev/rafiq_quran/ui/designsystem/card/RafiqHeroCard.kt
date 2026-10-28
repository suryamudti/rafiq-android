package com.smiledev.rafiq_quran.ui.designsystem.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable

@Composable
fun RafiqHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gradient: Brush = RafiqTheme.extendedColors.heroPrayerGradient,
    shape: Shape = RafiqTheme.customShapes.extraLarge,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    // Subtle top border highlight simulating light catching physical material
    val topHighlightBorder = BorderStroke(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    )

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .rafiqPressable(
                    interactionSource = interactionSource,
                    targetScale = RafiqMotion.CardPressScale,
                    enabled = true
                ),
            shape = shape,
            border = topHighlightBorder,
            elevation = CardDefaults.cardElevation(defaultElevation = RafiqTheme.elevation.high),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            interactionSource = interactionSource
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(contentPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content
                )
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            border = topHighlightBorder,
            elevation = CardDefaults.cardElevation(defaultElevation = RafiqTheme.elevation.high),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(contentPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content
                )
            }
        }
    }
}
