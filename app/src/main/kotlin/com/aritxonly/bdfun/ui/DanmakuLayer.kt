package com.aritxonly.bdfun.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.DanmakuItem

private const val TRAVEL_MS = 8500L

/** 同一条轨道上两条弹幕的最小时间间隔，避免叠字。 */
private const val MIN_GAP_MS = 2600L

@Immutable
private data class DanmakuTrack(
    val timeMs: Long,
    val text: String,
    val color: Int,
    val scale: Float,
    val lane: Int,
    val self: Boolean,
)

@Immutable
private data class FixedItem(
    val timeMs: Long,
    val text: String,
    val color: Int,
    val size: Float,
    val mode: Int,
    val self: Boolean,
)

/**
 * 弹幕轨道分配：按时间排序贪心占轨，同一条轨道上两条弹幕至少间隔 [MIN_GAP_MS]。
 * 轨道数由「显示范围」决定（1 行 ~ 全屏）。
 */
private fun buildTracks(
    items: List<DanmakuItem>,
    laneCount: Int,
    dedupe: Boolean,
): List<DanmakuTrack> {
    val lanes = laneCount.coerceIn(1, 24)
    val laneFreeAt = LongArray(lanes) { Long.MIN_VALUE }
    val out = ArrayList<DanmakuTrack>(items.size)
    val sorted = items.asSequence()
        .filter { it.text.isNotBlank() && it.mode == 1 }
        .filter { item ->
            when {
                DanmakuSettings.blockScroll -> false
                DanmakuSettings.blockColor && item.color != 0xFFFFFF -> false
                DanmakuSettings.blockAdvanced && item.mode == 7 -> false
                DanmakuSettings.blockCount && item.mode == 8 -> false
                DanmakuSettings.keywordFilter &&
                    DanmakuSettings.keywords.any { key -> key.isNotBlank() && item.text.contains(key) } -> false
                else -> true
            }
        }
        .sortedBy { it.timeMs }
    val source = if (dedupe) sorted.distinctBy { it.text }.asSequence() else sorted
    source.forEach { item ->
        val lane = laneFreeAt.indexOfFirst { it <= item.timeMs }
        if (lane < 0) return@forEach
        laneFreeAt[lane] = item.timeMs + MIN_GAP_MS
        out += DanmakuTrack(
            timeMs = item.timeMs,
            text = item.text,
            color = 0xFF000000.toInt() or item.color,
            scale = item.size.coerceIn(0.75f, 1.25f),
            lane = lane,
            self = item.self,
        )
    }
    return out
}

private fun buildFixed(items: List<DanmakuItem>): List<FixedItem> {
    if (DanmakuSettings.blockFixed) return emptyList()
    return items.asSequence()
        .filter { it.text.isNotBlank() && (it.mode == 4 || it.mode == 5) }
        .filter { item ->
            !(DanmakuSettings.blockColor && item.color != 0xFFFFFF) &&
                !(DanmakuSettings.keywordFilter &&
                    DanmakuSettings.keywords.any { key ->
                        key.isNotBlank() && item.text.contains(key)
                    })
        }
        .map { item ->
            FixedItem(
                timeMs = item.timeMs,
                text = item.text,
                color = 0xFF000000.toInt() or item.color,
                size = item.size,
                mode = item.mode,
                self = item.self,
            )
        }
        .sortedBy { it.timeMs }
        .toList()
}

/** 二分查找首个 timeMs >= target 的下标。 */
private fun lowerBound(tracks: List<DanmakuTrack>, target: Long): Int {
    var low = 0
    var high = tracks.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (tracks[mid].timeMs < target) low = mid + 1 else high = mid
    }
    return low
}

/**
 * 弹幕层：单块 Canvas + 原生 Paint 绘制。
 *
 * 之前每条弹幕都是一个 Compose Text，进度每变一次整层重组、重新布局几十个文本；
 * 现在进度只在 draw 阶段读取，滚动只重绘、不重组；文本宽度按基础字号缓存，
 * 描边用 Paint.STROKE 先画黑边再填色，比阴影更省也更清楚。
 */
