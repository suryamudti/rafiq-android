package com.smiledev.rafiq_quran.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smiledev.rafiq_quran.domain.model.Mosque

@Composable
expect fun MosqueMapView(
    mosques: List<Mosque>,
    userLat: Double?,
    userLon: Double?,
    selectedMosque: Mosque?,
    onMosqueSelected: (Mosque?) -> Unit,
    modifier: Modifier
)
