package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** 把官方 22×22 视口的路径按当前画布尺寸等比缩放后绘制。 */
private fun DrawScope.drawViewportPaths(
    paths: List<Path>,
    viewport: Float,
    color: Color,
    strokeWidth: Float? = null,
) {
    scale(size.width / viewport, size.height / viewport, pivot = Offset.Zero) {
        paths.forEach { path ->
            if (strokeWidth == null) {
                drawPath(path, color)
            } else {
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

private fun parsePaths(vararg data: String): List<Path> =
    data.map { PathParser().parsePathString(it).toPath() }

@Composable
internal fun InlineBackGlyph() {
    androidx.compose.material3.Icon(MiuixIcons.ChevronBackward, contentDescription = "返回",
        tint = Color.White, modifier = Modifier.size(20.dp))
}

/** 首页：官方播放器顶栏的“小房子”（biliplayer_ic_topbar_back_bold，22×22）。 */
@Composable
internal fun InlineHomeGlyph(tint: Color = Color.White) {
    val paths = remember {
        parsePaths(
            "M2 9.64352L10.9535 2L19.8727 9.64352",
            "M17.9642,7.8686L17.9642,15.6321C17.9642,17.8412 16.1734,19.6321 13.9642,19.6321L8.061,19.6321C5.8519,19.6321 4.061,17.8412 4.061,15.6321L4.061,11.5731",
            "M8.45352 13.6154C8.45352 13.6154 8.88545 15.5 10.9427 15.5C13 15.5 13.4319 13.6154 13.4319 13.6154",
        )
    }
    Canvas(modifier = Modifier.size(21.dp)) {
        drawViewportPaths(paths, viewport = 22f, color = tint, strokeWidth = 1.7f)
    }
}

/** 听视频：官方矢量耳机（ic_audio_headset，16×16 视口）。 */
@Composable
internal fun InlineHeadphoneGlyph() {
    val paths = remember {
        parsePaths(
            "M1.6667,8.0003C1.6667,4.5025 4.5023,1.667 8.0001,1.667C11.4979,1.667 14.3334,4.5025 14.3334,8.0003V12.167C14.3334,12.9954 13.6618,13.667 12.8334,13.667H12.5001C11.3034,13.667 10.3334,12.697 10.3334,11.5003V10.167C10.3334,8.9704 11.3034,8.0003 12.5001,8.0003L13.3321,8.0037V7.917C13.2881,5.01 10.9177,2.667 8.0001,2.667C5.0824,2.667 2.7121,5.01 2.6681,7.917V8.0037L3.5001,8.0003C4.6967,8.0003 5.6667,8.9704 5.6667,10.167V11.5003C5.6667,12.697 4.6967,13.667 3.5001,13.667H3.1667C2.3383,13.667 1.6667,12.9954 1.6667,12.167V8.0003ZM2.6667,9.0003V12.167C2.6667,12.4431 2.8906,12.667 3.1667,12.667H3.5001C4.1444,12.667 4.6667,12.1447 4.6667,11.5003V10.167C4.6667,9.5227 4.1444,9.0003 3.5001,9.0003H2.6667ZM13.3334,9.0003H12.5001C11.8557,9.0003 11.3334,9.5227 11.3334,10.167V11.5003C11.3334,12.1447 11.8557,12.667 12.5001,12.667H12.8334C13.1095,12.667 13.3334,12.4431 13.3334,12.167V9.0003Z",
        )
    }
    Canvas(modifier = Modifier.size(22.dp)) {
        drawViewportPaths(paths, viewport = 16f, color = Color.White)
    }
}

/** 投屏：官方顶栏的电视图标（biliplayer_ic_topbar_tv_bold，22×22）。 */
@Composable
internal fun InlineCastGlyph() {
    val paths = remember {
        parsePaths(
            "M11.8542,15.5903L15.0599,18.4398C15.4284,18.7674 15.4616,19.3318 15.134,19.7003C14.9646,19.8909 14.7217,20 14.4667,20L7.5333,20C7.0402,20 6.6404,19.6003 6.6404,19.1071C6.6404,18.8521 6.7495,18.6092 6.9401,18.4398L10.1458,15.5903C10.633,15.1573 11.367,15.1573 11.8542,15.5903ZM10.8102,16.3377L7.8151,19L14.1849,19L11.1898,16.3377C11.0816,16.2415 10.9184,16.2415 10.8102,16.3377ZM17.9231,3C19.8998,3 21.5,4.618 21.5,6.6111L21.5,14.3889C21.5,16.382 19.8998,18 17.9231,18L16.3846,18C16.1085,18 15.8846,17.7761 15.8846,17.5C15.8846,17.2239 16.1085,17 16.3846,17L17.9231,17C19.345,17 20.5,15.8322 20.5,14.3889L20.5,6.6111C20.5,5.1678 19.345,4 17.9231,4L4.0769,4C2.655,4 1.5,5.1678 1.5,6.6111L1.5,14.3889C1.5,15.8322 2.655,17 4.0769,17L5.6154,17C5.8915,17 6.1154,17.2239 6.1154,17.5C6.1154,17.7761 5.8915,18 5.6154,18L4.0769,18C2.1002,18 0.5,16.382 0.5,14.3889L0.5,6.6111C0.5,4.618 2.1002,3 4.0769,3L17.9231,3ZM8,7.75L8.009,7.75L10.5,7.75C10.7761,7.75 11,7.9739 11,8.25C11,8.5261 10.7761,8.75 10.5,8.75L8.499,8.75L8.5,13.75C8.5,14.0261 8.2761,14.25 8,14.25C7.7239,14.25 7.5,14.0261 7.5,13.75L7.499,8.75L5.5,8.75C5.2239,8.75 5,8.5261 5,8.25C5,7.9739 5.2239,7.75 5.5,7.75L7.9897,7.7501C7.9931,7.75 7.9966,7.75 8,7.75ZM17.2069,7.7948C17.4583,7.9091 17.5695,8.2055 17.4552,8.4569L14.9552,13.9569C14.7775,14.3477 14.2225,14.3477 14.0448,13.9569L11.5448,8.4569C11.4305,8.2055 11.5417,7.9091 11.7931,7.7948C12.0445,7.6805 12.3409,7.7917 12.4552,8.0431L14.5,12.5417L16.5448,8.0431C16.6591,7.7917 16.9555,7.6805 17.2069,7.7948Z",
        )
    }
    Canvas(modifier = Modifier.size(23.dp)) {
        drawViewportPaths(paths, viewport = 22f, color = Color.White)
        drawViewportPaths(paths, viewport = 22f, color = Color.White, strokeWidth = 0.5f)
    }
}

@Composable
internal fun InlineMoreGlyph(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(22.dp)) {
        repeat(3) { i ->
            drawCircle(
                color = tint,
                radius = 1.8.dp.toPx(),
                center = Offset(size.width / 2f, size.height * (0.24f + i * 0.26f)),
            )
        }
    }
}

@Composable
internal fun DanmakuSettingsGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(19.dp)) {
        val w = size.width
        val stroke = w * 0.10f
        val ys = listOf(0.24f, 0.50f, 0.76f)
        val knobX = listOf(0.62f, 0.38f, 0.58f)
        ys.forEachIndexed { index, y ->
            drawLine(
                color = tint,
                start = androidx.compose.ui.geometry.Offset(w * 0.10f, w * y),
                end = androidx.compose.ui.geometry.Offset(w * 0.90f, w * y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = tint,
                radius = w * 0.11f,
                center = androidx.compose.ui.geometry.Offset(w * knobX[index], w * y),
            )
        }
    }
}

/** 后台播放图标：耳机。 */
@Composable
internal fun BackgroundPlayGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(19.dp)) {
        val w = size.width
        val stroke = w * 0.10f
        drawArc(
            color = tint,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.12f, w * 0.14f),
            size = androidx.compose.ui.geometry.Size(w * 0.76f, w * 0.76f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = StrokeCap.Round,
            ),
        )
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.12f, w * 0.50f),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, w * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
        )
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.70f, w * 0.50f),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, w * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
        )
    }
}
