package com.smiledev.rafiq_quran.ui.qibla

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.platform.rememberDeviceAzimuth
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBackButton
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeroCard
import com.smiledev.rafiq_quran.ui.theme.AppleSpringSpec
import com.smiledev.rafiq_quran.ui.theme.PrimaryTeal
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme
import com.smiledev.rafiq_quran.ui.theme.SecondaryGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SharedQiblaScreen(
    viewModel: SharedQiblaViewModel = remember { SharedQiblaViewModel() },
    onBackClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val rawAzimuth by rememberDeviceAzimuth()

    val animatedAzimuth by animateFloatAsState(
        targetValue = rawAzimuth,
        animationSpec = AppleSpringSpec(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessLow
        ),
        label = "azimuth_anim"
    )

    val isAligned = viewModel.isAligned(animatedAzimuth)
    val relativeQiblaAngle = (state.bearing - animatedAzimuth + 360f) % 360f

    val statusColor by animateColorAsState(
        targetValue = if (isAligned) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "status_color"
    )

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = "Qibla Compass",
                subtitle = "${state.cityName} • ${state.distanceKm} km to Makkah",
                navigationIcon = if (onBackClick != null) {
                    { RafiqSharedBackButton(onClick = onBackClick) }
                } else null
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Status Card
            item {
                RafiqSharedHeroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "QIBLA DIRECTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${state.bearing}° ${state.compassDirection}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isAligned)
                                        SecondaryGold
                                    else
                                        Color.White.copy(alpha = 0.20f)
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isAligned) "✓ ALIGNED" else "FINDING",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAligned) Color(0xFF1B1B1B) else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🕋 Kaaba: ${state.kaabaCoordinates}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "${state.distanceKm} km",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryGold
                        )
                    }
                }
            }

            // Visual Compass Rose
            item {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Compass Dial (rotates with device azimuth)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = -animatedAzimuth
                            }
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2 - 12.dp.toPx()

                        // Outer ring
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.25f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 360 Degree Ticks
                        for (i in 0 until 360 step 15) {
                            val angleRad = (i - 90) * PI / 180
                            val isMajor = (i % 90 == 0)
                            val isSemi = (i % 45 == 0)
                            val tickLength = when {
                                isMajor -> 14.dp.toPx()
                                isSemi -> 10.dp.toPx()
                                else -> 6.dp.toPx()
                            }
                            val startX = (center.x + (radius - tickLength) * cos(angleRad)).toFloat()
                            val startY = (center.y + (radius - tickLength) * sin(angleRad)).toFloat()
                            val endX = (center.x + radius * cos(angleRad)).toFloat()
                            val endY = (center.y + radius * sin(angleRad)).toFloat()

                            drawLine(
                                color = if (isMajor) Color.Gray.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.35f),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Rotating Kaaba Pointer Needle (rotates to relativeQiblaAngle)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = relativeQiblaAngle
                            }
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val needleLength = size.minDimension / 2 - 24.dp.toPx()

                        // Golden Qibla Pointer Needle
                        val needlePath = Path().apply {
                            moveTo(center.x, center.y - needleLength)
                            lineTo(center.x - 12.dp.toPx(), center.y - 16.dp.toPx())
                            lineTo(center.x + 12.dp.toPx(), center.y - 16.dp.toPx())
                            close()
                        }
                        drawPath(
                            path = needlePath,
                            color = if (isAligned) PrimaryTeal else SecondaryGold
                        )

                        // Counter needle (southern tail)
                        val tailPath = Path().apply {
                            moveTo(center.x, center.y + needleLength * 0.5f)
                            lineTo(center.x - 8.dp.toPx(), center.y + 12.dp.toPx())
                            lineTo(center.x + 8.dp.toPx(), center.y + 12.dp.toPx())
                            close()
                        }
                        drawPath(
                            path = tailPath,
                            color = Color.Gray.copy(alpha = 0.40f)
                        )
                    }

                    // Center Hub with Alignment Glow
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAligned)
                                    PrimaryTeal
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🕋",
                                fontSize = 24.sp
                            )
                            Text(
                                text = "${state.bearing}°",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAligned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Alignment Status Pill
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isAligned)
                                PrimaryTeal.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isAligned)
                            "✓ Facing the Kaaba (Aligned)"
                        else
                            "Rotate phone until the golden needle points straight up",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Calibration & Tips Card
            item {
                RafiqSharedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "💡 Compass Accuracy Tips",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• Place phone flat on your hand or horizontal surface\n• Move in a figure-8 motion if accuracy seems off\n• Stay away from metal cases, magnets, or electronic devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
