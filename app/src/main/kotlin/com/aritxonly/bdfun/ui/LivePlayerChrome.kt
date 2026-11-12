package com.aritxonly.bdfun.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.BrandPink
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Refresh
import kotlin.math.abs

/**
 * 直播播放器控件层。
 *
 * 视觉和交互直接照搬视频播放器：
 * - 竖屏半屏使用底部 36dp 紧凑条（[VideoPlayer] 的 InlineCompactChrome）；
 * - 横屏全屏使用顶部标题行 + 底部 42dp 控制行；
 * - 点画面显隐，4 秒后自动收起，控件一触重新计时。
 *
 * 和视频播放器的区别：没有进度条、时长、选集、倍速；播放键右侧固定是「直播重载」。
 */
@Composable
internal fun LivePlayerChrome(
    landscape: Boolean,
    playing: Boolean,
    danmakuEnabled: Boolean,
    qualityLabel: String,
    title: String,
    paused: Boolean,
    onPlayPause: () -> Unit,
    onReload: () -> Unit,
    onToggleDanmaku: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDanmakuSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(true) }
    var touchTick by remember { mutableIntStateOf(0) }

    fun touch() {
        touchTick++
    }

    LaunchedEffect(visible, paused, touchTick) {
        if (visible && !paused) {
            delay(4_000L)
            visible = false
        }
    }

    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val activity = remember(context) {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) break
            current = current.baseContext
        }
        current as? Activity
    }
    val audio = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    var dragBase by remember { mutableFloatStateOf(0f) }
    var gestureTip by remember { mutableStateOf<LiveGestureTip?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .livePlayerGestures(
                ignoreDown = { position, height ->
                    // 控件显示时，顶部和底部的按钮区域交给子控件处理，不抢点击
                    val topZone = with(density) { (if (landscape) 58.dp else 0.dp).toPx() }
                    val bottomZone = with(density) { (if (landscape) 54.dp else 48.dp).toPx() }
                    visible && (
                        (landscape && position.y < topZone) ||
                            position.y > height - bottomZone
                        )
                },
                onTap = {
                    if (!paused) {
                        visible = !visible
                        touch()
                    }
                },
                onDoubleTap = {
                    if (!paused) {
                        onPlayPause()
                        touch()
                    }
                },
                onVerticalDragStart = { leftSide ->
                    dragBase = if (leftSide) {
                        currentBrightness(activity)
                    } else {
                        audio?.let(::currentVolumeRatio) ?: 0f
                    }
                },
                onVerticalDrag = { leftSide, totalDy ->
                    val next = (dragBase - totalDy / 1200f).coerceIn(0f, 1f)
                    if (leftSide) {
                        applyBrightness(activity, next)
                        gestureTip = LiveGestureTip(brightness = true, value = next)
                    } else {
                        audio?.let { setVolumeRatio(it, next) }
                        gestureTip = LiveGestureTip(brightness = false, value = next)
                    }
                },
                onVerticalDragEnd = {
                    gestureTip = null
                },
                onCenterSwipe = {
                    if (!paused) {
                        onToggleFullscreen()
                        touch()
                    }
                },
            ),
    ) {
        if (visible) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 底部压暗，保证白字在亮画面上仍然可读
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(if (landscape) 235.dp else 80.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                (if (landscape) 0.45f else 0.35f) to Color.Black.copy(alpha = 0.35f),
                                1f to Color.Black.copy(alpha = if (landscape) 0.82f else 0.65f),
                            ),
                        ),
                )

                if (landscape) {
                    // 横屏没有页面顶栏，把返回和标题叠在播放器上，和视频全屏一致
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.62f),
                                    0.5f to Color.Black.copy(alpha = 0.25f),
                                    1f to Color.Transparent,
                                ),
                            ),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp, end = 24.dp, top = 12.dp)
                            .height(46.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                ) {
                                    touch()
                                    onBack()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.ChevronBackward,
                                contentDescription = "返回",
                                tint = Color.White,
                                modifier = Modifier.size(23.5.dp),
                            )
                        }
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 底部控制行：播放 / 直播重载 / 弹幕开关 / 设置 / 全屏
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            start = if (landscape) 42.dp else 10.dp,
                            end = if (landscape) 28.dp else 12.dp,
                            bottom = if (landscape) 6.dp else 7.dp,
                        )
                        .height(if (landscape) 42.dp else 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiveControlButton(
                        size = if (landscape) 40.dp else 28.dp,
                        onClick = {
                            touch()
                            onPlayPause()
                        },
                    ) {
                        if (playing) {
                            PauseGlyph()
                        } else {
                            PlayGlyph(size = if (landscape) 24.dp else 22.dp)
                        }
                    }
                    Spacer(Modifier.width(if (landscape) 12.dp else 7.dp))

                    // 紧挨播放键：直播重载，竖屏和横屏都在同一位置
                    LiveControlButton(
                        size = 32.dp,
                        onClick = {
                            touch()
                            onReload()
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = "直播重载",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Spacer(Modifier.width(if (landscape) 12.dp else 8.dp))

                    LiveControlButton(
                        size = 32.dp,
                        onClick = {
                            touch()
                            onToggleDanmaku()
                        },
                    ) {
                        // 视频播放器这里用的是原图，不染色；粉色和白色都保留
                        LivePixelIcon(
                            res = if (danmakuEnabled) TeakIcon.PlayerDanmakuOn else TeakIcon.PlayerDanmakuOff,
                            size = 23.dp,
                        )
                    }
                    Spacer(Modifier.width(if (landscape) 8.dp else 4.dp))

                    LiveControlButton(
                        size = 32.dp,
                        onClick = {
                            touch()
                            onOpenDanmakuSettings()
                        },
                    ) {
                        LivePixelIcon(res = TeakIcon.PlayerDanmakuSettings, size = 23.dp)
                    }

                    Spacer(Modifier.weight(1f))

                    if (qualityLabel.isNotBlank()) {
                        Text(
                            text = qualityLabel,
                            color = Color.White,
                            fontSize = if (landscape) 14.sp else 12.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                ) {
                                    touch()
                                    onOpenSettings()
                                }
                                .padding(
                                    horizontal = if (landscape) 6.dp else 5.dp,
                                    vertical = if (landscape) 8.dp else 6.dp,
                                ),
                        )
                        Spacer(Modifier.width(if (landscape) 12.dp else 6.dp))
                    }

                    LiveControlButton(
                        size = if (landscape) 32.dp else 29.dp,
                        onClick = {
                            touch()
                            onToggleFullscreen()
                        },
                    ) {
                        ResourceGlyph(
                            res = TeakIcon.Fullscreen,
                            tint = Color.White,
                            size = if (landscape) 20.dp else 17.dp,
                        )
                    }
                }
            }
        }

        gestureTip?.let { tip ->
            LiveGestureIconTip(
                brightness = tip.brightness,
                value = tip.value,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private data class LiveGestureTip(val brightness: Boolean, val value: Float)

@Composable
private fun LiveControlButton(
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun LivePixelIcon(
    res: Int,
    size: androidx.compose.ui.unit.Dp,
) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier.size(size),
    )
}

/* ------------------------------------------------------------------ */
/* 手势：单击显隐、双击播放/暂停、左竖滑亮度、右竖滑音量                  */
/* ------------------------------------------------------------------ */

private data class LiveGestureCallbacks(
    val ignoreDown: (Offset, Float) -> Boolean,
    val onTap: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onVerticalDragStart: (leftSide: Boolean) -> Unit,
    val onVerticalDrag: (leftSide: Boolean, totalDy: Float) -> Unit,
    val onVerticalDragEnd: () -> Unit,
    val onCenterSwipe: () -> Unit,
)

@Composable
private fun Modifier.livePlayerGestures(
    ignoreDown: (Offset, Float) -> Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onVerticalDragStart: (leftSide: Boolean) -> Unit,
    onVerticalDrag: (leftSide: Boolean, totalDy: Float) -> Unit,
    onVerticalDragEnd: () -> Unit,
    onCenterSwipe: () -> Unit,
): Modifier {
    val tapState = remember { longArrayOf(0L) }
    val pendingTap = remember { arrayOfNulls<Job>(1) }
    val scope = rememberCoroutineScope()
    val callbacks by rememberUpdatedState(
        LiveGestureCallbacks(
            ignoreDown = ignoreDown,
            onTap = onTap,
            onDoubleTap = onDoubleTap,
            onVerticalDragStart = onVerticalDragStart,
            onVerticalDrag = onVerticalDrag,
            onVerticalDragEnd = onVerticalDragEnd,
            onCenterSwipe = onCenterSwipe,
        ),
    )

    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (callbacks.ignoreDown(down.position, size.height.toFloat())) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    if (event.changes.none { it.pressed }) break
                }
                return@awaitEachGesture
            }
            // 系统返回手势区域：24dp 内完全放行，避免抢系统侧滑
            val systemEdge = 24.dp.toPx()
            if (down.position.x < systemEdge || down.position.x > size.width - systemEdge) {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.none { it.pressed }) break
                }
                return@awaitEachGesture
            }

            val side = when {
                down.position.x < size.width * 0.28f -> true
                down.position.x > size.width * 0.72f -> false
                else -> null
            }
            val verticalAllowed = AppGraph.settings.gestureBrightnessVolume &&
                side != null &&
                down.position.y > 80.dp.toPx() &&
                down.position.y < size.height - 100.dp.toPx()
            val centerSwipeEnabled = AppGraph.settings.gestureCenterFullscreen && side == null
            val leftSide = side ?: (down.position.x < size.width / 2f)

            var dragged = false
            var vertical = false
            var startedVertical = false
            var centerSwipe = false
            var centerSwipeDy = 0f
            var totalDy = 0f
            var totalDx = 0f
            var previous = down
            val downTime = System.currentTimeMillis()

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (!dragged) {
                        val held = System.currentTimeMillis() - downTime
                        if (held < 300) {
                            val now = System.currentTimeMillis()
                            if (now - tapState[0] < 260) {
                                tapState[0] = 0
                                pendingTap[0]?.cancel()
                                pendingTap[0] = null
                                callbacks.onDoubleTap()
                            } else {
                                tapState[0] = now
                                pendingTap[0]?.cancel()
                                pendingTap[0] = scope.launch {
                                    delay(260)
                                    if (tapState[0] != 0L) callbacks.onTap()
                                }
                            }
                        }
                    }
                    break
                }
                val dx = change.position.x - previous.position.x
                val dy = change.position.y - previous.position.y
                totalDx += dx
                totalDy += dy
                previous = change
                if (!dragged &&
                    (abs(totalDy) > viewConfiguration.touchSlop ||
                        abs(totalDx) > viewConfiguration.touchSlop)
                ) {
                    dragged = true
                    val verticalDrag = abs(totalDy) >= abs(totalDx)
                    centerSwipe = verticalDrag && centerSwipeEnabled
                    vertical = verticalDrag && verticalAllowed && !centerSwipe
                    if (vertical) {
                        startedVertical = true
                        tapState[0] = 0
                        pendingTap[0]?.cancel()
                        pendingTap[0] = null
                        callbacks.onVerticalDragStart(leftSide)
                    } else if (centerSwipe) {
                        tapState[0] = 0
                        pendingTap[0]?.cancel()
                        pendingTap[0] = null
                    }
                }
                if (dragged && centerSwipe) {
                    centerSwipeDy = totalDy
                    change.consume()
                } else if (dragged && vertical) {
                    callbacks.onVerticalDrag(leftSide, totalDy)
                    change.consume()
                }
            }
            if (startedVertical) callbacks.onVerticalDragEnd()
            if (centerSwipe && abs(centerSwipeDy) > 70.dp.toPx()) callbacks.onCenterSwipe()
        }
    }
}

