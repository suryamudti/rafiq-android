package com.smiledev.rafiq_quran.ui.tasbih

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.domain.model.TasbihItem
import com.smiledev.rafiq_quran.theme.Gold500
import com.smiledev.rafiq_quran.theme.Gold700
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqEnterTransition
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqExitTransition
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.arabic.RafiqArabicText
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqBadge
import com.smiledev.rafiq_quran.ui.designsystem.button.RafiqButton
import com.smiledev.rafiq_quran.ui.designsystem.button.RafiqOutlinedButton
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.chip.RafiqFilterChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    viewModel: TasbihViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isIndonesian = currentLocaleCode() == "id"

    Scaffold(
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.tasbih_counter),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.tasbih_history),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleSound() },
                        modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (state.isSoundEnabled) R.drawable.ic_volume_up else R.drawable.ic_volume_off
                            ),
                            contentDescription = stringResource(R.string.tasbih_sound),
                            tint = if (state.isSoundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleVibration() },
                        modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (state.isVibrationEnabled) R.drawable.ic_vibrate else R.drawable.ic_vibrate_off
                            ),
                            contentDescription = stringResource(R.string.tasbih_vibration),
                            tint = if (state.isVibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Active Dhikr Card with specular border highlight
            ActiveDhikrCard(
                selectedItem = state.selectedItem,
                isIndonesian = isIndonesian,
                onChangeDhikrClick = { viewModel.setShowDhikrPicker(true) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

            // 2. Target Preset Chips
            TargetPresetRow(
                currentTarget = state.target,
                onSelectTarget = { target -> viewModel.setTarget(target) },
                onCustomTargetClick = { viewModel.setShowCustomTargetDialog(true) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

            // 3. Stats row (Lap, Target, Today)
            StatsRow(
                lap = state.lap,
                target = state.target,
                todayTotal = state.todayDhikrCount,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))

            // 4. Hero Circular Counter Area with tactile fluid spring physics
            CircularCounter(
                count = state.count,
                progress = state.progress,
                isTargetReached = state.isTargetReachedNotice,
                onTap = { viewModel.increment() },
                modifier = Modifier.padding(vertical = RafiqTheme.spacing.m)
            )

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))

            // 5. Bottom Control Row (Undo, Reset)
            BottomControlRow(
                canUndo = state.count > 0,
                onUndo = { viewModel.decrement() },
                onReset = { viewModel.setShowResetDialog(true) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))
        }
    }

    // Modal Bottom Sheet: Dhikr Selection
    if (state.showDhikrPicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowDhikrPicker(false) },
            sheetState = sheetState
        ) {
            DhikrPickerSheetContent(
                items = state.items,
                selectedId = state.selectedItem?.id,
                isIndonesian = isIndonesian,
                onSelect = { item -> viewModel.selectDhikr(item) },
                onSelectCustom = { viewModel.selectDhikr(null) }
            )
        }
    }

    // Dialog: Reset Confirmation
    if (state.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowResetDialog(false) },
            title = {
                Text(
                    text = stringResource(R.string.tasbih_reset_title),
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.1).sp),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.tasbih_reset_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.reset(resetAll = true) },
                    modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.ChipPressScale)
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_reset_all),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { viewModel.setShowResetDialog(false) },
                        modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.ChipPressScale)
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = { viewModel.reset(resetAll = false) },
                        modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.ChipPressScale)
                    ) {
                        Text(text = stringResource(R.string.tasbih_reset_current))
                    }
                }
            }
        )
    }

    // Dialog: Custom Target
    if (state.showCustomTargetDialog) {
        var targetText by remember { mutableStateOf(if (state.target > 0) state.target.toString() else "33") }
        AlertDialog(
            onDismissRequest = { viewModel.setShowCustomTargetDialog(false) },
            title = {
                Text(
                    text = stringResource(R.string.tasbih_custom_target_title),
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.1).sp),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input -> targetText = input.filter { it.isDigit() } },
                    label = { Text(text = stringResource(R.string.tasbih_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val num = targetText.toIntOrNull() ?: 33
                        viewModel.setTarget(num)
                    },
                    modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.ChipPressScale)
                ) {
                    Text(text = stringResource(R.string.confirm), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.setShowCustomTargetDialog(false) },
                    modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.ChipPressScale)
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ActiveDhikrCard(
    selectedItem: TasbihItem?,
    isIndonesian: Boolean,
    onChangeDhikrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RafiqCard(
        modifier = modifier,
        contentPadding = RafiqTheme.spacing.m,
        border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder)
    ) {
        if (selectedItem != null) {
            val meaning = if (isIndonesian) selectedItem.meaningId else selectedItem.meaningEn
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RafiqArabicText(
                    text = selectedItem.arabic,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))
                Text(
                    text = selectedItem.transliteration,
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.1).sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))
                TextButton(
                    onClick = onChangeDhikrClick,
                    modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.ChipPressScale),
                    contentPadding = PaddingValues(horizontal = RafiqTheme.spacing.m, vertical = RafiqTheme.spacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_change_dhikr),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.tasbih_custom_dhikr),
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.1).sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))
                TextButton(
                    onClick = onChangeDhikrClick,
                    modifier = Modifier.rafiqPressable(targetScale = RafiqMotion.ChipPressScale),
                    contentPadding = PaddingValues(horizontal = RafiqTheme.spacing.m, vertical = RafiqTheme.spacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_select_dhikr),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetPresetRow(
    currentTarget: Int,
    onSelectTarget: (Int) -> Unit,
    onCustomTargetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(33, 99, 100, 0)
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s, Alignment.CenterHorizontally)
    ) {
        items(presets) { target ->
            val label = if (target == 0) stringResource(R.string.tasbih_free_target) else target.toString()
            RafiqFilterChip(
                selected = currentTarget == target,
                onClick = { onSelectTarget(target) },
                label = label
            )
        }
        item {
            val isCustom = currentTarget !in presets
            RafiqFilterChip(
                selected = isCustom,
                onClick = onCustomTargetClick,
                label = if (isCustom) "$currentTarget" else stringResource(R.string.tasbih_custom_target)
            )
        }
    }
}

