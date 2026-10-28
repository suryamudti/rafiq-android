package com.smiledev.rafiq_quran.ui.prophets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun ProphetsScreen(
    onProphetClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: ProphetsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val filtered = viewModel.filterProphets(state)

    Scaffold(
        modifier = modifier,
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.prophets_title),
                onBack = onBack
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.prophets.isEmpty() -> {
                    RafiqLoadingIndicator(modifier = Modifier.fillMaxSize())
                }
                state.error != null && state.prophets.isEmpty() -> {
                    RafiqErrorState(
                        error = state.error!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar
                        RafiqSearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.search(it) },
                            placeholder = stringResource(R.string.search_prophets),
                            modifier = Modifier.padding(
                                horizontal = RafiqTheme.spacing.l,
                                vertical = RafiqTheme.spacing.xs
                            )
                        )

                        // Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = RafiqTheme.spacing.l,
                                    vertical = RafiqTheme.spacing.xs
                                ),
                            horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RafiqFilterChip(
                                selected = state.showFavoritesOnly,
                                onClick = { viewModel.setShowFavoritesOnly(!state.showFavoritesOnly) },
                                label = stringResource(R.string.prophets_favorites),
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

                        if (filtered.isEmpty()) {
                            val emptyTitle = if (state.showFavoritesOnly) {
                                stringResource(R.string.no_favorite_prophets)
                            } else {
                                stringResource(R.string.no_prophets_match, state.searchQuery)
                            }
                            RafiqEmptyState(
                                title = emptyTitle,
                                fullScreen = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(
                                    horizontal = RafiqTheme.spacing.m,
                                    vertical = RafiqTheme.spacing.s
                                ),
                                horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s),
                                verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)
                            ) {
                                items(filtered, key = { it.id }) { prophet ->
                                    val localizedName = if (viewModel.localeCode == "id") {
                                        prophet.nameId
                                    } else {
                                        prophet.nameEn
                                    }

                                    RafiqCard(
                                        onClick = { onProphetClick(prophet.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RafiqTheme.customShapes.large,
                                        border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
                                        contentPadding = RafiqTheme.spacing.l
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RafiqTheme.customShapes.pill)
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "#${prophet.id}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Spacer(Modifier.height(RafiqTheme.spacing.xs))
                                            Text(
                                                text = prophet.nameArabic,
                                                fontFamily = arabicFont,
                                                fontSize = 24.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(Modifier.height(RafiqTheme.spacing.xxs))
                                            Text(
                                                text = localizedName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
}
