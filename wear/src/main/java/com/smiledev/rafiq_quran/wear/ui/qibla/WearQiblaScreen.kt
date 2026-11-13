package com.smiledev.rafiq_quran.wear.ui.qibla

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.smiledev.rafiq_quran.wear.BuildConfig
import com.smiledev.rafiq_quran.wear.R
import com.smiledev.rafiq_quran.wear.theme.WearColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun WearQiblaScreen(
    viewModel: WearQiblaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    var deviceAzimuth by remember { mutableFloatStateOf(0f) }
    var simulatedAzimuthOverride by remember { mutableStateOf<Float?>(null) }
    var hasRotationSensor by remember { mutableStateOf(true) }
    var needsCalibration by remember { mutableStateOf(false) }

    val declinationDegrees = remember(state.userLat, state.userLon) {
        runCatching {
            GeomagneticField(
                state.userLat.toFloat(),
                state.userLon.toFloat(),
                0f,
                System.currentTimeMillis()
            ).declination
        }.getOrDefault(0f)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshLocation(forceGeocode = true)
        }
    }

    val onRefreshLocationClick: () -> Unit = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.refreshLocation(forceGeocode = true)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    DisposableEffect(declinationDegrees) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        hasRotationSensor = rotationSensor != null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthRad = orientation[0].toDouble()
                    val magneticDeg = ((azimuthRad * 180.0 / PI) + 360.0) % 360.0
                    val trueNorthDeg = ((magneticDeg + declinationDegrees) % 360.0 + 360.0) % 360.0
                    deviceAzimuth = trueNorthDeg.toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                needsCalibration = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                    accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
            }
        }

        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(
                listener,
                rotationSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val activeAzimuth = if (BuildConfig.DEBUG) {
        simulatedAzimuthOverride ?: deviceAzimuth
    } else {
        deviceAzimuth
    }
    val aligned = isQiblaAligned(state.bearing, activeAzimuth)
    val angleDiff = wearNormalizeAngle180(state.bearing.toDouble() - activeAzimuth.toDouble())

    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(aligned) {
        if (aligned && !wasAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasAligned = aligned
    }

    val statusText = when {
        !hasRotationSensor && simulatedAzimuthOverride == null ->
            stringResource(R.string.wear_compass_unavailable)
        aligned -> stringResource(R.string.wear_facing_qibla)
        angleDiff > 0 -> stringResource(R.string.wear_turn_right, angleDiff.roundToInt())
        else -> stringResource(R.string.wear_turn_left, abs(angleDiff).roundToInt())
    }

    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier
            .fillMaxSize()
            .background(WearColors.DeepNavy)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Rotating compass bezel & needle ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = -activeAzimuth
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f - 4.dp.toPx()

                    // Outer circular bezel rim
                    drawCircle(
                        color = if (aligned) {
                            WearColors.TealBright.copy(alpha = 0.7f)
                        } else {
                            WearColors.GoldAccent.copy(alpha = 0.35f)
                        },
                        radius = radius,
                        center = center,
                        style = Stroke(width = if (aligned) 2.5.dp.toPx() else 1.5.dp.toPx())
                    )

                    // Inner compass guideline
                    drawCircle(
                        color = WearColors.SurfaceCard.copy(alpha = 0.5f),
                        radius = radius * 0.85f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // 360° tick marks every 15° and 30°
                    for (deg in 0 until 360 step 15) {
                        val rad = (deg - 90) * PI / 180.0
                        val cosVal = cos(rad).toFloat()
                        val sinVal = sin(rad).toFloat()
                        val outerPoint = Offset(center.x + radius * cosVal, center.y + radius * sinVal)

                        val (innerRadius, strokeW, tickColor) = when {
                            deg % 90 == 0 -> Triple(
                                radius - 12.dp.toPx(),
                                2.5.dp.toPx(),
                                if (deg == 0) WearColors.GoldAccent else WearColors.TextPrimary
                            )
                            deg % 30 == 0 -> Triple(
                                radius - 8.dp.toPx(),
                                1.5.dp.toPx(),
                                WearColors.TextSecondary.copy(alpha = 0.75f)
                            )
                            else -> Triple(
                                radius - 5.dp.toPx(),
                                1.dp.toPx(),
                                WearColors.TextMuted.copy(alpha = 0.4f)
                            )
                        }

                        val innerPoint = Offset(center.x + innerRadius * cosVal, center.y + innerRadius * sinVal)
                        drawLine(
                            color = tickColor,
                            start = innerPoint,
                            end = outerPoint,
                            strokeWidth = strokeW
                        )
                    }

                    // Glowing Qibla needle pointing to state.bearing
                    rotate(state.bearing.toFloat(), pivot = center) {
                        val needleLength = radius * 0.72f
                        val needleWidth = 10.dp.toPx()
                        val primaryColor = if (aligned) WearColors.TealBright else WearColors.GoldAccent
                        val secondaryColor = if (aligned) WearColors.TealPrimary else WearColors.GoldSoft

                        // Glowing aura around tip when aligned
                        if (aligned) {
                            drawCircle(
                                color = WearColors.TealBright.copy(alpha = 0.35f),
                                radius = 10.dp.toPx(),
                                center = Offset(center.x, center.y - needleLength)
                            )
                        }

                        // Sharp directional needle pointer towards Kaaba
                        val needlePath = Path().apply {
                            moveTo(center.x, center.y - needleLength)
                            lineTo(center.x + needleWidth / 2f, center.y - 12.dp.toPx())
                            lineTo(center.x, center.y - 4.dp.toPx())
                            lineTo(center.x - needleWidth / 2f, center.y - 12.dp.toPx())
                            close()
                        }

                        drawPath(
                            path = needlePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, secondaryColor),
                                startY = center.y - needleLength,
                                endY = center.y
                            )
                        )

                        // Center highlight spine
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(center.x, center.y - needleLength + 2.dp.toPx()),
                            end = Offset(center.x, center.y - 12.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Balanced needle counter-weight
                        val tailPath = Path().apply {
                            moveTo(center.x, center.y + 14.dp.toPx())
                            lineTo(center.x + 3.dp.toPx(), center.y + 4.dp.toPx())
                            lineTo(center.x - 3.dp.toPx(), center.y + 4.dp.toPx())
                            close()
                        }
                        drawPath(
                            path = tailPath,
                            color = WearColors.TextMuted.copy(alpha = 0.5f)
                        )

                        // Tip beacon dot
                        drawCircle(
                            color = primaryColor,
                            radius = 3.5.dp.toPx(),
                            center = Offset(center.x, center.y - needleLength)
                        )

                        // Center pivot hub
                        drawCircle(
                            color = primaryColor,
                            radius = 7.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = WearColors.DeepNavy,
                            radius = 3.5.dp.toPx(),
                            center = center
                        )
                    }
                }

                // Cardinal Markers (N, E, S, W) on rotating ring
                Text(
                    text = "N",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = WearColors.GoldAccent,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 13.dp)
                )
                Text(
                    text = "S",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = WearColors.TextMuted,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 13.dp)
                )
                Text(
                    text = "E",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = WearColors.TextMuted,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 13.dp)
                )
                Text(
                    text = "W",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = WearColors.TextMuted,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 13.dp)
                )
            }

            // Central / Overlay HUD (stationary above rotating bezel)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Top pill: City (tappable to refresh GPS) & Bearing + Direction Cardinal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(
                                color = WearColors.SurfaceDark.copy(alpha = 0.90f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = WearColors.SurfaceCard,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(onClick = onRefreshLocationClick)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (state.isSyncingLocation) {
                                stringResource(R.string.wear_updating_gps)
                            } else {
                                state.cityName
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WearColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 88.dp)
                        )
                        Text(
                            text = "•",
                            fontSize = 9.sp,
                            color = WearColors.TextMuted
                        )
                        Text(
                            text = "${state.bearing}° ${state.directionCardinal}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (aligned) WearColors.TealBright else WearColors.GoldAccent
                        )
                    }

                    if (needsCalibration && simulatedAzimuthOverride == null) {
                        Text(
                            text = stringResource(R.string.wear_calibrate_compass),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = WearColors.GoldSoft,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(
                                    color = WearColors.SurfaceDark.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }

                // Center/bottom status badge + Distance to Mecca
                // In debug builds only, tapping the status pill cycles/clears heading simulation for emulator testing.
                val statusModifier = if (BuildConfig.DEBUG) {
                    Modifier.clickable {
                        simulatedAzimuthOverride = when {
                            simulatedAzimuthOverride == null -> state.bearing.toFloat()
                            aligned -> (state.bearing + 45f) % 360f
                            else -> null
                        }
                    }
                } else {
                    Modifier
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (aligned) {
                                    WearColors.TealPrimary.copy(alpha = 0.25f)
                                } else {
                                    WearColors.SurfaceDark.copy(alpha = 0.90f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (aligned) {
                                    WearColors.TealBright
                                } else {
                                    WearColors.GoldAccent.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .then(statusModifier)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (aligned) WearColors.TealBright else WearColors.GoldAccent,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = stringResource(R.string.wear_distance_km, state.distanceKm),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = WearColors.TextSecondary
                    )
                }
            }
        }
    }
}
