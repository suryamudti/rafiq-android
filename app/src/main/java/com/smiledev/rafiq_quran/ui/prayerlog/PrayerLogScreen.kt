package com.smiledev.rafiq_quran.ui.prayerlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.domain.repository.PrayerLogDay
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqEnterTransition
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqExitTransition
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqStatCard
import com.smiledev.rafiq_quran.ui.designsystem.list.RafiqSectionHeader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class PrayerItemInfo(
    val key: String,
    val labelResId: Int,
    val arabicResId: Int
)

private val prayerItems = listOf(
    PrayerItemInfo("fajr", R.string.fajr_subuh, R.string.fajr_arabic),
    PrayerItemInfo("dhuhr", R.string.dhuhr, R.string.dhuhr_arabic),
    PrayerItemInfo("asr", R.string.asr, R.string.asr_arabic),
    PrayerItemInfo("maghrib", R.string.maghrib, R.string.maghrib_arabic),
    PrayerItemInfo("isha", R.string.isha, R.string.isha_arabic)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerLogScreen(
    onBack: () -> Unit,
    viewModel: PrayerLogViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.prayer_log_title),
                onBack = onBack
            )
        }
    ) { padding ->
        var isRefreshing by remember { mutableStateOf(false) }
        LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true; viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = RafiqTheme.spacing.l,
                    vertical = RafiqTheme.spacing.m
                ),
                verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.l)
            ) {
                // 1. Stats Row (Current Streak + Weekly Completion)
                item {
                    StatsHeader(
                        currentStreak = state.currentStreak,
                        weeklyPercentage = state.weeklyPercentage,
                        weeklyCompleted = state.weeklyCompletedCount,
                        weeklyTotal = state.weeklyTotalCount
                    )
                }

                // 2. 7-Day Date Selector Strip with tactile chip press
                item {
                    RecentDaysStrip(
                        days = state.recentDays,
                        onDayClick = { viewModel.selectDate(it.date) }
                    )
                }

                // 3. Date Navigation & Active Day Header with 44dp touch targets
                item {
                    DateNavigationHeader(
                        selectedDate = state.selectedDate,
                        todayDate = state.todayDate,
                        onPreviousDay = { viewModel.previousDay() },
                        onNextDay = { viewModel.nextDay() },
                        onJumpToToday = { viewModel.jumpToToday() }
                    )
                }

                // 4. Celebration Banner (All 5 completed) with 0.95f fluid scale enter
                val activeLog = state.selectedLog
                val completedCount = activeLog?.completedCount ?: 0
                item {
                    AnimatedVisibility(
                        visible = completedCount == 5,
                        enter = rafiqEnterTransition(initialScale = 0.95f),
                        exit = rafiqExitTransition(targetScale = 0.95f)
                    ) {
                        AllCompletedBanner()
                    }
                }

                // 5. Active Day Prayer Checklist Card
                item {
                    PrayerChecklistCard(
                        activeLog = activeLog,
                        completedCount = completedCount,
                        onTogglePrayer = { key, checked -> viewModel.togglePrayer(key, checked) },
                        onMarkAll = { allDone -> viewModel.markAllPrayers(allDone) }
                    )
                }

                // 6. Recent History Section
                if (state.historyLogs.isNotEmpty()) {
                    item {
                        RafiqSectionHeader(
                            title = stringResource(R.string.recent_history),
                            modifier = Modifier.padding(top = RafiqTheme.spacing.s)
                        )
                    }

                    items(state.historyLogs.take(14), key = { it.date }) { dayLog ->
                        HistoryDayCard(
                            dayLog = dayLog,
                            isCurrentSelected = dayLog.date == state.selectedDate,
                            onClick = { viewModel.selectDate(dayLog.date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(
    currentStreak: Int,
    weeklyPercentage: Int,
    weeklyCompleted: Int,
    weeklyTotal: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.m)
    ) {
        RafiqStatCard(
            label = stringResource(R.string.current_streak),
            value = stringResource(R.string.streak_days_format, currentStreak),
            valueColor = RafiqTheme.extendedColors.goldAccent,
            subtitle = if (currentStreak > 0) "🔥 Active" else stringResource(R.string.no_streak),
            modifier = Modifier.weight(1f)
        )

        RafiqStatCard(
            label = stringResource(R.string.weekly_progress),
            value = "$weeklyPercentage%",
            valueColor = MaterialTheme.colorScheme.primary,
            subtitle = stringResource(R.string.weekly_stats_format, weeklyCompleted, weeklyPercentage),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RecentDaysStrip(
    days: List<DaySummary>,
    onDayClick: (DaySummary) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)
    ) {
        items(days, key = { it.date }) { day ->
            val isSelected = day.isSelected
            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val borderColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                RafiqTheme.extendedColors.cardBorder
            }

            Surface(
                modifier = Modifier
                    .clip(RafiqTheme.customShapes.medium)
                    .rafiqPressable(targetScale = RafiqMotion.ChipPressScale)
                    .clickable { onDayClick(day) },
                shape = RafiqTheme.customShapes.medium,
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = day.dayNumber,
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.1).sp),
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Spacer(Modifier.height(6.dp))
                    // Completion Indicator Dot / Badge
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    day.isAllCompleted -> MaterialTheme.colorScheme.primary
                                    day.completedCount > 0 -> RafiqTheme.extendedColors.goldAccent
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun DateNavigationHeader(
    selectedDate: String,
    todayDate: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onJumpToToday: () -> Unit
) {
    val isToday = selectedDate == todayDate
    val isPastDay = selectedDate < todayDate
    val displayDate = formatDisplayDate(selectedDate, todayDate)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Day Button with min 44dp touch target
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                .clickable(onClick = onPreviousDay),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "‹",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Date Display & Today Indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayDate,
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.1).sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isToday) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .rafiqPressable(targetScale = RafiqMotion.ChipPressScale)
                        .clickable(onClick = onJumpToToday)
                ) {
                    Text(
                        text = stringResource(R.string.jump_to_today),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Next Day Button (Disabled if already today) with min 44dp touch target
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (isPastDay) {
                        Modifier
                            .rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                            .clickable(onClick = onNextDay)
                    } else Modifier
                ),
            shape = CircleShape,
            color = if (isPastDay) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            },
            border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPastDay) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    }
                )
            }
        }
    }
}

