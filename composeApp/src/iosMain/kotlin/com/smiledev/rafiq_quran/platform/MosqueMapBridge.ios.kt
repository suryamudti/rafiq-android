package com.smiledev.rafiq_quran.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smiledev.rafiq_quran.domain.model.Mosque

@Composable
actual fun MosqueMapView(
    mosques: List<Mosque>,
    userLat: Double?,
    userLon: Double?,
    selectedMosque: Mosque?,
    onMosqueSelected: (Mosque?) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Mosque Map iOS (${mosques.size} locations)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
