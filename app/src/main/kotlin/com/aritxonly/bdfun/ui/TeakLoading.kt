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
 * 全局加载指示：B 站小电视动图。
 *
 * 素材是原始 GIF（16 帧 / 30ms）逐帧去白底得到的透明 PNG 序列，所以底色任意都不会有白块，
 * 动画就是原图的动画（吉祥物上下浮动 + 天线晃动），不是程序化模拟。
 *
 * 参数签名与 [androidx.compose.material3.CircularProgressIndicator] 兼容，
 * 调用点把名字换掉即可（color / strokeWidth 等参数会被忽略）。
 */
@Composable
fun TeakLoading(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 0.dp,
    trackColor: Color = Color.Unspecified,
    strokeCap: StrokeCap = StrokeCap.Round,
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
