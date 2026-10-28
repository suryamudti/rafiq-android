package com.smiledev.rafiq_quran.ui.designsystem.chip

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable

@Composable
fun RafiqFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surface,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val interactionSource = remember { MutableInteractionSource() }

    val animatedContainerColor by animateColorAsState(
        targetValue = if (selected) selectedContainerColor else unselectedContainerColor,
        animationSpec = tween(durationMillis = RafiqMotion.DurationNormal, easing = EaseOutCubic),
        label = "rafiq_chip_container_color"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = tween(durationMillis = RafiqMotion.DurationNormal, easing = EaseOutCubic),
        label = "rafiq_chip_content_color"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        animationSpec = tween(durationMillis = RafiqMotion.DurationNormal, easing = EaseOutCubic),
        label = "rafiq_chip_border_color"
    )

    Surface(
        modifier = modifier
            .rafiqPressable(
                interactionSource = interactionSource,
                targetScale = RafiqMotion.ChipPressScale,
                enabled = true
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RafiqTheme.customShapes.pill,
        color = animatedContainerColor,
        border = BorderStroke(1.dp, animatedBorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(RafiqTheme.spacing.s))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = animatedContentColor
            )
        }
    }
}
