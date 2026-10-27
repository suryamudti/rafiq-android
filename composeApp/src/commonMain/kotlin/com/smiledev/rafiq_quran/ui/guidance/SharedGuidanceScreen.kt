package com.smiledev.rafiq_quran.ui.guidance

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceCategory
import com.smiledev.rafiq_quran.domain.model.PrayerGuidanceItem
import com.smiledev.rafiq_quran.domain.model.PrayerStep
import com.smiledev.rafiq_quran.domain.model.SunnahCategory
import com.smiledev.rafiq_quran.domain.model.SunnahGuidanceItem
import com.smiledev.rafiq_quran.domain.model.SunnahStep
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.theme.SecondaryGold

@Composable
fun SharedGuidanceScreen(
    viewModel: SharedGuidanceViewModel = remember { SharedGuidanceViewModel() },
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    when {
        state.selectedPrayerItem != null -> {
            PrayerDetailView(
                item = state.selectedPrayerItem!!,
                onBack = { viewModel.selectPrayerItem(null) }
            )
        }
        state.selectedSunnahItem != null -> {
            SunnahDetailView(
                item = state.selectedSunnahItem!!,
                onBack = { viewModel.selectSunnahItem(null) }
            )
        }
        else -> {
            GuidanceListView(
                state = state,
                viewModel = viewModel,
                onBackClick = onBackClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun GuidanceListView(
    state: SharedGuidanceUiState,
    viewModel: SharedGuidanceViewModel,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = "Islamic Guidance",
                subtitle = "Step-by-step Prayers & Sunnahs",
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
                        text = "${state.prayerItems.size + state.sunnahItems.size} Topics",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Secondary Tabs: Prayer vs Sunnah
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                GuidanceTab.values().forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search prayers or sunnahs...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Chips Row
                if (state.selectedTab == GuidanceTab.PRAYER) {
                    PrayerCategoriesRow(state = state, viewModel = viewModel)
                    val filteredPrayers = viewModel.getFilteredPrayerItems(state)
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredPrayers, key = { it.id }) { item ->
                            PrayerItemCard(item = item, onClick = { viewModel.selectPrayerItem(item) })
                        }
                    }
                } else {
                    SunnahCategoriesRow(state = state, viewModel = viewModel)
                    val filteredSunnahs = viewModel.getFilteredSunnahItems(state)
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredSunnahs, key = { it.id }) { item ->
                            SunnahItemCard(item = item, onClick = { viewModel.selectSunnahItem(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerCategoriesRow(state: SharedGuidanceUiState, viewModel: SharedGuidanceViewModel) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = state.prayerCategoryFilter == null,
            onClick = { viewModel.selectPrayerCategory(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        PrayerGuidanceCategory.values().forEach { cat ->
            val label = when (cat) {
                PrayerGuidanceCategory.OBLIGATORY -> "Obligatory"
                PrayerGuidanceCategory.SUNNAH -> "Sunnah"
                PrayerGuidanceCategory.PURIFICATION -> "Purification"
                PrayerGuidanceCategory.POST_PRAYER -> "Post-Prayer"
            }
            FilterChip(
                selected = state.prayerCategoryFilter == cat,
                onClick = { viewModel.selectPrayerCategory(cat) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun SunnahCategoriesRow(state: SharedGuidanceUiState, viewModel: SharedGuidanceViewModel) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = state.sunnahCategoryFilter == null,
            onClick = { viewModel.selectSunnahCategory(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        SunnahCategory.values().forEach { cat ->
            val label = when (cat) {
                SunnahCategory.PRAYER -> "Prayers"
                SunnahCategory.DAILY_LIFESTYLE -> "Lifestyle"
                SunnahCategory.FRIDAY -> "Friday"
                SunnahCategory.FASTING -> "Fasting"
                SunnahCategory.DHIKR_DUA -> "Dhikr & Dua"
            }
            FilterChip(
                selected = state.sunnahCategoryFilter == cat,
                onClick = { viewModel.selectSunnahCategory(cat) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun PrayerItemCard(item: PrayerGuidanceItem, onClick: () -> Unit) {
    RafiqSharedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.nameEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.rakaat != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        RafiqSharedBadge(
                            text = "${item.rakaat} Rakaat",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.descriptionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = item.nameArabic,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SunnahItemCard(item: SunnahGuidanceItem, onClick: () -> Unit) {
    RafiqSharedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.summaryEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = item.titleArabic,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PrayerDetailView(item: PrayerGuidanceItem, onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = item.nameEn,
                subtitle = item.category.name,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Title Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.nameArabic,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.nameEn, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (item.nameId.isNotEmpty()) {
                        Text(text = "(${item.nameId})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }

            // Niyyah (Intention) Section
            val niyyahAr = item.niyyahArabic
            if (niyyahAr != null) {
                RafiqSharedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Niyyah (Intention)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = niyyahAr,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp, lineHeight = 34.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (item.niyyahTransliteration != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = item.niyyahTransliteration!!, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (item.niyyahTranslationEn != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "\"${item.niyyahTranslationEn}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Step-by-Step Instructions
            if (item.steps.isNotEmpty()) {
                Text(
                    text = "Step-by-Step Guidance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                item.steps.forEach { step ->
                    StepCard(step = step)
                }
            }
        }
    }
}

@Composable
private fun StepCard(step: PrayerStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${step.order}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = step.titleEn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = step.descriptionEn, style = MaterialTheme.typography.bodySmall)

            val stepAr = step.arabic
            if (stepAr != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stepAr,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val stepTrans = step.transliteration
            if (stepTrans != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stepTrans, style = MaterialTheme.typography.bodySmall)
            }
            if (step.translationEn != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "\"${step.translationEn}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun SunnahDetailView(item: SunnahGuidanceItem, onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = item.titleEn,
                subtitle = item.category.name,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Title Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.titleArabic,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.titleEn, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (item.titleId.isNotEmpty()) {
                        Text(text = "(${item.titleId})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }

            // Description
            RafiqSharedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = item.descriptionEn, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Virtues
            if (item.virtuesEn.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondaryGold.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "✨ Virtues & Rewards", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SecondaryGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        item.virtuesEn.forEach { virtue ->
                            Text(text = "• $virtue", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
