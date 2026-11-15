package com.smiledev.rafiq_quran.wear.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.smiledev.rafiq_quran.wear.R
import com.smiledev.rafiq_quran.wear.theme.WearColors
import com.smiledev.rafiq_quran.wear.ui.components.wearRotaryScroll
import com.smiledev.rafiq_quran.wear.ui.prayertimes.WearPrayerTimesViewModel
import com.smiledev.rafiq_quran.wear.widget.WearNextPrayerCalculator

@Composable
fun WearWatchfaceWidgetScreen(
    onOpenPrayerTimes: () -> Unit = {},
    viewModel: WearPrayerTimesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    var syncedRecently by remember { mutableStateOf(false) }

    val fallbackSnapshot = remember { WearNextPrayerCalculator.defaultPreviewSnapshot() }
    val nextName = state.nextPrayerName.ifBlank { fallbackSnapshot.nextPrayerName }
    val nextTime = state.nextPrayerTime.ifBlank { fallbackSnapshot.nextPrayerTime }
    val countdown = state.countdown.ifBlank { fallbackSnapshot.countdown }
    val cityName = state.cityName.ifBlank { fallbackSnapshot.cityName }
    val nextEntry = state.prayerTimes.firstOrNull { it.isNext }
    val iconRes = if (nextEntry != null && nextEntry.iconResId != 0) {
        nextEntry.iconResId
    } else {
        fallbackSnapshot.iconResId
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(WearColors.DeepNavy)
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .wearRotaryScroll(listState),
            state = listState,
            autoCentering = null,
            contentPadding = PaddingValues(
                top = 28.dp,
                start = 10.dp,
                end = 10.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.wear_watchface_preview_header),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WearColors.GoldAccent,
                        letterSpacing = 1.1.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.wear_watchface_widget_subtitle),
                        fontSize = 9.sp,
                        color = WearColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Watchface Complication Slots Preview (Ranged Arc + Short Text)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ranged Value Complication Slot
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .background(WearColors.SurfaceDark, RoundedCornerShape(14.dp))
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(54.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = 0.68f,
                                modifier = Modifier.fillMaxSize(),
                                startAngle = 295f,
                                endAngle = 245f,
                                indicatorColor = WearColors.TealBright,
                                trackColor = WearColors.SurfaceCard,
                                strokeWidth = 4.dp
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = nextName.take(5),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WearColors.TealBright,
                                    maxLines = 1
                                )
                                Text(
                                    text = nextTime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WearColors.TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.wear_complication_ranged),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = WearColors.TextMuted
                        )
                    }

                    // Short Text Complication Slot
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .background(WearColors.SurfaceDark, RoundedCornerShape(14.dp))
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(54.dp)
                                .border(1.5.dp, WearColors.GoldAccent.copy(alpha = 0.6f), CircleShape)
                                .background(WearColors.SurfaceCard, CircleShape)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = nextName,
                                    tint = WearColors.GoldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = nextTime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WearColors.TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = nextName.take(6),
                                    fontSize = 8.sp,
                                    color = WearColors.TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.wear_complication_short),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = WearColors.TextMuted
                        )
                    }
                }
            }

            // Long Text Watchface Complication Pill Preview
            item {
                Card(
                    onClick = onOpenPrayerTimes,
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = WearColors.SurfaceCard,
                        endBackgroundColor = WearColors.SurfaceDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.wear_complication_long),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = WearColors.TealBright
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = nextName,
                                    tint = WearColors.TealBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "$nextName • $nextTime",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WearColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "-$countdown • $cityName",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WearColors.GoldAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sync Watchface Button
            item {
                CompactChip(
                    onClick = {
                        viewModel.syncWatchfaceWidget()
                        syncedRecently = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.wear_sync_watchface),
                            tint = if (syncedRecently) Color.Black else WearColors.TealBright,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(
                                if (syncedRecently) {
                                    R.string.wear_watchface_synced
                                } else {
                                    R.string.wear_sync_watchface
                                }
                            ),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (syncedRecently) WearColors.TealPrimary else WearColors.SurfaceDark,
                        contentColor = if (syncedRecently) Color.Black else WearColors.TextPrimary
                    )
                )
            }

            // Setup Instructions
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WearColors.SurfaceDark, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.wear_watchface_setup_hint),
                        fontSize = 9.sp,
                        color = WearColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
