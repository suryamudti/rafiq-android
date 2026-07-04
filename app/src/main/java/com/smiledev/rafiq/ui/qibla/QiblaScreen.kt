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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

private const val KAABA_LAT = 21.4225
private const val KAABA_LON = 39.8262

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userLat = -6.2088
    val userLon = 106.8456
    val bearing = calculateBearing(userLat, userLon, KAABA_LAT, KAABA_LON)
    val distanceKm = calculateDistance(userLat, userLon, KAABA_LAT, KAABA_LON)

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
                text = "$bearing° from North",
                fontSize = 16.sp,
                color = Color(0xFF009688),
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(16.dp))

            QiblaCompass(bearing = bearing.toFloat())

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Distance to Mecca: $distanceKm km",
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

            val arrowTip = Offset(
                center.x + arrowLength * sin(arrowAngle).toFloat(),
                center.y - arrowLength * cos(arrowAngle).toFloat()
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

private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val phi1 = lat1 * PI / 180
    val phi2 = lat2 * PI / 180
    val deltaLambda = (lon2 - lon1) * PI / 180

    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

    val bearing = atan2(y, x) * 180 / PI
    return ((bearing + 360) % 360).roundToInt()
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val r = 6371.0
    val dLat = (lat2 - lat1) * PI / 180
    val dLon = (lon2 - lon1) * PI / 180
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180) * cos(lat2 * PI / 180) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (r * c).roundToInt()
}
