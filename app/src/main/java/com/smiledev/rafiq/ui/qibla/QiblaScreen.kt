package com.smiledev.rafiq.ui.qibla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onBack: () -> Unit,
    viewModel: QiblaViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Qibla Compass") },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Qibla Direction",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${state.bearing}° from North",
                fontSize = 16.sp,
                color = Color(0xFF009688),
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(16.dp))

            QiblaCompass(bearing = state.bearing.toFloat())

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Distance to Mecca: ${state.distanceKm} km",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Move your phone in a figure-8 pattern\nto calibrate the compass",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun QiblaCompass(bearing: Float) {
    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2

            drawCircle(color = Color(0xFFE0E0E0), radius = radius, center = center)
            drawCircle(color = Color(0xFFBDBDBD), radius = radius - 2.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))

            for (i in 0 until 360 step 30) {
                val angle = i * PI / 180
                val inner = radius * 0.85f
                val outer = radius * 0.92f
                drawLine(
                    color = Color(0xFF757575),
                    start = Offset(center.x + inner * cos(angle).toFloat(), center.y + inner * sin(angle).toFloat()),
                    end = Offset(center.x + outer * cos(angle).toFloat(), center.y + outer * sin(angle).toFloat()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            val arrowAngle = (bearing - 180) * PI / 180
            val arrowLength = radius * 0.65f

            drawLine(
                color = Color(0xFF009688),
                start = center,
                end = Offset(
                    center.x + arrowLength * sin(arrowAngle).toFloat(),
                    center.y - arrowLength * cos(arrowAngle).toFloat()
                ),
                strokeWidth = 6.dp.toPx()
            )

            drawCircle(color = Color(0xFFFF9800), radius = 8.dp.toPx(), center = center)
        }

        Text(
            text = "N",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757575),
            modifier = Modifier.padding(bottom = 260.dp)
        )

        Text(
            text = "S",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757575),
            modifier = Modifier.padding(top = 260.dp)
        )

        Text(
            text = "E",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757575),
            modifier = Modifier.padding(start = 260.dp)
        )

        Text(
            text = "W",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757575),
            modifier = Modifier.padding(end = 260.dp)
        )
    }
}
