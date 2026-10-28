package com.smiledev.rafiq_quran.ui.prophets

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.smiledev.rafiq_quran.domain.model.ProphetStory
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBackButton
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeroCard
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme
import com.smiledev.rafiq_quran.ui.theme.SecondaryGold
import com.smiledev.rafiq_quran.ui.theme.rafiqPressable

@Composable
fun SharedProphetsScreen(
    viewModel: SharedProphetsViewModel = remember { SharedProphetsViewModel() },
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // If a prophet is selected, display the detailed story view
    if (state.selectedProphet != null) {
        ProphetDetailView(
            prophet = state.selectedProphet!!,
            isFavorite = state.selectedProphet!!.id in state.favoriteIds,
            fontSize = state.storyFontSize,
            onBack = { viewModel.selectProphet(null) },
            onToggleFavorite = { viewModel.toggleFavorite(state.selectedProphet!!.id) },
            onFontSizeChange = { viewModel.setStoryFontSize(it) }
        )
    } else {
        ProphetsListView(
            state = state,
            viewModel = viewModel,
            onBackClick = onBackClick,
            modifier = modifier
        )
    }
}

@Composable
private fun ProphetsListView(
    state: SharedProphetsUiState,
    viewModel: SharedProphetsViewModel,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val filteredProphets = remember(state.prophets, state.searchQuery, state.showFavoritesOnly, state.favoriteIds) {
        viewModel.getFilteredProphets(state)
    }

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = "25 Prophets",
                subtitle = "Stories of the Messengers",
                navigationIcon = onBackClick?.let { action ->
                    { RafiqSharedBackButton(onClick = action) }
                },
                actions = {
                    RafiqSharedBadge(
                        text = "${filteredProphets.size} Prophets",
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f),
                        contentColor = MaterialTheme.colorScheme.primary
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
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Search field with continuous corners
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text(
                        "Search prophet name...",
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

            // Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.showFavoritesOnly,
                    onClick = { viewModel.toggleFavoritesOnly() },
                    label = { Text("Favorites (${state.favoriteIds.size})", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.rafiqPressable(targetScale = 0.94f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
            }

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                filteredProphets.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.showFavoritesOnly) "No favorite prophets found" else "No prophets match your query",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProphets, key = { it.id }) { prophet ->
                            val isFavorite = prophet.id in state.favoriteIds
                            ProphetCard(
                                prophet = prophet,
                                isFavorite = isFavorite,
                                onClick = { viewModel.selectProphet(prophet) },
                                onToggleFavorite = { viewModel.toggleFavorite(prophet.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProphetCard(
    prophet: ProphetStory,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    RafiqSharedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        contentPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${prophet.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prophet.nameEn,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = prophet.summaryEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = prophet.nameArabic,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .rafiqPressable(targetScale = 0.85f, onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isFavorite) "❤️" else "🤍", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ProphetDetailView(
    prophet: ProphetStory,
    isFavorite: Boolean,
    fontSize: Int,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onFontSizeChange: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = prophet.nameEn,
                subtitle = "Prophet #${prophet.id}",
                navigationIcon = {
                    RafiqSharedBackButton(onClick = onBack)
                },
                actions = {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.rafiqPressable(targetScale = 0.85f)
                    ) {
                        Text(if (isFavorite) "❤️" else "🤍", fontSize = 20.sp)
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Arabic Display with Gold Gradient
            RafiqSharedHeroCard(
                gradient = RafiqMultiplatformTheme.colors.heroGoldGradient,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = prophet.nameArabic,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = prophet.nameEn,
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = (-0.2).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (prophet.nameId.isNotEmpty()) {
                        Text(
                            text = "(${prophet.nameId})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Quick Facts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (prophet.eraEn.isNotEmpty()) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, RafiqMultiplatformTheme.colors.cardBorder),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Era",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(prophet.eraEn, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (prophet.peopleEn.isNotEmpty()) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, RafiqMultiplatformTheme.colors.cardBorder),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Sent To",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(prophet.peopleEn, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Font size adjuster
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Font size: ${fontSize}sp",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onFontSizeChange(fontSize - 2) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.rafiqPressable(targetScale = 0.92f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        Text("A-", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onFontSizeChange(fontSize + 2) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.rafiqPressable(targetScale = 0.92f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        Text("A+", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Story Narrative
            RafiqSharedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = 18.dp
            ) {
                Column {
                    Text(
                        text = "The Story",
                        style = MaterialTheme.typography.titleMedium.copy(
                            letterSpacing = (-0.2).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = prophet.storyEn,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.6).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Miracles Section
            if (prophet.miraclesEn.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.35f)),
                    colors = CardDefaults.cardColors(containerColor = SecondaryGold.copy(alpha = 0.10f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✨ Miracles Given by Allah",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = prophet.miraclesEn,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                        )
                    }
                }
            }

            // Key Lessons
            if (prophet.lessonsEn.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, RafiqMultiplatformTheme.colors.cardBorder),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 Lessons & Reflections",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        prophet.lessonsEn.forEach { lesson ->
                            Text(
                                text = "• $lesson",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Quran References
            if (prophet.verses.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📖 Quran References",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        prophet.verses.forEach { verse ->
                            Text(
                                text = "Surah ${verse.surahNameEn} (${verse.surah}): Ayat ${verse.ayahStart}-${verse.ayahEnd}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
