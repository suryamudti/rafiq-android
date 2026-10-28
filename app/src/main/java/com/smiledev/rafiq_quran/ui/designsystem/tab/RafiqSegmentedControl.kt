package com.smiledev.rafiq_quran.ui.designsystem.tab

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable

/**
 * Apple-style segmented control with tactile spring press feedback
 * and smooth color transitions following Emil Kowalski's design guidelines.
 */
@Composable
fun RafiqSegmentedControl(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.xs),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                val animatedBgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = tween(durationMillis = RafiqMotion.DurationNormal, easing = EaseOutCubic),
                    label = "segmented_bg_color"
                )
                val animatedTextColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = RafiqMotion.DurationNormal, easing = EaseOutCubic),
                    label = "segmented_text_color"
                )
                val interactionSource = remember { MutableInteractionSource() }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .rafiqPressable(
                            interactionSource = interactionSource,
                            targetScale = RafiqMotion.ChipPressScale,
                            enabled = true
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTabSelected(index) }
                        ),
                    shape = RoundedCornerShape(20.dp),
                    color = animatedBgColor,
                    shadowElevation = if (isSelected) 1.dp else 0.dp
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = animatedTextColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}
