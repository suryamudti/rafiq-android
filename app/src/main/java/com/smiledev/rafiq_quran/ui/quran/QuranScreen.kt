package com.smiledev.rafiq_quran.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.ui.bookmarks.BookmarkListTabContent

private val arabicFont = FontFamily(Font(R.font.me_quran))

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
    val tabs = listOf("Surahs", stringResource(R.string.bookmarks))
    var selectedTabIndex by remember(initialTab) { mutableStateOf(initialTab) }
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.quran)) },
                    navigationIcon = {
                        Text(stringResource(R.string.back), modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                    },
                    actions = {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                if (showSearch) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.search(it) },
                        placeholder = { Text(stringResource(R.string.search_quran_hint)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { contentDescription = "Search field" },
                        singleLine = true
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
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).semantics { contentDescription = "Loading" })
                                }
                                state.error != null -> {
                                    Text(
                                        text = state.error?.displayMessage ?: "",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = modifier.fillMaxSize()
                                    ) {
                                        itemsIndexed(state.surahs) { index, surah ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                                    .clickable { onSurahClick(surah.chapterNumber, surah.nameSimple) },
                                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${surah.chapterNumber}.",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(end = 12.dp)
                                                    )
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
                                                            style = MaterialTheme.typography.titleMedium,
                                                            textAlign = TextAlign.End
                                                        )
                                                        Text(
                                                            text = "${surah.versesCount} verses",
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
            Text(
                text = state.searchError?.displayMessage ?: "",
                modifier = modifier.fillMaxSize().padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
        state.searchLoading && state.searchResults.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading" })
            }
        }
        state.searchResults.isEmpty() -> {
            Text(
                text = stringResource(R.string.no_ayahs_match),
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "$surahName · ${ayah.sura}:${ayah.aya}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = highlightMatches(ayah.text, query),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = arabicFont,
                    fontSize = 18.sp,
                    textDirection = TextDirection.Rtl
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            val translation = ayah.translation
            if (!translation.isNullOrBlank()) {
                Text(
                    text = highlightMatches(translation, query),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
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
