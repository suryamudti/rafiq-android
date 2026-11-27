package com.smiledev.rafiq_quran.ui.dashboard

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.smiledev.rafiq_quran.Ayah
import com.smiledev.rafiq_quran.BookmarkList
import com.smiledev.rafiq_quran.HadithDetail
import com.smiledev.rafiq_quran.PrayerLog
import com.smiledev.rafiq_quran.PrayerTimes
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.Settings
import com.smiledev.rafiq_quran.ui.components.AppUpdateDialog

@Composable
fun DashboardScreen(
    onNavigate: (NavKey) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    if (state.updateInfo != null) {
        AppUpdateDialog(
            updateInfo = state.updateInfo!!,
            onDismiss = viewModel::dismissUpdateDialog
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Top Header Bar
        DashboardHeader(
            greeting = state.greeting,
            hijriDate = state.hijriDate,
            gregorianDate = state.gregorianDate,
            cityName = state.cityName,
            onSettingsClick = { onNavigate(Settings) },
            onBookmarksClick = { onNavigate(BookmarkList) }
        )

        Spacer(Modifier.height(16.dp))

        // 2. Hero Prayer Schedule Card
        HeroPrayerCard(
            state = state,
            onPrayerTimesClick = { onNavigate(PrayerTimes) },
            onRetry = { viewModel.refresh() }
        )

        Spacer(Modifier.height(16.dp))

        // 3. Last Read / Continue Reading Quran Card
        LastReadQuranCard(
            suraNumber = state.lastReadSura,
            ayaNumber = state.lastReadAya,
            suraName = state.lastReadSuraName,
            onContinueReading = {
                val targetSura = if (state.lastReadSura > 0) state.lastReadSura else 1
                val targetName = if (state.lastReadSuraName.isNotBlank()) state.lastReadSuraName else "Al-Fatihah"
                val targetAya = if (state.lastReadAya > 0) state.lastReadAya else 1
                onNavigate(Ayah(suraNumber = targetSura, suraName = targetName, scrollToAya = targetAya))
            }
        )

        Spacer(Modifier.height(20.dp))

        // 4. Quick Access Hub
        Text(
            text = stringResource(R.string.quick_access),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        val visibleServices = quickServices.filter { it.key !in state.hiddenKeys }
        if (visibleServices.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_empty_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { onNavigate(Settings) }) {
                        Text(
                            text = stringResource(R.string.dashboard_empty_action),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            QuickServicesGrid(
                items = visibleServices,
                onServiceClick = { onNavigate(it) }
            )
        }

        Spacer(Modifier.height(20.dp))

        // 5. Verse of the Day Card
        DailyAyahCard(
            arabic = state.dailyAyahArabic,
            translation = state.dailyAyahTranslation,
            surahRef = state.dailyAyahSurahRef,
            onReadClick = {
                onNavigate(
                    Ayah(
                        suraNumber = state.dailyAyahSuraNumber,
                        suraName = state.dailyAyahSurahRef,
                        scrollToAya = state.dailyAyahNumber
                    )
                )
            }
        )

        Spacer(Modifier.height(16.dp))

        // 6. Hadith of the Day Card
        DailyHadithCard(
            arabic = state.dailyHadithArabic,
            translation = state.dailyHadithTranslation,
            narrator = state.dailyHadithNarrator,
            hadithRef = state.dailyHadithRef,
            onReadClick = {
                onNavigate(HadithDetail(hadithId = state.dailyHadithId))
            }
        )

        Spacer(Modifier.height(16.dp))

        // 7. Today's Prayer Completion Overview
        TodayPrayerStatusCard(
            completedCount = state.todayCompletedPrayersCount,
            onOpenLog = { onNavigate(PrayerLog) }
        )

        Spacer(Modifier.height(24.dp))

        // 8. Footer App Version
        Text(
            text = "v${state.appVersion}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Center
        )
    }
}
