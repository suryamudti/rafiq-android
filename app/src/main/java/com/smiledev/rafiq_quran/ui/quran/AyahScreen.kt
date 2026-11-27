package com.smiledev.rafiq_quran.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.domain.model.Surah
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.common.rememberNotificationPermissionRequester
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
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
            showTranslation = state.showTranslation,
            onAyahFontSizeChange = viewModel::setAyahFontSize,
            onTranslationFontSizeChange = viewModel::setTranslationFontSize,
            onToggleShowTranslation = viewModel::setShowTranslation,
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
                                            if (state.showTranslation) stringResource(R.string.hide_translation)
                                            else stringResource(R.string.show_translation)
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleShowTranslation()
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
                                showTranslation = state.showTranslation,
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
