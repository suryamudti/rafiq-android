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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
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

@Composable
fun WearSurahListScreen(
    onSurahClick: (Int) -> Unit,
    viewModel: WearQuranViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(WearColors.DeepNavy)
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
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
            // Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "AL-QUR'AN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WearColors.GoldAccent,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "114 Surahs",
                        fontSize = 10.sp,
                        color = WearColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Highlighted Last Read Chip
            if (state.lastReadSura > 0) {
                item {
                    val lastSurah = state.lastReadSurah
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSurahClick(state.lastReadSura) },
                        label = {
                            Text(
                                text = "Last Read",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WearColors.GoldAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = if (lastSurah != null) {
                                    "${lastSurah.nameSimple} \u2022 Ayah ${state.lastReadAya}"
                                } else {
                                    "Surah ${state.lastReadSura} \u2022 Ayah ${state.lastReadAya}"
                                },
                                fontSize = 10.sp,
                                color = WearColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_quran),
                                contentDescription = "Last Read",
                                tint = WearColors.GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = WearColors.SurfaceCard,
                            contentColor = WearColors.TextPrimary
                        )
                    )
                }
            }

            // Compact Filter Pills Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allSelected = state.revelationFilter == WearRevelationFilter.ALL
                    CompactChip(
                        onClick = { viewModel.setRevelationFilter(WearRevelationFilter.ALL) },
                        label = {
                            Text(
                                text = "All",
                                fontSize = 10.sp,
                                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (allSelected) WearColors.TealPrimary else WearColors.SurfaceDark,
                            contentColor = if (allSelected) Color.Black else WearColors.TextPrimary
                        )
                    )

                    val meccanSelected = state.revelationFilter == WearRevelationFilter.MECCAN
                    CompactChip(
                        onClick = { viewModel.setRevelationFilter(WearRevelationFilter.MECCAN) },
                        label = {
                            Text(
                                text = "Makkah",
                                fontSize = 10.sp,
                                fontWeight = if (meccanSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (meccanSelected) WearColors.TealPrimary else WearColors.SurfaceDark,
                            contentColor = if (meccanSelected) Color.Black else WearColors.TextPrimary
                        )
                    )

                    val medinanSelected = state.revelationFilter == WearRevelationFilter.MEDINAN
                    CompactChip(
                        onClick = { viewModel.setRevelationFilter(WearRevelationFilter.MEDINAN) },
                        label = {
                            Text(
                                text = "Madinah",
                                fontSize = 10.sp,
                                fontWeight = if (medinanSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (medinanSelected) WearColors.TealPrimary else WearColors.SurfaceDark,
                            contentColor = if (medinanSelected) Color.Black else WearColors.TextPrimary
                        )
                    )
                }
            }

            // Loading State
            if (state.isLoadingSurahs) {
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
            if (state.surahsError != null && !state.isLoadingSurahs) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WearColors.SurfaceDark, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = state.surahsError?.displayMessage ?: "Failed to load Surahs",
                            fontSize = 10.sp,
                            color = WearColors.ErrorRed,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompactChip(
                            onClick = { viewModel.loadSurahs() },
                            label = { Text("Retry", fontSize = 10.sp) },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = WearColors.TealPrimary,
                                contentColor = Color.Black
                            )
                        )
                    }
                }
            }

            // Surahs List
            items(state.filteredSurahs, key = { it.chapterNumber }) { surah ->
                Card(
                    onClick = { onSurahClick(surah.chapterNumber) },
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = WearColors.SurfaceCard,
                        endBackgroundColor = WearColors.SurfaceDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(WearColors.SurfaceDark, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${surah.chapterNumber}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WearColors.TealBright
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = surah.nameSimple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WearColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${surah.translatedName} \u2022 ${surah.versesCount} Ayahs",
                                    fontSize = 9.sp,
                                    color = WearColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = surah.nameArabic,
                            fontFamily = WearArabicFontFamily,
                            fontSize = 14.sp,
                            color = WearColors.GoldAccent,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
