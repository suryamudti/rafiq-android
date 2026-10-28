package com.smiledev.rafiq_quran.ui.hadith

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.domain.model.Hadith
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBackButton
import com.smiledev.rafiq_quran.ui.components.RafiqSharedBadge
import com.smiledev.rafiq_quran.ui.components.RafiqSharedCard
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeader
import com.smiledev.rafiq_quran.ui.components.RafiqSharedHeroCard
import com.smiledev.rafiq_quran.ui.theme.SecondaryGold
import com.smiledev.rafiq_quran.ui.theme.rafiqPressable

@Composable
fun SharedHadithScreen(
    viewModel: SharedHadithViewModel = remember { SharedHadithViewModel() },
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val filteredHadiths = remember(
        state.hadiths,
        state.selectedBook,
        state.selectedTopic,
        state.searchQuery,
        state.showFavoritesOnly,
        state.favoriteIds
    ) {
        viewModel.getFilteredHadiths(state)
    }

    Scaffold(
        topBar = {
            RafiqSharedHeader(
                title = "Hadith Library",
                subtitle = "Prophetic Traditions & Wisdom",
                navigationIcon = onBackClick?.let { action ->
                    { RafiqSharedBackButton(onClick = action) }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.toggleFavoritesOnly() },
                        modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                    ) {
                        Text(
                            text = if (state.showFavoritesOnly) "★ Saved (${state.favoriteIds.size})" else "☆ Saved",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.showFavoritesOnly) SecondaryGold else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hadith of the Day Hero Card
            item {
                RafiqSharedHeroCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RafiqSharedBadge(text = "Hadith of the Day", isGold = true)
                            Text(
                                text = state.hadithOfTheDay.bookId.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Text(
                            text = state.hadithOfTheDay.textAr,
                            style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 36.sp),
                            color = Color.White,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "\"${state.hadithOfTheDay.textEn}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 22.sp
                        )

                        state.hadithOfTheDay.narratorEn?.let { narrator ->
                            Text(
                                text = "Narrated by $narrator",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search hadiths, narrators, topics...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Canonical Books Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Canonical Collections",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        state.books.forEach { book ->
                            val isSelected = state.selectedBook?.id == book.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectBook(book) },
                                label = { Text(book.nameEn) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                            )
                        }
                    }
                }
            }

            // Topics Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Browse by Topic",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        state.topics.forEach { topic ->
                            val isSelected = state.selectedTopic?.id == topic.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectTopic(topic) },
                                label = { Text(topic.nameEn) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SecondaryGold,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Hadiths (${filteredHadiths.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Hadith Items
            items(filteredHadiths) { hadith ->
                val isFav = hadith.id in state.favoriteIds
                HadithListItem(
                    hadith = hadith,
                    isFavorite = isFav,
                    onToggleFavorite = { viewModel.toggleFavorite(hadith.id) },
                    onClick = { viewModel.selectHadith(hadith) }
                )
            }
        }
    }

    // Detail Modal Dialog
    state.selectedHadith?.let { hadith ->
        val isFav = hadith.id in state.favoriteIds
        AlertDialog(
            onDismissRequest = { viewModel.selectHadith(null) },
            confirmButton = {
                Button(
                    onClick = { viewModel.selectHadith(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                ) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.toggleFavorite(hadith.id) },
                    modifier = Modifier.rafiqPressable(targetScale = 0.94f)
                ) {
                    Text(if (isFav) "★ Saved" else "☆ Save to Favorites")
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Hadith #${hadith.inBookNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    RafiqSharedBadge(text = hadith.bookId.uppercase())
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Font Size Adjuster
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Size: ",
                            style = MaterialTheme.typography.labelSmall
                        )
                        IconButton(
                            onClick = { viewModel.setFontSize(state.fontSize - 2) },
                            modifier = Modifier.size(28.dp).rafiqPressable(targetScale = 0.88f)
                        ) {
                            Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { viewModel.setFontSize(state.fontSize + 2) },
                            modifier = Modifier.size(28.dp).rafiqPressable(targetScale = 0.88f)
                        ) {
                            Text("A+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Arabic Text
                    Text(
                        text = hadith.textAr,
                        fontSize = (state.fontSize + 4).sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = (state.fontSize + 14).sp,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // English Translation
                    Text(
                        text = hadith.textEn,
                        fontSize = state.fontSize.sp,
                        lineHeight = (state.fontSize + 6).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Indonesian Translation
                    Text(
                        text = hadith.textId,
                        fontSize = (state.fontSize - 2).sp,
                        lineHeight = (state.fontSize + 4).sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    hadith.narratorEn?.let { narrator ->
                        Text(
                            text = "Narrator: $narrator",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryGold
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun HadithListItem(
    hadith: Hadith,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RafiqSharedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                RafiqSharedBadge(
                    text = "${hadith.bookId.uppercase()} • #${hadith.inBookNumber}",
                    isGold = isFavorite
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp).rafiqPressable(targetScale = 0.88f)
                ) {
                    Text(
                        text = if (isFavorite) "★" else "☆",
                        fontSize = 18.sp,
                        color = if (isFavorite) SecondaryGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = hadith.textAr,
                style = MaterialTheme.typography.titleMedium.copy(lineHeight = 28.sp),
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = hadith.textEn,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            hadith.narratorEn?.let { narrator ->
                Text(
                    text = "Narrated by $narrator",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
