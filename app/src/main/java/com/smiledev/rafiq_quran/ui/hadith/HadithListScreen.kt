package com.smiledev.rafiq_quran.ui.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.domain.model.Hadith
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqErrorState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithListScreen(
    bookId: String,
    onHadithClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: HadithListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(bookId) { viewModel.load(bookId) }

    val title = state.book?.let {
        if (viewModel.resolvedLanguage() == "id") it.nameId else it.nameEn
    } ?: stringResource(R.string.hadiths)

    Scaffold(
        modifier = modifier,
        topBar = {
            RafiqTopAppBar(
                title = title,
                onBack = onBack
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.hadiths.isEmpty() -> {
                    RafiqLoadingIndicator(modifier = Modifier.fillMaxSize())
                }
                state.error != null && state.hadiths.isEmpty() -> {
                    RafiqErrorState(
                        error = state.error!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = RafiqTheme.spacing.l,
                            vertical = RafiqTheme.spacing.s
                        )
                    ) {
                        items(state.hadiths, key = { it.id }) { hadith ->
                            HadithCard(
                                hadith = hadith,
                                onClick = { onHadithClick(hadith.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = RafiqTheme.spacing.xs)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithCard(
    hadith: Hadith,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RafiqCard(
        onClick = onClick,
        modifier = modifier,
        shape = RafiqTheme.customShapes.medium,
        border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
        contentPadding = RafiqTheme.spacing.l
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .clip(RafiqTheme.customShapes.pill)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.hadith_book_hadith_ref,
                        hadith.bookId.substringAfterLast('.'),
                        hadith.inBookNumber
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(RafiqTheme.spacing.xs))
        val isId = currentLocaleCode() == "id"
        val previewText = if (isId) {
            hadith.textId?.takeIf { it.isNotBlank() } ?: hadith.narratorEn ?: hadith.textEn
        } else {
            hadith.narratorEn ?: hadith.textEn
        }
        Text(
            text = previewText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