@Composable
private fun LiveGestureIconTip(
    brightness: Boolean,
    value: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
            val stroke = 1.7.dp.toPx()
            if (brightness) {
                drawCircle(
                    color = Color.White,
                    radius = size.width * 0.19f,
                    style = Stroke(stroke),
                )
                repeat(8) { i ->
                    val angle = Math.toRadians((i * 45).toDouble())
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r1 = size.width * 0.31f
                    val r2 = size.width * 0.44f
                    drawLine(
                        color = Color.White,
                        start = Offset(
                            cx + (r1 * kotlin.math.cos(angle)).toFloat(),
                            cy + (r1 * kotlin.math.sin(angle)).toFloat(),
                        ),
                        end = Offset(
                            cx + (r2 * kotlin.math.cos(angle)).toFloat(),
                            cy + (r2 * kotlin.math.sin(angle)).toFloat(),
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            } else {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.38f)
                    lineTo(size.width * 0.32f, size.height * 0.38f)
                    lineTo(size.width * 0.55f, size.height * 0.16f)
                    lineTo(size.width * 0.55f, size.height * 0.84f)
                    lineTo(size.width * 0.32f, size.height * 0.62f)
                    lineTo(size.width * 0.12f, size.height * 0.62f)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(
                        width = stroke,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
                drawArc(
                    color = Color.White,
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.52f, size.height * 0.3f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.3f, size.height * 0.4f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFB7299)),
            )
        }
    }
}

