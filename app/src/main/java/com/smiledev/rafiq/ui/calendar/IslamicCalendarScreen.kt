package com.smiledev.rafiq.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.data.models.IslamicEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicCalendarScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedEvent by remember { mutableStateOf<IslamicEvent?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Islamic Calendar") },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.fillMaxSize().padding(padding))
            }
            state.error != null -> {
                Text(
                    text = "Error: ${state.error}",
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (state.todayEvents.isNotEmpty()) {
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
                                        text = "Today's Events",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00695C)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                state.todayEvents.forEach { event ->
                                    Text(
                                        text = if (viewModel.localeCode == "id") event.titleId else event.titleEn,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    LazyRow(
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        items((1..12).toList()) { month ->
                            FilterChip(
                                label = { Text(viewModel.getMonthName(month), fontSize = 12.sp) },
                                selected = month == state.selectedMonth,
                                onClick = { viewModel.selectMonth(month) },
                                modifier = Modifier.padding(horizontal = 3.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF009688)
                                )
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (state.events.isEmpty()) {
                        Text(
                            text = "No events for this month",
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.events) { event ->
                                EventCard(
                                    event = event,
                                    localeCode = viewModel.localeCode,
                                    monthName = viewModel.getMonthName(event.hijriMonth),
                                    onClick = { selectedEvent = event }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedEvent?.let { event ->
        ModalBottomSheet(
            onDismissRequest = { selectedEvent = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (viewModel.localeCode == "id") event.titleId else event.titleEn,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${viewModel.getMonthName(event.hijriMonth)} ${event.hijriDay}",
                    color = Color(0xFF009688),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (viewModel.localeCode == "id") event.descriptionId else event.descriptionEn,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EventCard(event: IslamicEvent, localeCode: String, monthName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (event.eventType) {
                "holiday" -> "\uD83C\uDF89"
                "observance" -> "\uD83C\uDF19"
                else -> "\uD83D\uDCC5"
            }
            Text(text = icon, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (localeCode == "id") event.titleId else event.titleEn,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$monthName ${event.hijriDay}",
                    color = Color(0xFF009688),
                    fontSize = 12.sp
                )
            }
        }
    }
}
