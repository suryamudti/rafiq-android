package com.smiledev.rafiq_quran.ui.qibla

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqBadge
import com.smiledev.rafiq_quran.ui.designsystem.button.RafiqButton
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    val directionName = compassDirection(state.bearing, currentLocaleCode())
    val distanceFormatted = String.format(Locale.US, "%,d", state.distanceKm)
    val offsetText = when {
        isAligned -> stringResource(R.string.facing_qibla)
        offset > 0 -> stringResource(R.string.turn_right, abs(offset).roundToInt())
        else -> stringResource(R.string.turn_left, abs(offset).roundToInt())
    }

    Scaffold(
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.qibla_compass),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Direction Title and Info Header
            Text(
                text = stringResource(R.string.qibla_direction),
                style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = (-0.15).sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.bearing_from_north, state.bearing.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = directionName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(RafiqTheme.spacing.l))

            // Compass Area with spring bounce physics on alignment
            QiblaCompass(
                bearing = state.bearing.toFloat(),
                azimuth = deviceAzimuth,
                isAligned = isAligned,
                offsetText = offsetText
            )

            Spacer(Modifier.height(RafiqTheme.spacing.l))

            // Heading & Target Bearing Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.qibla_heading, deviceAzimuth.roundToInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.qibla_bearing, state.bearing.toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(Modifier.height(RafiqTheme.spacing.m))

            // Alignment Status Badge
            RafiqBadge(
                text = offsetText,
                containerColor = if (isAligned) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                },
                contentColor = if (isAligned) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                },
                border = BorderStroke(
                    1.dp,
                    if (isAligned) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                )
            )

            Spacer(Modifier.height(RafiqTheme.spacing.s))

            Text(
                text = stringResource(R.string.distance_to_mecca, state.distanceKm.toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(RafiqTheme.spacing.l))

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

            Spacer(Modifier.height(RafiqTheme.spacing.m))

            Text(
                text = stringResource(R.string.calibrate_compass),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(RafiqTheme.spacing.m))
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

    RafiqCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.kaaba_info),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.1).sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(RafiqTheme.spacing.s))
            Text(
                text = stringResource(R.string.kaaba_coords, kaabaLatFormatted, kaabaLonFormatted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.your_coords, userLatFormatted, userLonFormatted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.distance_to_mecca_formatted, distanceKm),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(RafiqTheme.spacing.m))
            RafiqButton(
                onClick = { onOpenMaps(kaabaLabel) },
                text = stringResource(R.string.open_in_maps),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun QiblaCompass(
    bearing: Float,
    azimuth: Float,
    modifier: Modifier = Modifier,
    isAligned: Boolean = false,
    offsetText: String = ""
) {
    val compassDescription = if (isAligned) {
        stringResource(R.string.qibla_compass_aligned_desc, bearing.roundToInt())
    } else {
        stringResource(
            R.string.qibla_compass_desc,
            bearing.roundToInt(),
            azimuth.roundToInt(),
            offsetText
        )
    }

    // Emil Kowalski spring motion: subtle scale pop when alignment state snaps
    val alignmentScale by animateFloatAsState(
        targetValue = if (isAligned) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 400f
        ),
        label = "qibla_aligned_scale"
    )

    val dialBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val targetRingColor = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val ringColor by animateColorAsState(
        targetValue = targetRingColor,
        animationSpec = tween(durationMillis = RafiqMotion.DurationNormal, easing = EaseOutCubic),
        label = "qibla_ring_color"
    )
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val needleColor = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val cardinalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val northColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(300.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = compassDescription
            }
            .graphicsLayer {
                scaleX = alignmentScale
                scaleY = alignmentScale
                rotationZ = -azimuth
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(280.dp).clearAndSetSemantics { }) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2

            // Compass Dial Background
            drawCircle(color = dialBackgroundColor, radius = radius, center = center)
            drawCircle(
                color = ringColor,
                radius = radius - 2.dp.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (isAligned) 3.dp.toPx() else 2.dp.toPx())
            )

            // Degree Tick Marks (every 30 deg)
            for (i in 0 until 360 step 30) {
                val angle = i * PI / 180
                val inner = radius * 0.85f
                val outer = radius * 0.92f
                drawLine(
                    color = tickColor,
                    start = Offset(center.x + inner * cos(angle).toFloat(), center.y + inner * sin(angle).toFloat()),
                    end = Offset(center.x + outer * cos(angle).toFloat(), center.y + outer * sin(angle).toFloat()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Qibla Needle pointing towards Kaaba bearing
            val arrowAngle = (bearing - 180) * PI / 180
            val arrowLength = radius * 0.65f

            drawLine(
                color = needleColor,
                start = center,
                end = Offset(
                    center.x + arrowLength * sin(arrowAngle).toFloat(),
                    center.y - arrowLength * cos(arrowAngle).toFloat()
                ),
                strokeWidth = 6.dp.toPx()
            )

            // Center Pivot Hub
            drawCircle(color = needleColor, radius = 9.dp.toPx(), center = center)
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = center)
        }

        Text(
            text = stringResource(R.string.north),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = northColor,
            modifier = Modifier
                .padding(bottom = 260.dp)
                .clearAndSetSemantics { }
        )

        Text(
            text = stringResource(R.string.south),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = cardinalColor,
            modifier = Modifier
                .padding(top = 260.dp)
                .clearAndSetSemantics { }
        )

        Text(
            text = stringResource(R.string.east),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = cardinalColor,
            modifier = Modifier
                .padding(start = 260.dp)
                .clearAndSetSemantics { }
        )

        Text(
            text = stringResource(R.string.west),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = cardinalColor,
            modifier = Modifier
                .padding(end = 260.dp)
                .clearAndSetSemantics { }
        )
    }
}
