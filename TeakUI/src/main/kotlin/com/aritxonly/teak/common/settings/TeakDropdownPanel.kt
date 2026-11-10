package com.aritxonly.teak.common.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassPanel
import kotlinx.coroutines.launch
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
    var fullHeightPx by remember { mutableStateOf(0) }
    // 和 Miuix ListPopupDefaults 完全相同的两条进度：
    // 缩放 / 裁剪展开用 spring(0.82, 362.5)，透明度进场 200ms、退场 150ms。
    val fraction = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            mounted = true
            fraction.snapTo(0f)
            alpha.snapTo(0f)
            // 等真实高度量出来、并让首帧先画一次，再开始动画，避免开场被吃掉。
            snapshotFlow { fullHeightPx }.first { it > 0 }
            withFrameNanos { }
            withFrameNanos { }
            kotlinx.coroutines.coroutineScope {
                launch {
                    fraction.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = 362.5f,
                            visibilityThreshold = 0.0001f,
                        ),
                    )
                }
                alpha.animateTo(1f, tween(durationMillis = 200))
            }
        } else if (mounted) {
            kotlinx.coroutines.coroutineScope {
                launch {
                    fraction.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = 362.5f,
                            visibilityThreshold = 0.0001f,
                        ),
                    )
                }
                alpha.animateTo(0f, tween(durationMillis = 150))
            }
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

    // 和 Miuix ListPopupLayout 一致：面板始终按最终尺寸摆放，只做
    // 0.15 → 1 的缩放、透明度渐入，以及从触发方向展开的裁剪。
    val showBelow = anchorTop != null && anchorHeight != null &&
        safeFinalTop >= anchorTop + anchorHeight
    val showAbove = anchorTop != null && anchorHeight != null &&
        safeFinalTop + finalHeight <= anchorTop
    val transformOrigin = TransformOrigin(
        pivotFractionX = 1f,
        pivotFractionY = when {
            showBelow -> 0f
            showAbove -> 1f
            else -> 0.5f
        },
    )
    val shape = RoundedCornerShape(24.dp)
    val panelAlpha = alpha.value
    val shadowAlpha = alpha.value

    Dialog(
        onDismissRequest = { if (alpha.value > 0.55f) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        val hostActivity = LocalContext.current.hostActivity()
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setDimAmount(0f)
            dialogWindow?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
            )
            dialogWindow?.decorView?.elevation = 0f
            // 二级菜单是独立窗口。不主动同步时，系统会按 Dialog 自己的默认值
            // 重算状态栏图标明暗，看起来像整页“反色”。这里完全沿用原页面的状态。
            matchSystemBars(dialogWindow, hostActivity?.window)
        }

        CompositionLocalProvider(
            LocalTeakBackdrop provides backdrop,
            LocalTeakMaterialSpec provides materialSpec,
            LocalTeakDropdownInteractive provides (visible && fraction.value >= 0.72f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = null, indication = null) {
                            if (alpha.value > 0.55f) onDismiss()
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

                // 阴影单独一层，跟着同一套缩放 / 透明度动画走。
                Box(
                    modifier = Modifier
                        .offsetInPx(finalLeft, safeFinalTop)
                        .width(fullWidth)
                        .height(finalHeight)
                        .graphicsLayer {
                            val scale = 0.15f + 0.85f * fraction.value
                            scaleX = scale
                            scaleY = scale
                            this.alpha = shadowAlpha
                            this.transformOrigin = transformOrigin
                        }
                        .dropShadow(
                            shape = shape,
                            shadow = Shadow(radius = 16.dp, color = Color.Black, alpha = 0.13f),
                        )
                        .background(Color.White.copy(alpha = 0.001f), shape),
                )

                // 白底 + 玻璃 + 文字：动画参数完全照 Miuix ListPopupContent。
                Box(
                    modifier = Modifier
                        .offsetInPx(finalLeft, safeFinalTop)
                        .width(fullWidth)
                        .height(finalHeight)
                        .graphicsLayer {
                            val scale = 0.15f + 0.85f * fraction.value
                            scaleX = scale
                            scaleY = scale
                            this.alpha = panelAlpha
                            this.transformOrigin = transformOrigin
                        }
                        .dropdownClipReveal(
                            fraction = { fraction.value },
                            showBelow = showBelow,
                            showAbove = showAbove,
                            corner = 24.dp,
                        )
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

fun Context.hostActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** 让 Dialog 窗口的状态栏 / 导航栏图标明暗和宿主页面完全一致。 */
fun matchSystemBars(dialogWindow: Window?, hostWindow: Window?) {
    if (dialogWindow == null || hostWindow == null) return
    runCatching {
        val host = WindowCompat.getInsetsController(hostWindow, hostWindow.decorView)
        val dialog = WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
        dialog.isAppearanceLightStatusBars = host.isAppearanceLightStatusBars
        dialog.isAppearanceLightNavigationBars = host.isAppearanceLightNavigationBars
        // 视频页等隐藏状态栏的场景，菜单窗口也不要把它重新显示出来。
        val hostInsets = ViewCompat.getRootWindowInsets(hostWindow.decorView)
        if (hostInsets != null &&
            !hostInsets.isVisible(WindowInsetsCompat.Type.statusBars())
        ) {
            dialog.hide(WindowInsetsCompat.Type.statusBars())
        }
    }
}

/**
 * Miuix ListPopupContent 同款方向性裁剪：
 * 从触发方向（按钮下方 / 上方 / 中间）把面板逐步展开，边缘按面板圆角裁切。
 */
private fun Modifier.dropdownClipReveal(
    fraction: () -> Float,
    showBelow: Boolean,
    showAbove: Boolean,
    corner: Dp,
): Modifier = drawWithCache {
    val path = Path()
    val cornerPx = corner.toPx()
    onDrawWithContent {
        val p = fraction().coerceIn(0f, 1f)
        if (p <= 0f) return@onDrawWithContent
        val height = size.height
        val visible = height * p
        if (visible <= 0f) return@onDrawWithContent
        val start = when {
            showBelow -> 0f
            showAbove -> height * (1f - p)
            else -> height * (0.5f - 0.5f * p)
        }
        path.rewind()
        path.addRoundRect(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = visible,
                cornerRadius = CornerRadius(cornerPx),
            ),
        )
        if (start == 0f) {
            clipPath(path) {
                this@onDrawWithContent.drawContent()
            }
        } else {
            translate(top = start) {
                clipPath(path) {
                    translate(top = -start) {
                        this@onDrawWithContent.drawContent()
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

