package com.aritxonly.bdfun.ui

import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_BOTTOM
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_SCROLL
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_TOP
import com.android.purebilibili.danmaku.engine.DanmakuItem
import com.android.purebilibili.danmaku.engine.DanmakuMaskFrame
import com.android.purebilibili.danmaku.engine.DanmakuRenderConfig
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.android.purebilibili.danmaku.engine.DanmakuWindow
import com.aritxonly.bdfun.data.DanmakuItem as BiliDanmakuItem
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * 弹幕层：整套使用 BiliPai 的 danmaku-engine（上游 bytedance/DanmakuRenderEngine）。
 *
 * 画面由原生 View 绘制，Compose 只负责三件事：
 * 1. 把 bdFun 的弹幕列表和显示设置映射成引擎配置；
 * 2. 用播放器进度驱动引擎时钟（播放 / 暂停 / 拖动进度）；
 * 3. 把原有的底色、透明度等外观设置继续套在外面。
 *
 * 轨道分配、追尾判定、文字测量、固定弹幕、遮罩等都由引擎负责，不再自己写一套。
 */
private const val BASE_TRAVEL_MS = 8_500f
private const val FIXED_SHOW_MS = 4_000L
private const val SYNC_INTERVAL_MS = 200L
private const val SEEK_THRESHOLD_MS = 700L

