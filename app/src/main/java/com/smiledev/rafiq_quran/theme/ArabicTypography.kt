package com.smiledev.rafiq_quran.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.R

val ArabicFontFamily = FontFamily(Font(R.font.me_quran))

@Immutable
data class RafiqArabicTypography(
    val display: TextStyle = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 52.sp,
        textDirection = TextDirection.Rtl
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 44.sp,
        textDirection = TextDirection.Rtl
    ),
    val title: TextStyle = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 38.sp,
        textDirection = TextDirection.Rtl
    ),
    val body: TextStyle = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 32.sp,
        textDirection = TextDirection.Rtl
    ),
    val bismillah: TextStyle = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 38.sp,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Rtl
    )
)

val LocalRafiqArabicTypography = staticCompositionLocalOf { RafiqArabicTypography() }
