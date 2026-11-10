package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.R
import kotlinx.coroutines.delay

/**
 * 普通页面的加载占位。
 *
 * 按需求，小电视动图只保留在首页/动态页的下拉刷新里；
 * 其他页面不再显示任何加载图标，所以这里保持空实现。
 * 调用点不用删，布局会自然收起。
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun TeakLoading(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 0.dp,
    trackColor: Color = Color.Unspecified,
    strokeCap: StrokeCap = StrokeCap.Round,
    size: Dp = 88.dp,
    animated: Boolean = true,
    tint: Color? = null,
) {
}

/** 下拉刷新专用：B 站小电视动图，只有在首页/动态页下拉时出现。 */
@Composable
fun TeakTvLoading(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    /** false 时停在第一帧（下拉过程中用静态，松手后才播动画）。 */
    animated: Boolean = true,
    /** 传了就整体染色（比如视频页黑色背景上用白色）。 */
    tint: Color? = null,
) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(animated) {
        if (!animated) {
            frame = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(TeakLoadingFrameMillis)
            frame = (frame + 1) % TeakLoadingFrames.size
        }
    }

    Image(
        painter = painterResource(TeakLoadingFrames[frame]),
        contentDescription = "加载中",
        contentScale = ContentScale.Fit,
        colorFilter = tint?.let {
            androidx.compose.ui.graphics.ColorFilter.tint(
                it,
                androidx.compose.ui.graphics.BlendMode.SrcIn,
            )
        },
        modifier = modifier.size(size),
    )
}

/** GIF 原始帧间隔 30ms。 */
private const val TeakLoadingFrameMillis = 30L

private val TeakLoadingFrames = intArrayOf(
    R.drawable.teak_loading_00,
    R.drawable.teak_loading_01,
    R.drawable.teak_loading_02,
    R.drawable.teak_loading_03,
    R.drawable.teak_loading_04,
    R.drawable.teak_loading_05,
    R.drawable.teak_loading_06,
    R.drawable.teak_loading_07,
    R.drawable.teak_loading_08,
    R.drawable.teak_loading_09,
    R.drawable.teak_loading_10,
    R.drawable.teak_loading_11,
    R.drawable.teak_loading_12,
    R.drawable.teak_loading_13,
    R.drawable.teak_loading_14,
    R.drawable.teak_loading_15,
)
