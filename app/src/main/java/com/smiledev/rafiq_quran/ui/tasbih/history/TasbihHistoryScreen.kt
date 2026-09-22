package com.smiledev.rafiq_quran.ui.tasbih.history

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.domain.model.TasbihDayHistory
import com.smiledev.rafiq_quran.domain.model.TasbihHistoryItem
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.arabic.RafiqArabicText
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqBadge
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihHistoryScreen(
    onBack: () -> Unit,
    viewModel: TasbihHistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.tasbih_history),
                onBack = onBack,
                actions = {
                    if (state.dailyHistories.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setShowClearAllDialog(true) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.tasbih_clear_history),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.dailyHistories.isEmpty()) {
            EmptyHistoryView(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(RafiqTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.m)
            ) {
                item {
                    HistoryStatsHeader(
                        totalAll = state.totalAllTimeCount,
                        activeDays = state.activeDaysCount,
                        todayTotal = state.todayCount,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(state.dailyHistories, key = { it.date }) { dayHistory ->
                    DayHistoryCard(
                        dayHistory = dayHistory,
                        onDeleteClick = { viewModel.setDayToDelete(dayHistory.date) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Dialog: Clear All History
    if (state.showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowClearAllDialog(false) },
            title = { Text(text = stringResource(R.string.tasbih_clear_history)) },
            text = { Text(text = stringResource(R.string.tasbih_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmClearAll() }) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowClearAllDialog(false) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog: Delete Day History
    state.dayToDelete?.let { dateStr ->
        val formattedDate = formatDateHeader(dateStr)
        AlertDialog(
            onDismissRequest = { viewModel.setDayToDelete(null) },
            title = { Text(text = stringResource(R.string.tasbih_delete_day)) },
            text = { Text(text = stringResource(R.string.tasbih_delete_day_confirm, formattedDate)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteDay() }) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setDayToDelete(null) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun HistoryStatsHeader(
    totalAll: Int,
    activeDays: Int,
    todayTotal: Int,
    modifier: Modifier = Modifier
) {
    RafiqCard(
        modifier = modifier,
        contentPadding = RafiqTheme.spacing.m
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn(
                value = "$totalAll",
                label = stringResource(R.string.tasbih_total_all_time),
                modifier = Modifier.weight(1f)
            )
            StatColumn(
                value = "$activeDays",
                label = stringResource(R.string.tasbih_active_days),
                modifier = Modifier.weight(1f)
            )
            StatColumn(
                value = "$todayTotal",
                label = stringResource(R.string.tasbih_today_count),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DayHistoryCard(
    dayHistory: TasbihDayHistory,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RafiqCard(
        modifier = modifier,
        contentPadding = RafiqTheme.spacing.m
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDateHeader(dayHistory.date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RafiqBadge(
                        text = "${dayHistory.totalCount}x",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(RafiqTheme.iconSizes.s)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)
            ) {
                dayHistory.items.forEach { item ->
                    DhikrHistoryRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun DhikrHistoryRow(
    item: TasbihHistoryItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (item.arabic.isNotEmpty()) {
                RafiqArabicText(
                    text = item.arabic,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = item.dhikrName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(RafiqTheme.spacing.m))
        RafiqBadge(
            text = "${item.count}x",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyHistoryView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(RafiqTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tasbih),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))
        Text(
            text = stringResource(R.string.tasbih_history_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))
        Text(
            text = stringResource(R.string.tasbih_history_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private data class FormattedDateResult(
    val formatted: String,
    val isToday: Boolean,
    val isYesterday: Boolean
)

private fun parseDate(dateStr: String): FormattedDateResult {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val targetDate = parser.parse(dateStr) ?: return FormattedDateResult(dateStr, isToday = false, isYesterday = false)

        val calTarget = Calendar.getInstance().apply { time = targetDate }
        val calToday = Calendar.getInstance()
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val isToday = calTarget.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
            calTarget.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)

        val isYesterday = calTarget.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
            calTarget.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)

        val formatter = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        FormattedDateResult(formatter.format(targetDate), isToday, isYesterday)
    } catch (_: Throwable) {
        FormattedDateResult(dateStr, isToday = false, isYesterday = false)
    }
}

@Composable
private fun formatDateHeader(dateStr: String): String {
    val result = parseDate(dateStr)
    return when {
        result.isToday -> "${stringResource(R.string.today_button)}, ${result.formatted}"
        result.isYesterday -> "${stringResource(R.string.yesterday)}, ${result.formatted}"
        else -> result.formatted
    }
}
