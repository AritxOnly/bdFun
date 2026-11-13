package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.teak.common.theme.TeakUiStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 唱片在页面上的统一位置。
 *
 * 首页、收藏、历史、消息、设置等所有非视频页都用这一份计算：
 * 以「底栏 + 8dp 缝隙 + 唱片」整组居中，唱片外壳的右边距 = 整组左留白。
 */
internal data class DiscPlacement(
    val endPadding: Dp,
    val bottomOffset: Dp,
    val size: Dp,
)

@Composable
internal fun rememberDiscPlacement(tabCount: Int = 3): DiscPlacement {
    val windowWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val classic = TeakUiStyle.isClassic
    val tabBarWidth = minOf(
        TeakFloatingTabBarDefaults.MaximumWidth,
        TeakFloatingTabBarDefaults.MaximumItemWidth * tabCount,
        (windowWidthDp - 32.dp).coerceAtLeast(0.dp),
    )
    val discWidth = TeakFloatingTabBarDefaults.Height
    val discGap = 8.dp
    val groupLeft = ((windowWidthDp - tabBarWidth - discGap - discWidth) / 2)
        .coerceAtLeast(0.dp)
    // HyperOS 4：所有页面统一成动态详情页那一档（贴右边缘、抬到底栏之上）。
    if (TeakUiStyle.isHyperOs4) {
        return DiscPlacement(endPadding = 0.dp, bottomOffset = (-70).dp, size = discWidth)
    }
    val endPadding = if (classic) 8.dp else (groupLeft - 8.dp).coerceAtLeast(0.dp)
    val bottomOffset = if (classic) {
        8.dp
    } else {
        4.dp - if (AppGraph.settings.bottomBarRaise) {
            TeakChromeDefaults.RaisedBottomBarExtra
        } else {
            0.dp
        }
    }
    return DiscPlacement(
        endPadding = endPadding,
        bottomOffset = bottomOffset,
        size = discWidth,
    )
}

/**
 * 底栏右侧的后台播放唱片。
 *
 * 底座和返回键/底栏同款 TeakUI 玻璃；玻璃上画唱片纹路，中间是圆形封面。
 * 播放时整张唱片（纹路 + 封面）一起转，暂停就停。
 *
 * 手势：
 * - 单击：打开原视频（立即）
 * - 左滑：下一集；右滑：上一集
 * - 上滑：退出播放；下滑：暂停 / 继续
 * - 长按不动：加速
 */
