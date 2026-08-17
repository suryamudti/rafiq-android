package com.smiledev.rafiq.ui.recitation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.displayMessage
import androidx.compose.ui.res.stringResource
import com.smiledev.rafiq.domain.model.Surah

private val arabicFont = FontFamily(Font(R.font.me_quran))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitationScreen(
    onBack: () -> Unit,
    viewModel: RecitationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.selectedReciter != null) stringResource(R.string.select_surah_recitation) else stringResource(R.string.audio_recitations)
                    )
                },
                navigationIcon = {
                    Text(
                        stringResource(R.string.back),
                        modifier = Modifier.clickable(
                            onClick = {
                                if (state.selectedReciter != null) viewModel.backToReciters() else onBack
                            }
                        ).padding(16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.fillMaxSize().padding(padding).semantics { contentDescription = "Loading" })
            }
            state.error != null -> {
                Text(
                    text = state.error?.displayMessage ?: "",
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            state.selectedReciter != null -> SurahListContent(state, viewModel, modifier, padding)
            else -> ReciterListContent(state, viewModel, modifier, padding)
        }
    }
}

@Composable
private fun ReciterListContent(
    state: RecitationUiState,
    viewModel: RecitationViewModel,
    modifier: Modifier,
    padding: androidx.compose.foundation.layout.PaddingValues
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        item {
            Text(
                text = stringResource(R.string.select_reciter),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(state.reciters) { reciter ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { viewModel.selectReciter(reciter) },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reciter.nameAr,
                            fontFamily = arabicFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = reciter.nameEn,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${reciter.style} | ${reciter.country}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahListContent(
    state: RecitationUiState,
    viewModel: RecitationViewModel,
    modifier: Modifier,
    padding: androidx.compose.foundation.layout.PaddingValues
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        state.currentSurah?.let { surah ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${surah.chapterNumber}. ${surah.nameSimple}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = surah.nameArabic,
                                fontFamily = arabicFont,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(onClick = { viewModel.togglePlayback() }) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = if (state.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.select_surah),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(state.surahs) { surah ->
            val isCurrent = state.currentSurah?.chapterNumber == surah.chapterNumber
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { viewModel.playSurah(surah) },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(if (isCurrent) 4.dp else 1.dp),
                colors = if (isCurrent) CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) else CardDefaults.cardColors()
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
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = surah.nameSimple,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = surah.translatedName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = surah.nameArabic,
                        fontFamily = arabicFont,
                        fontSize = 16.sp,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
