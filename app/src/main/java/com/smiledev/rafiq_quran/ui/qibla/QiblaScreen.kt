package com.smiledev.rafiq_quran.ui.qibla

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import kotlin.math.*
import java.util.Locale

private const val KAABA_LAT = 21.4225
private const val KAABA_LON = 39.8262
private const val ALIGNED_THRESHOLD_DEGREES = 5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onBack: () -> Unit,
    viewModel: QiblaViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var deviceAzimuth by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthRad = orientation[0]
                    deviceAzimuth = ((Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360) % 360)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val offset = normalizeAngle180((state.bearing - deviceAzimuth).toDouble())
    val isAligned = abs(offset) <= ALIGNED_THRESHOLD_DEGREES
    val directionName = compassDirection(state.bearing)
    val distanceFormatted = String.format(Locale.US, "%,d", state.distanceKm)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qibla_compass)) },
                navigationIcon = {
                    Text(stringResource(R.string.back), modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
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
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.qibla_direction),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.bearing_from_north, state.bearing.toInt()),
                fontSize = 16.sp,
                color = Color(0xFF009688),
                fontWeight = FontWeight.Medium
            )

            Text(
                text = directionName,
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(16.dp))

            QiblaCompass(bearing = state.bearing.toFloat(), azimuth = deviceAzimuth)

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.qibla_heading, deviceAzimuth.roundToInt()),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.qibla_bearing, state.bearing.toInt()),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(8.dp))

            val offsetText = when {
                isAligned -> stringResource(R.string.facing_qibla)
                offset > 0 -> stringResource(R.string.turn_right, abs(offset).roundToInt())
                else -> stringResource(R.string.turn_left, abs(offset).roundToInt())
            }
            Text(
                text = offsetText,
                fontSize = 16.sp,
                color = if (isAligned) Color(0xFF009688) else Color(0xFFFF9800),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.distance_to_mecca, state.distanceKm.toInt()),
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(16.dp))

            KaabaInfoCard(
                userLat = state.userLat,
                userLon = state.userLon,
                distanceKm = distanceFormatted,
                kaabaLabel = stringResource(R.string.kaaba_info),
                onOpenMaps = { label ->
                    runCatching {
                        val uri = Uri.parse("geo:$KAABA_LAT,$KAABA_LON?q=$KAABA_LAT,$KAABA_LON($label)")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.calibrate_compass),
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KaabaInfoCard(
    userLat: Double,
    userLon: Double,
    distanceKm: String,
    kaabaLabel: String,
    onOpenMaps: (label: String) -> Unit
) {
    val userLatFormatted = String.format(Locale.US, "%.4f", userLat)
    val userLonFormatted = String.format(Locale.US, "%.4f", userLon)
    val kaabaLatFormatted = String.format(Locale.US, "%.4f", KAABA_LAT)
    val kaabaLonFormatted = String.format(Locale.US, "%.4f", KAABA_LON)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.kaaba_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.kaaba_coords, kaabaLatFormatted, kaabaLonFormatted),
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.your_coords, userLatFormatted, userLonFormatted),
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.distance_to_mecca_formatted, distanceKm),
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onOpenMaps(kaabaLabel) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.open_in_maps))
            }
        }
    }
}

@Composable
private fun QiblaCompass(bearing: Float, azimuth: Float) {
    Box(
        modifier = Modifier
            .size(300.dp)
            .graphicsLayer {
                rotationZ = -azimuth
            },
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
            text = stringResource(R.string.north),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757575),
            modifier = Modifier.padding(bottom = 260.dp)
        )

        Text(
            text = stringResource(R.string.south),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757575),
            modifier = Modifier.padding(top = 260.dp)
        )

        Text(
            text = stringResource(R.string.east),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757575),
            modifier = Modifier.padding(start = 260.dp)
        )

        Text(
            text = stringResource(R.string.west),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757575),
            modifier = Modifier.padding(end = 260.dp)
        )
    }
}
