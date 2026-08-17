package com.smiledev.rafiq.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.displayMessage
import com.smiledev.rafiq.domain.model.IslamicEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal fun eventColor(eventType: String): Color =
    when (eventType) {
        "holiday" -> Color(0xFFB8860B)
        "fasting" -> Color(0xFF7B1FA2)
        else -> Color(0xFF009688)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicCalendarScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val weekdays = stringArrayResource(R.array.weekdays_short)
    val hijriSuffix = stringResource(R.string.hijri_year_suffix)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.islamic_calendar)) },
                navigationIcon = {
                    Text(stringResource(R.string.back), modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize().padding(padding).semantics { contentDescription = "Loading" }
                )
            }
            state.error != null -> {
                Text(
                    text = state.error?.displayMessage ?: "",
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (state.todayEvents.isNotEmpty()) {
                        TodayEventsCard(events = state.todayEvents, localeCode = viewModel.localeCode)
                    }
                    HijriHeaderBanner(grid = state.grid, viewModel = viewModel, hijriSuffix = hijriSuffix)
                    MonthNavigator(state = state, viewModel = viewModel)
                    if (state.monthlyRecommendations.isNotEmpty()) {
                        state.monthlyRecommendations.forEach { rec ->
                            RecommendationBanner(title = if (viewModel.localeCode == "id") rec.titleId else rec.titleEn)
                        }
                    }
                    WeekdayHeader(weekdays = weekdays)
                    CalendarGrid(
                        grid = state.grid,
                        selectedIndex = state.selectedIndex,
                        onDayClick = viewModel::onDayClick
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    val selectedDay = state.selectedIndex?.let { state.grid.getOrNull(it) }
    if (selectedDay != null) {
        DayDetailSheet(
            day = selectedDay,
            displayedYear = state.displayedYear,
            displayedMonth = state.displayedMonth,
            viewModel = viewModel,
            hijriSuffix = hijriSuffix,
            onDismiss = viewModel::dismissDaySheet
        )
    }
}

@Composable
private fun TodayEventsCard(events: List<IslamicEvent>, localeCode: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "\uD83D\uDCC5", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.todays_events),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00695C)
                )
            }
            Spacer(Modifier.height(8.dp))
            events.forEach { event ->
                Text(
                    text = if (localeCode == "id") event.titleId else event.titleEn,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HijriHeaderBanner(
    grid: List<CalendarDay?>,
    viewModel: CalendarViewModel,
    hijriSuffix: String
) {
    val first = grid.firstOrNull { it != null }?.hijriDate
    val last = grid.lastOrNull { it != null }?.hijriDate
    if (first == null || last == null) return
    val monthName = { month: Int -> viewModel.getMonthName(month) }
    val headerText = if (first.month == last.month && first.year == last.year) {
        stringResource(R.string.hijri_header_prefix, "${monthName(first.month)} ${first.year} $hijriSuffix")
    } else {
        stringResource(
            R.string.hijri_header_prefix,
            "${monthName(first.month)} \u2013 ${monthName(last.month)} ${last.year} $hijriSuffix"
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF009688))
    ) {
        Text(
            text = headerText,
            modifier = Modifier.padding(16.dp),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MonthNavigator(state: CalendarUiState, viewModel: CalendarViewModel) {
    val label = remember(state.displayedYear, state.displayedMonth) {
        val cal = Calendar.getInstance().apply {
            clear()
            set(state.displayedYear, state.displayedMonth - 1, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "\u2039",
            fontSize = 28.sp,
            modifier = Modifier
                .clickable(onClick = viewModel::previousMonth)
                .padding(8.dp)
                .semantics { contentDescription = "Previous month" }
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = stringResource(R.string.today_button),
                color = Color(0xFF009688),
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(onClick = viewModel::goToToday)
                    .padding(vertical = 4.dp)
            )
        }
        Text(
            text = "\u203A",
            fontSize = 28.sp,
            modifier = Modifier
                .clickable(onClick = viewModel::nextMonth)
                .padding(8.dp)
                .semantics { contentDescription = "Next month" }
        )
    }
}

@Composable
private fun WeekdayHeader(weekdays: Array<String>) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        weekdays.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    grid: List<CalendarDay?>,
    selectedIndex: Int?,
    onDayClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    DayCell(
                        day = grid.getOrNull(index),
                        selected = index == selectedIndex,
                        onClick = { onDayClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    day: CalendarDay?,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (day == null) {
        Box(modifier = Modifier.weight(1f).height(58.dp))
        return
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    day.isToday -> Color(0xFFE0F2F1)
                    selected -> Color(0xFFB2DFDB)
                    else -> Color.Transparent
                }
            )
            .border(if (selected) 1.dp else 0.dp, Color(0xFF009688), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = day.gregorianDay.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = day.hijriDate.day.toString(),
            fontSize = 10.sp,
            color = Color.Gray
        )
        if (day.events.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.Center) {
                day.events.take(3).forEach { event ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(eventColor(event.eventType))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    day: CalendarDay,
    displayedYear: Int,
    displayedMonth: Int,
    viewModel: CalendarViewModel,
    hijriSuffix: String,
    onDismiss: () -> Unit
) {
    val hijri = day.hijriDate
    val gregorianLabel = remember(displayedYear, displayedMonth, day.gregorianDay) {
        val cal = Calendar.getInstance().apply {
            clear()
            set(displayedYear, displayedMonth - 1, day.gregorianDay)
        }
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(cal.time)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = gregorianLabel, fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${viewModel.getMonthName(hijri.month)} ${hijri.day}, ${hijri.year} $hijriSuffix",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF009688)
            )
            Spacer(Modifier.height(16.dp))
            if (day.events.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_events_on_day),
                    color = Color.Gray
                )
            } else {
                day.events.forEach { event ->
                    Text(
                        text = if (viewModel.localeCode == "id") event.titleId else event.titleEn,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (viewModel.localeCode == "id") event.descriptionId else event.descriptionEn,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecommendationBanner(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = Color(0xFF4A148C),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
