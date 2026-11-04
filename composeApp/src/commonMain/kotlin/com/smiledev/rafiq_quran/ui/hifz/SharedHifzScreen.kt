package com.smiledev.rafiq_quran.ui.hifz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.domain.repository.HifzGoal
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBackButton
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeroCard
import com.smiledev.rafiq_quran.ui.theme.PrimaryTeal
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme
import com.smiledev.rafiq_quran.ui.theme.SecondaryGold
import com.smiledev.rafiq_quran.ui.theme.rafiqPressable

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedHifzScreen(
    viewModel: SharedHifzViewModel = remember { SharedHifzViewModel() },
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = "Hifz Tracker",
                subtitle = "Al-Qur'an Memorization Journey",
                navigationIcon = onBackClick?.let { action ->
                    { RafiqSharedBackButton(onClick = action) }
                },
                actions = {
                    TextButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                    ) {
                        Text(
                            text = "+ New Goal",
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
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Hero Memorization Stats Card
            item {
                RafiqSharedHeroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "📖 ${state.totalAyahsMemorized}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "of ${state.totalAyahsTarget} ayahs",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            Text(
                                text = "${state.completedGoalsCount} of ${state.goals.size} Surah goals completed (${state.overallPercentage}%)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.80f)
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        RafiqSharedBadge(
                            text = "${state.overallPercentage}% Done",
                            isGold = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { (state.overallPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = SecondaryGold,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }

            // 2. Filter Chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HifzFilter.values().forEach { filterOption ->
                        val isSelected = state.filter == filterOption
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(filterOption) },
                            label = { Text(filterOption.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // 3. Goal Items
            if (state.filteredGoals.isEmpty()) {
                item {
                    RafiqSharedCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No memorization goals in this filter.\nTap '+ New Goal' above to start tracking!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            } else {
                items(state.filteredGoals, key = { it.id }) { goal ->
                    HifzGoalCard(
                        goal = goal,
                        onToggleAyah = { aya -> viewModel.toggleAyah(goal.id, aya) },
                        onMarkAll = { memorized -> viewModel.markAllAyahs(goal.id, memorized) },
                        onDelete = { viewModel.deleteGoal(goal.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddHifzGoalDialog(
            errorMessage = state.formError,
            onDismiss = {
                viewModel.clearFormError()
                showAddDialog = false
            },
            onAdd = { num, name, start, end ->
                val success = viewModel.addGoal(num, name, start, end)
                if (success) {
                    showAddDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HifzGoalCard(
    goal: HifzGoal,
    onToggleAyah: (Int) -> Unit,
    onMarkAll: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    RafiqSharedCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${goal.suraNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = goal.suraName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ayah ${goal.ayaStart} – ${goal.ayaEnd} (${goal.memorizedCount}/${goal.totalAyahs} memorized)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            RafiqSharedBadge(
                text = if (goal.isComplete) "Mastered" else "${goal.progressPercent}%",
                isGold = !goal.isComplete
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { (goal.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (goal.isComplete) PrimaryTeal else SecondaryGold,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Ayah Chips Grid
        if (expanded) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (aya in goal.ayaStart..goal.ayaEnd) {
                    val isMemorized = aya in goal.memorizedAyahs
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .rafiqPressable(targetScale = 0.90f) { onToggleAyah(aya) }
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isMemorized) PrimaryTeal else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$aya",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isMemorized) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isMemorized) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.rafiqPressable(targetScale = 0.94f)
            ) {
                Text(
                    text = if (expanded) "Collapse" else "Expand Ayahs (${goal.totalAyahs})",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { onMarkAll(!goal.isComplete) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                ) {
                    Text(
                        text = if (goal.isComplete) "Reset" else "Mark All",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddHifzGoalDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAdd: (suraNumber: Int, suraName: String, ayaStart: Int, ayaEnd: Int) -> Unit
) {
    var suraNumberText by remember { mutableStateOf("") }
    var suraNameText by remember { mutableStateOf("") }
    var ayaStartText by remember { mutableStateOf("1") }
    var ayaEndText by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Memorization Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = suraNumberText,
                    onValueChange = { suraNumberText = it.filter { char -> char.isDigit() } },
                    label = { Text("Surah Number (1-114)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = suraNameText,
                    onValueChange = { suraNameText = it },
                    label = { Text("Surah Name (e.g. Al-Mulk)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = ayaStartText,
                        onValueChange = { ayaStartText = it.filter { char -> char.isDigit() } },
                        label = { Text("Ayah Start") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = ayaEndText,
                        onValueChange = { ayaEndText = it.filter { char -> char.isDigit() } },
                        label = { Text("Ayah End") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = suraNumberText.toIntOrNull() ?: 0
                    val start = ayaStartText.toIntOrNull() ?: 1
                    val end = ayaEndText.toIntOrNull() ?: 1
                    onAdd(num, suraNameText, start, end)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text("Add Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
