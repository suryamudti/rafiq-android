package com.smiledev.rafiq.ui.hadith

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.domain.model.Hadith

private val arabicFont = FontFamily(Font(R.font.me_quran))

private fun collectionName(collection: String): String =
    if (collection == "bukhari") "al-Bukhari" else "Muslim"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithDetailScreen(
    hadithId: Int,
    onBack: () -> Unit,
    viewModel: HadithListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val hadith = state.hadiths.find { it.id == hadithId }
    val resolvedLang = viewModel.resolvedLanguage()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hadiths)) },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (hadith == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                val collection = state.book?.collection ?: "bukhari"
                Text(
                    text = "Sahih ${collectionName(collection)} · Book ${hadith.bookId.substringAfterLast('.')}, Hadith ${hadith.inBookNumber}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                // Arabic matn
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(
                            text = hadith.textAr,
                            fontFamily = arabicFont,
                            fontSize = 28.sp,
                            lineHeight = 44.sp,
                            textAlign = TextAlign.Center
                        )
                        hadith.narratorAr?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = it,
                                fontFamily = arabicFont,
                                fontSize = 15.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                hadith.narratorEn?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                }

                when (resolvedLang) {
                    "id" -> TranslationSection("ID", hadith.translationText(preferId = true).orEmpty())
                    "en" -> TranslationSection("EN", hadith.translationText(preferId = false).orEmpty())
                    else -> {
                        TranslationSection("ID", hadith.translationText(preferId = true).orEmpty())
                        Spacer(Modifier.height(12.dp))
                        TranslationSection("EN", hadith.translationText(preferId = false).orEmpty())
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationSection(chip: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row {
                Text(
                    text = chip,
                    modifier = Modifier.padding(end = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (text.isBlank()) {
                Text(
                    text = "Translation unavailable",
                    fontSize = 15.sp,
                    color = Color.Gray
                )
            } else {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

private fun Hadith.translationText(preferId: Boolean): String? {
    val text = if (preferId) textId else textEn
    return text.takeIf { it.isNotBlank() }
        ?: (if (preferId) textEn else textId).takeIf { it.isNotBlank() }
}