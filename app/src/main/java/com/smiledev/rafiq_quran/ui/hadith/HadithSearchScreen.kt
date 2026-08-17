package com.smiledev.rafiq.ui.hadith

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.displayMessage
import com.smiledev.rafiq.domain.model.Hadith
import com.smiledev.rafiq.domain.model.HadithBook

private val arabicFont = FontFamily(Font(R.font.me_quran))

private fun collectionName(collection: String): String =
    if (collection == "bukhari") "al-Bukhari" else "Muslim"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithSearchScreen(
    onHadithClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: HadithSearchViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_hadiths)) },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = state.query,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text(stringResource(R.string.search_hadiths)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            when {
                state.error != null -> Text(
                    text = state.error?.displayMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
                state.query.isBlank() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_hadiths_hint),
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                state.isLoading && state.results.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                state.results.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_hadiths_match, state.query.trim()),
                        color = Color.Gray,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.results, key = { it.id }) { hadith ->
                        SearchResultCard(
                            hadith = hadith,
                            query = state.query.trim(),
                            book = state.books.find { it.id == hadith.bookId },
                            lang = viewModel.resolvedLanguage(),
                            onClick = { onHadithClick(hadith.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    hadith: Hadith,
    query: String,
    book: HadithBook?,
    lang: String,
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
                text = if (book != null) {
                    stringResource(
                        R.string.hadith_reference,
                        collectionName(book.collection),
                        book.number,
                        hadith.inBookNumber
                    )
                } else {
                    "Book ${hadith.bookId.substringAfterLast('.')} · Hadith ${hadith.inBookNumber}"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = highlightMatches(snippetFor(hadith, lang), query),
                fontFamily = if (snippetFor(hadith, lang) == hadith.textAr) arabicFont else FontFamily.Default,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun snippetFor(hadith: Hadith, lang: String): String = when {
    lang == "id" || lang == "both" -> hadith.textId.ifBlank { hadith.textEn }
    lang == "en" -> hadith.textEn.ifBlank { hadith.textId }
    else -> hadith.textAr
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