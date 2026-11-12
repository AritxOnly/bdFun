package com.aritxonly.bdfun.ui

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule

@Composable
internal fun PixelIcon(
    @DrawableRes res: Int,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    contentDescription: String? = null,
) {

    val context = LocalContext.current
    val bitmap = remember(res) {
        BitmapFactory.decodeResource(context.resources, res).asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = tint?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) },
        filterQuality = FilterQuality.High,
    )
}

@Composable
internal fun BackChevron(tint: Color = Color.White) {

    androidx.compose.material3.Icon(MiuixIcons.ChevronBackward, contentDescription = "返回",
        tint = tint, modifier = Modifier.size(23.5.dp))
}

@Composable
internal fun MoreGlyph(tint: Color = Color.White) {

    Canvas(modifier = Modifier.size(20.dp)) {
        val r = size.width * 0.075f
        listOf(0.22f, 0.5f, 0.78f).forEach { y ->
            drawCircle(tint, radius = r, center = Offset(size.width * 0.5f, size.height * y))
        }
    }
}

@Composable
internal fun CloseGlyph(tint: Color = Color.White.copy(alpha = 0.7f)) {

    Canvas(modifier = Modifier.size(18.dp)) {
        val s = size.width * 0.12f
        drawLine(tint, Offset(size.width * 0.2f, size.height * 0.2f), Offset(size.width * 0.8f, size.height * 0.8f), s, StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.8f, size.height * 0.2f), Offset(size.width * 0.2f, size.height * 0.8f), s, StrokeCap.Round)
    }
}

@Composable
internal fun CheckGlyph(tint: Color = Color.White) {

    Canvas(modifier = Modifier.size(15.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.52f)
            lineTo(size.width * 0.42f, size.height * 0.78f)
            lineTo(size.width * 0.86f, size.height * 0.25f)
        }
        drawPath(
            path,
            tint,
            style = Stroke(size.width * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
internal fun ChevronRightGlyph(tint: Color = Color.White.copy(alpha = 0.4f)) {

    Canvas(modifier = Modifier.size(14.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.38f, size.height * 0.2f)
            lineTo(size.width * 0.68f, size.height * 0.5f)
            lineTo(size.width * 0.38f, size.height * 0.8f)
        }
        drawPath(path, tint, style = Stroke(size.width * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
internal fun PlayGlyph(tint: Color = Color.White, size: androidx.compose.ui.unit.Dp = 24.dp) {

    PixelIcon(
        res = TeakIcon.PlayerPlay,
        tint = tint,
        contentDescription = "播放",
        modifier = Modifier.size(size),
    )
}

@Composable
internal fun PauseGlyph(tint: Color = Color.White) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 6.dp, height = 22.dp).clip(Capsule()).background(tint))
        Box(Modifier.size(width = 6.dp, height = 22.dp).clip(Capsule()).background(tint))
    }
}

@Composable
internal fun NextGlyph(enabled: Boolean) {

    PixelIcon(
        res = TeakIcon.PlayerNext,
        tint = Color.White,
        contentDescription = "下一集",
        modifier = Modifier
            .size(19.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.28f },
    )
}

@Composable
internal fun DanmakuToggleGlyph(enabled: Boolean) {

    PixelIcon(
        res = if (enabled) TeakIcon.PlayerDanmakuOn else TeakIcon.PlayerDanmakuOff,
        contentDescription = if (enabled) "关闭弹幕" else "开启弹幕",
        modifier = Modifier.size(23.dp),
    )
}

@Composable
internal fun SlidersGlyph() {

    PixelIcon(
        res = TeakIcon.PlayerDanmakuSettings,
        contentDescription = "弹幕设置",
        modifier = Modifier.size(23.dp),
    )
}

@Composable
internal fun HeadphoneGlyph(tint: Color = Color.White) {

    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = size.width * 0.09f
        drawArc(
            tint,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(size.width * 0.13f, size.height * 0.13f),
            size = Size(size.width * 0.74f, size.height * 0.74f),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawRoundRect(
            tint,
            topLeft = Offset(size.width * 0.13f, size.height * 0.5f),
            size = Size(size.width * 0.17f, size.height * 0.33f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.07f),
        )
        drawRoundRect(
            tint,
            topLeft = Offset(size.width * 0.7f, size.height * 0.5f),
            size = Size(size.width * 0.17f, size.height * 0.33f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.07f),
        )
    }
}

@Composable
internal fun ClockGlyph(tint: Color = Color.White) {

    Canvas(modifier = Modifier.size(24.dp)) {
        drawCircle(tint, radius = size.width * 0.38f, style = Stroke(size.width * 0.08f))
        drawLine(tint, center, Offset(center.x, size.height * 0.28f), size.width * 0.07f, StrokeCap.Round)
        drawLine(tint, center, Offset(size.width * 0.68f, center.y), size.width * 0.07f, StrokeCap.Round)
    }
}

@Composable
internal fun PipGlyph(tint: Color = Color.White) {

    Canvas(modifier = Modifier.size(24.dp)) {
        drawRoundRect(
            tint,
            topLeft = Offset(size.width * 0.08f, size.height * 0.12f),
            size = Size(size.width * 0.84f, size.height * 0.64f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = Stroke(size.width * 0.07f),
        )
        drawRoundRect(
            tint,
            topLeft = Offset(size.width * 0.52f, size.height * 0.48f),
            size = Size(size.width * 0.34f, size.height * 0.22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
        )
    }
}

@Composable
internal fun CastGlyph(tint: Color = Color.White) {

    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = size.width * 0.07f
        drawArc(
            tint,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
            size = Size(size.width * 0.8f, size.height * 0.8f),
            style = Stroke(stroke),
        )
        drawArc(
            tint,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(size.width * 0.28f, size.height * 0.28f),
            size = Size(size.width * 0.44f, size.height * 0.44f),
            style = Stroke(stroke),
        )
        drawCircle(tint, radius = size.width * 0.07f, center = Offset(size.width * 0.26f, size.height * 0.74f))
    }
}

/* -------------------------------------------------------------- helpers */
