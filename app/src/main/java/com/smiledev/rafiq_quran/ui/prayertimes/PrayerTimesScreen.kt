package com.smiledev.rafiq_quran.ui.prayertimes

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqBadge
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqHeroCard
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqErrorState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    onBack: () -> Unit,
    onNavigateToQibla: () -> Unit = {},
    onNavigateToPrayerLog: () -> Unit = {},
    onNavigateToPrayerGuidance: () -> Unit = {},
    viewModel: PrayerTimesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.prayer_times),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleNotifications() },
                        modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.prayer_notifications),
                            tint = if (state.prayerNotificationsEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        var isRefreshing by remember { mutableStateOf(false) }
        LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true; viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                state.isLoading && !isRefreshing -> {
                    RafiqLoadingIndicator()
                }
                state.error != null -> {
                    val err = state.error
                    if (err != null) {
                        RafiqErrorState(
                            error = err,
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = RafiqTheme.spacing.xl)
                    ) {
                        // 1. Hero Card: Location, Next Prayer countdown with Emil Kowalski specular highlights
                        item {
                            RafiqHeroCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = RafiqTheme.spacing.l,
                                        vertical = RafiqTheme.spacing.m
                                    ),
                                gradient = RafiqTheme.extendedColors.heroPrayerGradient
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Location & Method pill with specular top border
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        modifier = Modifier.padding(bottom = RafiqTheme.spacing.s)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocationOn,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "${state.cityName} • ${state.calculationMethodName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Text(
                                        text = stringResource(R.string.upcoming_prayer).uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))
                                    Text(
                                        text = state.currentPrayer.uppercase(),
                                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = (-0.2).sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = state.currentPrayerTime,
                                        style = MaterialTheme.typography.displayMedium.copy(letterSpacing = (-0.75).sp),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))
                                    if (state.countdown.isNotBlank()) {
                                        RafiqBadge(
                                            text = stringResource(R.string.next_prayer, state.countdown),
                                            containerColor = Color.White.copy(alpha = 0.22f),
                                            contentColor = Color.White,
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Quick Actions: Qibla, Prayer Log, Guidance with tactile bounce feedback
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = RafiqTheme.spacing.l,
                                        vertical = RafiqTheme.spacing.xs
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickActionButton(
                                    label = stringResource(R.string.qibla),
                                    iconResId = R.drawable.ic_qibla,
                                    onClick = onNavigateToQibla,
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionButton(
                                    label = stringResource(R.string.prayer_log),
                                    iconResId = R.drawable.ic_prayer_log,
                                    onClick = onNavigateToPrayerLog,
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionButton(
                                    label = stringResource(R.string.prayer_guidance),
                                    iconResId = R.drawable.ic_prayer_guide,
                                    onClick = onNavigateToPrayerGuidance,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // 3. Date Navigation Card with Chevron Icons & Today Chip
                        item {
                            Spacer(Modifier.height(RafiqTheme.spacing.xs))
                            RafiqCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = RafiqTheme.spacing.l,
                                        vertical = RafiqTheme.spacing.xs
                                    ),
                                contentPadding = RafiqTheme.spacing.s
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { viewModel.goToPreviousDay() },
                                        modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_chevron_left),
                                            contentDescription = stringResource(R.string.previous_day),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (state.hijriDate.isNotBlank()) {
                                            Text(
                                                text = state.hijriDate,
                                                style = MaterialTheme.typography.titleSmall.copy(letterSpacing = (-0.1).sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Text(
                                            text = viewModel.displayDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        if (!state.isToday) {
                                            Spacer(Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                                modifier = Modifier
                                                    .rafiqPressable(targetScale = RafiqMotion.ChipPressScale)
                                                    .clickable { viewModel.goToToday() }
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.today_button),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.goToNextDay() },
                                        modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_chevron_right),
                                            contentDescription = stringResource(R.string.next_day),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(Modifier.height(RafiqTheme.spacing.s)) }

                        // 4. Prayer Times Schedule List Items
                        items(state.prayerTimes) { prayer ->
                            PrayerTimeItemCard(
                                prayer = prayer,
                                notificationsActive = state.prayerNotificationsEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = RafiqTheme.spacing.l,
                                        vertical = 4.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .heightIn(min = 68.dp)
            .rafiqPressable(targetScale = RafiqMotion.DefaultPressScale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PrayerTimeItemCard(
    prayer: PrayerTimeEntry,
    notificationsActive: Boolean,
    modifier: Modifier = Modifier
) {
    val isNext = prayer.isNext
    val isPassed = prayer.isPassed

    val targetContainerColor = when {
        isNext -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        isPassed -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surface
    }

    val targetBorderColor = when {
        isNext -> MaterialTheme.colorScheme.primary
        else -> RafiqTheme.extendedColors.cardBorder
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = RafiqMotion.DurationNormal, easing = EaseOutCubic),
        label = "prayer_card_container"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = RafiqMotion.DurationNormal, easing = EaseOutCubic),
        label = "prayer_card_border"
    )

    Card(
        modifier = modifier
            .heightIn(min = 60.dp)
            .rafiqPressable(targetScale = RafiqMotion.CardPressScale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = animatedContainerColor),
        border = BorderStroke(if (isNext) 1.5.dp else 1.dp, animatedBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in rounded container with specular accent
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isNext -> MaterialTheme.colorScheme.primary
                            isPassed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (prayer.iconResId != 0) {
                    Icon(
                        painter = painterResource(prayer.iconResId),
                        contentDescription = prayer.name,
                        tint = when {
                            isNext -> Color.White
                            isPassed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Prayer Name & Badges
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prayer.name,
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.1).sp),
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
                        color = when {
                            isPassed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (isNext) {
                        Spacer(Modifier.width(8.dp))
                        RafiqBadge(
                            text = stringResource(R.string.status_now),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    } else if (isPassed) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.status_passed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Prayer Time & Bell status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = prayer.time,
                    style = MaterialTheme.typography.titleLarge.copy(letterSpacing = (-0.2).sp),
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isNext -> MaterialTheme.colorScheme.primary
                        isPassed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = if (notificationsActive && !isPassed) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
