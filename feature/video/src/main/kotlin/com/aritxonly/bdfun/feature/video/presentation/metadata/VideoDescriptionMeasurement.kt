package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size

internal data class VideoDescriptionMeasurement(val fullPx: Float, val collapsedPx: Float, val tailText: String)

internal fun measureVideoDescription(
    measurer: androidx.compose.ui.text.TextMeasurer,
    annotatedText: androidx.compose.ui.text.AnnotatedString, text: String,
    style: androidx.compose.ui.text.TextStyle, widthPx: Int,
): VideoDescriptionMeasurement {
    val measured = measurer.measure(
        text = annotatedText,
        style = style,
        maxLines = Int.MAX_VALUE,
        constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
    )
    val fullPx = measured.size.height.toFloat()
    // 两行的真实高度与第二行结束位置：模糊只作用在这之后的部分
    val twoLineLayout = measurer.measure(
        text = annotatedText,
        style = style,
        maxLines = 2,
        constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
    )
    val twoLinePx = twoLineLayout.size.height.toFloat()
    val collapsedPx = minOf(twoLinePx, fullPx)
    val tailText = if (twoLineLayout.lineCount >= 2) {
        val end = runCatching { twoLineLayout.getLineEnd(1, visibleEnd = false) }
            .getOrDefault(text.length)
            .coerceIn(0, text.length)
        text.substring(end)
    } else {
        ""
    }
    return VideoDescriptionMeasurement(fullPx, collapsedPx, tailText)
}
