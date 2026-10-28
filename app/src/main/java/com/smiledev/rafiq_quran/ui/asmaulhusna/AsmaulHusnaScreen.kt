package com.smiledev.rafiq_quran.ui.asmaulhusna

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.theme.ArabicFontFamily
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.button.RafiqButton
import com.smiledev.rafiq_quran.ui.designsystem.button.RafiqIconButton
import com.smiledev.rafiq_quran.ui.designsystem.button.RafiqOutlinedButton
import com.smiledev.rafiq_quran.ui.designsystem.button.RafiqTextButton
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.chip.RafiqFilterChip
import com.smiledev.rafiq_quran.ui.designsystem.input.RafiqSearchBar
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqEmptyState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqErrorState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqLoadingIndicator

private val arabicFont = ArabicFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsmaulHusnaScreen(
    onBack: () -> Unit,
    viewModel: AsmaulHusnaViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val filtered = remember(state.names, state.searchQuery, state.showFavoritesOnly, state.favoriteIds) {
        viewModel.filterNames(state)
    }
    val dailyName = remember(state.names) {
        viewModel.getDailyName(state.names)
    }
    val locale = viewModel.localeCode
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    Scaffold(
        modifier = modifier,
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.asmaul_husna_title),
                onBack = onBack,
                actions = {
                    RafiqTextButton(
                        text = stringResource(R.string.asmaul_husna_random),
                        onClick = {
                            viewModel.getRandomName()?.let { viewModel.selectName(it) }
                        }
                    )
                    RafiqIconButton(onClick = { viewModel.setGridView(!state.isGridView) }) {
                        Text(
                            text = if (state.isGridView) "☰" else "⊞",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.names.isEmpty() -> {
                RafiqLoadingIndicator(modifier = Modifier.padding(padding))
            }
            state.error != null && state.names.isEmpty() -> {
                val err = state.error
                if (err != null) {
                    RafiqErrorState(
                        error = err,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Search Bar
                    RafiqSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.search(it) },
                        placeholder = stringResource(R.string.search_names),
                        modifier = Modifier.padding(
                            horizontal = RafiqTheme.spacing.l,
                            vertical = RafiqTheme.spacing.xs
                        )
                    )

                    // Filters and Count Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)) {
                            RafiqFilterChip(
                                selected = !state.showFavoritesOnly,
                                onClick = { viewModel.setShowFavoritesOnly(false) },
                                label = stringResource(R.string.asmaul_husna_all)
                            )
                            RafiqFilterChip(
                                selected = state.showFavoritesOnly,
                                onClick = { viewModel.setShowFavoritesOnly(true) },
                                label = "${stringResource(R.string.asmaul_husna_favorites)} (${state.favoriteIds.size})",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (state.showFavoritesOnly) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                            )
                        }

                        Text(
                            text = stringResource(
                                R.string.asmaul_husna_count_format,
                                filtered.size,
                                state.names.size
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Daily Reflection Card (shown when not searching and not favorite-filtered)
                    if (state.searchQuery.isEmpty() && !state.showFavoritesOnly && dailyName != null) {
                        val isFavDaily = dailyName.id in state.favoriteIds
                        RafiqCard(
                            onClick = { viewModel.selectName(dailyName) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = RafiqTheme.spacing.l,
                                    vertical = RafiqTheme.spacing.xs
                                ),
                            shape = RafiqTheme.customShapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            contentPadding = RafiqTheme.spacing.l
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✨ ${stringResource(R.string.asmaul_husna_daily_title)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Favorite",
                                    modifier = Modifier
                                        .size(22.dp)
                                        .rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                                        .clickable { viewModel.toggleFavorite(dailyName.id) },
                                    tint = if (isFavDaily) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                )
                            }
                            Spacer(Modifier.height(RafiqTheme.spacing.s))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${dailyName.id}. ${dailyName.transliteration}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.1).sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (locale == "id") dailyName.meaningId else dailyName.meaningEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(RafiqTheme.spacing.xs))
                                    Text(
                                        text = stringResource(R.string.asmaul_husna_reflect),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(RafiqTheme.spacing.m))
                                Text(
                                    text = dailyName.arabic,
                                    fontFamily = arabicFont,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Content: Grid or List
                    if (filtered.isEmpty()) {
                        RafiqEmptyState(
                            title = stringResource(R.string.no_names_found),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            fullScreen = false
                        )
                    } else if (state.isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = RafiqTheme.spacing.m,
                                vertical = RafiqTheme.spacing.s
                            ),
                            horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s),
                            verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                val isFav = item.id in state.favoriteIds
                                RafiqCard(
                                    onClick = { viewModel.selectName(item) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RafiqTheme.customShapes.large,
                                    border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
                                    contentPadding = RafiqTheme.spacing.m
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${item.id}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = "Favorite",
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                                                    .clickable { viewModel.toggleFavorite(item.id) },
                                                tint = if (isFav) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outlineVariant
                                                }
                                            )
                                        }
                                        Spacer(Modifier.height(RafiqTheme.spacing.xs))
                                        Text(
                                            text = item.arabic,
                                            fontFamily = arabicFont,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(RafiqTheme.spacing.xs))
                                        Text(
                                            text = item.transliteration,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (locale == "id") item.meaningId else item.meaningEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = RafiqTheme.spacing.l,
                                vertical = RafiqTheme.spacing.s
                            ),
                            verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                val isFav = item.id in state.favoriteIds
                                RafiqCard(
                                    onClick = { viewModel.selectName(item) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RafiqTheme.customShapes.medium,
                                    border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
                                    contentPadding = RafiqTheme.spacing.m
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${item.id}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(Modifier.width(RafiqTheme.spacing.m))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.transliteration,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (locale == "id") item.meaningId else item.meaningEn,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.width(RafiqTheme.spacing.s))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = item.arabic,
                                                fontFamily = arabicFont,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.height(RafiqTheme.spacing.xxs))
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = "Favorite",
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
                                                    .clickable { viewModel.toggleFavorite(item.id) },
                                                tint = if (isFav) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outlineVariant
                                                }
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
    }

    // Detail ModalBottomSheet
    val selected = state.selectedName
    if (selected != null) {
        val isFav = selected.id in state.favoriteIds
        val meaning = if (locale == "id") selected.meaningId else selected.meaningEn
        val benefit = if (locale == "id") selected.benefitId else selected.benefitEn
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { viewModel.selectName(null) },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RafiqTheme.spacing.xl)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with number and favorite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RafiqTheme.customShapes.pill)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#${selected.id} of 99",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    RafiqIconButton(onClick = { viewModel.toggleFavorite(selected.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = if (isFav) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }

                Spacer(Modifier.height(RafiqTheme.spacing.m))

                // Arabic Calligraphy
                Text(
                    text = selected.arabic,
                    fontFamily = arabicFont,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(RafiqTheme.spacing.xs))

                // Transliteration
                Text(
                    text = selected.transliteration,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(RafiqTheme.spacing.xxs))

                // Meaning
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(RafiqTheme.spacing.xl))

                // Virtues & Benefits (Fadhilah)
                if (benefit.isNotBlank()) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RafiqTheme.customShapes.large,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(RafiqTheme.spacing.l)) {
                            Text(
                                text = stringResource(R.string.asmaul_husna_benefits_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(RafiqTheme.spacing.xs))
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(Modifier.height(RafiqTheme.spacing.l))
                }

                // Interactive Dhikr Counter
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RafiqTheme.customShapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RafiqTheme.spacing.l),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.asmaul_husna_dhikr_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(RafiqTheme.spacing.m))

                        // Large tactile tap target for counting with instant spring physics
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .rafiqPressable(targetScale = 0.90f)
                                .clickable {
                                    viewModel.incrementDhikr()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${state.dhikrCount}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = stringResource(R.string.count),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(Modifier.height(RafiqTheme.spacing.s))
                        Text(
                            text = stringResource(R.string.asmaul_husna_tap_to_count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (state.dhikrCount > 0) {
                            Spacer(Modifier.height(RafiqTheme.spacing.xs))
                            RafiqTextButton(
                                text = stringResource(R.string.reset),
                                color = MaterialTheme.colorScheme.error,
                                onClick = { viewModel.resetDhikr() }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(RafiqTheme.spacing.xl))

                // Actions: Copy and Share
                val benefitsTitle = stringResource(R.string.asmaul_husna_benefits_title)
                val copiedToast = stringResource(R.string.asmaul_husna_copied)
                val shareFormattedText = buildString {
                    append("${selected.id}. ${selected.arabic} (${selected.transliteration})\n")
                    append("$meaning\n\n")
                    if (benefit.isNotBlank()) {
                        append("$benefitsTitle:\n$benefit\n\n")
                    }
                    append("Shared via Rafiq App")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.m)
                ) {
                    RafiqOutlinedButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.asmaul_husna_copy),
                        onClick = {
                            clipboardManager.setText(AnnotatedString(shareFormattedText))
                            Toast.makeText(
                                context,
                                copiedToast,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    RafiqButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.asmaul_husna_share),
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, shareFormattedText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }
                    )
                }
            }
        }
    }
}
