package com.smiledev.rafiq_quran.ui.sunnahguidance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.domain.model.SunnahCategory
import com.smiledev.rafiq_quran.domain.model.SunnahGuidanceItem
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.chip.RafiqFilterChip
import com.smiledev.rafiq_quran.ui.designsystem.input.RafiqSearchBar
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqEmptyState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqErrorState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqLoadingIndicator

private val arabicFont = FontFamily(Font(R.font.me_quran))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunnahGuidanceScreen(
    onSunnahClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SunnahGuidanceViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isId = viewModel.localeCode == "id"
    val filteredItems = remember(state.items, state.selectedCategory, state.searchQuery) {
        viewModel.filterItems(state)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.sunnah_guidance),
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    RafiqLoadingIndicator(modifier = Modifier.fillMaxSize())
                }
                state.error != null && state.items.isEmpty() -> {
                    RafiqErrorState(
                        error = state.error!!,
                        onRetry = { viewModel.loadSunnahGuidance() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Search Bar
                        RafiqSearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.search(it) },
                            placeholder = stringResource(R.string.search_sunnah_guide),
                            modifier = Modifier.padding(
                                horizontal = RafiqTheme.spacing.l,
                                vertical = RafiqTheme.spacing.xs
                            )
                        )

                        // 2. Category Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(
                                    horizontal = RafiqTheme.spacing.l,
                                    vertical = RafiqTheme.spacing.xs
                                ),
                            horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)
                        ) {
                            RafiqFilterChip(
                                selected = state.selectedCategory == null,
                                onClick = { viewModel.selectCategory(null) },
                                label = stringResource(R.string.category_all)
                            )
                            RafiqFilterChip(
                                selected = state.selectedCategory == SunnahCategory.PRAYER,
                                onClick = { viewModel.selectCategory(SunnahCategory.PRAYER) },
                                label = stringResource(R.string.category_sunnah_prayer)
                            )
                            RafiqFilterChip(
                                selected = state.selectedCategory == SunnahCategory.DAILY_LIFESTYLE,
                                onClick = { viewModel.selectCategory(SunnahCategory.DAILY_LIFESTYLE) },
                                label = stringResource(R.string.category_daily_lifestyle)
                            )
                            RafiqFilterChip(
                                selected = state.selectedCategory == SunnahCategory.FRIDAY,
                                onClick = { viewModel.selectCategory(SunnahCategory.FRIDAY) },
                                label = stringResource(R.string.category_friday)
                            )
                            RafiqFilterChip(
                                selected = state.selectedCategory == SunnahCategory.FASTING,
                                onClick = { viewModel.selectCategory(SunnahCategory.FASTING) },
                                label = stringResource(R.string.category_fasting)
                            )
                            RafiqFilterChip(
                                selected = state.selectedCategory == SunnahCategory.DHIKR_DUA,
                                onClick = { viewModel.selectCategory(SunnahCategory.DHIKR_DUA) },
                                label = stringResource(R.string.category_dhikr_dua)
                            )
                        }

                        // 3. List of Sunnah Guides
                        if (filteredItems.isEmpty()) {
                            RafiqEmptyState(
                                title = stringResource(R.string.no_sunnah_found),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                fullScreen = false
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(
                                    horizontal = RafiqTheme.spacing.l,
                                    vertical = RafiqTheme.spacing.s
                                ),
                                verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.m)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    SunnahGuidanceCard(
                                        item = item,
                                        isId = isId,
                                        onClick = { onSunnahClick(item.id) }
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

@Composable
private fun SunnahGuidanceCard(
    item: SunnahGuidanceItem,
    isId: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RafiqCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RafiqTheme.customShapes.large,
        border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
        contentPadding = RafiqTheme.spacing.l
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category badge
            val (badgeText, badgeColor, badgeTextColor) = when (item.category) {
                SunnahCategory.PRAYER -> Triple(
                    stringResource(R.string.category_sunnah_prayer),
                    Color(0xFF0D9488).copy(alpha = 0.15f),
                    Color(0xFF0D9488)
                )
                SunnahCategory.DAILY_LIFESTYLE -> Triple(
                    stringResource(R.string.category_daily_lifestyle),
                    Color(0xFF2563EB).copy(alpha = 0.15f),
                    Color(0xFF2563EB)
                )
                SunnahCategory.FRIDAY -> Triple(
                    stringResource(R.string.category_friday),
                    Color(0xFF16A34A).copy(alpha = 0.15f),
                    Color(0xFF16A34A)
                )
                SunnahCategory.FASTING -> Triple(
                    stringResource(R.string.category_fasting),
                    Color(0xFFD97706).copy(alpha = 0.15f),
                    Color(0xFFD97706)
                )
                SunnahCategory.DHIKR_DUA -> Triple(
                    stringResource(R.string.category_dhikr_dua),
                    Color(0xFF7C3AED).copy(alpha = 0.15f),
                    Color(0xFF7C3AED)
                )
            }

            Surface(
                shape = RafiqTheme.customShapes.pill,
                color = badgeColor
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeTextColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Steps count badge
            Surface(
                shape = RafiqTheme.customShapes.pill,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = stringResource(R.string.sunnah_steps_count, item.steps.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(Modifier.height(RafiqTheme.spacing.s))

        // Arabic Calligraphy Header
        Text(
            text = item.titleArabic,
            fontFamily = arabicFont,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Right,
            style = TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.Right),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(RafiqTheme.spacing.xs))

        // Title
        Text(
            text = if (isId) item.titleId else item.titleEn,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(RafiqTheme.spacing.xxs))

        // Summary
        Text(
            text = if (isId) item.summaryId else item.summaryEn,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )

        // Dalil Reference Preview if available
        val hadithRef = item.hadithReference
        val surahRef = item.surahReference
        if (hadithRef != null || surahRef != null) {
            Spacer(Modifier.height(RafiqTheme.spacing.s))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RafiqTheme.customShapes.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = surahRef ?: hadithRef ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(RafiqTheme.spacing.m))

        // Bottom row: Virtues info and Details link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val virtues = if (isId) item.virtuesId else item.virtuesEn
            if (virtues.isNotEmpty()) {
                Text(
                    text = "★ ${virtues.first()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD97706),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }

            Text(
                text = stringResource(R.string.view_details),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
