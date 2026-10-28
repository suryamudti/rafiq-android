package com.smiledev.rafiq_quran.ui.prayerguidance

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CardDefaults
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
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceCategory
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceItem
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
fun PrayerGuidanceScreen(
    onGuidanceClick: (String) -> Unit,
    onBack: () -> Unit,
    onExploreSunnahClick: (() -> Unit)? = null,
    viewModel: PrayerGuidanceViewModel = hiltViewModel(),
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
                title = stringResource(R.string.prayer_guidance),
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
                        onRetry = { viewModel.loadGuidance() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Search Bar
                        RafiqSearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.search(it) },
                            placeholder = stringResource(R.string.search_prayer_guide),
                            modifier = Modifier.padding(
                                horizontal = RafiqTheme.spacing.l,
                                vertical = RafiqTheme.spacing.xs
                            )
                        )

                        // Banner to explore Sunnah guidance
                        if (onExploreSunnahClick != null) {
                            RafiqCard(
                                onClick = onExploreSunnahClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = RafiqTheme.spacing.l,
                                        vertical = RafiqTheme.spacing.xs
                                    ),
                                shape = RafiqTheme.customShapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF00796B).copy(alpha = 0.10f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF00796B).copy(alpha = 0.25f)),
                                contentPadding = RafiqTheme.spacing.m
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.sunnah_guidance),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00796B)
                                        )
                                        Text(
                                            text = stringResource(R.string.sunnah_guidance_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "→",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00796B),
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(start = RafiqTheme.spacing.s)
                                    )
                                }
                            }
                        }

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
                                selected = state.selectedCategory == PrayerGuidanceCategory.OBLIGATORY,
                                onClick = { viewModel.selectCategory(PrayerGuidanceCategory.OBLIGATORY) },
                                label = stringResource(R.string.category_fardhu)
                            )
                            RafiqFilterChip(
                                selected = state.selectedCategory == PrayerGuidanceCategory.SUNNAH,
                                onClick = { viewModel.selectCategory(PrayerGuidanceCategory.SUNNAH) },
                                label = stringResource(R.string.category_sunnah)
                            )
                            RafiqFilterChip(
                                selected = state.selectedCategory == PrayerGuidanceCategory.PURIFICATION,
                                onClick = { viewModel.selectCategory(PrayerGuidanceCategory.PURIFICATION) },
                                label = stringResource(R.string.category_purification)
                            )
                            RafiqFilterChip(
                                selected = state.selectedCategory == PrayerGuidanceCategory.POST_PRAYER,
                                onClick = { viewModel.selectCategory(PrayerGuidanceCategory.POST_PRAYER) },
                                label = stringResource(R.string.category_post_prayer)
                            )
                        }

                        // 3. List of Guides
                        if (filteredItems.isEmpty()) {
                            RafiqEmptyState(
                                title = stringResource(R.string.no_guides_found),
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
                                    PrayerGuidanceCard(
                                        item = item,
                                        isId = isId,
                                        onClick = { onGuidanceClick(item.id) }
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
private fun PrayerGuidanceCard(
    item: PrayerGuidanceItem,
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
            // Category & Rakaat Badges
            Row(horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.xs)) {
                val (badgeText, badgeColor, badgeTextColor) = when (item.category) {
                    PrayerGuidanceCategory.OBLIGATORY -> Triple(
                        stringResource(R.string.category_fardhu),
                        Color(0xFF00796B).copy(alpha = 0.15f),
                        Color(0xFF00796B)
                    )
                    PrayerGuidanceCategory.SUNNAH -> Triple(
                        stringResource(R.string.category_sunnah),
                        Color(0xFFD97706).copy(alpha = 0.15f),
                        Color(0xFFD97706)
                    )
                    PrayerGuidanceCategory.PURIFICATION -> Triple(
                        stringResource(R.string.category_purification),
                        Color(0xFF2563EB).copy(alpha = 0.15f),
                        Color(0xFF2563EB)
                    )
                    PrayerGuidanceCategory.POST_PRAYER -> Triple(
                        stringResource(R.string.category_post_prayer),
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

                val rakaat = item.rakaat
                if (rakaat != null) {
                    Surface(
                        shape = RafiqTheme.customShapes.pill,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.rakaat_count, rakaat),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Arabic title on the right
            Text(
                text = item.nameArabic,
                fontFamily = arabicFont,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right,
                style = TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.Right)
            )
        }

        Spacer(Modifier.height(RafiqTheme.spacing.s))

        // Primary title
        Text(
            text = if (isId) item.nameId else item.nameEn,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(RafiqTheme.spacing.xxs))

        // Description
        Text(
            text = if (isId) item.descriptionId else item.descriptionEn,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(RafiqTheme.spacing.m))

        // Footer info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.guidance_steps_count, item.steps.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.view_details),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