@Composable
private fun StatsRow(
    lap: Int,
    target: Int,
    todayTotal: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RafiqBadge(
            text = "${stringResource(R.string.tasbih_lap)} $lap",
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )
        RafiqBadge(
            text = if (target > 0) "${stringResource(R.string.tasbih_target)}: $target" else "${stringResource(R.string.tasbih_target)}: ∞",
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
        )
        RafiqBadge(
            text = "${stringResource(R.string.tasbih_today_count)}: $todayTotal",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder)
        )
    }
}

@Composable
private fun CircularCounter(
    count: Int,
    progress: Float,
    isTargetReached: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Emil Kowalski spring motion: Medium bouncy (0.75f) with 400f stiffness for snappy zero-delay mechanical click response
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 400f
        ),
        label = "tasbih_press_scale"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = RafiqMotion.DurationMedium, easing = EaseOutCubic),
        label = "progress_arc"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val strokeColor = if (progress >= 1f && progress > 0f) Gold500 else primaryColor

    val counterDescription = if (isTargetReached) {
        stringResource(R.string.tasbih_target_reached_semantics, count)
    } else {
        stringResource(
            R.string.tasbih_counter_semantics,
            count,
            (progress.coerceIn(0f, 1f) * 100).toInt()
        )
    }

    Box(
        modifier = modifier
            .size(260.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics(mergeDescendants = true) {
                contentDescription = counterDescription
                role = Role.Button
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer Progress Ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clearAndSetSemantics { }
        ) {
            val strokeWidth = 14.dp.toPx()
            // Track
            drawArc(
                color = surfaceVariant.copy(alpha = 0.6f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Animated Progress
            if (animatedProgress > 0f) {
                drawArc(
                    color = strokeColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Inner Circle Card with specular highlight border
        Surface(
            modifier = Modifier.size(210.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardHighlight),
            shadowElevation = RafiqTheme.elevation.high
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.0).sp
                    ),
                    color = strokeColor
                )
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.tap_to_count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedVisibility(
                    visible = isTargetReached,
                    enter = rafiqEnterTransition(initialScale = 0.95f),
                    exit = rafiqExitTransition(targetScale = 0.95f)
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_target_completed),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = Gold700,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = RafiqTheme.spacing.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomControlRow(
    canUndo: Boolean,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.m, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RafiqOutlinedButton(
            onClick = onUndo,
            enabled = canUndo,
            text = stringResource(R.string.tasbih_undo),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = stringResource(R.string.tasbih_undo),
                    modifier = Modifier.size(RafiqTheme.iconSizes.s)
                )
            },
            modifier = Modifier.weight(1f)
        )
        RafiqButton(
            onClick = onReset,
            text = stringResource(R.string.reset),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.reset),
                    modifier = Modifier.size(RafiqTheme.iconSizes.s)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DhikrPickerSheetContent(
    items: List<TasbihItem>,
    selectedId: Int?,
    isIndonesian: Boolean,
    onSelect: (TasbihItem) -> Unit,
    onSelectCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RafiqTheme.spacing.l)
            .padding(bottom = RafiqTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)
    ) {
        item {
            Text(
                text = stringResource(R.string.tasbih_select_dhikr),
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = (-0.2).sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = RafiqTheme.spacing.m)
            )
        }

        items(items) { item ->
            val isSelected = item.id == selectedId
            val meaning = if (isIndonesian) item.meaningId else item.meaningEn

            RafiqCard(
                onClick = { onSelect(item) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = RafiqTheme.spacing.m,
                border = BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else RafiqTheme.extendedColors.cardBorder
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        RafiqArabicText(
                            text = item.arabic,
                            fontSize = 22.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))
                        Text(
                            text = item.transliteration,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = meaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(RafiqTheme.spacing.m))
                    RafiqBadge(
                        text = "${item.defaultCount}x",
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else RafiqTheme.extendedColors.cardBorder)
                    )
                }
            }
        }

        item {
            RafiqCard(
                onClick = onSelectCustom,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = RafiqTheme.spacing.m,
                border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_custom_dhikr),
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.1).sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    RafiqBadge(
                        text = stringResource(R.string.tasbih_free_target),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                    )
                }
            }
        }
    }
}