private fun currentBrightness(activity: Activity?): Float {
    val value = activity?.window?.attributes?.screenBrightness ?: -1f
    return if (value < 0f) 0.5f else value
}

private fun applyBrightness(activity: Activity?, value: Float) {
    val window = activity?.window ?: return
    window.attributes = window.attributes.apply {
        screenBrightness = value.coerceIn(0.02f, 1f)
    }
}

private fun currentVolumeRatio(audio: AudioManager): Float {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

private fun setVolumeRatio(audio: AudioManager, ratio: Float) {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    audio.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        (ratio * max).toInt().coerceIn(0, max),
        0,
    )
}

/* ------------------------------------------------------------------ */
/* 弹幕设置：复用视频播放器的 DanmakuSettingsContent                      */
/* ------------------------------------------------------------------ */

@Composable
internal fun LiveDanmakuSettingsOverlay(
    landscape: Boolean,
    onDismiss: () -> Unit,
) {
    if (landscape) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val panelWidth = (maxWidth.value * 0.41f).dp.coerceAtLeast(360.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.25f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.25f),
                            0.58f to Color.Black.copy(alpha = 0.74f),
                            0.68f to Color.Black.copy(alpha = 0.95f),
                            1f to Color.Black,
                        ),
                    )
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(panelWidth)
                    .fillMaxHeight()
                    .clickable(interactionSource = null, indication = null) {}
                    .navigationBarsPadding()
                    .padding(start = 32.dp, end = 30.dp, top = 30.dp, bottom = 18.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DanmakuSettingsContent(
                    fullscreen = true,
                    onInteract = {},
                    liveMode = true,
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Color(0xFF141519))
                    .clickable(interactionSource = null, indication = null) {}
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DanmakuSettingsContent(
                    fullscreen = false,
                    onInteract = {},
                    liveMode = true,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 播放设置：竖屏底部弹出，横屏从右侧滑入                                 */
/* ------------------------------------------------------------------ */

@Composable
internal fun LiveSettingsOverlay(
    landscape: Boolean,
    qualities: List<Pair<Int, String>>,
    currentQuality: Int,
    onSelectQuality: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (landscape) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val panelWidth = (maxWidth.value * 0.41f).dp.coerceAtLeast(360.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.25f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.25f),
                            0.58f to Color.Black.copy(alpha = 0.74f),
                            0.68f to Color.Black.copy(alpha = 0.95f),
                            1f to Color.Black,
                        ),
                    )
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(panelWidth)
                    .fillMaxHeight()
                    .clickable(interactionSource = null, indication = null) {}
                    .navigationBarsPadding()
                    .padding(start = 32.dp, end = 30.dp, top = 30.dp, bottom = 18.dp),
            ) {
                LiveSettingsContent(
                    qualities = qualities,
                    currentQuality = currentQuality,
                    onSelectQuality = onSelectQuality,
                    onClose = onDismiss,
                    scrollable = true,
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Color(0xFF141519))
                    .clickable(interactionSource = null, indication = null) {}
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                LiveSettingsContent(
                    qualities = qualities,
                    currentQuality = currentQuality,
                    onSelectQuality = onSelectQuality,
                    onClose = onDismiss,
                    scrollable = false,
                )
            }
        }
    }
}

