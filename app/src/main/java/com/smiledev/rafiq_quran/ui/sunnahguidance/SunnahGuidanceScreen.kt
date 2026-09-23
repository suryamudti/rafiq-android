package com.smiledev.rafiq_quran.ui.sunnahguidance

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
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.domain.model.SunnahCategory
import com.smiledev.rafiq_quran.domain.model.SunnahGuidanceItem

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
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.sunnah_guidance),
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
                        TextButton(onClick = { viewModel.loadSunnahGuidance() }) {
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
                                Text(stringResource(R.string.search_sunnah_guide))
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
                                label = { Text(stringResource(R.string.category_all)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = state.selectedCategory == SunnahCategory.PRAYER,
                                onClick = { viewModel.selectCategory(SunnahCategory.PRAYER) },
                                label = { Text(stringResource(R.string.category_sunnah_prayer)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = state.selectedCategory == SunnahCategory.DAILY_LIFESTYLE,
                                onClick = { viewModel.selectCategory(SunnahCategory.DAILY_LIFESTYLE) },
                                label = { Text(stringResource(R.string.category_daily_lifestyle)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = state.selectedCategory == SunnahCategory.FRIDAY,
                                onClick = { viewModel.selectCategory(SunnahCategory.FRIDAY) },
                                label = { Text(stringResource(R.string.category_friday)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = state.selectedCategory == SunnahCategory.FASTING,
                                onClick = { viewModel.selectCategory(SunnahCategory.FASTING) },
                                label = { Text(stringResource(R.string.category_fasting)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = state.selectedCategory == SunnahCategory.DHIKR_DUA,
                                onClick = { viewModel.selectCategory(SunnahCategory.DHIKR_DUA) },
                                label = { Text(stringResource(R.string.category_dhikr_dua)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // 3. List of Sunnah Guides
                        if (filteredItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_sunnah_found),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

                // Steps count badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
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

            Spacer(Modifier.height(10.dp))

            // Arabic Calligraphy Header
            Text(
                text = item.titleArabic,
                fontFamily = arabicFont,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                style = TextStyle(textDirection = TextDirection.Rtl),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            // Title
            Text(
                text = if (isId) item.titleId else item.titleEn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

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
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
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

            Spacer(Modifier.height(10.dp))

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
}
