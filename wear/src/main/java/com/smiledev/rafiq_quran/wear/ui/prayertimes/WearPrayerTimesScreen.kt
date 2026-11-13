package com.smiledev.rafiq_quran.wear.ui.prayertimes

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
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
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.wear.R
import com.smiledev.rafiq_quran.wear.theme.WearColors
import com.smiledev.rafiq_quran.wear.ui.components.wearRotaryScroll
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WearPrayerTimesScreen(
    viewModel: WearPrayerTimesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val displayDateFormat = remember(locale) { SimpleDateFormat("EEEE, d MMM", locale) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshLocation(forceGeocode = true)
        }
    }

    val onUpdateGpsClick: () -> Unit = {
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
            // Header Card
            item {
                Card(
                    onClick = { viewModel.toggleNotifications() },
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = WearColors.SurfaceCard,
                        endBackgroundColor = WearColors.SurfaceDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.cityName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WearColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = if (state.prayerNotificationsEnabled) {
                                            WearColors.TealBright.copy(alpha = 0.18f)
                                        } else {
                                            WearColors.SurfaceDark
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = stringResource(
                                        if (state.prayerNotificationsEnabled) {
                                            R.string.wear_notifications_on
                                        } else {
                                            R.string.wear_notifications_off
                                        }
                                    ),
                                    tint = if (state.prayerNotificationsEnabled) {
                                        WearColors.TealBright
                                    } else {
                                        WearColors.TextMuted
                                    },
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = stringResource(
                                        if (state.prayerNotificationsEnabled) {
                                            R.string.wear_on
                                        } else {
                                            R.string.wear_off
                                        }
                                    ),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.prayerNotificationsEnabled) {
                                        WearColors.TealBright
                                    } else {
                                        WearColors.TextMuted
                                    }
                                )
                            }
                        }
                        if (state.hijriDate.isNotBlank()) {
                            Text(
                                text = state.hijriDate,
                                fontSize = 10.sp,
                                color = WearColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = if (state.nextPrayerName.isNotBlank()) {
                                        stringResource(R.string.wear_next_prefix, state.nextPrayerName)
                                    } else {
                                        stringResource(R.string.wear_prayer_schedule)
                                    },
                                    fontSize = 11.sp,
                                    color = WearColors.TealBright,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (state.nextPrayerTime.isNotBlank()) {
                                    Text(
                                        text = state.nextPrayerTime,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WearColors.TextPrimary
                                    )
                                }
                            }
                            if (state.countdown.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .background(WearColors.SurfaceDark, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "-${state.countdown}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WearColors.GoldAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Day Navigation Row
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactChip(
                            onClick = { viewModel.goToPreviousDay() },
                            label = { Text(stringResource(R.string.wear_prev_day), fontSize = 10.sp, maxLines = 1) },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = WearColors.SurfaceDark,
                                contentColor = WearColors.TextPrimary
                            )
                        )
                        CompactChip(
                            onClick = { viewModel.goToToday() },
                            label = {
                                Text(
                                    text = stringResource(R.string.wear_today),
                                    fontSize = 10.sp,
                                    fontWeight = if (state.isToday) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = if (state.isToday) WearColors.TealPrimary else WearColors.SurfaceDark,
                                contentColor = if (state.isToday) Color.Black else WearColors.TextPrimary
                            )
                        )
                        CompactChip(
                            onClick = { viewModel.goToNextDay() },
                            label = { Text(stringResource(R.string.wear_next_day), fontSize = 10.sp, maxLines = 1) },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = WearColors.SurfaceDark,
                                contentColor = WearColors.TextPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayDateFormat.format(state.currentDate),
                        fontSize = 10.sp,
                        color = WearColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Loading State
            if (state.isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .size(26.dp),
                        indicatorColor = WearColors.TealBright,
                        trackColor = WearColors.SurfaceCard
                    )
                }
            }

            // Error State
            if (state.error != null && !state.isLoading) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WearColors.SurfaceDark, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = state.error?.displayMessage ?: stringResource(R.string.wear_prayer_load_error),
                            fontSize = 10.sp,
                            color = WearColors.ErrorRed,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompactChip(
                            onClick = { viewModel.loadPrayerTimes(forceRefresh = true) },
                            label = { Text(stringResource(R.string.wear_retry), fontSize = 10.sp) },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = WearColors.TealPrimary,
                                contentColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Prayer Times Entries List
            items(state.prayerTimes) { entry ->
                WearPrayerTimeRow(entry = entry)
            }

            // GPS Sync & Calculation Method Info
            if (!state.isLoading) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        CompactChip(
                            onClick = onUpdateGpsClick,
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = stringResource(R.string.wear_update_gps),
                                    tint = WearColors.TealBright,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(
                                        if (state.isSyncingLocation) {
                                            R.string.wear_updating_gps
                                        } else {
                                            R.string.wear_update_gps
                                        }
                                    ),
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = WearColors.SurfaceDark,
                                contentColor = WearColors.TextPrimary
                            )
                        )
                        if (state.calculationMethodName.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.wear_method_format, state.calculationMethodName),
                                fontSize = 9.sp,
                                color = WearColors.TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WearPrayerTimeRow(
    entry: WearPrayerTimeEntry,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (entry.isNext) {
        WearColors.SurfaceCard
    } else {
        WearColors.SurfaceDark
    }

    val alpha = if (entry.isPassed) 0.45f else 1.0f

    val accentColor = when {
        entry.isNext -> WearColors.TealBright
        entry.isPassed -> WearColors.TextMuted
        else -> WearColors.GoldAccent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (entry.iconResId != 0) {
                    Icon(
                        painter = painterResource(id = entry.iconResId),
                        contentDescription = entry.name,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = entry.name,
                    fontSize = 12.sp,
                    fontWeight = if (entry.isNext) FontWeight.Bold else FontWeight.Medium,
                    color = if (entry.isNext) WearColors.TealBright else WearColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.isNext) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = WearColors.TealBright.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.wear_next_badge),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = WearColors.TealBright
                        )
                    }
                }
            }

            Text(
                text = entry.time,
                fontSize = 12.sp,
                fontWeight = if (entry.isNext) FontWeight.Bold else FontWeight.SemiBold,
                color = if (entry.isNext) WearColors.TealBright else WearColors.TextPrimary
            )
        }
    }
}
