package com.smiledev.rafiq.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R

@Composable
fun BookmarkListTabContent(
    onBookmarkClick: (suraNumber: Int, suraName: String, ayaNumber: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarkListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) isRefreshing = false }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && !isRefreshing -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).semantics { contentDescription = "Loading" })
            }
            state.bookmarks.isEmpty() -> {
                Text(
                    text = stringResource(R.string.no_bookmarks_yet),
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    color = Color.Gray
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.bookmarks, key = { it.id }) { bookmark ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable { onBookmarkClick(bookmark.sura, bookmark.suraName, bookmark.aya) },
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${bookmark.suraName} ${bookmark.sura}:${bookmark.aya}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = bookmark.insertTime,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(onClick = { viewModel.delete(bookmark.sura, bookmark.aya) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
