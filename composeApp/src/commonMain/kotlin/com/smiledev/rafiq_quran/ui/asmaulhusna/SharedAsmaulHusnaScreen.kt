package com.smiledev.rafiq_quran.ui.asmaulhusna

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.domain.model.AsmaulHusna
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBackButton
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeroCard
import com.smiledev.rafiq_quran.ui.theme.RafiqMultiplatformTheme
import com.smiledev.rafiq_quran.ui.theme.SecondaryGold
import com.smiledev.rafiq_quran.ui.theme.rafiqPressable

@Composable
fun SharedAsmaulHusnaScreen(
    viewModel: SharedAsmaulHusnaViewModel = remember { SharedAsmaulHusnaViewModel() },
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val filteredNames = remember(state.names, state.searchQuery, state.showFavoritesOnly, state.favoriteIds) {
        viewModel.getFilteredNames(state)
    }

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = "Asmaul Husna",
                subtitle = "99 Names of Allah",
                navigationIcon = onBackClick?.let { action ->
                    { RafiqSharedBackButton(onClick = action) }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { viewModel.selectRandomName() },
                            modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                        ) {
                            Text(
                                text = "🎲 Random",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleGridView() },
                            modifier = Modifier.rafiqPressable(targetScale = 0.90f)
                        ) {
                            Text(
                                text = if (state.isGridView) "☰" else "⊞",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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

                RafiqSharedBadge(
                    text = "${filteredNames.size} Names",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                )
            }

            // Loading / Error / Content
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                filteredNames.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.showFavoritesOnly) "No favorite names found" else "No names match your query",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                state.isGridView -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredNames, key = { it.id }) { item ->
                            val isFavorite = item.id in state.favoriteIds
                            AsmaulHusnaGridCard(
                                item = item,
                                isFavorite = isFavorite,
                                onCardClick = { viewModel.selectName(item) },
                                onToggleFavorite = { viewModel.toggleFavorite(item.id) }
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredNames, key = { it.id }) { item ->
                            val isFavorite = item.id in state.favoriteIds
                            AsmaulHusnaListCard(
                                item = item,
                                isFavorite = isFavorite,
                                onCardClick = { viewModel.selectName(item) },
                                onToggleFavorite = { viewModel.toggleFavorite(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail & Dhikr Dialog
    state.selectedName?.let { item ->
        AsmaulHusnaDetailDialog(
            item = item,
            dhikrCount = state.dhikrCount,
            isFavorite = item.id in state.favoriteIds,
            onDismiss = { viewModel.selectName(null) },
            onIncrementDhikr = { viewModel.incrementDhikr() },
            onResetDhikr = { viewModel.resetDhikr() },
            onToggleFavorite = { viewModel.toggleFavorite(item.id) }
        )
    }
}

@Composable
private fun AsmaulHusnaGridCard(
    item: AsmaulHusna,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .rafiqPressable(targetScale = 0.96f, onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RafiqMultiplatformTheme.colors.cardBorder),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${item.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier.rafiqPressable(targetScale = 0.85f, onClick = onToggleFavorite)
                ) {
                    Text(
                        text = if (isFavorite) "❤️" else "🤍",
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.arabic,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.transliteration,
                style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = (-0.1).sp),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.meaningEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AsmaulHusnaListCard(
    item: AsmaulHusna,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    RafiqSharedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onCardClick,
        shape = RoundedCornerShape(16.dp),
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.id}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.transliteration,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.1).sp
                    )
                )
                Text(
                    text = "${item.meaningEn} • ${item.meaningId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.arabic,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))

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

@Composable
private fun AsmaulHusnaDetailDialog(
    item: AsmaulHusna,
    dhikrCount: Int,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onIncrementDhikr: () -> Unit,
    onResetDhikr: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "#${item.id} ${item.transliteration}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.rafiqPressable(targetScale = 0.85f)
                ) {
                    Text(if (isFavorite) "❤️" else "🤍", fontSize = 20.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Card for Arabic Calligraphy
                RafiqSharedHeroCard(
                    gradient = RafiqMultiplatformTheme.colors.heroEmeraldGradient,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = item.arabic,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 42.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, RafiqMultiplatformTheme.colors.cardBorder),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Meanings",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🇬🇧 ${item.meaningEn}", style = MaterialTheme.typography.bodyMedium)
                        Text("🇮🇩 ${item.meaningId}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (item.benefitEn.isNotEmpty() || item.benefitId.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.35f)),
                        colors = CardDefaults.cardColors(containerColor = SecondaryGold.copy(alpha = 0.10f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Benefits & Reflections",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (item.benefitEn.isNotEmpty()) {
                                Text("• ${item.benefitEn}", style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp))
                            }
                            if (item.benefitId.isNotEmpty()) {
                                Text("• ${item.benefitId}", style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Dedicated Dhikr counter for this name
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dhikr Counter", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$dhikrCount",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            OutlinedButton(
                                onClick = onResetDhikr,
                                enabled = dhikrCount > 0,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.rafiqPressable(targetScale = 0.94f),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Reset")
                            }
                            Button(
                                onClick = onIncrementDhikr,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.rafiqPressable(targetScale = 0.94f),
                                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("+ Count", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
