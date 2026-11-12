package com.aritxonly.bdfun.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.TextStyle
import com.aritxonly.bdfun.core.application.*

@Composable
internal fun PlainEmoteText(text: String, style: EmoteTextStyle, modifier: Modifier) {
    val color = style.color
    val fontSize = style.fontSize
    val lineHeight = style.lineHeight
    val fontWeight = style.fontWeight
    val maxLines = style.maxLines
    Text(
        text = text,
        color = color,
        style = TextStyle(
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
        ),
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
internal fun AnnotatedEmoteText(annotated: AnnotatedString, inline: Map<String, InlineTextContent>, style: EmoteTextStyle, modifier: Modifier) {
    val color = style.color
    val fontSize = style.fontSize
    val lineHeight = style.lineHeight
    val fontWeight = style.fontWeight
    val maxLines = style.maxLines
    Text(
        text = annotated,
        inlineContent = inline,
        color = color,
        style = TextStyle(
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
        ),
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
