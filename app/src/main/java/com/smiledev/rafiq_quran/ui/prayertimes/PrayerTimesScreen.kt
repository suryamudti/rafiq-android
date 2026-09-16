package com.smiledev.rafiq_quran.ui.prayertimes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqBadge
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqHeroCard
import com.smiledev.rafiq_quran.ui.designsystem.divider.RafiqDivider
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqErrorState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    onBack: () -> Unit,
    viewModel: PrayerTimesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.prayer_times),
                onBack = onBack
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
                        modifier = modifier.fillMaxSize()
                    ) {
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
                                    Text(
                                        text = state.currentPrayer.uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.85f),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))
                                    Text(
                                        text = state.currentPrayerTime,
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))
                                    RafiqBadge(
                                        text = stringResource(R.string.next_prayer, state.countdown),
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    )
                                }
                            }
                        }

                        item {
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
                                    TextButton(onClick = { viewModel.goToPreviousDay() }) {
                                        Text("<", style = MaterialTheme.typography.titleLarge)
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (state.hijriDate.isNotBlank()) {
                                            Text(
                                                text = state.hijriDate,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = viewModel.displayDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { viewModel.goToNextDay() }) {
                                        Text(">", style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                        }

                        item { Spacer(Modifier.height(RafiqTheme.spacing.s)) }

                        items(state.prayerTimes) { prayer ->
                            Column {
                                RafiqDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = RafiqTheme.spacing.xl,
                                            vertical = RafiqTheme.spacing.m
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prayer.name,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = prayer.time,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
