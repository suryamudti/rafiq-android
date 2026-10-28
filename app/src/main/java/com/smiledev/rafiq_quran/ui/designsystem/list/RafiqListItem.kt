package com.smiledev.rafiq_quran.ui.designsystem.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable

@Composable
fun RafiqListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val backgroundColor by animateColorAsState(
            targetValue = if (isPressed) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            } else {
                Color.Transparent
            },
            animationSpec = tween(durationMillis = RafiqMotion.DurationFast, easing = EaseOutCubic),
            label = "rafiq_list_item_bg"
        )

        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RafiqTheme.customShapes.small)
                .background(backgroundColor)
                .rafiqPressable(
                    interactionSource = interactionSource,
                    targetScale = RafiqMotion.CardPressScale,
                    enabled = true
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Box(contentAlignment = Alignment.Center) {
                    leading()
                }
                Spacer(modifier = Modifier.width(RafiqTheme.spacing.l))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(RafiqTheme.spacing.xxs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = 0.15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(RafiqTheme.spacing.m))
                Box(contentAlignment = Alignment.CenterEnd) {
                    trailing()
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Box(contentAlignment = Alignment.Center) {
                    leading()
                }
                Spacer(modifier = Modifier.width(RafiqTheme.spacing.l))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(RafiqTheme.spacing.xxs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = 0.15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(RafiqTheme.spacing.m))
                Box(contentAlignment = Alignment.CenterEnd) {
                    trailing()
                }
            }
        }
    }
}
