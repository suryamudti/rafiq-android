package com.smiledev.rafiq_quran.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.domain.model.IslamicEvent
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBackButton
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeroCard
import com.smiledev.rafiq_quran.ui.theme.SecondaryGold
import com.smiledev.rafiq_quran.ui.theme.rafiqPressable

private val WEEKDAY_HEADERS = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

@Composable
fun SharedCalendarScreen(
    viewModel: SharedCalendarViewModel = remember { SharedCalendarViewModel() },
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val filteredEvents = remember(state.events, state.searchQuery) {
        viewModel.getFilteredEvents(state)
    }

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = "Islamic Calendar",
                subtitle = "Hijri & Gregorian Synchronized",
                navigationIcon = onBackClick?.let { action ->
                    { RafiqSharedBackButton(onClick = action) }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.goToToday() },
                        modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Month Header & Navigation Card
            item {
                RafiqSharedHeroCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { viewModel.previousMonth() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .rafiqPressable(targetScale = 0.88f)
                            ) {
                                Text("<", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${viewModel.getMonthName(state.displayedMonth)} ${state.displayedYear}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Hijri Synchronized",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.nextMonth() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .rafiqPressable(targetScale = 0.88f)
                            ) {
                                Text(">", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Weekdays
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WEEKDAY_HEADERS.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (dayName == "Fr") SecondaryGold else Color.White.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days Calendar Grid
                        val chunks = state.grid.chunked(7)
                        chunks.forEach { rowDays ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                for (col in 0 until 7) {
                                    val cell = rowDays.getOrNull(col)
                                    if (cell != null) {
                                        CalendarDayCell(
                                            day = cell,
                                            onClick = { viewModel.selectDay(cell) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Observances / Holidays
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search Islamic holidays & sunnah days...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Major Islamic Events (${filteredEvents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Events List
            items(filteredEvents) { event ->
                IslamicEventCard(
                    event = event,
                    getHijriMonthName = { viewModel.getHijriMonthName(it) }
                )
            }
        }
    }

    // Selected Day Modal Dialog
    state.selectedDay?.let { selected ->
        AlertDialog(
            onDismissRequest = { viewModel.selectDay(null) },
            confirmButton = {
                Button(
                    onClick = { viewModel.selectDay(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                ) {
                    Text("Close")
                }
            },
            title = {
                Text(
                    text = "${selected.gregorianDay} ${viewModel.getMonthName(selected.gregorianMonth)} ${selected.gregorianYear}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Hijri: ${selected.hijriDate.day} ${viewModel.getHijriMonthName(selected.hijriDate.month)} ${selected.hijriDate.year} H",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (selected.isToday) {
                        RafiqSharedBadge(text = "Today", isGold = true)
                    }

                    if (selected.events.isNotEmpty()) {
                        Text(
                            text = "Religious Observances:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        selected.events.forEach { ev ->
                            Text(
                                text = "• ${ev.titleEn} (${ev.titleId})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = ev.descriptionEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "No major Islamic events recorded on this date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun CalendarDayCell(
    day: SharedCalendarDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEvent = day.events.isNotEmpty()
    val bgColor = when {
        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        isEvent -> SecondaryGold.copy(alpha = 0.25f)
        else -> Color.Transparent
    }
    val border = if (day.isToday) BorderStroke(1.dp, Color.White) else null

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(if (border != null) Modifier.background(bgColor, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick)
            .rafiqPressable(targetScale = 0.90f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${day.gregorianDay}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = "${day.hijriDate.day}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = if (isEvent) SecondaryGold else Color.White.copy(alpha = 0.6f)
            )
            if (isEvent) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(SecondaryGold)
                )
            }
        }
    }
}

@Composable
private fun IslamicEventCard(
    event: IslamicEvent,
    getHijriMonthName: (Int) -> String,
    modifier: Modifier = Modifier
) {
    RafiqSharedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = event.titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                RafiqSharedBadge(
                    text = "${event.hijriDay} ${getHijriMonthName(event.hijriMonth)}",
                    isGold = event.eventType == "HOLIDAY"
                )
            }

            Text(
                text = event.titleId,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = event.descriptionEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
