package com.smiledev.rafiq_quran.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.smiledev.rafiq_quran.AsmaulHusna
import com.smiledev.rafiq_quran.Ayah
import com.smiledev.rafiq_quran.BookmarkList
import com.smiledev.rafiq_quran.HadithBooks
import com.smiledev.rafiq_quran.IslamicCalendar
import com.smiledev.rafiq_quran.Mosques
import com.smiledev.rafiq_quran.PrayerLog
import com.smiledev.rafiq_quran.PrayerTimes
import com.smiledev.rafiq_quran.Prophets
import com.smiledev.rafiq_quran.Qibla
import com.smiledev.rafiq_quran.Quran
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.Recitation
import com.smiledev.rafiq_quran.Settings
import com.smiledev.rafiq_quran.Tasbih
import com.smiledev.rafiq_quran.ZakatCalculator
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.domain.model.PrayerTimeEntry

private data class QuickServiceItem(
    val labelResId: Int,
    val navKey: NavKey,
    val iconResId: Int,
    val tintColor: Color,
    val containerColor: Color
)

private val quickServices = listOf(
    // Row 1: Core Daily Features
    QuickServiceItem(
        labelResId = R.string.quran,
        navKey = Quran(),
        iconResId = R.drawable.ic_quran,
        tintColor = Color(0xFF00796B),
        containerColor = Color(0xFFE0F2F1)
    ),
    QuickServiceItem(
        labelResId = R.string.prayer_times,
        navKey = PrayerTimes,
        iconResId = R.drawable.ic_calendar,
        tintColor = Color(0xFF00897B),
        containerColor = Color(0xFFE0F2F1)
    ),
    QuickServiceItem(
        labelResId = R.string.qibla,
        navKey = Qibla,
        iconResId = R.drawable.ic_qibla,
        tintColor = Color(0xFFD97706),
        containerColor = Color(0xFFFEF3C7)
    ),
    QuickServiceItem(
        labelResId = R.string.tasbih,
        navKey = Tasbih,
        iconResId = R.drawable.ic_tasbih,
        tintColor = Color(0xFF0D9488),
        containerColor = Color(0xFFCCFBF1)
    ),

    // Row 2: Knowledge & Stories
    QuickServiceItem(
        labelResId = R.string.hadiths,
        navKey = HadithBooks,
        iconResId = R.drawable.ic_hadith,
        tintColor = Color(0xFF78350F),
        containerColor = Color(0xFFFDE68A).copy(alpha = 0.5f)
    ),
    QuickServiceItem(
        labelResId = R.string.prophets,
        navKey = Prophets,
        iconResId = R.drawable.ic_prophet,
        tintColor = Color(0xFFB45309),
        containerColor = Color(0xFFFDE68A)
    ),
    QuickServiceItem(
        labelResId = R.string.asmaul_husna,
        navKey = AsmaulHusna,
        iconResId = R.drawable.ic_asmaul_husna,
        tintColor = Color(0xFF15803D),
        containerColor = Color(0xFFDCFCE7)
    ),
    QuickServiceItem(
        labelResId = R.string.recitations,
        navKey = Recitation,
        iconResId = R.drawable.ic_play,
        tintColor = Color(0xFF2563EB),
        containerColor = Color(0xFFDBEAFE)
    ),

    // Row 3: Islamic Community & Tools
    QuickServiceItem(
        labelResId = R.string.mosques,
        navKey = Mosques,
        iconResId = R.drawable.ic_mosque,
        tintColor = Color(0xFF16A34A),
        containerColor = Color(0xFFDCFCE7)
    ),
    QuickServiceItem(
        labelResId = R.string.calendar,
        navKey = IslamicCalendar,
        iconResId = R.drawable.ic_calendar,
        tintColor = Color(0xFF0F766E),
        containerColor = Color(0xFFCCFBF1)
    ),
    QuickServiceItem(
        labelResId = R.string.zakat,
        navKey = ZakatCalculator,
        iconResId = R.drawable.ic_zakat,
        tintColor = Color(0xFFEA580C),
        containerColor = Color(0xFFFFEDD5)
    ),
    QuickServiceItem(
        labelResId = R.string.prayer_log,
        navKey = PrayerLog,
        iconResId = R.drawable.ic_prayer_log,
        tintColor = Color(0xFF4F46E5),
        containerColor = Color(0xFFEEF2FF)
    )
)

