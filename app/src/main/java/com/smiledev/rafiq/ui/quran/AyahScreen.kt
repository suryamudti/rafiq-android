package com.smiledev.rafiq.ui.quran

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.core.displayMessage
import com.smiledev.rafiq.domain.model.Ayah

private val arabicFont = FontFamily(Font(R.font.me_quran))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahScreen(
    suraNumber: Int,
    suraName: String,
    scrollToAya: Int = 0,
    onBack: () -> Unit,
    viewModel: QuranViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var longPressedAyah by remember { mutableStateOf<Ayah?>(null) }

    val listState = rememberLazyListState()
    var hasScrolled by remember(suraNumber, scrollToAya) { mutableStateOf(false) }

    LaunchedEffect(suraNumber) {
        viewModel.loadAyahs(suraNumber)
    }

    LaunchedEffect(state.isLoading, state.ayahs, scrollToAya) {
        if (!state.isLoading && state.ayahs.isNotEmpty() && scrollToAya > 0 && !hasScrolled) {
            val ayahIndex = state.ayahs.indexOfFirst { it.aya == scrollToAya }
            if (ayahIndex != -1) {
                val targetIndex = if (state.currentSurah != null) ayahIndex + 1 else ayahIndex
                listState.scrollToItem(targetIndex)
                hasScrolled = true
            }
        }
    }

    longPressedAyah?.let { ayah ->
        val isBookmarked = state.bookmarkedAyahs.contains(ayah.aya)
        AlertDialog(
            onDismissRequest = { longPressedAyah = null },
            title = { Text("$suraNumber:${ayah.aya} - $suraName") },
            text = {
                Text(
                    text = ayah.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = arabicFont,
                        fontSize = 20.sp,
                        textDirection = TextDirection.Rtl
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleBookmark(suraNumber, ayah.aya, suraName)
                    longPressedAyah = null
                }) {
                    Text(if (isBookmarked) "Remove Bookmark" else "Add Bookmark")
                }
            },
            dismissButton = {
                TextButton(onClick = { longPressedAyah = null }) {
                    Text("Cancel")
                }
            }
        )
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).semantics { contentDescription = "Loading" })
                }
                state.error != null -> {
                    Text(
                        text = state.error?.displayMessage ?: "",
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = modifier.fillMaxSize()
                    ) {
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
                            VerseCell(
                                ayah = ayah,
                                translationLanguage = state.translationLanguage,
                                isBookmarked = state.bookmarkedAyahs.contains(ayah.aya),
                                onLongPress = { longPressedAyah = ayah },
                                ayahFontSize = state.ayahFontSize,
                                translationFontSize = state.translationFontSize
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerseCell(
    ayah: Ayah,
    translationLanguage: String,
    isBookmarked: Boolean,
    onLongPress: () -> Unit,
    ayahFontSize: Int = 22,
    translationFontSize: Int = 15
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {},
            onLongClick = onLongPress
        )
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
                fontSize = ayahFontSize.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            )
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
        }

        if (ayah.sajda) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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
            Column(
                modifier = Modifier.width(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${ayah.aya}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (isBookmarked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = stringResource(R.string.bookmark_ayah_bookmarked),
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = ayah.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = ayahFontSize.sp,
                    fontFamily = arabicFont,
                    textDirection = TextDirection.Rtl,
                    lineHeight = (ayahFontSize * 2).sp
                ),
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }

        val resolvedLang = if (translationLanguage == "system") {
            currentLocaleCode()
        } else {
            translationLanguage
        }

        val hasId = !ayah.translationId.isNullOrBlank()
        val hasEn = !ayah.translationEn.isNullOrBlank()

        when (resolvedLang) {
            "id" -> {
                val text = if (hasId) ayah.translationId else if (hasEn) ayah.translationEn else null
                if (text != null) {
                    Text(
                        text = "${ayah.aya}. $text",
                        fontSize = translationFontSize.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = (translationFontSize * 1.6).sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                } else {
                    Text(
                        text = "[Translation unavailable]",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
            "en" -> {
                val text = if (hasEn) ayah.translationEn else if (hasId) ayah.translationId else null
                if (text != null) {
                    Text(
                        text = "${ayah.aya}. $text",
                        fontSize = translationFontSize.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = (translationFontSize * 1.6).sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                } else {
                    Text(
                        text = "[Translation unavailable]",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
            "both" -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasId) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ID",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ayah.translationId!!,
                                fontSize = translationFontSize.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = (translationFontSize * 1.6).sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (hasEn) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "EN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ayah.translationEn!!,
                                fontSize = translationFontSize.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = (translationFontSize * 1.6).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (!hasId && !hasEn) {
                        Text(
                            text = "[Translation unavailable]",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Color.Gray.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun BadgesRow(ayah: Ayah) {
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
