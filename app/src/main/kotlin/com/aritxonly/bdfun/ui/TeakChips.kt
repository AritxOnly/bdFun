package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.bdfun.BrandPink
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * tab 栏右侧的小胶囊（「点我发弹幕」「弹」）。
 *
 * 与「简介/评论」同一套真玻璃；「弹」开启时叠一层粉色玻璃（半透明，仍然能透出底层模糊），
 * 文字转白；关闭时回到与 tab 一致的灰字白玻璃。
 */
@Composable
fun TeakGlassChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 34.dp,
    horizontalPadding: Dp = 14.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val backdrop = LocalTeakBackdrop.current
    val colors = MiuixTheme.colorScheme

    TeakGlassSurface(
        modifier = modifier
            .height(height)
            .clip(Capsule())
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        shape = Capsule(),
        recipe = recipe,
        backdrop = backdrop,
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.height(height)) {
            if (active) {
                // 粉色玻璃：不是纯色块，留一点透明度让底下的模糊透出来
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    BrandPink.copy(alpha = 0.92f),
                                    BrandPink.copy(alpha = 0.78f),
                                ),
                            ),
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.38f),
                            Capsule(),
                        ),
                )
            }
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .height(height)
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = if (active) Color.White else colors.onBackgroundVariant,
                    fontSize = fontSize,
                    maxLines = 1,
                )
            }
        }
    }
}

enum class MenuGlyphKind { Reply, Copy, Report, Block, Delete, Coin, Star }

/** 评论长按菜单的小图标（1.6dp 描边、圆头）。 */
@Composable
fun MenuGlyph(kind: MenuGlyphKind, tint: Color, size: Dp = 18.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(
            width = w * 0.095f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (kind) {
            MenuGlyphKind.Reply -> {
                val path = Path().apply {
                    moveTo(w * 0.16f, h * 0.46f)
                    lineTo(w * 0.40f, h * 0.24f)
                    moveTo(w * 0.16f, h * 0.46f)
                    lineTo(w * 0.40f, h * 0.68f)
                    moveTo(w * 0.18f, h * 0.46f)
                    cubicTo(w * 0.52f, h * 0.46f, w * 0.66f, h * 0.50f, w * 0.82f, h * 0.72f)
                }
                drawPath(path, tint, style = stroke)
            }
            MenuGlyphKind.Copy -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.28f, h * 0.12f),
                    size = Size(w * 0.50f, h * 0.50f),
                    cornerRadius = CornerRadius(w * 0.10f),
                    style = stroke,
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.34f),
                    size = Size(w * 0.50f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.10f),
                    style = stroke,
                )
            }
            MenuGlyphKind.Report -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.26f, h * 0.12f),
                    end = Offset(w * 0.26f, h * 0.88f),
                    strokeWidth = w * 0.095f,
                    cap = StrokeCap.Round,
                )
                val flag = Path().apply {
                    moveTo(w * 0.30f, h * 0.16f)
                    lineTo(w * 0.78f, h * 0.16f)
                    lineTo(w * 0.64f, h * 0.36f)
                    lineTo(w * 0.78f, h * 0.56f)
                    lineTo(w * 0.30f, h * 0.56f)
                    close()
                }
                drawPath(flag, tint, style = stroke)
            }
            MenuGlyphKind.Block -> {
                drawCircle(color = tint, radius = w * 0.34f, style = stroke)
                drawLine(
                    color = tint,
                    start = Offset(w * 0.28f, h * 0.72f),
                    end = Offset(w * 0.72f, h * 0.28f),
                    strokeWidth = w * 0.095f,
                    cap = StrokeCap.Round,
                )
            }
            MenuGlyphKind.Delete -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.18f, h * 0.26f),
                    end = Offset(w * 0.82f, h * 0.26f),
                    strokeWidth = w * 0.095f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.40f, h * 0.26f),
                    end = Offset(w * 0.40f, h * 0.16f),
                    strokeWidth = w * 0.095f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.40f, h * 0.16f),
                    end = Offset(w * 0.60f, h * 0.16f),
                    strokeWidth = w * 0.095f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.60f, h * 0.16f),
                    end = Offset(w * 0.60f, h * 0.26f),
                    strokeWidth = w * 0.095f,
                    cap = StrokeCap.Round,
                )
                val body = Path().apply {
                    moveTo(w * 0.26f, h * 0.30f)
                    lineTo(w * 0.30f, h * 0.84f)
                    lineTo(w * 0.70f, h * 0.84f)
                    lineTo(w * 0.74f, h * 0.30f)
                }
                drawPath(body, tint, style = stroke)
                listOf(0.42f, 0.58f).forEach { x ->
                    drawLine(
                        color = tint,
                        start = Offset(w * x, h * 0.44f),
                        end = Offset(w * x, h * 0.72f),
                        strokeWidth = w * 0.075f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            MenuGlyphKind.Coin -> {
                drawCircle(color = tint, radius = w * 0.34f, style = stroke)
                drawLine(
                    color = tint,
                    start = Offset(w * 0.38f, h * 0.42f),
                    end = Offset(w * 0.62f, h * 0.42f),
                    strokeWidth = w * 0.085f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.42f),
                    end = Offset(w * 0.50f, h * 0.68f),
                    strokeWidth = w * 0.085f,
                    cap = StrokeCap.Round,
                )
            }
            MenuGlyphKind.Star -> {
                val path = Path().apply {
                    val cx = w * 0.5f
                    val cy = h * 0.5f
                    val outer = w * 0.36f
                    val inner = outer * 0.52f
                    for (k in 0 until 10) {
                        val r = if (k % 2 == 0) outer else inner
                        val angle = (-90 + k * 36) * Math.PI / 180
                        val x = cx + (r * Math.cos(angle)).toFloat()
                        val y = cy + (r * Math.sin(angle)).toFloat()
                        if (k == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
        }
    }
}

/** 空占位（保持 import 稳定用）。 */
@Composable
internal fun MenuGlyphSpacer() {
    Spacer(Modifier.size(0.dp))
}

/** UP 标识：首页卡片与评论区共用同一套样式。 */
@Composable
fun UpBadge() {
    Box(
        modifier = Modifier
            .clip(Capsule())
            .background(BrandPink.copy(alpha = 0.14f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = "UP",
            color = BrandPink,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }
}

/** 首页推荐里已关注 UP 的橙色小胶囊，尺寸和 UP 标签完全一致。 */
@Composable
fun FollowedBadge() {
    val orange = Color(0xFFFF8A00)
    Box(
        modifier = Modifier
            .clip(Capsule())
            .background(orange.copy(alpha = 0.14f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = "已关注",
            color = orange,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }
}
