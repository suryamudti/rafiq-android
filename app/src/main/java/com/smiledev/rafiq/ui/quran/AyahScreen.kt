package com.smiledev.rafiq.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.data.models.AyahData

private val arabicFont = FontFamily(Font(R.font.me_quran))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahScreen(
    suraNumber: Int,
    suraName: String,
    onBack: () -> Unit,
    viewModel: QuranViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(suraNumber) {
        viewModel.loadAyahs(suraNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$suraNumber. $suraName") },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = "Error: ${state.error}",
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    LazyColumn(modifier = modifier.fillMaxSize()) {
                        state.currentSurah?.let { surah ->
                            item {
                                Text(
                                    text = surah.nameArabic,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontFamily = arabicFont,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                                HorizontalDivider()
                            }
                        }
                        itemsIndexed(state.ayahs) { index, ayah ->
                            VerseCell(ayah = ayah)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseCell(ayah: AyahData) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (ayah.isFirstAyaOfJuz || ayah.isFirstAyaOfPage) {
            BadgesRow(ayah)
            Spacer(Modifier.height(8.dp))
        }

        if (ayah.bismillah != null) {
            Text(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                fontFamily = arabicFont,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            )
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (ayah.sajda) {
                Text(
                    text = "\u06E9",
                    fontSize = 24.sp,
                    color = if (ayah.sajdaType == "obligatory") Color.Red else Color(0xFFFF9800)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${ayah.aya}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp).padding(top = 4.dp)
            )
            Text(
                text = ayah.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 22.sp,
                    fontFamily = arabicFont,
                    textDirection = TextDirection.Rtl,
                    lineHeight = 44.sp
                ),
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }

        if (!ayah.translation.isNullOrBlank()) {
            Text(
                text = "${ayah.aya}. ${ayah.translation}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Color.Gray.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun BadgesRow(ayah: AyahData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        if (ayah.isFirstAyaOfJuz) {
            BadgeChip(
                label = "Juz ${ayah.juz}",
                bgColor = Color(0xFF009688).copy(alpha = 0.1f),
                textColor = Color(0xFF009688)
            )
            if (ayah.isFirstAyaOfPage) {
                Spacer(Modifier.width(12.dp))
            }
        }
        if (ayah.isFirstAyaOfPage) {
            BadgeChip(
                label = "Page ${ayah.page}",
                bgColor = Color(0xFF607D8B).copy(alpha = 0.1f),
                textColor = Color(0xFF607D8B)
            )
        }
    }
}

@Composable
private fun BadgeChip(label: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .border(1.dp, textColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
