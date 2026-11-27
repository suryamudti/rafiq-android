package com.smiledev.rafiq_quran.ui.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.R
import com.smiledev.rafiq_quran.domain.model.Ayah
import com.smiledev.rafiq_quran.theme.ArabicFontFamily
import com.smiledev.rafiq_quran.theme.RafiqTheme
import com.smiledev.rafiq_quran.ui.designsystem.badge.RafiqBadge
import com.smiledev.rafiq_quran.ui.designsystem.card.RafiqCard
import com.smiledev.rafiq_quran.ui.designsystem.divider.RafiqDivider

/**
 * Bottom Sheet for Ayah Actions (Copy Arabic, Copy Translation, Share, Bookmark).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AyahActionsBottomSheet(
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

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = ayah.text,
                    textAlign = TextAlign.Right,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = ArabicFontFamily,
                        fontSize = 20.sp,
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Right
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = RafiqTheme.spacing.s)
                )
            }

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
 * Bottom Sheet for Font Size & Display Settings adjustments with live preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FontSizeBottomSheet(
    ayahFontSize: Int,
    translationFontSize: Int,
    showTranslation: Boolean,
    onAyahFontSizeChange: (Int) -> Unit,
    onTranslationFontSizeChange: (Int) -> Unit,
    onToggleShowTranslation: (Boolean) -> Unit,
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

            // Show translation toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.show_translation),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = showTranslation,
                    onCheckedChange = onToggleShowTranslation
                )
            }

            Spacer(modifier = Modifier.height(RafiqTheme.spacing.m))

            // Live Preview Card
            RafiqCard(
                colors = CardDefaults.cardColors(
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
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = stringResource(R.string.bismillah_full),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = ArabicFontFamily,
                            fontSize = ayahFontSize.sp,
                            textDirection = TextDirection.Rtl,
                            lineHeight = (ayahFontSize * 1.8).sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showTranslation) {
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

            if (showTranslation) {
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
internal fun JumpToAyahBottomSheet(
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