@Composable
fun DanmakuLayer(
    items: List<BiliDanmakuItem>,
    /** 读取当前播放进度。只在协程里读取，不参与重组。 */
    positionMs: () -> Long,
    playing: Boolean,
    playbackSpeed: Float,
    modifier: Modifier = Modifier,
    fullscreen: Boolean = false,
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val renderView = remember { DanmakuRenderView(context).apply { setRendererTouchable(false) } }
    DisposableEffect(renderView) {
        onDispose { renderView.releaseRenderer() }
    }
    val engine = renderView.engine

    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    // 读取设置（SettingsStore 内部是 Compose 状态，改了会触发这里重组）。
    val opacity = DanmakuSettings.opacity
    val area = DanmakuSettings.area.coerceIn(0.15f, 1f)
    val speed = DanmakuSettings.speed.coerceAtLeast(0.1f)
    val fontScale = if (fullscreen) DanmakuSettings.scaleFullscreen else DanmakuSettings.scaleInline
    val baseTextPx = with(density) { 12.sp.toPx() } * fontScale
    val strokePx = with(density) { DanmakuSettings.stroke.dp.toPx() }
    val portraitGuard = DanmakuSettings.portraitGuard
    val subtitleGuard = DanmakuSettings.subtitleGuard
    val dedupe = DanmakuSettings.dedupe
    val blockScroll = DanmakuSettings.blockScroll
    val blockFixed = DanmakuSettings.blockFixed
    val blockColor = DanmakuSettings.blockColor
    val blockAdvanced = DanmakuSettings.blockAdvanced
    val blockCount = DanmakuSettings.blockCount
    val keywordFilter = DanmakuSettings.keywordFilter
    val keywords = DanmakuSettings.keywords

    // 过滤规则保持和原设置面板一致；剩下的事交给引擎。
    val filtered = remember(
        items, dedupe, blockScroll, blockFixed, blockColor, blockAdvanced,
        blockCount, keywordFilter, keywords,
    ) {
        items.asSequence()
            .filter { it.text.isNotBlank() && it.mode in 1..5 }
            .filter { item ->
                when {
                    blockScroll && item.mode == 1 -> false
                    blockFixed && (item.mode == 4 || item.mode == 5) -> false
                    blockColor && item.color != 0xFFFFFF -> false
                    blockAdvanced && item.mode == 7 -> false
                    blockCount && item.mode == 8 -> false
                    keywordFilter && keywords.any { key ->
                        key.isNotBlank() && item.text.contains(key)
                    } -> false
                    else -> true
                }
            }
            .let { source ->
                if (dedupe) source.distinctBy { it.text } else source
            }
            .sortedBy { it.timeMs }
            .toList()
    }

    val engineItems = remember(filtered) {
        filtered.map { item ->
            DanmakuItem().apply {
                text = item.text
                showAtTime = item.timeMs.coerceAtLeast(0L)
                layerType = when (item.mode) {
                    4 -> DANMAKU_LAYER_BOTTOM
                    5 -> DANMAKU_LAYER_TOP
                    else -> DANMAKU_LAYER_SCROLL
                }
                textColor = 0xFF000000.toInt() or item.color
                textSizeScale = item.size.coerceIn(0.75f, 1.25f)
                isSelf = item.self
            }
        }
    }
    val window = remember(engineItems) {
        DanmakuWindow(anchorSegment = 0, segmentIndices = emptyList(), items = engineItems)
    }

    val typeface = remember { Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    val lineHeightPx = (baseTextPx * 1.9f).coerceAtLeast(1f)
    val heightPx = viewSize.height.toFloat()
    val lineCount = if (heightPx > 0f) {
        // 一页最多 9 行；行高加大后上下弹幕不会挤在一起。
        (((heightPx * area) / lineHeightPx).toInt()).coerceIn(1, 9)
    } else {
        1
    }
    val bottomMarginPx = with(density) {
        (64.dp + if (subtitleGuard) 22.dp else 0.dp).toPx()
    }
    val config = DanmakuRenderConfig(
        alpha = 255,
        textSizePx = baseTextPx,
        typeface = typeface,
        strokeWidthPx = strokePx,
        strokeColor = 0xE6000000.toInt(),
        // 弹幕速度设置 + 视频倍速：引擎自己按真实时间走，时长要一起缩放。
        scrollDurationMs = (BASE_TRAVEL_MS / (speed * playbackSpeed.coerceAtLeast(0.1f)))
            .toLong()
            .coerceAtLeast(1L),
        lineHeightPx = lineHeightPx,
        lineMarginPx = 0f,
        lineCount = lineCount,
        topMarginPx = 0f,
        bottomMarginPx = bottomMarginPx,
        pinnedDurationMs = FIXED_SHOW_MS,
        playSpeedPercent = 100,
        maskEnabled = portraitGuard,
    )

    // 人像防挡：用引擎的遮罩层把画面中间 30%~70% 擦掉，和原来的“避开中间”一致。
    val masks = remember(portraitGuard, viewSize) {
        if (!portraitGuard || viewSize.width <= 0 || viewSize.height <= 0) {
            emptyList()
        } else {
            val w = viewSize.width.toFloat()
            val h = viewSize.height.toFloat()
            val path = Path().apply {
                addRect(0f, h * 0.30f, w, h * 0.70f, Path.Direction.CW)
            }
            listOf(
                DanmakuMaskFrame(
                    startTimeMs = 0L,
                    endTimeMs = Long.MAX_VALUE,
                    path = path,
                    sourceWidth = viewSize.width,
                    sourceHeight = viewSize.height,
                ),
            )
        }
    }

    AndroidView(
        factory = { renderView },
        modifier = modifier.onSizeChanged { viewSize = it },
        update = { view -> view.alpha = opacity.coerceIn(0f, 1f) },
    )

    // 降低弹幕刷新率：默认开启（60 帧），关闭后交给屏幕刷新率。
    val lowRefresh = DanmakuSettings.lowRefresh
    LaunchedEffect(lowRefresh) {
        renderView.setMaxFrameRate(if (lowRefresh) 60 else 0)
    }

    LaunchedEffect(config) {
        engine.updateConfig(config)
    }

    LaunchedEffect(window) {
        engine.replaceWindow(window, positionMs().coerceAtLeast(0L))
        if (playing) {
            engine.start(positionMs().coerceAtLeast(0L))
        } else {
            engine.pause()
        }
    }

    LaunchedEffect(masks) {
        engine.replaceMaskFrames(masks, positionMs().coerceAtLeast(0L))
    }

    LaunchedEffect(playing) {
        if (playing) {
            engine.start(positionMs().coerceAtLeast(0L))
        } else {
            engine.pause()
        }
    }

    // 播放器进度是唯一的时钟来源：每 200ms 对齐一次，拖动进度条时做大跳转。
    // 这样长按倍速、蓝牙延迟、暂停恢复都不用再单独处理。
    val playingNow by rememberUpdatedState(playing)
    LaunchedEffect(engine) {
        var last = positionMs()
        while (true) {
            delay(SYNC_INTERVAL_MS)
            val current = positionMs()
            val jump = abs(current - last)
            if (playingNow) {
                if (jump > SEEK_THRESHOLD_MS) {
                    engine.seekTo(current.coerceAtLeast(0L))
                } else {
                    engine.synchronizeTo(current.coerceAtLeast(0L))
                }
            } else if (jump > SEEK_THRESHOLD_MS) {
                engine.seekTo(current.coerceAtLeast(0L))
            }
            last = current
        }
    }
}
