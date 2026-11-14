package com.smiledev.rafiq_quran.tv.ui.ambient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.tv.service.TvAdhanAlertEvent
import com.smiledev.rafiq_quran.tv.theme.TvArabicFontFamily
import com.smiledev.rafiq_quran.tv.theme.TvColors
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TvAmbientDisplayScreen(
    viewModel: TvAmbientViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val displayDateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TvColors.DeepNavy,
                        Color(0xFF0B1A2C),
                        TvColors.DeepNavy
                    )
                )
            )
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Header Bar: Brand, City & Calculation Method, Live Digital Clock & Dates
            TvHeaderSection(
                state = state,
                formattedGregorianDate = displayDateFormat.format(state.currentDate)
            )

            // 2. Active Adhan Alert Banner (if triggered)
            val activeAlert = state.activeAdhanAlert
            if (activeAlert != null) {
                TvAdhanAlertBanner(
                    alert = activeAlert,
                    cityName = state.cityName,
                    onDismiss = { viewModel.dismissAdhanAlert() }
                )
            }

            // 3. Center Hero Row: Next Prayer Countdown Card + Spiritual Quranic Verse Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvNextPrayerHeroCard(
                    state = state,
                    modifier = Modifier.weight(0.45f)
                )

                TvSpiritualVerseCard(
                    reminder = state.currentReminder,
                    onClickNext = { viewModel.nextSpiritualReminder() },
                    modifier = Modifier.weight(0.55f)
                )
            }

            // 4. 8-Prayer Schedule Horizontal Strip (10-foot UI cards)
            if (state.isLoading && state.prayerTimes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TvColors.TealBright,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else if (state.error != null && state.prayerTimes.isEmpty()) {
                TvErrorBanner(
                    message = state.error?.displayMessage ?: "Unable to load prayer schedule",
                    onRetry = { viewModel.refresh() }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.prayerTimes.forEach { entry ->
                        TvPrayerTimeCard(
                            entry = entry,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. D-Pad Interactive Control Bar (Day Nav, City Switcher, Method, Adhan Alert, Refresh)
            TvControlBar(
                state = state,
                onPreviousDay = { viewModel.goToPreviousDay() },
                onToday = { viewModel.goToToday() },
                onNextDay = { viewModel.goToNextDay() },
                onSwitchCity = { viewModel.selectNextCity() },
                onCycleMethod = { viewModel.cycleCalculationMethod() },
                onToggleNotifications = { viewModel.toggleNotifications() },
                onTestAdhanAlert = { viewModel.triggerAdhanPreview() },
                onRefresh = { viewModel.refresh() }
            )
        }
    }
}

@Composable
private fun TvHeaderSection(
    state: TvAmbientUiState,
    formattedGregorianDate: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Brand + Location + Calculation Method Badge
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = TvColors.TealPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = TvColors.TealBright.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "RAFIQ TV • MASJID DISPLAY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvColors.TealBright,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(TvColors.SurfaceCard, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = TvColors.GoldAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${state.cityName}  •  ${state.calculationMethodName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TvColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formattedGregorianDate,
                    fontSize = 15.sp,
                    color = TvColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                if (state.hijriDate.isNotBlank()) {
                    Text(
                        text = "•",
                        fontSize = 15.sp,
                        color = TvColors.GoldAccent
                    )
                    Text(
                        text = "${state.hijriDate} H",
                        fontSize = 15.sp,
                        color = TvColors.GoldAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Right: Live Digital Clock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(TvColors.SurfaceCard, RoundedCornerShape(14.dp))
                    .border(1.dp, TvColors.TealBright.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = state.currentClockTime.ifBlank { "--:--" },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TvColors.TextPrimary,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
private fun TvAdhanAlertBanner(
    alert: TvAdhanAlertEvent,
    cityName: String,
    onDismiss: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A3A3A),
        border = BorderStroke(
            width = if (isFocused) 3.dp else 2.dp,
            color = TvColors.GoldAccent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onDismiss() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = TvColors.GoldAccent,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "ADHAN TIME ENTERED • ${alert.prayerName.uppercase()} (${alert.prayerTime})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TvColors.GoldAccent
                    )
                    Text(
                        text = "Hayya 'alas-Salah — It is time to establish ${alert.prayerName} prayer in $cityName",
                        fontSize = 13.sp,
                        color = TvColors.TextPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(TvColors.GoldAccent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Dismiss (OK)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun TvNextPrayerHeroCard(
    state: TvAmbientUiState,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = TvColors.SurfaceCard,
        border = BorderStroke(1.5.dp, TvColors.TealBright.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (state.isToday) "UPCOMING PRAYER" else "SELECTED SCHEDULE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvColors.TealBright,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = state.nextPrayerName.ifBlank { "Schedule Ready" },
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TvColors.TextPrimary
                    )
                    if (state.nextPrayerArabic.isNotBlank()) {
                        Text(
                            text = state.nextPrayerArabic,
                            fontFamily = TvArabicFontFamily,
                            fontSize = 24.sp,
                            color = TvColors.GoldAccent,
                            style = TextStyle(textDirection = TextDirection.Rtl)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (state.nextPrayerTime.isNotBlank()) {
                    Text(
                        text = "Scheduled at ${state.nextPrayerTime}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TvColors.TextSecondary
                    )
                }
            }

            if (state.countdown.isNotBlank()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .background(TvColors.SurfaceDark, RoundedCornerShape(14.dp))
                        .border(1.dp, TvColors.GoldAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "TIME REMAINING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvColors.TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "-${state.countdown}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TvColors.GoldAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun TvSpiritualVerseCard(
    reminder: TvSpiritualReminder,
    onClickNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isFocused) TvColors.SurfaceHighlight else TvColors.SurfaceDark,
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) TvColors.GoldAccent else TvColors.SurfaceCard
        ),
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClickNext() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = reminder.arabic,
                fontFamily = TvArabicFontFamily,
                fontSize = 21.sp,
                color = TvColors.GoldSoft,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(textDirection = TextDirection.Rtl),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\"${reminder.translation}\"",
                fontSize = 13.sp,
                color = TvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Press OK to cycle verse",
                    fontSize = 11.sp,
                    color = TvColors.TextMuted
                )
                Text(
                    text = reminder.reference,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvColors.TealBright
                )
            }
        }
    }
}

@Composable
private fun TvPrayerTimeCard(
    entry: TvPrayerTimeEntry,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardColor = when {
        isFocused -> TvColors.SurfaceHighlight
        entry.isNext -> TvColors.SurfaceCard
        else -> TvColors.SurfaceDark
    }

    val borderColor = when {
        isFocused -> TvColors.GoldAccent
        entry.isNext -> TvColors.TealBright
        else -> Color(0xFF1E344B)
    }

    val borderWidth = if (isFocused || entry.isNext) 2.5.dp else 1.dp
    val contentAlpha = if (entry.isPassed && !entry.isNext && !isFocused) 0.55f else 1.0f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .alpha(contentAlpha)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (entry.isNext) {
                Box(
                    modifier = Modifier
                        .background(TvColors.TealBright.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEXT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TvColors.TealBright
                    )
                }
            } else {
                Text(
                    text = entry.arabicName,
                    fontFamily = TvArabicFontFamily,
                    fontSize = 14.sp,
                    color = TvColors.TextSecondary,
                    maxLines = 1,
                    style = TextStyle(textDirection = TextDirection.Rtl)
                )
            }

            if (entry.iconResId != 0) {
                Icon(
                    painter = painterResource(id = entry.iconResId),
                    contentDescription = entry.name,
                    tint = if (entry.isNext) TvColors.GoldAccent else TvColors.TealBright,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = entry.name,
                fontSize = 15.sp,
                fontWeight = if (entry.isNext) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (entry.isNext) TvColors.TealBright else TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = entry.time,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (entry.isNext) TvColors.GoldAccent else TvColors.TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TvErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TvColors.SurfaceDark, RoundedCornerShape(14.dp))
            .border(1.dp, TvColors.ErrorRed, RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            color = TvColors.ErrorRed,
            modifier = Modifier.weight(1f)
        )
        TvActionChip(
            label = "Retry",
            isHighlighted = true,
            onClick = onRetry
        )
    }
}

@Composable
private fun TvControlBar(
    state: TvAmbientUiState,
    onPreviousDay: () -> Unit,
    onToday: () -> Unit,
    onNextDay: () -> Unit,
    onSwitchCity: () -> Unit,
    onCycleMethod: () -> Unit,
    onToggleNotifications: () -> Unit,
    onTestAdhanAlert: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day Navigation
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvActionChip(label = "‹ Prev Day", onClick = onPreviousDay)
            TvActionChip(
                label = "Today",
                isHighlighted = state.isToday,
                onClick = onToday
            )
            TvActionChip(label = "Next Day ›", onClick = onNextDay)
        }

        // City, Calculation Method, Adhan Alerts & Refresh
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvActionChip(
                label = "City: ${state.cityName}",
                onClick = onSwitchCity
            )
            TvActionChip(
                label = "Method: ${state.calculationMethodName}",
                onClick = onCycleMethod
            )
            TvActionChip(
                label = if (state.prayerNotificationsEnabled) "Adhan Alerts: ON" else "Adhan Alerts: OFF",
                isHighlighted = state.prayerNotificationsEnabled,
                onClick = onToggleNotifications
            )
            TvActionChip(
                label = "Preview Adhan",
                onClick = onTestAdhanAlert
            )
            TvActionChip(
                label = "Refresh",
                onClick = onRefresh,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = TvColors.TextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun TvActionChip(
    label: String,
    isHighlighted: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        isFocused -> TvColors.GoldAccent
        isHighlighted -> TvColors.TealPrimary
        else -> TvColors.SurfaceCard
    }

    val textColor = when {
        isFocused || isHighlighted -> Color.Black
        else -> TvColors.TextPrimary
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) Color.White else TvColors.TealBright.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isFocused || isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}
