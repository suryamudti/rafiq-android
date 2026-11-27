package com.smiledev.rafiq_quran.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.theme.ArabicFontFamily
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqBadge
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqNumberBadge
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqHeroCard
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqOutlinedCard
import com.smiledev.rafiq_quran.ui.designsystem.divider.RafiqDivider

/**
 * Ornate Hero Header card for Surah, including the Bismillah banner.
 */
@Composable
internal fun SurahHeroHeaderCard(
    surah: Surah?,
    suraNumber: Int,
    suraName: String,
    modifier: Modifier = Modifier
) {
    RafiqHeroCard(
        gradient = RafiqTheme.extendedColors.quranGradient,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$suraNumber. ${surah?.nameSimple ?: suraName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (surah != null) {
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = surah.nameArabic,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = ArabicFontFamily,
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Center
                        ),
                        color = RafiqTheme.extendedColors.goldAccent
                    )
                }
                Text(
                    text = surah.translatedName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RafiqBadge(
                        text = surah.revelationPlace.replaceFirstChar { it.uppercase() },
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                    RafiqBadge(
                        text = stringResource(R.string.verses_count, surah.versesCount),
                        containerColor = RafiqTheme.extendedColors.goldAccent.copy(alpha = 0.25f),
                        contentColor = RafiqTheme.extendedColors.goldAccent
                    )
                }
            }

            // Bismillah banner (All surahs except Surah At-Tawbah 9)
            if (suraNumber != 9) {
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.12f),
                            RafiqTheme.customShapes.medium
                        )
                        .padding(vertical = RafiqTheme.spacing.m, horizontal = RafiqTheme.spacing.l),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = stringResource(R.string.bismillah_full),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = ArabicFontFamily,
                                textDirection = TextDirection.Rtl,
                                textAlign = TextAlign.Center
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Verse Card with Juz/Page/Sajda markers, Arabic script, translation, and expandable tafsir.
 */
