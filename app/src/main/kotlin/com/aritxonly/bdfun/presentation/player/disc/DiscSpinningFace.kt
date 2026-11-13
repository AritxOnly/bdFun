package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DiscSpinningFace(runtime: DiscRuntime, visual: DiscVisual) {
    val cover = runtime.ui.displayedCover
    val context = runtime.context
    val coverPx = runtime.coverPx
    val coverRequest = remember(cover, coverPx) {
        coil.request.ImageRequest.Builder(context)
            .data(cover)
            .size(coverPx)
            .crossfade(false)
            .build()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .graphicsLayer { rotationZ = runtime.ui.spin }
            .then(
                if (visual.glassBlurDp > 0.1f) {
                    Modifier.blur(
                        visual.glassBlurDp.dp,
                        // 裁成圆形：模糊量不变，只是不再把方形模糊边缘露出来。
                        edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment(CircleShape),
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        DiscGrooves(runtime.size)

        // 中间圆封面。
        DiscCover(runtime, coverRequest)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.DiscCover(runtime: DiscRuntime, coverRequest: coil.request.ImageRequest) {
    val cover = runtime.ui.displayedCover
    val colors = MiuixTheme.colorScheme
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
                        (runtime.ui.coverBlur * 8f).dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    ),
            )
        }
    }
}

@Composable
private fun DiscGrooves(size: Dp) {
    val colors = MiuixTheme.colorScheme
    val darkTheme = colors.background.luminance() < 0.5f
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
}
