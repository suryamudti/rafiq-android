package com.smiledev.rafiq_quran.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.ui.theme.rafiqPressable

/**
 * Multiplatform Pill Badge with Specular Border & Continuous Geometry
 */
@Composable
fun RafiqSharedBadge(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    contentColor: Color = MaterialTheme.colorScheme.primary,
    border: BorderStroke? = BorderStroke(1.dp, contentColor.copy(alpha = 0.22f)),
    onClick: (() -> Unit)? = null
) {
    val badgeModifier = modifier
        .then(
            if (onClick != null) {
                Modifier.rafiqPressable(targetScale = 0.94f, onClick = onClick)
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(color = containerColor)
        .then(
            if (border != null) {
                Modifier.border(border, shape)
            } else {
                Modifier
            }
        )
        .padding(horizontal = 10.dp, vertical = 4.dp)

    Box(
        modifier = badgeModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
    }
}
