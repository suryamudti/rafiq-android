package com.smiledev.rafiq.ui.prophets

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.domain.model.ProphetStory

private val arabicFont = FontFamily(Font(R.font.me_quran))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProphetDetailScreen(
    prophetId: Int,
    onBack: () -> Unit,
    onVerseRefClick: (surah: Int, surahName: String, ayaStart: Int) -> Unit,
    onProphetNavigate: (Int) -> Unit,
    viewModel: ProphetsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val prophet = state.prophets.find { it.id == prophetId }
    val localeCode = viewModel.localeCode
    val context = LocalContext.current
    val index = state.prophets.indexOfFirst { it.id == prophetId }
    var showFontSizeSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (localeCode == "id") prophet?.nameId ?: "Prophet"
                        else prophet?.nameEn ?: "Prophet"
                    )
                },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                actions = {
                    Text(
                        text = "Aa",
                        modifier = Modifier
                            .clickable { showFontSizeSheet = true }
                            .padding(horizontal = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = if (prophetId in state.favoriteIds) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .clickable { viewModel.toggleFavorite(prophetId) }
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.prophets_share),
                        modifier = Modifier
                            .clickable { prophet?.let { shareProphetStory(context, it, localeCode) } }
                            .padding(horizontal = 8.dp),
                        fontSize = 14.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (prophet == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading" })
                } else {
                    Text(stringResource(R.string.prophet_not_found))
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = prophet.nameArabic,
                            fontFamily = arabicFont,
                            fontSize = 48.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (localeCode == "id") prophet.nameId else prophet.nameEn,
                            fontSize = 22.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                val summary = if (localeCode == "id") prophet.summaryId else prophet.summaryEn
                if (summary.isNotBlank()) {
                    SectionCard(title = stringResource(R.string.prophets_summary), content = summary)
                    Spacer(Modifier.height(16.dp))
                }

                val story = if (localeCode == "id") prophet.storyId else prophet.storyEn
                if (story.isNotBlank()) {
                    SectionCard(title = stringResource(R.string.prophets_story), content = story, contentFontSize = state.storyFontSize.sp)
                    Spacer(Modifier.height(16.dp))
                }

                val miracles = if (localeCode == "id") prophet.miraclesId else prophet.miraclesEn
                if (miracles.isNotBlank()) {
                    SectionCard(title = stringResource(R.string.prophets_miracles), content = miracles)
                    Spacer(Modifier.height(16.dp))
                }

                val facts = listOf(
                    stringResource(R.string.prophets_era) to (if (localeCode == "id") prophet.eraId else prophet.eraEn),
                    stringResource(R.string.prophets_people) to (if (localeCode == "id") prophet.peopleId else prophet.peopleEn),
                    stringResource(R.string.prophets_lifespan) to (if (localeCode == "id") prophet.lifespanId else prophet.lifespanEn)
                ).filter { it.second.isNotBlank() }

                if (facts.isNotEmpty()) {
                    SectionCard(
                        title = stringResource(R.string.prophets_facts),
                        content = facts.joinToString("\n") { (label, value) -> "$label: $value" }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                val events = if (localeCode == "id") prophet.eventsId else prophet.eventsEn
                if (events.isNotEmpty()) {
                    SectionCard(
                        title = stringResource(R.string.prophets_key_events),
                        content = events.mapIndexed { i, e -> "${i + 1}. $e" }.joinToString("\n")
                    )
                    Spacer(Modifier.height(16.dp))
                }

                val lessons = if (localeCode == "id") prophet.lessonsId else prophet.lessonsEn
                if (lessons.isNotEmpty()) {
                    SectionCard(
                        title = stringResource(R.string.prophets_lessons),
                        content = lessons.joinToString("\n") { "• $it" }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (prophet.verses.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.prophets_verse_references),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            prophet.verses.forEach { ref ->
                                val refName = if (localeCode == "id") ref.surahNameId else ref.surahNameEn
                                val label = stringResource(R.string.verse_ref, refName, ref.surah, ref.ayahStart)
                                Text(
                                    text = label,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onVerseRefClick(ref.surah, refName, ref.ayahStart) }
                                        .padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (state.prophets.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (index > 0) {
                            Text(
                                text = stringResource(R.string.prophets_previous),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onProphetNavigate(state.prophets[index - 1].id) }
                                    .padding(8.dp)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        if (index in 0 until state.prophets.lastIndex) {
                            Text(
                                text = stringResource(R.string.prophets_next),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onProphetNavigate(state.prophets[index + 1].id) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFontSizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFontSizeSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.font_size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = stringResource(R.string.prophets_story_font_size, state.storyFontSize),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("12", fontSize = 12.sp, fontWeight = FontWeight.Light)
                    Slider(
                        value = state.storyFontSize.toFloat(),
                        onValueChange = { viewModel.setStoryFontSize(it.toInt()) },
                        valueRange = 12f..30f,
                        steps = 17,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("30", fontSize = 12.sp, fontWeight = FontWeight.Light)
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { showFontSizeSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: String, contentFontSize: androidx.compose.ui.unit.TextUnit = 16.sp) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = contentFontSize
            )
        }
    }
}

private fun shareProphetStory(context: Context, prophet: ProphetStory, localeCode: String) {
    val name = if (localeCode == "id") prophet.nameId else prophet.nameEn
    val story = if (localeCode == "id") prophet.storyId else prophet.storyEn
    val text = buildString {
        appendLine(name)
        appendLine(prophet.nameArabic)
        appendLine()
        append(story)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Story"))
}