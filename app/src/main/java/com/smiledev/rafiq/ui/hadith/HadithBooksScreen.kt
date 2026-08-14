package com.smiledev.rafiq.ui.hadith

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.displayMessage
import com.smiledev.rafiq.domain.model.HadithBook

private val arabicFont = FontFamily(Font(R.font.me_quran))

private fun collectionLabel(collection: String): String =
    if (collection == "bukhari") "Sahih al-Bukhari" else "Sahih Muslim"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithBooksScreen(
    onHadithBookClick: (String) -> Unit,
    onSearch: () -> Unit = {},
    onBack: () -> Unit,
    viewModel: HadithBooksViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hadiths)) },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_hadiths)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.error != null -> Text(
                    text = state.error?.displayMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
                state.isLoading && state.books.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                ) {
                    items(state.books) { book ->
                        val localizedName = if (viewModel.resolvedLanguage() == "id") book.nameId else book.nameEn
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .clickable { onHadithBookClick(book.id) },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = book.nameAr,
                                    fontFamily = arabicFont,
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = localizedName,
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${collectionLabel(book.collection)} · Book ${book.number}",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}