@Composable
fun DashboardScreen(
    onNavigate: (NavKey) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

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

        // 4. Quick Access Hub (12 Features in 4 Columns)
        Text(
            text = stringResource(R.string.quick_access),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        QuickServicesGrid(
            items = quickServices,
            onServiceClick = { onNavigate(it) }
        )

        Spacer(Modifier.height(20.dp))

        // 5. Verse of the Day Card
        DailyAyahCard(
            arabic = state.dailyAyahArabic,
            translation = state.dailyAyahTranslation,
            surahRef = state.dailyAyahSurahRef,
            onReadClick = {
                onNavigate(Ayah(
                    suraNumber = state.dailyAyahSuraNumber,
                    suraName = state.dailyAyahSurahRef,
                    scrollToAya = state.dailyAyahNumber
                ))
            }
        )

        Spacer(Modifier.height(16.dp))

        // 6. Today's Prayer Completion Overview
        TodayPrayerStatusCard(
            completedCount = state.todayCompletedPrayersCount,
            onOpenLog = { onNavigate(PrayerLog) }
        )

        Spacer(Modifier.height(24.dp))

        // 7. Footer App Version
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

@Composable
private fun DashboardHeader(
    greeting: String,
    hijriDate: String,
    gregorianDate: String,
    cityName: String,
    onSettingsClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.greeting_assalamualaikum),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.your_islamic_companion),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBookmarksClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bookmark),
                        contentDescription = stringResource(R.string.bookmarks),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Hijri & Gregorian date badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                val displayDate = if (hijriDate.isNotBlank()) {
                    "$hijriDate • $gregorianDate"
                } else {
                    gregorianDate
                }
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = cityName,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = cityName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HeroPrayerCard(
    state: DashboardUiState,
    onPrayerTimesClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onPrayerTimesClick),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00695C), // Deep Emerald Teal
                            Color(0xFF00897B), // Rich Teal
                            Color(0xFF004D40)  // Darker Teal base
                        )
                    )
                )
                .padding(20.dp)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error?.displayMessage ?: "",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onRetry) {
                            Text(
                                text = stringResource(R.string.retry),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top row: Next prayer label & countdown pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = state.nextPrayerName.uppercase(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = state.nextPrayerTime,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Countdown badge
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = stringResource(R.string.next_prayer, state.countdown),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(Modifier.height(14.dp))

                        // Horizontal 5-prayer timeline
                        if (state.prayerTimeline.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                state.prayerTimeline.forEachIndexed { index, prayer ->
                                    val isActive = index == state.activePrayerIndex
                                    PrayerTimelineItem(
                                        name = prayer.name,
                                        time = prayer.time,
                                        isActive = isActive
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTimelineItem(
    name: String,
    time: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Color.White.copy(alpha = 0.25f) else Color.Transparent,
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)) else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = time,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (isActive) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD54F)) // Gold indicator dot
                )
            }
        }
    }
}

@Composable
private fun LastReadQuranCard(
    suraNumber: Int,
    ayaNumber: Int,
    suraName: String,
    onContinueReading: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onContinueReading),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Book icon in rounded teal container
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00796B).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_quran),
                        contentDescription = stringResource(R.string.quran),
                        tint = Color(0xFF00796B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        text = if (suraNumber > 0) stringResource(R.string.last_read) else stringResource(R.string.quran),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (suraNumber > 0) {
                            stringResource(R.string.surah_and_ayah, suraName, ayaNumber)
                        } else {
                            stringResource(R.string.start_reading)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Continue CTA button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = if (suraNumber > 0) stringResource(R.string.continue_reading) else stringResource(R.string.start_reading),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickServicesGrid(
    items: List<QuickServiceItem>,
    onServiceClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rowItems.forEach { item ->
                    QuickServiceCard(
                        item = item,
                        onClick = { onServiceClick(item.navKey) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining spaces if row has fewer than 4 items
                repeat(4 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickServiceCard(
    item: QuickServiceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(item.containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(item.iconResId),
                contentDescription = stringResource(item.labelResId),
                tint = item.tintColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(item.labelResId),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DailyAyahCard(
    arabic: String,
    translation: String,
    surahRef: String,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onReadClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFA000).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = stringResource(R.string.verse_of_the_day),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = surahRef,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))

            // Arabic text with me_quran font
            Text(
                text = arabic,
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.me_quran)),
                    fontSize = 20.sp,
                    lineHeight = 36.sp,
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Right
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // Translation text
            Text(
                text = "\"$translation\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun TodayPrayerStatusCard(
    completedCount: Int,
    onOpenLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenLog),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF4F46E5).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_prayer_log),
                        contentDescription = stringResource(R.string.prayer_log),
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.today_prayers),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stringResource(R.string.completed)}: $completedCount/5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${stringResource(R.string.open_prayer_log)} >",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
