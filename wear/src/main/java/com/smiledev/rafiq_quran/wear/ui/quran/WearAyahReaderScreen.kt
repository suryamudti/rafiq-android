package com.smiledev.rafiq_quran.wear.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.wear.R
import com.smiledev.rafiq_quran.wear.theme.WearArabicFontFamily
import com.smiledev.rafiq_quran.wear.theme.WearColors
import com.smiledev.rafiq_quran.wear.ui.components.wearRotaryScroll

@Composable
fun WearAyahReaderScreen(
    suraNumber: Int,
    initialAyaNumber: Int = 1,
    onNavigateSurah: (Int) -> Unit = {},
    viewModel: WearQuranViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var scrolledToInitialAya by remember(suraNumber, initialAyaNumber) { mutableStateOf(false) }

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(suraNumber) {
        viewModel.loadAyahs(suraNumber)
    }

    LaunchedEffect(suraNumber, initialAyaNumber, state.ayahs, state.isLoadingAyahs) {
        if (!scrolledToInitialAya && initialAyaNumber > 1 && !state.isLoadingAyahs && state.ayahs.isNotEmpty()) {
            val ayahIdx = state.ayahs.indexOfFirst { it.aya == initialAyaNumber }
            if (ayahIdx >= 0) {
                val hasBismillah = suraNumber != 1 && suraNumber != 9
                val headerItemsCount = if (hasBismillah) 3 else 2
                listState.scrollToItem(headerItemsCount + ayahIdx)
            }
            scrolledToInitialAya = true
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(WearColors.DeepNavy)
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .wearRotaryScroll(listState),
            state = listState,
            autoCentering = null,
            contentPadding = PaddingValues(
                top = 28.dp,
                start = 10.dp,
                end = 10.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Surah Header Card
            item {
                Card(
                    onClick = {},
                    enabled = false,
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = WearColors.SurfaceCard,
                        endBackgroundColor = WearColors.SurfaceDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val surah = state.currentSurah
                        if (surah != null) {
                            Text(
                                text = surah.nameArabic,
                                fontFamily = WearArabicFontFamily,
                                fontSize = 16.sp,
                                color = WearColors.GoldAccent,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${surah.chapterNumber}. ${surah.nameSimple}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WearColors.TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = surah.translatedName,
                                fontSize = 10.sp,
                                color = WearColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Surah $suraNumber",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WearColors.TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Compact Toggle Translation Chip
            item {
                CompactChip(
                    onClick = { viewModel.toggleShowTranslation() },
                    label = {
                        Text(
                            text = stringResource(
                                if (state.showTranslation) {
                                    R.string.wear_hide_translation
                                } else {
                                    R.string.wear_show_translation
                                }
                            ),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (state.showTranslation) WearColors.TealPrimary else WearColors.SurfaceDark,
                        contentColor = if (state.showTranslation) Color.Black else WearColors.TextPrimary
                    )
                )
            }

            // Bismillah Banner (All except Surah Al-Fatihah 1 and Surah At-Tawbah 9)
            if (suraNumber != 1 && suraNumber != 9) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WearColors.SurfaceDark, RoundedCornerShape(10.dp))
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                            fontFamily = WearArabicFontFamily,
                            fontSize = 14.sp,
                            color = WearColors.GoldAccent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Loading State
            if (state.isLoadingAyahs) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .size(26.dp),
                        indicatorColor = WearColors.TealBright,
                        trackColor = WearColors.SurfaceCard
                    )
                }
            }

            // Error State
            if (state.ayahsError != null && !state.isLoadingAyahs) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WearColors.SurfaceDark, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = state.ayahsError?.displayMessage ?: stringResource(R.string.wear_ayah_load_error),
                            fontSize = 10.sp,
                            color = WearColors.ErrorRed,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompactChip(
                            onClick = { viewModel.loadAyahs(suraNumber) },
                            label = { Text(stringResource(R.string.wear_retry), fontSize = 10.sp) },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = WearColors.TealPrimary,
                                contentColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Ayahs List
            items(state.ayahs, key = { it.aya }) { ayah ->
                val isSaved = state.lastReadSura == ayah.sura && state.lastReadAya == ayah.aya
                Card(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.saveLastRead(ayah.sura, ayah.aya)
                    },
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = if (isSaved) WearColors.SurfaceCard else WearColors.SurfaceDark,
                        endBackgroundColor = if (isSaved) WearColors.SurfaceDark else WearColors.SurfaceCard
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSaved) WearColors.GoldAccent else WearColors.TealPrimary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${ayah.sura}:${ayah.aya}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSaved) Color.Black else WearColors.TealBright
                                    )
                                }
                                if (ayah.juz > 0 || ayah.page > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Juz ${ayah.juz} • P.${ayah.page}",
                                        fontSize = 8.sp,
                                        color = WearColors.TextMuted
                                    )
                                }
                            }
                            if (isSaved) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = WearColors.GoldAccent.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.wear_saved),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WearColors.GoldAccent
                                    )
                                }
                            }
                        }

                        // Arabic Text
                        Text(
                            text = ayah.text,
                            fontFamily = WearArabicFontFamily,
                            fontSize = 15.sp,
                            color = WearColors.TextPrimary,
                            textAlign = TextAlign.Right,
                            style = TextStyle(textDirection = TextDirection.Rtl),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Optional Translation Text
                        if (state.showTranslation) {
                            val translationText = viewModel.getTranslationText(ayah)
                            if (!translationText.isNullOrBlank()) {
                                Text(
                                    text = translationText,
                                    fontSize = 11.sp,
                                    color = WearColors.TextSecondary,
                                    textAlign = TextAlign.Left,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Prev / Next Surah Footer Navigation
            if (!state.isLoadingAyahs && state.ayahs.isNotEmpty() && (suraNumber > 1 || suraNumber < 114)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (suraNumber > 1) {
                            CompactChip(
                                onClick = { onNavigateSurah(suraNumber - 1) },
                                label = {
                                    Text(
                                        text = stringResource(R.string.wear_prev_surah, suraNumber - 1),
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = WearColors.SurfaceDark,
                                    contentColor = WearColors.TextPrimary
                                )
                            )
                        }
                        if (suraNumber < 114) {
                            CompactChip(
                                onClick = { onNavigateSurah(suraNumber + 1) },
                                label = {
                                    Text(
                                        text = stringResource(R.string.wear_next_surah, suraNumber + 1),
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = WearColors.TealPrimary,
                                    contentColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
