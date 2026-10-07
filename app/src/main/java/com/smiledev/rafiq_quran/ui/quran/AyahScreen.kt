package com.smiledev.rafiq_quran.ui.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.theme.ArabicFontFamily
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.common.formatDuration
import com.smiledev.rafiq_quran.ui.common.rememberNotificationPermissionRequester
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqBadge
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqNumberBadge
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqHeroCard
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqOutlinedCard
import com.smiledev.rafiq_quran.ui.designsystem.divider.RafiqDivider
import com.smiledev.rafiq_quran.ui.designsystem.input.RafiqSearchBar
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqEmptyState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqErrorState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqLoadingIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahScreen(
    suraNumber: Int,
    suraName: String,
    scrollToAya: Int = 0,
    onBack: () -> Unit,
    onNavigateSurah: ((Int, String) -> Unit)? = null,
    viewModel: AyahViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var hasScrolled by remember(suraNumber, scrollToAya) { mutableStateOf(false) }
    var actionAyah by remember { mutableStateOf<Ayah?>(null) }
    var showJumpSheet by remember { mutableStateOf(false) }
    var showFontSizeSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    val expandedTafsirAyahs = remember { mutableStateMapOf<Int, Boolean>() }
    val scope = rememberCoroutineScope()
    val requestNotificationPermission = rememberNotificationPermissionRequester()

    LaunchedEffect(suraNumber) {
        viewModel.loadAyahs(suraNumber)
    }

    LaunchedEffect(state.isLoading, state.ayahs, scrollToAya) {
        if (!state.isLoading && state.ayahs.isNotEmpty() && !hasScrolled) {
            val targetAyah = if (scrollToAya > 0) scrollToAya else viewModel.getLastReadAyahForSura(suraNumber)
            if (targetAyah > 0) {
                val ayahIndex = state.ayahs.indexOfFirst { it.aya == targetAyah }
                if (ayahIndex != -1) {
                    listState.scrollToItem(ayahIndex + 1) // +1 for Surah header card
                    hasScrolled = true
                }
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (!state.isLoading && state.ayahs.isNotEmpty() && hasScrolled) {
            val ayahIndex = listState.firstVisibleItemIndex - 1
            if (ayahIndex in state.ayahs.indices) {
                delay(2000)
                val ayah = state.ayahs[ayahIndex]
                viewModel.saveLastReadPosition(suraNumber, ayah.aya)
            }
        }
    }

    val context = LocalContext.current

    // Action Bottom Sheet
    actionAyah?.let { ayah ->
        AyahActionsBottomSheet(
            ayah = ayah,
            suraNumber = suraNumber,
            suraName = suraName,
            isBookmarked = state.bookmarkedAyahs.contains(ayah.aya),
            translationText = viewModel.getTranslationText(ayah, state.translationLanguage),
            onToggleBookmark = { viewModel.toggleBookmark(suraNumber, ayah.aya, suraName) },
            onDismiss = { actionAyah = null }
        )
    }

    // Font Size Bottom Sheet
    if (showFontSizeSheet) {
        FontSizeBottomSheet(
            ayahFontSize = state.ayahFontSize,
            translationFontSize = state.translationFontSize,
            onAyahFontSizeChange = viewModel::setAyahFontSize,
            onTranslationFontSizeChange = viewModel::setTranslationFontSize,
            onDismiss = { showFontSizeSheet = false }
        )
    }

    // Jump to Ayah / Markers Bottom Sheet
    if (showJumpSheet) {
        JumpToAyahBottomSheet(
            totalVerses = state.ayahs.size,
            markers = viewModel.getNavMarkers(),
            onJumpToAyah = { targetAyah ->
                val index = state.ayahs.indexOfFirst { it.aya == targetAyah }
                if (index != -1) {
                    scope.launch { listState.scrollToItem(index + 1) }
                }
                showJumpSheet = false
            },
            onDismiss = { showJumpSheet = false }
        )
    }

    Scaffold(
        topBar = {
            Column {
                RafiqTopAppBar(
                    title = "$suraNumber. ${state.currentSurah?.nameSimple ?: suraName}",
                    subtitle = state.currentSurah?.let {
                        "${it.nameArabic} • ${stringResource(R.string.verses_count, it.versesCount)}"
                    },
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = {
                            showSearch = !showSearch
                            if (!showSearch) viewModel.clearSearchQuery()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search_in_surah)
                            )
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.jump)) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.List,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showJumpSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.font)) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showFontSizeSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (state.memorizationMode) stringResource(R.string.exit_memorization)
                                            else stringResource(R.string.memorize)
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleMemorizationMode()
                                    }
                                )
                            }
                        }
                    }
                )

                AnimatedVisibility(
                    visible = showSearch,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    RafiqSearchBar(
                        query = state.searchQuery,
                        onQueryChange = viewModel::setSearchQuery,
                        placeholder = stringResource(R.string.search_surah_hint),
                        modifier = Modifier.padding(
                            horizontal = RafiqTheme.spacing.l,
                            vertical = RafiqTheme.spacing.s
                        )
                    )
                }
            }
        },
        bottomBar = {
            when {
                state.currentPlayingAyah != null -> {
                    AudioPlayerBottomBar(
                        suraName = state.currentSurah?.nameSimple ?: suraName,
                        currentPlayingAyah = state.currentPlayingAyah ?: 1,
                        isPlaying = state.isPlaying,
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onPlayNext = viewModel::playNextAyah,
                        onPlayPrevious = viewModel::playPreviousAyah,
                        onSeekTo = viewModel::seekTo,
                        onStop = viewModel::stopAudio,
                        onClickAyah = {
                            val playingAyah = state.currentPlayingAyah
                            if (playingAyah != null) {
                                val index = state.ayahs.indexOfFirst { it.aya == playingAyah }
                                if (index != -1) {
                                    scope.launch { listState.animateScrollToItem(index + 1) }
                                }
                            }
                        }
                    )
                }
                state.memorizationMode -> {
                    MemorizationBottomBar(
                        onExit = viewModel::toggleMemorizationMode
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    RafiqLoadingIndicator()
                }
                state.error != null -> {
                    val err = state.error
                    if (err != null) {
                        RafiqErrorState(
                            error = err,
                            onRetry = { viewModel.loadAyahs(suraNumber) }
                        )
                    }
                }
                state.searchQuery.isNotBlank() && state.displayedAyahs.isEmpty() -> {
                    RafiqEmptyState(
                        title = stringResource(R.string.no_ayahs_match_query, state.searchQuery),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Surah Header Banner (shown when not filtering search)
                        if (state.searchQuery.isBlank()) {
                            item(key = "surah_header") {
                                SurahHeroHeaderCard(
                                    surah = state.currentSurah,
                                    suraNumber = suraNumber,
                                    suraName = suraName,
                                    modifier = Modifier.padding(RafiqTheme.spacing.l)
                                )
                            }
                        }

                        itemsIndexed(
                            items = state.displayedAyahs,
                            key = { _, ayah -> ayah.aya }
                        ) { _, ayah ->
                            val isPlayingAyah = state.currentPlayingAyah == ayah.aya && state.isPlaying
                            val isBookmarked = state.bookmarkedAyahs.contains(ayah.aya)
                            val isTafsirExpanded = expandedTafsirAyahs[ayah.aya] == true

                            VerseCard(
                                ayah = ayah,
                                suraNumber = suraNumber,
                                suraName = suraName,
                                translationLanguage = state.translationLanguage,
                                isBookmarked = isBookmarked,
                                isPlayingAyah = isPlayingAyah,
                                onToggleAudio = {
                                    requestNotificationPermission()
                                    viewModel.toggleAyahAudio(ayah.aya)
                                },
                                onToggleBookmark = {
                                    viewModel.toggleBookmark(suraNumber, ayah.aya, suraName)
                                },
                                onToggleTafsir = {
                                    val next = !isTafsirExpanded
                                    expandedTafsirAyahs[ayah.aya] = next
                                    if (next && state.tafsirCache[viewModel.tafsirCacheKey(ayah.aya)] == null) {
                                        viewModel.loadTafsir(ayah.aya)
                                    }
                                },
                                onOpenActions = { actionAyah = ayah },
                                memorizationMode = state.memorizationMode,
                                memorizationRevealed = state.memorizationRevealedAyah == ayah.aya,
                                onRevealTranslation = { viewModel.revealTranslation(ayah.aya) },
                                tafsirText = if (ayah.aya in state.tafsirErrors) "Failed to load tafsir"
                                    else state.tafsirCache[viewModel.tafsirCacheKey(ayah.aya)],
                                tafsirLoading = state.tafsirLoadingAyah == ayah.aya,
                                tafsirExpanded = isTafsirExpanded,
                                onRetryTafsir = { viewModel.loadTafsir(ayah.aya) },
                                ayahFontSize = state.ayahFontSize,
                                translationFontSize = state.translationFontSize,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = RafiqTheme.spacing.l,
                                        vertical = RafiqTheme.spacing.s
                                    )
                            )
                        }

                        // Footer Navigation at end of Surah
                        if (state.searchQuery.isBlank() && state.ayahs.isNotEmpty()) {
                            item(key = "surah_footer") {
                                SurahFooterNavigation(
                                    suraNumber = suraNumber,
                                    suraName = suraName,
                                    previousSurah = state.previousSurah,
                                    nextSurah = state.nextSurah,
                                    onNavigateSurah = onNavigateSurah,
                                    onBackToTop = {
                                        scope.launch { listState.animateScrollToItem(0) }
                                    },
                                    modifier = Modifier.padding(RafiqTheme.spacing.l)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ornate Hero Header card for Surah.
 */
@Composable
private fun SurahHeroHeaderCard(
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
                Text(
                    text = surah.nameArabic,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = ArabicFontFamily,
                        textDirection = TextDirection.Rtl
                    ),
                    color = RafiqTheme.extendedColors.goldAccent
                )
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
                    Text(
                        text = stringResource(R.string.bismillah_full),
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

/**
 * Individual Verse Card with clean metadata header, Arabic script, translation, and tafsir.
 */
@Composable
private fun VerseCard(
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
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
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

            // Action buttons
            IconButton(
                onClick = onToggleAudio,
                modifier = Modifier.size(36.dp)
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
                modifier = Modifier.size(36.dp)
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
                modifier = Modifier.size(36.dp)
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
                modifier = Modifier.size(36.dp)
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
        Text(
            text = ayah.text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = ayahFontSize.sp,
                fontFamily = ArabicFontFamily,
                textDirection = TextDirection.Rtl,
                lineHeight = (ayahFontSize * 1.85).sp,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

        // Translation section
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

/**
 * Sticky Bottom Audio Player Bar with full playback controls.
 */
@Composable
private fun AudioPlayerBottomBar(
    suraName: String,
    currentPlayingAyah: Int,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onStop: () -> Unit,
    onClickAyah: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = RafiqTheme.elevation.high,
        shadowElevation = RafiqTheme.elevation.default
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.s)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClickAyah)
                ) {
                    Text(
                        text = stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$suraName • Ayah $currentPlayingAyah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onPlayPrevious, modifier = Modifier.size(36.dp)) {
                    Text("⏮", fontSize = 18.sp)
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(22.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = onPlayNext, modifier = Modifier.size(36.dp)) {
                    Text("⏭", fontSize = 18.sp)
                }

                IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                    Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Progress Slider
            val duration = durationMs.coerceAtLeast(1L)
            var dragPosition by remember { mutableStateOf<Float?>(null) }
            Slider(
                value = ((dragPosition ?: positionMs.toFloat())).coerceIn(0f, duration.toFloat()),
                onValueChange = { dragPosition = it },
                onValueChangeFinished = {
                    dragPosition?.let { onSeekTo(it.toLong()) }
                    dragPosition = null
                },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDuration(positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Sticky Memorization bar when mode is active.
 */
@Composable
private fun MemorizationBottomBar(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = RafiqTheme.elevation.default
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.memorization_mode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.tap_to_reveal_translation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onExit) {
                Text(
                    text = stringResource(R.string.exit),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Footer at the end of the Surah allowing navigation to Previous Surah, Next Surah, or Back to Top.
 */
@Composable
private fun SurahFooterNavigation(
    suraNumber: Int,
    suraName: String,
    previousSurah: Surah?,
    nextSurah: Surah?,
    onNavigateSurah: ((Int, String) -> Unit)?,
    onBackToTop: () -> Unit,
    modifier: Modifier = Modifier
) {
    RafiqCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "— ${stringResource(R.string.end_of_surah, suraName)} —",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (previousSurah != null && onNavigateSurah != null) {
                    TextButton(onClick = { onNavigateSurah(previousSurah.chapterNumber, previousSurah.nameSimple) }) {
                        Text("← ${previousSurah.nameSimple}")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                TextButton(onClick = onBackToTop) {
                    Text(stringResource(R.string.back_to_top))
                }

                if (nextSurah != null && onNavigateSurah != null) {
                    TextButton(onClick = { onNavigateSurah(nextSurah.chapterNumber, nextSurah.nameSimple) }) {
                        Text("${nextSurah.nameSimple} →")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
    }
}

/**
 * Bottom Sheet for Ayah Actions (Copy Arabic, Copy Translation, Share, Bookmark).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyahActionsBottomSheet(
    ayah: Ayah,
    suraNumber: Int,
    suraName: String,
    isBookmarked: Boolean,
    translationText: String?,
    onToggleBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m)
        ) {
            Text(
                text = "$suraNumber:${ayah.aya} — $suraName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))

            Text(
                text = ayah.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = ArabicFontFamily,
                    fontSize = 20.sp,
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RafiqTheme.spacing.s)
            )

            if (!translationText.isNullOrBlank()) {
                Text(
                    text = translationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = RafiqTheme.spacing.m)
                )
            }

            RafiqDivider()
            Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))

            TextButton(
                onClick = {
                    copyToClipboard(context, "Ayah Text", ayah.text)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.copy_text))
            }

            if (!translationText.isNullOrBlank()) {
                TextButton(
                    onClick = {
                        copyToClipboard(context, "Ayah Translation", translationText)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.copy_translation))
                }
            }

            TextButton(
                onClick = {
                    shareAyah(context, suraNumber, ayah.aya, ayah.text, translationText)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.share_ayah))
            }

            TextButton(
                onClick = {
                    onToggleBookmark()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isBookmarked) stringResource(R.string.remove_bookmark)
                    else stringResource(R.string.add_bookmark)
                )
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))
        }
    }
}

/**
 * Bottom Sheet for Font Size adjustments with live preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontSizeBottomSheet(
    ayahFontSize: Int,
    translationFontSize: Int,
    onAyahFontSizeChange: (Int) -> Unit,
    onTranslationFontSizeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m)
        ) {
            Text(
                text = stringResource(R.string.font),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

            // Live Preview Card
            RafiqCard(
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                contentPadding = RafiqTheme.spacing.m,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.preview),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.bismillah_full),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = ArabicFontFamily,
                        fontSize = ayahFontSize.sp,
                        textDirection = TextDirection.Rtl,
                        lineHeight = (ayahFontSize * 1.8).sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.xs))
                Text(
                    text = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = translationFontSize.sp,
                        lineHeight = (translationFontSize * 1.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))

            // Arabic Font Size Slider
            Text(
                text = stringResource(R.string.ayah_font_size, ayahFontSize),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("14", fontSize = 12.sp)
                Slider(
                    value = ayahFontSize.toFloat(),
                    onValueChange = { onAyahFontSizeChange(it.toInt()) },
                    valueRange = 14f..40f,
                    steps = 25,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = RafiqTheme.spacing.s)
                )
                Text("40", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

            // Translation Font Size Slider
            Text(
                text = stringResource(R.string.translation_font_size, translationFontSize),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("10", fontSize = 12.sp)
                Slider(
                    value = translationFontSize.toFloat(),
                    onValueChange = { onTranslationFontSizeChange(it.toInt()) },
                    valueRange = 10f..28f,
                    steps = 17,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = RafiqTheme.spacing.s)
                )
                Text("28", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

/**
 * Bottom sheet to jump directly to any Ayah or Juz/Page markers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JumpToAyahBottomSheet(
    totalVerses: Int,
    markers: List<NavMarker>,
    onJumpToAyah: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAyah by remember(totalVerses) { mutableIntStateOf(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RafiqTheme.spacing.l, vertical = RafiqTheme.spacing.m)
        ) {
            Text(
                text = stringResource(R.string.jump_to_ayah),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

            if (totalVerses > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = selectedAyah.toFloat(),
                        onValueChange = { selectedAyah = it.toInt() },
                        valueRange = 1f..totalVerses.toFloat(),
                        steps = (totalVerses - 2).coerceAtLeast(0),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = RafiqTheme.spacing.s)
                    )
                    Text("$totalVerses", style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.ayah_number_label, selectedAyah),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))

                TextButton(
                    onClick = { onJumpToAyah(selectedAyah) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary, RafiqTheme.customShapes.small)
                ) {
                    Text(
                        text = stringResource(R.string.jump_to_ayah),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (markers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.l))
                Text(
                    text = stringResource(R.string.jump_to_marker),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(RafiqTheme.spacing.s))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(markers) { marker ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RafiqTheme.customShapes.small)
                                .clickable { onJumpToAyah(marker.ayahNumber) }
                                .padding(vertical = RafiqTheme.spacing.s, horizontal = RafiqTheme.spacing.m),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = marker.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            RafiqBadge(
                                text = "Ayah ${marker.ayahNumber}",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
}

private fun shareAyah(context: Context, suraNumber: Int, ayaNumber: Int, arabicText: String, translation: String?) {
    val text = buildString {
        appendLine(arabicText)
        if (!translation.isNullOrBlank()) {
            appendLine()
            append(translation)
        }
        appendLine()
        append("(QS $suraNumber:$ayaNumber) — ${context.getString(R.string.quran_via_rafiq)}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_ayah)))
}