@Composable
fun DanmakuLayer(
    items: List<DanmakuItem>,
    /** 读取当前播放进度。只在 draw 阶段调用，避免每帧重组。 */
    positionMs: () -> Long,
    modifier: Modifier = Modifier,
    fullscreen: Boolean = false,
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val lanePx = with(density) { 30.dp.toPx() }
    val rawArea = DanmakuSettings.area.coerceIn(0.15f, 1f)
    val area = if (DanmakuSettings.subtitleGuard) rawArea * 0.82f else rawArea
    val fontScale = if (fullscreen) {
        DanmakuSettings.scaleFullscreen
    } else {
        DanmakuSettings.scaleInline
    }
    val baseTextPx = with(density) { 12.sp.toPx() } * fontScale
    val strokePx = with(density) { DanmakuSettings.stroke.dp.toPx() }
    val portraitGuard = DanmakuSettings.portraitGuard
    val speed = DanmakuSettings.speed.coerceAtLeast(0.1f)

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer { alpha = DanmakuSettings.opacity },
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val laneCount = ((heightPx * area) / lanePx).toInt().coerceIn(1, 24)
        val tracks = remember(
            items,
            laneCount,
            DanmakuSettings.blockScroll,
            DanmakuSettings.blockColor,
            DanmakuSettings.blockAdvanced,
            DanmakuSettings.blockCount,
            DanmakuSettings.keywordFilter,
            DanmakuSettings.keywords,
            DanmakuSettings.dedupe,
        ) {
            buildTracks(items, laneCount, DanmakuSettings.dedupe)
        }
        val fixed = remember(
            items,
            DanmakuSettings.blockFixed,
            DanmakuSettings.blockColor,
            DanmakuSettings.keywordFilter,
            DanmakuSettings.keywords,
        ) {
            buildFixed(items)
        }
        val paint = remember {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                isSubpixelText = true
            }
        }
        val strokePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
        val bgPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
        // 文本宽度按基础字号量一次；实际宽度 = 基础宽度 × 该条弹幕的字号倍率。
        val baseWidths = remember(tracks, baseTextPx) {
            FloatArray(tracks.size) { index ->
                paint.textSize = baseTextPx * tracks[index].scale
                paint.measureText(tracks[index].text)
            }
        }
        val travel = (TRAVEL_MS / speed).toLong()

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (widthPx <= 0f || heightPx <= 0f) return@Canvas
            val currentMs = positionMs()

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas

                // 滚动弹幕
                val start = lowerBound(tracks, currentMs - travel)
                val end = lowerBound(tracks, currentMs + 1)
                for (index in start until end) {
                    val track = tracks[index]
                    val elapsed = currentMs - track.timeMs
                    if (elapsed < 0 || elapsed > travel) continue
                    val textSize = baseTextPx * track.scale
                    val y = track.lane * lanePx + textSize
                    if (portraitGuard && isInPortraitBand(y, heightPx)) continue
                    val progress = elapsed.toFloat() / travel
                    val textWidth = baseWidths[index]
                    val x = widthPx - progress * (widthPx + textWidth)
                    if (x > widthPx || x + textWidth < 0f) continue
                    if (track.self) {
                        bgPaint.color = 0x99FB7299.toInt()
                        native.drawRoundRect(
                            x - 8f,
                            y - textSize - 4f,
                            x + textWidth + 8f,
                            y + 6f,
                            8f,
                            8f,
                            bgPaint,
                        )
                    }
                    paint.textSize = textSize
                    paint.color = track.color
                    drawTextWithStroke(native, track.text, x, y, paint, strokePaint, strokePx)
                }

                // 顶部固定弹幕
                val topVisible = fixed.asSequence()
                    .filter { it.mode == 5 && currentMs >= it.timeMs && currentMs <= it.timeMs + 4_000L }
                    .take(3)
                    .toList()
                var topY = 18.dp.toPx() + baseTextPx
                topVisible.forEach { item ->
                    val textSize = fixedTextPx(item, baseTextPx)
                    paint.textSize = textSize
                    val textWidth = paint.measureText(item.text)
                    val x = (widthPx - textWidth) / 2f
                    if (item.self) {
                        bgPaint.color = 0x99FB7299.toInt()
                        native.drawRoundRect(
                            x - 8f,
                            topY - textSize - 4f,
                            x + textWidth + 8f,
                            topY + 6f,
                            8f,
                            8f,
                            bgPaint,
                        )
                    }
                    paint.color = item.color
                    drawTextWithStroke(native, item.text, x, topY, paint, strokePaint, strokePx)
                    topY += textSize + 8.dp.toPx()
                }

                // 底部固定弹幕
                val bottomVisible = fixed.asSequence()
                    .filter { it.mode == 4 && currentMs >= it.timeMs && currentMs <= it.timeMs + 4_000L }
                    .take(3)
                    .toList()
                var bottomY = heightPx - 76.dp.toPx()
                bottomVisible.asReversed().forEach { item ->
                    val textSize = fixedTextPx(item, baseTextPx)
                    paint.textSize = textSize
                    val textWidth = paint.measureText(item.text)
                    val x = (widthPx - textWidth) / 2f
                    if (item.self) {
                        bgPaint.color = 0x99FB7299.toInt()
                        native.drawRoundRect(
                            x - 8f,
                            bottomY - textSize - 4f,
                            x + textWidth + 8f,
                            bottomY + 6f,
                            8f,
                            8f,
                            bgPaint,
                        )
                    }
                    paint.color = item.color
                    drawTextWithStroke(native, item.text, x, bottomY, paint, strokePaint, strokePx)
                    bottomY -= textSize + 8.dp.toPx()
                }
            }
        }
    }
}

private fun fixedTextPx(item: FixedItem, baseTextPx: Float): Float =
    (baseTextPx * (item.size / 25f)).coerceIn(baseTextPx * 0.75f, baseTextPx * 2.5f)

private fun isInPortraitBand(y: Float, heightPx: Float): Boolean =
    y > heightPx * 0.30f && y < heightPx * 0.70f

private fun drawTextWithStroke(
    canvas: android.graphics.Canvas,
    text: String,
    x: Float,
    y: Float,
    fill: Paint,
    stroke: Paint,
    strokeWidth: Float,
) {
    if (strokeWidth > 0.05f) {
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = strokeWidth
        stroke.strokeJoin = Paint.Join.ROUND
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.typeface = fill.typeface
        stroke.textSize = fill.textSize
        stroke.isAntiAlias = true
        stroke.color = 0xE6000000.toInt()
        canvas.drawText(text, x, y, stroke)
    }
    fill.style = Paint.Style.FILL
    canvas.drawText(text, x, y, fill)
    // 留在 FILL，下一次循环开头再改 textSize/color
    fill.style = Paint.Style.FILL
}
