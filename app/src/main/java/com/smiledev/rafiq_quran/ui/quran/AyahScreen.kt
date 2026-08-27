package com.smiledev.rafiq_quran.ui.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.core.currentLocaleCode
import com.smiledev.rafiq_quran.core.displayMessage
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.ui.common.formatDuration
import com.smiledev.rafiq_quran.ui.common.rememberNotificationPermissionRequester
import com.smiledev.rafiq_quran.ui.quran.AyahViewModel
import com.smiledev.rafiq_quran.ui.quran.NavMarker
import kotlinx.coroutines.launch

private val arabicFont = FontFamily(Font(R.font.me_quran))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahScreen(
    suraNumber: Int,
    suraName: String,
    scrollToAya: Int = 0,
    onBack: () -> Unit,
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
                    val targetIndex = if (state.currentSurah != null) ayahIndex + 1 else ayahIndex
                    listState.scrollToItem(targetIndex)
                    hasScrolled = true
                }
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (!state.isLoading && state.ayahs.isNotEmpty() && hasScrolled) {
            val offset = if (state.currentSurah != null) 1 else 0
            val ayahIndex = listState.firstVisibleItemIndex - offset
            if (ayahIndex in state.ayahs.indices) {
                kotlinx.coroutines.delay(2000)
                val ayah = state.ayahs[ayahIndex]
                viewModel.saveLastReadPosition(suraNumber, ayah.aya)
            }
        }
    }

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    actionAyah?.let { ayah ->
        val isBookmarked = state.bookmarkedAyahs.contains(ayah.aya)
        val translationText = viewModel.getTranslationText(ayah, state.translationLanguage)

        ModalBottomSheet(
            onDismissRequest = { actionAyah = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "$suraNumber:${ayah.aya} - $suraName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = ayah.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = arabicFont,
                        fontSize = 20.sp,
                        textDirection = TextDirection.Rtl
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                if (translationText != null) {
                    Text(
                        text = translationText,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }

                TextButton(
                    onClick = {
                        copyToClipboard(context, "Ayah Text", ayah.text)
                        actionAyah = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy Text")
                }
                TextButton(
                    onClick = {
                        if (translationText != null) {
                            copyToClipboard(context, "Ayah Translation", translationText)
                        }
                        actionAyah = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = translationText != null
                ) {
                    Text("Copy Translation")
                }
                TextButton(
                    onClick = {
                        shareAyah(context, suraNumber, ayah.aya, ayah.text, translationText)
                        actionAyah = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share")
                }
                TextButton(
                    onClick = {
                        viewModel.toggleBookmark(suraNumber, ayah.aya, suraName)
                        actionAyah = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isBookmarked) "Remove Bookmark" else "Add Bookmark")
                }
                TextButton(
                    onClick = { actionAyah = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showFontSizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFontSizeSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.font_size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = stringResource(R.string.ayah_font_size, state.ayahFontSize),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("12", fontSize = 12.sp, fontWeight = FontWeight.Light)
                    Slider(
                        value = state.ayahFontSize.toFloat(),
                        onValueChange = { viewModel.setAyahFontSize(it.toInt()) },
                        valueRange = 12f..40f,
                        steps = 27,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("40", fontSize = 12.sp, fontWeight = FontWeight.Light)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.translation_font_size, state.translationFontSize),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("10", fontSize = 12.sp, fontWeight = FontWeight.Light)
                    Slider(
                        value = state.translationFontSize.toFloat(),
                        onValueChange = { viewModel.setTranslationFontSize(it.toInt()) },
                        valueRange = 10f..30f,
                        steps = 19,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("30", fontSize = 12.sp, fontWeight = FontWeight.Light)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { showFontSizeSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showJumpSheet) {
        ModalBottomSheet(
            onDismissRequest = { showJumpSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            LazyColumn {
                items(viewModel.getNavMarkers()) { marker: NavMarker ->
                    TextButton(
                        onClick = {
                            val offset = if (state.currentSurah != null) 1 else 0
                            val index = state.ayahs.indexOfFirst { it.aya == marker.ayahNumber }
                            if (index != -1) {
                                scope.launch { listState.scrollToItem(index + offset) }
                            }
                            showJumpSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Text("${marker.label} — Ayah ${marker.ayahNumber}")
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$suraNumber. $suraName") },
                navigationIcon = {
                    Text("Back", modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Jump") },
                                onClick = {
                                    showOverflowMenu = false
                                    showJumpSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Font") },
                                onClick = {
                                    showOverflowMenu = false
                                    showFontSizeSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (state.memorizationMode) "Exit Memorization" else "Memorize")
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleMemorizationMode()
                                }
                            )
                        }
                    }
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
                                onLongPress = { actionAyah = ayah },
                                memorizationMode = state.memorizationMode,
                                memorizationRevealedAyah = state.memorizationRevealedAyah,
                                onRevealTranslation = { viewModel.revealTranslation(it) },
                                isPlayingAyah = state.currentPlayingAyah == ayah.aya && state.isPlaying,
                                onToggleAudio = {
                                    requestNotificationPermission()
                                    viewModel.toggleAyahAudio(ayah.aya)
                                },
                                positionMs = if (state.currentPlayingAyah == ayah.aya) state.positionMs else 0L,
                                durationMs = if (state.currentPlayingAyah == ayah.aya) state.durationMs else 0L,
                                onSeekTo = viewModel::seekTo,
                                tafsirText = if (ayah.aya in state.tafsirErrors) "Failed to load tafsir" else state.tafsirCache[viewModel.tafsirCacheKey(ayah.aya)],
                                tafsirLoading = state.tafsirLoadingAyah == ayah.aya,
                                onLoadTafsir = { viewModel.loadTafsir(ayah.aya) },
                                ayahFontSize = state.ayahFontSize,
                                translationFontSize = state.translationFontSize
                            )
                        }
                    }
            }
        }
    }

        if (state.memorizationMode) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Memorization Mode", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    TextButton(onClick = { viewModel.toggleMemorizationMode() }) {
                        Text("Exit", color = MaterialTheme.colorScheme.error)
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
    memorizationMode: Boolean = false,
    memorizationRevealedAyah: Int? = null,
    onRevealTranslation: ((Int) -> Unit)? = null,
    isPlayingAyah: Boolean = false,
    onToggleAudio: (() -> Unit)? = null,
    positionMs: Long = 0L,
    durationMs: Long = 0L,
    onSeekTo: ((Long) -> Unit)? = null,
    tafsirText: String? = null,
    tafsirLoading: Boolean = false,
    onLoadTafsir: (() -> Unit)? = null,
    ayahFontSize: Int = 22,
    translationFontSize: Int = 15
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .then(
            if (isPlayingAyah) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            else Modifier
        )
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
                IconButton(
                    onClick = { onToggleAudio?.invoke() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = if (isPlayingAyah) "Playing" else "Play",
                        tint = if (isPlayingAyah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
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

        if (isPlayingAyah && onSeekTo != null) {
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDuration(positionMs),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (memorizationMode && memorizationRevealedAyah != ayah.aya) {
            TextButton(
                onClick = { onRevealTranslation?.invoke(ayah.aya) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) {
                Text(
                    text = "Tap to reveal translation",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        } else {
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
                            modifier = if (memorizationRevealedAyah == ayah.aya) {
                                Modifier.fillMaxWidth().padding(top = 6.dp)
                                    .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp)).padding(8.dp)
                            } else {
                                Modifier.fillMaxWidth().padding(top = 6.dp)
                            }
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
                            modifier = if (memorizationRevealedAyah == ayah.aya) {
                                Modifier.fillMaxWidth().padding(top = 6.dp)
                                    .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp)).padding(8.dp)
                            } else {
                                Modifier.fillMaxWidth().padding(top = 6.dp)
                            }
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
                        modifier = if (memorizationRevealedAyah == ayah.aya) {
                            Modifier.fillMaxWidth().padding(top = 6.dp)
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp)).padding(8.dp)
                        } else {
                            Modifier.fillMaxWidth().padding(top = 6.dp)
                        },
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
        }

        var showTafsir by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    showTafsir = !showTafsir
                    if (showTafsir && tafsirText == null && !tafsirLoading) {
                        onLoadTafsir?.invoke()
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (showTafsir) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (showTafsir) "Hide Tafsir" else "Show Tafsir",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
        if (showTafsir) {
            when {
                tafsirLoading -> {
                    Text(
                        text = "Loading tafsir...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
                tafsirText != null -> {
                    Text(
                        text = tafsirText!!,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
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

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun shareAyah(context: Context, suraNumber: Int, ayaNumber: Int, arabicText: String, translation: String?) {
    val text = buildString {
        appendLine(arabicText)
        if (!translation.isNullOrBlank()) {
            appendLine()
            append(translation)
        }
        appendLine()
        append("$suraNumber:$ayaNumber — Quran via Rafiq")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Ayah"))
}
