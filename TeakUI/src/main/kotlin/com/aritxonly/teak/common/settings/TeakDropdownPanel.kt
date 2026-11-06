package com.aritxonly.teak.common.settings

import android.view.ViewTreeObserver
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassPanel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.first
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 二级窗口内容当前是否接受点击：
 * 打开到位（进度 ≥ 0.72）才算“可点”，一开始收起就立刻变回不可点，
 * 和 dropdownlab 的 armed 逻辑一致。
 */
val LocalTeakDropdownInteractive = compositionLocalOf { true }

/**
 * TeakUI 统一二级窗口：MIUIDeskClock ArcAnimation 同款双弹簧。
 *
 * 尺寸弹簧：阻尼 0.8、响应 0.35s；
 * 中心位置弹簧：阻尼 0.8、响应 0.22s（比尺寸快）。
 * 起始窗口是最终窗口右上角对齐的 69% 宽、宽×0.34 高的卡片。
 *
 * 和 dropdownlab 完全一致：内容按当前窗口宽度重新排版，字号全程不变（不缩放），
 * 窗口只负责从下面多露出几行。最终高度由内容实际测量得到，
 * 所以行高、字号、选项数量都可以不同，动画比例不变。
 */
@Composable
fun TeakDropdownPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 196.dp,
    maxHeight: Dp = 460.dp,
    /** 触发按钮在窗口里的位置；面板统一按它来定位。 */
    anchorBounds: Rect? = null,
    /** 面板顶部相对触发按钮底边的偏移；负数表示略微压住按钮。 */
    anchorTopGap: Dp = (-12).dp,
    /** 面板右边相对触发按钮右边的偏移。 */
    anchorEndGap: Dp = 16.dp,
    /** 若不为 null，最终顶边放在触发按钮高度的这个比例处，优先于 [anchorTopGap]。 */
    anchorTopRatio: Float? = null,
    /** 没有 anchor 时的兜底位置。 */
    topPadding: Dp = 36.dp,
    endPadding: Dp = 28.dp,
    /** 面板内容的垂直内边距。 */
    contentPadding: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    var mounted by remember { mutableStateOf(false) }
    var openGen by remember { mutableStateOf(0) }
    var readyGen by remember { mutableStateOf(-1) }
    var fullHeightPx by remember { mutableStateOf(0) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            mounted = true
            // 每次打开都换一代，避免上一次的 ready 状态让动画提前启动、吃掉开头。
            openGen += 1
            progress.snapTo(0f)
        } else if (mounted) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 225, easing = FastOutLinearInEasing),
            )
            mounted = false
        }
    }

    if (!mounted) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val backdrop = LocalTeakBackdrop.current
    val materialSpec = LocalTeakMaterialSpec.current
    val colors = MiuixTheme.colorScheme

    val fullWidth = minOf(width, screenWidth - 32.dp)
    val finalHeight = if (fullHeightPx > 0) {
        with(density) { fullHeightPx.toDp() }.coerceAtMost(maxHeight)
    } else {
        1.dp
    }

    val anchorTop = anchorBounds?.let { with(density) { it.top.toDp() } }
    val anchorHeight = anchorBounds?.let { with(density) { it.height.toDp() } }
    val anchorRight = anchorBounds?.let { with(density) { it.right.toDp() } }
    val statusTop = with(density) {
        androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(density)
    }

    val finalTop = when {
        anchorTopRatio != null && anchorTop != null && anchorHeight != null ->
            Dp(anchorTop.value + anchorHeight.value * anchorTopRatio)
        anchorTop != null && anchorHeight != null && anchorRight != null ->
            anchorTop + anchorHeight + anchorTopGap
        else -> with(density) { statusTop.toDp() } + topPadding
    }
    val finalRight = if (anchorRight != null) {
        anchorRight - anchorEndGap
    } else {
        screenWidth - endPadding
    }
    val finalLeft = (finalRight - fullWidth).coerceAtLeast(8.dp)
    val safeFinalTop = finalTop
        .coerceAtLeast(20.dp)
        .coerceAtMost((screenHeight - finalHeight - 20.dp).coerceAtLeast(20.dp))

    // 起始窗口：右上角对齐最终窗口。
    val startWidth = Dp(fullWidth.value * 0.69f)
    val startHeight = Dp(startWidth.value * 0.34f)
    val startRight = finalLeft + fullWidth
    val startLeft = startRight - startWidth
    val startTop = safeFinalTop
    val startBottom = startTop + startHeight
    val startCenterX = Dp((startLeft.value + startRight.value) / 2f)
    val startCenterY = Dp((startTop.value + startBottom.value) / 2f)
    val endCenterX = Dp(finalLeft.value + fullWidth.value / 2f)
    val endCenterY = Dp(safeFinalTop.value + finalHeight.value / 2f)

    // 必须等“这一代”的 Dialog 第一帧真正画出来，并且量到内容高度，才启动打开动画。
    // 逐帧推进，单帧最多按 1/30 秒算：掉帧时宁可整体慢一点，也不许把开头那几帧直接跳过去。
    LaunchedEffect(visible, openGen, readyGen) {
        if (!visible || readyGen != openGen) return@LaunchedEffect
        // 等第一次量到内容高度；用 snapshotFlow 而不是把高度当 key，
        // 否则动画中途高度变化会让整段动画从 0 重放。
        snapshotFlow { fullHeightPx }.first { it > 0 }
        // 先空转两帧，保证起始的小卡片确实被显示过一次。
        withFrameNanos { }
        withFrameNanos { }
        var elapsedNanos = 0L
        var lastFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val raw = frame - lastFrame
            lastFrame = frame
            elapsedNanos += raw.coerceIn(0L, 33_000_000L)
            val p = (elapsedNanos / 700_000_000f).coerceIn(0f, 1f)
            progress.snapTo(p)
            if (p >= 1f) break
        }
    }

    val elapsedSec = progress.value * 0.7f
    val fraction = if (visible) springStep(elapsedSec, 0.8f, 0.35f) else progress.value
    val position = if (visible) springStep(elapsedSec, 0.8f, 0.22f) else progress.value

    val currentWidth = Dp(startWidth.value + (fullWidth.value - startWidth.value) * fraction)
    val startKgb = 0.34f
    val endKgb = if (fullWidth.value == 0f) 0f else finalHeight.value / fullWidth.value
    val kgb = startKgb + (endKgb - startKgb) * fraction
    val currentHeight = Dp(currentWidth.value * kgb)
    val currentCenterX = Dp(startCenterX.value + (endCenterX.value - startCenterX.value) * position)
    val currentCenterY = Dp(startCenterY.value + (endCenterY.value - startCenterY.value) * position)
    // 和测试程序一致：每一帧都夹在屏幕内，而不是只夹最终位置。
    val currentLeft = (Dp(currentCenterX.value - currentWidth.value / 2f))
        .coerceAtLeast(8.dp)
        .coerceAtMost((screenWidth - currentWidth - 8.dp).coerceAtLeast(8.dp))
    val currentTop = (Dp(currentCenterY.value - currentHeight.value / 2f))
        .coerceAtLeast(20.dp)
        .coerceAtMost((screenHeight - currentHeight - 20.dp).coerceAtLeast(20.dp))

    val shape = RoundedCornerShape(24.dp)
    // 关闭：225ms 线性回缩（比原来 250ms 快 10%），最后 63ms 淡出，整块模糊从 0 涨到 10dp。
    val panelAlpha = if (visible) 1f else (progress.value * 225f / 63f).coerceIn(0f, 1f)
    val blurDp = if (visible) 0f else (1f - progress.value.coerceIn(0f, 1f)) * 10f
    val shadowAlpha = ((panelAlpha - 0.2f) / 0.8f).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = { if (progress.value > 0.55f) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setDimAmount(0f)
            dialogWindow?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
            )
            dialogWindow?.decorView?.elevation = 0f
        }

        LaunchedEffect(visible, openGen) {
            if (!visible) return@LaunchedEffect
            // 用“这一帧真的画完了”作为信号，而不是“即将画”：
            // 设置页比较重，Dialog 窗口从创建到真正上屏可能比 demo 慢一两帧，
            // 用 preDraw 会让动画在窗口还看不见的时候就开始跑，开场就被吃掉了。
            suspendCancellableCoroutine<Unit> { continuation ->
                val observer = view.viewTreeObserver
                val fired = java.util.concurrent.atomic.AtomicBoolean(false)
                val listener = object : ViewTreeObserver.OnDrawListener {
                    override fun onDraw() {
                        // onDraw 内部不允许移除监听，交回主线程队列，下一帧前处理。
                        if (!fired.compareAndSet(false, true)) return
                        view.post {
                            if (observer.isAlive) {
                                runCatching { observer.removeOnDrawListener(this) }
                            }
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }
                }
                observer.addOnDrawListener(listener)
                continuation.invokeOnCancellation {
                    view.post {
                        if (observer.isAlive) {
                            runCatching { observer.removeOnDrawListener(listener) }
                        }
                    }
                }
            }
            readyGen = openGen
        }

        CompositionLocalProvider(
            LocalTeakBackdrop provides backdrop,
            LocalTeakMaterialSpec provides materialSpec,
            LocalTeakDropdownInteractive provides (visible && progress.value >= 0.72f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = null, indication = null) {
                            if (progress.value > 0.55f) onDismiss()
                        },
                )

                // 屏幕外把内容按最终宽度量一次真实高度，用来算终点位置；
                // 这份副本 alpha=0、放在屏幕外，看不见也不可点。
                Column(
                    modifier = Modifier
                        .offsetInPx(Dp(-10000f), Dp(0f))
                        .width(fullWidth)
                        .wrapContentHeight(unbounded = true)
                        .alpha(0f)
                        .onGloballyPositioned { coordinates ->
                            if (coordinates.size.height > 0) {
                                fullHeightPx = coordinates.size.height
                            }
                        }
                        .padding(vertical = contentPadding),
                ) {
                    content()
                }

                // 阴影单独一层，跟随整体进度淡出。
                Box(
                    modifier = Modifier
                        .offsetInPx(currentLeft, currentTop)
                        .width(currentWidth)
                        .height(currentHeight)
                        .graphicsLayer { alpha = shadowAlpha }
                        .dropShadow(
                            shape = shape,
                            shadow = Shadow(radius = 16.dp, color = Color.Black, alpha = 0.13f),
                        )
                        .background(Color.White.copy(alpha = 0.001f), shape),
                )

                // 白底 + 玻璃 + 文字放在同一个模糊图层里。
                Box(
                    modifier = Modifier
                        .offsetInPx(currentLeft, currentTop)
                        .width(currentWidth)
                        .height(currentHeight)
                        .graphicsLayer {
                            val radiusPx = blurDp.dp.toPx()
                            renderEffect = if (radiusPx > 0.5f) {
                                BlurEffect(radiusPx, radiusPx, TileMode.Decal)
                            } else {
                                null
                            }
                        }
                        .clip(shape)
                        .background(Color.White),
                ) {
                    TeakGlassPanel(
                        modifier = Modifier.fillMaxSize(),
                        shape = shape,
                        solid = backdrop == null,
                        solidColor = colors.surface,
                        tintAlpha = 1f,
                        baseAlpha = 0f,
                        materialAlpha = 1f,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = contentPadding),
                            verticalArrangement = Arrangement.Top,
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.offsetInPx(xDp: Dp, yDp: Dp) =
    this.then(
        Modifier.offset {
            IntOffset(xDp.roundToPx(), yDp.roundToPx())
        },
    )

/**
 * 与 MIUI Folme `SpringInterpolator` 相同的单位阶跃响应：
 * ω = 2π / response，零初速度、目标 1。
 */
private fun springStep(timeSec: Float, damping: Float, response: Float): Float {
    if (timeSec <= 0f) return 0f
    val zeta = damping.coerceIn(0.05f, 1.5f).toDouble()
    val omega = 2.0 * PI / response.coerceAtLeast(0.02f).toDouble()
    return if (zeta < 1.0 - 1e-4) {
        val wd = omega * sqrt(1.0 - zeta * zeta)
        val decay = exp(-zeta * omega * timeSec)
        (1.0 - decay * (cos(wd * timeSec) + (zeta / sqrt(1.0 - zeta * zeta)) * sin(wd * timeSec)))
            .toFloat()
    } else if (abs(zeta - 1.0) <= 1e-4) {
        val decay = exp(-omega * timeSec)
        (1.0 - decay * (1.0 + omega * timeSec)).toFloat()
    } else {
        val s = sqrt(zeta * zeta - 1.0)
        val r1 = -zeta * omega + omega * s
        val r2 = -zeta * omega - omega * s
        val c1 = r2 / (r1 - r2)
        val c2 = -r1 / (r1 - r2)
        (1.0 + c1 * exp(r1 * timeSec) + c2 * exp(r2 * timeSec)).toFloat()
    }
}
