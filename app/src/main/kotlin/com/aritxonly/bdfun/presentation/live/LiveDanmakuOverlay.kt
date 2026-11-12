package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.aritxonly.bdfun.core.model.live.LiveChatMessage

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

    val ui = remember { LiveDanmakuUiStateHolder(messages.lastOrNull()?.id ?: 0L) }
    var viewSize by ui::viewSize

    val settings = LocalSettingsBindings.current
    val opacity = settings.danmakuOpacity.coerceIn(0f, 1f)
    val config = liveDanmakuConfig(viewSize, fullscreen)
    val textSizePx = config.textSizePx

    AndroidView(
        factory = { renderView },
        modifier = modifier.onSizeChanged { viewSize = it },
        update = { view -> view.alpha = opacity },
    )

    val lowRefresh = settings.danmakuLowRefresh
    LaunchedEffect(lowRefresh) {
        renderView.setMaxFrameRate(if (lowRefresh) 60 else 0)
    }

    LaunchedEffect(config) {
        engine.updateConfig(config)
    }

    LiveDanmakuClock(engine, playing)
    LiveDanmakuMessages(engine, messages, playing, textSizePx, roomEmotes, ui)
}
