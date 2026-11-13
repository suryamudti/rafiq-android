package com.smiledev.rafiq_quran.wear.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
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

@Composable
fun WearHomeScreen(
    onNavigatePrayerTimes: () -> Unit,
    onNavigateQuran: () -> Unit,
    onNavigateQibla: () -> Unit,
    prayerViewModel: WearPrayerTimesViewModel = hiltViewModel()
) {
    val prayerState by prayerViewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)

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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.wear_brand_header),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WearColors.GoldAccent,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )
                    if (prayerState.hijriDate.isNotBlank()) {
                        Text(
                            text = prayerState.hijriDate,
                            fontSize = 10.sp,
                            color = WearColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Card(
                    onClick = onNavigatePrayerTimes,
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = WearColors.SurfaceCard,
                        endBackgroundColor = WearColors.SurfaceDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (prayerState.nextPrayerName.isNotBlank()) {
                                    stringResource(R.string.wear_next_prefix, prayerState.nextPrayerName)
                                } else {
                                    stringResource(R.string.wear_prayer_schedule)
                                },
                                fontSize = 11.sp,
                                color = WearColors.TealBright,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (prayerState.nextPrayerTime.isNotBlank()) {
                                    prayerState.nextPrayerTime
                                } else {
                                    prayerState.cityName
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WearColors.TextPrimary
                            )
                        }
                        if (prayerState.countdown.isNotBlank()) {
                            Text(
                                text = "-${prayerState.countdown}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WearColors.GoldAccent
                            )
                        }
                    }
                }
            }

            item {
                WearFeatureChip(
                    title = stringResource(R.string.wear_prayer_times),
                    subtitle = "${prayerState.cityName} • ${prayerState.calculationMethodName}",
                    iconRes = R.drawable.ic_prayer_times,
                    accentColor = WearColors.TealBright,
                    onClick = onNavigatePrayerTimes
                )
            }

            item {
                WearFeatureChip(
                    title = stringResource(R.string.wear_quran),
                    subtitle = stringResource(R.string.wear_quran_subtitle),
                    iconRes = R.drawable.ic_quran,
                    accentColor = WearColors.GoldAccent,
                    onClick = onNavigateQuran
                )
            }

            item {
                WearFeatureChip(
                    title = stringResource(R.string.wear_qibla),
                    subtitle = stringResource(R.string.wear_qibla_subtitle),
                    iconRes = R.drawable.ic_qibla,
                    accentColor = WearColors.TealPrimary,
                    onClick = onNavigateQibla
                )
            }
        }
    }
}

@Composable
private fun WearFeatureChip(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    accentColor: Color,
    onClick: () -> Unit
) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        label = {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = WearColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = WearColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        },
        colors = ChipDefaults.chipColors(
            backgroundColor = WearColors.SurfaceDark,
            contentColor = MaterialTheme.colors.onSurface
        )
    )
}
