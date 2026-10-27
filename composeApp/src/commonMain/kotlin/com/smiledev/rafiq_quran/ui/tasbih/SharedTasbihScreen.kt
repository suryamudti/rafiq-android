package com.smiledev.rafiq_quran.ui.tasbih

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.domain.model.TasbihItem
import com.smiledev.rafiq_quran.platform.TasbihFeedbackController
import com.smiledev.rafiq_quran.platform.rememberTasbihFeedbackController
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.theme.PrimaryTeal
import com.smiledev.rafiq_quran.ui.theme.SecondaryGold

@Composable
fun SharedTasbihScreen(
    viewModel: SharedTasbihViewModel = remember { SharedTasbihViewModel() },
    feedbackController: TasbihFeedbackController = rememberTasbihFeedbackController(),
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = "Digital Tasbih",
                subtitle = "Lap ${state.lap}",
                navigationIcon = onBackClick?.let { action ->
                    {
                        IconButton(onClick = action) {
                            Text(
                                text = "←",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    RafiqSharedBadge(
                        text = "Total: ${state.totalCount}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target Reached Banner
            AnimatedVisibility(
                visible = state.isTargetReachedNotice,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SecondaryGold.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🎉 Target reached! Starting Lap ${state.lap}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryGold
                        )
                        TextButton(
                            onClick = { viewModel.dismissTargetReachedNotice() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("OK", color = SecondaryGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Dhikr Arabic Card
            RafiqSharedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setDhikrPickerVisible(true) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.selectedItem.arabic,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 32.sp,
                            lineHeight = 44.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.selectedItem.transliteration,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = state.selectedItem.meaningEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.setDhikrPickerVisible(true) },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Select Dhikr ▾", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Circular Counter
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.onIncrement(feedbackController)
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Tasbih counter: ${state.count} of ${state.target}. Tap to count."
                    },
                contentAlignment = Alignment.Center
            ) {
                val progress = state.progress
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    val strokeWidth = 14.dp.toPx()

                    // Background track
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )

                    // Progress arc
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${state.count}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "/ ${state.target}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "TAP TO COUNT",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick Target Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                listOf(33, 99, 100).forEach { targetValue ->
                    val isSelected = state.target == targetValue
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setTarget(targetValue) },
                        label = { Text("$targetValue") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                val isCustom = state.target !in listOf(33, 99, 100)
                FilterChip(
                    selected = isCustom,
                    onClick = { viewModel.setCustomTargetDialogVisible(true) },
                    label = { Text(if (isCustom) "Target: ${state.target}" else "Custom") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            // Control Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vibration Toggle
                OutlinedButton(
                    onClick = { viewModel.toggleVibration() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (state.isVibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(if (state.isVibrationEnabled) "📳 Haptic On" else "📴 Haptic Off")
                }

                // Sound Toggle
                OutlinedButton(
                    onClick = { viewModel.toggleSound() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (state.isSoundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(if (state.isSoundEnabled) "🔊 Sound On" else "🔇 Sound Off")
                }

                // Reset Action
                OutlinedButton(
                    onClick = { viewModel.setResetDialogVisible(true) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("↺ Reset")
                }
            }

            // Decrement & Manual Add Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.onDecrement() },
                    enabled = state.count > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("− Undo Count (-1)")
                }

                Button(
                    onClick = { viewModel.onIncrement(feedbackController) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("+ Count (+1)")
                }
            }
        }
    }

    // Dhikr Selection Dialog
    if (state.showDhikrPicker) {
        AlertDialog(
            onDismissRequest = { viewModel.setDhikrPickerVisible(false) },
            title = { Text("Select Dhikr") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items) { item ->
                        val isSelected = item.id == state.selectedItem.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onSelectItem(item) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = item.arabic,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.transliteration} • Target: ${item.defaultCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setDhikrPickerVisible(false) }) {
                    Text("Close")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (state.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setResetDialogVisible(false) },
            title = { Text("Reset Counter?") },
            text = { Text("Are you sure you want to reset the current count and lap back to 0?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.onReset() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setResetDialogVisible(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Custom Target Dialog
    if (state.showCustomTargetDialog) {
        var customInput by remember { mutableStateOf("${state.target}") }
        AlertDialog(
            onDismissRequest = { viewModel.setCustomTargetDialogVisible(false) },
            title = { Text("Custom Target") },
            text = {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Enter target count") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = customInput.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            viewModel.setTarget(parsed)
                        }
                    }
                ) {
                    Text("Set Target")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setCustomTargetDialogVisible(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}
