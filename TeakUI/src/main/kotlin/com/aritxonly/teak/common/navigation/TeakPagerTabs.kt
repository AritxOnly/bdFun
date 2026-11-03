package com.aritxonly.teak.common.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakMotion
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay

/** 视频页 / 二级页的玻璃 Tab 规范；数值与首页底栏保持同一套视觉节奏。 */
object TeakPagerTabDefaults {
    val Height = 40.dp
    val ItemWidth = 60.dp
    val ContentPadding = 4.dp
    val ItemHeight = 32.dp
    val IndicatorAlpha = 0.075f
    const val IndicatorStiffness = 340f
    const val IndicatorDamping = 25f
    val LabelFontSize = 13.sp
}

/**
 * 与底栏同一套逻辑的 Tab：玻璃底座 + **灰色**滑动指示器 + **选中粉色字**。
 * 玻璃底座、指示器动画和弹簧参数沿用 HyperPiliPlus 底栏体系的观感；
 * 组件代码是 Teak 重新组合的，不是上游同名文件；来源记录见 docs/PROVENANCE.md。
 * 必须放在采集层之外（Activity 覆盖层），否则玻璃会自引用。
 */
@Composable
fun TeakPagerTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemWidth: androidx.compose.ui.unit.Dp = TeakPagerTabDefaults.ItemWidth,
) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

    // 指示器位置：手动补间（本项目 frame clock 动画不推进）
    // 只比最初 320/22 稍微弹一点，避免过冲
    val animIndex = rememberPagerSpring(
        target = selected.toFloat(),
        stiffness = TeakPagerTabDefaults.IndicatorStiffness,
        damping = TeakPagerTabDefaults.IndicatorDamping,
    )

    TeakGlassSurface(
        modifier = modifier.height(TeakPagerTabDefaults.Height),
        shape = Capsule(),
        recipe = recipe,
        backdrop = LocalTeakBackdrop.current,
    ) {
        Box(modifier = Modifier.padding(TeakPagerTabDefaults.ContentPadding)) {
            // 灰色指示器
            Box(
                modifier = Modifier
                    .offset(x = itemWidth * animIndex)
                    .width(itemWidth)
                    .height(TeakPagerTabDefaults.ItemHeight)
                    .background(
                        Color.Black.copy(alpha = TeakPagerTabDefaults.IndicatorAlpha),
                        Capsule(),
                    ),
            )
            Row {
                options.forEachIndexed { index, label ->
                    val active = index == selected
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(TeakPagerTabDefaults.ItemHeight)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (active) colors.primary else colors.onBackgroundVariant,
                            fontSize = TeakPagerTabDefaults.LabelFontSize,
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

/** 与页面 chrome 同一套手动弹簧：不依赖宿主 App 的 rememberSpringFloat。 */
@Composable
private fun rememberPagerSpring(
    target: Float,
    stiffness: Float,
    damping: Float,
): Float {
    var value by remember { mutableFloatStateOf(target) }
    var velocity by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(target) {
        if (!TeakMotion.enabled) {
            value = target
            velocity = 0f
            return@LaunchedEffect
        }
        var last = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            last = now
            val accel = -stiffness * (value - target) - damping * velocity
            velocity += accel * dt
            value += velocity * dt
            if (kotlin.math.abs(value - target) < 0.0006f &&
                kotlin.math.abs(velocity) < 0.006f
            ) {
                value = target
                velocity = 0f
                break
            }
            delay(8)
        }
    }
    return value
}