@Composable
internal fun BackgroundPlayDisc(
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    onOpenFallback: (VideoItem) -> Unit,
) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    val oneTake = rememberOneTakeAnchor()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val radiusPx = with(density) { size.toPx() / 2f }
    val swipeThreshold = with(density) { 32.dp.toPx() }
    val touchSlop = with(density) { 8.dp.toPx() }

    var pressed by remember { mutableStateOf(false) }
    var longPressActive by remember { mutableStateOf(false) }
    var dragTarget by remember { mutableStateOf(Offset.Zero) }
    var spin by remember { mutableFloatStateOf(0f) }
    var pressGen by remember { mutableIntStateOf(0) }
    var exiting by remember { mutableStateOf(false) }
    var exitComposeLogged by remember { mutableStateOf(false) }
    var displayedCover by remember { mutableStateOf(BackgroundPlayback.cover) }
    var coverBlur by remember { mutableFloatStateOf(0f) }
    var coverTintColor by remember { mutableStateOf(Color.White) }

    androidx.compose.runtime.SideEffect {
        if (exiting && !exitComposeLogged) {
            exitComposeLogged = true
            com.aritxonly.bdfun.data.FreezeWatchdog.log("DISC", "recomposed with exiting=true")
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
            com.aritxonly.bdfun.data.LiveCounters.discInstances++
        }
        onDispose {
            if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
                com.aritxonly.bdfun.data.LiveCounters.discInstances--
            }
            com.aritxonly.bdfun.data.FreezeWatchdog.log("DISC", "composable disposed")
        }
    }

    val dragX = rememberSpringFloat(dragTarget.x, stiffness = 520f, damping = 28f)
    val dragY = rememberSpringFloat(dragTarget.y, stiffness = 520f, damping = 28f)
    val exitProgress = rememberManualProgress(if (exiting) 1f else 0f, 240f)
    val maxDragPx = with(density) { 8.dp.toPx() }
    // 封面固定按这个像素尺寸请求，预加载和显示用同一个尺寸，内存缓存才能直接命中。
    val coverPx = with(density) { (size * 0.60f).roundToPx() }

    // 只触发一次：淡出结束后再关播放，不在动画帧里做重活。
    LaunchedEffect(exiting) {
        if (!exiting) return@LaunchedEffect
        com.aritxonly.bdfun.data.FreezeWatchdog.log("DISC", "exit effect start, wait 280ms")
        delay(280)
        com.aritxonly.bdfun.data.FreezeWatchdog.log("DISC", "calling BackgroundPlayback.exit()")
        BackgroundPlayback.exit()
        com.aritxonly.bdfun.data.FreezeWatchdog.log("DISC", "BackgroundPlayback.exit() returned")
    }

    // 换封面：先偷偷把新图加载好，加载完成前继续显示旧封面；
    // 加载好之后再走原来的“模糊淡出 → 换图 → 由模糊变清晰”，所以既不会白一下，也保留模糊过渡。
    val latestCover = BackgroundPlayback.cover
    // 封面取色独立于“换图动画”执行：首次进入时 displayedCover 已经等于当前封面，
    // 换图动画会直接 return，不能把取色绑在它里面，否则永远是白色。
    LaunchedEffect(latestCover) {
        if (latestCover.isBlank()) return@LaunchedEffect
        val request = coil.request.ImageRequest.Builder(context)
            .data(latestCover)
            .size(coverPx)
            .allowHardware(false)
            .crossfade(false)
            .build()
        val result = runCatching {
            coil.Coil.imageLoader(context).execute(request)
        }.getOrNull()
        val success = result as? coil.request.SuccessResult ?: return@LaunchedEffect
        averageCoverColor(success.drawable)?.let { coverTintColor = it }
    }
    LaunchedEffect(latestCover) {
        if (latestCover.isBlank() || latestCover == displayedCover) return@LaunchedEffect
        val request = coil.request.ImageRequest.Builder(context)
            .data(latestCover)
            .size(coverPx)
            .crossfade(false)
            .build()
        val result = runCatching {
            coil.Coil.imageLoader(context).execute(request)
        }.getOrNull()
        val ok = result is coil.request.SuccessResult
        if (!ok) return@LaunchedEffect
        val start = System.nanoTime()
        while (true) {
            val t = ((System.nanoTime() - start) / 1_000_000f / 120f).coerceIn(0f, 1f)
            coverBlur = t
            if (t >= 1f) break
            delay(8)
        }
        displayedCover = latestCover
        val startClear = System.nanoTime()
        while (true) {
            val t = ((System.nanoTime() - startClear) / 1_000_000f / 180f).coerceIn(0f, 1f)
            coverBlur = 1f - t
            if (t >= 1f) break
            delay(8)
        }
        coverBlur = 0f
    }

    val playing = BackgroundPlayback.playing
    val cover = displayedCover
    val coverRequest = remember(cover, coverPx) {
        coil.request.ImageRequest.Builder(context)
            .data(cover)
            .size(coverPx)
            .crossfade(false)
            .build()
    }
    val darkTheme = colors.background.luminance() < 0.5f

    // 呼吸灯：只在播放时出现；白色或封面取色，围绕唱片缓慢明暗呼吸。
    val breathEnabled = AppGraph.settings.discBreathEnabled && playing
    val breathCoverColor = AppGraph.settings.discBreathCoverColor
    var breathPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(breathEnabled) {
        if (!breathEnabled) return@LaunchedEffect
        val start = System.nanoTime()
        while (true) {
            breathPhase = ((System.nanoTime() - start) / 1_000_000f % 2400f) / 2400f
            delay(33)
        }
    }
    val breath01 = (0.5f + 0.5f * sin(breathPhase * 2f * PI.toFloat())).coerceIn(0f, 1f)
    val breathTint = if (breathCoverColor) coverTintColor else Color.White

    // 手动补间：项目里的 frame clock 动画有时不推进，这里直接用 delay 驱动旋转。
    // 退出过程中不再旋转，减少这一帧的绘制负担。
    LaunchedEffect(playing, exiting) {
        if (!playing || exiting) return@LaunchedEffect
        var last = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = (now - last) / 1_000_000_000f
            last = now
            spin = (spin + dt * 55f) % 360f
            delay(33L)
        }
    }

    val blurProgress = rememberManualProgress(
        // 上滑退出期间保持和正常手势一样的 3dp 模糊，直到整张唱片淡出。
        target = if (exiting || pressed || longPressActive) 1f else 0f,
        durationMs = 140f,
    )
    // 按压/拖动、上滑退出用同一道 3dp 模糊；换封面保持原来的单独模糊。
    // 换封面期间关掉外层模糊，避免两道模糊叠在一起。
    val gestureBlur = blurProgress * 3f
    val glassBlurDp = if (coverBlur > 0.01f) 0f else gestureBlur

    fun openOriginal() {
        val base = BackgroundPlayback.video ?: return
        val target = base.copy(
            startPositionMs = BackgroundPlayback.positionMs.coerceAtLeast(0L),
        )
        // 和其他卡片一样走小米系统的一镜到底；新视频页会接管播放。
        oneTake.open(context, target, radiusPx) {
            onOpenFallback(target)
        }
    }

    Box(
        modifier = modifier.size(size + 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (breathEnabled) {
            // 光圈和唱片一起淡出，不再留在原地。
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - exitProgress },
            ) {
                // 光圈比唱片外壳更大，允许画到 70dp 盒子之外，浅色背景下也能看出光晕在呼吸。
                val glowRadius = this.size.minDimension * (0.58f + 0.10f * breath01)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            breathTint.copy(alpha = 0.30f * breath01),
                            breathTint.copy(alpha = 0.72f * (0.30f + 0.70f * breath01)),
                            breathTint.copy(alpha = 0.20f * breath01),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = center,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(size)
                .then(oneTake.modifier)
                .graphicsLayer {
                    translationX = dragX
                    translationY = dragY - exitProgress * 26.dp.toPx()
                    alpha = 1f - exitProgress
                    scaleX = if (pressed) 0.96f else 1f
                    scaleY = if (pressed) 0.96f else 1f
                }
                .dropShadow(
                    shape = CircleShape,
                    shadow = Shadow(
                        radius = 18.dp,
                        spread = 0.dp,
                        offset = DpOffset(0.dp, 5.dp),
                        color = Color.Black,
                        alpha = if (darkTheme) 0.36f else 0.20f,
                    ),
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (exiting) {
                            // 退出动画期间不要再触发任何手势，但必须挂起等待手指抬起。
                            // 这里千万不能直接 return：手指已经抬起时 awaitEachGesture
                            // 会立刻再次进入、又立刻返回，形成界面线程不挂起的死循环。
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                            return@awaitEachGesture
                        }
                        down.consume()
                        pressed = true
                        longPressActive = false
                        dragTarget = Offset.Zero
                        val generation = ++pressGen
                        var total = Offset.Zero
                        var last = down.position
                        var dragging = false
                        var armed = false

                        // 静止按住不会再有 pointer 事件，用外部协程计时触发长按。
                        scope.launch {
                            delay(380)
                            if (pressGen == generation && !dragging && !longPressActive) {
                                longPressActive = true
                                BackgroundPlayback.boostStart()
                            }
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val delta = change.position - last
                            last = change.position
                            if (change.pressed) {
                                total += delta
                                if (!longPressActive) {
                                    if (!dragging && total.getDistance() > touchSlop) {
                                        dragging = true
                                        pressGen += 1
                                    }
                                    // 只是按住或刚起步时唱片不动；
                                    // 只有真正越过手势阈值、触发某个手势之后才跟手。
                                    if (!armed &&
                                        (abs(total.x) >= swipeThreshold ||
                                            abs(total.y) >= swipeThreshold)
                                    ) {
                                        armed = true
                                    }
                                    if (armed) {
                                        dragTarget = if (abs(total.x) >= abs(total.y)) {
                                            Offset(total.x.coerceIn(-maxDragPx, maxDragPx), 0f)
                                        } else {
                                            Offset(0f, total.y.coerceIn(-maxDragPx, maxDragPx))
                                        }
                                    }
                                }
                                change.consume()
                            } else {
                                break
                            }
                        }

                        pressed = false
                        pressGen += 1
                        com.aritxonly.bdfun.data.FreezeWatchdog.log(
                            "DISC",
                            "release total=(${total.x.toInt()},${total.y.toInt()}) " +
                                "longPress=$longPressActive exiting=$exiting",
                        )
                        if (longPressActive) {
                            longPressActive = false
                            BackgroundPlayback.boostEnd()
                        } else if (abs(total.x) >= swipeThreshold &&
                            abs(total.x) >= abs(total.y)
                        ) {
                            if (total.x < 0f) {
                                BackgroundPlayback.next()
                            } else {
                                BackgroundPlayback.previous()
                            }
                        } else if (total.y <= -swipeThreshold &&
                            abs(total.y) > abs(total.x)
                        ) {
                            // 上滑：先向上 + 模糊淡出，再真正退出。
                            exiting = true
                            com.aritxonly.bdfun.data.FreezeWatchdog.log(
                                "DISC",
                                "UP_SWIPE -> exiting=true",
                            )
                        } else if (total.y >= swipeThreshold &&
                            abs(total.y) > abs(total.x)
                        ) {
                            BackgroundPlayback.togglePlayPause()
                        } else if (total.getDistance() < touchSlop) {
                            openOriginal()
                        }
                        dragTarget = Offset.Zero
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
        // 关键性能修复：玻璃底座和旋转内容拆成兄弟节点。
        // 原来是玻璃把旋转层包在里面，封面每转一帧，整块玻璃都会重画，
        // 玻璃又采样整页背景，等于每帧重绘整个首页，界面直接卡死。
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // 静态玻璃：不随唱片旋转，只画一次。
            // 模糊只给唱片本体，玻璃高光和外围光圈始终清晰。
            TeakGlassSurface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
            ) {}

            // 旋转层：纹路、标记、封面一起转；它不包含玻璃，转起来不会触发背景采样。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .graphicsLayer { rotationZ = spin }
                    .then(
                        if (glassBlurDp > 0.1f) {
                            Modifier.blur(
                                glassBlurDp.dp,
                                // 裁成圆形：模糊量不变，只是不再把方形模糊边缘露出来。
                                edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment(CircleShape),
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.toPx() / 2f
                    val lineColor = colors.onBackground.copy(
                        alpha = if (darkTheme) 0.22f else 0.26f,
                    )
                    // 纹路：细同心圆。
                    for (i in 1..7) {
                        drawCircle(
                            color = lineColor.copy(alpha = lineColor.alpha * (1f - i * 0.09f)),
                            radius = r * (0.46f + i * 0.072f),
                            style = Stroke(width = 0.6.dp.toPx()),
                        )
                    }
                    // 不对称标记，转起来才看得出来。
                    drawCircle(
                        color = colors.primary.copy(alpha = 0.85f),
                        radius = r * 0.06f,
                        center = Offset(center.x + r * 0.75f, center.y - r * 0.10f),
                    )
                }

                // 中间圆封面。
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.60f)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh),
                ) {
                    if (cover.isNotBlank()) {
                        AsyncImage(
                            model = coverRequest,
                            contentDescription = "正在播放",
                            contentScale = ContentScale.Crop,
                            // 换封面保持原来的单独模糊（8dp、Unbounded）。
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(
                                    (coverBlur * 8f).dp,
                                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                                ),
                        )
                    }
                }
            }

            // 中心小孔；对称，不需要转。
            Box(
                modifier = Modifier
                    .size(size * 0.055f)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(colors.background.copy(alpha = 0.55f)),
            )
        }
    }
}
}

/** ???????????????????????? */
private fun averageCoverColor(drawable: android.graphics.drawable.Drawable?): Color? {
    val source = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return null
    if (source.isRecycled) return null
    return runCatching {
        // ?????? getPixels??????????
        val readable = if (source.config == android.graphics.Bitmap.Config.HARDWARE) {
            source.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return null
        } else {
            source
        }
        val small = android.graphics.Bitmap.createScaledBitmap(readable, 8, 8, false)
        val pixels = IntArray(64)
        small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        if (small !== readable && small !== source) small.recycle()
        if (readable !== source) readable.recycle()
        var r = 0L
        var g = 0L
        var b = 0L
        pixels.forEach { p ->
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
        }
        val n = pixels.size.coerceAtLeast(1)
        // ???????????????????????/??????
        // ????????????????????
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (r / n).toInt(),
            (g / n).toInt(),
            (b / n).toInt(),
            hsv,
        )
        hsv[1] = hsv[1].coerceAtLeast(0.55f)
        hsv[2] = hsv[2].coerceAtLeast(0.80f)
        Color(android.graphics.Color.HSVToColor(hsv))
    }.getOrNull()
}
