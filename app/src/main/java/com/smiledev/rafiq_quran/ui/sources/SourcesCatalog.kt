package com.smiledev.rafiq_quran.ui.sources

import com.smiledev.rafiq_quran.R

internal data class SourceItem(
    val titleRes: Int,
    val descriptionRes: Int,
    val authenticityRes: Int,
    val translatorRes: Int? = null,
    val linkUrl: String? = null,
)

internal data class SourceSection(
    val titleRes: Int,
    val items: List<SourceItem>,
)

internal fun sourcesSections(): List<SourceSection> = listOf(
    SourceSection(
        titleRes = R.string.source_section_quran,
        items = listOf(
            SourceItem(
                titleRes = R.string.source_quran_uthmani,
                descriptionRes = R.string.source_quran_uthmani_desc,
                authenticityRes = R.string.source_authentic_text,
                linkUrl = "https://quran.com"
            ),
            SourceItem(
                titleRes = R.string.source_quran_en,
                descriptionRes = R.string.source_quran_en_desc,
                authenticityRes = R.string.source_recognized_translation,
                translatorRes = R.string.source_translator_saheeh,
                linkUrl = "https://quran.com"
            ),
            SourceItem(
                titleRes = R.string.source_quran_id,
                descriptionRes = R.string.source_quran_id_desc,
                authenticityRes = R.string.source_recognized_translation,
                translatorRes = R.string.source_translator_kemenag,
                linkUrl = "https://quran.kemenag.go.id"
            )
        )
    ),
    SourceSection(
        titleRes = R.string.source_section_hadith,
        items = listOf(
            SourceItem(
                titleRes = R.string.source_hadith_bukhari,
                descriptionRes = R.string.source_hadith_bukhari_desc,
                authenticityRes = R.string.source_authentic_collection,
                translatorRes = R.string.source_translator_muhsin_khan,
                linkUrl = "https://sunnah.com/bukhari"
            ),
            SourceItem(
                titleRes = R.string.source_hadith_muslim,
                descriptionRes = R.string.source_hadith_muslim_desc,
                authenticityRes = R.string.source_authentic_collection,
                translatorRes = R.string.source_translator_siddiqui,
                linkUrl = "https://sunnah.com/muslim"
            )
        )
    ),
    SourceSection(
        titleRes = R.string.source_section_prayer_times,
        items = listOf(
            SourceItem(
                titleRes = R.string.source_prayer_aladhan,
                descriptionRes = R.string.source_prayer_aladhan_desc,
                authenticityRes = R.string.source_kemenag_method,
                linkUrl = "https://aladhan.com"
            ),
            SourceItem(
                titleRes = R.string.source_prayer_location,
                descriptionRes = R.string.source_prayer_location_desc,
                authenticityRes = R.string.source_default_jakarta
            )
        )
    )
)
