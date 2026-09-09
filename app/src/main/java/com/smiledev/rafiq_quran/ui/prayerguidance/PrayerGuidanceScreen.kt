package com.smiledev.rafiq_quran.ui.prayerguidance

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceCategory
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceItem

private val arabicFont = FontFamily(Font(R.font.me_quran))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerGuidanceScreen(
    onGuidanceClick: (String) -> Unit,
    onBack: () -> Unit,
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
            TopAppBar(
                title = {
                    Text(
                        text = if (isId) "Panduan Sholat" else stringResource(R.string.prayer_guidance),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    Text(
                        text = stringResource(R.string.back),
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .padding(16.dp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.error != null && state.items.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error?.displayMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.loadGuidance() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Search Bar
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.search(it) },
                            placeholder = {
                                Text(
                                    if (isId) "Cari panduan sholat, wudhu, dzikir…"
                                    else stringResource(R.string.search_prayer_guide)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        // 2. Category Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedCategory == null,
                                onClick = { viewModel.selectCategory(null) },
                                label = { Text(if (isId) "Semua" else stringResource(R.string.category_all)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = state.selectedCategory == PrayerGuidanceCategory.OBLIGATORY,
                                onClick = { viewModel.selectCategory(PrayerGuidanceCategory.OBLIGATORY) },
                                label = { Text(if (isId) "Sholat Fardhu" else stringResource(R.string.category_fardhu)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = state.selectedCategory == PrayerGuidanceCategory.SUNNAH,
                                onClick = { viewModel.selectCategory(PrayerGuidanceCategory.SUNNAH) },
                                label = { Text(if (isId) "Sholat Sunnah" else stringResource(R.string.category_sunnah)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = state.selectedCategory == PrayerGuidanceCategory.PURIFICATION,
                                onClick = { viewModel.selectCategory(PrayerGuidanceCategory.PURIFICATION) },
                                label = { Text(if (isId) "Wudhu & Syarat" else stringResource(R.string.category_purification)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = state.selectedCategory == PrayerGuidanceCategory.POST_PRAYER,
                                onClick = { viewModel.selectCategory(PrayerGuidanceCategory.POST_PRAYER) },
                                label = { Text(if (isId) "Dzikir & Doa" else stringResource(R.string.category_post_prayer)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // 3. List of Guides
                        if (filteredItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isId) "Tidak ada panduan yang sesuai" else stringResource(R.string.no_guides_found),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category & Rakaat Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val (badgeText, badgeColor, badgeTextColor) = when (item.category) {
                        PrayerGuidanceCategory.OBLIGATORY -> Triple(
                            if (isId) "Fardhu" else "Obligatory",
                            Color(0xFF00796B).copy(alpha = 0.15f),
                            Color(0xFF00796B)
                        )
                        PrayerGuidanceCategory.SUNNAH -> Triple(
                            "Sunnah",
                            Color(0xFFD97706).copy(alpha = 0.15f),
                            Color(0xFFD97706)
                        )
                        PrayerGuidanceCategory.PURIFICATION -> Triple(
                            if (isId) "Thaharah" else "Purification",
                            Color(0xFF2563EB).copy(alpha = 0.15f),
                            Color(0xFF2563EB)
                        )
                        PrayerGuidanceCategory.POST_PRAYER -> Triple(
                            if (isId) "Dzikir & Doa" else "Dhikr & Dua",
                            Color(0xFF7C3AED).copy(alpha = 0.15f),
                            Color(0xFF7C3AED)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
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

                    if (item.rakaat != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${item.rakaat} Raka'at",
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
                    color = Color(0xFF00796B)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Primary title
            Text(
                text = if (isId) item.nameId else item.nameEn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            // Description
            Text(
                text = if (isId) item.descriptionId else item.descriptionEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            // Footer info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isId) "${item.steps.size} Langkah panduan" else "${item.steps.size} Steps guide",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isId) "Lihat Selengkapnya →" else "View Details →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
