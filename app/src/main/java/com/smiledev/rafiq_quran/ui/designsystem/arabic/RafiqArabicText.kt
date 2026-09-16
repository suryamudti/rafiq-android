package com.smiledev.rafiq_quran.ui.designsystem.arabic

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.smiledev.rafiq_quran.theme.ArabicFontFamily
import com.smiledev.rafiq_quran.theme.RafiqTheme

@Composable
fun RafiqArabicText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.End,
    lineHeight: TextUnit = (fontSize.value * 1.7f).sp,
    style: TextStyle = TextStyle(
        fontFamily = ArabicFontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        textDirection = TextDirection.Rtl,
        textAlign = textAlign,
        color = color
    )
) {
    Text(
        text = text,
        style = style.copy(
            fontFamily = ArabicFontFamily,
            textDirection = TextDirection.Rtl,
            textAlign = textAlign,
            color = color
        ),
        modifier = modifier
    )
}
