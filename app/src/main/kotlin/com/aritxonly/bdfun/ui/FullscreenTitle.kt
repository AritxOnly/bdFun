package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun ScrollingTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    val displayTitle = title.trim()
    BoxWithConstraints(
        modifier = modifier
            .height(46.dp)
            .clipToBounds(),
    ) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val style = TextStyle(
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
        )
        val widthPx = constraints.maxWidth
        val measured = remember(displayTitle, widthPx, density.density, density.fontScale) {
            textMeasurer.measure(
                text = AnnotatedString(displayTitle),
                style = style,
                maxLines = 1,
                softWrap = false,
            )
        }
        val textWidthPx = measured.size.width.toFloat()
        val fadePx = with(density) { 22.dp.toPx() }
        val gapPx = with(density) { 48.dp.toPx() }
        val overflow = textWidthPx > widthPx - fadePx * 0.5f

        if (!overflow) {
            Text(
                text = displayTitle,
                style = style,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            return@BoxWithConstraints
        }

        var offset by remember(title) { mutableFloatStateOf(0f) }
        var started by remember(title, widthPx) { mutableStateOf(false) }
        LaunchedEffect(title, textWidthPx, widthPx) {
            offset = 0f
            started = false
            delay(900L)
            started = true
            var startNanos = 0L
            while (true) {
                withFrameNanos { now ->
                    if (startNanos == 0L) startNanos = now
                    val total = textWidthPx + gapPx
                    val elapsedSec = (now - startNanos) / 1_000_000_000f
                    // 往返回键方向滚：负方向，循环时第二份文本刚好接上第一份
                    offset = -((elapsedSec * 42f * density.density) % total)
                }
            }
        }

        if (!started) {
            Text(
                text = displayTitle,
                style = style,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            return@BoxWithConstraints
        }

        val fade = Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val fadeAmount = ((-offset) / fadePx).coerceIn(0f, 1f)
                if (fadeAmount > 0.01f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = fadeAmount),
                            1f to Color.Transparent,
                        ),
                        size = Size(fadePx, size.height),
                        blendMode = BlendMode.DstOut,
                    )
                }
            }

        Box(modifier = Modifier.fillMaxSize().then(fade)) {
            Row(
                modifier = Modifier
                    .requiredWidth(with(density) { (textWidthPx * 2f + gapPx).toDp() })
                    .height(46.dp)
                    .graphicsLayer { translationX = offset },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayTitle,
                    style = style,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(with(density) { textWidthPx.toDp() }),
                )
                Spacer(Modifier.width(48.dp))
                Text(
                    text = displayTitle,
                    style = style,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(with(density) { textWidthPx.toDp() }),
                )
            }
        }
    }
}

