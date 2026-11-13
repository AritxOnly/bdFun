package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.application.*

internal fun AnnotatedString.Builder.appendRichEmote(runtime: RichTextRenderContext, inline: MutableMap<String, InlineTextContent>, hit: RichTextHit, key: String) {
    val fontSize = runtime.style.fontSize
    val token = hit.emoteToken.orEmpty()
    appendInlineContent(key, "[$token]")
    inline[key] = InlineTextContent(
        Placeholder(
            width = fontSize * 1.25f,
            height = fontSize * 1.25f,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
        ),
    ) { _ ->
        AsyncImage(
            model = hit.emoteUrl,
            contentDescription = token,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(1.dp),
        )
    }

}
