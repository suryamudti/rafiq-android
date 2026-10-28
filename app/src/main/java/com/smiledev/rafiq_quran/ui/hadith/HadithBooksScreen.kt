package com.smiledev.rafiq_quran.ui.hadith

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.domain.model.HadithBook
import com.smiledev.rafiq_quran.domain.model.HadithTopic
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseInOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.EaseOutCubic
import com.smiledev.rafiq_quran.ui.designsystem.animation.RafiqMotion
import com.smiledev.rafiq_quran.ui.designsystem.animation.rafiqPressable
import com.smiledev.rafiq_quran.ui.designsystem.appbar.RafiqTopAppBar
import com.smiledev.rafiq_quran.ui.designsystem.button.RafiqIconButton
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqErrorState
import com.smiledev.rafiq_quran.ui.designsystem.state.RafiqLoadingIndicator
import com.smiledev.rafiq_quran.ui.designsystem.tab.RafiqSegmentedControl

private val arabicFont = FontFamily(Font(R.font.me_quran))

@Composable
private fun collectionLabel(collection: String): String =
    if (collection == "bukhari") stringResource(R.string.collection_sahih_bukhari) else stringResource(R.string.collection_sahih_muslim)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithBooksScreen(
    onHadithBookClick: (String) -> Unit,
    onSearch: () -> Unit = {},
    onBack: () -> Unit,
    viewModel: HadithBooksViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val resolvedLang = viewModel.resolvedLanguage()

    Scaffold(
        modifier = modifier,
        topBar = {
            RafiqTopAppBar(
                title = stringResource(R.string.hadiths),
                onBack = onBack,
                actions = {
                    RafiqIconButton(onClick = onSearch) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_hadiths),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val tabs = listOf(
                stringResource(R.string.hadith_tab_books),
                stringResource(R.string.hadith_tab_topics)
            )

            RafiqSegmentedControl(
                tabs = tabs,
                selectedIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                modifier = Modifier.padding(vertical = RafiqTheme.spacing.xs)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    state.isLoading && state.books.isEmpty() -> {
                        RafiqLoadingIndicator(modifier = Modifier.fillMaxSize())
                    }
                    state.error != null && state.books.isEmpty() -> {
                        RafiqErrorState(
                            error = state.error!!,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    selectedTabIndex == 0 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = RafiqTheme.spacing.m,
                                vertical = RafiqTheme.spacing.s
                            ),
                            horizontalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s),
                            verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.s)
                        ) {
                            items(state.books, key = { it.id }) { book ->
                                val localizedName = if (resolvedLang == "id") {
                                    book.nameId.ifBlank { book.nameEn }
                                } else {
                                    book.nameEn
                                }

                                RafiqCard(
                                    onClick = { onHadithBookClick(book.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RafiqTheme.customShapes.large,
                                    border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
                                    contentPadding = RafiqTheme.spacing.l
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = book.nameAr,
                                            fontFamily = arabicFont,
                                            fontSize = 22.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(RafiqTheme.spacing.xs))
                                        Text(
                                            text = localizedName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(RafiqTheme.spacing.xs))
                                        Surface(
                                            shape = RafiqTheme.customShapes.pill,
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = "${collectionLabel(book.collection)} · ${stringResource(R.string.hadith_book_number, book.number)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        HadithTopicsContent(
                            topics = state.topics,
                            books = state.books,
                            resolvedLang = resolvedLang,
                            onHadithBookClick = onHadithBookClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithTopicsContent(
    topics: List<HadithTopic> ,
    books: List<HadithBook>,
    resolvedLang: String,
    onHadithBookClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val booksMap = remember(books) { books.associateBy { it.id } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = RafiqTheme.spacing.l,
            vertical = RafiqTheme.spacing.s
        ),
        verticalArrangement = Arrangement.spacedBy(RafiqTheme.spacing.m)
    ) {
        items(topics, key = { it.id }) { topic ->
            var isExpanded by rememberSaveable(topic.id) { mutableStateOf(false) }
            val topicBooks = remember(topic.bookIds, booksMap) {
                topic.bookIds.mapNotNull { booksMap[it] }
            }
            val topicTitle = if (resolvedLang == "id") topic.nameId else topic.nameEn
            val interactionSource = remember { MutableInteractionSource() }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .rafiqPressable(
                        interactionSource = interactionSource,
                        targetScale = RafiqMotion.CardPressScale
                    ),
                shape = RafiqTheme.customShapes.large,
                border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = RafiqTheme.elevation.low)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { isExpanded = !isExpanded }
                            )
                            .padding(RafiqTheme.spacing.l),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topicTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(RafiqTheme.spacing.xs))
                            Box(
                                modifier = Modifier
                                    .clip(RafiqTheme.customShapes.pill)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.hadith_topic_books_count, topicBooks.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(RafiqTheme.spacing.m))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = topic.nameAr,
                                fontFamily = arabicFont,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End
                            )
                            Spacer(Modifier.height(RafiqTheme.spacing.xxs))
                            Text(
                                text = if (isExpanded) "▲" else "▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn(animationSpec = tween(RafiqMotion.DurationNormal, easing = EaseOutCubic)) +
                                expandVertically(animationSpec = tween(RafiqMotion.DurationNormal, easing = EaseOutCubic)),
                        exit = fadeOut(animationSpec = tween(RafiqMotion.DurationFast, easing = EaseInOutCubic)) +
                                shrinkVertically(animationSpec = tween(RafiqMotion.DurationFast, easing = EaseInOutCubic))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = RafiqTheme.spacing.l,
                                    end = RafiqTheme.spacing.l,
                                    bottom = RafiqTheme.spacing.l
                                )
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.padding(bottom = RafiqTheme.spacing.m),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            topicBooks.forEach { book ->
                                val localizedBookName = if (resolvedLang == "id") {
                                    book.nameId.ifBlank { book.nameEn }
                                } else {
                                    book.nameEn
                                }
                                RafiqCard(
                                    onClick = { onHadithBookClick(book.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = RafiqTheme.spacing.xxs),
                                    shape = RafiqTheme.customShapes.medium,
                                    border = BorderStroke(1.dp, RafiqTheme.extendedColors.cardBorder),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    contentPadding = RafiqTheme.spacing.m
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = localizedBookName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${collectionLabel(book.collection)} · ${stringResource(R.string.hadith_book_number, book.number)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.width(RafiqTheme.spacing.m))
                                        Text(
                                            text = book.nameAr,
                                            fontFamily = arabicFont,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.End
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
}
