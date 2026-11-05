package com.aritxonly.teak.common.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aritxonly.teak.common.chrome.TeakMotion
import com.aritxonly.teak.common.material.TeakGlassPanel
import kotlinx.coroutines.delay

/**
 * TeakUI 通用二级菜单面板。
 *
 * 与设置页下拉、收藏页右上角省略号使用同一套动效：
 * 18dp 圆角、以右上角为原点缩放、内容由模糊变清晰、200ms 渐显 / 240ms 收起。
 * 位置固定贴右上角，调用方只需要给出宽度和上下留白。
 *
 * [mounted] 由可见状态驱动，退场期间保留 Dialog，避免收起时闪一下。
 */
@Composable
fun TeakDropdownPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 196.dp,
    maxHeight: Dp = 460.dp,
    /** 触发按钮在窗口里的位置；面板统一按它来定位，和收藏页省略号保持同一套距离。 */
    anchorBounds: androidx.compose.ui.geometry.Rect? = null,
    /** 面板顶部相对触发按钮底边的偏移；负数表示略微压住按钮，和收藏页一致。 */
    anchorTopGap: Dp = (-12).dp,
    /** 面板右边相对触发按钮右边的偏移。 */
    anchorEndGap: Dp = 16.dp,
    /** 没有 anchor 时的兜底位置。 */
    topPadding: Dp = 36.dp,
    endPadding: Dp = 28.dp,
    /** 面板内容的垂直内边距；筛选面板这种分组内容通常传 0，自己控制间距。 */
    contentPadding: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    var mounted by remember { mutableStateOf(false) }
    var dismissArmed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            mounted = true
            dismissArmed = false
            delay(220)
            dismissArmed = true
        } else {
            dismissArmed = false
            delay(240)
            mounted = false
        }
    }
    if (!mounted) return

    val motionEnabled = TeakMotion.enabled
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible
    val transition = androidx.compose.animation.core.rememberTransition(
        transitionState,
        label = "teakDropdownPanel",
    )
    val fraction by transition.animateFloat(
        transitionSpec = {
            when {
                !motionEnabled -> snap()
                targetState -> spring(
                    dampingRatio = 0.76f,
                    stiffness = 350f,
                    visibilityThreshold = 1e-4f,
                )
                else -> tween(240, easing = FastOutSlowInEasing)
            }
        },
        label = "fraction",
    ) { state -> if (state) 1f else 0f }
    val fade by transition.animateFloat(
        transitionSpec = {
            if (!motionEnabled) {
                snap()
            } else if (targetState) {
                tween(200)
            } else {
                tween(220)
            }
        },
        label = "alpha",
    ) { state -> if (state) 1f else 0f }

    Dialog(
        onDismissRequest = { if (dismissArmed) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect { dialogWindow?.setDimAmount(0f) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        val panelWidthPx = with(density) { width.toPx() }
        val statusTopPx = androidx.compose.foundation.layout.WindowInsets.statusBars
            .getTop(density).toFloat()
        val anchor = anchorBounds
        val desiredX = if (anchor != null) {
            anchor.right - with(density) { anchorEndGap.toPx() } - panelWidthPx
        } else {
            screenWidthPx - with(density) { endPadding.toPx() } - panelWidthPx
        }
        val desiredY = if (anchor != null) {
            anchor.bottom + with(density) { anchorTopGap.toPx() }
        } else {
            statusTopPx + with(density) { topPadding.toPx() }
        }
        val margin = with(density) { 8.dp.toPx() }
        val panelX = desiredX.coerceIn(margin, (screenWidthPx - panelWidthPx - margin).coerceAtLeast(margin))
        val maxPanelHeightPx = with(density) { maxHeight.toPx() }
        val panelY = desiredY.coerceIn(
            margin,
            (screenHeightPx - maxPanelHeightPx - margin).coerceAtLeast(margin),
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = null, indication = null) {
                        if (dismissArmed) onDismiss()
                    },
            )
            Box(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(panelX.toInt(), panelY.toInt()) }
                    .width(width)
                    .then(modifier),
            ) {
                // 阴影层：与设置页一致，展开时比面板先出现，收起时先消失。
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val t = fraction.coerceAtLeast(0f)
                            val a = fade.coerceIn(0f, 1f)
                            alpha = if (visible) {
                                (a * 6f).coerceIn(0f, 1f)
                            } else {
                                (a * a).coerceIn(0f, 1f)
                            }
                            val s = 0.15f + 0.85f * t
                            scaleX = s
                            scaleY = s
                            transformOrigin = TransformOrigin(1f, 0f)
                        }
                        .dropShadow(
                            shape = RoundedCornerShape(18.dp),
                            shadow = Shadow(
                                radius = 8.dp,
                                color = Color.Black,
                                alpha = 0.10f,
                            ),
                        )
                        .background(Color.White.copy(alpha = 0.001f), RoundedCornerShape(18.dp)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val t = fraction.coerceAtLeast(0f)
                            alpha = fade.coerceIn(0f, 1f)
                            val s = 0.15f + 0.85f * t
                            scaleX = s
                            scaleY = s
                            transformOrigin = TransformOrigin(1f, 0f)
                            val blurDp = ((1f - t.coerceIn(0f, 1f)) * 6f /
                                s.coerceAtLeast(0.2f)).coerceAtMost(10f)
                            renderEffect = if (blurDp > 0.05f) {
                                val r = blurDp.dp.toPx()
                                BlurEffect(
                                    radiusX = r,
                                    radiusY = r,
                                    edgeTreatment = TileMode.Decal,
                                )
                            } else {
                                null
                            }
                        },
                ) {
                    TeakGlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        tintAlpha = 0.3f,
                        // 搜索页的白底不在 backdrop 采集范围内；没有采样源时退化成
                        // 与页面同色的纯色面板，避免亮色主题下出现一整块黑。
                        solid = com.aritxonly.teak.common.material.LocalTeakBackdrop.current == null,
                        solidColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxHeight)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = contentPadding),
                            content = content,
                        )
                    }
                }
            }
        }
    }
}
