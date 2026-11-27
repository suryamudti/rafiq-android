package com.smiledev.rafiq_quran.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.smiledev.rafiq_quran.AsmaulHusna
import com.smiledev.rafiq_quran.HadithBooks
import com.smiledev.rafiq_quran.Hifz
import com.smiledev.rafiq_quran.IslamicCalendar
import com.smiledev.rafiq_quran.Mosques
import com.smiledev.rafiq_quran.PrayerGuidance
import com.smiledev.rafiq_quran.PrayerLog
import com.smiledev.rafiq_quran.PrayerTimes
import com.smiledev.rafiq_quran.Prophets
import com.smiledev.rafiq_quran.Qibla
import com.smiledev.rafiq_quran.Quran
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.Recitation
import com.smiledev.rafiq_quran.SunnahGuidance
import com.smiledev.rafiq_quran.Tasbih
import com.smiledev.rafiq_quran.ZakatCalculator
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable

internal data class QuickServiceItem(
    val key: String,
    val labelResId: Int,
    val navKey: NavKey,
    val iconResId: Int,
    val tintColor: Color,
    val containerColor: Color
)

internal val quickServices = listOf(
    // Row 1: Core Daily Features
    QuickServiceItem(
        key = "quran",
        labelResId = R.string.quran,
        navKey = Quran(),
        iconResId = R.drawable.ic_quran,
        tintColor = Color(0xFF00796B),
        containerColor = Color(0xFFE0F2F1)
    ),
    QuickServiceItem(
        key = "prayer_times",
        labelResId = R.string.prayer_times,
        navKey = PrayerTimes,
        iconResId = R.drawable.ic_prayer_times,
        tintColor = Color(0xFF00897B),
        containerColor = Color(0xFFE0F2F1)
    ),
    QuickServiceItem(
        key = "qibla",
        labelResId = R.string.qibla,
        navKey = Qibla,
        iconResId = R.drawable.ic_qibla,
        tintColor = Color(0xFFD97706),
        containerColor = Color(0xFFFEF3C7)
    ),
    QuickServiceItem(
        key = "tasbih",
        labelResId = R.string.tasbih,
        navKey = Tasbih,
        iconResId = R.drawable.ic_tasbih,
        tintColor = Color(0xFF0D9488),
        containerColor = Color(0xFFCCFBF1)
    ),

    // Row 2: Knowledge & Stories
    QuickServiceItem(
        key = "hadith",
        labelResId = R.string.hadiths,
        navKey = HadithBooks,
        iconResId = R.drawable.ic_hadith,
        tintColor = Color(0xFF78350F),
        containerColor = Color(0xFFFDE68A).copy(alpha = 0.5f)
    ),
    QuickServiceItem(
        key = "prophets",
        labelResId = R.string.prophets,
        navKey = Prophets,
        iconResId = R.drawable.ic_prophet,
        tintColor = Color(0xFFB45309),
        containerColor = Color(0xFFFDE68A)
    ),
    QuickServiceItem(
        key = "asmaul_husna",
        labelResId = R.string.asmaul_husna,
        navKey = AsmaulHusna,
        iconResId = R.drawable.ic_asmaul_husna,
        tintColor = Color(0xFF15803D),
        containerColor = Color(0xFFDCFCE7)
    ),
    QuickServiceItem(
        key = "recitation",
        labelResId = R.string.recitations,
        navKey = Recitation,
        iconResId = R.drawable.ic_recitation,
        tintColor = Color(0xFF2563EB),
        containerColor = Color(0xFFDBEAFE)
    ),

    // Row 3: Islamic Community & Tools
    QuickServiceItem(
        key = "mosques",
        labelResId = R.string.mosques,
        navKey = Mosques,
        iconResId = R.drawable.ic_mosque,
        tintColor = Color(0xFF16A34A),
        containerColor = Color(0xFFDCFCE7)
    ),
    QuickServiceItem(
        key = "calendar",
        labelResId = R.string.calendar,
        navKey = IslamicCalendar,
        iconResId = R.drawable.ic_calendar,
        tintColor = Color(0xFF0F766E),
        containerColor = Color(0xFFCCFBF1)
    ),
    QuickServiceItem(
        key = "zakat",
        labelResId = R.string.zakat,
        navKey = ZakatCalculator,
        iconResId = R.drawable.ic_zakat,
        tintColor = Color(0xFFEA580C),
        containerColor = Color(0xFFFFEDD5)
    ),
    QuickServiceItem(
        key = "prayer_log",
        labelResId = R.string.prayer_log,
        navKey = PrayerLog,
        iconResId = R.drawable.ic_prayer_log,
        tintColor = Color(0xFF4F46E5),
        containerColor = Color(0xFFEEF2FF)
    ),
    QuickServiceItem(
        key = "prayer_guidance",
        labelResId = R.string.prayer_guidance,
        navKey = PrayerGuidance,
        iconResId = R.drawable.ic_prayer_guide,
        tintColor = Color(0xFF00796B),
        containerColor = Color(0xFFE0F2F1)
    ),
    QuickServiceItem(
        key = "sunnah_guidance",
        labelResId = R.string.sunnah_guidance,
        navKey = SunnahGuidance,
        iconResId = R.drawable.ic_sunnah,
        tintColor = Color(0xFFD97706),
        containerColor = Color(0xFFFEF3C7)
    ),
    QuickServiceItem(
        key = "hifz",
        labelResId = R.string.hifz_memorization,
        navKey = Hifz,
        iconResId = R.drawable.ic_hifz,
        tintColor = Color(0xFF0D9488),
        containerColor = Color(0xFFCCFBF1)
    )
)

@Composable
internal fun LastReadQuranCard(
    suraNumber: Int,
    ayaNumber: Int,
    suraName: String,
    onContinueReading: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .rafiqPressable(targetScale = RafiqMotion.CardPressScale)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onContinueReading),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
internal fun QuickServicesGrid(
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
            .clip(RoundedCornerShape(14.dp))
            .rafiqPressable(targetScale = RafiqMotion.ChipPressScale)
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
internal fun DailyAyahCard(
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
            .rafiqPressable(targetScale = RafiqMotion.CardPressScale)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onReadClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = arabic,
                    textAlign = TextAlign.Right,
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.me_quran)),
                        fontSize = 20.sp,
                        lineHeight = 42.sp,
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Right
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
internal fun DailyHadithCard(
    arabic: String,
    translation: String,
    narrator: String?,
    hadithRef: String,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .rafiqPressable(targetScale = RafiqMotion.CardPressScale)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onReadClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    color = Color(0xFF00796B).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = stringResource(R.string.hadith_of_the_day),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00796B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = hadithRef,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (arabic.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                // Arabic text with me_quran font
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = arabic,
                        textAlign = TextAlign.Right,
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.me_quran)),
                            fontSize = 20.sp,
                            lineHeight = 42.sp,
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Right
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!narrator.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = narrator,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(8.dp))

            // Translation text
            Text(
                text = "\"$translation\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.read_hadith)} \u2192",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
internal fun TodayPrayerStatusCard(
    completedCount: Int,
    onOpenLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .rafiqPressable(targetScale = RafiqMotion.CardPressScale)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onOpenLog),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
