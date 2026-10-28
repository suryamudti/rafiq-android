package com.smiledev.rafiq_quran.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBackButton
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeroCard
import com.smiledev.rafiq_quran.ui.theme.PrimaryTeal
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme
import com.smiledev.rafiq_quran.ui.theme.SecondaryGold
import com.smiledev.rafiq_quran.ui.theme.rafiqPressable

@Composable
fun SharedQuranScreen(
    viewModel: SharedQuranViewModel = remember { SharedQuranViewModel() },
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    if (state.selectedSurah != null) {
        AyahReaderView(
            surah = state.selectedSurah!!,
            state = state,
            viewModel = viewModel,
            onBack = { viewModel.selectSurah(null) }
        )
    } else {
        SurahListView(
            state = state,
            viewModel = viewModel,
            onBackClick = onBackClick,
            modifier = modifier
        )
    }
}

@Composable
private fun SurahListView(
    state: SharedQuranUiState,
    viewModel: SharedQuranViewModel,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = "Al-Qur'an",
                subtitle = "Read, Listen, and Reflect",
                navigationIcon = onBackClick?.let { action ->
                    { RafiqSharedBackButton(onClick = action) }
                },
                actions = {
                    RafiqSharedBadge(
                        text = "${state.surahs.size} Surahs",
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f),
                        contentColor = MaterialTheme.colorScheme.primary
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Input with Continuous Corners
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text(
                        "Search by name, meaning, or number...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)
                        )
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChange("") },
                            modifier = Modifier.size(28.dp).rafiqPressable(targetScale = 0.90f)
                        ) {
                            Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            )

            // Revelation Filter Row with tactile bounce
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.revelationFilter == null,
                    onClick = { viewModel.selectRevelationFilter(null) },
                    label = { Text("All", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.rafiqPressable(targetScale = 0.94f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                FilterChip(
                    selected = state.revelationFilter == "makkah",
                    onClick = { viewModel.selectRevelationFilter("makkah") },
                    label = { Text("Makkiyyah", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.rafiqPressable(targetScale = 0.94f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                FilterChip(
                    selected = state.revelationFilter == "madinah",
                    onClick = { viewModel.selectRevelationFilter("madinah") },
                    label = { Text("Madaniyyah", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.rafiqPressable(targetScale = 0.94f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
            }

            // Surah Cards List
            val filteredSurahs = viewModel.getFilteredSurahs(state)
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSurahs, key = { it.id }) { surah ->
                    SurahCard(surah = surah, onClick = { viewModel.selectSurah(surah) })
                }
            }
        }
    }
}

@Composable
private fun SurahCard(surah: Surah, onClick: () -> Unit) {
    RafiqSharedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        contentPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter Number Hexagon / Circle Badge with specular border
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${surah.chapterNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Surah English & Revelation Place
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameSimple,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = surah.translatedName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${surah.versesCount} Ayahs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Arabic Name
            Text(
                text = surah.nameArabic,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AyahReaderView(
    surah: Surah,
    state: SharedQuranUiState,
    viewModel: SharedQuranViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = surah.nameSimple,
                subtitle = "${surah.revelationPlace.replaceFirstChar { it.uppercase() }} • ${surah.versesCount} Verses",
                navigationIcon = {
                    RafiqSharedBackButton(onClick = onBack)
                },
                actions = {
                    Text(
                        text = surah.nameArabic,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Reader Controls Bar (Font Sizing, Translation Toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.toggleTranslation() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                ) {
                    Text(
                        if (state.showTranslation) "Hide Translation" else "Show Translation",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .rafiqPressable(targetScale = 0.90f) { viewModel.decreaseArabicFontSize() }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
                            .border(BorderStroke(1.dp, RafiqMultiplatformTheme.colors.cardBorder), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${state.arabicFontSizeSp}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .rafiqPressable(targetScale = 0.90f) { viewModel.increaseArabicFontSize() }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
                            .border(BorderStroke(1.dp, RafiqMultiplatformTheme.colors.cardBorder), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A+", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Bismillah (except for Surah At-Tawbah 9)
                    if (surah.chapterNumber != 9 && surah.chapterNumber != 1) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    items(state.ayahs, key = { "${it.sura}:${it.aya}" }) { ayah ->
                        AyahCard(
                            ayah = ayah,
                            arabicFontSize = state.arabicFontSizeSp,
                            showTranslation = state.showTranslation
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahCard(
    ayah: Ayah,
    arabicFontSize: Int,
    showTranslation: Boolean
) {
    RafiqSharedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Verse Number Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${ayah.aya}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (ayah.juz > 0) {
                    RafiqSharedBadge(
                        text = "Juz ${ayah.juz}",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                    )
                }
            }

            // Arabic Text with enhanced typography
            Text(
                text = ayah.text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = arabicFontSize.sp,
                    lineHeight = (arabicFontSize * 1.8).sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            // Translation Text
            if (showTranslation) {
                val translationText = ayah.translationEn ?: ayah.translationId ?: ayah.translation
                if (translationText != null) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = translationText,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
