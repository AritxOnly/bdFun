package com.aritxonly.teak.common.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.settings.TeakDropdownPanel
import kotlinx.coroutines.delay

/**
 * 右上角“省略号 / 漏斗”菜单的统一入口。
 *
 * 把收藏页省略号那一整套动作收进 TeakUI，任何按钮都能复用：
 * 1. 点击后按钮先往左下让位、缩小并渐隐；
 * 2. 菜单随后从右上角弹出，缩放 + 由模糊变清晰；
 * 3. 关闭时按钮回到原位、恢复显示。
 *
 * [trigger] 只负责画按钮外观，点击事件由本组件注入，所以可以是省略号、
 * 漏斗、齿轮或任何自定义图标。
 */
@Composable
fun TeakMenuAnchor(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pressOffsetX: Dp = (-9).dp,
    pressOffsetY: Dp = 9.dp,
    pressScale: Float = 0.94f,
    panelWidth: Dp = 196.dp,
    panelMaxHeight: Dp = 460.dp,
    panelTopPadding: Dp = 36.dp,
    panelEndPadding: Dp = 28.dp,
    panelContentPadding: Dp = 6.dp,
    trigger: @Composable (onClick: () -> Unit) -> Unit,
    panel: @Composable ColumnScope.() -> Unit,
) {
    // 按钮的让位和渐隐跟随 expanded；关闭时一起回位。
    val press by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 700f,
            visibilityThreshold = 0.001f,
        ),
        label = "teakMenuAnchorPress",
    )
    var triggerAlpha by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(expanded) {
        val from = triggerAlpha
        val to = if (expanded) 0f else 1f
        if (from == to) return@LaunchedEffect
        val start = System.nanoTime()
        while (true) {
            val p = ((System.nanoTime() - start) / 150_000_000f).coerceIn(0f, 1f)
            triggerAlpha = from + (to - from) * p
            if (p >= 1f) break
            delay(8)
        }
        triggerAlpha = to
    }

    var anchorBounds by remember { mutableStateOf<Rect?>(null) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    anchorBounds = Rect(
                        left = pos.x,
                        top = pos.y,
                        right = pos.x + coords.size.width,
                        bottom = pos.y + coords.size.height,
                    )
                }
                .graphicsLayer {
                    translationX = pressOffsetX.toPx() * press
                    translationY = pressOffsetY.toPx() * press
                    alpha = triggerAlpha
                    val s = 1f - (1f - pressScale) * press
                    scaleX = s
                    scaleY = s
                },
        ) {
            trigger { onExpandedChange(true) }
        }
        TeakDropdownPanel(
            visible = expanded,
            onDismiss = { onExpandedChange(false) },
            width = panelWidth,
            maxHeight = panelMaxHeight,
            anchorBounds = anchorBounds,
            topPadding = panelTopPadding,
            endPadding = panelEndPadding,
            contentPadding = panelContentPadding,
            content = panel,
        )
    }
}
