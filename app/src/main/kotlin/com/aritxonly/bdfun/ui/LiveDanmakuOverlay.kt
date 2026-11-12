package com.aritxonly.bdfun.ui

import android.graphics.Typeface
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_SCROLL
import com.android.purebilibili.danmaku.engine.DanmakuItem
import com.android.purebilibili.danmaku.engine.DanmakuRenderConfig
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.aritxonly.bdfun.data.LiveChatKind
import com.aritxonly.bdfun.data.LiveChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * 直播滚动弹幕层。
 *
 * 参考 BiliPai 的 LiveDanmakuOverlay：同样使用 `danmaku-engine` 的
 * `DanmakuRenderView + DanmakuEngine`，由引擎负责轨道分配、追尾判断和文字测量。
 * 直播没有时间轴，所以这里不替换 `DanmakuWindow`，而是把新消息用
 * `addImmediate` 直接送进引擎；暂停时暂停引擎时钟。
 */
private const val LIVE_DM_BASE_TRAVEL_MS = 8_500f
private const val LIVE_DM_POLL_MS = 100L

@Composable
fun LiveDanmakuOverlay(
    messages: List<LiveChatMessage>,
    playing: Boolean,
    enabled: Boolean,
    fullscreen: Boolean,
    roomEmotes: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val renderView = remember {
        DanmakuRenderView(context).apply { setRendererTouchable(false) }
    }
    DisposableEffect(renderView) {
        onDispose { renderView.releaseRenderer() }
    }
    val engine = renderView.engine

    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var lastSeenId by remember { mutableLongStateOf(messages.lastOrNull()?.id ?: 0L) }

    val opacity = DanmakuSettings.opacity.coerceIn(0f, 1f)
    val area = DanmakuSettings.area.coerceIn(0.15f, 1f)
    val speed = DanmakuSettings.speed.coerceAtLeast(0.1f)
    val fontScale = if (fullscreen) {
        DanmakuSettings.scaleFullscreen
    } else {
        DanmakuSettings.scaleInline
    }
    val textSizePx = with(density) { 12.sp.toPx() } * fontScale
    val strokePx = with(density) { DanmakuSettings.stroke.dp.toPx() }
    val lineHeightPx = (textSizePx * 1.9f).coerceAtLeast(1f)
    val lineCount = if (viewSize.height > 0) {
        (((viewSize.height * area) / lineHeightPx).toInt()).coerceIn(1, 9)
    } else {
        1
    }
    val bottomMarginPx = with(density) {
        (if (fullscreen) 44.dp else 42.dp).toPx()
    }
    val config = DanmakuRenderConfig(
        alpha = 255,
        textSizePx = textSizePx,
        typeface = remember { Typeface.create("sans-serif-medium", Typeface.NORMAL) },
        strokeWidthPx = strokePx,
        strokeColor = 0xE6000000.toInt(),
        scrollDurationMs = (LIVE_DM_BASE_TRAVEL_MS / speed)
            .toLong()
            .coerceAtLeast(1L),
        lineHeightPx = lineHeightPx,
        lineMarginPx = 0f,
        lineCount = lineCount,
        topMarginPx = 0f,
        bottomMarginPx = bottomMarginPx,
        pinnedDurationMs = 4_000L,
    )

    AndroidView(
        factory = { renderView },
        modifier = modifier.onSizeChanged { viewSize = it },
        update = { view -> view.alpha = opacity },
    )

    val lowRefresh = DanmakuSettings.lowRefresh
    LaunchedEffect(lowRefresh) {
        renderView.setMaxFrameRate(if (lowRefresh) 60 else 0)
    }

    LaunchedEffect(config) {
        engine.updateConfig(config)
    }

    val startTime = remember { SystemClock.elapsedRealtime() }
    var pausedAt by remember { mutableLongStateOf(0L) }
    var pausedTotal by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playing) {
        if (playing) {
            if (pausedAt > 0L) {
                pausedTotal += SystemClock.elapsedRealtime() - pausedAt
                pausedAt = 0L
            }
            engine.start(
                (SystemClock.elapsedRealtime() - startTime - pausedTotal).coerceAtLeast(0L),
            )
        } else {
            pausedAt = SystemClock.elapsedRealtime()
            engine.pause()
        }
    }

    val latestMessages by rememberUpdatedState(messages)
    val latestPlaying by rememberUpdatedState(playing)
    val latestTextSizePx by rememberUpdatedState(textSizePx)
    val latestRoomEmotes by rememberUpdatedState(roomEmotes)
    val scope = rememberCoroutineScope()
    val emoteLoadBudget = remember { Semaphore(4) }

    LaunchedEffect(engine) {
        while (isActive) {
            val pending = latestMessages.asSequence()
                .filter { it.id > lastSeenId && it.kind == LiveChatKind.DANMAKU }
                .filter { item ->
                    if (item.text.isBlank()) return@filter false
                    if (DanmakuSettings.blockScroll) return@filter false
                    if (DanmakuSettings.keywordFilter &&
                        DanmakuSettings.keywords.any { it.isNotBlank() && item.text.contains(it) }
                    ) {
                        return@filter false
                    }
                    true
                }
                .toList()
            if (pending.isNotEmpty()) {
                lastSeenId = pending.last().id
                if (latestPlaying) {
                    pending.forEach { message ->
                        val rgb = message.nameColor?.toInt()?.and(0xFFFFFF) ?: 0xFFFFFF
                        val textColor = 0xFF000000.toInt() or rgb
                        val emotes = buildMap {
                            putAll(latestRoomEmotes)
                            putAll(message.emotes)
                            message.emoteUrl
                                ?.takeIf { it.isNotBlank() }
                                ?.let { put(message.text, it) }
                        }
                        if (emotes.isNotEmpty()) {
                            scope.launch {
                                emoteLoadBudget.withPermit {
                                    val item = withContext(Dispatchers.IO) {
                                        buildLiveEmoteDanmaku(
                                            context = context,
                                            text = message.text,
                                            textColor = textColor,
                                            textSizePx = latestTextSizePx,
                                            emotes = emotes,
                                            emoteHeights = message.emoteHeights,
                                        )
                                    }
                                    if (item != null) engine.addImmediate(item)
                                }
                            }
                        } else {
                            engine.addImmediate(
                                DanmakuItem().apply {
                                    text = message.text
                                    layerType = DANMAKU_LAYER_SCROLL
                                    this.textColor = textColor
                                },
                            )
                        }
                    }
                }
            } else {
                // 列表被截断或重载时，避免旧 id 一直卡住
                val newest = latestMessages.lastOrNull()?.id ?: 0L
                if (newest > lastSeenId) lastSeenId = newest
            }
            delay(LIVE_DM_POLL_MS)
        }
    }
}
