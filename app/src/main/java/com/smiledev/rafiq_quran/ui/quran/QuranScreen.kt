package com.smiledev.rafiq_quran.ui.quran

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.theme.ArabicFontFamily
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.bookmarks.BookmarkListTabContent
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqNumberBadge
import com.smiledev.rafiq_quran.ui.designsystem.input.RafiqSearchBar
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqEmptyState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqErrorState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    initialTab: Int = 0,
    onSurahClick: (Int, String) -> Unit,
    onBookmarkClick: (Int, String, Int) -> Unit,
    onSearchResultClick: (Int, String, Int) -> Unit,
    onBack: () -> Unit,
    viewModel: QuranViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val mainTabs = listOf(stringResource(R.string.surahs), stringResource(R.string.bookmarks))
    var selectedTabIndex by remember(initialTab) { mutableStateOf(initialTab) }
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RafiqTopAppBar(
                    title = stringResource(R.string.quran),
                    onBack = {
                        if (showSearch) {
                            showSearch = false
                            viewModel.clearSearch()
                        } else {
                            onBack()
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showSearch = !showSearch
                            if (!showSearch) {
                                viewModel.clearSearch()
                            }
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                )
                if (showSearch) {
                    RafiqSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.search(it) },
                        placeholder = stringResource(R.string.search_quran_hint),
                        modifier = Modifier.padding(
                            horizontal = RafiqTheme.spacing.l,
                            vertical = RafiqTheme.spacing.s
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showSearch) {
                // Scope tabs: Surahs vs Verses
                TabRow(selectedTabIndex = state.searchTab.ordinal) {
                    Tab(
                        selected = state.searchTab == QuranSearchTab.SURAHS,
                        onClick = { viewModel.setSearchTab(QuranSearchTab.SURAHS) },
                        text = {
                            Text(
                                "${stringResource(R.string.search_mode_surahs)} (${state.filteredSurahs.size})"
                            )
                        }
                    )
                    Tab(
                        selected = state.searchTab == QuranSearchTab.AYAHS,
                        onClick = { viewModel.setSearchTab(QuranSearchTab.AYAHS) },
                        text = {
                            val countSuffix = if (!state.searchLoading && state.searchQuery.isNotBlank()) {
                                " (${state.searchResults.size})"
                            } else ""
                            Text("${stringResource(R.string.search_mode_ayahs)}$countSuffix")
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    if (state.searchTab == QuranSearchTab.SURAHS) {
                        SurahListTabContent(
                            state = state,
                            onSurahClick = onSurahClick,
                            onContinueReadingClick = { sura, name, aya ->
                                onSearchResultClick(sura, name, aya)
                            },
                            onFilterChange = { viewModel.setRevelationFilter(it) },
                            onRefresh = { viewModel.refresh() },
                            showHeroBanner = false
                        )
                    } else {
                        SearchResultsContent(
                            state = state,
                            query = state.searchQuery,
                            onResultClick = onSearchResultClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    mainTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> {
                            SurahListTabContent(
                                state = state,
                                onSurahClick = onSurahClick,
                                onContinueReadingClick = { sura, name, aya ->
                                    onSearchResultClick(sura, name, aya)
                                },
                                onFilterChange = { viewModel.setRevelationFilter(it) },
                                onRefresh = { viewModel.refresh() },
                                showHeroBanner = true
                            )
                        }
                        else -> {
                            BookmarkListTabContent(
                                onBookmarkClick = onBookmarkClick,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahListTabContent(
    state: QuranUiState,
    onSurahClick: (Int, String) -> Unit,
    onContinueReadingClick: (Int, String, Int) -> Unit,
    onFilterChange: (RevelationFilter) -> Unit,
    onRefresh: () -> Unit,
    showHeroBanner: Boolean,
    modifier: Modifier = Modifier
) {
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; onRefresh() },
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && !isRefreshing -> {
                RafiqLoadingIndicator()
            }
            state.error != null -> {
                RafiqErrorState(
                    error = state.error!!,
                    onRetry = onRefresh
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = RafiqTheme.spacing.xl)
                ) {
                    // Last Read Hero Banner
                    if (showHeroBanner && state.lastReadSura > 0 && state.lastReadSurah != null) {
                        item(key = "hero_last_read") {
                            val surah = state.lastReadSurah!!
                            LastReadHeroBanner(
                                surah = surah,
                                lastReadAya = state.lastReadAya,
                                onClick = {
                                    val targetAya = if (state.lastReadAya > 0) state.lastReadAya else 1
                                    onContinueReadingClick(surah.chapterNumber, surah.nameSimple, targetAya)
                                }
                            )
                        }
                    }

                    // Revelation Filters Row
                    item(key = "revelation_filter_chips") {
                        RevelationFilterRow(
                            selectedFilter = state.revelationFilter,
                            totalCount = state.surahs.size,
                            meccanCount = state.meccanCount,
                            medinanCount = state.medinanCount,
                            onFilterSelect = onFilterChange
                        )
                    }

                    // Filtered Surahs List
                    if (state.filteredSurahs.isEmpty()) {
                        item(key = "empty_surahs") {
                            RafiqEmptyState(
                                title = stringResource(R.string.no_surahs_found),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = RafiqTheme.spacing.xxl)
                            )
                        }
                    } else {
                        items(state.filteredSurahs, key = { it.chapterNumber }) { surah ->
                            SurahCard(
                                surah = surah,
                                isLastRead = surah.chapterNumber == state.lastReadSura,
                                onClick = { onSurahClick(surah.chapterNumber, surah.nameSimple) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LastReadHeroBanner(
    surah: Surah,
    lastReadAya: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = RafiqTheme.spacing.l,
                vertical = RafiqTheme.spacing.s
            )
            .clickable(onClick = onClick),
        shape = RafiqTheme.customShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RafiqTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_quran),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(RafiqTheme.spacing.m))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.last_read).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = surah.nameSimple,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val posText = if (lastReadAya > 0) {
                    stringResource(R.string.continue_from_ayah, lastReadAya)
                } else {
                    surah.translatedName
                }
                Text(
                    text = posText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(RafiqTheme.spacing.s))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = surah.nameArabic,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = ArabicFontFamily,
                        textDirection = TextDirection.Rtl
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RafiqTheme.customShapes.pill,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = RafiqTheme.spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.continue_reading),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RevelationFilterRow(
    selectedFilter: RevelationFilter,
    totalCount: Int,
    meccanCount: Int,
    medinanCount: Int,
    onFilterSelect: (RevelationFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = RafiqTheme.spacing.l,
                vertical = RafiqTheme.spacing.xs
            ),
        horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)
    ) {
        FilterChip(
            selected = selectedFilter == RevelationFilter.ALL,
            onClick = { onFilterSelect(RevelationFilter.ALL) },
            label = { Text("${stringResource(R.string.revelation_all)} ($totalCount)") },
            shape = RafiqTheme.customShapes.pill,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        FilterChip(
            selected = selectedFilter == RevelationFilter.MECCAN,
            onClick = { onFilterSelect(RevelationFilter.MECCAN) },
            label = { Text("${stringResource(R.string.revelation_meccan)} ($meccanCount)") },
            shape = RafiqTheme.customShapes.pill,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
        FilterChip(
            selected = selectedFilter == RevelationFilter.MEDINAN,
            onClick = { onFilterSelect(RevelationFilter.MEDINAN) },
            label = { Text("${stringResource(R.string.revelation_medinan)} ($medinanCount)") },
            shape = RafiqTheme.customShapes.pill,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        )
    }
}

@Composable
private fun SurahCard(
    surah: Surah,
    isLastRead: Boolean,
    onClick: () -> Unit
) {
    val isMeccan = surah.revelationPlace.equals("makkah", ignoreCase = true) ||
        surah.revelationPlace.equals("meccan", ignoreCase = true)
    val revelationText = if (isMeccan) {
        stringResource(R.string.revelation_meccan)
    } else {
        stringResource(R.string.revelation_medinan)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = RafiqTheme.spacing.l,
                vertical = RafiqTheme.spacing.xs
            )
            .clickable(onClick = onClick),
        shape = RafiqTheme.customShapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLastRead) RafiqTheme.elevation.default else RafiqTheme.elevation.low
        ),
        border = if (isLastRead) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null,
        colors = if (isLastRead) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            )
        } else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RafiqTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RafiqNumberBadge(
                number = surah.chapterNumber,
                isHighlighted = isLastRead
            )

            Spacer(modifier = Modifier.width(RafiqTheme.spacing.m))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.xs)
                ) {
                    Text(
                        text = surah.nameSimple,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isLastRead) {
                        Surface(
                            shape = RafiqTheme.customShapes.pill,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(R.string.last_read_surah_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = surah.translatedName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RafiqTheme.customShapes.small,
                        color = if (isMeccan) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        }
                    ) {
                        Text(
                            text = revelationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMeccan) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )

                    Text(
                        text = stringResource(R.string.verses_count, surah.versesCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(RafiqTheme.spacing.m))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = surah.nameArabic,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = ArabicFontFamily,
                        textDirection = TextDirection.Rtl
                    ),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    state: QuranUiState,
    query: String,
    onResultClick: (Int, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.searchError != null -> {
            RafiqErrorState(
                message = state.searchError?.displayMessage ?: "",
                modifier = modifier
            )
        }
        state.searchLoading && state.searchResults.isEmpty() -> {
            RafiqLoadingIndicator(modifier = modifier)
        }
        state.searchResults.isEmpty() -> {
            RafiqEmptyState(
                title = stringResource(R.string.no_ayahs_match),
                modifier = modifier
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(vertical = RafiqTheme.spacing.s)
            ) {
                items(state.searchResults, key = { "${it.sura}:${it.aya}" }) { ayah ->
                    val surahName = state.surahs.find { it.chapterNumber == ayah.sura }?.nameSimple
                        ?: "Surah ${ayah.sura}"
                    QuranSearchResultCard(
                        ayah = ayah,
                        surahName = surahName,
                        query = query.trim(),
                        onClick = { onResultClick(ayah.sura, surahName, ayah.aya) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuranSearchResultCard(
    ayah: Ayah,
    surahName: String,
    query: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.xs)
            .clickable(onClick = onClick),
        shape = RafiqTheme.customShapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = RafiqTheme.elevation.default)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(RafiqTheme.spacing.l)) {
            Text(
                text = "$surahName · ${ayah.sura}:${ayah.aya}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = highlightMatches(ayah.text, query),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ArabicFontFamily,
                    fontSize = 18.sp,
                    textDirection = TextDirection.Rtl
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = RafiqTheme.spacing.xs)
            )
            val translation = ayah.translation
            if (!translation.isNullOrBlank()) {
                Text(
                    text = highlightMatches(translation, query),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = RafiqTheme.spacing.xs)
                )
            }
        }
    }
}

@Composable
private fun highlightMatches(text: String, query: String): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        val q = query.trim()
        if (q.isEmpty()) return@buildAnnotatedString
        val style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        var index = text.indexOf(q, ignoreCase = true)
        while (index >= 0) {
            addStyle(style, index, index + q.length)
            index = text.indexOf(q, index + q.length, ignoreCase = true)
        }
    }
}