@Composable
private fun AllCompletedBanner() {
    RafiqCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = RafiqTheme.extendedColors.quranContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, RafiqTheme.extendedColors.badgeSunnah.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RafiqTheme.extendedColors.badgeSunnah.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_prayer_log),
                    contentDescription = null,
                    tint = RafiqTheme.extendedColors.badgeSunnah,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.all_prayers_completed_title),
                    style = MaterialTheme.typography.titleSmall.copy(letterSpacing = (-0.1).sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.all_prayers_completed_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PrayerChecklistCard(
    activeLog: PrayerLogDay?,
    completedCount: Int,
    onTogglePrayer: (String, Boolean) -> Unit,
    onMarkAll: (Boolean) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = completedCount / 5f,
        animationSpec = tween(durationMillis = RafiqMotion.DurationMedium, easing = EaseOutCubic),
        label = "prayer_log_progress"
    )

    RafiqCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder)
    ) {
        // Card Header: Progress and Batch Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.prayers_completed_format, completedCount),
                    style = MaterialTheme.typography.titleSmall.copy(letterSpacing = (-0.1).sp),
                    fontWeight = FontWeight.Bold
                )
            }

            val allDone = completedCount == 5
            TextButton(
                onClick = { onMarkAll(!allDone) },
                modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.ChipPressScale),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(if (allDone) R.string.reset_all else R.string.mark_all_done),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(Modifier.height(RafiqTheme.spacing.m))

        // 5 Prayer Rows
        prayerItems.forEachIndexed { index, item ->
            val isChecked = when (item.key) {
                "fajr" -> activeLog?.fajr ?: false
                "dhuhr" -> activeLog?.dhuhr ?: false
                "asr" -> activeLog?.asr ?: false
                "maghrib" -> activeLog?.maghrib ?: false
                "isha" -> activeLog?.isha ?: false
                else -> false
            }

            PrayerLogRowItem(
                item = item,
                isChecked = isChecked,
                onCheckedChange = { onTogglePrayer(item.key, it) }
            )

            if (index < prayerItems.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PrayerLogRowItem(
    item: PrayerItemInfo,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val backgroundColor = if (isChecked) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .rafiqPressable(targetScale = 0.99f),
        shape = RafiqTheme.customShapes.small,
        color = backgroundColor,
        border = BorderStroke(1.dp, if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else RafiqTheme.extendedColors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(item.labelResId),
                        style = MaterialTheme.typography.bodyLarge.copy(letterSpacing = (-0.1).sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(item.arabicResId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = stringResource(
                        if (isChecked) R.string.status_completed else R.string.status_not_completed
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isChecked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
            }

            // AGENTS.md constraint: PrayerLogScreen uses Switch for toggling each of the 5 daily prayers
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun HistoryDayCard(
    dayLog: PrayerLogDay,
    isCurrentSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isCurrentSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        RafiqTheme.extendedColors.cardBorder
    }

    RafiqCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        border = BorderStroke(1.dp, borderColor),
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = dayLog.date,
                    style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = (-0.05).sp),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${dayLog.completedCount}/5 prayers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 5 mini prayer status indicators: Fajr, Dhuhr, Asr, Maghrib, Isha
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniPrayerDot("F", dayLog.fajr)
                MiniPrayerDot("D", dayLog.dhuhr)
                MiniPrayerDot("A", dayLog.asr)
                MiniPrayerDot("M", dayLog.maghrib)
                MiniPrayerDot("I", dayLog.isha)
            }
        }
    }
}

@Composable
private fun MiniPrayerDot(label: String, completed: Boolean) {
    val containerColor = if (completed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (completed) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private fun formatDisplayDate(dateStr: String, todayStr: String): String {
    if (dateStr.isEmpty()) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    return try {
        val parsedDate = sdf.parse(dateStr) ?: return dateStr
        val parsedToday = sdf.parse(todayStr) ?: Date()

        cal.time = parsedToday
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        when (dateStr) {
            todayStr -> {
                val fullFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                "Today • ${fullFormat.format(parsedDate)}"
            }
            yesterdayStr -> {
                val fullFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                "Yesterday • ${fullFormat.format(parsedDate)}"
            }
            else -> {
                val fullFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                fullFormat.format(parsedDate)
            }
        }
    } catch (_: Exception) {
        dateStr
    }
}
