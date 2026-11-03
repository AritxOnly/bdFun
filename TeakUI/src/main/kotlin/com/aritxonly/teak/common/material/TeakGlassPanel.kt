package com.aritxonly.teak.common.material

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.TeakMaterialFineTuning
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 平玻璃配方：保留磨砂，折射只开一点点（试验值），高光收着用。
 * 之前大面板上那个"圆形覆盖"是小胶囊配方的折射/高光按大尺寸绘制出来的，
 * 所以这里把 height / amount / depthEffect 都压低，并把 shader 的圆角对齐到 18dp。
 */
private fun flatRecipe(tuning: TeakMaterialFineTuning) =
    TeakGlassRecipes.bottomSheet(tuning).let { base ->
        base.copy(
            // 大面板走底栏配方的 24%（原来 12%）：用户要求磨砂再厚一倍
            blur = base.blur.copy(radiusMultiplier = base.blur.radiusMultiplier * 0.24f),
            refraction = base.refraction.copy(
                enabled = true,
                height = 10.dp,
                amount = 8.dp,
                depthEffect = 0.35f,
                chromaticAberration = 0.5.dp,
            ),
            // shader 的圆角必须和面板形状一致，否则高光会落在错误的边上
            geometry = base.geometry.copy(cornerRadius = 18.dp),
            edgeOptics = base.edgeOptics.copy(
                highlightAlpha = 0.60f,
                darkHighlightMultiplier = 0.60f,
                fallbackWidth = 1.5.dp,
                fallbackLightAlpha = 0.60f,
                fallbackDarkAlpha = 0.12f,
            ),
        )
    }

/**
 * 弹窗底板，两种模式：
 * - 默认（页面上的弹窗、对话框）：真玻璃磨砂，平配方，没有圆形高光。
 * - [solid] = true：纯渐变卡片。播放器控件层（弹幕设置、倍速）必须用这个 ——
 *   那一层本身在 backdrop 录制中，再读 backdrop 会成环（闪退/花屏）。
 */
@Composable
fun TeakGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    solid: Boolean = false,
    solidColor: Color = Color(0xFF1A1B20),
    tintAlpha: Float = 1f,
    backdropImage: ImageBitmap? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val rim = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.16f)),
    )
    // 玻璃面板只加一点点底灰：亮色主题压 2%，暗色主题提 3%（多了会发闷）
    val resolvedTintAlpha = tintAlpha.coerceIn(0f, 1f)
    val tint = if (MiuixTheme.colorScheme.onBackground.luminance() > 0.5f) {
        Color.White.copy(alpha = 0.03f * resolvedTintAlpha)
    } else {
        Color.Black.copy(alpha = 0.02f * resolvedTintAlpha)
    }
    val backdrop = LocalTeakBackdrop.current
    if (!solid && backdrop != null) {
        val recipe = flatRecipe(LocalTeakMaterialSpec.current.fineTuning)
        TeakGlassSurface(
            modifier = modifier,
            shape = shape,
            recipe = recipe,
            backdrop = backdrop,
            contentAlignment = androidx.compose.ui.Alignment.TopStart,
        ) {
            Box(modifier = Modifier.matchParentSize().background(tint))
            content()
        }
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(solidColor),
        ) {
            if (backdropImage != null) {
                // 截一帧视频做底，模糊后压在面板下面 —— 就是面板区域的"磨砂视频"
                Image(
                    bitmap = backdropImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .blur(24.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(solidColor.copy(alpha = 0.60f)),
                )
            }
            Box(modifier = Modifier.matchParentSize().border(1.dp, rim, shape))
            content()
        }
    }
}
