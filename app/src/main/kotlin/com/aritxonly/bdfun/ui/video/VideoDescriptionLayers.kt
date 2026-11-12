package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

internal data class VideoDescriptionLayersContent(
    val fullPx: Float, val safeTopPx: Float, val progress: Float, val tailText: String,
    val annotatedText: androidx.compose.ui.text.AnnotatedString,
    val style: androidx.compose.ui.text.TextStyle, val backgroundColor: Color,
    val onExpandableChange: () -> Unit,
)

@Composable
internal fun VideoDescriptionTail(
    content: VideoDescriptionLayersContent, linkifyTail: (String) -> androidx.compose.ui.text.AnnotatedString,
) {
    val density = LocalDensity.current
    val fullPx = content.fullPx
    val safeTopPx = content.safeTopPx
    val p = content.progress
    val tailText = content.tailText
    val annotatedText = content.annotatedText
    val style = content.style
    val backgroundColor = content.backgroundColor
    val onExpandableChange = content.onExpandableChange
    if (tailText.isNotEmpty()) {
        Box(
            modifier = Modifier
                .offset(y = with(density) { safeTopPx.toDp() })
                .fillMaxWidth()
                .height(with(density) { (fullPx - safeTopPx).coerceAtLeast(0f).toDp() }),
        ) {
            Text(
                text = linkifyTail(tailText),
                style = style,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier
                    .graphicsLayer { alpha = p }
                    .blur(((1f - p).coerceIn(0f, 1f) * 9f).dp),
            )
        }
    }

}

@Composable
internal fun VideoDescriptionFirstLines(content: VideoDescriptionLayersContent) {
    val density = LocalDensity.current
    val fullPx = content.fullPx
    val safeTopPx = content.safeTopPx
    val p = content.progress
    val tailText = content.tailText
    val annotatedText = content.annotatedText
    val style = content.style
    val backgroundColor = content.backgroundColor
    val onExpandableChange = content.onExpandableChange
    // 遮住前两行区域的模糊溢出
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(density) { safeTopPx.toDp() })
            .background(backgroundColor)
            .zIndex(0.5f),
    )
    // 折叠态：两行 + 省略号（随进度淡出）
    Text(
        text = annotatedText,
        style = style,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout ->
            if (layout.hasVisualOverflow) onExpandableChange()
        },
        modifier = Modifier
            .graphicsLayer { alpha = 1f - p }
            .zIndex(1f),
    )
    // 原有的前两行：展开后保持清晰，不参与模糊
    Text(
        text = annotatedText,
        style = style,
        maxLines = 2,
        overflow = TextOverflow.Clip,
        modifier = Modifier
            .graphicsLayer { alpha = p }
            .zIndex(1f),
    )

}
