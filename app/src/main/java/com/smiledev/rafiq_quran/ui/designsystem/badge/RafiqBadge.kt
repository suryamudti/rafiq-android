package com.smiledev.rafiq_quran.ui.designsystem.badge

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smiledev.rafiq_quran.theme.RafiqTheme

@Composable
fun RafiqBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    shape: Shape = RafiqTheme.customShapes.pill
) {
    Surface(
        shape = shape,
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = RafiqTheme.spacing.m, vertical = RafiqTheme.spacing.xs)
        )
    }
}

@Composable
fun RafiqDateBadge(
    hijriDate: String,
    gregorianDate: String,
    modifier: Modifier = Modifier
) {
    val displayDate = if (hijriDate.isNotBlank()) {
        "$hijriDate • $gregorianDate"
    } else {
        gregorianDate
    }
    RafiqBadge(
        text = displayDate,
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
    )
}
