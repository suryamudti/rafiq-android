package com.smiledev.rafiq_quran.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
    val tabs = listOf(stringResource(R.string.surahs), stringResource(R.string.bookmarks))
    var selectedTabIndex by remember(initialTab) { mutableStateOf(initialTab) }
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                RafiqTopAppBar(
                    title = stringResource(R.string.quran),
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = { showSearch = !showSearch }) {
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
            val searching = showSearch && state.searchQuery.isNotBlank()
            if (!searching) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when {
                    searching -> SearchResultsContent(
                        state = state,
                        query = state.searchQuery,
                        onResultClick = onSearchResultClick,
                        modifier = Modifier.fillMaxSize()
                    )
                    selectedTabIndex == 0 -> {
                        var isRefreshing by remember { mutableStateOf(false) }
                        LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { isRefreshing = true; viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when {
                                state.isLoading && !isRefreshing -> {
                                    RafiqLoadingIndicator()
                                }
                                state.error != null -> {
                                    val err = state.error
                                    if (err != null) {
                                        RafiqErrorState(
                                            error = err,
                                            onRetry = { viewModel.refresh() }
                                        )
                                    }
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = modifier.fillMaxSize()
                                    ) {
                                        itemsIndexed(state.surahs) { _, surah ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        horizontal = RafiqTheme.spacing.l,
                                                        vertical = RafiqTheme.spacing.xs
                                                    )
                                                    .clickable { onSurahClick(surah.chapterNumber, surah.nameSimple) },
                                                shape = RafiqTheme.customShapes.medium,
                                                elevation = CardDefaults.cardElevation(defaultElevation = RafiqTheme.elevation.low)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(RafiqTheme.spacing.l),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RafiqNumberBadge(
                                                        number = surah.chapterNumber
                                                    )
                                                    Spacer(modifier = Modifier.width(RafiqTheme.spacing.m))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = surah.nameSimple,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = surah.translatedName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = surah.nameArabic,
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontFamily = ArabicFontFamily,
                                                                textDirection = TextDirection.Rtl
                                                            ),
                                                            textAlign = TextAlign.End
                                                        )
                                                        Text(
                                                            text = stringResource(R.string.verses_count, surah.versesCount),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    else -> {
                        BookmarkListTabContent(
                            onBookmarkClick = onBookmarkClick,
                            modifier = Modifier
                        )
                    }
                }
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
            LazyColumn(modifier = modifier) {
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
            .padding(horizontal = RafiqTheme.spacing.m, vertical = RafiqTheme.spacing.xs)
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