@Composable
internal fun VerseCard(
    ayah: Ayah,
    suraNumber: Int,
    suraName: String,
    translationLanguage: String,
    isBookmarked: Boolean,
    isPlayingAyah: Boolean,
    onToggleAudio: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleTafsir: () -> Unit,
    onOpenActions: () -> Unit,
    showTranslation: Boolean = true,
    memorizationMode: Boolean,
    memorizationRevealed: Boolean,
    onRevealTranslation: () -> Unit,
    tafsirText: String?,
    tafsirLoading: Boolean,
    tafsirExpanded: Boolean,
    onRetryTafsir: () -> Unit,
    ayahFontSize: Int,
    translationFontSize: Int,
    modifier: Modifier = Modifier
) {
    val cardColor = if (isPlayingAyah) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    RafiqOutlinedCard(
        colors = CardDefaults.outlinedCardColors(
            containerColor = cardColor
        ),
        modifier = modifier
    ) {
        // Juz / Page Markers if this Ayah starts one
        if (ayah.isFirstAyaOfJuz || ayah.isFirstAyaOfPage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RafiqTheme.spacing.m),
                horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)
            ) {
                if (ayah.isFirstAyaOfJuz) {
                    RafiqBadge(
                        text = stringResource(R.string.juz_badge, ayah.juz),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
                if (ayah.isFirstAyaOfPage) {
                    RafiqBadge(
                        text = stringResource(R.string.page_badge, ayah.page),
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Top Verse Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ayah number badge
            RafiqNumberBadge(number = ayah.aya)

            // Sajdah chip
            if (ayah.sajda) {
                Spacer(modifier = Modifier.width(RafiqTheme.spacing.s))
                val isObligatory = ayah.sajdaType == "obligatory"
                RafiqBadge(
                    text = if (isObligatory) stringResource(R.string.sajda_obligatory) else stringResource(R.string.sajda_recommended),
                    containerColor = if (isObligatory) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else RafiqTheme.extendedColors.goldAccent.copy(alpha = 0.2f),
                    contentColor = if (isObligatory) MaterialTheme.colorScheme.error else RafiqTheme.extendedColors.goldAccent
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tactile Action buttons (Emil Kowalski micro-interactions)
            IconButton(
                onClick = onToggleAudio,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = if (isPlayingAyah) "Playing" else "Play",
                    tint = if (isPlayingAyah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = if (isBookmarked) stringResource(R.string.remove_bookmark) else stringResource(R.string.add_bookmark),
                    tint = if (isBookmarked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onToggleTafsir,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
            ) {
                Icon(
                    imageVector = if (tafsirExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (tafsirExpanded) stringResource(R.string.hide_tafsir) else stringResource(R.string.show_tafsir),
                    tint = if (tafsirExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenActions,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .rafiqPressable(targetScale = RafiqMotion.IconButtonPressScale)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Ayah options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

        // Arabic Verse Text
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = ayah.text,
                textAlign = TextAlign.Right,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = ayahFontSize.sp,
                    fontFamily = ArabicFontFamily,
                    textDirection = TextDirection.Rtl,
                    lineHeight = (ayahFontSize * 1.95).sp,
                    textAlign = TextAlign.Right
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Translation section
        if (showTranslation) {
            Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

            if (memorizationMode && !memorizationRevealed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RafiqTheme.customShapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .clickable(onClick = onRevealTranslation)
                        .padding(RafiqTheme.spacing.m),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.tap_to_reveal_translation),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                val resolvedLang = if (translationLanguage == "system") currentLocaleCode() else translationLanguage
                val hasId = !ayah.translationId.isNullOrBlank()
                val hasEn = !ayah.translationEn.isNullOrBlank()
                val localTransId = ayah.translationId
                val localTransEn = ayah.translationEn

                val containerModifier = if (memorizationRevealed) {
                    Modifier
                        .fillMaxWidth()
                        .background(
                            RafiqTheme.extendedColors.goldContainer,
                            RafiqTheme.customShapes.small
                        )
                        .padding(RafiqTheme.spacing.m)
                } else {
                    Modifier.fillMaxWidth()
                }

                Column(modifier = containerModifier) {
                    when (resolvedLang) {
                        "id" -> {
                            val text = if (hasId) localTransId else if (hasEn) localTransEn else null
                            if (text != null) {
                                Text(
                                    text = "${ayah.aya}. $text",
                                    fontSize = translationFontSize.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = (translationFontSize * 1.6).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.translation_unavailable_bracketed),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                        "en" -> {
                            val text = if (hasEn) localTransEn else if (hasId) localTransId else null
                            if (text != null) {
                                Text(
                                    text = "${ayah.aya}. $text",
                                    fontSize = translationFontSize.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = (translationFontSize * 1.6).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.translation_unavailable_bracketed),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                        "both" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)) {
                                if (hasId && localTransId != null) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        RafiqBadge(
                                            text = "ID",
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(RafiqTheme.spacing.s))
                                        Text(
                                            text = localTransId,
                                            fontSize = translationFontSize.sp,
                                            lineHeight = (translationFontSize * 1.6).sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                if (hasEn && localTransEn != null) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        RafiqBadge(
                                            text = "EN",
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.width(RafiqTheme.spacing.s))
                                        Text(
                                            text = localTransEn,
                                            fontSize = translationFontSize.sp,
                                            lineHeight = (translationFontSize * 1.6).sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expandable Tafsir Section
        AnimatedVisibility(
            visible = tafsirExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RafiqTheme.spacing.m)
            ) {
                RafiqDivider()
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.tafsir),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (tafsirLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))

                when {
                    tafsirLoading -> {
                        Text(
                            text = stringResource(R.string.loading_tafsir),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = RafiqTheme.spacing.s)
                        )
                    }
                    tafsirText != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    RafiqTheme.customShapes.small
                                )
                                .padding(RafiqTheme.spacing.m)
                        ) {
                            Text(
                                text = tafsirText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = RafiqTheme.spacing.xs)
                        ) {
                            Text(
                                text = "Tafsir unavailable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(RafiqTheme.spacing.s))
                            TextButton(onClick = onRetryTafsir) {
                                Text(stringResource(R.string.retry), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