@Composable
private fun LiveSettingsContent(
    qualities: List<Pair<Int, String>>,
    currentQuality: Int,
    onSelectQuality: (Int) -> Unit,
    onClose: () -> Unit,
    scrollable: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "播放设置",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(interactionSource = null, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            LiveCloseGlyph()
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = "清晰度",
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = if (scrollable) 480.dp else 320.dp)
            .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
    ) {
        if (qualities.isEmpty()) {
            Text(
                text = "暂无可用清晰度",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
        qualities.forEach { (qn, name) ->
            val active = qn == currentQuality
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) BrandPink.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable(interactionSource = null, indication = null) {
                        onSelectQuality(qn)
                    }
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = name,
                    color = if (active) BrandPink else Color.White,
                    fontSize = 15.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                if (active) LiveCheckGlyph(BrandPink)
            }
        }
    }
}

@Composable
private fun LiveCloseGlyph(tint: Color = Color.White.copy(alpha = 0.7f)) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = size.width * 0.12f
        drawLine(
            tint,
            Offset(size.width * 0.2f, size.height * 0.2f),
            Offset(size.width * 0.8f, size.height * 0.8f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            tint,
            Offset(size.width * 0.8f, size.height * 0.2f),
            Offset(size.width * 0.2f, size.height * 0.8f),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun LiveCheckGlyph(tint: Color = Color.White) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(15.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.52f)
            lineTo(size.width * 0.42f, size.height * 0.78f)
            lineTo(size.width * 0.86f, size.height * 0.25f)
        }
        drawPath(
            path,
            tint,
            style = Stroke(
                size.width * 0.13f,
                cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}
